package com.kniazev.cards.word.ui.component;

import com.kniazev.cards.word.db.services.IEntityService;
import com.kniazev.cards.word.ui.component.dialog.BaseDialog;
import com.kniazev.cards.word.ui.dto.UIEntity;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UIEntityDialog<T> extends BaseDialog {
    private UIEntity<T> uiEntity;
    private IEntityService<T> entityService;
    
    {
        setModal(false);
        setDraggable(true);
        setWidth("500px");
    }
    
    public UIEntityDialog(String title, UIEntity<T> uiEntity, IEntityService<T> entityService) {
        this.uiEntity = uiEntity;
        this.entityService = entityService;
        
        setHeaderTitle(title);
    }
    
    protected Collection<Component> getDialogComponents() {
        return uiEntity.asComponents();
    }
    
    @Override
    protected ComponentEventListener<ClickEvent<Button>> saveBtnAction() {
        return e -> {
            uiEntity.setParametersToEntity();

            entityService.save(uiEntity.getEntity());

            close();
        };
    }
    
    public static class UIEntityBuilder<T> {
        private T entity;
        private final List<BaseEntityParameter<T, ?>> params = new ArrayList<>();
        
        public UIEntityBuilder<T> withEntity(T entity) {
            this.entity = entity;
            return this;
        }
        
        public UIEntityBuilder<T> withComponent(BaseEntityParameter<T, ?> param) {
            this.params.add(param);
            return this;
        }
        
        public UIEntity<T> build() {
            UIEntity<T> result = new UIEntity<T>(entity, params);
            
            if (entity != null) {
                result.setComponentValuesFromEntity();
            }
            
            return result;
        }
    }
}
