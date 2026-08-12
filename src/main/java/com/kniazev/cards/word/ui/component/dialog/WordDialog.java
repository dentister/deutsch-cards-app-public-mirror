package com.kniazev.cards.word.ui.component.dialog;

import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.db.model.word.Word.WordLevel;
import com.kniazev.cards.word.db.services.WordService;
import com.kniazev.cards.word.ui.component.atomic.MultipleTextField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;

import java.util.*;

public abstract class WordDialog extends BaseDialog {
    protected WordService wordService;
    protected Word word;
    
    protected TextField deField;
    protected TextField ruField;
    protected Select<WordLevel> levelField; 
    protected MultipleTextField tagsField;
    
    {
        setModal(false);
        setDraggable(true);
        setWidth("500px");
        setMaxHeight("700px");
        setMinHeight("300px");
    }
    
    public WordDialog(WordService wordService) {
        this.wordService = wordService;
    }
    
    public WordDialog(WordService wordService, Word word) {
        this.word = word;
        this.wordService = wordService;
        
        ruField().setValue(word.getRu());
        deField().setValue(word.getDe());
        levelField().setValue(word.getLevel());
        
        if (word.getTags() != null) {
            tagsField().setValue(new HashSet<>(word.getTags()));
        }
    }
    
    @Override
    protected Collection<Component> getDialogComponents() {
        List<Component> components = new ArrayList<>();
        
        components.add(deField());
        components.add(ruField());
        components.add(levelField());
        components.add(tagsField());
        
        return components;
    }
    
    protected TextField deField() {
        if (deField == null) {
            deField = new TextField("DE");
            deField.setWidthFull();
        }
        
        return deField;
    }
    
    protected TextField ruField() {
        if (ruField == null) {
            ruField = new TextField("RU");
            ruField.setWidthFull();
        }
        
        return ruField;
    }
    
    protected MultipleTextField tagsField() {
        if (tagsField == null) {
            tagsField = new MultipleTextField("Tags");
            tagsField.setWidthFull();
        }
        
        return tagsField;
    }
    
    protected Select<WordLevel> levelField() {
        if (levelField == null) {
            levelField = new Select<>();
            levelField.setLabel("Sort by");
            levelField.setItems(WordLevel.values());
            levelField.setValue(WordLevel.C2);
            levelField.setWidthFull();
        }
        
        return levelField;
    }
}
