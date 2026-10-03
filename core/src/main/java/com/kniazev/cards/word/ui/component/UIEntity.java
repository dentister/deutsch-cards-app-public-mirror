package com.kniazev.cards.word.ui.component;

import com.vaadin.flow.component.Component;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class UIEntity<T> {
    private final T entity;
    private final List<BaseEntityParameter<T, ?>> params;
    
    public static <T> UIEntity<T> of(@NonNull T obj, @NonNull List<BaseEntityParameter<T, ?>> params) {
        return new UIEntity<>(obj, params);
    }

    public List<Component> asComponents() {
        return params.stream().map(BaseEntityParameter::getComponent).toList();
    }

    public T setComponentValuesFromEntity() {
        if (entity != null) {
            for (BaseEntityParameter<T, ?> ep : params) {
                ep.reset(entity);
            }
        }
        
        return entity;
    }

    public T setParametersToEntity() {
        for (BaseEntityParameter<T, ?> ep : params) {
            ep.setValueToEntity(entity);
        }

        return entity;
    }
    
    public T getEntity() {
        return entity;
    }
}
