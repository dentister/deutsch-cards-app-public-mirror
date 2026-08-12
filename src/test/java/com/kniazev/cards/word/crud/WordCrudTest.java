package com.kniazev.cards.word.crud;

import com.kniazev.cards.word.AbstractWithDbConnectionTest;
import com.kniazev.cards.word.db.model.word.Noun;
import com.kniazev.cards.word.db.model.word.Verb;
import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.db.model.word.Noun.GenderType;
import com.kniazev.cards.word.db.services.WordService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.NonTransientDataAccessException;

import java.util.Optional;

public class WordCrudTest extends AbstractWithDbConnectionTest {
    
    @Autowired
    private WordService wordService;
    
    @BeforeEach
    void setup() {
        wordService.deleteAll();
    }
    
    @Test
    void createNoun() {
        Noun w = dasWasser();
        
        Long persistedNounId = wordService.save(w).getId();
        
        Noun persistedNoun = (Noun) wordService.getOneById(persistedNounId);
        
        Assertions.assertAll(
                () -> Assertions.assertEquals(w.getDe(), persistedNoun.getDe()),
                () -> Assertions.assertEquals(w.getRu(), persistedNoun.getRu()),
                () -> Assertions.assertEquals(w.getPlural(), persistedNoun.getDe()),
                () -> Assertions.assertEquals("das Wasser", persistedNoun.getFullDe()),
                () -> Assertions.assertEquals("die Wasser", persistedNoun.getFullPlural())
        );
    }
    
    @Test
    void duplicatesAreNotAllowed() {
        wordService.save(dasWasser());
        
        Assertions.assertThrows(NonTransientDataAccessException.class, () -> wordService.save(dasWasser()));
    }
    
    @Test
    void updateNoun() {
        Noun w = dasWasser();
        Noun cola = dieCola();
        
        Noun persistedNoun = (Noun) wordService.save(w);
        
        persistedNoun.setRu(cola.getRu());
        persistedNoun.setDe(cola.getDe());
        persistedNoun.setGender(cola.getGender());
        persistedNoun.setPlural(cola.getPlural());
        
        
        Assertions.assertAll(
                () -> Assertions.assertEquals(cola.getDe(), persistedNoun.getDe()),
                () -> Assertions.assertEquals(cola.getRu(), persistedNoun.getRu()),
                () -> Assertions.assertEquals(cola.getPlural(), persistedNoun.getDe()),
                () -> Assertions.assertEquals("die Cola", persistedNoun.getFullDe()),
                () -> Assertions.assertEquals("die Cola", persistedNoun.getFullPlural())
        );
    }
    
    @Test
    void deleteNoun() {
        Word persistedNoun = wordService.save(dasWasser());
        Long persistedNounId = persistedNoun.getId();
        
        wordService.delete(persistedNoun);
        
        Optional<Word> deletedWord = wordService.findOneById(persistedNounId);
        
        Assertions.assertTrue(deletedWord.isEmpty());
    }
    
    @Test
    void createVerb() {
        Verb w = sein();
        
        Long persistedVerbId = wordService.save(w).getId();
        
        Verb persistedNoun = (Verb) wordService.getOneById(persistedVerbId);
        
        Assertions.assertAll(
                () -> Assertions.assertEquals(w.getDe(), persistedNoun.getDe()),
                () -> Assertions.assertEquals(w.getRu(), persistedNoun.getRu()),
                
                () -> Assertions.assertEquals(w.getIch(), persistedNoun.getIch()),
                () -> Assertions.assertEquals(w.getDu(), persistedNoun.getDu()),
                () -> Assertions.assertEquals(w.getEr(), persistedNoun.getEr()),
                () -> Assertions.assertEquals(w.getWir(), persistedNoun.getWir()),
                () -> Assertions.assertEquals(w.getIhr(), persistedNoun.getIhr()),
                () -> Assertions.assertEquals(w.getSie(), persistedNoun.getSie())
                
//                () -> Assertions.assertEquals(3, persistedNoun.getAnchor()),
//                () -> Assertions.assertEquals(0, persistedNoun.getScore())
        );
    }
    
    @Test
    void updateVerb() {
        Verb w = sein();
        Verb haben = haben();
        
        Verb persistedVerb = (Verb) wordService.save(w);
        
        persistedVerb.setRu(haben.getRu());
        persistedVerb.setDe(haben.getDe());
        
        Assertions.assertAll(
                () -> Assertions.assertEquals(haben.getDe(), persistedVerb.getDe()),
                () -> Assertions.assertEquals(haben.getRu(), persistedVerb.getRu())
        );
    }
    
    @Test
    void deleteVerb() {
        Word persistedNoun = wordService.save(dasWasser());
        Long persistedNounId = persistedNoun.getId();
        
        wordService.delete(persistedNoun);
        
        Optional<Word> deletedWord = wordService.findOneById(persistedNounId);
        
        Assertions.assertTrue(deletedWord.isEmpty());
    }
    
    private Noun dasWasser() {
        return Noun.builder()
                .de("Wasser")
                .ru("вода")
                .plural("Wasser")
                .gender(GenderType.N)
                .build();
    }
    
    private Noun dieCola() {
        return Noun.builder()
                .de("Cola")
                .ru("кола")
                .plural("Cola")
                .gender(GenderType.F)
                .build();
    }
    
    private Verb sein() {
        return Verb.builder()
                .de("sein")
                .ru("быть")
                .ich("bin")
                .du("bist")
                .er("ist")
                .wir("sind")
                .ihr("seid")
                .sie("sind")
                .build();
    }
    
    private Verb haben() {
        return Verb.builder()
                .de("haben")
                .ru("иметь")
                .ich("habe")
                .du("hast")
                .er("hat")
                .wir("haben")
                .ihr("habt")
                .sie("haben")
                .build();
    }
}
