package com.kniazev.cards.word.telegram.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kniazev.cards.word.ai.blitz.BlitzGameService;
import com.kniazev.cards.word.game.GameService;
import com.kniazev.cards.word.game.representation.WordCard;
import com.kniazev.cards.word.model.user.GameConfiguration;
import com.kniazev.cards.word.model.user.User;
import com.kniazev.cards.word.service.UserService;
import com.kniazev.cards.word.service.UserService.TelegramUserResolution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class GermanCardsBotTests {

    private static final String WEB_URL = "https://germancards.run.place";

    @Mock
    private TelegramClient telegramClient;
    @Mock
    private GameService gameService;
    @Mock
    private UserService userService;
    @Mock
    private GermanCardsBotKeyboards keyboards;
    @Mock
    private GameCardPresenter presenter;
    @Mock
    private BlitzGameService blitzGameService;

    @Test
    void anAccountWithoutATelegramUsernameIsServedByItsHandle() {
        GameConfiguration configuration = new GameConfiguration();

        when(userService.getOrCreateByTelegramId(42L, null)).thenReturn(resolution("tg-42", 42L, null));
        when(gameService.getGameConfiguration("tg-42")).thenReturn(configuration);

        bot(WEB_URL).consume(commandUpdate(42L, null, "/switchpreview"));

        verify(gameService).saveGameConfiguration("tg-42", configuration);
        assertThat(configuration.getShowWordsPreview()).isFalse();
    }

    @Test
    void theCredentialsFollowTheWelcomeMessageWhenTheAccountWasJustCreated() throws Exception {
        stubRestart("alice");

        when(userService.getOrCreateByTelegramId(42L, "alice")).thenReturn(resolution("alice", 42L, "fresh-secret"));

        bot(WEB_URL).consume(commandUpdate(42L, "alice", "/start"));

        List<String> texts = sentTexts();

        assertThat(texts).filteredOn(t -> t.contains("Password:")).hasSize(1)
                .allSatisfy(t -> assertThat(t).contains("alice").contains("fresh\\-secret").contains("germancards"));
    }

    @Test
    void aReturningUserIsNotShownCredentialsOnStart() throws Exception {
        stubRestart("alice");

        when(userService.getOrCreateByTelegramId(42L, "alice")).thenReturn(resolution("alice", 42L, null));

        bot(WEB_URL).consume(commandUpdate(42L, "alice", "/start"));

        assertThat(sentTexts()).noneMatch(t -> t.contains("Password:"));
    }

    @Test
    void noCredentialsAreSentWhenTheWebAppUrlIsNotConfigured() throws Exception {
        stubRestart("alice");

        when(userService.getOrCreateByTelegramId(42L, "alice")).thenReturn(resolution("alice", 42L, "fresh-secret"));

        bot("").consume(commandUpdate(42L, "alice", "/start"));

        assertThat(sentTexts()).noneMatch(t -> t.contains("Password:") || t.contains("fresh"));
    }

    @Test
    void websiteReissuesThePasswordOfTheCallersAccount() throws Exception {
        when(userService.getOrCreateByTelegramId(42L, "alice")).thenReturn(resolution("alice", 42L, null));
        when(userService.regeneratePassword("alice")).thenReturn("new-secret");

        bot(WEB_URL).consume(commandUpdate(42L, "alice", "/website"));

        assertThat(sentTexts()).singleElement()
                .satisfies(t -> assertThat(t).contains("alice").contains("new\\-secret").contains("germancards"));
    }

    @Test
    void websiteDoesNotTouchThePasswordWhenTheWebAppUrlIsNotConfigured() throws Exception {
        when(userService.getOrCreateByTelegramId(42L, "alice")).thenReturn(resolution("alice", 42L, null));

        bot("").consume(commandUpdate(42L, "alice", "/website"));

        verify(userService, never()).regeneratePassword(any());
        assertThat(sentTexts()).singleElement().satisfies(t -> assertThat(t).contains("not available yet"));
    }

    private GermanCardsBot bot(String webAppUrl) {
        return new GermanCardsBot("fake-token", "", webAppUrl, gameService, userService, keyboards, presenter,
                blitzGameService, telegramClient);
    }

    /** Stubs what the game restart that follows /start needs, so the flow can run to the end. */
    private void stubRestart(String handle) {
        WordCard nextTask = mock(WordCard.class);

        when(presenter.task(eq(nextTask), any())).thenReturn("Next noun");
        when(gameService.getNextTask(eq(handle), any())).thenReturn(nextTask);
        when(gameService.getGameConfiguration(handle)).thenReturn(new GameConfiguration());
    }

    private List<String> sentTexts() throws Exception {
        ArgumentCaptor<BotApiMethod> sent = ArgumentCaptor.forClass(BotApiMethod.class);

        verify(telegramClient, atLeastOnce()).execute(sent.capture());

        return sent.getAllValues().stream()
                .filter(SendMessage.class::isInstance)
                .map(m -> ((SendMessage) m).getText())
                .toList();
    }

    private static TelegramUserResolution resolution(String handle, long telegramId, String newPassword) {
        User account = User.builder().username(handle).telegramId(telegramId).roles(List.of("PLAYER")).enabled(true).build();

        return new TelegramUserResolution(account, newPassword);
    }

    private static Update commandUpdate(long telegramId, String username, String command) {
        org.telegram.telegrambots.meta.api.objects.User from =
                new org.telegram.telegrambots.meta.api.objects.User(telegramId, "Test", false);
        from.setUserName(username);

        Message message = new Message();
        message.setFrom(from);
        message.setChat(new Chat(telegramId, "private"));
        message.setMessageId(1);
        message.setText(command);
        message.setEntities(List.of(MessageEntity.builder().type("bot_command").offset(0).length(command.length()).build()));

        Update update = new Update();
        update.setMessage(message);

        return update;
    }
}
