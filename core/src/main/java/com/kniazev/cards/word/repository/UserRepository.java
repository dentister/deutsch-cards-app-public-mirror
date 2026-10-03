package com.kniazev.cards.word.repository;

import com.kniazev.cards.word.model.user.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByUsername(String username);

    Optional<User> findByTelegramId(Long telegramId);

}
