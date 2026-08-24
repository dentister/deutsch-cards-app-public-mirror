package com.kniazev.cards.word.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kniazev.cards.word.ai.WordExampleService;
import com.kniazev.cards.word.db.model.User;
import com.kniazev.cards.word.db.model.word.Adjective;
import com.kniazev.cards.word.db.model.word.Word.WordLevel;
import com.kniazev.cards.word.db.service.UserService;
import com.kniazev.cards.word.db.service.WordScoreService;
import com.kniazev.cards.word.db.service.WordService;
import com.kniazev.cards.word.error.exception.GameNotStartedException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Locale;

@ExtendWith(MockitoExtension.class)
class GameServiceTests {

    private static final String USERNAME = "alice";

    @Mock
    private WordService wordService;
    @Mock
    private WordScoreService wordScoreService;
    @Mock
    private UserService userService;
    @Mock
    private WordExampleService wordExampleService;
    @Mock
    private GameCache gameCache;

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService(wordService, wordScoreService, userService, wordExampleService, gameCache);
    }

    @Test
    void getNextTaskRestartsGameWhenCacheIsEmpty() {
        User user = User.builder().id(1L).username(USERNAME).build();
        Adjective word = Adjective.builder().id(10L).de("schnell").ru("быстрый").level(WordLevel.A1).build();
        WordCard task = GameCardFactory.rebuildCard(word, TaskEnum.ADJECTIVE);

        when(gameCache.isEmpty(USERNAME)).thenReturn(true);
        when(userService.getOneByUsername(USERNAME)).thenReturn(user);
        when(wordScoreService.findRandomLowScoredWordIds(eq(1L), any(), any(), any(), any()))
                .thenReturn(List.of(10L));
        when(wordService.findByIds(List.of(10L))).thenReturn(List.of(word));
        when(wordScoreService.isNewWord(user, word)).thenReturn(true);
        when(gameCache.peek(USERNAME)).thenReturn(task);

        WordCard result = gameService.getNextTask(USERNAME, Locale.ROOT);

        assertThat(result).isSameAs(task);
        verify(gameCache).clear(USERNAME);
        verify(gameCache).push(eq(USERNAME), any(WordCard.class));
    }

    @Test
    void getNextTaskSkipsRestartWhenCacheIsNotEmpty() {
        Adjective word = Adjective.builder().id(11L).de("laut").ru("громкий").level(WordLevel.A1).build();
        WordCard task = GameCardFactory.rebuildCard(word, TaskEnum.ADJECTIVE);

        when(gameCache.isEmpty(USERNAME)).thenReturn(false);
        when(gameCache.peek(USERNAME)).thenReturn(task);

        WordCard result = gameService.getNextTask(USERNAME, Locale.ROOT);

        assertThat(result).isSameAs(task);
        verifyNoInteractions(userService, wordScoreService, wordExampleService);
        verify(gameCache, never()).clear(any());
        verify(gameCache, never()).push(any(), any());
    }

    @Test
    void checkUserAnswerThrowsWhenNoCardIsPending() {
        when(gameCache.poll(USERNAME)).thenReturn(null);

        assertThatThrownBy(() -> gameService.checkUserAnswer(USERNAME, "anything"))
                .isInstanceOf(GameNotStartedException.class);
    }

    @Test
    void checkUserAnswerPushesRetryCardCarryingTheAnsweredCardsLocale() {
        User user = User.builder().id(1L).username(USERNAME).build();
        Adjective word = Adjective.builder().id(12L).de("schnell").ru("быстрый").level(WordLevel.A1).build();
        WordCard pending = GameCardFactory.rebuildCard(word, TaskEnum.ADJECTIVE);
        pending.setLocale(Locale.forLanguageTag("ru"));

        when(gameCache.poll(USERNAME)).thenReturn(pending);
        when(wordService.getOneById(12L)).thenReturn(word);
        when(userService.getOneByUsername(USERNAME)).thenReturn(user);

        WordCard answered = gameService.checkUserAnswer(USERNAME, "wrong answer");

        assertThat(answered.isRightAnswered()).isFalse();
        verify(wordScoreService).saveScore(user, word, false);

        ArgumentCaptor<WordCard> retryCaptor = ArgumentCaptor.forClass(WordCard.class);
        verify(gameCache).push(eq(USERNAME), retryCaptor.capture());
        assertThat(retryCaptor.getValue().getLocale()).isEqualTo(Locale.forLanguageTag("ru"));
    }

    @Test
    void checkUserAnswerDoesNotPushRetryCardOnRightAnswer() {
        User user = User.builder().id(1L).username(USERNAME).build();
        Adjective word = Adjective.builder().id(13L).de("schnell").ru("быстрый").level(WordLevel.A1).build();
        WordCard pending = GameCardFactory.rebuildCard(word, TaskEnum.ADJECTIVE);

        when(gameCache.poll(USERNAME)).thenReturn(pending);
        when(wordService.getOneById(13L)).thenReturn(word);
        when(userService.getOneByUsername(USERNAME)).thenReturn(user);

        WordCard answered = gameService.checkUserAnswer(USERNAME, "schnell");

        assertThat(answered.isRightAnswered()).isTrue();
        verify(wordScoreService).saveScore(user, word, true);
        verify(gameCache, never()).push(any(), any());
    }

    @Test
    void restartGameSetsLocaleOnPulledCards() {
        User user = User.builder().id(1L).username(USERNAME).build();
        Adjective word = Adjective.builder().id(14L).de("schnell").ru("быстрый").level(WordLevel.A1).build();
        Locale locale = Locale.forLanguageTag("de");

        when(userService.getOneByUsername(USERNAME)).thenReturn(user);
        when(wordScoreService.findRandomLowScoredWordIds(eq(1L), any(), any(), any(), any()))
                .thenReturn(List.of(14L));
        when(wordService.findByIds(List.of(14L))).thenReturn(List.of(word));
        when(wordScoreService.isNewWord(user, word)).thenReturn(false);

        gameService.restartGame(USERNAME, null, locale);

        ArgumentCaptor<WordCard> pushedCaptor = ArgumentCaptor.forClass(WordCard.class);
        verify(gameCache).push(eq(USERNAME), pushedCaptor.capture());
        assertThat(pushedCaptor.getValue().getLocale()).isEqualTo(locale);
    }

    @Test
    void restartGameSavesPulledWordIdsAsLastRound() {
        User user = User.builder().id(1L).username(USERNAME).build();
        Adjective word = Adjective.builder().id(15L).de("schnell").ru("быстрый").level(WordLevel.A1).build();

        when(userService.getOneByUsername(USERNAME)).thenReturn(user);
        when(wordScoreService.findRandomLowScoredWordIds(eq(1L), any(), any(), any(), any()))
                .thenReturn(List.of(15L));
        when(wordService.findByIds(List.of(15L))).thenReturn(List.of(word));
        when(wordScoreService.isNewWord(user, word)).thenReturn(false);

        gameService.restartGame(USERNAME, null, Locale.ROOT);

        verify(gameCache).saveRoundWordIds(USERNAME, List.of(15L));
    }

    @Test
    void getLastRoundWordsHydratesIdsFromCache() {
        Adjective word = Adjective.builder().id(16L).de("schnell").ru("быстрый").level(WordLevel.A1).build();

        when(gameCache.getRoundWordIds(USERNAME)).thenReturn(List.of(16L));
        when(wordService.findByIds(List.of(16L))).thenReturn(List.of(word));

        assertThat(gameService.getLastRoundWords(USERNAME)).containsExactly(word);
    }

    @Test
    void getLastRoundWordsReturnsEmptyWhenNoRoundCached() {
        when(gameCache.getRoundWordIds(USERNAME)).thenReturn(List.of());

        assertThat(gameService.getLastRoundWords(USERNAME)).isEmpty();
        verifyNoInteractions(wordService);
    }

    @Test
    void getGameWordsDelegatesToCache() {
        List<WordCard> cached = List.of();
        when(gameCache.list(USERNAME)).thenReturn(cached);

        assertThat(gameService.getGameWords(USERNAME)).isSameAs(cached);
    }
}
