package com.kniazev.cards.word.ui.view;

import com.kniazev.cards.word.constant.Roles;
import com.kniazev.cards.word.constant.UIRoute;
import com.kniazev.cards.word.db.model.word.Phrase;
import com.kniazev.cards.word.db.service.WordService;
import com.kniazev.cards.word.ui.MainLayout;
import com.kniazev.cards.word.util.Action;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;

@RolesAllowed({Roles.ROLE_ADMIN, Roles.ROLE_USER})
@PageTitle("Phrases")
@Route(value = UIRoute.PHRASES_PAGE, layout = MainLayout.class)
public class PhraseListView extends AbstractWordListView<Phrase> {
    
    public PhraseListView(WordService wordService) {
        super(wordService);
    }

    @Override
    protected boolean isFilterFieldVisible() {
        return true;
    }
    
    protected List<Phrase> getData() {
        String filterValue = getFilterField().getValue();
        
        return wordService.findAllCachedPhrases(false)
                .stream()
                .filter(e -> StringUtils.containsIgnoreCase(e.getDe(), filterValue) 
                        || StringUtils.containsIgnoreCase(e.getRu(), filterValue) )
                .filter(e -> CollectionUtils.isEmpty(tagsComboBox().getValue()) ||
                        CollectionUtils.isNotEmpty(e.getTags()) && CollectionUtils.containsAny(tagsComboBox().getValue(), e.getTags()) )
                .toList();
    }
    
    @Override
    protected List<Component> filterBarComponents() {
        List<Component> filterBarComponents = super.filterBarComponents();
        
        filterBarComponents.add(tagsComboBox());
        
        return filterBarComponents;
    }
    
    @Override
    protected Action refreshButtonAction() {
        return () -> {
            wordService.findAllCachedPhrases(true);
            refreshData();
        };
    }
    
    @Override
    protected void configureGrid(Grid<Phrase> grid) {
        grid.getStyle().setFontSize("medium");
        
        grid.addColumn(verb -> verb.getDe()).setHeader("Phrase").setPartNameGenerator(verb -> "fold");
        grid.addColumn(verb -> verb.getRu()).setHeader("Translation").setPartNameGenerator(verb -> "fold");
    }
}
