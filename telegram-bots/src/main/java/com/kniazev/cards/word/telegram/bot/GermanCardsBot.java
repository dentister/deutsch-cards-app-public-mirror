package com.kniazev.cards.word.telegram.bot;

import com.kniazev.cards.word.ai.blitz.BlitzCheckResult;
import com.kniazev.cards.word.ai.blitz.BlitzGameService;
import com.kniazev.cards.word.ai.blitz.BlitzSentences;
import com.kniazev.cards.word.error.exception.GameNotStartedException;
import com.kniazev.cards.word.game.GameService;
import com.kniazev.cards.word.game.representation.WordCard;
import com.kniazev.cards.word.model.dictionary.Word;
import com.kniazev.cards.word.model.dictionary.Word.WordLevel;
import com.kniazev.cards.word.model.user.GameConfiguration;
import com.kniazev.cards.word.service.UserService;

import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.*;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@ConditionalOnProperty(name = "telegram.bot.enabled", havingValue = "true", matchIfMissing = true)
public class GermanCardsBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final int MIN_BLITZ_ELIGIBLE_WORDS = 3;

    private final GameService gameService;
    private final UserService userService;
    private final GermanCardsBotKeyboards keyboards;
    private final GameCardPresenter presenter;
    private final BlitzGameService blitzGameService;
    private final String botToken;
    private final String miniAppUrl;
    private final String webAppUrl;
    private final TelegramMessenger messenger;
    private final Map<String, CommandDefinition> commands;
    
    private final Map<String, BlitzSession> blitzSessions = new ConcurrentHashMap<>();

    private record BlitzSession(List<String> sentences) { }

    private record CommandDefinition(String text, String description, CommandHandler handler) { }

    public GermanCardsBot(@Value("${telegram.bot.token}") String botToken,
                          @Value("${miniapp.url:}") String miniAppUrl,
                          @Value("${app.public-url:}") String webAppUrl,
                          GameService gameService,
                          UserService userService,
                          GermanCardsBotKeyboards keyboards,
                          GameCardPresenter presenter,
                          BlitzGameService blitzGameService,
                          @Qualifier("germanCardsBotTelegramClient") TelegramClient telegramClient) {
        this.botToken = botToken;
        this.miniAppUrl = miniAppUrl;
        this.webAppUrl = webAppUrl;
        this.gameService = gameService;
        this.userService = userService;
        this.keyboards = keyboards;
        this.presenter = presenter;
        this.blitzGameService = blitzGameService;
        this.messenger = new TelegramMessenger(telegramClient);
        this.commands = buildCommands();
    }

    @PostConstruct
    public void init() {
        setBotCommands();
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        Message msg = update.getMessage();

        try {
            GameChat chatInfo = update.hasCallbackQuery() ? new GameChat(update.getCallbackQuery()) : new GameChat(msg);

            if (update.hasCallbackQuery()) {
                handleCallbackQuery(update.getCallbackQuery(), chatInfo);
            } else if (msg.isCommand()) {
                blitzSessions.remove(chatInfo.getUsername());
                handleCommand(msg, chatInfo);
            } else if (blitzSessions.containsKey(chatInfo.getUsername())) {
                handleBlitzAnswer(msg, chatInfo);
            } else {
                handleAnswer(msg, chatInfo);
            }
        } catch (TelegramApiException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void handleCallbackQuery(CallbackQuery cbQuery, GameChat chatInfo) throws TelegramApiException {
        String callbackData = cbQuery.getData();

        if (List.of("A1", "A2", "B1", "B2", "C1", "C2").contains(callbackData)) {
            GameConfiguration gameConfiguration = gameService.getGameConfiguration(chatInfo.getUsername());
            WordLevel selectedLevel = WordLevel.valueOf(callbackData);

            gameConfiguration.setMaxLevel(selectedLevel);

            restartGameAndNotify(chatInfo, gameConfiguration);
        } else if (GermanCardsBotKeyboards.CALLBACK_BLITZ_YES.equals(callbackData)) {
            messenger.answerCallback(cbQuery.getId());
            messenger.removeKeyboard(chatInfo.getChatId(), cbQuery.getMessage().getMessageId());

            handleBlitzYes(chatInfo);
        } else if (GermanCardsBotKeyboards.CALLBACK_BLITZ_NO.equals(callbackData)) {
            messenger.answerCallback(cbQuery.getId());
            messenger.removeKeyboard(chatInfo.getChatId(), cbQuery.getMessage().getMessageId());

            handleBlitzNo(chatInfo);
        }
    }

    private void handleAnswer(Message msg, GameChat chatInfo) throws TelegramApiException {
        StringBuilder sb = new StringBuilder();
        boolean gameFinished;
        boolean naturalCompletion = false;

        try {
            WordCard wordCard = gameService.checkUserAnswer(chatInfo.getUsername(), msg.getText());

            sb.append(presenter.feedback(wordCard, wordCard.getLocale()));

            gameFinished = gameService.getGameWords(chatInfo.getUsername()).isEmpty();
            naturalCompletion = gameFinished;
        } catch (GameNotStartedException e) {
            sb.append(Messages.get("bot.game_was_inactive", chatInfo.getLocale()));
            gameFinished = true;
        }

        if (gameFinished) {
            messenger.sendMarkdown(chatInfo.getChatId(), msg.getMessageId(), sb.toString());

            if (naturalCompletion
                    && gameService.getLastRoundWords(chatInfo.getUsername()).size() >= MIN_BLITZ_ELIGIBLE_WORDS) {
                offerBlitzGame(chatInfo);
                return;
            }

            restartGameAndNotify(chatInfo, null);
        } else {
            sb.append("\n\n");
            WordCard nextTask = gameService.getNextTask(chatInfo.getUsername(), chatInfo.getLocale());
            sb.append(presenter.task(nextTask, nextTask.getLocale()));

            messenger.sendMarkdown(chatInfo.getChatId(), msg.getMessageId(), sb.toString());
        }
    }

    private void offerBlitzGame(GameChat chatInfo) throws TelegramApiException {
        messenger.send(chatInfo.getChatId(), Messages.get("bot.blitz_offer", chatInfo.getLocale()),
                keyboards.buildYesNoKeyboard(chatInfo.getLocale()));
    }

    private void handleBlitzYes(GameChat chatInfo) throws TelegramApiException {
        List<Word> roundWords = gameService.getLastRoundWords(chatInfo.getUsername());
        Optional<BlitzSentences> generated = blitzGameService.generateSentences(roundWords);

        if (generated.isEmpty()) {
            messenger.send(chatInfo.getChatId(), Messages.get("bot.blitz_generation_failed", chatInfo.getLocale()));
            restartGameAndNotify(chatInfo, null);
            return;
        }

        List<String> sentences = generated.get().sentences();
        blitzSessions.put(chatInfo.getUsername(), new BlitzSession(sentences));

        StringBuilder sb = new StringBuilder(Messages.get("bot.blitz_sentences_header", chatInfo.getLocale()));
        sb.append("\n\n");
        for (int i = 0; i < sentences.size(); i++) {
            sb.append(i + 1).append(". ").append(sentences.get(i)).append("\n");
        }

        messenger.sendMarkdown(chatInfo.getChatId(), sb.toString());
    }

    private void handleBlitzNo(GameChat chatInfo) throws TelegramApiException {
        restartGameAndNotify(chatInfo, null);
    }

    private void handleBlitzAnswer(Message msg, GameChat chatInfo) throws TelegramApiException {
        String username = chatInfo.getUsername();
        BlitzSession session = blitzSessions.get(username);
        List<String> translations = parseBlitzAnswer(msg.getText());

        if (translations.isEmpty()) {
            messenger.sendMarkdown(chatInfo.getChatId(), msg.getMessageId(),
                    Messages.get("bot.blitz_answer_incomplete", chatInfo.getLocale()));
            return;
        }

        blitzSessions.remove(username);

        Optional<BlitzCheckResult> result = blitzGameService.checkTranslations(session.sentences(), translations);

        if (result.isEmpty()) {
            messenger.sendMarkdown(chatInfo.getChatId(), msg.getMessageId(),
                    Messages.get("bot.blitz_check_failed", chatInfo.getLocale()));
        } else {
            StringBuilder sb = new StringBuilder(Messages.get("bot.blitz_feedback_header", chatInfo.getLocale()));
            sb.append("\n\n").append(result.get().feedback());

            messenger.sendMarkdown(chatInfo.getChatId(), msg.getMessageId(), sb.toString());
        }

        restartGameAndNotify(chatInfo, null);
    }

    private static List<String> parseBlitzAnswer(String rawText) {
        if (rawText == null) {
            return List.of();
        }

        return Arrays.stream(rawText.split("\\R"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(line -> line.replaceFirst("^\\d+[.):\\-]?\\s*", ""))
                .limit(3)
                .toList();
    }

    @FunctionalInterface
    private interface CommandHandler {
        void handle(GameChat gameChat) throws TelegramApiException;
    }


    private Map<String, CommandDefinition> buildCommands() {
        Map<String, CommandDefinition> map = new LinkedHashMap<>();

        for (CommandDefinition def : List.of(
                new CommandDefinition("/start", "Start work with bot", this::doStart),
                new CommandDefinition("/restart", "Restart game", this::doRestart),
                new CommandDefinition("/setwords", "Choose specific words to study (Mini App)", this::doSetWords),
                new CommandDefinition("/clearwords", "Play all words again (clear specific words)", this::doClearWords),
                new CommandDefinition("/setlevel", "Set max word level", this::doSetLevel),
                //Temporary disabled due to most part of dictionary does not have defined Tag
                //new CommandDefinition("/settag", "Filter words by topic tag", this::doSetTag),
                //new CommandDefinition("/cleartag", "Remove tag filter (all words)", this::doClearTag),
                new CommandDefinition("/switchpreview", "Toggle word list preview on game start", this::doSwitchPreview),
                new CommandDefinition("/website", "Get your web app link and login", this::doWebsite))) {
            map.put(def.text(), def);
        }

        return map;
    }

    private void handleCommand(Message msg, GameChat chatInfo) {
        CommandDefinition def = commands.get(msg.getText());
        if (def == null) {
            return;
        }

        try {
            def.handler().handle(chatInfo);
        } catch (TelegramApiException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void doStart(GameChat gameChat) throws TelegramApiException {
        sendWelcomeMessage(gameChat);

        doRestart(gameChat);
    }

    private void doRestart(GameChat chatInfo) throws TelegramApiException {
        restartGameAndNotify(chatInfo, null);
    }

    private void doSetWords(GameChat chatInfo) throws TelegramApiException {
        sendSpecificWordsButton(chatInfo);
    }

    private void doClearWords(GameChat chatInfo) throws TelegramApiException {
        GameConfiguration gameConfiguration = gameService.getGameConfiguration(chatInfo.getUsername());

        gameConfiguration.setSpecificWordIds(null);

        restartGameAndNotify(chatInfo, gameConfiguration);
    }

    private void doSetLevel(GameChat chatInfo) throws TelegramApiException {
        messenger.send(chatInfo.getChatId(), Messages.get("bot.choose_max_level", chatInfo.getLocale()), keyboards.buildLevelKeyboard());
    }

    private void doSwitchPreview(GameChat chatInfo) throws TelegramApiException {
        GameConfiguration previewCfg = gameService.getGameConfiguration(chatInfo.getUsername());

        previewCfg.setShowWordsPreview(!previewCfg.getShowWordsPreview());

        gameService.saveGameConfiguration(chatInfo.getUsername(), previewCfg);

        messenger.send(chatInfo.getChatId(), previewCfg.getShowWordsPreview()
                ? Messages.get("bot.word_list_preview_enabled", chatInfo.getLocale())
                : Messages.get("bot.word_list_preview_disabled", chatInfo.getLocale()));
    }

    private void doWebsite(GameChat chatInfo) throws TelegramApiException {
        if (webAppUrl == null || webAppUrl.isBlank()) {
            messenger.send(chatInfo.getChatId(), Messages.get("bot.website_not_configured", chatInfo.getLocale()));
            return;
        }

        String newPassword = userService.regeneratePassword(chatInfo.getUsername());

        messenger.sendMarkdown(chatInfo.getChatId(), Messages.get("bot.web_credentials", chatInfo.getLocale(),
                Map.of("url", webAppUrl, "username", chatInfo.getUsername(), "password", newPassword)));
    }

    private record UserResolution(String username, String newPassword) { }

    private UserResolution getOrCreate(User telegramUser) {
        UserService.TelegramUserResolution resolution =
                userService.getOrCreateByTelegramId(telegramUser.getId(), telegramUser.getUserName());

        return new UserResolution(resolution.user().getUsername(), resolution.newPassword());
    }

    private void sendWelcomeMessage(GameChat chatInfo) {
        try {
            messenger.sendMarkdown(chatInfo.getChatId(), Messages.get("bot.welcome", chatInfo.getLocale()));

            if (chatInfo.getNewPassword() != null && webAppUrl != null && !webAppUrl.isBlank()) {
                messenger.sendMarkdown(chatInfo.getChatId(), Messages.get("bot.web_credentials", chatInfo.getLocale(),
                        Map.of("url", webAppUrl, "username", chatInfo.getUsername(), "password", chatInfo.getNewPassword())));
            }
        } catch (TelegramApiException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void setBotCommands() {
        List<BotCommand> commandList = commands.values().stream()
                .map(def -> new BotCommand(def.text(), def.description()))
                .toList();

        try {
            messenger.registerCommands(commandList);
        } catch (TelegramApiException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void sendSpecificWordsButton(GameChat chatInfo) throws TelegramApiException {
        if (miniAppUrl == null || miniAppUrl.isBlank()) {
            messenger.send(chatInfo.getChatId(), Messages.get("bot.word_picker_is_not_ready", chatInfo.getLocale()));
            return;
        }

        messenger.send(chatInfo.getChatId(), Messages.get("bot.tap_to_choose_words", chatInfo.getLocale()),
                InlineKeyboards.singleButtonRow(InlineKeyboardButton.builder()
                        .text(Messages.get("bot.choose_words", chatInfo.getLocale()))
                        .webApp(new WebAppInfo(miniAppUrl))
                        .build()));
    }

    private void restartGameAndNotify(GameChat chatInfo, GameConfiguration gameConfiguration) throws TelegramApiException {
        gameService.restartGame(chatInfo.getUsername(), gameConfiguration, chatInfo.getLocale());

        notifyGameRestarted(chatInfo.getUsername(), chatInfo.getChatId(), chatInfo.getLocale());
    }

    public void notifyGameRestarted(String username, String chatId, Locale locale) throws TelegramApiException {
        WordCard nextTask = gameService.getNextTask(username, locale);
        GameConfiguration actual = gameService.getGameConfiguration(username);

        List<Long> specificWordIds = actual.getSpecificWordIds();
        String banner;

        if (specificWordIds != null && !specificWordIds.isEmpty()) {
            banner = Messages.get("bot.game_with_specific_words_restarted", locale, Map.of("count", Integer.toString(specificWordIds.size())));
        } else {
            banner = Messages.get("bot.game_with_max_level_restarted", locale, Map.of("level", actual.getMaxLevel().name()));
        }

        messenger.send(chatId, banner);

        if (actual.getShowWordsPreview()) {
            List<WordCard> previewWords = gameService.getGameWords(username).stream()
                    .filter(WordCard::isNewWord)
                    .toList();

            if (!previewWords.isEmpty()) {
                messenger.sendMarkdown(chatId, presenter.newWordsPreview(previewWords, locale));
            }
        }

        messenger.sendMarkdown(chatId, presenter.task(nextTask, nextTask.getLocale()));
    }

    @Getter
    private class GameChat {
        final String chatId;
        final String username;
        final String newPassword;
        final Locale locale;

        public GameChat(CallbackQuery cbQuery) {
            chatId = cbQuery.getMessage().getChatId().toString();

            UserResolution resolution = getOrCreate(cbQuery.getFrom());
            username = resolution.username();
            newPassword = resolution.newPassword();

            locale = Messages.resolveLocale(cbQuery.getFrom().getLanguageCode());
        }

        public GameChat(Message msg) {
            chatId = msg.getChatId().toString();

            UserResolution resolution = getOrCreate(msg.getFrom());
            username = resolution.username();
            newPassword = resolution.newPassword();

            locale = Messages.resolveLocale(msg.getFrom().getLanguageCode());
        }
    }

}
