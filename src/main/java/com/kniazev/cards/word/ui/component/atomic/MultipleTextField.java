package com.kniazev.cards.word.ui.component.atomic;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.HasTooltip;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.Style;

import java.util.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import software.xdev.vaadin.chips.ChipComponent;

public class MultipleTextField
extends AbstractCompositeField<VerticalLayout, MultipleTextField, Set<String>>
implements HasStyle, Focusable<MultipleTextField>, HasSize, HasTheme, HasLabel, HasTooltip {

    protected FlexLayout chipsContainer = new FlexLayout();
    private TextField textField = new TextField();

    private final List<ChipComponent<String>> selectedComponents = new ArrayList<>();

    public MultipleTextField() {
        this(new HashSet<>());
    }

    public MultipleTextField(final Set<String> defaultValue) {
        super(defaultValue);

        this.initUI();
        this.initListeners();
    }

    public MultipleTextField(final String label) {
        this();
        
        setLabel(label);
    }

    public MultipleTextField(final String label, final Set<String> defaultValue) {
        this(defaultValue);
        
        setLabel(label);
    }

    @Override
    public String getLabel() {
        return this.textField.getLabel();
    }

    @Override
    public void setLabel(final String label) {
        this.textField.setLabel(label);
    }

    public String getPlaceholder() {
        return this.textField.getPlaceholder();
    }

    public void setPlaceholder(final String placeholder) {
        this.textField.setPlaceholder(placeholder);
    }

    public void typeText(String text) {
        this.textField.setValue(text);
    }

    @Override
    public void setValue(final Set<String> value) {
        if (CollectionUtils.isEmpty(value)) {
            return;
        }

        super.setValue(value);
    }

    @Override
    public Set<String> getValue() {
        return super.getValue();
    }

    @Override
    public void setReadOnly(final boolean readOnly) {
        super.setReadOnly(readOnly);

        this.textField.setReadOnly(readOnly);
        this.selectedComponents.forEach(comp -> comp.setReadonly(readOnly));
    }

    protected void initUI() {
        final Style chipsContainerStyle = this.chipsContainer.getStyle();
        chipsContainerStyle.set("flex-flow", "wrap");
        chipsContainerStyle.set("flex-direction", "row");

        this.textField.setWidthFull();

        this.getContent().setPadding(false);
        this.getContent().setSpacing(false);
        this.setSizeUndefined();

        this.getContent().add(this.textField, this.chipsContainer);
    }

    protected void initListeners() {
        this.textField.addKeyPressListener(Key.ENTER, event -> {
            if (StringUtils.isNotBlank(textField.getValue())) {
                addItem(textField.getValue(), true);
            }

            textField.setValue("");
        });
    }

    @Override
    protected void setPresentationValue(final Set<String> newPresentationValue) {
        this.selectedComponents.removeIf(comp -> !newPresentationValue.contains(comp.getItem()));

        final Collection<String> existingValues = this.selectedComponents.stream().map(ChipComponent::getItem).toList();

        newPresentationValue.stream().filter(v -> !existingValues.contains(v)).map(item -> {
            final ChipComponent<String> chipComponent = new ChipComponent<String>(item);

            chipComponent.setItemLabelGenerator(i -> i.toString());

            chipComponent.addBtnDeleteClickListener(ev -> {
                if (this.isReadOnly()) {
                    return;
                }

                this.removeItem(item, ev.isFromClient());
            });

            return chipComponent;
        }).forEach(this.selectedComponents::add);

        this.updateUI();
    }

    protected void addItem(final String item, final boolean isFromClient) {
        final Set<String> values = new LinkedHashSet<>(this.getValue());
        values.add(item);
        this.updateValues(values, isFromClient);
    }

    protected void removeItem(final String item, final boolean isFromClient) {
        final Set<String> values = new LinkedHashSet<>(this.getValue());
        values.remove(item);
        this.updateValues(values, isFromClient);
    }

    protected void updateValues(final Set<String> newValues, final boolean isFromClient) {
        final Set<String> oldValue = this.getValue();
        this.setModelValue(newValues, isFromClient);

        if (!this.valueEquals(oldValue, newValues)) {
            this.setPresentationValue(newValues);
        }
    }

    protected void updateUI() {
        this.chipsContainer.removeAll();
        this.chipsContainer.add(this.selectedComponents.toArray(new ChipComponent[] {}));
    }
}
