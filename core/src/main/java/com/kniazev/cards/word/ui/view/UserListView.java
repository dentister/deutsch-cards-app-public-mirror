package com.kniazev.cards.word.ui.view;

import com.kniazev.cards.word.common.Roles;
import com.kniazev.cards.word.model.user.User;
import com.kniazev.cards.word.service.UserService;
import com.kniazev.cards.word.ui.MainLayout;
import com.kniazev.cards.word.ui.UIRoute;
import com.kniazev.cards.word.ui.component.AbstractTableView;
import com.kniazev.cards.word.ui.component.BaseEntityParameter;
import com.kniazev.cards.word.ui.component.StringEntityParameter;
import com.kniazev.cards.word.ui.component.UIEntity;
import com.kniazev.cards.word.ui.component.UIEntityDialog;
import com.kniazev.cards.word.ui.component.UIEntityDialog.UIEntityBuilder;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Objects;

import jakarta.annotation.security.RolesAllowed;

@RolesAllowed({Roles.ROLE_ADMIN})
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
        String filterValue = getFilterField().getValue();

        return userService.findAll(Sort.by("id"))
                .stream()
                .filter(e -> StringUtils.isEmpty(filterValue)
                        || StringUtils.containsIgnoreCase(e.getUsername(), filterValue)
                        || StringUtils.containsIgnoreCase(Objects.toString(e.getTelegramId(), ""), filterValue))
                .toList();
    }

    @Override
    protected void configureGrid(Grid<User> grid) {
        grid.getStyle().setFontSize("medium");
        
        grid.addColumn(user -> user.getId()).setHeader("ID");
        grid.addColumn(user -> user.getUsername()).setHeader("Login");
        grid.addColumn(user -> Objects.toString(user.getTelegramId(), "")).setHeader("Telegram ID");
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
        TextField username = new TextField("Username");
        username.setReadOnly(user.getTelegramId() != null);

        return new UIEntityBuilder<User>()
                .withEntity(user)
                .withComponent(new StringEntityParameter<User, Long>(User::getId, new TextField("ID"), Long::valueOf))
                .withComponent(new StringEntityParameter<User, Long>(User::getTelegramId, new TextField("Telegram ID"), Long::valueOf))
                .withComponent(new BaseEntityParameter<>(User::getUsername, User::setUsername, username))
                .withComponent(new BaseEntityParameter<>(User::getPassword, User::setPassword, new PasswordField("Password")))
                .build();
    }

}
