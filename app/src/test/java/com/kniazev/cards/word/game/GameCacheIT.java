package com.kniazev.cards.word.game;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kniazev.cards.word.game.representation.TaskEnum;
import com.kniazev.cards.word.game.representation.WordCard;
import com.kniazev.cards.word.model.dictionary.Adjective;
import com.kniazev.cards.word.model.dictionary.Verb;
import com.kniazev.cards.word.model.dictionary.Word.WordLevel;
import com.kniazev.cards.word.service.WordService;
import com.kniazev.cards.word.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

/**
 * Exercises {@link GameCache} on real Redis, rehydrating cards from real words in Postgres. Redis is flushed before
 * every test by {@link AbstractIntegrationTest}, so the user names need not be unique.
 */
class GameCacheIT extends AbstractIntegrationTest {

    private static final String USERNAME = "alice";

    @Autowired
    private GameCache cache;
    @Autowired
    private WordService wordService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void pushedCardRoundTripsWithCorrectRightAnswer() {
        Adjective schnell = testData.adjective("schnell", "быстрый", WordLevel.A1);

        cache.push(USERNAME, GameCardFactory.rebuildCard(schnell, TaskEnum.ADJECTIVE));

        WordCard card = cache.poll(USERNAME);

        assertThat(card).isNotNull();
        assertThat(card.getWordId()).isEqualTo(schnell.getId());
        assertThat(card.getRightAnswer().get()).isEqualTo("schnell");
    }

    @Test
    void queueIsFifoAcrossMultiplePushes() {
        Adjective erste = testData.adjective("erste", "первый", WordLevel.A1);
        Adjective zweite = testData.adjective("zweite", "второй", WordLevel.A1);

        cache.push(USERNAME, GameCardFactory.rebuildCard(erste, TaskEnum.ADJECTIVE));
        cache.push(USERNAME, GameCardFactory.rebuildCard(zweite, TaskEnum.ADJECTIVE));

        assertThat(cache.poll(USERNAME).getWordId()).isEqualTo(erste.getId());
        assertThat(cache.poll(USERNAME).getWordId()).isEqualTo(zweite.getId());
        assertThat(cache.isEmpty(USERNAME)).isTrue();
    }

    @Test
    void queuesOfDifferentUsersDoNotMix() {
        Adjective laut = testData.adjective("laut", "громкий", WordLevel.A1);

        cache.push(USERNAME, GameCardFactory.rebuildCard(laut, TaskEnum.ADJECTIVE));

        assertThat(cache.isEmpty("bob")).isTrue();
        assertThat(cache.isEmpty(USERNAME)).isFalse();
    }

    @Test
    void clearRemovesAllCardsForTheUser() {
        Adjective laut = testData.adjective("laut", "громкий", WordLevel.A1);

        cache.push(USERNAME, GameCardFactory.rebuildCard(laut, TaskEnum.ADJECTIVE));
        cache.clear(USERNAME);

        assertThat(cache.isEmpty(USERNAME)).isTrue();
        assertThat(cache.peek(USERNAME)).isNull();
    }

    @Test
    void pushSetsATtlOnTheQueueKey() {
        Adjective leise = testData.adjective("leise", "тихий", WordLevel.A1);

        cache.push(USERNAME, GameCardFactory.rebuildCard(leise, TaskEnum.ADJECTIVE));

        Long ttl = redisTemplate.getExpire("game:queue:" + USERNAME);

        assertThat(ttl).isNotNull().isGreaterThan(0);
    }

    @Test
    void rehydrateRestoresTheExactTaskTypeItWasPushedWith() {
        // Adjective only ever has one possible task variant, so it can't catch a regression where rehydration picks a
        // random task instead of the one that was actually shown to the user - a Verb (8 possible variants) can.
        // Made up, because the seeded dictionary already holds the real "gehen".
        Verb testgehen = testData.verb("testgehen", "тест-идти", "testgehe", "testgehst", "testgeht", "testgehen", "testgeht", "testgehen");

        cache.push(USERNAME, GameCardFactory.rebuildCard(testgehen, TaskEnum.ICH_VERB));

        WordCard card = cache.poll(USERNAME);

        assertThat(card).isNotNull();
        assertThat(card.getTaskId()).isEqualTo(TaskEnum.ICH_VERB);
        assertThat(card.getRightAnswer().get()).isEqualTo("testgehe");
    }

    @Test
    void stateIsVisibleFromAFreshGameCacheInstance() {
        Adjective schwer = testData.adjective("schwer", "тяжёлый", WordLevel.A1);

        cache.push(USERNAME, GameCardFactory.rebuildCard(schwer, TaskEnum.ADJECTIVE));

        // A fresh GameCache instance simulates an app restart: nothing may live outside Redis.
        List<WordCard> survived = new GameCache(wordService, redisTemplate, objectMapper).list(USERNAME);

        assertThat(survived).singleElement().satisfies(card -> assertThat(card.getWordId()).isEqualTo(schwer.getId()));
    }

    @Test
    void theRoundWordIdsAreRememberedIndependentlyOfTheQueue() {
        cache.saveRoundWordIds(USERNAME, List.of(3L, 1L, 2L));
        cache.clear(USERNAME);

        assertThat(cache.getRoundWordIds(USERNAME)).containsExactly(3L, 1L, 2L);
        assertThat(cache.getRoundWordIds("bob")).isEmpty();
    }
}
