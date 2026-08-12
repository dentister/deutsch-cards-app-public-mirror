package com.kniazev.cards.word.game;

import com.kniazev.cards.word.db.model.word.Word;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor

@Getter
public class TaskMessage {
    private final TaskEnum taskId;
    private final Word word;
    
    public String getTaskTextWeb() {
        String ru = word.getRu();
        
        return switch (taskId) {
            case SINGULAR_NOUN: yield String.format("Next noun: < %s >", ru);
            case PLURAL_NOUN: String.format("Next noun in plural form: < %s >", ru);
            case VERB: yield String.format("Next verb: < %s >", ru);
            case ICH_VERB: yield String.format("Next verb: ich < %s >", ru);
            case DU_VERB: yield String.format("Next verb: du < %s >", ru);
            case ER_VERB: yield String.format("Next verb: er < %s >", ru);
            case WIR_VERB: yield String.format("Next verb: wir < %s >", ru);
            case IHR_VERB: yield String.format("Next verb: ihr < %s >", ru);
            case SIE_VERB: yield String.format("Next verb: Sie < %s >", ru);
            case PARTIZIP2: yield String.format("Next verb: ich habe/ist < %s >", ru);
            case ADJECTIVE: yield String.format("Next adjective: < %s >", ru);
            case ADVERB: yield String.format("Next adverb: < %s >", ru);
            case PHRASE: yield String.format("Next phrase: < %s >", ru);
        };
    }
    
    public String getTaskTextTg() {
        String ru = word.getRu();
        String level = word.getLevel().name();
        
        String taskText = switch (taskId) {
            case SINGULAR_NOUN: yield String.format("Next noun: *%s* (singular, %s)", ru, level);
            case PLURAL_NOUN: yield String.format("Next noun: *%s* (plural, %s)", ru, level);
            case VERB: yield String.format("Next verb: *%s* (%s)", ru, level);
            case ICH_VERB: yield String.format("Next verb: ich *%s* (%s)", ru, level);
            case DU_VERB: yield String.format("Next verb: du *%s* (%s)", ru, level);
            case ER_VERB: yield String.format("Next verb: er *%s* (%s)", ru, level);
            case WIR_VERB: yield String.format("Next verb: wir *%s* (%s)", ru, level);
            case IHR_VERB: yield String.format("Next verb: ihr *%s* (%s)", ru, level);
            case SIE_VERB: yield String.format("Next verb: Sie *%s* (%s)", ru, level);
            case PARTIZIP2: yield String.format("Next verb: ich habe/ist *%s* (%s)", ru, level);
            case ADJECTIVE: yield String.format("Next adjective: *%s* (%s)", ru, level);
            case ADVERB: yield String.format("Next adverb: *%s* (%s)", ru, level);
            case PHRASE: yield String.format("Next phrase: *%s* (%s)", ru, level);
        };
        
        return taskText;
    }

}
