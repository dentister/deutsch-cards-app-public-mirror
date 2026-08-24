package com.kniazev.cards.word.game;

import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.i18n.Messages;

import java.util.Locale;
import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public class TaskMessage {
    private final TaskEnum taskId;
    private final Word word;

    public String getTaskText(Locale locale) {
        String ru = word.getRu();
        String level = word.getLevel().name();
        Map<String, String> map = Map.of("ru", ru, "level", level);

        String taskText = switch (taskId) {
            case SINGULAR_NOUN: yield Messages.get("game.task.singular_noun", locale, map);
            case PLURAL_NOUN:   yield Messages.get("game.task.plural_noun", locale, map);
            case VERB:          yield Messages.get("game.task.verb", locale, map);
            case ICH_VERB:      yield Messages.get("game.task.ich_verb", locale, map);
            case DU_VERB:       yield Messages.get("game.task.du_verb", locale, map);
            case ER_VERB:       yield Messages.get("game.task.er_verb", locale, map);
            case WIR_VERB:      yield Messages.get("game.task.wir_verb", locale, map);
            case IHR_VERB:      yield Messages.get("game.task.ihr_verb", locale, map);
            case SIE_VERB:      yield Messages.get("game.task.sie_verb", locale, map);
            case PARTIZIP2:     yield Messages.get("game.task.partizip2", locale, map);
            case ADJECTIVE:     yield Messages.get("game.task.adjective", locale, map);
            case ADVERB:        yield Messages.get("game.task.adverb", locale, map);
            case PHRASE:        yield Messages.get("game.task.phrase", locale, map);
        };

        return taskText;
    }

}
