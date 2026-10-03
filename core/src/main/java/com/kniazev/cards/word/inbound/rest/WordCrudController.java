package com.kniazev.cards.word.inbound.rest;

import com.kniazev.cards.word.api.delegate.WordCrudApiDelegate;
import com.kniazev.cards.word.api.model.*;
import com.kniazev.cards.word.model.MappingFunctions;
import com.kniazev.cards.word.model.dictionary.*;
import com.kniazev.cards.word.service.WordService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

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
        
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override 
    public ResponseEntity<Void> delete(Long id) {
        wordService.deleteById(id);

        return ResponseEntity.noContent().build();
    }
    
    @Override
    public ResponseEntity<List<WordCrudApiV1GetWordRsInner>> search(Integer limit, String value) {
        List<Word> foundWords = wordService.findByValue(value, limit);

        List<WordCrudApiV1GetWordRsInner> foundWordsDto = foundWords.stream()
                .map(e -> new WordCrudApiV1GetWordRsInner().id(e.getId()).word(MappingFunctions.map(e, getTargetClass(e))))
                .toList();

        return ResponseEntity.ok(foundWordsDto);
    }
    
    private Class<? extends WordDto> getTargetClass(Word word) {
        return switch (word.getWordType()) {
            case NOUN      -> NounDto.class;
            case VERB      -> VerbDto.class;
            case ADJECTIVE -> AdjectiveDto.class;
            case ADVERB    -> AdverbDto.class;
            case PHRASE    -> PhraseDto.class;
            default        -> throw new IllegalArgumentException("Unexpected value: " + word.getWordType());
        };
    }
    
    private Class<? extends Word> getTargetClass(WordDto wordDto) {
        String wordType = wordDto.getWordType() != null ? wordDto.getWordType().toUpperCase() : "";
        
        return switch (wordType) {
            case "NOUN"      -> Noun.class;
            case "VERB"      -> Verb.class;
            case "ADJECTIVE" -> Adjective.class;
            case "ADVERB"    -> Adverb.class;
            case "PHRASE"    -> Phrase.class;
            default          -> throw new IllegalArgumentException("Unexpected value: " + wordDto.getWordType());
        };
    }
}
