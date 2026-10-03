package com.kniazev.cards.word.ui.view;

import com.kniazev.cards.word.common.Roles;
import com.kniazev.cards.word.model.dictionary.*;
import com.kniazev.cards.word.model.dictionary.Word.WordType;
import com.kniazev.cards.word.service.WordService;
import com.kniazev.cards.word.ui.MainLayout;
import com.kniazev.cards.word.ui.UIRoute;
import com.kniazev.cards.word.ui.VaadinSecurityService;
import com.kniazev.cards.word.ui.WordDialogFactory;
import com.kniazev.cards.word.ui.component.BaseDialog;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox.AutoExpandMode;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;

@RolesAllowed({Roles.ROLE_ADMIN, Roles.ROLE_LEARNER})
@PageTitle("Dictionary")
@Route(value = UIRoute.DICTIONARY_PAGE, layout = MainLayout.class)
public class DictionaryView extends AbstractWordListView<Word> {
    private final WordDialogFactory wordDialogFactory;
    private final VaadinSecurityService securityService;

    protected Select<WordType> select;
    protected MultiSelectComboBox<String> tagsComboBox;
    private BaseDialog dialog;
    private MenuItem addMItem;

    public DictionaryView(WordService wordService, WordDialogFactory wordDialogFactory, VaadinSecurityService securityService) {
        super(wordService);

        this.wordDialogFactory = wordDialogFactory;
        this.securityService = securityService;
    }

    @Override
    protected boolean isEditable() {
        return securityService.isAdmin();
    }

    @Override
    protected boolean isFilterFieldVisible() {
        return true;
    }

    @Override
    protected boolean isRefreshBtnVisible() {
        return true;
    }
    
    @Override
    protected MenuItem addMItem() {
        if (isEditable() && addMItem == null) {
            MenuItem item = createIconItem(menuBarButtons(), VaadinIcon.PLUS, null, null);
            
            SubMenu sm = item.getSubMenu();
            
            sm.addItem("New Noun", e -> openNewWordDialog(Noun.class));
            sm.addItem("New Verb", e -> openNewWordDialog(Verb.class));
            sm.addItem("New Adjective", e -> openNewWordDialog(Adjective.class));
            sm.addItem("New Adverb", e -> openNewWordDialog(Adverb.class));
            sm.addItem("New Phrase", e -> openNewWordDialog(Phrase.class));
            sm.addItem("As Json", e -> openNewJsonWordDialog());
        }
        
        return addMItem;
    }
    
    @Override
    protected List<Component> filterBarComponents() {
        List<Component> filterBarComponents = super.filterBarComponents();
        
        filterBarComponents.add(wordTypeSelect());
        filterBarComponents.add(tagsComboBox());
        
        return filterBarComponents;
    }
    
    protected Select<WordType> wordTypeSelect() {
        if (select == null) {
            select = new Select<>();
        
            select.setEmptySelectionAllowed(true);
            select.setEmptySelectionCaption("All");
            
            select.setItems(WordType.values());
            
            select.addValueChangeListener(e -> refreshData());
        }
        
        return select;
    }
    
    protected MultiSelectComboBox<String> tagsComboBox() {
        if (tagsComboBox == null) {
            tagsComboBox = new MultiSelectComboBox<>();
            tagsComboBox.setPlaceholder("Tags");
            tagsComboBox.setItems(wordService.findAllTags());
            tagsComboBox.setAutoExpand(AutoExpandMode.BOTH);
            tagsComboBox.addValueChangeListener(e -> refreshData());
        }
        
        return tagsComboBox;
    }

    @Override
    protected List<Word> getData() {
        String filterValue = getFilterField().getValue();
        
        return wordService.findAll(Sort.by("id"))
                .stream()
                .filter(e -> wordTypeSelect().getValue()== null || e.getWordType() == wordTypeSelect().getValue())
                .filter(e -> StringUtils.containsIgnoreCase(e.getDe(), filterValue) 
                        || StringUtils.containsIgnoreCase(e.getRu(), filterValue) )
                .filter(e -> CollectionUtils.isEmpty(tagsComboBox().getValue()) ||
                        (CollectionUtils.isNotEmpty(e.getTags()) && CollectionUtils.containsAny(tagsComboBox().getValue(), e.getTags())) )
                .toList();
    }

    @Override
    protected void configureGrid(Grid<Word> grid) {
        grid.getStyle().setFontSize("medium");
        
        grid.addColumn(word -> word.getId()).setHeader("ID");
        grid.addColumn(word -> word.getDe()).setHeader("DE");
        grid.addColumn(word -> word.getWordType()).setHeader("Word Type");
        grid.addColumn(word -> word.getRu()).setHeader("RU");
        grid.addColumn(word -> word.getLevel()).setHeader("Level");
        grid.addColumn(word -> word.getTags()).setHeader("Tags");
    }
    
    private void openNewWordDialog(Class<? extends Word> wordClass) {
        if (dialog == null || !dialog.isOpened()) {
            dialog = wordDialogFactory.createWordDialog(wordClass);
            dialog.open();
            
            dialog.getSaveButton().addClickListener(event -> {
                if (!dialog.isOpened()) {
                    getUI().ifPresent(ui -> ui.access(() -> {
                        refreshData();
                    }));
                }
            });
        }
    }
    
    private void openNewJsonWordDialog() {
        if (dialog == null || !dialog.isOpened()) {
            dialog = wordDialogFactory.createJsonDialog(null);
            dialog.open();
            
            dialog.getSaveButton().addClickListener(event -> {
                if (!dialog.isOpened()) {
                    getUI().ifPresent(ui -> ui.access(() -> {
                        refreshData();
                    }));
                }
            });
        }
    }
    
    @Override
    protected void openEditEntityDialog(Word w) {
        if (dialog == null || !dialog.isOpened()) {
            dialog = wordDialogFactory.createWordDialog(w);
            dialog.open();
            
            dialog.getSaveButton().addClickListener(event -> {
                if (!dialog.isOpened()) {
                    getUI().ifPresent(ui -> ui.access(() -> {
                        refreshData();
                    }));
                }
            });
        }
    }
    
    @Override
    protected void deleteButtonAction() {
        wordService.delete(grid().getSelectedItems());

        refreshData();
    }

}
