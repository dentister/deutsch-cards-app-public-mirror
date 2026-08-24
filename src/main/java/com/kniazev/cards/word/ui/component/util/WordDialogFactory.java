package com.kniazev.cards.word.ui.component.util;

import com.kniazev.cards.word.db.model.word.*;
import com.kniazev.cards.word.db.service.WordService;
import com.kniazev.cards.word.ui.component.dialog.*;

import org.springframework.stereotype.Service;

import javax.annotation.Nullable;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class WordDialogFactory {
    private final WordService wordService;
    
    public WordDialog createWordDialog(Class<? extends Word> wordClass) {
        if (wordClass == Noun.class) {
            return new NounDialog(wordService);
        } else if (wordClass == Verb.class) {
            return new VerbDialog(wordService);
        } else if (wordClass == Adverb.class) {
            return new AdverbDialog(wordService);
        } else if (wordClass == Adjective.class) {
            return new AdjectiveDialog(wordService);
        } else if (wordClass == Phrase.class) {
            return new PhraseDialog(wordService);
        } else {
            throw new IllegalStateException("Unsupported word class");
        }
    }
    
    public WordDialog createWordDialog(@NonNull Word word) {
        if (word instanceof Noun) {
            
            return new NounDialog(wordService, word);
        } else if (word instanceof Verb) {
        
            return new VerbDialog(wordService, word);
        } else if (word instanceof Adverb) {
            
            return new AdverbDialog(wordService, word);
        } else if (word instanceof Adjective) {
            
            return new AdjectiveDialog(wordService, word);
        } else if (word instanceof Phrase) {
            
            return new PhraseDialog(wordService, word);
        } else {
            throw new IllegalStateException("Unsupported word class");
        }
    }
    
    public JsonWordDialog createJsonDialog(@Nullable Word word) {
        return new JsonWordDialog(wordService, word);
    }
}
