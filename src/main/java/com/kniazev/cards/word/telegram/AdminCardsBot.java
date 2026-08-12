package com.kniazev.cards.word.telegram;

import com.kniazev.cards.word.ai.WordDraft;
import com.kniazev.cards.word.ai.WordDraftMapper;
import com.kniazev.cards.word.ai.WordDraftMapper.ValidationResult;
import com.kniazev.cards.word.ai.WordDraftService;
import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.db.services.WordService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

/**
 * Admin-only bot that turns free text into a word card via AI and confirms it with an
 * Accept / Reject / Recheck loop before writing anything to the dictionary. Separate from
 * {@link GermanCardsBot} on purpose - regular users never see this bot. No moderation
 * queue: this is the admin's own direct-add path, so their review at Accept time is the
 * approval. Only one draft is ever "in flight", held in memory - nothing is persisted
 * until Accept.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "admin.bot.enabled", havingValue = "true", matchIfMissing = true)
public class AdminCardsBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final String botToken;
    private final String allowedUsername;
    private final TelegramClient telegramClient;
    private final WordDraftService wordDraftService;
    private final WordService wordService;
    private final DraftReviewPresenter presenter;

    // Single-admin, single-draft-at-a-time: consume(Update) is delivered sequentially
    // (LongPollingSingleThreadUpdateConsumer), so plain mutable state is safe, but these are
    // marked volatile anyway for visibility if that ever changes.
    private volatile WordDraft currentDraft;
    private volatile boolean awaitingCorrection;

    @Autowired
    public AdminCardsBot(@Value("${admin.bot.token}") String botToken,
                          @Value("${admin.bot.allowed-username}") String allowedUsername,
                          WordDraftService wordDraftService,
                          WordService wordService,
                          DraftReviewPresenter presenter) {
        this(botToken, allowedUsername, new OkHttpTelegramClient(botToken), wordDraftService, wordService, presenter);
    }

    // Package-private: lets tests inject a mock TelegramClient instead of hitting the network.
    AdminCardsBot(String botToken, String allowedUsername, TelegramClient telegramClient,
                  WordDraftService wordDraftService, WordService wordService, DraftReviewPresenter presenter) {
        this.botToken = botToken;
        this.allowedUsername = allowedUsername;
        this.telegramClient = telegramClient;
        this.wordDraftService = wordDraftService;
        this.wordService = wordService;
        this.presenter = presenter;
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
        try {
            if (update.hasCallbackQuery()) {
                CallbackQuery cq = update.getCallbackQuery();
                if (isAllowed(cq.getFrom())) {
                    handleCallback(cq);
                }
            } else if (update.getMessage() != null && update.getMessage().hasText()) {
                Message msg = update.getMessage();
                if (isAllowed(msg.getFrom())) {
                    if (msg.isCommand()) {
                        handleCommand(msg);
                    } else {
                        handleText(msg);
                    }
                }
            }
        } catch (TelegramApiException e) {
            log.error("Telegram API call failed: {}", e.getMessage(), e);
        }
    }

    private boolean isAllowed(User from) {
        String username = from == null ? null : from.getUserName();
        boolean allowed = username != null && username.equalsIgnoreCase(allowedUsername);
        if (!allowed) {
            log.info("Ignoring update from non-admin user [{}]", username);
        }
        return allowed;
    }

    private void handleCommand(Message msg) throws TelegramApiException {
        currentDraft = null;
        awaitingCorrection = false;
        telegramClient.execute(SendMessage.builder()
                .chatId(msg.getChatId().toString())
                .text("Send a German word or phrase to draft a card, e.g. \"überkommen\" or \"überkommen преодолеть\".")
                .build());
    }

    private void handleText(Message msg) throws TelegramApiException {
        String chatId = msg.getChatId().toString();

        if (awaitingCorrection && currentDraft != null) {
            WordDraft previous = currentDraft;
            awaitingCorrection = false;
            Optional<WordDraft> revised = wordDraftService.reviseDraft(previous, msg.getText());
            if (revised.isEmpty()) {
                telegramClient.execute(SendMessage.builder().chatId(chatId)
                        .text("AI request failed - tap Recheck to try again.").build());
                return;
            }
            currentDraft = revised.get();
            sendDraft(chatId);
            return;
        }

        awaitingCorrection = false;
        Optional<WordDraft> draft = wordDraftService.generateDraft(msg.getText());
        if (draft.isEmpty()) {
            telegramClient.execute(SendMessage.builder().chatId(chatId)
                    .text("AI request failed, please try again.").build());
            return;
        }
        currentDraft = draft.get();
        sendDraft(chatId);
    }

    private void sendDraft(String chatId) throws TelegramApiException {
        ValidationResult validation = WordDraftMapper.validate(currentDraft);
        List<String> warnings = new ArrayList<>(validation.warnings());
        wordService.findOneByDeAndWordType(currentDraft.de(), currentDraft.wordType())
                .ifPresent(existing -> warnings.add(
                        "A word with this German text + type already exists (id=" + existing.getId()
                                + ") - Accept will update it, not create a new one."));

        DraftReviewPresenter.Rendered rendered = presenter.render(currentDraft, validation.errors(), warnings);
        telegramClient.execute(SendMessage.builder()
                .chatId(chatId)
                .text(rendered.text())
                .replyMarkup(rendered.keyboard())
                .build());
    }

    private void handleCallback(CallbackQuery cq) throws TelegramApiException {
        String chatId = cq.getMessage().getChatId().toString();
        Integer messageId = cq.getMessage().getMessageId();

        telegramClient.execute(AnswerCallbackQuery.builder().callbackQueryId(cq.getId()).build());

        switch (cq.getData()) {
            case DraftReviewPresenter.CALLBACK_ACCEPT -> handleAccept(chatId, messageId);
            case DraftReviewPresenter.CALLBACK_REJECT -> handleReject(chatId, messageId);
            case DraftReviewPresenter.CALLBACK_RECHECK -> handleRecheck(chatId, messageId);
            default -> log.warn("Unknown callback data [{}]", cq.getData());
        }
    }

    private void handleAccept(String chatId, Integer messageId) throws TelegramApiException {
        if (currentDraft == null) {
            telegramClient.execute(SendMessage.builder().chatId(chatId)
                    .text("Nothing to accept anymore (already resolved).").build());
            return;
        }

        ValidationResult validation = WordDraftMapper.validate(currentDraft);
        if (validation.hasErrors()) {
            telegramClient.execute(SendMessage.builder().chatId(chatId)
                    .text("Can't accept yet:\n" + String.join("\n", validation.errors())
                            + "\n\nTap Recheck to fix it, or Reject.").build());
            return;
        }

        Word saved;
        boolean existedBefore;
        try {
            existedBefore = wordService.findOneByDeAndWordType(currentDraft.de(), currentDraft.wordType()).isPresent();
            saved = wordService.createOrRewrite(WordDraftMapper.toEntity(currentDraft));
        } catch (RuntimeException e) {
            // Validation above catches the constraints we know about, but this is a real DB
            // call - anything unexpected here must not silently kill the update-consumer thread
            // with no feedback to the admin (that's exactly what happened before this guard).
            log.error("Failed to save word draft [{}]: {}", currentDraft.de(), e.getMessage(), e);
            telegramClient.execute(SendMessage.builder().chatId(chatId)
                    .text("Save failed: " + e.getMessage() + "\n\nTap Recheck to fix it, or Reject.").build());
            return;
        }
        currentDraft = null;
        awaitingCorrection = false;

        removeKeyboard(chatId, messageId);
        telegramClient.execute(SendMessage.builder().chatId(chatId)
                .text((existedBefore ? "✅ Updated existing word: " : "✅ Added: ")
                        + saved.getDe() + " (id=" + saved.getId() + ")")
                .build());
    }

    private void handleReject(String chatId, Integer messageId) throws TelegramApiException {
        currentDraft = null;
        awaitingCorrection = false;
        removeKeyboard(chatId, messageId);
        telegramClient.execute(SendMessage.builder().chatId(chatId).text("❌ Discarded.").build());
    }

    private void handleRecheck(String chatId, Integer messageId) throws TelegramApiException {
        if (currentDraft == null) {
            telegramClient.execute(SendMessage.builder().chatId(chatId)
                    .text("Nothing to recheck anymore (already resolved).").build());
            return;
        }
        awaitingCorrection = true;
        removeKeyboard(chatId, messageId);
        telegramClient.execute(SendMessage.builder().chatId(chatId).text("What should be corrected?").build());
    }

    private void removeKeyboard(String chatId, Integer messageId) throws TelegramApiException {
        telegramClient.execute(EditMessageReplyMarkup.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build());
    }
}
