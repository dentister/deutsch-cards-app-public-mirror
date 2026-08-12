package com.kniazev.cards.word.services;

import com.kniazev.cards.word.db.model.word.*;
import com.kniazev.cards.word.db.model.word.Noun.GenderType;
import com.kniazev.cards.word.game.TaskEnum;
import com.kniazev.cards.word.game.WordCard;

import org.apache.commons.lang3.StringUtils;

import java.util.Random;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GameCardFactory {
    private static Random random = new Random();
    
    public static WordCard buildCard(Word word) {
        if (word instanceof Noun noun) {
            return buildNounCard().apply(noun);
        } else if (word instanceof Verb verb) {
            return buildVerbCard().apply(verb);
        } else if (word instanceof Adjective adjective) {
            return buildAdjectiveCard().apply(adjective);
        } else if (word instanceof Adverb adverb) {
            return buildAdverbCard().apply(adverb);
        } else if (word instanceof Phrase phrase) {
            return buildPhraseCard().apply(phrase);
        }
        
        log.error("Unknown word: " + word);
        
        return null;
    }
    
    private static Function<Noun, WordCard> buildNounCard() {
        return noun -> {
            int task = random.nextInt(2);
            
            if (noun.getGender() == GenderType.PL) {
                return new WordCard(noun, TaskEnum.PLURAL_NOUN, noun::getFullPlural);
            } else if (StringUtils.isEmpty(noun.getPlural())) {
                return new WordCard(noun, TaskEnum.SINGULAR_NOUN, noun::getFullDe);
            } else {
                return task == 1 
                        ? new WordCard(noun, TaskEnum.PLURAL_NOUN, noun::getFullPlural)
                        : new WordCard(noun, TaskEnum.SINGULAR_NOUN, noun::getFullDe);
            }
        };
    }
    
    private static Function<Verb, WordCard> buildVerbCard() {
        return verb -> {
            int task = random.nextInt(8);
            
            return switch (task) {
                case 0: yield new WordCard(verb, TaskEnum.VERB, verb::getDe);
                case 1: yield new WordCard(verb, TaskEnum.ICH_VERB, verb::getIch);
                case 2: yield new WordCard(verb, TaskEnum.DU_VERB, verb::getDu);
                case 3: yield new WordCard(verb, TaskEnum.ER_VERB, verb::getEr);
                case 4: yield new WordCard(verb, TaskEnum.WIR_VERB, verb::getWir);
                case 5: yield new WordCard(verb, TaskEnum.IHR_VERB, verb::getIhr);
                case 6: yield new WordCard(verb, TaskEnum.SIE_VERB, verb::getSie);
                case 7: yield new WordCard(verb, TaskEnum.PARTIZIP2, verb::getPartizip2);
                default: throw new IllegalStateException("Unsupported task"); 
            };
        };
    }
    
    private static Function<Adjective, WordCard> buildAdjectiveCard() {
        return adjective -> new WordCard(adjective, TaskEnum.ADJECTIVE, adjective::getDe);
    }
    
    private static Function<Adverb, WordCard> buildAdverbCard() {
        return adverb -> new WordCard(adverb, TaskEnum.ADVERB, adverb::getDe);
    }
    
    private static Function<Phrase, WordCard> buildPhraseCard() {
        return phrase -> {
            Random rd = new Random(); 
            char[] ch = phrase.getDe().toCharArray();
            
            for (int i = 0; i < ch.length; i++) {
                if (Character.isLetter(ch[i])) {
                    ch[i] = rd.nextBoolean() ? ch[i] : '*';
                }
            }
            
            return new WordCard(phrase, TaskEnum.PHRASE, phrase::getDe);
        };
    }
}
