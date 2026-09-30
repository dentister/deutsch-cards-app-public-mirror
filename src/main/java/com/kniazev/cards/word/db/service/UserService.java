package com.kniazev.cards.word.db.service;

import com.kniazev.cards.word.db.model.User;
import com.kniazev.cards.word.db.repository.UserRepository;
import com.kniazev.cards.word.error.exception.InconsistentDataException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor

@Service
public class UserService implements FilteredEntityService<User>, UserDetailsService, UserDetailsPasswordService {
    private static final Pattern BCRYPT_HASH = Pattern.compile("\\$2[aby]?\\$\\d{2}\\$[./A-Za-z0-9]{53}");
    private static final int USERNAME_MAX_LENGTH = 50;
    private static final String ADMIN_ROLE = "ADMIN";

    public record TelegramUserResolution(User user, String newPassword) { }

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

        // A row loaded from the DB (or passed to updatePassword) already carries a hash: hashing it again locks the user out.
        if (password == null || !BCRYPT_HASH.matcher(password).matches()) {
            user.setPassword(passwordEncoder.encode(password));
        }

        return user;
    }

    public Optional<User> findOneByTelegramId(long telegramId) {
        return repository.findByTelegramId(telegramId);
    }

    /** Looks the account up by Telegram id, lazily binding a pre-existing unbound account (ADMIN included) by its handle. Never creates. */
    public Optional<User> findOrBindByTelegramId(long telegramId, String telegramUsername) {
        Optional<User> bound = findOneByTelegramId(telegramId);

        return bound.isPresent() ? bound : bindLegacyUser(telegramId, telegramUsername);
    }

    /** Like {@link #findOrBindByTelegramId}, but creates the account when there is none. */
    public TelegramUserResolution getOrCreateByTelegramId(long telegramId, String telegramUsername) {
        Optional<User> existing = findOrBindByTelegramId(telegramId, telegramUsername);

        if (existing.isPresent()) {
            return new TelegramUserResolution(existing.get(), null);
        }

        try {
            return createTelegramUser(telegramId, telegramUsername);
        } catch (DataIntegrityViolationException e) {
            // Lost a race for this Telegram id: the winner's row is authoritative. Anything else is a real error.
            return findOneByTelegramId(telegramId)
                    .map(user -> new TelegramUserResolution(user, null))
                    .orElseThrow(() -> e);
        }
    }

    public String regeneratePassword(String username) {
        return assignRandomPassword(getOneByUsername(username));
    }

    private Optional<User> bindLegacyUser(long telegramId, String telegramUsername) {
        if (StringUtils.isBlank(telegramUsername)) {
            return Optional.empty();
        }

        Optional<User> candidate = findOneByUsername(telegramUsername).filter(user -> user.getTelegramId() == null);

        if (candidate.isEmpty()) {
            return Optional.empty();
        }

        User legacy = candidate.get();

        legacy.setTelegramId(telegramId);

        try {
            // Straight to the repository: the password column must stay untouched.
            User bound = repository.saveAndFlush(legacy);

            if (isAdmin(bound)) {
                log.warn("Bound legacy ADMIN account [handle={}, userId={}] to Telegram id {}", bound.getUsername(), bound.getId(), telegramId);
            } else {
                log.info("Bound legacy account [handle={}, userId={}] to Telegram id {}", bound.getUsername(), bound.getId(), telegramId);
            }

            return Optional.of(bound);
        } catch (DataIntegrityViolationException e) {
            // Another request bound this Telegram id first; leave the managed entity clean and let the caller retry.
            legacy.setTelegramId(null);

            log.warn("Could not bind account [handle={}, userId={}] to Telegram id {}: {}",
                    legacy.getUsername(), legacy.getId(), telegramId, e.getMessage());

            return Optional.empty();
        }
    }

    private static boolean isAdmin(User user) {
        return user.getRoles() != null && user.getRoles().contains(ADMIN_ROLE);
    }

    private TelegramUserResolution createTelegramUser(long telegramId, String telegramUsername) {
        User user = User.builder()
                .username(pickHandle(telegramId, telegramUsername))
                .telegramId(telegramId)
                .roles(List.of("ALL", "PLAYER", "LEARNER"))
                .enabled(true)
                .build();

        user.getUserSettings().setUser(user);

        String plainPassword = assignRandomPassword(user);

        log.info("Created account [handle={}, userId={}, telegramId={}]", user.getUsername(), user.getId(), telegramId);

        return new TelegramUserResolution(user, plainPassword);
    }

    /**
     * The handle is the internal login name: the Telegram username when it is free, otherwise (or when there is none)
     * a name derived from the Telegram id. "tg-" can never collide with a real Telegram username (no hyphens there).
     */
    private String pickHandle(long telegramId, String telegramUsername) {
        String base = StringUtils.isNotBlank(telegramUsername) ? telegramUsername : "tg-" + telegramId;
        String suffix = "_" + telegramId;
        String suffixed = StringUtils.left(base, USERNAME_MAX_LENGTH - suffix.length()) + suffix;

        for (String candidate : List.of(base, suffixed)) {
            if (findOneByUsername(candidate).isEmpty()) {
                return candidate;
            }
        }

        throw new InconsistentDataException(String.format("No free handle for Telegram id %d", telegramId));
    }

    private String assignRandomPassword(User user) {
        String plainPassword = UUID.randomUUID().toString();

        user.setPassword(plainPassword);
        saveAndFlush(user);

        return plainPassword;
    }
}
