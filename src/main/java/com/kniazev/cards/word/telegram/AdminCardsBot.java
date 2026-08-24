package com.kniazev.cards.word.telegram;

import com.kniazev.cards.word.ai.WordDraft;
import com.kniazev.cards.word.ai.WordDraftMapper;
import com.kniazev.cards.word.ai.WordDraftMapper.ValidationResult;
import com.kniazev.cards.word.ai.WordDraftService;
import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.db.service.UserService;
import com.kniazev.cards.word.db.service.WordService;

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
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

import static com.kniazev.cards.word.constant.Emoji.*;

/**
 * Admin-only bot that turns free text into a word card via AI and confirms it with an
 * Accept / Reject / Recheck loop before writing anything to the dictionary. Separate from
 * {@link GermanCardsBot} on purpose - regular users never see this bot. No moderation
 * queue: this is the admin's own direct-add path, so their review at Accept time is the
 * approval. Any number of admins (users with the ADMIN role) can use this bot at once, each
 * with their own in-flight draft tracked per Telegram chat - see {@link AdminSession}. Nothing
 * is persisted until Accept.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "admin.bot.enabled", havingValue = "true", matchIfMissing = true)
public class AdminCardsBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final String MSG_UPDATED_WORD = OK_ICON + " Updated existing word: ";
    private static final String MSG_ADDED_WORD = OK_ICON + " Added: ";
    private static final String MSG_DISCARDED = CROSS_ICON + " Discarded.";
    private static final String MSG_NOTHING_TO_RECHECK = "Nothing to recheck anymore (already resolved).";
    private static final String MSG_WHAT_TO_CORRECT = "What should be corrected?";
    private static final String MSG_NOTHING_TO_ACCEPT = "Nothing to accept anymore (already resolved).";

    private final String botToken;
    private final UserService userService;
    private final TelegramMessenger messenger;
    private final WordDraftService wordDraftService;
    private final WordService wordService;
    private final DraftReviewPresenter presenter;

    // One session per Telegram chat, so each admin's in-flight draft is independent of every
    // other admin's. consume(Update) is delivered sequentially
    // (LongPollingSingleThreadUpdateConsumer), so plain mutable session fields are safe, but
    // they're marked volatile anyway for visibility if that ever changes; the map itself is
    // concurrent for the same reason.
    private final Map<Long, AdminSession> sessions = new ConcurrentHashMap<>();

    private static final class AdminSession {
        private volatile WordDraft draft;
        private volatile boolean awaitingCorrection;
        private volatile String adminUsername;
    }

    public AdminCardsBot(@Value("${admin.bot.token}") String botToken,
                          @Qualifier("adminCardsBotTelegramClient") TelegramClient telegramClient,
                          UserService userService,
                          WordDraftService wordDraftService,
                          WordService wordService,
                          DraftReviewPresenter presenter) {
        this.botToken = botToken;
        this.userService = userService;
        this.messenger = new TelegramMessenger(telegramClient);
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
                    AdminSession session = sessionFor(cq.getMessage().getChatId());
                    
                    session.adminUsername = cq.getFrom().getUserName();
                    
                    handleCallback(cq, session);
                }
            } else if (update.getMessage() != null && update.getMessage().hasText()) {
                Message msg = update.getMessage();

                if (isAllowed(msg.getFrom())) {
                    AdminSession session = sessionFor(msg.getChatId());
                    
                    session.adminUsername = msg.getFrom().getUserName();

                    if (msg.isCommand()) {
                        handleCommand(msg, session);
                    } else {
                        handleText(msg, session);
                    }
                }
            }
        } catch (TelegramApiException e) {
            log.error("Telegram API call failed: {}", e.getMessage(), e);
        }
    }

    private AdminSession sessionFor(Long chatId) {
        return sessions.computeIfAbsent(chatId, id -> new AdminSession());
    }

    private boolean isAllowed(User from) {
        String username = from == null ? null : from.getUserName();
        boolean allowed = username != null && userService.findOneByUsername(username)
                .map(u -> u.getRoles() != null && u.getRoles().contains("ADMIN"))
                .orElse(false);

        if (!allowed) {
            log.info("Ignoring update from non-admin user [{}]", username);
        }

        return allowed;
    }

    private void handleCommand(Message msg, AdminSession session) throws TelegramApiException {
        session.draft = null;
        session.awaitingCorrection = false;

        messenger.send(msg.getChatId().toString(), "Send a German word or phrase to draft a card, e.g. \"überkommen\" or \"überkommen преодолеть\".");
    }

    private void handleText(Message msg, AdminSession session) throws TelegramApiException {
        String chatId = msg.getChatId().toString();

        // Recheck mode
        if (session.awaitingCorrection && session.draft != null) {
            WordDraft previous = session.draft;
            session.awaitingCorrection = false;
            Optional<WordDraft> revised = wordDraftService.reviseDraft(previous, msg.getText());

            if (revised.isEmpty()) {
                messenger.send(chatId, "AI request failed - tap Recheck to try again.");
                return;
            }

            session.draft = revised.get();
            sendDraft(chatId, session);

            return;
        }

        session.awaitingCorrection = false;
        Optional<WordDraft> draft = wordDraftService.generateDraft(msg.getText());

        if (draft.isEmpty()) {
            messenger.send(chatId, "AI request failed, please try again.");
            return;
        }

        session.draft = draft.get();
        sendDraft(chatId, session);
    }

    private void sendDraft(String chatId, AdminSession session) throws TelegramApiException {
        WordDraft draft = session.draft;
        ValidationResult validation = WordDraftMapper.validate(draft);

        List<String> warnings = new ArrayList<>(validation.warnings());

        wordService.findOneByDeAndWordType(draft.de(), draft.wordType())
                .ifPresent(existing -> warnings.add(
                        "A word with this German text + type already exists (id=" + existing.getId()
                                + ") - Accept will update it, not create a new one."));

        sessions.values().stream()
                .filter(other -> other != session && other.draft != null)
                .filter(other -> other.draft.wordType() == draft.wordType()
                        && other.draft.de().equalsIgnoreCase(draft.de()))
                .findFirst()
                .ifPresent(other -> warnings.add(WARNING_ICON
                        + " Admin @" + other.adminUsername + " is also currently drafting this word - "
                        + "accepting may conflict with their changes."));

        DraftReviewPresenter.Rendered rendered = presenter.render(draft, validation.errors(), warnings);

        messenger.send(chatId, rendered.text(), rendered.keyboard());
    }

    private void handleCallback(CallbackQuery cq, AdminSession session) throws TelegramApiException {
        String chatId = cq.getMessage().getChatId().toString();
        Integer messageId = cq.getMessage().getMessageId();

        messenger.answerCallback(cq.getId());

        switch (cq.getData()) {
            case DraftReviewPresenter.CALLBACK_ACCEPT -> handleAccept(chatId, messageId, session);
            case DraftReviewPresenter.CALLBACK_REJECT -> handleReject(chatId, messageId, session);
            case DraftReviewPresenter.CALLBACK_RECHECK -> handleRecheck(chatId, messageId, session);
            default -> log.warn("Unknown callback data [{}]", cq.getData());
        }
    }

    private void handleAccept(String chatId, Integer messageId, AdminSession session) throws TelegramApiException {
        if (session.draft == null) {
            messenger.send(chatId, MSG_NOTHING_TO_ACCEPT);
            return;
        }

        ValidationResult validation = WordDraftMapper.validate(session.draft);
        if (validation.hasErrors()) {
            messenger.send(chatId, "Can't accept yet:\n" + String.join("\n", validation.errors())
                    + "\n\nTap Recheck to fix it, or Reject.");
            return;
        }

        Word saved;
        boolean existedBefore;
        try {
            existedBefore = wordService.findOneByDeAndWordType(session.draft.de(), session.draft.wordType()).isPresent();

            Word toSave = WordDraftMapper.toEntity(session.draft);
            if (!existedBefore) {
                toSave.setCreatedBy(session.adminUsername);
            }

            saved = wordService.createOrRewrite(toSave);
        } catch (RuntimeException e) {
            log.error("Failed to save word draft [{}]: {}", session.draft.de(), e.getMessage(), e);

            messenger.send(chatId, "Save failed: " + e.getMessage() + "\n\nTap Recheck to fix it, or Reject.");

            return;
        }

        session.draft = null;
        session.awaitingCorrection = false;

        messenger.removeKeyboard(chatId, messageId);
        messenger.send(chatId,
                (existedBefore ? MSG_UPDATED_WORD : MSG_ADDED_WORD)
                + saved.getDe() + " (id=" + saved.getId() + ")");
    }

    private void handleReject(String chatId, Integer messageId, AdminSession session) throws TelegramApiException {
        session.draft = null;
        session.awaitingCorrection = false;

        messenger.removeKeyboard(chatId, messageId);
        messenger.send(chatId, MSG_DISCARDED);
    }

    private void handleRecheck(String chatId, Integer messageId, AdminSession session) throws TelegramApiException {
        if (session.draft == null) {
            messenger.send(chatId, MSG_NOTHING_TO_RECHECK);
            return;
        }

        session.awaitingCorrection = true;
        messenger.removeKeyboard(chatId, messageId);
        messenger.send(chatId, MSG_WHAT_TO_CORRECT);
    }
}
