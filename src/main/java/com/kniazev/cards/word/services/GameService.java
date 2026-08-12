package com.kniazev.cards.word.services;

import com.kniazev.cards.word.ai.WordExampleService;
import com.kniazev.cards.word.db.model.User;
import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.db.model.word.Word.WordLevel;
import com.kniazev.cards.word.db.model.word.Word.WordType;
import com.kniazev.cards.word.db.services.UserService;
import com.kniazev.cards.word.db.services.WordScoreService;
import com.kniazev.cards.word.db.services.WordService;
import com.kniazev.cards.word.error.handler.exception.GameNotStartedException;
import com.kniazev.cards.word.game.WordCard;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor

@Service
public class GameService {
    private final WordService wordService;
    private final WordScoreService wordScoreService;
    private final UserService userService;
    private final WordExampleService wordExampleService;
    private final Map<String, Queue<WordCard>> games = new ConcurrentHashMap<>();

    public WordCard getNextTask(String username) {
        Queue<WordCard> cardsList = getWordCardsQueue(username);
        GameConfiguration gameConfiguration = null;

        if (cardsList.isEmpty()) {
            restartGame(username, gameConfiguration);
        }

        return cardsList.peek();
    }

    public WordCard checkUserAnswer(String username, String userAnswer) {
        Queue<WordCard> cardsList = games.get(username);

        WordCard wordCard = Optional.ofNullable(getWordCardsQueue(username).poll())
                .orElseThrow(GameNotStartedException::new);

        wordCard.setUserAnswer(userAnswer);

        Word word = wordService.getOneById(wordCard.getWordId());
        User user = userService.getOneByUsername(username);

        wordScoreService.saveScore(user, word, wordCard.isRightAnswered());

        if (!wordCard.isRightAnswered()) {
            cardsList.add(GameCardFactory.buildCard(word));
        }

        return wordCard;
    }

    private List<WordCard> pullNewWords(Long userId, WordLevel level, WordType wordType, Collection<String> tags,
            Collection<Long> specificWordIds) {
        List<WordCard> list = new ArrayList<>();

        List<Long> scoredWordIds = wordScoreService.findRandomLowScoredWordIds(userId, level, wordType, tags, specificWordIds);

        Map<Long, Word> words = wordService.findByIds(scoredWordIds)
                .stream()
                .collect(Collectors.toMap(Word::getId, w -> w));

        for (Long id : scoredWordIds) {
            WordCard card = GameCardFactory.buildCard(words.get(id));

            list.add(card);
        }

        log.info("Pulled the following words [userId={},\n wordIds={}]", userId, scoredWordIds);

        return list;
    }

    public void restartGame(String username) {
        restartGame(username, null);
    }

    public void restartGame(String username, GameConfiguration gameConfiguration) {
        User user = userService.getOneByUsername(username);

        if (gameConfiguration != null) {
            user.getUserSettings().setGameConfiguration(gameConfiguration);

            userService.save(user);
        } else {
            gameConfiguration = user.getUserSettings().getGameConfiguration();
        }

        List<Long> specificWordIds = gameConfiguration.getSpecificWordIds();

        if (specificWordIds != null && !specificWordIds.isEmpty()) {
            // "Only selected" mode: the hand-picked set overrides level/type/tag filters.
            restartGame(username, null, null, null, specificWordIds);
        } else {
            restartGame(username, gameConfiguration.getMaxLevel(), gameConfiguration.getWordType(),
                    gameConfiguration.getTags(), null);
        }
    }

    public GameConfiguration getGameConfiguration(String username) {
        User user = userService.getOneByUsername(username);

        return user.getUserSettings().getGameConfiguration();
    }

    public void saveGameConfiguration(String username, GameConfiguration gameConfiguration) {
        User user = userService.getOneByUsername(username);
        user.getUserSettings().setGameConfiguration(gameConfiguration);
        userService.save(user);
    }

    private void restartGame(String username, WordLevel maxLevel, WordType wordType, Collection<String> tags,
            Collection<Long> specificWordIds) {
        Queue<WordCard> cardsList = getWordCardsQueue(username);

        cardsList.clear();

        User user = userService.getOneByUsername(username);

        for (WordCard card : pullNewWords(user.getId(), maxLevel, wordType, tags, specificWordIds)) {
            card.setNewWord(wordScoreService.isNewWord(user, card.getWord()));
            wordExampleService.ensureExampleCached(card.getWord());
            cardsList.offer(card);
        }
    }

    private Queue<WordCard> getWordCardsQueue(String username) {
        return games.computeIfAbsent(username, k -> new LinkedList<>());
    }

    public List<WordCard> getGameWords(String username) {
        return new ArrayList<>(getWordCardsQueue(username));
    }

    public List<String> findAllTags() {
        return wordService.findAllTags();
    }
}
