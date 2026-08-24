package com.kniazev.cards.word.game;

import com.kniazev.cards.word.db.model.word.Noun;
import com.kniazev.cards.word.db.model.word.Verb;
import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.constant.Emoji;
import com.kniazev.cards.word.i18n.Messages;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Map;
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
    private final Locale locale;

    public String getRightResultToken() {
        return Messages.get("game.right_answer", locale);
    }

    public String getWrongResultToken() {
        return Messages.get("game.wrong_answer", locale, Map.of("rightAnswerToken", getRightAnswerToken()));
    }

    public String getRightAnswerToken() {
        String emodji = switch (color) {
            case BLUE: yield Emoji.BLUE_CIRCLE_ICON;
            case GREEN: yield Emoji.GREEN_CIRCLE_ICON;
            case RED: yield Emoji.RED_CIRCLE_ICON;
            default: yield "";
        };
        String emodjiSuffix = StringUtils.isEmpty(emodji) ? "" : " " + emodji;

        return Messages.get("game.right_answer_token", locale,
                Map.of("rightAnswer", rightAnswer, "emodjiSuffix", emodjiSuffix));
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

    public String getUsageExample() {
        if (StringUtils.isBlank(word.getSample())) {
            return "";
        }

        StringBuilder sb = new StringBuilder("\n")
                .append("\n" + Emoji.GER_FLAG + " " + word.getSample())
                .append("\n" + Emoji.RUS_FLAG + " " + word.getSampleRu())
                .append("\n" + Emoji.ENG_FLAG + " " + word.getSampleEn());

        return sb.toString();
    }

}
