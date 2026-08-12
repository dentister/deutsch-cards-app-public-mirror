package com.kniazev.cards.word.ui.component.dialog;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.kniazev.cards.word.api.model.WordDto;
import com.kniazev.cards.word.controller.adapter.MappingFunctions;
import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.db.services.WordService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class JsonWordDialog extends BaseDialog {
    private WordService wordService;
    private Word word;
    private TextArea textArea;
    
    {
        setModal(false);
        setDraggable(true);
        setWidth("1000px");
        setHeight("700px");
    }
    
    public JsonWordDialog(WordService wordService) {
        this(wordService, null);
    }
    
    public JsonWordDialog(WordService wordService, Word word) {
        this.word = word;
        this.wordService = wordService;
    }
    
    @Override
    protected Collection<Component> getDialogComponents() {
        return List.of(getJsonTabContent());
    }
    
    protected final Component getJsonTabContent() {
        VerticalLayout container = new VerticalLayout();
        
        container.setSizeFull();
        container.add(getJsonTextArea());
        
        return container;
    }
    
    protected final TextArea getJsonTextArea() {
        if (textArea == null) {
            textArea = new TextArea();
            textArea.setWidthFull();
            textArea.setHeightFull();
            textArea.setSizeFull();
            textArea.setLabel("JSON");
            textArea.setValue(getInitialJsonText());
        }
        
        return textArea;
    }
    
    @Override
    protected ComponentEventListener<ClickEvent<Button>> saveBtnAction() {
        return event -> {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String value = getJsonTextArea().getValue();
                
                WordDto[] wordDtos = value.startsWith("[") 
                        ? objectMapper.readValue(value, WordDto[].class)
                        : new WordDto[] {objectMapper.readValue(value, WordDto.class)};
                
                Arrays.stream(wordDtos).map(MappingFunctions::mapToWord).forEach(wordService::createOrRewrite);
                
                close();
            } catch(JsonParseException e) {
                e.printStackTrace();
                Notification notification = new Notification("Not valid JSON");
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.open();
            } catch (JsonProcessingException e) {
                Notification notification = new Notification(e.getMessage());
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.open();
            }
        };
    }
    
    private String getInitialJsonText() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();
        
        if (word != null) {
            WordDto dto = MappingFunctions.mapToWordDto(word);
            
            try {
                String json = writer.writeValueAsString(dto).replaceFirst("\"wordType\"\s*:\s*\"\\w*\",?", "");
                
                Object jsonObject = mapper.readValue(json, Object.class);
                json = writer.writeValueAsString(jsonObject);
                
                return json;
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return null;
            }
        }
                    
        return getExampleJson();        
    }
    
    private static String getExampleJson() {
        return """
                [
                    {
                        "wordType": "NOUN",
                        "ru": "",
                        "de": "",
                        "plural": "",
                        "gender": "FMN"
                    },
                    {
                        "wordType": "VERB",
                        "ru": "",
                        "de": "",
                        "ich": "",
                        "du": "",
                        "er": "",
                        "wir": "",
                        "ihr": "",
                        "sie": "",
                        "partizip2": "",
                        "rootVerb": null,
                        "prefix": null
                    },
                    {
                        "wordType": "ADJECTIVE",
                        "ru": "",
                        "de": ""
                    },
                    {
                        "wordType": "ADVERB",
                        "ru": "",
                        "de": ""
                    },
                    {
                        "wordType": "PHRASE",
                        "ru": "",
                        "de": ""
                    }
                    
                ]
                """.trim();
    }
    
}
