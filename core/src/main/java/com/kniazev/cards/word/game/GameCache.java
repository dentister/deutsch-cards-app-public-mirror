package com.kniazev.cards.word.game;

import com.kniazev.cards.word.game.representation.TaskEnum;
import com.kniazev.cards.word.game.representation.WordCard;
import com.kniazev.cards.word.model.dictionary.Word;
import com.kniazev.cards.word.service.WordService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
class GameCache {
    private static final String GAME_KEY_PREFIX = "game:queue:";
    private static final String ROUND_KEY_PREFIX = "game:round:";
    private static final Duration GAME_TTL = Duration.ofDays(60);

    private final WordService wordService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private record CachedCard(Long wordId, TaskEnum taskId, String userAnswer, boolean newWord, String locale) { }

    public boolean isEmpty(String username) {
        String key = queueKey(username);
        Long size = redisTemplate.opsForList().size(key);
        redisTemplate.expire(key, GAME_TTL);

        return size == null || size == 0;
    }

    public WordCard peek(String username) {
        String key = queueKey(username);
        String json = redisTemplate.opsForList().index(key, 0);
        redisTemplate.expire(key, GAME_TTL);

        return json == null ? null : rehydrate(fromJson(json));
    }

    public WordCard poll(String username) {
        String key = queueKey(username);
        String json = redisTemplate.opsForList().leftPop(key);
        redisTemplate.expire(key, GAME_TTL);

        return json == null ? null : rehydrate(fromJson(json));
    }

    public void push(String username, WordCard card) {
        String key = queueKey(username);
        redisTemplate.opsForList().rightPush(key, toJson(toCached(card)));
        redisTemplate.expire(key, GAME_TTL);
    }

    public void clear(String username) {
        redisTemplate.delete(queueKey(username));
    }

    public void saveRoundWordIds(String username, List<Long> wordIds) {
        String key = roundKey(username);
        redisTemplate.opsForValue().set(key, toJson(wordIds));
        redisTemplate.expire(key, GAME_TTL);
    }

    public List<Long> getRoundWordIds(String username) {
        String key = roundKey(username);
        String json = redisTemplate.opsForValue().get(key);
        redisTemplate.expire(key, GAME_TTL);

        return json == null ? List.of() : fromJsonList(json);
    }

    public List<WordCard> list(String username) {
        String key = queueKey(username);
        List<String> jsons = redisTemplate.opsForList().range(key, 0, -1);
        redisTemplate.expire(key, GAME_TTL);

        if (jsons == null || jsons.isEmpty()) {
            return new ArrayList<>();
        }

        return jsons.stream()
                .map(this::fromJson)
                .map(this::rehydrate)
                .collect(Collectors.toList());
    }

    private String queueKey(String username) {
        return GAME_KEY_PREFIX + username;
    }

    private String roundKey(String username) {
        return ROUND_KEY_PREFIX + username;
    }

    private CachedCard toCached(WordCard card) {
        String locale = card.getLocale() != null ? card.getLocale().toLanguageTag() : null;

        return new CachedCard(card.getWordId(), card.getTaskId(), card.getUserAnswer(), card.isNewWord(),
                locale);
    }

    private WordCard rehydrate(CachedCard cached) {
        Word word = wordService.getOneById(cached.wordId());
        WordCard card = GameCardFactory.rebuildCard(word, cached.taskId());

        card.setNewWord(cached.newWord());
        card.setLocale(cached.locale() != null ? Locale.forLanguageTag(cached.locale()) : null);

        if (cached.userAnswer() != null) {
            card.setUserAnswer(cached.userAnswer());
        }

        return card;
    }

    private String toJson(CachedCard cachedCard) {
        try {
            return objectMapper.writeValueAsString(cachedCard);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize game card", e);
        }
    }

    private CachedCard fromJson(String json) {
        try {
            return objectMapper.readValue(json, CachedCard.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize game card", e);
        }
    }

    private String toJson(List<Long> wordIds) {
        try {
            return objectMapper.writeValueAsString(wordIds);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize round word ids", e);
        }
    }

    private List<Long> fromJsonList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() { });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize round word ids", e);
        }
    }
}
