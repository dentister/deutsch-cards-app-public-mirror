package com.kniazev.cards.word.ui.component.dialog;

import com.kniazev.cards.word.db.model.word.Verb;

import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.db.model.word.Noun.GenderType;
import com.kniazev.cards.word.db.services.WordService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;

import static org.apache.commons.lang3.StringUtils.*;

import java.util.ArrayList;
import java.util.Collection;

public class VerbDialog extends WordDialog {
    
    protected RadioButtonGroup<GenderType> genderRadioButton;
    protected TextField pluralField;
    protected TextField ichField;
    protected TextField duField;
    protected TextField erField;
    protected TextField wirField;
    protected TextField ihrField;
    protected TextField sieField;
    protected TextField partizip2Field;
    protected TextField prefixField;
    protected TextField rootVerbField;
    protected TextField notesField;
    
    public VerbDialog(WordService wordService) {
        super(wordService);
        
        setHeaderTitle("New Verb");
    }

    public VerbDialog(WordService wordService, Word word) {
        super(wordService, word);
        
        setHeaderTitle("Edit Verb");
        
        Verb verb = (Verb) word;
        
        ichField().setValue(verb.getIch());
        duField().setValue(verb.getDu());
        erField().setValue(verb.getEr());
        wirField().setValue(verb.getWir());
        ihrField().setValue(verb.getIhr());
        sieField().setValue(verb.getSie());
        partizip2Field().setValue(verb.getPartizip2());
        rootVerbField().setValue(verb.getRootVerb());
        
        prefixField().setValue(defaultString(verb.getPrefix()));
    }
    
    @Override
    protected Collection<Component> getDialogComponents() {
        Collection<Component> dialogComponents = super.getDialogComponents();
        
        dialogComponents.add(ichField());
        dialogComponents.add(duField());
        dialogComponents.add(erField());
        dialogComponents.add(wirField());
        dialogComponents.add(ihrField());
        dialogComponents.add(sieField());
        dialogComponents.add(partizip2Field());
        dialogComponents.add(prefixField());
        dialogComponents.add(rootVerbField());
        
        return dialogComponents;
    }
    
    protected TextField ichField() {
        if (ichField == null) {
            ichField = new TextField("Ich");
            ichField.setWidthFull();
        }
        
        return ichField;
    }
    
    protected TextField duField() {
        if (duField == null) {
            duField = new TextField("Du");
            duField.setWidthFull();
        }
        
        return duField;
    }
    
    protected TextField erField() {
        if (erField == null) {
            erField = new TextField("Er/Sie/Es");
            erField.setWidthFull();
        }
        
        return erField;
    }
    
    protected TextField wirField() {
        if (wirField == null) {
            wirField = new TextField("Wir");
            wirField.setWidthFull();
        }
        
        return wirField;
    }
    
    protected TextField ihrField() {
        if (ihrField == null) {
            ihrField = new TextField("Ihr");
            ihrField.setWidthFull();
        }
        
        return ihrField;
    }
    
    protected TextField sieField() {
        if (sieField == null) {
            sieField = new TextField("Sie/sie");
            sieField.setWidthFull();
        }
        
        return sieField;
    }
    
    protected TextField partizip2Field() {
        if (partizip2Field == null) {
            partizip2Field = new TextField("Partizip II");
            partizip2Field.setWidthFull();
        }
        
        return partizip2Field;
    }
    
    protected TextField prefixField() {
        if (prefixField == null) {
            prefixField = new TextField("Prefix");
            prefixField.setWidthFull();
        }
        
        return prefixField;
    }
    
    protected TextField rootVerbField() {
        if (rootVerbField == null) {
            rootVerbField = new TextField("Root");
            rootVerbField.setWidthFull();
        }
        
        return rootVerbField;
    }
    
    protected TextField notesField() {
        if (notesField == null) {
            notesField = new TextField("Root");
            notesField.setWidthFull();
        }
        
        return notesField;
    }
    
    @Override
    protected ComponentEventListener<ClickEvent<Button>> saveBtnAction() {
        return e -> {
            Verb verb = word != null ? (Verb) word : new Verb();
            
            verb.setDe(deField().getValue());
            verb.setRu(ruField().getValue());
            verb.setTags(new ArrayList<>(tagsField().getValue()));
            verb.setIch(ichField().getValue());
            verb.setDu(duField().getValue());
            verb.setEr(erField().getValue());
            verb.setWir(wirField().getValue());
            verb.setIhr(ihrField().getValue());
            verb.setSie(sieField().getValue());
            verb.setPartizip2(partizip2Field().getValue());
            verb.setPrefix(defaultIfEmpty(prefixField().getValue(), null));
            verb.setRootVerb(rootVerbField().getValue());
            verb.setNotes(notesField().getValue());
            
            wordService.createOrRewrite(verb);
            
            close();
        };
    }
}
