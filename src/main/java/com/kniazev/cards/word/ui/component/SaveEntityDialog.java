package com.kniazev.cards.word.ui.component;

import java.util.Optional;

import com.kniazev.cards.word.ui.dto.UIEntity;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class SaveEntityDialog<T> extends Dialog {
    
    private VerticalLayout dialogLayout;
    private UIEntity<T> uiEntity;
    private T entity;
    
    public SaveEntityDialog() {
        setModal(false);
        setDraggable(true);
        setWidth("500px");
        add(dialogLayout());
        
        getFooter().add(
                new Button("Ok", e -> {
                    entity = uiEntity.setParametersToEntity();
                    close();
                }),
                new Button("Cancel", e -> close()) );
    }
    
    public Optional<T> getResultEntity() {
        return Optional.ofNullable(entity);
    }
    
    @Override
    public void open() {
        throw new UnsupportedOperationException();
    }
    
    public void open(UIEntity<T> uiEntity, String title) {
        this.uiEntity = uiEntity;
        
        dialogLayout().removeAll();
        dialogLayout().add(uiEntity.asComponents());
        
        setHeaderTitle(title);
        
        super.open();
    }
    
    private VerticalLayout dialogLayout() {
        if (dialogLayout == null) {
            dialogLayout = new VerticalLayout();
            dialogLayout.setPadding(false);
            dialogLayout.setSpacing(false);
            dialogLayout.setHeightFull();
        }

        return dialogLayout;
    }
}
