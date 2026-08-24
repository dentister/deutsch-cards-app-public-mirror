package com.kniazev.cards.word.db.service;

import com.kniazev.cards.word.controller.adapter.MappingFunctions;
import com.kniazev.cards.word.db.model.word.*;
import com.kniazev.cards.word.db.model.word.Word.WordType;
import com.kniazev.cards.word.db.repository.WordRepository;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor

@Transactional
@Service
public class WordService implements IEntityService<Word> {
    private static final int DEFAULT_LIMIT = 100;
    
    private final WordRepository<Word> wordRepository;
    private final List<Noun> allNouns = new ArrayList<>();
    private final List<Verb> allVerbs = new ArrayList<>();
    private final List<Adjective> allAdjectives = new ArrayList<>();
    private final List<Adverb> allAdverbs = new ArrayList<>();
    private final List<Phrase> allPhrases = new ArrayList<>();
    
    @PostConstruct
    private void init() {
        Thread cleanerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    allNouns.clear();
                    Thread.sleep(120_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        cleanerThread.setDaemon(true);
        cleanerThread.start();
    }
    
    @Override
    public JpaRepository<Word, Long> getRepository() {
        return wordRepository;
    }
    
    public List<Noun> findAllCachedNouns(boolean reload) {
        if (reload) {
            allNouns.clear();
        }
        
        if (allNouns.isEmpty()) {
            for (Word w : wordRepository.findAllByWordTypeOrderById(WordType.NOUN)) {
                allNouns.add((Noun) w);
            }
        }
        
        return allNouns;
    }
    
    public List<Verb> findAllCachedVerbs(boolean reload) {
        if (reload) {
            allVerbs.clear();
        }
        
        if (allVerbs.isEmpty()) {
            for (Word w : wordRepository.findAllByWordTypeOrderById(WordType.VERB)) {
                allVerbs.add((Verb) w);
            }
        }
        
        return allVerbs;
    }
    
    public List<Adjective> findAllCachedAdjectives(boolean reload) {
        if (reload) {
            allAdjectives.clear();
        }
        
        if (allAdjectives.isEmpty()) {
            for (Word w : wordRepository.findAllByWordTypeOrderById(WordType.ADJECTIVE)) {
                allAdjectives.add((Adjective) w);
            }
        }
        
        return allAdjectives;
    }
    
    public List<Adverb> findAllCachedAdverbs(boolean reload) {
        if (reload) {
            allAdverbs.clear();
        }
        
        if (allAdverbs.isEmpty()) {
            for (Word w : wordRepository.findAllByWordTypeOrderById(WordType.ADVERB)) {
                allAdverbs.add((Adverb) w);
            }
        }
        
        return allAdverbs;
    }
    
    public List<Phrase> findAllCachedPhrases(boolean reload) {
        if (reload) {
            allPhrases.clear();
        }
        
        if (allPhrases.isEmpty()) {
            for (Word w : wordRepository.findAllByWordTypeOrderById(WordType.PHRASE)) {
                allPhrases.add((Phrase) w);
            }
        }
        
        return allPhrases;
    }

    public Optional<Word> findOneByRuAndDe(String ru, String de) {
        return wordRepository.findByRuAndDe(ru, de).stream().findFirst();
    }
    
    public Optional<Word> findOneByDeAndWordType(String de, WordType wordType) {
        return wordRepository.findByDeAndWordType(de, wordType).stream().findFirst();
    }
    
    public List<Word> findByValue(String pattern) {
        return findByValue(pattern, DEFAULT_LIMIT);
    }

    public List<Word> findByValue(String pattern, Integer limit) {
        return StringUtils.isBlank(pattern)
                ? findAll()
                : wordRepository.findByPattern(pattern + "%", PageRequest.of(0, limit));
    }
    
    public List<Word> findByValue(String pattern, Integer limit, Object wordType) {
        return StringUtils.isBlank(pattern)
                ? findAll()
                : wordRepository.findByPattern(pattern + "%", PageRequest.of(0, limit));
    }
    
    public Word createOrNothing(Word patchWord) {
        Optional<Word> persistedWord = findOneByDeAndWordType(patchWord.getDe(), patchWord.getWordType());
        
        return persistedWord.isEmpty() ? save(patchWord) : persistedWord.get();
    }

    public Word createOrRewrite(Word patchWord) {
        Word targetWord = findOneByDeAndWordType(patchWord.getDe(), patchWord.getWordType()).orElse(patchWord);

        MappingFunctions.mergeObjects(patchWord, targetWord);

        return save(targetWord);
    }

    public void deleteAll() {
        wordRepository.deleteAll();
    }

    public List<String> findAllTags() {
        return wordRepository.findAllTags();
    }
}
