package com.kniazev.cards.word.ui.component.dialog;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.Collection;
import java.util.Collections;

public class BaseDialog extends Dialog {
    private VerticalLayout dialogLayout;
    private Button saveBtn;
    private Button cancelBtn;
    
    @Override
    public void open() {
        buildDialog();
        
        super.open();
    }
    
    protected ComponentEventListener<ClickEvent<Button>> saveBtnAction() {
        return e -> Notification.show("Not implemented :(");
    }
    
    protected ComponentEventListener<ClickEvent<Button>> cancelBtnAction() {
        return e -> close();
    }
    
    public final Button getSaveButton() {
        if (saveBtn == null) {
            saveBtn = new Button("Save", saveBtnAction());
        }
        
        return saveBtn;
    }
    
    public final Button getCancelButton() {
        if (cancelBtn == null) {
            cancelBtn = new Button("Cancel", cancelBtnAction());
        }
        
        return cancelBtn;
    }
    
    protected final VerticalLayout dialogLayout() {
        if (dialogLayout == null) {
            dialogLayout = new VerticalLayout();
            dialogLayout.setPadding(false);
            dialogLayout.setSpacing(false);
            dialogLayout.setHeightFull();
            dialogLayout.setWidthFull();
            dialogLayout.add(getDialogComponents());
        }

        return dialogLayout;
    }
    
    protected Collection<Component> getDialogComponents() {
        return Collections.emptyList();
    }
    
    private void buildDialog() {
        add(dialogLayout());
        
        getFooter().add(
                getSaveButton(),
                getCancelButton());
    }
}
