package com.kniazev.cards.word.db.model.word;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder

@Entity
@Table(name = "adverb")
@DiscriminatorValue("ADVERB")
public class Adverb extends Word {

}
