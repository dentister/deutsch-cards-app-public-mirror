package com.kniazev.cards.word.db.repository;

import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.db.model.word.Word.WordType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WordRepository<T extends Word> extends JpaRepository<T, Long> {
    List<T> findByRuAndDe(String ru, String de);
    
    List<T> findByDeAndWordType(String de, WordType wordType);

    @Query(value = "select distinct w from Word w "
            + "where w.ru like :pattern or w.de like :pattern " )
    List<Word> findByPattern(String pattern, Pageable pageRequest);

    List<Word> findAllByWordTypeOrderById(WordType wordType);

    @Query(value = "select distinct unnest(tags) val from word w order by val", nativeQuery = true)
    List<String> findAllTags();
}
