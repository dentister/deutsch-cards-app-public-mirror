package com.kniazev.cards.word.service;

import com.kniazev.cards.word.model.MappingFunctions;
import com.kniazev.cards.word.model.dictionary.*;
import com.kniazev.cards.word.model.dictionary.Word.WordType;
import com.kniazev.cards.word.repository.WordRepository;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor

@Transactional
@Service
public class WordService implements IEntityService<Word> {
    private static final int DEFAULT_LIMIT = 100;
    
    private final WordRepository<Word> wordRepository;
    private final List<Noun> allNouns = new CopyOnWriteArrayList<>();
    private final List<Verb> allVerbs = new CopyOnWriteArrayList<>();
    private final List<Adjective> allAdjectives = new CopyOnWriteArrayList<>();
    private final List<Adverb> allAdverbs = new CopyOnWriteArrayList<>();
    private final List<Phrase> allPhrases = new CopyOnWriteArrayList<>();
    
    @Scheduled(fixedDelay = 120_000)
    public void evictNounsCache() {
        allNouns.clear();
    }
    
    @Override
    public JpaRepository<Word, Long> getRepository() {
        return wordRepository;
    }
    
    public List<Noun> findAllCachedNouns(boolean reload) {
        return getCachedWords(WordType.NOUN, allNouns, reload);
    }
    
    public List<Verb> findAllCachedVerbs(boolean reload) {
        return getCachedWords(WordType.VERB, allVerbs, reload);
    }
    
    public List<Adjective> findAllCachedAdjectives(boolean reload) {
        return getCachedWords(WordType.ADJECTIVE, allAdjectives, reload);
    }
    
    public List<Adverb> findAllCachedAdverbs(boolean reload) {
        return getCachedWords(WordType.ADVERB, allAdverbs, reload);
    }
    
    public List<Phrase> findAllCachedPhrases(boolean reload) {
        return getCachedWords(WordType.PHRASE, allPhrases, reload);
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
        PageRequest page = PageRequest.of(0, limit);

        return StringUtils.isBlank(pattern)
                ? wordRepository.findAll(page).getContent() 
                : wordRepository.findByPattern(pattern + "%", page);
    }

    public Word createOrNothing(Word patchWord) {
        return findOneByDeAndWordType(patchWord.getDe(), patchWord.getWordType())
                .orElseGet(() -> save(patchWord));
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
    
    @SuppressWarnings("unchecked")
    private <T extends Word> List<T> getCachedWords(WordType type, List<T> cache, boolean reload) {
        if (reload) {
            cache.clear();
        }
        
        if (cache.isEmpty()) {
            synchronized (cache) {
                if (cache.isEmpty()) {
                    List<T> wordsFromDb = wordRepository.findAllByWordTypeOrderById(type)
                            .stream()
                            .map(w -> (T) w)
                            .toList();
                    cache.addAll(wordsFromDb);
                }
            }
        }
        return cache;
    }
}
