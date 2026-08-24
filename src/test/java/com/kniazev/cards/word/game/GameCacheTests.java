package com.kniazev.cards.word.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kniazev.cards.word.db.model.word.Adjective;
import com.kniazev.cards.word.db.model.word.Verb;
import com.kniazev.cards.word.db.model.word.Word.WordLevel;
import com.kniazev.cards.word.db.service.WordService;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class GameCacheTests {

    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;

    @Mock
    private WordService wordService;

    @BeforeAll
    static void startRedis() {
        REDIS.start();

        RedisStandaloneConfiguration config =
                new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory = new LettuceConnectionFactory(config);
        connectionFactory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
    }

    @AfterAll
    static void stopRedis() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
        REDIS.stop();
    }

    private GameCache newGameCache() {
        return new GameCache(wordService, redisTemplate, new ObjectMapper());
    }

    @Test
    void pushedCardRoundTripsWithCorrectRightAnswer() {
        String username = "alice-roundtrip";
        Adjective word = Adjective.builder().id(10L).de("schnell").ru("быстрый").level(WordLevel.A1).build();
        when(wordService.getOneById(10L)).thenReturn(word);

        GameCache cache = newGameCache();
        cache.push(username, GameCardFactory.rebuildCard(word, TaskEnum.ADJECTIVE));

        WordCard card = cache.poll(username);

        assertThat(card).isNotNull();
        assertThat(card.getWordId()).isEqualTo(10L);
        assertThat(card.getRightAnswer().get()).isEqualTo("schnell");
    }

    @Test
    void queueIsFifoAcrossMultiplePushes() {
        String username = "alice-fifo";
        Adjective first = Adjective.builder().id(20L).de("erste").ru("первый").level(WordLevel.A1).build();
        Adjective second = Adjective.builder().id(21L).de("zweite").ru("второй").level(WordLevel.A1).build();
        when(wordService.getOneById(20L)).thenReturn(first);
        when(wordService.getOneById(21L)).thenReturn(second);

        GameCache cache = newGameCache();
        cache.push(username, GameCardFactory.rebuildCard(first, TaskEnum.ADJECTIVE));
        cache.push(username, GameCardFactory.rebuildCard(second, TaskEnum.ADJECTIVE));

        assertThat(cache.poll(username).getWordId()).isEqualTo(20L);
        assertThat(cache.poll(username).getWordId()).isEqualTo(21L);
        assertThat(cache.isEmpty(username)).isTrue();
    }

    @Test
    void clearRemovesAllCardsForTheUser() {
        String username = "alice-clear";
        Adjective word = Adjective.builder().id(30L).de("laut").ru("громкий").level(WordLevel.A1).build();

        GameCache cache = newGameCache();
        cache.push(username, GameCardFactory.rebuildCard(word, TaskEnum.ADJECTIVE));
        cache.clear(username);

        assertThat(cache.isEmpty(username)).isTrue();
        assertThat(cache.peek(username)).isNull();
    }

    @Test
    void pushSetsATtlOnTheQueueKey() {
        String username = "alice-ttl";
        Adjective word = Adjective.builder().id(40L).de("leise").ru("тихий").level(WordLevel.A1).build();

        newGameCache().push(username, GameCardFactory.rebuildCard(word, TaskEnum.ADJECTIVE));

        Long ttl = redisTemplate.getExpire("game:queue:" + username);
        assertThat(ttl).isNotNull();
        assertThat(ttl).isGreaterThan(0);
    }

    @Test
    void rehydrateRestoresTheExactTaskTypeItWasPushedWith() {
        // Adjective only ever has one possible task variant, so it can't catch a regression
        // where rehydration picks a random task instead of the one that was actually shown to
        // the user - a Verb (8 possible variants) can. Regression test for exactly that bug.
        String username = "alice-verb-task";
        Verb word = Verb.builder().id(60L).de("gehen").ru("идти").level(WordLevel.A1)
                .ich("gehe").du("gehst").er("geht").wir("gehen").ihr("geht").sie("gehen")
                .partizip2("gegangen").build();
        when(wordService.getOneById(60L)).thenReturn(word);

        GameCache cache = newGameCache();
        cache.push(username, GameCardFactory.rebuildCard(word, TaskEnum.ICH_VERB));

        WordCard card = cache.poll(username);

        assertThat(card).isNotNull();
        assertThat(card.getTaskMsg().getTaskId()).isEqualTo(TaskEnum.ICH_VERB);
        assertThat(card.getRightAnswer().get()).isEqualTo("gehe");
    }

    @Test
    void stateIsVisibleFromAFreshGameCacheInstance() {
        // A fresh GameCache instance simulates an app restart.
        String username = "alice-restart";
        Adjective word = Adjective.builder().id(50L).de("schwer").ru("тяжёлый").level(WordLevel.A1).build();
        when(wordService.getOneById(50L)).thenReturn(word);

        newGameCache().push(username, GameCardFactory.rebuildCard(word, TaskEnum.ADJECTIVE));

        List<WordCard> survived = newGameCache().list(username);

        assertThat(survived).hasSize(1);
        assertThat(survived.get(0).getWordId()).isEqualTo(50L);
    }
}
