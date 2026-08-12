package com.kniazev.cards.word.db.repository;

import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.db.model.word.WordScore;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;


public interface WordScoreRepository extends JpaRepository<WordScore, Long> {
    
    @Query(value = "SELECT word_id FROM get_word_scores(:userId)", nativeQuery = true)
    List<Long> findWordScores(@Param("userId") Long userId);
    
    @Query(value = "SELECT word_id FROM get_word_scores(:userId, :maxLevel, :wordType, :tags, :wordIds)", nativeQuery = true)
    List<Long> findWordScores(@Param("userId") Long userId, @Param("maxLevel") String maxLevel, @Param("wordType") String wordType, @Param("tags") String[] tags, @Param("wordIds") Long[] wordIds);
    
    Optional<WordScore> findFirstByUserAndWord(UserDetails user, Word word);

}
