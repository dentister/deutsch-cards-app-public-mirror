package com.kniazev.cards.word.controller.adapter;

import com.kniazev.cards.word.api.model.*;
import com.kniazev.cards.word.db.model.word.*;

import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import org.modelmapper.convention.MatchingStrategies;

import java.time.OffsetDateTime;

public class MappingFunctions {
    private static final ModelMapper mapper;

    static {
        mapper = new ModelMapper();
        mapper.getConfiguration().setSkipNullEnabled(true);
        mapper.getConfiguration().setAmbiguityIgnored(true);
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        mapper.getConfiguration().setFieldAccessLevel(AccessLevel.PRIVATE);

        mapper.createTypeMap(String.class, OffsetDateTime.class).setConverter(Converters.stringToOffsetDateTimeConverter);
        mapper.createTypeMap(WordDto.class, Word.class);
        mapper.createTypeMap(NounDto.class, Noun.class).includeBase(WordDto.class, Word.class);
        mapper.createTypeMap(VerbDto.class, Verb.class).includeBase(WordDto.class, Word.class);
        mapper.createTypeMap(AdjectiveDto.class, Adjective.class).includeBase(WordDto.class, Word.class);
        mapper.createTypeMap(AdverbDto.class, Adverb.class).includeBase(WordDto.class, Word.class);
        mapper.createTypeMap(PhraseDto.class, Phrase.class).includeBase(WordDto.class, Word.class);
        
        mapper.createTypeMap(Word.class, WordDto.class);
        mapper.createTypeMap(Noun.class, NounDto.class).includeBase(Word.class, WordDto.class);
    }

    public static <T> T mergeObjects(T sourceObj, T targetObj) {
        mapper.map(sourceObj, targetObj);
        
        return targetObj;
    }


    public static <S, T> T map(S sourceObj, Class<T> clazz) {
        return mapper.map(sourceObj, clazz);
    }
    
    public static WordDto mapToWordDto(Word word) {
        return mapper.map(word, getTargetClass(word));
    }
    
    public static Word mapToWord(WordDto wordDto) {
        return mapper.map(wordDto, getTargetClass(wordDto));
    }
    
    private static Class<? extends WordDto> getTargetClass(Word word) {
        return switch (word.getWordType()) {
            case NOUN: yield NounDto.class;
            case VERB: yield VerbDto.class;
            case ADJECTIVE: yield AdjectiveDto.class;
            case ADVERB: yield AdverbDto.class;
            case PHRASE: yield PhraseDto.class;
            default:
                throw new IllegalArgumentException("Unexpected value: " + word.getWordType());
        };
    }
    
    private static Class<? extends Word> getTargetClass(WordDto wordDto) {
        return switch (wordDto.getWordType()) {
            case "NOUN": yield Noun.class;
            case "VERB": yield Verb.class;
            case "ADJECTIVE": yield Adjective.class;
            case "ADVERB": yield Adverb.class;
            case "PHRASE": yield Phrase.class;
            default:
                throw new IllegalArgumentException("Unexpected value: " + wordDto.getWordType());
        };
    }
}
