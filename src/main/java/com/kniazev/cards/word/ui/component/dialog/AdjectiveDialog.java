package com.kniazev.cards.word.ui.component.dialog;

import com.kniazev.cards.word.db.model.word.Adjective;
import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.db.service.WordService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;

import java.util.ArrayList;

public class AdjectiveDialog extends WordDialog {
    
    public AdjectiveDialog(WordService wordService) {
        super(wordService);
        
        setHeaderTitle("New Adjective");
    }

    public AdjectiveDialog(WordService wordService, Word word) {
        super(wordService, word);
        
        setHeaderTitle("Edit Adjective");
    }
    
    @Override
    protected ComponentEventListener<ClickEvent<Button>> saveBtnAction() {
        return e -> {
            Adjective adjective = word != null ? (Adjective) word : new Adjective();
            
            adjective.setDe(deField().getValue());
            adjective.setRu(ruField().getValue());
            adjective.setTags(new ArrayList<>(tagsField().getValue()));
            
            wordService.createOrRewrite(adjective);
            
            close();
        };
    }
}
