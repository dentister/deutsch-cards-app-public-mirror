package com.kniazev.cards.word.game;

import com.kniazev.cards.word.db.model.word.Word.WordLevel;
import com.kniazev.cards.word.db.model.word.Word.WordType;

import java.util.List;

import lombok.Data;

@Data
public class GameConfiguration {
    private WordLevel maxLevel;
    private WordType wordType;
    private List<String> tags;
    private Boolean showWordsPreview;
    private List<Long> specificWordIds;

    public WordLevel getMaxLevel() {
        return maxLevel == null ? WordLevel.C2 : maxLevel;
    }

    public Boolean getShowWordsPreview() {
        return showWordsPreview == null ? Boolean.TRUE : showWordsPreview;
    }

    public List<Long> getSpecificWordIds() {
        return specificWordIds == null ? List.of() : specificWordIds;
    }
}
