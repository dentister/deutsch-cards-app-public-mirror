package com.kniazev.cards.word.db.repository;

import com.kniazev.cards.word.db.model.Task;
import com.kniazev.cards.word.db.model.Task.TaskStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Optional<Task> findFirstByStatusOrderByAttemptsAscIdAsc(TaskStatus status);
}
