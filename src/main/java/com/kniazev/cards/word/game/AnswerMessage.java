package com.kniazev.cards.word.game;

import com.kniazev.cards.word.db.model.word.Noun;
import com.kniazev.cards.word.db.model.word.Verb;
import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.telegram.constants.TextPatterns;
import com.kniazev.cards.word.ui.configuration.ChatMessage.RightAnswer.Color;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AnswerMessage {
    private final Word word;
    private final boolean isCorrect;
    private final Color color;
    private final String userAnswer;
    private final String rightAnswer;

    public String getRightResultTokenWeb() {
        return "Right: ";
    }
    
    public String getRightResultTokenTg() {
        return "**Right**" + TextPatterns.RIGHT_ANSWER_ICON;
    }

    public String getWrongResultTokenWeb() {
        return "Wrong, right answer: ";
    }
    
    public String getWrongResultTokenTg() {
        return "*Wrong*" + TextPatterns.WRONG_ANSWER_ICON + "\n\nRight answer: ";
    }

    public String getRightAnswerTokenWeb() {
        return rightAnswer;
    }
    
    public String getRightAnswerTokenTg() {
        String emodji = switch (color) {
            case BLUE: yield TextPatterns.BLUE_CIRCLE_ICON;
            case GREEN: yield TextPatterns.GREEN_CIRCLE_ICON;
            case RED: yield TextPatterns.RED_CIRCLE_ICON;
            default: yield "";
        };
        
        return StringUtils.isEmpty(emodji) ? String.format("*%s*", rightAnswer) : String.format("*%s* %s", rightAnswer, emodji);
    }
    
    public String getHints() {
        StringBuilder sb = new StringBuilder("```" + word.getLevel().name() + "\n");
        
        if (word instanceof Noun noun) {
            Optional.ofNullable(noun.getFullDe())
                .ifPresent(sb::append);
            
            Optional.ofNullable(noun.getFullPlural())
                .ifPresent(s -> {
                    if (sb.length() > 1) {
                        sb.append(", ");
                    }
                    
                    sb.append(s);
                });
        } else if (word instanceof Verb verb) {
            sb.append(String.format("ich | %s\n", verb.getIch()));
            sb.append(String.format("du  | %s\n", verb.getDu()));
            sb.append(String.format("er  | %s\n", verb.getEr()));
            sb.append(String.format("wir | %s\n", verb.getWir()));
            sb.append(String.format("ihr | %s\n", verb.getIhr()));
            sb.append(String.format("Sie | %s\n", verb.getSie()));
            sb.append(String.format("Partizip II: %s", verb.getPartizip2()));
        } else {
            sb.append(String.format("%s", word.getDe()));
        }
        
        sb.append("```");

        return sb.toString();
    }

    public String getUsageExampleTg() {
        if (StringUtils.isBlank(word.getSample())) {
            return "";
        }

        return "\n\n" + TextPatterns.GER_FLAG + " " + word.getSample()
                + "\n" + TextPatterns.RUS_FLAG + " " + word.getSampleRu();
    }

}
