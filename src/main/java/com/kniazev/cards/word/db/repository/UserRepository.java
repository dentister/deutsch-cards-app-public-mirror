package com.kniazev.cards.word.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kniazev.cards.word.db.model.User;
import java.util.List;
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByUsername(String username);

    Optional<User> findByTelegramId(Long telegramId);

}
