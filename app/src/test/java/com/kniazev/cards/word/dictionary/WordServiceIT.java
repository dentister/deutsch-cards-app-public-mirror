package com.kniazev.cards.word.dictionary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kniazev.cards.word.model.dictionary.Noun;
import com.kniazev.cards.word.model.dictionary.Noun.GenderType;
import com.kniazev.cards.word.model.dictionary.Verb;
import com.kniazev.cards.word.model.dictionary.Word;
import com.kniazev.cards.word.repository.WordScoreRepository;
import com.kniazev.cards.word.service.WordService;
import com.kniazev.cards.word.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.NonTransientDataAccessException;

import java.util.List;

class WordServiceIT extends AbstractIntegrationTest {

    @Autowired
    private WordService wordService;
    @Autowired
    private WordScoreRepository wordScoreRepository;

    @Test
    void aSecondNounWithTheSameWordIsRejected() {
        wordService.save(testwasser());

        assertThatThrownBy(() -> wordService.save(testwasser())).isInstanceOf(NonTransientDataAccessException.class);
    }

    @Test
    void anUpdatedNounIsPersisted() {
        Noun noun = (Noun) wordService.save(testwasser());

        noun.setDe("Testcola");
        noun.setRu("тестовая кола");
        noun.setGender(GenderType.F);
        noun.setPlural("Testcolas");
        wordService.saveAndFlush(noun);

        Noun persisted = (Noun) wordService.getOneById(noun.getId());

        assertThat(persisted.getFullDe()).isEqualTo("die Testcola");
        assertThat(persisted.getRu()).isEqualTo("тестовая кола");
        assertThat(persisted.getFullPlural()).isEqualTo("die Testcolas");
    }

    @Test
    void aVerbIsPersistedWithAllItsForms() {
        Verb testgehen = testgehen();

        Verb persisted = (Verb) wordService.getOneById(wordService.save(testgehen).getId());

        assertThat(persisted).usingRecursiveComparison()
                .comparingOnlyFields("de", "ru", "ich", "du", "er", "wir", "ihr", "sie")
                .isEqualTo(testgehen);
    }

    @Test
    void anUpdatedVerbIsPersisted() {
        Verb verb = (Verb) wordService.save(testgehen());

        verb.setDe("testhaben");
        verb.setRu("тест-иметь");
        verb.setEr("hat");
        wordService.saveAndFlush(verb);

        Verb persisted = (Verb) wordService.getOneById(verb.getId());

        assertThat(persisted.getDe()).isEqualTo("testhaben");
        assertThat(persisted.getRu()).isEqualTo("тест-иметь");
        assertThat(persisted.getEr()).isEqualTo("hat");
    }

    @Test
    void selectionIsRestrictedToSpecificWordIds() {
        List<Long> picked = wordService.findAll().stream().map(Word::getId).limit(3).toList();

        // p_word_ids restricts the weighted-random pool to exactly these ids ("only selected").
        List<Long> result = wordScoreRepository.findWordScores(null, null, null, null, picked.toArray(new Long[0]));

        assertThat(result).isNotEmpty().isSubsetOf(picked);
        assertThat(result).hasSizeLessThanOrEqualTo(picked.size());
    }

    private Noun testwasser() {
        return testData.newNoun("Testwasser", "тестовая вода", "Testwasser", GenderType.N);
    }

    private Verb testgehen() {
        return testData.newVerb("testgehen", "тест-идти", "testgehe", "testgehst", "testgeht", "testgehen", "testgeht", "testgehen");
    }
}
