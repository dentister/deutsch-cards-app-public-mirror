package com.kniazev.cards.word.repository;

import com.kniazev.cards.word.model.dictionary.Word;
import com.kniazev.cards.word.model.dictionary.Word.WordType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WordRepository<T extends Word> extends JpaRepository<T, Long> {
    List<T> findByRuAndDe(String ru, String de);
    
    List<T> findByDeAndWordType(String de, WordType wordType);

    @Query("select distinct w from Word w where lower(w.ru) like lower(:pattern) or lower(w.de) like lower(:pattern)")
    List<T> findByPattern(String pattern, Pageable pageRequest);

    List<T> findAllByWordTypeOrderById(WordType wordType);

    @Query(value = "select distinct unnest(tags) val from word w order by val", nativeQuery = true)
    List<String> findAllTags();
}
