package com.kniazev.cards.word.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kniazev.cards.word.db.model.User;
import java.util.List;


public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByUsername(String username);
    
}
