package com.kniazev.cards.word.ui.component.dialog;

import com.kniazev.cards.word.db.model.word.Adverb;
import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.db.service.WordService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;

import java.util.ArrayList;

public class AdverbDialog extends WordDialog {
    
    public AdverbDialog(WordService wordService) {
        super(wordService);
        
        setHeaderTitle("New Adverb");
    }

    public AdverbDialog(WordService wordService, Word word) {
        super(wordService, word);
        
        setHeaderTitle("Edit Adverb");
    }
    
    @Override
    protected ComponentEventListener<ClickEvent<Button>> saveBtnAction() {
        return e -> {
            Adverb adverb = word != null ? (Adverb) word : new Adverb();
            
            adverb.setDe(deField().getValue());
            adverb.setRu(ruField().getValue());
            adverb.setTags(new ArrayList<>(tagsField().getValue()));
            
            wordService.createOrRewrite(adverb);
            
            close();
        };
    }
}
