package com.kniazev.cards.word.telegram.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kniazev.cards.word.ai.draft.WordDraft;
import com.kniazev.cards.word.ai.draft.WordDraftService;
import com.kniazev.cards.word.model.dictionary.Word;
import com.kniazev.cards.word.model.dictionary.Word.WordLevel;
import com.kniazev.cards.word.model.dictionary.Word.WordType;
import com.kniazev.cards.word.service.UserService;
import com.kniazev.cards.word.service.WordService;

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
    private static final long ADMIN_ID = 1001L;
    private static final long ADMIN_ID_2 = 1002L;
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
    void ignoresMessagesFromTelegramIdsWithoutAnAccount() throws Exception {
        when(userService.findOrBindByTelegramId(999L, null)).thenReturn(Optional.empty());

        bot.consume(textUpdate(999L, "Добавь überkommen"));

        verifyNoInteractions(wordDraftService);
        verify(telegramClient, never()).execute(any(BotApiMethod.class));
    }

    @Test
    void ignoresAnAccountThatIsNotAnAdmin() throws Exception {
        when(userService.findOrBindByTelegramId(999L, null)).thenReturn(Optional.of(playerUser("some_player", 999L)));

        bot.consume(textUpdate(999L, "Добавь überkommen"));

        verifyNoInteractions(wordDraftService);
        verify(telegramClient, never()).execute(any(BotApiMethod.class));
    }

    @Test
    void aStrangerHoldingTheAdminsUsernameIsIgnoredOnceTheAdminAccountBelongsToSomeoneElse() {
        // The admin row is already bound to the real admin's id, so the service does not hand it to the stranger.
        when(userService.findOrBindByTelegramId(777L, ADMIN_USERNAME)).thenReturn(Optional.empty());

        bot.consume(textUpdate(telegramUser(777L, ADMIN_USERNAME), CHAT_ID, "Добавь überkommen"));

        verify(userService).findOrBindByTelegramId(777L, ADMIN_USERNAME);
        verifyNoInteractions(wordDraftService);
    }

    @Test
    void anAdminIsRecognisedOnFirstContactThroughTheirTelegramIdAndUsername() {
        WordDraft draft = phraseDraft();
        when(userService.findOrBindByTelegramId(ADMIN_ID, ADMIN_USERNAME)).thenReturn(Optional.of(adminUser(ADMIN_USERNAME, ADMIN_ID)));
        when(wordDraftService.generateDraft("überkommen")).thenReturn(Optional.of(draft));
        when(wordService.findOneByDeAndWordType(draft.de(), draft.wordType())).thenReturn(Optional.empty());

        bot.consume(textUpdate(telegramUser(ADMIN_ID, ADMIN_USERNAME), CHAT_ID, "überkommen"));

        verify(wordDraftService).generateDraft("überkommen");
    }

    @Test
    void anAdminWithoutATelegramUsernameIsRecognisedById() {
        WordDraft draft = phraseDraft();
        when(userService.findOrBindByTelegramId(ADMIN_ID, null)).thenReturn(Optional.of(adminUser(ADMIN_USERNAME, ADMIN_ID)));
        when(wordDraftService.generateDraft("überkommen")).thenReturn(Optional.of(draft));
        when(wordService.findOneByDeAndWordType(draft.de(), draft.wordType())).thenReturn(Optional.empty());

        bot.consume(textUpdate(ADMIN_ID, "überkommen"));

        verify(wordDraftService).generateDraft("überkommen");
    }

    @Test
    void savedWordIsAttributedToTheAdminHandle() {
        WordDraft draft = phraseDraft();
        when(userService.findOrBindByTelegramId(ADMIN_ID, null)).thenReturn(Optional.of(adminUser(ADMIN_USERNAME, ADMIN_ID)));
        when(wordDraftService.generateDraft("auf jeden Fall")).thenReturn(Optional.of(draft));
        when(wordService.findOneByDeAndWordType(draft.de(), draft.wordType())).thenReturn(Optional.empty());
        when(wordService.createOrRewrite(any(Word.class))).thenAnswer(inv -> {
            Word w = inv.getArgument(0);
            w.setId(42L);
            return w;
        });

        bot.consume(textUpdate(ADMIN_ID, "auf jeden Fall"));
        bot.consume(callbackUpdate(ADMIN_ID, DraftReviewPresenter.CALLBACK_ACCEPT, 1));

        ArgumentCaptor<Word> savedCaptor = ArgumentCaptor.forClass(Word.class);
        verify(wordService).createOrRewrite(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getCreatedBy()).isEqualTo(ADMIN_USERNAME);
    }

    @Test
    void recheckThenTextRevisesDraftInsteadOfStartingFresh() {
        WordDraft original = phraseDraft();
        WordDraft revised = new WordDraft(WordType.PHRASE, "auf jeden Fall", "в любом случае, точно", null,
                WordLevel.B1, null, null, null, null, null, null, null, null, null, null, null, null,
                "Er kommt auf jeden Fall.", "Он точно придёт.", null, null);

        when(userService.findOrBindByTelegramId(ADMIN_ID, null)).thenReturn(Optional.of(adminUser(ADMIN_USERNAME, ADMIN_ID)));
        when(wordDraftService.generateDraft("auf jeden Fall")).thenReturn(Optional.of(original));
        when(wordService.findOneByDeAndWordType(any(), any())).thenReturn(Optional.empty());
        when(wordDraftService.reviseDraft(original, "fix the translation")).thenReturn(Optional.of(revised));

        bot.consume(textUpdate(ADMIN_ID, "auf jeden Fall"));
        bot.consume(callbackUpdate(ADMIN_ID, DraftReviewPresenter.CALLBACK_RECHECK, 1));
        bot.consume(textUpdate(ADMIN_ID, "fix the translation"));

        verify(wordDraftService).reviseDraft(original, "fix the translation");
        verify(wordDraftService, never()).generateDraft("fix the translation");
    }

    @Test
    void secondAcceptOnAlreadyResolvedDraftIsNoOp() {
        WordDraft draft = phraseDraft();
        when(userService.findOrBindByTelegramId(ADMIN_ID, null)).thenReturn(Optional.of(adminUser(ADMIN_USERNAME, ADMIN_ID)));
        when(wordDraftService.generateDraft("auf jeden Fall")).thenReturn(Optional.of(draft));
        when(wordService.findOneByDeAndWordType(draft.de(), draft.wordType())).thenReturn(Optional.empty());
        when(wordService.createOrRewrite(any(Word.class))).thenAnswer(inv -> {
            Word w = inv.getArgument(0);
            w.setId(42L);
            return w;
        });

        bot.consume(textUpdate(ADMIN_ID, "auf jeden Fall"));
        bot.consume(callbackUpdate(ADMIN_ID, DraftReviewPresenter.CALLBACK_ACCEPT, 1));
        bot.consume(callbackUpdate(ADMIN_ID, DraftReviewPresenter.CALLBACK_ACCEPT, 1));

        verify(wordService, times(1)).createOrRewrite(any(Word.class));
    }

    @Test
    void acceptWithDbFailureReportsErrorAndKeepsDraftAliveForRetry() {
        // Regression test for a real crash: a DataIntegrityViolationException (e.g. a value too
        // long for its column) escaping createOrRewrite must not silently kill the
        // update-consumer thread - the admin needs to see it and be able to retry.
        WordDraft draft = phraseDraft();
        when(userService.findOrBindByTelegramId(ADMIN_ID, null)).thenReturn(Optional.of(adminUser(ADMIN_USERNAME, ADMIN_ID)));
        when(wordDraftService.generateDraft("auf jeden Fall")).thenReturn(Optional.of(draft));
        when(wordService.findOneByDeAndWordType(draft.de(), draft.wordType())).thenReturn(Optional.empty());
        when(wordService.createOrRewrite(any(Word.class)))
                .thenThrow(new DataIntegrityViolationException("value too long for type character varying(50)"))
                .thenAnswer(inv -> {
                    Word w = inv.getArgument(0);
                    w.setId(42L);
                    return w;
                });

        bot.consume(textUpdate(ADMIN_ID, "auf jeden Fall"));
        bot.consume(callbackUpdate(ADMIN_ID, DraftReviewPresenter.CALLBACK_ACCEPT, 1));
        // Draft must still be alive after the failed save - a second Accept retries instead of no-op'ing.
        bot.consume(callbackUpdate(ADMIN_ID, DraftReviewPresenter.CALLBACK_ACCEPT, 1));

        verify(wordService, times(2)).createOrRewrite(any(Word.class));
    }

    @Test
    void acceptWithHardValidationErrorsDoesNotSaveAndKeepsDraftAlive() {
        // NOUN drafted without a gender is a hard error per WordDraftMapper.validate().
        WordDraft incomplete = new WordDraft(WordType.NOUN, "Tisch", "стол", null, WordLevel.A1,
                null, null, null, null, null, null, null, null, null, null, null, null,
                "Sample.", "Пример.", null, null);
        when(userService.findOrBindByTelegramId(ADMIN_ID, null)).thenReturn(Optional.of(adminUser(ADMIN_USERNAME, ADMIN_ID)));
        when(wordDraftService.generateDraft("Tisch")).thenReturn(Optional.of(incomplete));
        when(wordService.findOneByDeAndWordType(incomplete.de(), incomplete.wordType())).thenReturn(Optional.empty());

        bot.consume(textUpdate(ADMIN_ID, "Tisch"));
        bot.consume(callbackUpdate(ADMIN_ID, DraftReviewPresenter.CALLBACK_ACCEPT, 1));

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

        when(userService.findOrBindByTelegramId(ADMIN_ID, null)).thenReturn(Optional.of(adminUser(ADMIN_USERNAME, ADMIN_ID)));
        when(userService.findOrBindByTelegramId(ADMIN_ID_2, null)).thenReturn(Optional.of(adminUser(ADMIN_USERNAME_2, ADMIN_ID_2)));
        when(wordDraftService.generateDraft("auf jeden Fall")).thenReturn(Optional.of(draft1));
        when(wordDraftService.generateDraft("guten Morgen")).thenReturn(Optional.of(draft2));
        when(wordService.findOneByDeAndWordType(draft1.de(), draft1.wordType())).thenReturn(Optional.empty());
        when(wordService.findOneByDeAndWordType(draft2.de(), draft2.wordType())).thenReturn(Optional.empty());
        when(wordService.createOrRewrite(any(Word.class))).thenAnswer(inv -> {
            Word w = inv.getArgument(0);
            w.setId(42L);
            return w;
        });

        bot.consume(textUpdate(ADMIN_ID, CHAT_ID, "auf jeden Fall"));
        bot.consume(textUpdate(ADMIN_ID_2, CHAT_ID_2, "guten Morgen"));
        bot.consume(callbackUpdate(ADMIN_ID, CHAT_ID, DraftReviewPresenter.CALLBACK_ACCEPT, 1));

        ArgumentCaptor<Word> savedCaptor = ArgumentCaptor.forClass(Word.class);
        verify(wordService).createOrRewrite(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getDe()).isEqualTo(draft1.de());
    }

    @Test
    void secondAdminDraftingSameWordSeesCollisionWarning() throws Exception {
        WordDraft draft = phraseDraft();
        when(userService.findOrBindByTelegramId(ADMIN_ID, null)).thenReturn(Optional.of(adminUser(ADMIN_USERNAME, ADMIN_ID)));
        when(userService.findOrBindByTelegramId(ADMIN_ID_2, null)).thenReturn(Optional.of(adminUser(ADMIN_USERNAME_2, ADMIN_ID_2)));
        when(wordDraftService.generateDraft("auf jeden Fall")).thenReturn(Optional.of(draft));
        when(wordService.findOneByDeAndWordType(draft.de(), draft.wordType())).thenReturn(Optional.empty());

        bot.consume(textUpdate(ADMIN_ID, CHAT_ID, "auf jeden Fall"));
        bot.consume(textUpdate(ADMIN_ID_2, CHAT_ID_2, "auf jeden Fall"));

        ArgumentCaptor<BotApiMethod> sentMessages = ArgumentCaptor.forClass(BotApiMethod.class);
        verify(telegramClient, times(2)).execute(sentMessages.capture());

        // Neither admin has a Telegram username here, so the mention comes from the account handle.
        SendMessage secondAdminMessage = (SendMessage) sentMessages.getAllValues().get(1);
        assertThat(secondAdminMessage.getText()).contains("@" + ADMIN_USERNAME);
    }

    private static WordDraft phraseDraft() {
        return new WordDraft(WordType.PHRASE, "auf jeden Fall", "в любом случае", null, WordLevel.B1,
                null, null, null, null, null, null, null, null, null, null, null, null,
                "Er kommt auf jeden Fall.", "Он в любом случае придёт.", null, null);
    }

    private static Update textUpdate(long telegramId, String text) {
        return textUpdate(telegramId, CHAT_ID, text);
    }

    private static Update textUpdate(long telegramId, Long chatId, String text) {
        return textUpdate(telegramUser(telegramId, null), chatId, text);
    }

    private static Update textUpdate(User from, Long chatId, String text) {
        Update update = new Update();
        Message message = new Message();
        message.setFrom(from);
        message.setChat(new Chat(chatId, "private"));
        message.setMessageId(1);
        message.setText(text);
        update.setMessage(message);
        return update;
    }

    private static Update callbackUpdate(long telegramId, String data, int messageId) {
        return callbackUpdate(telegramId, CHAT_ID, data, messageId);
    }

    private static Update callbackUpdate(long telegramId, Long chatId, String data, int messageId) {
        Update update = new Update();
        Message message = new Message();
        message.setChat(new Chat(chatId, "private"));
        message.setMessageId(messageId);
        CallbackQuery cq = new CallbackQuery();
        cq.setId("cbq-1");
        cq.setFrom(telegramUser(telegramId, null));
        cq.setData(data);
        cq.setMessage(message);
        update.setCallbackQuery(cq);
        return update;
    }

    private static com.kniazev.cards.word.model.user.User adminUser(String handle, long telegramId) {
        return account(handle, telegramId, "ADMIN");
    }

    private static com.kniazev.cards.word.model.user.User playerUser(String handle, long telegramId) {
        return account(handle, telegramId, "PLAYER");
    }

    private static com.kniazev.cards.word.model.user.User account(String handle, long telegramId, String role) {
        return com.kniazev.cards.word.model.user.User.builder()
                .username(handle)
                .telegramId(telegramId)
                .roles(List.of(role))
                .build();
    }

    private static User telegramUser(long id, String username) {
        User user = new User(id, "Test", false);
        user.setUserName(username);
        return user;
    }
}
