package com.kniazev.cards.word.db.services;

import com.kniazev.cards.word.db.model.User;
import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.db.model.word.WordScore;
import com.kniazev.cards.word.db.model.word.Word.WordLevel;
import com.kniazev.cards.word.db.model.word.Word.WordType;
import com.kniazev.cards.word.db.repository.WordScoreRepository;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class WordScoreService implements IEntityService<WordScore> {
    private static final int MAX_NEW_WORD_SCORE = 1;

    private final WordScoreRepository repository;

    @Override
    public JpaRepository<WordScore, Long> getRepository() {
        return repository;
    }
    
    public List<Long> findRandomLowScoredWordIds(Long userId) {
        return repository.findWordScores(userId);
    }
    
    public List<Long> findRandomLowScoredWordIds(Long userId, WordLevel level, WordType wordType, Collection<String> tags,
            Collection<Long> specificWordIds) {
        String spWordType = wordType == null ? null : wordType.toString();
        String spLevel = level == null ? null : level.toString();
        String[] spTags = CollectionUtils.isEmpty(tags) ? null : tags.toArray(new String[0]);
        Long[] spWordIds = CollectionUtils.isEmpty(specificWordIds) ? null : specificWordIds.toArray(new Long[0]);

        return repository.findWordScores(userId, spLevel, spWordType, spTags, spWordIds);
    }
    
    public boolean isNewWord(User user, Word word) {
        return repository.findFirstByUserAndWord(user, word)
                .map(ws -> ws.getScore() <= MAX_NEW_WORD_SCORE)
                .orElse(true); // no row = never answered correctly = new
    }

    public void saveScore(User user, Word word, boolean isRight) {
        WordScore wordScore = repository.findFirstByUserAndWord(user, word)
                .orElse(WordScore.builder().user(user).word(word).build());
        
        wordScore.incrementAnchor();
        
        if (isRight) {
            wordScore.incrementScore();
        }
        
        repository.saveAndFlush(wordScore);
    }
}
