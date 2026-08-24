package com.kniazev.cards.word.game;

import com.kniazev.cards.word.ai.WordExampleService;
import com.kniazev.cards.word.db.model.User;
import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.db.model.word.Word.WordLevel;
import com.kniazev.cards.word.db.model.word.Word.WordType;
import com.kniazev.cards.word.db.service.UserService;
import com.kniazev.cards.word.db.service.WordScoreService;
import com.kniazev.cards.word.db.service.WordService;
import com.kniazev.cards.word.error.exception.GameNotStartedException;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Locale;
import java.util.stream.Collectors;

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
    private final GameCache gameCache;

    public List<WordCard> getGameWords(String username) {
        return gameCache.list(username);
    }

    public WordCard getNextTask(String username, Locale locale) {
        GameConfiguration gameConfiguration = null;

        if (gameCache.isEmpty(username)) {
            restartGame(username, gameConfiguration, locale);
        }

        return gameCache.peek(username);
    }

    public WordCard checkUserAnswer(String username, String userAnswer) {
        WordCard wordCard = Optional.ofNullable(gameCache.poll(username))
                .orElseThrow(GameNotStartedException::new);

        wordCard.setUserAnswer(userAnswer);

        Word word = wordService.getOneById(wordCard.getWordId());
        User user = userService.getOneByUsername(username);

        wordScoreService.saveScore(user, word, wordCard.isRightAnswered());

        if (!wordCard.isRightAnswered()) {
            WordCard retryCard = GameCardFactory.buildCard(word);
            retryCard.setLocale(wordCard.getLocale());

            gameCache.push(username, retryCard);
        }

        return wordCard;
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

    public void restartGame(String username, GameConfiguration gameConfiguration, Locale locale) {
        User user = userService.getOneByUsername(username);

        if (gameConfiguration != null) {
            user.getUserSettings().setGameConfiguration(gameConfiguration);

            userService.save(user);
        } else {
            gameConfiguration = user.getUserSettings().getGameConfiguration();
        }

        List<Long> specificWordIds = gameConfiguration.getSpecificWordIds();

        if (specificWordIds != null && !specificWordIds.isEmpty()) {
            restartGame(username, null, null, null, specificWordIds, locale);
        } else {
            restartGame(username, gameConfiguration.getMaxLevel(), gameConfiguration.getWordType(),
                    gameConfiguration.getTags(), null, locale);
        }
    }

    private void restartGame(String username, WordLevel maxLevel, WordType wordType, Collection<String> tags,
            Collection<Long> specificWordIds, Locale locale) {
        gameCache.clear(username);

        User user = userService.getOneByUsername(username);
        List<WordCard> cards = pullNewWords(user.getId(), maxLevel, wordType, tags, specificWordIds);

        for (WordCard card : cards) {
            card.setNewWord(wordScoreService.isNewWord(user, card.getWord()));
            card.setLocale(locale);

            wordExampleService.ensureExampleAndEnglishExist(card.getWord());

            gameCache.push(username, card);
        }

        gameCache.saveRoundWordIds(username, cards.stream().map(WordCard::getWordId).toList());
    }

    public List<Word> getLastRoundWords(String username) {
        List<Long> wordIds = gameCache.getRoundWordIds(username);

        return wordIds.isEmpty() ? List.of() : wordService.findByIds(wordIds);
    }

    private List<WordCard> pullNewWords(Long userId, WordLevel level, WordType wordType, Collection<String> tags, Collection<Long> specificWordIds) {
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

}
