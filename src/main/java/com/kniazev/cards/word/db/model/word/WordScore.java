package com.kniazev.cards.word.db.model.word;

import com.kniazev.cards.word.db.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder

@Entity
@Table(name = "word_score")
@SequenceGenerator(name= "word_score_seq_gen", sequenceName = "word_score_seq", initialValue=1, allocationSize = 1)
public class WordScore {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="word_score_seq_gen")
    protected Long id;
    
    @ManyToOne
    @JoinColumn(name = "word_id")
    private Word word;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @Builder.Default
    @Column(name = "anchor")
    protected Integer anchor = 2;
    
    @Builder.Default
    @Column(name = "score")
    protected Integer score = 0;
    
    public void incrementScore() {
        score++;
    }
    
    public void incrementAnchor() {
        anchor++;
    }
}
