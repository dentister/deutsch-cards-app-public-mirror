package com.kniazev.cards.word.ui.view;

import com.kniazev.cards.word.service.WordService;
import com.kniazev.cards.word.ui.component.AbstractTableView;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox.AutoExpandMode;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractWordListView<T> extends AbstractTableView<T> {
    protected MultiSelectComboBox<String> tagsComboBox;
    protected final WordService wordService;
    
    protected MultiSelectComboBox<String> tagsComboBox() {
        if (tagsComboBox == null) {
            tagsComboBox = new MultiSelectComboBox<>();
            tagsComboBox.setPlaceholder("Tags");
            tagsComboBox.setItems(wordService.findAllTags());
            tagsComboBox.setAutoExpand(AutoExpandMode.BOTH);
            tagsComboBox.addValueChangeListener(e -> refreshData());
        }
        
        return tagsComboBox;
    }

}
