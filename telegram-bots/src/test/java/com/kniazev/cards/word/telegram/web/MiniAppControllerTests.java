package com.kniazev.cards.word.telegram.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kniazev.cards.word.game.GameService;
import com.kniazev.cards.word.model.dictionary.Word;
import com.kniazev.cards.word.model.user.GameConfiguration;
import com.kniazev.cards.word.model.user.User;
import com.kniazev.cards.word.service.UserService;
import com.kniazev.cards.word.service.WordService;
import com.kniazev.cards.word.telegram.bot.GermanCardsBot;
import com.kniazev.cards.word.telegram.web.MiniAppController.SelectionRequest;
import com.kniazev.cards.word.telegram.web.TelegramInitDataValidator.TelegramUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class MiniAppControllerTests {

    private static final String INIT_DATA = "signed-init-data";

    @Mock
    private WordService wordService;
    @Mock
    private GameService gameService;
    @Mock
    private UserService userService;
    @Mock
    private TelegramInitDataValidator initDataValidator;
    @Mock
    private GermanCardsBot bot;

    private MiniAppController controller;

    @BeforeEach
    void setUp() {
        controller = new MiniAppController(wordService, gameService, userService, initDataValidator, Optional.of(bot));
    }

    @Test
    void aUserWithoutATelegramUsernameIsServedByTheAccountHandle() {
        GameConfiguration configuration = new GameConfiguration();
        configuration.setSpecificWordIds(List.of(1L, 2L));

        when(initDataValidator.validateAndExtractUser(INIT_DATA)).thenReturn(new TelegramUser(42L, null, "Alice", "de"));
        when(userService.findOrBindByTelegramId(42L, null)).thenReturn(Optional.of(account("tg-42", 42L)));
        when(gameService.getGameConfiguration("tg-42")).thenReturn(configuration);

        assertThat(controller.getSelection(INIT_DATA)).containsExactly(1L, 2L);
    }

    @Test
    void aTelegramUserWithoutAnAccountIsRejected() {
        when(initDataValidator.validateAndExtractUser(INIT_DATA)).thenReturn(new TelegramUser(42L, "alice", "Alice", "de"));
        when(userService.findOrBindByTelegramId(42L, "alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getSelection(INIT_DATA))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verifyNoInteractions(gameService);
    }

    @Test
    void savingTheSelectionRestartsTheAccountsGameAndNotifiesTheSignedTelegramChat() throws Exception {
        GameConfiguration configuration = new GameConfiguration();
        Word word = new Word();
        word.setId(5L);

        when(initDataValidator.validateAndExtractUser(INIT_DATA)).thenReturn(new TelegramUser(42L, null, "Alice", "de"));
        when(userService.findOrBindByTelegramId(42L, null)).thenReturn(Optional.of(account("tg-42", 42L)));
        when(gameService.getGameConfiguration("tg-42")).thenReturn(configuration);
        when(wordService.findByIds(List.of(5L))).thenReturn(List.of(word));

        Map<String, Object> result = controller.saveSelection(INIT_DATA, new SelectionRequest(List.of(5L)));

        assertThat(result).containsEntry("saved", 1);
        assertThat(configuration.getSpecificWordIds()).containsExactly(5L);

        verify(gameService).restartGame(eq("tg-42"), eq(configuration), any(Locale.class));
        // The chat id is the signed Telegram id, not the internal handle.
        verify(bot).notifyGameRestarted(eq("tg-42"), eq("42"), any(Locale.class));
    }

    private static User account(String handle, long telegramId) {
        return User.builder()
                .username(handle)
                .telegramId(telegramId)
                .roles(List.of("PLAYER"))
                .enabled(true)
                .build();
    }
}
