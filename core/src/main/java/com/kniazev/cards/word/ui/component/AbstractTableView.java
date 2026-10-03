package com.kniazev.cards.word.ui.component;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.selection.SelectionListener;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;

public abstract class AbstractTableView<T> extends VerticalLayout {
    private static final long serialVersionUID = 958468672531632671L;
    
    private Grid<T> grid;
    private TextField filterField;
    private MenuBar menuBar;
    private MenuItem addMItem;
    private MenuItem editMItem;
    private MenuItem delMItem;
    
    protected abstract void configureGrid(Grid<T> grid);
    protected abstract List<T> getData();
    
    @PostConstruct
    protected void init() {
        configureGrid(grid());
        
        HorizontalLayout toolbar = new HorizontalLayout();
        
        toolbar.add(filterBarComponents());
        toolbar.add(menuBarButtons());
        
        customizeMenubar(menuBarButtons());

        add(toolbar, grid());

        refreshData();
    }
    
    protected List<Component> filterBarComponents() {
        List<Component> components = new ArrayList<>();
        
        if (isFilterFieldVisible()) {
            components.add(getFilterField());
        }
        
        return components;
    }
    
    protected void customizeMenubar(MenuBar menubar) {}
    
    protected boolean isEditable() {
        return false;
    }
    
    protected boolean isFilterFieldVisible() {
        return false;
    }

    protected boolean isRefreshBtnVisible() {
        return true;
    }
    
    protected final Grid<T> grid() {
        if (grid == null) {
            grid = new Grid<>();

            getElement().getStyle().set("height", "100%");
            grid.setHeight("100%");
            
            if (isEditable()) {
                grid.addSelectionListener(onGridSelectListener());
                grid.addItemDoubleClickListener(onGridDoubleClickListener());
                grid.setSelectionMode(SelectionMode.SINGLE);
            } else {
                grid.setSelectionMode(SelectionMode.NONE);
            }
        }

        return grid;
    }
    
    protected final void refreshData() {
        grid().setItems(getData());
        grid().getDataProvider().refreshAll();
    }
    
    protected Action refreshButtonAction() {
        return () -> refreshData();
    }
    
    protected void deleteButtonAction() {
        Notification.show("Not implemented :(");
    }
    
    protected void openNewEntityDialog() {
        Notification.show("Not implemented :(");
    }
    
    protected void openEditEntityDialog(T obj) {
        Notification.show("Not implemented :(");
    }
    
    protected final TextField getFilterField() {
        if (filterField == null) {
            filterField = new TextField();

            filterField.setPlaceholder("Filter by value");

            filterField.setValueChangeMode(ValueChangeMode.LAZY);
            filterField.addValueChangeListener(e -> {
                getUI().ifPresent(ui -> ui.access(() -> {
                    refreshData();
                }));
            });
        }

        return filterField;
    }
    
    protected MenuBar menuBarButtons() {
        if (menuBar == null)  {
            menuBar = new MenuBar();
            menuBar.addThemeVariants(MenuBarVariant.LUMO_ICON);
            
            if (isRefreshBtnVisible()) {
                createIconItem(menuBar, VaadinIcon.REFRESH, null, wrap(refreshButtonAction()));
            }
            
            addMItem();
            
            editMItem();
            
            delMItem();
        }

        return menuBar;
    }

    private ComponentEventListener<ClickEvent<MenuItem>> wrap(Action action) {
        return e -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                action.execute();
            }));
        };
    }
    
    protected MenuItem addMItem() {
        if (isEditable() && addMItem == null) {
            addMItem = createIconItem(menuBarButtons(), VaadinIcon.PLUS, null, e -> openNewEntityDialog());
        }
        
        return addMItem;
    }
    
    private MenuItem editMItem() {
        if (isEditable() && editMItem == null) {
            editMItem = createIconItem(menuBarButtons(), VaadinIcon.PENCIL, null, e -> openEditEntityDialog(grid().getSelectedItems().iterator().next()));
            editMItem.setEnabled(false);
        }
        
        return editMItem;
    }
    
    private MenuItem delMItem() {
        if (isEditable() && delMItem == null) {
            delMItem = createIconItem(menuBarButtons(), VaadinIcon.TRASH, null, e -> deleteButtonAction());
            delMItem.setEnabled(false);
        }
        
        return delMItem;
    }

    
    private SelectionListener<Grid<T>, T> onGridSelectListener() {
        return event -> {
            editMItem().setEnabled( event.getFirstSelectedItem().isPresent() );
            delMItem().setEnabled( event.getFirstSelectedItem().isPresent() );
        };

    }

    private ComponentEventListener<ItemDoubleClickEvent<T>> onGridDoubleClickListener() {
        return event -> {
            openEditEntityDialog(event.getItem());
        };
    }
    
    protected MenuItem createIconItem(MenuBar menu, VaadinIcon iconName, String label, ComponentEventListener<ClickEvent<MenuItem>> action) {
        Icon icon = iconName.create();

        MenuItem item = menu.addItem(icon, action);

        if (label != null) {
            item.add(new Text(label));
        }

        return item;
    }
    
    public static enum UIElementState {
        ENABLED, DISABLED, NOT_PRESENT;
    }
}
