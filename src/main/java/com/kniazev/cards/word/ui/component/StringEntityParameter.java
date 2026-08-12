package com.kniazev.cards.word.ui.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.textfield.TextField;

import org.modelmapper.spi.DestinationSetter;
import org.modelmapper.spi.TypeSafeSourceGetter;

import java.util.Optional;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StringEntityParameter<E, V>  extends BaseEntityParameter<E, V> {
    
    private final Function<String, V> s2v;

    public StringEntityParameter(TypeSafeSourceGetter<E, V> sourceGetter, DestinationSetter<E, V> destinationSetter, Component component, Function<String, V> s2v ) {
        super(sourceGetter, destinationSetter, component);
        
        this.s2v = s2v;
    } 
    
    public StringEntityParameter(TypeSafeSourceGetter<E, V> destinationSetter, TextField textField, Function<String, V> s2v) {
        super(destinationSetter, textField);
        
        this.s2v = s2v;
    }

    @SuppressWarnings("unchecked")
    public void reset(E entity) {
        if (component instanceof HasValue) {
            HasValue<ValueChangeEvent<String>, String> c = (HasValue<ValueChangeEvent<String>, String>) component;
            
            Optional.ofNullable(sourceGetter.get(entity)).map(Object::toString).ifPresentOrElse(c::setValue, c::clear);
        } else {
            log.warn("Attempt to operate with component that does not realize HasValue interface");
        }
    }

    @SuppressWarnings("unchecked")
    public void setValueToEntity(E entity) {
        if (component instanceof HasValue) {
            HasValue<ValueChangeEvent<String>, String> c = (HasValue<ValueChangeEvent<String>, String>) component;
            
            if (!c.isReadOnly()) {
                V value = Optional.ofNullable(c.getValue()).map(s2v).orElse(null);
                
                destinationSetter.accept(entity, value);
            }
        } else {
            log.warn("Attempt to operate with component that does not realize HasValue interface");
        }
    }

}
