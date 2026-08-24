package com.kniazev.cards.word.ui.component.dialog;

import com.kniazev.cards.word.db.model.word.Noun;
import com.kniazev.cards.word.db.model.word.Noun.GenderType;
import com.kniazev.cards.word.db.service.WordService;
import com.kniazev.cards.word.db.model.word.Word;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;

import java.util.ArrayList;
import java.util.Collection;

import static org.apache.commons.lang3.StringUtils.*;

public class NounDialog extends WordDialog {
    
    protected RadioButtonGroup<GenderType> genderRadioButton;
    protected TextField pluralField;    
    
    public NounDialog(WordService wordService) {
        super(wordService);
        
        setHeaderTitle("New Noun");
    }

    public NounDialog(WordService wordService, Word word) {
        super(wordService, word);
        
        setHeaderTitle("Edit Noun");
        
        Noun noun = (Noun) word;
        
        genderRadioButton().setValue(noun.getGender());
        pluralField().setValue(defaultString(noun.getPlural()));
    }
    
    @Override
    protected Collection<Component> getDialogComponents() {
        Collection<Component> dialogComponents = super.getDialogComponents();
        
        dialogComponents.add(pluralField());
        dialogComponents.add(genderRadioButton());
        
        return dialogComponents;
    }
    
    protected RadioButtonGroup<GenderType> genderRadioButton() {
        if (genderRadioButton == null) {
            genderRadioButton = new RadioButtonGroup<>();
            genderRadioButton.setLabel("Gender");
            genderRadioButton.setItems(GenderType.M, GenderType.N, GenderType.F, GenderType.PL);
            genderRadioButton.setWidthFull();
        }
        
        return genderRadioButton;
    }
    
    protected TextField pluralField() {
        if (pluralField == null) {
            pluralField = new TextField("Plural");
            pluralField.setWidthFull();
        }
        
        return pluralField;
    }
    
    @Override
    protected ComponentEventListener<ClickEvent<Button>> saveBtnAction() {
        return e -> {
            Noun noun = word != null ? (Noun) word : new Noun();
            
            noun.setDe(deField().getValue());
            noun.setRu(ruField().getValue());
            noun.setTags(new ArrayList<>(tagsField().getValue()));
            noun.setGender(genderRadioButton().getValue());
            noun.setPlural(defaultIfEmpty(pluralField().getValue(), null));
            noun.setLevel(levelField().getValue());
            
            wordService.createOrRewrite(noun);
            
            close();
        };
    }
    
    
}
