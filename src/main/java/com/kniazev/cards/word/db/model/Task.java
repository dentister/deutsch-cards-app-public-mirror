package com.kniazev.cards.word.db.model;

import com.kniazev.cards.word.db.model.word.Word.WordType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * One entry of the queue of words the {@code WordDraftJob} has to draft with AI and add to {@code word}.
 * The queue is seeded by a Liquibase migration in descending order of word frequency, so a lower id means
 * a more frequent word.
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder

@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "de_word")
    private String deWord;

    @Enumerated(EnumType.STRING)
    @Column(name = "word_type")
    private WordType wordType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private TaskStatus status = TaskStatus.NEW;

    @Column(name = "attempts")
    @Builder.Default
    private int attempts = 0;

    @Column(name = "error")
    private String error;

    public enum TaskStatus {
        NEW, DONE, FAILED
    }
}
