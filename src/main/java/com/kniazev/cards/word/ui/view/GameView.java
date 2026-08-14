package com.kniazev.cards.word.ui.view;

import com.kniazev.cards.word.constant.Roles;
import com.kniazev.cards.word.constant.UIRoute;
import com.kniazev.cards.word.db.model.word.Word.WordType;
import com.kniazev.cards.word.db.services.WordService;
import com.kniazev.cards.word.game.AnswerMessage;
import com.kniazev.cards.word.game.TaskMessage;
import com.kniazev.cards.word.game.WordCard;
import com.kniazev.cards.word.services.ChatStorage;
import com.kniazev.cards.word.services.GameService;
import com.kniazev.cards.word.services.SecurityService;
import com.kniazev.cards.word.ui.MainLayout;
import com.kniazev.cards.word.ui.configuration.ChatMessage;
import com.kniazev.cards.word.ui.configuration.ChatMessage.GeneralMessage;
import com.kniazev.cards.word.ui.configuration.ChatMessage.RightAnswer;
import com.kniazev.cards.word.ui.configuration.ChatMessage.UserAnswer;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox.AutoExpandMode;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.security.RolesAllowed;

/**
 * Legacy and will not be supported in the future
 */
@RolesAllowed({Roles.ROLE_ADMIN, Roles.ROLE_PLAYER})
@PageTitle("Game")
@Route(value = UIRoute.GAME_PAGE, layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class GameView extends VerticalLayout {
    
    private final GameService gameService;
    private final WordService wordService;
    private final String currentPrincipalName;
    private final MessageListProvider dataProvider;
    
    private WordCard currentCard;
    private MenuBar menuBar;
    private Select<WordType> select;
    private MultiSelectComboBox<String> tagsComboBox;
    private Grid<ChatMessage> chat;
    private MessageInput input;
    
    public GameView(GameService gameService, WordService wordService, ChatStorage chatStorage, SecurityService securityService) {
        this.gameService = gameService;
        this.wordService = wordService;
        this.currentPrincipalName = securityService.getAuthenticatedUsername();
        this.dataProvider = new MessageListProvider(currentPrincipalName, chatStorage);
        
        if (CollectionUtils.isEmpty(dataProvider.getItems())) {
            currentCard = gameService.getNextTask(currentPrincipalName);
            
            dataProvider.addMessages(new RightAnswer(null, currentCard.getTaskMsg()));
        }
        
        chat = new Grid<>();
        chat.setDataProvider(dataProvider);

        chat.addComponentColumn(message -> message.asComponent() ).setHeader("Messages");

        input = new MessageInput();
        
        HorizontalLayout toolbar = new HorizontalLayout();
        
        toolbar.add(menuBarButtons());
        toolbar.add(filterBarComponents());
        
        this.add(toolbar, chat, input);
        
        this.setHorizontalComponentAlignment(Alignment.CENTER, chat, input);
        this.setPadding(true);
        this.setHeightFull();
        chat.setSizeFull();
        input.setWidthFull();
        chat.setMaxWidth("1200px");
        input.setMaxWidth("1200px");
        chat.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_ROW_BORDERS, GridVariant.LUMO_NO_BORDER);
        
        input.addSubmitListener(this::onSubmit);
    }
    
    protected List<Component> filterBarComponents() {
        List<Component> components = new ArrayList<>();
        
        components.add(wordTypeSelect());
        components.add(tagsComboBox());
        
        return components;
    }
    
    protected MenuBar menuBarButtons() {
        if (menuBar == null)  {
            menuBar = new MenuBar();
            
            menuBar.addThemeVariants(MenuBarVariant.LUMO_ICON);
            menuBar.addItem(VaadinIcon.PLAY.create(), e -> {
                //TODO Fix it
                //gameService.restartGame(currentPrincipalName, select.getValue(), tagsComboBox.getValue());
                gameService.restartGame(currentPrincipalName);
                currentCard = gameService.getNextTask(currentPrincipalName);
                
                dataProvider.addMessages(new GeneralMessage(ChatMessage.SYSTEM, String.format("New game has been started for wordType=%s, tags=%s", select.getValue(), tagsComboBox.getValue())));
                dataProvider.addMessages(new RightAnswer(null, currentCard.getTaskMsg()));
            });
        }

        return menuBar;
    }
    
    protected Select<WordType> wordTypeSelect() {
        if (select == null) {
            select = new Select<>();
        
            select.setEmptySelectionAllowed(true);
            select.setEmptySelectionCaption("All");
            
            select.setItems(WordType.values());
        }
        
        return select;
    }
    
    protected MultiSelectComboBox<String> tagsComboBox() {
        if (tagsComboBox == null) {
            tagsComboBox = new MultiSelectComboBox<>();
            tagsComboBox.setPlaceholder("Tags");
            tagsComboBox.setItems(wordService.findAllTags());
            tagsComboBox.setAutoExpand(AutoExpandMode.BOTH);
        }
        
        return tagsComboBox;
    }

    private void onSubmit(MessageInput.SubmitEvent submitEvent) {
        getUI().ifPresent(ui -> ui.access(() -> {
            String text = submitEvent.getValue();
            
            WordCard wordCard = gameService.checkUserAnswer(currentPrincipalName, text);
            currentCard = gameService.getNextTask(currentPrincipalName);
            
            ChatMessage userAnswer = new UserAnswer(text);
            
            AnswerMessage answerMsg = wordCard.getAnswerMsg();
            TaskMessage taskMsg = currentCard.getTaskMsg();
            
            ChatMessage rightAnswer = new RightAnswer(answerMsg, taskMsg);
            
            dataProvider.addMessages(userAnswer, rightAnswer);
            
            chat.scrollToEnd();
        }));
    }
    
    private class MessageListProvider extends ListDataProvider<ChatMessage> {
        private final ChatStorage chatStorage;
        private final String username;
        
        public MessageListProvider(String username, ChatStorage chatStorage) {
            super(chatStorage.getMessages(username));
            
            this.chatStorage = chatStorage;
            this.username = username;
        }

        public void addMessages(ChatMessage...msgs) {
            chatStorage.addMessages(username, msgs);
            refreshAll();
        }
    }

}
