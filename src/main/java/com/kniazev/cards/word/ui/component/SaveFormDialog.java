package com.kniazev.cards.word.ui.component;

import com.kniazev.cards.word.db.model.word.Noun;
import com.kniazev.cards.word.db.model.word.Verb;
import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.db.model.word.Word.WordBuilder;
import com.kniazev.cards.word.db.model.word.Word.WordType;
import com.kniazev.cards.word.ui.component.atomic.MultipleTextField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class SaveFormDialog extends Dialog {

    private final Optional<Word> word;

    private VerticalLayout dialogLayout;

    private RadioButtonGroup<WordType> wordTypeRadioButtonGroup;
    private RadioButtonGroup<Article> articleRadioButtonGroup;
    private TextField deTextField;
    private MultipleTextField ruTextField;
    private Span articleLabel = new Span();

    public SaveFormDialog(Consumer<Word> okButtonAction) {
        this(null, okButtonAction);
    }

    public SaveFormDialog(Consumer<Word> okButtonAction, String startWord) {
        this(null, okButtonAction);

        if (Pattern.matches(".*\\p{InCyrillic}.*", startWord)) {
            ruTextField().typeText(startWord);
        } else {
            deTextField().setValue(startWord);
        }
    }

    public SaveFormDialog(Word word, Consumer<Word> okButtonAction) {
        this.word = Optional.ofNullable(word);

        setModal(false);
        setDraggable(true);
        setWidth("350px");

        add(dialogLayout());

        getFooter().add(
                new Button("Ok", onSaveButtonClickListener(okButtonAction)),
                new Button("Cancel", e -> close()) );

        init();
    }

    private void init() {
        if (word.isEmpty()) {
            setHeaderTitle("New word");
            return;
        }

        setHeaderTitle("Update word " + word.get().getId());

        String[] wordParts = word.get().getDe().split(" ");
        Article article = wordParts.length == 1 ? Article.none : Article.valueOf(wordParts[0]);

        wordTypeRadioButtonGroup().setValue(word.map(Word::getWordType).orElse(WordType.NOUN));

        articleRadioButtonGroup().setVisible(wordTypeRadioButtonGroup().getValue() == WordType.NOUN);
        articleRadioButtonGroup().setValue(article);

        articleLabel().setText(article == Article.none ? "" : article.toString());

        deTextField().setValue(wordParts[wordParts.length-1]);
        ruTextField().setValue(word.get().getRuAsSet());
    }

    private VerticalLayout dialogLayout() {
        if (dialogLayout == null) {
            dialogLayout = new VerticalLayout();
            dialogLayout.setSizeFull();

            dialogLayout.add(wordTypeRadioButtonGroup(),
                    articleRadioButtonGroup(),
                    deTextField(),
                    ruTextField());
        }

        return dialogLayout;
    }

    private RadioButtonGroup<WordType> wordTypeRadioButtonGroup() {
        if (wordTypeRadioButtonGroup == null) {
            wordTypeRadioButtonGroup = new RadioButtonGroup<>();

            wordTypeRadioButtonGroup.setItems(WordType.NOUN, WordType.VERB);

            wordTypeRadioButtonGroup.addValueChangeListener(event -> {
                if (event.getValue() == WordType.NOUN) {
                    articleRadioButtonGroup().setVisible(true);
                    articleLabel().setVisible(true);
                } else {
                    articleRadioButtonGroup().setVisible(false);
                    articleLabel().setVisible(false);
                }
            });

        }

        return wordTypeRadioButtonGroup;
    }

    private RadioButtonGroup<Article> articleRadioButtonGroup() {
        if (articleRadioButtonGroup == null) {
            articleRadioButtonGroup = new RadioButtonGroup<>();

            articleRadioButtonGroup.setItems(List.of(Article.der, Article.das, Article.die, Article.none));
            articleRadioButtonGroup.addValueChangeListener(event -> {
                articleLabel.setText( articleRadioButtonGroup().getValue() == Article.none ? "" : articleRadioButtonGroup().getValue().toString() );
            });
        }

        return articleRadioButtonGroup;
    }

    private MultipleTextField ruTextField() {
        if (ruTextField == null) {
            ruTextField = new MultipleTextField();
            ruTextField.setWidthFull();
            ruTextField.setPlaceholder("RU");
            ruTextField.setValue(Set.of());
        }

        return ruTextField;
    }

    private TextField deTextField() {
        if (deTextField == null) {
            deTextField = new TextField();
            deTextField.setWidthFull();
            deTextField.setPlaceholder("De");
            deTextField.setPrefixComponent(articleLabel());

            deTextField.addValueChangeListener(event -> {
                if (wordTypeRadioButtonGroup.getValue() == WordType.NOUN) {
                    deTextField.setValue(StringUtils.capitalize(event.getValue()));
                }
            });
        }

        return deTextField;
    }

    private Span articleLabel() {
        if (articleLabel == null) {
            articleLabel = new Span();
        }

        return articleLabel;
    }

    private Word mapFormDataToEntity() {
        WordBuilder<?, ?> builder = switch (wordTypeRadioButtonGroup.getValue()) {
            case NOUN: yield Noun.builder().wordType(WordType.NOUN);
            case VERB: yield Verb.builder().wordType(WordType.VERB);
            default: yield null;
        };

        StringBuffer sb = new StringBuffer("");
        if (StringUtils.isNotBlank(articleLabel().getText()) && articleLabel().isVisible()) {
            sb.append(articleLabel().getText()).append(' ');
        }
        sb.append(deTextField().getValue());

         Word w = builder
                 .wordType(wordTypeRadioButtonGroup().getValue())
                 .de(sb.toString())
                 .build();

         word.map(Word::getId).ifPresent(w::setId);
         w.setRuSetAsString(ruTextField().getValue());

         return w;
    }

    private ComponentEventListener<ClickEvent<Button>> onSaveButtonClickListener(Consumer<Word> okButtonAction) {
        return event -> {
            okButtonAction.accept(mapFormDataToEntity());
            close();
        };
    }

    enum Mode {
        ADD, MOD;
    }

    enum Article {
        der, das, die, none;
    }
}
