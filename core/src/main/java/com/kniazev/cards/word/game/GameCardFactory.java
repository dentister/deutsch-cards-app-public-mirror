package com.kniazev.cards.word.game;

import com.kniazev.cards.word.game.representation.TaskEnum;
import com.kniazev.cards.word.game.representation.WordCard;
import com.kniazev.cards.word.model.dictionary.*;
import com.kniazev.cards.word.model.dictionary.Noun.GenderType;

import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class GameCardFactory {

    public static WordCard buildCard(Word word) {
        return switch (word) {
            case Noun noun     -> buildNounCard(noun);
            case Verb verb     -> buildVerbCard(verb);
            case Adjective adj -> buildAdjectiveCard(adj);
            case Adverb adv    -> buildAdverbCard(adv);
            case Phrase phrase -> buildPhraseCard(phrase);
            default            -> throw new IllegalStateException("Unknown word: " + word);
        };
    }

    /*
     * It's needed due to already-displayed card must stay consistent with itself
     * between being shown and being checked
     */
    public static WordCard rebuildCard(Word word, TaskEnum taskId) {
        Supplier<String> rightAnswer = switch (taskId) {
            case SINGULAR_NOUN  -> ((Noun) word)::getFullDe;
            case PLURAL_NOUN    -> ((Noun) word)::getFullPlural;
            case VERB           -> ((Verb) word)::getDe;
            case ICH_VERB       -> ((Verb) word)::getIch;
            case DU_VERB        -> ((Verb) word)::getDu;
            case ER_VERB        -> ((Verb) word)::getEr;
            case WIR_VERB       -> ((Verb) word)::getWir;
            case IHR_VERB       -> ((Verb) word)::getIhr;
            case SIE_VERB       -> ((Verb) word)::getSie;
            case PARTIZIP2      -> ((Verb) word)::getPartizip2;
            case ADJECTIVE      -> ((Adjective) word)::getDe;
            case ADVERB         -> ((Adverb) word)::getDe;
            case PHRASE         -> ((Phrase) word)::getDe;
        };

        return new WordCard(word, taskId, rightAnswer);
    }

    private static WordCard buildNounCard(Noun noun) {
        if (noun.getGender() == GenderType.PL) {
            return new WordCard(noun, TaskEnum.PLURAL_NOUN, noun::getFullPlural);
        } 
        
        if (StringUtils.isEmpty(noun.getPlural())) {
            return new WordCard(noun, TaskEnum.SINGULAR_NOUN, noun::getFullDe);
        } 
        
        return ThreadLocalRandom.current().nextBoolean()
                ? new WordCard(noun, TaskEnum.PLURAL_NOUN, noun::getFullPlural)
                : new WordCard(noun, TaskEnum.SINGULAR_NOUN, noun::getFullDe);
    }
    
    private static WordCard buildVerbCard(Verb verb) {
        return switch (ThreadLocalRandom.current().nextInt(8)) {
            case 0 -> new WordCard(verb, TaskEnum.VERB, verb::getDe);
            case 1 -> new WordCard(verb, TaskEnum.ICH_VERB, verb::getIch);
            case 2 -> new WordCard(verb, TaskEnum.DU_VERB, verb::getDu);
            case 3 -> new WordCard(verb, TaskEnum.ER_VERB, verb::getEr);
            case 4 -> new WordCard(verb, TaskEnum.WIR_VERB, verb::getWir);
            case 5 -> new WordCard(verb, TaskEnum.IHR_VERB, verb::getIhr);
            case 6 -> new WordCard(verb, TaskEnum.SIE_VERB, verb::getSie);
            case 7 -> new WordCard(verb, TaskEnum.PARTIZIP2, verb::getPartizip2);
            default -> throw new IllegalStateException("Unsupported task");
        };
    }
    
    private static WordCard buildAdjectiveCard(Adjective adjective) {
        return new WordCard(adjective, TaskEnum.ADJECTIVE, adjective::getDe);
    }

    private static WordCard buildAdverbCard(Adverb adverb) {
        return new WordCard(adverb, TaskEnum.ADVERB, adverb::getDe);
    }

    private static WordCard buildPhraseCard(Phrase phrase) {
        return new WordCard(phrase, TaskEnum.PHRASE, phrase::getDe);
    }

}
