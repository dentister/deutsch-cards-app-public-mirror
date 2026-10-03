package com.kniazev.cards.word.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.kniazev.cards.word.error.exception.GameNotStartedException;
import com.kniazev.cards.word.game.representation.WordCard;
import com.kniazev.cards.word.model.WordScore;
import com.kniazev.cards.word.model.dictionary.Adjective;
import com.kniazev.cards.word.model.dictionary.Word;
import com.kniazev.cards.word.model.dictionary.Word.WordLevel;
import com.kniazev.cards.word.model.dictionary.Word.WordType;
import com.kniazev.cards.word.model.user.GameConfiguration;
import com.kniazev.cards.word.model.user.User;
import com.kniazev.cards.word.repository.WordScoreRepository;
import com.kniazev.cards.word.support.AbstractIntegrationTest;
import com.kniazev.cards.word.support.TestData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Locale;

/**
 * Plays {@link GameService} against the real stack: Postgres (including the {@code get_word_scores} function and the
 * json settings column) and Redis (the card queue). Only the Gemini-backed {@link WordEnrichment} is a mock.
 * Rounds are made deterministic with fixture words and {@link TestData#configFor}, because the seeded dictionary is large
 * and word selection is random.
 */
class GameServiceIT extends AbstractIntegrationTest {

    private static final String USERNAME = "alice";
    private static final Locale RU = Locale.forLanguageTag("ru");
    private static final Locale DE = Locale.forLanguageTag("de");

    @Autowired
    private GameService gameService;
    @Autowired
    private WordScoreRepository wordScoreRepository;

    private User alice;
    private Adjective schnell;
    private Adjective laut;

    @BeforeEach
    void createPlayerAndWords() {
        alice = testData.user(USERNAME);
        schnell = testData.adjective("schnell", "быстрый", WordLevel.A1);
        laut = testData.adjective("laut", "громкий", WordLevel.A1);
    }

    @Test
    void getNextTaskStartsAGameWhenNothingIsQueued() {
        gameService.saveGameConfiguration(USERNAME, testData.configFor(schnell, laut));

        WordCard task = gameService.getNextTask(USERNAME, Locale.ROOT);

        assertThat(task.getWordId()).isIn(schnell.getId(), laut.getId());
        assertThat(wordIds(gameService.getGameWords(USERNAME))).containsExactlyInAnyOrder(schnell.getId(), laut.getId());
        verify(wordEnrichment, times(2)).ensureEnriched(any(Word.class));
    }

    @Test
    void getNextTaskKeepsTheRunningGameWhenCardsAreQueued() {
        gameService.saveGameConfiguration(USERNAME, testData.configFor(schnell, laut));

        WordCard first = gameService.getNextTask(USERNAME, Locale.ROOT);
        WordCard second = gameService.getNextTask(USERNAME, Locale.ROOT);

        assertThat(second.getWordId()).isEqualTo(first.getWordId());
        assertThat(gameService.getGameWords(USERNAME)).hasSize(2);
        // Enrichment ran once per card of the first restart only; the second call did not restart.
        verify(wordEnrichment, times(2)).ensureEnriched(any(Word.class));
    }

    @Test
    void checkUserAnswerFailsWhenNoGameWasStarted() {
        assertThatThrownBy(() -> gameService.checkUserAnswer(USERNAME, "anything"))
                .isInstanceOf(GameNotStartedException.class);
    }

    @Test
    void aWrongAnswerIsScoredAsMissedAndTheWordComesBackWithTheSameLocale() {
        gameService.saveGameConfiguration(USERNAME, testData.configFor(schnell));
        gameService.getNextTask(USERNAME, RU);

        WordCard answered = gameService.checkUserAnswer(USERNAME, "wrong answer");

        assertThat(answered.isRightAnswered()).isFalse();
        // WordScore starts at anchor 2; every answer moves the anchor, only a right one moves the score.
        assertThat(scoreOf(schnell)).satisfies(score -> {
            assertThat(score.getAnchor()).isEqualTo(3);
            assertThat(score.getScore()).isZero();
        });

        List<WordCard> queue = gameService.getGameWords(USERNAME);

        assertThat(queue).singleElement().satisfies(retry -> {
            assertThat(retry.getWordId()).isEqualTo(schnell.getId());
            assertThat(retry.getLocale()).isEqualTo(RU);
        });
    }

    @Test
    void aRightAnswerIsScoredAndNothingIsQueuedForRetry() {
        gameService.saveGameConfiguration(USERNAME, testData.configFor(schnell));
        gameService.getNextTask(USERNAME, Locale.ROOT);

        WordCard answered = gameService.checkUserAnswer(USERNAME, "schnell");

        assertThat(answered.isRightAnswered()).isTrue();
        assertThat(scoreOf(schnell)).satisfies(score -> {
            assertThat(score.getAnchor()).isEqualTo(3);
            assertThat(score.getScore()).isEqualTo(1);
        });
        assertThat(gameService.getGameWords(USERNAME)).isEmpty();
    }

    @Test
    void aWordStopsBeingNewOnceItWasAnsweredRightTwice() {
        gameService.saveGameConfiguration(USERNAME, testData.configFor(schnell));

        assertThat(gameService.getNextTask(USERNAME, Locale.ROOT).isNewWord()).isTrue();
        gameService.checkUserAnswer(USERNAME, "schnell");

        // One right answer is still "new" (score <= 1).
        assertThat(gameService.getNextTask(USERNAME, Locale.ROOT).isNewWord()).isTrue();
        gameService.checkUserAnswer(USERNAME, "schnell");

        assertThat(gameService.getNextTask(USERNAME, Locale.ROOT).isNewWord()).isFalse();
    }

    @Test
    void restartGameQueuesTheConfiguredWordsWithTheRequestedLocaleAndPersistsTheConfiguration() {
        gameService.restartGame(USERNAME, testData.configFor(schnell, laut), DE);

        List<WordCard> queue = gameService.getGameWords(USERNAME);

        assertThat(wordIds(queue)).containsExactlyInAnyOrder(schnell.getId(), laut.getId());
        assertThat(queue).extracting(WordCard::getLocale).containsOnly(DE);
        assertThat(gameService.getGameConfiguration(USERNAME).getSpecificWordIds())
                .containsExactlyInAnyOrder(schnell.getId(), laut.getId());
    }

    @Test
    void restartGameRespectsTheLevelCapOfTheConfiguration() {
        Adjective advanced = testData.adjective("hartnäckig", "упорный", WordLevel.C1);
        GameConfiguration configuration = new GameConfiguration();

        configuration.setMaxLevel(WordLevel.B1);
        configuration.setWordType(WordType.ADJECTIVE);
        configuration.setTags(List.of(TestData.FIXTURE_TAG));

        gameService.restartGame(USERNAME, configuration, Locale.ROOT);

        assertThat(wordIds(gameService.getGameWords(USERNAME)))
                .containsExactlyInAnyOrder(schnell.getId(), laut.getId())
                .doesNotContain(advanced.getId());
    }

    @Test
    void restartGameReplacesTheQueueOfTheRunningGame() {
        gameService.restartGame(USERNAME, testData.configFor(schnell, laut), Locale.ROOT);
        gameService.restartGame(USERNAME, testData.configFor(laut), Locale.ROOT);

        assertThat(wordIds(gameService.getGameWords(USERNAME))).containsExactly(laut.getId());
    }

    @Test
    void theLastRoundIsRememberedEvenAfterTheCardsWereAnswered() {
        gameService.restartGame(USERNAME, testData.configFor(schnell, laut), Locale.ROOT);
        gameService.checkUserAnswer(USERNAME, "schnell");

        assertThat(gameService.getLastRoundWords(USERNAME)).extracting(Word::getId)
                .containsExactlyInAnyOrder(schnell.getId(), laut.getId());
    }

    @Test
    void thereIsNoLastRoundBeforeTheFirstGame() {
        assertThat(gameService.getLastRoundWords(USERNAME)).isEmpty();
    }

    @Test
    void theGameConfigurationRoundTripsThroughTheJsonSettingsColumn() {
        GameConfiguration configuration = new GameConfiguration();

        configuration.setMaxLevel(WordLevel.B2);
        configuration.setWordType(WordType.VERB);
        configuration.setTags(List.of("health", "it"));
        configuration.setShowWordsPreview(false);

        gameService.saveGameConfiguration(USERNAME, configuration);

        assertThat(gameService.getGameConfiguration(USERNAME)).isEqualTo(configuration);
    }

    private WordScore scoreOf(Word word) {
        return wordScoreRepository.findFirstByUserAndWord(alice, word).orElseThrow();
    }

    private static List<Long> wordIds(List<WordCard> cards) {
        return cards.stream().map(WordCard::getWordId).toList();
    }
}
