package com.kniazev.cards.word.ui.view;

import com.kniazev.cards.word.common.Roles;
import com.kniazev.cards.word.model.dictionary.Noun;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import jakarta.annotation.security.RolesAllowed;

@RolesAllowed({Roles.ROLE_ADMIN, Roles.ROLE_LEARNER})
@PageTitle("Nouns")
@Route(value = UIRoute.NOUNS_PAGE, layout = MainLayout.class)
public class NounListView extends AbstractWordListView<Integer> {
    private final List<Noun> dieNouns = new ArrayList<>();
    private final List<Noun> derNouns = new ArrayList<>();
    private final List<Noun> dasNouns = new ArrayList<>();
    
    public NounListView(WordService wordService) {
        super(wordService);
    }

    @Override
    protected boolean isFilterFieldVisible() {
        return true;
    }
    
    protected List<Integer> getData() {
        String filterValue = getFilterField().getValue();
        
        derNouns.clear();
        dasNouns.clear();
        dieNouns.clear();
        
        wordService.findAllCachedNouns(false)
                .stream()
                .filter(e -> StringUtils.containsIgnoreCase(e.getFullDe(), filterValue) || StringUtils.containsIgnoreCase(e.getRu(), filterValue))
                .filter(e -> CollectionUtils.isEmpty(tagsComboBox().getValue()) ||
                        CollectionUtils.isNotEmpty(e.getTags()) && CollectionUtils.containsAny(tagsComboBox().getValue(), e.getTags()) )
                .forEach(e -> {
                    switch (e.getGender()) {
                        case M -> derNouns.add(e);
                        case F -> dieNouns.add(e);
                        case PL -> dieNouns.add(e);
                        case N -> dasNouns.add(e);
                    };
                });
        
        int size = Math.max(derNouns.size(), Math.max(dieNouns.size(), dasNouns.size()));
        
        return IntStream.range(0, size).mapToObj(Integer::valueOf).toList();
    }
    
    @Override
    protected Action refreshButtonAction() {
        return () -> {
            wordService.findAllCachedNouns(true);
            refreshData();
        };
    }
    
    @Override
    protected void configureGrid(Grid<Integer> grid) {
        addColumn(grid, derNouns, "Der", "der-cell");
        addColumn(grid, dasNouns, "Das", "das-cell");
        addColumn(grid, dieNouns, "Die", "die-cell");
    }
    
    @Override
    protected List<Component> filterBarComponents() {
        List<Component> filterBarComponents = super.filterBarComponents();
        
        filterBarComponents.add(tagsComboBox());
        
        return filterBarComponents;
    }
    
    private void addColumn(Grid<Integer> grid, List<Noun> nouns, String title, String partName) {
        grid.addColumn(i -> nouns.size() <= i ? "" : nouns.get(i).getFullDe() + "   (" + nouns.get(i).getFullPlural() + ")")
                .setHeader(title)
                .setPartNameGenerator(i -> nouns.size() <= i ? "" : partName);
        
        grid.addColumn(i -> nouns.size() <= i ? "" : nouns.get(i).getRu())
                .setHeader(title + " Translation")
                .setPartNameGenerator(i -> nouns.size() <= i ? "" : partName + "-translation");
    }
}
