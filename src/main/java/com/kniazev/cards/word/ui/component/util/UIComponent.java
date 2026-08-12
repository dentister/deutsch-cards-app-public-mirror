package com.kniazev.cards.word.ui.component.util;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Target;

@Target(FIELD) 
public @interface UIComponent {

    String label() default "";
    
    int order() default 0;

    boolean multiple() default false;
    
    ValueType type() default ValueType.TEXT;
    
    Class<?> listValue() default Object.class;
    
    enum ValueType {
        TEXT, PASSWORD, LIST;
    }
}
