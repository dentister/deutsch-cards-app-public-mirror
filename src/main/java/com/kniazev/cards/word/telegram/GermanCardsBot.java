package com.kniazev.cards.word.telegram;

import com.kniazev.cards.word.db.model.word.Word.WordLevel;
import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.db.services.UserService;
import com.kniazev.cards.word.error.handler.exception.GameNotStartedException;
import com.kniazev.cards.word.game.*;
import com.kniazev.cards.word.services.GameConfiguration;
import com.kniazev.cards.word.services.GameService;
import com.kniazev.cards.word.telegram.constants.TextPatterns;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage.SendMessageBuilder;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.*;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import static com.kniazev.cards.word.telegram.constants.TextPatterns.*;

@Slf4j
@Service
@ConditionalOnProperty(name = "telegram.bot.enabled", havingValue = "true", matchIfMissing = true)
public class GermanCardsBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final String TAG_CALLBACK_PREFIX  = "TAG:";

    private static final String TAG_CLEAR_CALLBACK   = "TAG:__ALL__";

    private static final String TAG_APPLY_CALLBACK   = "TAG:__APPLY__";

    private static final int    TAG_COLUMNS           = 2;

    private final GameService gameService;
    private final UserService userService;
    private final Set<String> usersCache = new HashSet<>();
    private final String botToken;
    private final String miniAppUrl;
    private final TelegramClient telegramClient;

    public GermanCardsBot(@Value("${telegram.bot.token}") String botToken,
                          @Value("${miniapp.url:}") String miniAppUrl,
                          GameService gameService,
                          UserService userService) {
        this.botToken = botToken;
        this.miniAppUrl = miniAppUrl;
        this.gameService = gameService;
        this.userService = userService;
        this.telegramClient = new OkHttpTelegramClient(botToken);
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
            if (update.hasCallbackQuery()) {
                String callbackData = update.getCallbackQuery().getData();
                String username     = getOrCreate(update.getCallbackQuery().getFrom());
                String chatId       = update.getCallbackQuery().getMessage().getChatId().toString();

                if (callbackData.startsWith(TAG_CALLBACK_PREFIX)) {
                    handleTagCallback(username, chatId, callbackData);
                } else {
                    WordLevel selectedLevel = WordLevel.valueOf(callbackData);
                    GameConfiguration gameConfiguration = gameService.getGameConfiguration(username);
                    gameConfiguration.setMaxLevel(selectedLevel);
                    restartGameAndNotify(username, chatId, gameConfiguration);
                }
            } else if (msg.isCommand()) {
                handleCommand(msg);
            } else {
                handleAnswer(msg);
            }
        } catch (TelegramApiException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void handleTagCallback(String username, String chatId, String callbackData)
            throws TelegramApiException {
        GameConfiguration gameConfiguration = gameService.getGameConfiguration(username);

        if (TAG_CLEAR_CALLBACK.equals(callbackData)) {
            gameConfiguration.setTags(null);
            restartGameAndNotify(username, chatId, gameConfiguration);

        } else if (TAG_APPLY_CALLBACK.equals(callbackData)) {
            restartGameAndNotify(username, chatId, gameConfiguration);

        } else {
            String selectedTag = callbackData.substring(TAG_CALLBACK_PREFIX.length());
            List<String> current = gameConfiguration.getTags();
            List<String> updated = new ArrayList<>(current == null ? List.of() : current);

            if (updated.contains(selectedTag)) {
                updated.remove(selectedTag);
            } else {
                updated.add(selectedTag);
            }

            gameConfiguration.setTags(updated.isEmpty() ? null : updated);

            gameService.saveGameConfiguration(username, gameConfiguration);

            // Refresh the keyboard in-place so checkmarks update
            List<String> activeTags = gameConfiguration.getTags();
            telegramClient.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("Choose topic tags (tap to toggle, then Apply):")
                    .replyMarkup(buildTagKeyboard(activeTags))
                    .build());
        }
    }

    private void handleAnswer(Message msg) throws TelegramApiException {
        String username = getOrCreate(msg.getFrom());
        Long chatId = msg.getChatId();

        SendMessageBuilder smb = SendMessage.builder()
                .chatId(chatId.toString())
                .replyToMessageId(msg.getMessageId())
                .parseMode("MarkdownV2");

        StringBuilder sb = new StringBuilder();
        boolean gameFinished;

        try {
            WordCard wordCard = gameService.checkUserAnswer(username, msg.getText());

            AnswerMessage answerMsg = wordCard.getAnswerMsg();

            sb.append(answerMsg.isCorrect()
                    ? answerMsg.getRightResultTokenTg()
                    : answerMsg.getWrongResultTokenTg() + answerMsg.getRightAnswerTokenTg());
            sb.append("\n\n");
            sb.append(answerMsg.getHints());
            sb.append(answerMsg.getUsageExampleTg());

            // Wrong answers get re-queued, so an empty queue means every word was
            // answered correctly, i.e. the game just finished.
            gameFinished = gameService.getGameWords(username).isEmpty();
        } catch (GameNotStartedException e) {
            sb.append("Game was inactive\\. I've started new one\\.");
            gameFinished = true;
        }

        if (gameFinished) {
            // Send the answer feedback on its own, then run the full restart notification
            // (banner + words preview when enabled + first task of the new game).
            telegramClient.execute(smb.text(escaped(sb.toString())).build());
            restartGameAndNotify(username, chatId.toString(), null);
        } else {
            sb.append("\n\n");
            WordCard nextTask = gameService.getNextTask(username);
            sb.append(nextTask.getTaskMsg().getTaskTextTg());
            telegramClient.execute(smb.text(escaped(sb.toString())).build());
        }
    }

    private void handleCommand(Message msg) {
        String username = getOrCreate(msg.getFrom());
        Long chatId = msg.getChatId();

        try {
            switch (msg.getText()) {
                case "/start":
                case "/restart":
                    restartGameAndNotify(username, chatId.toString(), null);
                    break;

                case "/setwords":
                    sendSpecificWordsButton(chatId.toString());
                    break;

                case "/clearwords":
                    GameConfiguration wordsCfg = gameService.getGameConfiguration(username);
                    wordsCfg.setSpecificWordIds(null);
                    restartGameAndNotify(username, chatId.toString(), wordsCfg);
                    break;

                case "/setlevel":
                    telegramClient.execute(SendMessage.builder()
                            .chatId(chatId.toString())
                            .text("Choose max level of words:")
                            .replyMarkup(buildLevelKeyboard())
                            .build());
                    break;

                case "/settag":
                    List<String> currentTags = gameService.getGameConfiguration(username).getTags();
                    telegramClient.execute(SendMessage.builder()
                            .chatId(chatId.toString())
                            .text("Choose topic tags (tap to toggle, then Apply):")
                            .replyMarkup(buildTagKeyboard(currentTags))
                            .build());
                    break;

                case "/cleartag":
                    GameConfiguration cfg = gameService.getGameConfiguration(username);
                    cfg.setTags(null);
                    restartGameAndNotify(username, chatId.toString(), cfg);
                    break;

                case "/switchpreview":
                    GameConfiguration previewCfg = gameService.getGameConfiguration(username);
                    boolean enabled = !previewCfg.getShowWordsPreview();
                    previewCfg.setShowWordsPreview(enabled);
                    gameService.saveGameConfiguration(username, previewCfg);
                    telegramClient.execute(SendMessage.builder()
                            .chatId(chatId.toString())
                            .text("Word list preview " + (enabled ? "enabled " + OK_ICON : "disabled " + CROSS_ICON))
                            .build());
                    break;
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String getOrCreate(User telegramUser) {
        String userName = telegramUser.getUserName();

        if (usersCache.contains(userName)) {
            return userName;
        }

        if (!userService.userExists(userName)) {
            var user = com.kniazev.cards.word.db.model.User.builder()
                    .username(telegramUser.getUserName())
                    .password("")
                    .roles(List.of("ALL", "PLAYER", "LEARNER"))
                    .enabled(true)
                    .build();
            user.getUserSettings().setUser(user);
            userService.save(user);
        }

        usersCache.add(userName);
        return userName;
    }

    private void setBotCommands() {
        List<BotCommand> commandList = new ArrayList<>();
        commandList.add(new BotCommand("/start",    "Start work with bot"));
        commandList.add(new BotCommand("/restart",  "Restart game"));
        commandList.add(new BotCommand("/setwords", "Choose specific words to study (Mini App)"));
        commandList.add(new BotCommand("/clearwords", "Play all words again (clear specific words)"));
        commandList.add(new BotCommand("/setlevel", "Set max word level"));
        commandList.add(new BotCommand("/settag",   "Filter words by topic tag"));
        commandList.add(new BotCommand("/cleartag", "Remove tag filter (all words)"));
        commandList.add(new BotCommand("/switchpreview", "Toggle word list preview on game start"));

        try {
            telegramClient.execute(new SetMyCommands(commandList, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendSpecificWordsButton(String chatId) throws TelegramApiException {
        if (miniAppUrl == null || miniAppUrl.isBlank()) {
            telegramClient.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("The word picker is not configured yet.")
                    .build());
            return;
        }

        telegramClient.execute(SendMessage.builder()
                .chatId(chatId)
                .text("Tap to choose specific words to study (the game will then use only those):")
                .replyMarkup(InlineKeyboardMarkup.builder()
                        .keyboard(List.of(new InlineKeyboardRow(
                                InlineKeyboardButton.builder()
                                        .text(MSG_CHOOSE_WORDS)
                                        .webApp(new WebAppInfo(miniAppUrl))
                                        .build())))
                        .build())
                .build());
    }

    private InlineKeyboardMarkup buildLevelKeyboard() {
        InlineKeyboardButton a1 = InlineKeyboardButton.builder().text("A1").callbackData("A1").build();
        InlineKeyboardButton a2 = InlineKeyboardButton.builder().text("A2").callbackData("A2").build();
        InlineKeyboardButton b1 = InlineKeyboardButton.builder().text("B1").callbackData("B1").build();
        InlineKeyboardButton b2 = InlineKeyboardButton.builder().text("B2").callbackData("B2").build();
        InlineKeyboardButton c1 = InlineKeyboardButton.builder().text("C1").callbackData("C1").build();
        InlineKeyboardButton c2 = InlineKeyboardButton.builder().text("C2").callbackData("C2").build();

        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        new InlineKeyboardRow(a1, a2),
                        new InlineKeyboardRow(b1, b2),
                        new InlineKeyboardRow(c1, c2)
                ))
                .build();
    }

    private InlineKeyboardMarkup buildTagKeyboard(List<String> activeTags) {
        List<String> allTags = gameService.findAllTags();
        Set<String>  active  = activeTags == null ? Set.of() : new HashSet<>(activeTags);

        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(new InlineKeyboardRow(
                InlineKeyboardButton.builder()
                        .text("Clear all")
                        .callbackData(TAG_CLEAR_CALLBACK)
                        .build()
        ));

        InlineKeyboardRow currentRow = new InlineKeyboardRow();
        for (String tag : allTags) {
            String label = active.contains(tag) ? TextPatterns.OK_ICON + " " + tag : tag;
            currentRow.add(InlineKeyboardButton.builder()
                    .text(label)
                    .callbackData(TAG_CALLBACK_PREFIX + tag)
                    .build());

            if (currentRow.size() == TAG_COLUMNS) {
                rows.add(currentRow);
                currentRow = new InlineKeyboardRow();
            }
        }

        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }

        String applyLabel = active.isEmpty() ? "Apply " + OK_ARROW + " Text (all words)" : "Apply " + OK_ARROW + "(" + active.size() + " tag(s))";
        rows.add(new InlineKeyboardRow(
                InlineKeyboardButton.builder()
                        .text(applyLabel)
                        .callbackData(TAG_APPLY_CALLBACK)
                        .build()
        ));

        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }

    private void restartGameAndNotify(String username, String chatId, GameConfiguration gameConfiguration)
            throws TelegramApiException {
        gameService.restartGame(username, gameConfiguration);

        WordCard nextTask = gameService.getNextTask(username);
        GameConfiguration actual = gameService.getGameConfiguration(username);

        List<Long> specificWordIds = actual.getSpecificWordIds();
        String banner;
        if (specificWordIds != null && !specificWordIds.isEmpty()) {
            banner = String.format("Game restarted [specific words: %d]", specificWordIds.size());
        } else {
            List<String> activeTags = actual.getTags();
            String tagInfo = (activeTags == null || activeTags.isEmpty())
                    ? "all words"
                    : "tag: " + String.join(", ", activeTags);
            banner = String.format("Game restarted [level: %s, %s]", actual.getMaxLevel().name(), tagInfo);
        }

        telegramClient.execute(SendMessage.builder()
                .chatId(chatId)
                .text(banner)
                .build());

        if (actual.getShowWordsPreview()) {
            List<WordCard> previewWords = gameService.getGameWords(username).stream()
                    .filter(WordCard::isNewWord)
                    .toList();

            if (!previewWords.isEmpty()) {
                StringBuilder wordsListBuilder = new StringBuilder();
                wordsListBuilder.append("Neues Spiel. Nächste Wortgruppe zum Lernen:\n\n");
                for (WordCard card : previewWords) {
                    Word word = card.getWord();
                    wordsListBuilder.append(TextPatterns.GER_FLAG + "*").append(word.getDe()).append("* | ");
                    wordsListBuilder.append(TextPatterns.RUS_FLAG + "*").append(word.getRu()).append("* \n");
                    AnswerMessage msg = new AnswerMessage(word, false, null, null, null);
                    wordsListBuilder.append(msg.getHints()).append("\n\n");
                }

                telegramClient.execute(SendMessage.builder()
                        .chatId(chatId)
                        .parseMode("MarkdownV2")
                        .text(escaped(wordsListBuilder.toString()))
                        .build());
            }
        }

        telegramClient.execute(SendMessage.builder()
                .chatId(chatId)
                .parseMode("MarkdownV2")
                .text(escaped(nextTask.getTaskText(TaskTextFormat.TELEGRAM)))
                .build());
    }

    private static String escaped(String text) {
        return text
//                .replace("\\", "\\\\")
                .replace("_", "\\_")
//                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
//                .replace("~", "\\~")
//                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }
}
