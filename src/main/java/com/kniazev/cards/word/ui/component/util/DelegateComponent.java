package com.kniazev.cards.word.ui.component.util;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import com.kniazev.cards.word.ui.component.atomic.MultipleTextField;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasTheme;
import com.vaadin.flow.component.ScrollOptions;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.shared.HasTooltip;
import com.vaadin.flow.dom.ClassList;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.shared.Registration;

public abstract class DelegateComponent 
extends Component
implements HasStyle, Focusable<MultipleTextField>, HasSize, HasTheme, HasLabel, HasTooltip {
    
    protected final Component delegate;

    public DelegateComponent(Component delegate) {
        this.delegate = delegate;
    }

    @Override
    public void addClassName(String className) {
        delegate.addClassName(className);
    }

    @Override
    public boolean removeClassName(String className) {
        return delegate.removeClassName(className);
    }

    @Override
    public void setClassName(String className) {
        delegate.setClassName(className);
    }

    @Override
    public String getClassName() {
        return delegate.getClassName();
    }

    @Override
    public ClassList getClassNames() {
        return delegate.getClassNames();
    }

    @Override
    public void setClassName(String className, boolean set) {
        delegate.setClassName(className, set);
    }

    @Override
    public Registration addAttachListener(ComponentEventListener<AttachEvent> listener) {
        return delegate.addAttachListener(listener);
    }

    @Override
    public Registration addDetachListener(ComponentEventListener<DetachEvent> listener) {
        return delegate.addDetachListener(listener);
    }

    @Override
    public boolean hasClassName(String className) {
        return delegate.hasClassName(className);
    }

    @Override
    public Style getStyle() {
        return delegate.getStyle();
    }

    @Override
    public void addClassNames(String... classNames) {
        delegate.addClassNames(classNames);
    }

    @Override
    public void removeClassNames(String... classNames) {
        delegate.removeClassNames(classNames);
    }

    @Override
    public Element getElement() {
        return delegate.getElement();
    }

    @Override
    public Optional<Component> getParent() {
        return delegate.getParent();
    }

    @Override
    public Stream<Component> getChildren() {
        return delegate.getChildren();
    }

    @Override
    public Optional<UI> getUI() {
        return delegate.getUI();
    }

    @Override
    public void setId(String id) {
        delegate.setId(id);
    }

    @Override
    public Optional<String> getId() {
        return delegate.getId();
    }

    @Override
    public boolean isAttached() {
        return delegate.isAttached();
    }

    @Override
    public void setVisible(boolean visible) {
        delegate.setVisible(visible);
    }

    @Override
    public boolean isVisible() {
        return delegate.isVisible();
    }

    @Override
    public void onEnabledStateChanged(boolean enabled) {
        delegate.onEnabledStateChanged(enabled);
    }

    @Override
    public String getTranslation(String key, Object... params) {
        return delegate.getTranslation(key, params);
    }

    @Override
    public String getTranslation(Object key, Object... params) {
        return delegate.getTranslation(key, params);
    }

    @Deprecated
    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        return delegate.getTranslation(key, locale, params);
    }

    @Deprecated
    @Override
    public String getTranslation(Object key, Locale locale, Object... params) {
        return delegate.getTranslation(key, locale, params);
    }

    @Override
    public String getTranslation(Locale locale, String key, Object... params) {
        return delegate.getTranslation(locale, key, params);
    }

    @Override
    public String getTranslation(Locale locale, Object key, Object... params) {
        return delegate.getTranslation(locale, key, params);
    }

    @Override
    public void scrollIntoView() {
        delegate.scrollIntoView();
    }

    @Override
    public void scrollIntoView(ScrollOptions scrollOptions) {
        delegate.scrollIntoView(scrollOptions);
    }

    @Override
    public <T> T findAncestor(Class<T> componentType) {
        return delegate.findAncestor(componentType);
    }

    @Override
    public void removeFromParent() {
        delegate.removeFromParent();
    }
    
    

}
