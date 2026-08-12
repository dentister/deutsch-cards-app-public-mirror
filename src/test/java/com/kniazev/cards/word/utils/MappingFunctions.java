package com.kniazev.cards.word.utils;

import java.util.function.Function;

import org.apache.commons.csv.CSVRecord;

import com.kniazev.cards.word.api.model.NounDto;
import com.kniazev.cards.word.api.model.NounDto.GenderEnum;
import com.kniazev.cards.word.api.model.WordDto;

public class MappingFunctions {
    
    public static Function<CSVRecord, WordDto> toNounDto() {
        return rec -> {
            return new NounDto()
                    .wordType("NOUN")
                    .ru(rec.get("ru"))
                    .de(rec.get("de"))
                    .plural(rec.get("plural"))
                    .gender(GenderEnum.fromValue(rec.get("gender")));
        };
    }
}
