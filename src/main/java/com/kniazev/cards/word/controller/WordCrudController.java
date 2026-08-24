package com.kniazev.cards.word.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.kniazev.cards.word.api.delegate.WordCrudApiDelegate;
import com.kniazev.cards.word.api.model.AdjectiveDto;
import com.kniazev.cards.word.api.model.AdverbDto;
import com.kniazev.cards.word.api.model.NounDto;
import com.kniazev.cards.word.api.model.PhraseDto;
import com.kniazev.cards.word.api.model.VerbDto;
import com.kniazev.cards.word.api.model.WordCrudApiV1GetWordRsInner;
import com.kniazev.cards.word.api.model.WordDto;
import com.kniazev.cards.word.controller.adapter.MappingFunctions;
import com.kniazev.cards.word.db.model.word.*;
import com.kniazev.cards.word.db.service.WordService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WordCrudController implements WordCrudApiDelegate {

    private final WordService wordService;

    @Override
    public ResponseEntity<Void> createWord(List<WordDto> rqItems) {
        rqItems.forEach(rq -> {
            Word word = MappingFunctions.map(rq, getTargetClass(rq));
            
            wordService.createOrNothing(word);
        });
        
        return ResponseEntity.accepted().build();
    }

    @Override 
    public ResponseEntity<Void> delete(Long id) {
        wordService.deleteById(id);

        return ResponseEntity.ok().build();
    }
    
    @Override
    public ResponseEntity<List<WordCrudApiV1GetWordRsInner>> search(Integer limit, String value) {
        List<Word> foundWords = wordService.findByValue(value, limit);

        List<WordCrudApiV1GetWordRsInner> foundWordsDto = foundWords.stream()
                .map(e -> new WordCrudApiV1GetWordRsInner().id(e.getId()).word(MappingFunctions.map(e, getTargetClass(e))))
                .collect(Collectors.toList());

        return ResponseEntity.ok(foundWordsDto);
    }
    
    private Class<? extends WordDto> getTargetClass(Word word) {
        return switch (word.getWordType()) {
            case NOUN: yield NounDto.class;
            case VERB: yield VerbDto.class;
            case ADJECTIVE: yield AdjectiveDto.class;
            case ADVERB: yield AdverbDto.class;
            case PHRASE: yield PhraseDto.class;
            default:
                throw new IllegalArgumentException("Unexpected value: " + word.getWordType());
        };
    }
    
    private Class<? extends Word> getTargetClass(WordDto wordDto) {
        return switch (wordDto.getWordType()) {
            case "NOUN": yield Noun.class;
            case "VERB": yield Verb.class;
            case "ADJECTIVE": yield Adjective.class;
            case "ADVERB": yield Adverb.class;
            case "PHRASE": yield Phrase.class;
            default:
                throw new IllegalArgumentException("Unexpected value: " + wordDto.getWordType());
        };
    }
}
