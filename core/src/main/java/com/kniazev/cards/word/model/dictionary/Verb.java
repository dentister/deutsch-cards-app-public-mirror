package com.kniazev.cards.word.model.dictionary;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder

@Entity
@Table(name = "verb")
@DiscriminatorValue("VERB")
public class Verb extends Word {

    @Column
    private String ich;
    
    @Column
    private String du;
    
    @Column
    private String er;
    
    @Column
    private String wir;
    
    @Column
    private String ihr;
    
    @Column
    private String sie;
    
    @Column(name = "partizip_2")
    private String partizip2;
    
    @Column(name = "prefix")
    private String prefix;
    
    @Column(name = "root_verb")
    private String rootVerb;
    
    @Column
    private String notes;
}
