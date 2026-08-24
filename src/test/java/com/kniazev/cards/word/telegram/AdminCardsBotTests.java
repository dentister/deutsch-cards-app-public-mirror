package com.kniazev.cards.word.telegram;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.kniazev.cards.word.db.service.UserService;
import com.kniazev.cards.word.db.service.WordService;

import org.springframework.dao.DataIntegrityViolationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AdminCardsBotTests {

    private static final String ADMIN_USERNAME = "mr_dentister";
    private static final String ADMIN_USERNAME_2 = "second_admin";
    private static final Long CHAT_ID = 100L;
    private static final Long CHAT_ID_2 = 200L;

    @Mock
    private TelegramClient telegramClient;
    @Mock
    private UserService userService;
    @Mock
    private WordDraftService wordDraftService;
    @Mock
    private WordService wordService;

    private AdminCardsBot bot;

    @BeforeEach
    void setUp() {
        DraftReviewPresenter presenter = new DraftReviewPresenter(new ObjectMapper());
        bot = new AdminCardsBot("fake-token", telegramClient, userService, wordDraftService, wordService, presenter);
    }

    @Test
    void ignoresMessagesFromNonAdminUsernames() throws Exception {
        when(userService.findOneByUsername("someone_else")).thenReturn(Optional.empty());

        bot.consume(textUpdate("someone_else", "Добавь überkommen"));

        verifyNoInteractions(wordDraftService);
        verify(telegramClient, never()).execute(any(BotApiMethod.class));
    }

    @Test
    void allowListIsCaseInsensitive() {
        WordDraft draft = phraseDraft();
        when(userService.findOneByUsername("MR_DENTISTER")).thenReturn(Optional.of(adminUser("MR_DENTISTER")));
        when(wordDraftService.generateDraft("überkommen")).thenReturn(Optional.of(draft));
        when(wordService.findOneByDeAndWordType(draft.de(), draft.wordType())).thenReturn(Optional.empty());

        bot.consume(textUpdate("MR_DENTISTER", "überkommen"));

        verify(wordDraftService).generateDraft("überkommen");
    }

    @Test
    void recheckThenTextRevisesDraftInsteadOfStartingFresh() {
        WordDraft original = phraseDraft();
        WordDraft revised = new WordDraft(WordType.PHRASE, "auf jeden Fall", "в любом случае, точно", null,
                WordLevel.B1, null, null, null, null, null, null, null, null, null, null, null, null,
                "Er kommt auf jeden Fall.", "Он точно придёт.", null, null);

        when(userService.findOneByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser(ADMIN_USERNAME)));
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
        when(userService.findOneByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser(ADMIN_USERNAME)));
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
        when(userService.findOneByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser(ADMIN_USERNAME)));
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
        WordDraft incomplete = new WordDraft(WordType.NOUN, "Tisch", "стол", null, WordLevel.A1,
                null, null, null, null, null, null, null, null, null, null, null, null,
                "Sample.", "Пример.", null, null);
        when(userService.findOneByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser(ADMIN_USERNAME)));
        when(wordDraftService.generateDraft("Tisch")).thenReturn(Optional.of(incomplete));
        when(wordService.findOneByDeAndWordType(incomplete.de(), incomplete.wordType())).thenReturn(Optional.empty());

        bot.consume(textUpdate(ADMIN_USERNAME, "Tisch"));
        bot.consume(callbackUpdate(ADMIN_USERNAME, DraftReviewPresenter.CALLBACK_ACCEPT, 1));

        verify(wordService, never()).createOrRewrite(any(Word.class));
    }

    @Test
    void twoAdminsInDifferentChatsDraftDifferentWordsIndependently() {
        // Regression test: sessions used to be shared bot-wide, so admin 2's draft would
        // silently clobber admin 1's in-flight draft the moment admin 2 sent any text.
        WordDraft draft1 = phraseDraft();
        WordDraft draft2 = new WordDraft(WordType.PHRASE, "guten Morgen", "доброе утро", null, WordLevel.A1,
                null, null, null, null, null, null, null, null, null, null, null, null,
                "Er sagt guten Morgen.", "Он говорит доброе утро.", null, null);

        when(userService.findOneByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser(ADMIN_USERNAME)));
        when(userService.findOneByUsername(ADMIN_USERNAME_2)).thenReturn(Optional.of(adminUser(ADMIN_USERNAME_2)));
        when(wordDraftService.generateDraft("auf jeden Fall")).thenReturn(Optional.of(draft1));
        when(wordDraftService.generateDraft("guten Morgen")).thenReturn(Optional.of(draft2));
        when(wordService.findOneByDeAndWordType(draft1.de(), draft1.wordType())).thenReturn(Optional.empty());
        when(wordService.findOneByDeAndWordType(draft2.de(), draft2.wordType())).thenReturn(Optional.empty());
        when(wordService.createOrRewrite(any(Word.class))).thenAnswer(inv -> {
            Word w = inv.getArgument(0);
            w.setId(42L);
            return w;
        });

        bot.consume(textUpdate(ADMIN_USERNAME, CHAT_ID, "auf jeden Fall"));
        bot.consume(textUpdate(ADMIN_USERNAME_2, CHAT_ID_2, "guten Morgen"));
        bot.consume(callbackUpdate(ADMIN_USERNAME, CHAT_ID, DraftReviewPresenter.CALLBACK_ACCEPT, 1));

        ArgumentCaptor<Word> savedCaptor = ArgumentCaptor.forClass(Word.class);
        verify(wordService).createOrRewrite(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getDe()).isEqualTo(draft1.de());
    }

    @Test
    void secondAdminDraftingSameWordSeesCollisionWarning() throws Exception {
        WordDraft draft = phraseDraft();
        when(userService.findOneByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser(ADMIN_USERNAME)));
        when(userService.findOneByUsername(ADMIN_USERNAME_2)).thenReturn(Optional.of(adminUser(ADMIN_USERNAME_2)));
        when(wordDraftService.generateDraft("auf jeden Fall")).thenReturn(Optional.of(draft));
        when(wordService.findOneByDeAndWordType(draft.de(), draft.wordType())).thenReturn(Optional.empty());

        bot.consume(textUpdate(ADMIN_USERNAME, CHAT_ID, "auf jeden Fall"));
        bot.consume(textUpdate(ADMIN_USERNAME_2, CHAT_ID_2, "auf jeden Fall"));

        ArgumentCaptor<BotApiMethod> sentMessages = ArgumentCaptor.forClass(BotApiMethod.class);
        verify(telegramClient, times(2)).execute(sentMessages.capture());

        SendMessage secondAdminMessage = (SendMessage) sentMessages.getAllValues().get(1);
        assertThat(secondAdminMessage.getText()).contains("@" + ADMIN_USERNAME);
    }

    private static WordDraft phraseDraft() {
        return new WordDraft(WordType.PHRASE, "auf jeden Fall", "в любом случае", null, WordLevel.B1,
                null, null, null, null, null, null, null, null, null, null, null, null,
                "Er kommt auf jeden Fall.", "Он в любом случае придёт.", null, null);
    }

    private static Update textUpdate(String username, String text) {
        return textUpdate(username, CHAT_ID, text);
    }

    private static Update textUpdate(String username, Long chatId, String text) {
        Update update = new Update();
        Message message = new Message();
        message.setFrom(telegramUser(username));
        message.setChat(new Chat(chatId, "private"));
        message.setMessageId(1);
        message.setText(text);
        update.setMessage(message);
        return update;
    }

    private static Update callbackUpdate(String username, String data, int messageId) {
        return callbackUpdate(username, CHAT_ID, data, messageId);
    }

    private static Update callbackUpdate(String username, Long chatId, String data, int messageId) {
        Update update = new Update();
        Message message = new Message();
        message.setChat(new Chat(chatId, "private"));
        message.setMessageId(messageId);
        CallbackQuery cq = new CallbackQuery();
        cq.setId("cbq-1");
        cq.setFrom(telegramUser(username));
        cq.setData(data);
        cq.setMessage(message);
        update.setCallbackQuery(cq);
        return update;
    }

    private static com.kniazev.cards.word.db.model.User adminUser(String username) {
        return com.kniazev.cards.word.db.model.User.builder()
                .username(username)
                .roles(List.of("ADMIN"))
                .build();
    }

    private static User telegramUser(String username) {
        User user = new User(1L, "Test", false);
        user.setUserName(username);
        return user;
    }
}
