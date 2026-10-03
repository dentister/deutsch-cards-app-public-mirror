package com.kniazev.cards.word.model.dictionary;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)

@Entity
@Table(name = "noun")
@DiscriminatorValue("NOUN")
public class Noun extends Word {

    @Column(name = "plural")
    private String plural;
    
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "gender")
    private GenderType gender;
    
    public String getFullDe() {
        return String.format("%s %s", getArticle(), de);
    }
    
    public String getFullPlural() {
        return StringUtils.isEmpty(plural) ? null : String.format("die %s", plural);
    }
    
    public enum GenderType {
        F, M, N, PL;
    }
    
    private String getArticle() {
        return switch (gender) {
            case PL:
            case F: yield "die";
            case M: yield "der";
            case N: yield "das";
        };
    }
}
