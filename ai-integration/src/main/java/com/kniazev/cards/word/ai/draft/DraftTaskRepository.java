package com.kniazev.cards.word.ai.draft;

import com.kniazev.cards.word.ai.draft.DraftTask.TaskStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DraftTaskRepository extends JpaRepository<DraftTask, Long> {
    Optional<DraftTask> findFirstByStatusOrderByAttemptsAscIdAsc(TaskStatus status);
}
