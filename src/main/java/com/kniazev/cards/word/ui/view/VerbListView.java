package com.kniazev.cards.word.ui.view;

import com.kniazev.cards.word.constant.Roles;
import com.kniazev.cards.word.constant.UIRoute;
import com.kniazev.cards.word.db.model.word.Verb;
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

@RolesAllowed({Roles.ROLE_ADMIN, Roles.ROLE_LEARNER})
@PageTitle("Verbs")
@Route(value = UIRoute.VERBS_PAGE, layout = MainLayout.class)
public class VerbListView extends AbstractWordListView<Verb> {

    public VerbListView(WordService wordService) {
        super(wordService);
    }

    @Override
    protected boolean isFilterFieldVisible() {
        return true;
    }
    
    protected List<Verb> getData() {
        String filterValue = getFilterField().getValue();
        
        return wordService.findAllCachedVerbs(false)
                .stream()
                .filter(e -> StringUtils.containsIgnoreCase(e.getDe(), filterValue) 
                        || StringUtils.containsIgnoreCase(e.getRu(), filterValue)
                        || StringUtils.containsIgnoreCase(e.getIch(), filterValue)
                        || StringUtils.containsIgnoreCase(e.getDu(), filterValue)
                        || StringUtils.containsIgnoreCase(e.getEr(), filterValue)
                        || StringUtils.containsIgnoreCase(e.getWir(), filterValue)
                        || StringUtils.containsIgnoreCase(e.getIhr(), filterValue)
                        || StringUtils.containsIgnoreCase(e.getSie(), filterValue)
                        || StringUtils.containsIgnoreCase(e.getPartizip2(), filterValue)
                        )
                .filter(e -> CollectionUtils.isEmpty(tagsComboBox().getValue()) ||
                        CollectionUtils.isNotEmpty(e.getTags()) && CollectionUtils.containsAny(tagsComboBox().getValue(), e.getTags()) )
                .toList();
    }
    
    @Override
    protected Action refreshButtonAction() {
        return () -> {
            wordService.findAllCachedVerbs(true);
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
    protected void configureGrid(Grid<Verb> grid) {
        grid.getStyle().setFontSize("medium");
        
        grid.addColumn(verb -> verb.getDe()).setHeader("Infinitiv").setPartNameGenerator(verb -> "fold");
        grid.addColumn(verb -> verb.getRu()).setHeader("Translation").setPartNameGenerator(verb -> "fold");
        grid.addColumn(verb -> verb.getPartizip2()).setHeader("Partizip2");
        grid.addColumn(verb -> verb.getIch()).setHeader("ich");
        grid.addColumn(verb -> verb.getDu()).setHeader("du");
        grid.addColumn(verb -> verb.getEr()).setHeader("er/sie/es");
        grid.addColumn(verb -> verb.getWir()).setHeader("wir");
        grid.addColumn(verb -> verb.getIhr()).setHeader("ihr");
        //grid.addColumn(verb -> verb.getSie()).setHeader("Sie");
    }
}
