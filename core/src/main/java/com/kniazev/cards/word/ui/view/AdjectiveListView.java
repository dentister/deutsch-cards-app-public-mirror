package com.kniazev.cards.word.ui.view;

import com.kniazev.cards.word.common.Roles;
import com.kniazev.cards.word.model.dictionary.Adjective;
import com.kniazev.cards.word.service.WordService;
import com.kniazev.cards.word.ui.MainLayout;
import com.kniazev.cards.word.ui.UIRoute;
import com.kniazev.cards.word.ui.component.Action;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;

@RolesAllowed({Roles.ROLE_ADMIN, Roles.ROLE_LEARNER})
@PageTitle("Adjectives")
@Route(value = UIRoute.ADJECTIVES_PAGE, layout = MainLayout.class)
public class AdjectiveListView extends AbstractWordListView<Adjective> {

    public AdjectiveListView(WordService wordService) {
        super(wordService);
    }

    @Override
    protected boolean isFilterFieldVisible() {
        return true;
    }
    
    protected List<Adjective> getData() {
        String filterValue = getFilterField().getValue();
        
        return wordService.findAllCachedAdjectives(false)
                .stream()
                .filter(e -> StringUtils.containsIgnoreCase(e.getDe(), filterValue) 
                        || StringUtils.containsIgnoreCase(e.getRu(), filterValue) )
                .filter(e -> CollectionUtils.isEmpty(tagsComboBox().getValue()) ||
                        CollectionUtils.isNotEmpty(e.getTags()) && CollectionUtils.containsAny(tagsComboBox().getValue(), e.getTags()) )
                .toList();
    }
    
    @Override
    protected Action refreshButtonAction() {
        return () -> {
            wordService.findAllCachedAdjectives(true);
            refreshData();
        };
    }
    
    @Override
    protected List<Component> filterBarComponents() {
        List<Component> filterBarComponents = super.filterBarComponents();
        
        filterBarComponents.add(tagsComboBox());
        
        return filterBarComponents;
    }
    
    @Override
    protected void configureGrid(Grid<Adjective> grid) {
        grid.getStyle().setFontSize("medium");
        
        grid.addColumn(verb -> verb.getDe()).setHeader("Adjective").setPartNameGenerator(verb -> "fold");
        grid.addColumn(verb -> verb.getRu()).setHeader("Translation").setPartNameGenerator(verb -> "fold");
    }
}
