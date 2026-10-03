package com.kniazev.cards.word.ui.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;

import org.modelmapper.spi.DestinationSetter;
import org.modelmapper.spi.TypeSafeSourceGetter;

import java.util.Optional;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class BaseEntityParameter<E, V> {
    protected final TypeSafeSourceGetter<E, V> sourceGetter;
    protected final DestinationSetter<E, V> destinationSetter;
    protected final Component component;
    private final boolean readonly;
    
    public BaseEntityParameter(TypeSafeSourceGetter<E, V> sourceGetter, DestinationSetter<E, V> destinationSetter, Component component) {
        if (component instanceof HasSize hs) {
            hs.setWidthFull();
        }
        
        this.component = component;
        this.destinationSetter = destinationSetter;
        this.sourceGetter = sourceGetter;
        this.readonly = false;
    }
    
    public BaseEntityParameter(TypeSafeSourceGetter<E, V> sourceGetter, Component component) {
        if (component instanceof HasSize hs) {
            hs.setWidthFull(); 
        }
        
        if (component instanceof HasValue hv) {
            hv.setReadOnly(true);
        }
        
        this.component = component;
        this.destinationSetter = null;
        this.sourceGetter = sourceGetter;
        this.readonly = true;
    }
    
    @SuppressWarnings("unchecked")
    public void reset(E entity) {
        if (component instanceof HasValue) {
            HasValue<ValueChangeEvent<V>, V> c = (HasValue<ValueChangeEvent<V>, V>) component;
            
            Optional.ofNullable(sourceGetter.get(entity)).ifPresentOrElse(c::setValue, c::clear);
        } else {
            log.warn("Attempt to operate with component that does not realize HasValue interface");
        }
    }

    @SuppressWarnings("unchecked")
    public void setValueToEntity(E entity) {
        if (component instanceof HasValue) {
            HasValue<ValueChangeEvent<V>, V> c = (HasValue<ValueChangeEvent<V>, V>) component;
            
            if (!c.isReadOnly()) {
                V value = c.getValue();

                destinationSetter.accept(entity, value);
            }
        } else {
            log.warn("Attempt to operate with component that does not realize HasValue interface");
        }
    }

}
