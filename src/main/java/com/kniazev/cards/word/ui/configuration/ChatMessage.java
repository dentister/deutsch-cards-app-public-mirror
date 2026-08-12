package com.kniazev.cards.word.ui.configuration;

import com.kniazev.cards.word.game.AnswerMessage;
import com.kniazev.cards.word.game.TaskMessage;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Style.AlignSelf;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public abstract class ChatMessage {
    public static final String SYSTEM = "System";
    public static final String USER = "User";
   
    private static final String USER_AVATAR = "https://api.dicebear.com/9.x/adventurer/svg?seed=Abby";
    private static final String SYSTEM_AVATAR = "https://api.dicebear.com/9.x/adventurer/svg?seed=Bandit";
     
    private final String iconUri;
    private final String userName;
    

    public ChatMessage(String userName) {
        this.userName = userName;  
        this.iconUri = USER.equals(userName) ? USER_AVATAR : SYSTEM_AVATAR;
    }
    
    public Component asComponent() {
        HorizontalLayout rowLayout = new HorizontalLayout();
        
        rowLayout.setMinHeight("40px");

        Image icon = new Image(iconUri, "User Icon");
        icon.setWidth("30px");
        icon.setHeight("30px");
        icon.getStyle().setAlignSelf(AlignSelf.CENTER);

        Span spanUserName = new Span(userName);
        spanUserName.getElement().getStyle().set("font-weight", "bold");
        spanUserName.getElement().getStyle().set("font-size", "medium");
        spanUserName.getElement().getStyle().setWidth("60px");
        spanUserName.getElement().getStyle().setAlignSelf(AlignSelf.CENTER);
        
        rowLayout.add(icon, spanUserName, getMsgBody());

        return rowLayout;
    }
    
    protected abstract Component getMsgBody();
    
    public static class GeneralMessage extends ChatMessage {
        private String msg;
        
        public GeneralMessage(String userName, String msg) {
            super(USER);
            
            this.msg = msg;
        }
        
        @Override
        protected Component getMsgBody() {
            HorizontalLayout layout = new HorizontalLayout(Alignment.START, new Text(msg));

            layout.setSpacing(false);
            layout.getStyle().setAlignSelf(AlignSelf.CENTER);
            layout.getStyle().setMarginLeft("15px");
            layout.getStyle().setMarginTop("10px");
            layout.getStyle().setMarginBottom("10px");
            layout.getStyle().setFontSize("medium");
            
            return layout;
        }
        
    }
    
    public static class UserAnswer extends GeneralMessage {
        public UserAnswer(String userAnswer) {
            super(USER, userAnswer);
        }
    }
    
    public static class RightAnswer extends ChatMessage {
        private final AnswerMessage answerMsg;
        private final TaskMessage taskMsg;

        public RightAnswer(AnswerMessage answerMsg, TaskMessage taskMsg) {
            super(SYSTEM);
            
            this.answerMsg = answerMsg;
            this.taskMsg = taskMsg;
        }

        @Override
        protected Component getMsgBody() {
            VerticalLayout layout = new VerticalLayout();
            
            if (answerMsg != null) {
                layout.add(createRightAnswerBlock());
            }
            
            layout.getStyle().setMarginLeft("15px");
            layout.getStyle().setMarginTop("10px");
            layout.getStyle().setMarginBottom("10px");
            layout.setSpacing(false);
            layout.setPadding(false);
            layout.add(createTaskBlock(taskMsg.getTaskTextWeb()));
            
            return layout;
        }
        
        private HorizontalLayout createRightAnswerBlock() {
            HorizontalLayout layout = new HorizontalLayout(Alignment.START);
            
            layout.setSpacing(true);
            layout.setPadding(false);
            layout.getStyle().setFontSize("medium");
            layout.getStyle().setAlignSelf(AlignSelf.FLEX_START);
            layout.getStyle().setMarginBottom("5px");
            
            Span answerStatusSpan = new Span();
            answerStatusSpan.getStyle().set("font-weight", "bold");
            answerStatusSpan.setText(answerMsg.isCorrect() ? answerMsg.getRightResultTokenWeb() : answerMsg.getWrongResultTokenWeb());
            
            Span span = new Span(answerMsg.getRightAnswerTokenWeb());
            span.getStyle().setColor(answerMsg.getColor().getHtmlCode());
            
            layout.add(answerStatusSpan, span);
            
            return layout;
        }
        
        private static HorizontalLayout createTaskBlock(String taskText) {
            HorizontalLayout layout = new HorizontalLayout();
            
            layout.setSpacing(false);
            layout.setPadding(false);
            layout.getStyle().setAlignSelf(AlignSelf.FLEX_START);
            layout.getStyle().setFontSize("medium");
            layout.add(new Text(taskText));
            
            return layout;
        }

        @Getter
        @AllArgsConstructor
        public enum Color {
            RED("red"), 
            BLUE("blue"),
            GREEN("green"), 
            DEFAULT("black");
            
            private String htmlCode;
        }
        
    }
}
