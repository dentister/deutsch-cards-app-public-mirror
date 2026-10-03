package com.kniazev.cards.word.ui;

import com.kniazev.cards.word.model.dictionary.*;
import com.kniazev.cards.word.service.WordService;
import com.kniazev.cards.word.ui.component.dialog.*;

import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class WordDialogFactory {
    private final WordService wordService;
    
    public WordDialog createWordDialog(Class<? extends Word> wordClass) {
        if (wordClass == Noun.class)      return new NounDialog(wordService);
        if (wordClass == Verb.class)      return new VerbDialog(wordService);
        if (wordClass == Adverb.class)    return new AdverbDialog(wordService);
        if (wordClass == Adjective.class) return new AdjectiveDialog(wordService);
        if (wordClass == Phrase.class)    return new PhraseDialog(wordService);
        
        throw new IllegalStateException("Unsupported word class: " + wordClass.getSimpleName());
    }
    
    public WordDialog createWordDialog(@NonNull Word word) {
        return switch (word) {
            case Noun n      -> new NounDialog(wordService, n);
            case Verb v      -> new VerbDialog(wordService, v);
            case Adverb a    -> new AdverbDialog(wordService, a);
            case Adjective a -> new AdjectiveDialog(wordService, a);
            case Phrase p    -> new PhraseDialog(wordService, p);
            default          -> throw new IllegalStateException("Unsupported word class: " + word.getClass().getSimpleName());
        };
    }
    
    public JsonWordDialog createJsonDialog(@Nullable Word word) {
        return new JsonWordDialog(wordService, word);
    }
}
