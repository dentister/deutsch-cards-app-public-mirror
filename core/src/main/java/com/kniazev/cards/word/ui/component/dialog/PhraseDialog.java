package com.kniazev.cards.word.ui.component.dialog;

import com.kniazev.cards.word.model.dictionary.Phrase;
import com.kniazev.cards.word.model.dictionary.Word;
import com.kniazev.cards.word.service.WordService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;

import java.util.ArrayList;

public class PhraseDialog extends WordDialog {
    
    public PhraseDialog(WordService wordService) {
        super(wordService);
        
        setHeaderTitle("New Phrase");
    }

    public PhraseDialog(WordService wordService, Word word) {
        super(wordService, word);
        
        setHeaderTitle("Edit Phrase");
    }
    
    @Override
    protected ComponentEventListener<ClickEvent<Button>> saveBtnAction() {
        return e -> {
            Phrase adverb = word != null ? (Phrase) word : new Phrase();
            
            adverb.setDe(deField().getValue());
            adverb.setRu(ruField().getValue());
            adverb.setTags(new ArrayList<>(tagsField().getValue()));
            adverb.setLevel(levelField().getValue());
            
            wordService.createOrRewrite(adverb);
            
            close();
        };
    }
}
