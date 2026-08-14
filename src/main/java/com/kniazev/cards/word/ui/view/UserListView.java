package com.kniazev.cards.word.ui.view;

import com.kniazev.cards.word.constant.UIRoute;
import com.kniazev.cards.word.db.model.User;
import com.kniazev.cards.word.db.services.UserService;
import com.kniazev.cards.word.ui.MainLayout;
import com.kniazev.cards.word.ui.component.*;
import com.kniazev.cards.word.ui.component.UIEntityDialog.UIEntityBuilder;
import com.kniazev.cards.word.ui.dto.UIEntity;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.springframework.data.domain.Sort;

import java.util.List;

import jakarta.annotation.security.PermitAll;

@PermitAll
@PageTitle("Users")
@Route(value = UIRoute.USERS_PATH, layout = MainLayout.class)
public class UserListView extends AbstractTableView<User> {
    
    private final UserService userService;
    private UIEntityDialog<User> dialog;

    public UserListView(UserService userService) {
        super();
        
        this.userService = userService;
    }
    
    @Override
    protected boolean isFilterFieldVisible() {
        return true;
    }
    
    @Override
    protected boolean isEditable() {
        return true;
    }

    @Override
    protected List<User> getData() {
        return userService.findAll(Sort.by("id"));
    }

    @Override
    protected void configureGrid(Grid<User> grid) {
        grid.getStyle().setFontSize("medium");
        
        grid.addColumn(user -> user.getId()).setHeader("ID");
        grid.addColumn(user -> user.getUsername()).setHeader("Login");
        grid.addColumn(user -> user.getRoles()).setHeader("Roles");
    }
    
    protected void openNewEntityDialog() {
        if (dialog == null || !dialog.isOpened()) {
            User user = User.builder()
                    .roles(List.of("ALL", "PLAYER", "LEARNER"))
                    .enabled(true)
                    .build();
            user.getUserSettings().setUser(user);

            dialog = new UIEntityDialog<User>("New User", buildUiEntity(user), userService);
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
    
    protected void openEditEntityDialog(User user) {
        if (dialog == null || !dialog.isOpened()) {
            dialog = new UIEntityDialog<User>("Modify User", buildUiEntity(user), userService);
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
        userService.delete(grid().getSelectedItems());

        refreshData();
    }
    
    private UIEntity<User> buildUiEntity(User user) {
        return new UIEntityBuilder<User>()
                .withEntity(user)
                .withComponent(new StringEntityParameter<User, Long>(User::getId, new TextField("ID"), Long::valueOf))
                .withComponent(new BaseEntityParameter<>(User::getUsername, User::setUsername, new TextField("Username")))
                .withComponent(new BaseEntityParameter<>(User::getPassword, User::setPassword, new PasswordField("Password")))
                .build();
    }

}
