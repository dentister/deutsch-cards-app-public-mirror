package com.kniazev.cards.word.db.services;

import com.kniazev.cards.word.db.model.User;
import com.kniazev.cards.word.db.repository.UserRepository;
import com.kniazev.cards.word.error.handler.exception.InconsistentDataException;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor

@Service
public class UserService implements FilteredEntityService<User>, UserDetailsService, UserDetailsPasswordService {
    private final UserRepository repository;
    
    @Lazy
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Lazy
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public JpaRepository<User, Long> getRepository() {
        return repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return getOneByUsername(username);
    }
    
    public User getOneByUsername(String username) {
        return findOneByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException(String.format("User '%s' not found", username)));
    }
    
    public Optional<User> findOneByUsername(String username) {
        List<User> list = repository.findByUsername(username);
        
        if (CollectionUtils.size(list) > 1) {
            throw new InconsistentDataException(String.format( "Too much users with one username [username=%s, number=%d]",
                    username, list.size()));
        }
        
        return CollectionUtils.isEmpty(list) ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<User> save(Iterable<User> entities) {
        entities.forEach(this::encrypt);
        
        return repository.saveAll(entities);
    }
    
    @Override
    public User save(User entity) {
        return repository.save(encrypt(entity));
    }
    
    @Override
    public List<User> saveAndFlush(Iterable<User> entities) {
        entities.forEach(this::encrypt);
        
        return repository.saveAllAndFlush(entities);
    }
    
    @Override
    public User saveAndFlush(User entity) {
        return repository.saveAndFlush(encrypt(entity));
    }
    
    @Override
    public UserDetails updatePassword(UserDetails userDetails, String newPassword) {
        User user = getOneByUsername(userDetails.getUsername());
        
        user.setPassword(newPassword);
        
        return save(user);
    }
    
    private User encrypt(User user) {
        String password = user.getPassword();
        
        user.setPassword(passwordEncoder.encode(password));
        
        return user;
    }

    public boolean userExists(String userName) {
        return findOneByUsername(userName).isPresent();
    }
}
