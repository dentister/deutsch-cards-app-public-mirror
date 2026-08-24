package com.kniazev.cards.word.db.model.word;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@ToString(of = {"id", "de", "tags"})
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder

@Entity
@Table(name = "word")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "word_type")
@SequenceGenerator(name= "word_seq_gen", sequenceName = "word_seq", initialValue=1, allocationSize = 1)
public class Word {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="word_seq_gen")
    protected Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "word_type", insertable = false, updatable = false)
    protected WordType wordType;

    @Column(name = "ru")
    protected String ru;

    @Column(name = "de")
    protected String de;

    @Column(name = "en")
    protected String en;

    @Column(name = "tags", columnDefinition = "text[]")
    @Type(value = ListArrayType.class)
    private List<String> tags;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "level")
    private WordLevel level;

    @Column(name = "sample")
    private String sample;

    @Column(name = "sample_ru")
    private String sampleRu;

    @Column(name = "sample_en")
    private String sampleEn;

    @UpdateTimestamp
    @Column(name = "last_modified")
    private LocalDateTime lastModified;

    @Column(name = "created_by")
    private String createdBy;

    public Set<String> getRuAsSet() {
        return Arrays.stream(ru.split(";")).collect(Collectors.toSet());
    }

    public void setRuSetAsString(Set<String> values) {
        ru = StringUtils.join(values, ";");
    }
    
    public enum WordType {
        NOUN, VERB, ADJECTIVE, ADVERB, PHRASE;
    }

    public enum WordLevel {
        A1, A2, B1, B2, C1, C2;
    }
    
}
