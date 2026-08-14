package com.kniazev.cards.word.telegram;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kniazev.cards.word.ai.WordDraft;
import com.kniazev.cards.word.ai.WordDraftService;
import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.db.model.word.Word.WordLevel;
import com.kniazev.cards.word.db.model.word.Word.WordType;
import com.kniazev.cards.word.db.services.WordService;

import org.springframework.dao.DataIntegrityViolationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AdminCardsBotTests {

    private static final String ADMIN_USERNAME = "mr_dentister";
    private static final Long CHAT_ID = 100L;

    @Mock
    private TelegramClient telegramClient;
    @Mock
    private WordDraftService wordDraftService;
    @Mock
    private WordService wordService;

    private AdminCardsBot bot;

    @BeforeEach
    void setUp() {
        DraftReviewPresenter presenter = new DraftReviewPresenter(new ObjectMapper());
        bot = new AdminCardsBot("fake-token", ADMIN_USERNAME, telegramClient, wordDraftService, wordService, presenter);
    }

    @Test
    void ignoresMessagesFromNonAdminUsernames() throws Exception {
        bot.consume(textUpdate("someone_else", "Добавь überkommen"));

        verifyNoInteractions(wordDraftService);
        verify(telegramClient, never()).execute(any(BotApiMethod.class));
    }

    @Test
    void allowListIsCaseInsensitive() {
        WordDraft draft = phraseDraft();
        when(wordDraftService.generateDraft("überkommen")).thenReturn(Optional.of(draft));
        when(wordService.findOneByDeAndWordType(draft.de(), draft.wordType())).thenReturn(Optional.empty());

        bot.consume(textUpdate("MR_DENTISTER", "überkommen"));

        verify(wordDraftService).generateDraft("überkommen");
    }

    @Test
    void recheckThenTextRevisesDraftInsteadOfStartingFresh() {
        WordDraft original = phraseDraft();
        WordDraft revised = new WordDraft(WordType.PHRASE, "auf jeden Fall", "в любом случае, точно",
                WordLevel.B1, null, null, null, null, null, null, null, null, null, null, null, null,
                "Er kommt auf jeden Fall.", "Он точно придёт.", null);

        when(wordDraftService.generateDraft("auf jeden Fall")).thenReturn(Optional.of(original));
        when(wordService.findOneByDeAndWordType(any(), any())).thenReturn(Optional.empty());
        when(wordDraftService.reviseDraft(original, "fix the translation")).thenReturn(Optional.of(revised));

        bot.consume(textUpdate(ADMIN_USERNAME, "auf jeden Fall"));
        bot.consume(callbackUpdate(ADMIN_USERNAME, DraftReviewPresenter.CALLBACK_RECHECK, 1));
        bot.consume(textUpdate(ADMIN_USERNAME, "fix the translation"));

        verify(wordDraftService).reviseDraft(original, "fix the translation");
        verify(wordDraftService, never()).generateDraft("fix the translation");
    }

    @Test
    void secondAcceptOnAlreadyResolvedDraftIsNoOp() {
        WordDraft draft = phraseDraft();
        when(wordDraftService.generateDraft("auf jeden Fall")).thenReturn(Optional.of(draft));
        when(wordService.findOneByDeAndWordType(draft.de(), draft.wordType())).thenReturn(Optional.empty());
        when(wordService.createOrRewrite(any(Word.class))).thenAnswer(inv -> {
            Word w = inv.getArgument(0);
            w.setId(42L);
            return w;
        });

        bot.consume(textUpdate(ADMIN_USERNAME, "auf jeden Fall"));
        bot.consume(callbackUpdate(ADMIN_USERNAME, DraftReviewPresenter.CALLBACK_ACCEPT, 1));
        bot.consume(callbackUpdate(ADMIN_USERNAME, DraftReviewPresenter.CALLBACK_ACCEPT, 1));

        verify(wordService, times(1)).createOrRewrite(any(Word.class));
    }

    @Test
    void acceptWithDbFailureReportsErrorAndKeepsDraftAliveForRetry() {
        // Regression test for a real crash: a DataIntegrityViolationException (e.g. a value too
        // long for its column) escaping createOrRewrite must not silently kill the
        // update-consumer thread - the admin needs to see it and be able to retry.
        WordDraft draft = phraseDraft();
        when(wordDraftService.generateDraft("auf jeden Fall")).thenReturn(Optional.of(draft));
        when(wordService.findOneByDeAndWordType(draft.de(), draft.wordType())).thenReturn(Optional.empty());
        when(wordService.createOrRewrite(any(Word.class)))
                .thenThrow(new DataIntegrityViolationException("value too long for type character varying(50)"))
                .thenAnswer(inv -> {
                    Word w = inv.getArgument(0);
                    w.setId(42L);
                    return w;
                });

        bot.consume(textUpdate(ADMIN_USERNAME, "auf jeden Fall"));
        bot.consume(callbackUpdate(ADMIN_USERNAME, DraftReviewPresenter.CALLBACK_ACCEPT, 1));
        // Draft must still be alive after the failed save - a second Accept retries instead of no-op'ing.
        bot.consume(callbackUpdate(ADMIN_USERNAME, DraftReviewPresenter.CALLBACK_ACCEPT, 1));

        verify(wordService, times(2)).createOrRewrite(any(Word.class));
    }

    @Test
    void acceptWithHardValidationErrorsDoesNotSaveAndKeepsDraftAlive() {
        // NOUN drafted without a gender is a hard error per WordDraftMapper.validate().
        WordDraft incomplete = new WordDraft(WordType.NOUN, "Tisch", "стол", WordLevel.A1,
                null, null, null, null, null, null, null, null, null, null, null, null,
                "Sample.", "Пример.", null);
        when(wordDraftService.generateDraft("Tisch")).thenReturn(Optional.of(incomplete));
        when(wordService.findOneByDeAndWordType(incomplete.de(), incomplete.wordType())).thenReturn(Optional.empty());

        bot.consume(textUpdate(ADMIN_USERNAME, "Tisch"));
        bot.consume(callbackUpdate(ADMIN_USERNAME, DraftReviewPresenter.CALLBACK_ACCEPT, 1));

        verify(wordService, never()).createOrRewrite(any(Word.class));
    }

    private static WordDraft phraseDraft() {
        return new WordDraft(WordType.PHRASE, "auf jeden Fall", "в любом случае", WordLevel.B1,
                null, null, null, null, null, null, null, null, null, null, null, null,
                "Er kommt auf jeden Fall.", "Он в любом случае придёт.", null);
    }

    private static Update textUpdate(String username, String text) {
        Update update = new Update();
        Message message = new Message();
        message.setFrom(telegramUser(username));
        message.setChat(new Chat(CHAT_ID, "private"));
        message.setMessageId(1);
        message.setText(text);
        update.setMessage(message);
        return update;
    }

    private static Update callbackUpdate(String username, String data, int messageId) {
        Update update = new Update();
        Message message = new Message();
        message.setChat(new Chat(CHAT_ID, "private"));
        message.setMessageId(messageId);
        CallbackQuery cq = new CallbackQuery();
        cq.setId("cbq-1");
        cq.setFrom(telegramUser(username));
        cq.setData(data);
        cq.setMessage(message);
        update.setCallbackQuery(cq);
        return update;
    }

    private static User telegramUser(String username) {
        User user = new User(1L, "Test", false);
        user.setUserName(username);
        return user;
    }
}
