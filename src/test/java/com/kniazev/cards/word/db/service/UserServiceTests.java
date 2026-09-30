package com.kniazev.cards.word.db.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kniazev.cards.word.db.model.User;
import com.kniazev.cards.word.db.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    // The sysadm hash committed in 002-common-data.sql: a real bcrypt string produced outside this test.
    private static final String SEED_HASH = "$2a$10$2dOaDw8KFRSsi03szCIaKex9ksUxAq.uKNS75tpe8Q4j4DhZ/xGoa";

    @Mock
    private UserRepository repository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(repository);

        ReflectionTestUtils.setField(service, "passwordEncoder", encoder);
    }

    @Test
    void saveEncodesAPlainPasswordExactlyOnce() {
        User user = user("alice", "secret");

        service.save(user);

        assertThat(user.getPassword()).isNotEqualTo("secret");
        assertThat(encoder.matches("secret", user.getPassword())).isTrue();
    }

    @Test
    void saveKeepsAnAlreadyHashedPassword() {
        String hash = encoder.encode("secret");
        User user = user("alice", hash);

        service.save(user);

        assertThat(user.getPassword()).isEqualTo(hash);
    }

    @Test
    void everySaveOverloadKeepsAnAlreadyHashedPassword() {
        String hash = encoder.encode("secret");
        User first = user("a", hash);
        User second = user("b", hash);
        User third = user("c", hash);
        User fourth = user("d", hash);

        service.save(first);
        service.saveAndFlush(second);
        service.save(List.of(third));
        service.saveAndFlush(List.of(fourth));

        assertThat(List.of(first, second, third, fourth)).extracting(User::getPassword).containsOnly(hash);
    }

    @Test
    void theCommittedSeedHashIsRecognisedAsAHash() {
        User sysadm = user("sysadm", SEED_HASH);

        service.save(sysadm);

        assertThat(sysadm.getPassword()).isEqualTo(SEED_HASH);
    }

    @Test
    void updatePasswordDoesNotEncodeTheAlreadyEncodedPasswordItReceives() {
        User stored = user("alice", encoder.encode("old"));
        String newHash = encoder.encode("new");

        when(repository.findByUsername("alice")).thenReturn(List.of(stored));

        service.updatePassword(stored, newHash);

        assertThat(stored.getPassword()).isEqualTo(newHash);
    }

    @Test
    void aPasswordHandedOutByRegeneratePasswordSurvivesLaterSaves() {
        User stored = user("alice", encoder.encode("old"));

        when(repository.findByUsername("alice")).thenReturn(List.of(stored));

        String plain = service.regeneratePassword("alice");

        // GameService.saveGameConfiguration / restartGame load a user and save it again.
        service.save(stored);
        service.save(stored);

        assertThat(encoder.matches(plain, stored.getPassword())).isTrue();
    }

    @Test
    void findOrBindReturnsTheAccountAlreadyBoundToTheTelegramId() {
        User bound = account("alice", 42L, "PLAYER");

        when(repository.findByTelegramId(42L)).thenReturn(Optional.of(bound));

        assertThat(service.findOrBindByTelegramId(42L, "somebody_else")).containsSame(bound);

        verify(repository, never()).findByUsername(any());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void findOrBindBindsAnUnboundNonAdminAccountByItsExactHandleAndKeepsItsPassword() {
        User legacy = account("alice", null, "PLAYER");
        String passwordBefore = legacy.getPassword();

        when(repository.findByTelegramId(42L)).thenReturn(Optional.empty());
        when(repository.findByUsername("alice")).thenReturn(List.of(legacy));
        when(repository.saveAndFlush(legacy)).thenReturn(legacy);

        Optional<User> result = service.findOrBindByTelegramId(42L, "alice");

        assertThat(result).containsSame(legacy);
        assertThat(legacy.getTelegramId()).isEqualTo(42L);
        assertThat(legacy.getPassword()).isEqualTo(passwordBefore);
    }

    @Test
    void findOrBindBindsAnAdminAccountLikeAnyOtherAndKeepsItsRoles() {
        User admin = account("mr_dentister", null, "ADMIN");

        when(repository.findByTelegramId(42L)).thenReturn(Optional.empty());
        when(repository.findByUsername("mr_dentister")).thenReturn(List.of(admin));
        when(repository.saveAndFlush(admin)).thenReturn(admin);

        assertThat(service.findOrBindByTelegramId(42L, "mr_dentister")).containsSame(admin);
        assertThat(admin.getTelegramId()).isEqualTo(42L);
        assertThat(admin.getRoles()).containsExactly("ADMIN");
    }

    @Test
    void theAdminIsBoundInsteadOfGettingAShadowAccount() {
        User admin = account("mr_dentister", null, "ADMIN");

        when(repository.findByTelegramId(42L)).thenReturn(Optional.empty());
        when(repository.findByUsername("mr_dentister")).thenReturn(List.of(admin));
        when(repository.saveAndFlush(admin)).thenReturn(admin);

        UserService.TelegramUserResolution resolution = service.getOrCreateByTelegramId(42L, "mr_dentister");

        assertThat(resolution.user()).isSameAs(admin);
        assertThat(resolution.newPassword()).isNull();
        verify(repository, times(1)).saveAndFlush(any(User.class));
    }

    @Test
    void findOrBindNeverBindsAnAccountThatBelongsToAnotherTelegramId() {
        User other = account("alice", 1L, "PLAYER");

        when(repository.findByTelegramId(2L)).thenReturn(Optional.empty());
        when(repository.findByUsername("alice")).thenReturn(List.of(other));

        assertThat(service.findOrBindByTelegramId(2L, "alice")).isEmpty();
        assertThat(other.getTelegramId()).isEqualTo(1L);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void findOrBindDoesNotLookAtUsernamesWhenTheTelegramAccountHasNone() {
        when(repository.findByTelegramId(42L)).thenReturn(Optional.empty());

        assertThat(service.findOrBindByTelegramId(42L, null)).isEmpty();
        assertThat(service.findOrBindByTelegramId(42L, "  ")).isEmpty();

        verify(repository, never()).findByUsername(any());
    }

    @Test
    void aBindConflictIsSwallowedAndLeavesTheEntityClean() {
        User legacy = account("alice", null, "PLAYER");

        when(repository.findByTelegramId(42L)).thenReturn(Optional.empty());
        when(repository.findByUsername("alice")).thenReturn(List.of(legacy));
        when(repository.saveAndFlush(legacy)).thenThrow(new DataIntegrityViolationException("duplicate telegram_id"));

        assertThat(service.findOrBindByTelegramId(42L, "alice")).isEmpty();
        assertThat(legacy.getTelegramId()).isNull();
    }

    @Test
    void getOrCreateReturnsTheExistingAccountWithoutANewPassword() {
        User bound = account("alice", 42L, "PLAYER");

        when(repository.findByTelegramId(42L)).thenReturn(Optional.of(bound));

        UserService.TelegramUserResolution resolution = service.getOrCreateByTelegramId(42L, "alice");

        assertThat(resolution.user()).isSameAs(bound);
        assertThat(resolution.newPassword()).isNull();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void getOrCreateCreatesAnAccountHandledByTheTelegramUsername() {
        when(repository.findByTelegramId(42L)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserService.TelegramUserResolution resolution = service.getOrCreateByTelegramId(42L, "alice");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(repository).saveAndFlush(saved.capture());

        assertThat(saved.getValue().getUsername()).isEqualTo("alice");
        assertThat(saved.getValue().getTelegramId()).isEqualTo(42L);
        assertThat(saved.getValue().getRoles()).containsExactly("ALL", "PLAYER", "LEARNER");
        assertThat(saved.getValue().isEnabled()).isTrue();
        assertThat(resolution.newPassword()).isNotBlank();
        assertThat(encoder.matches(resolution.newPassword(), saved.getValue().getPassword())).isTrue();
    }

    @Test
    void getOrCreateGivesAUserWithoutATelegramUsernameAHyphenatedHandle() {
        when(repository.findByTelegramId(42L)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserService.TelegramUserResolution resolution = service.getOrCreateByTelegramId(42L, null);

        assertThat(resolution.user().getUsername()).isEqualTo("tg-42");
        assertThat(resolution.newPassword()).isNotBlank();
    }

    @Test
    void aHandleReclaimedOnTelegramDoesNotInheritTheOldAccount() {
        User previousOwner = account("alice", 1L, "PLAYER");

        when(repository.findByTelegramId(2L)).thenReturn(Optional.empty());
        when(repository.findByUsername("alice")).thenReturn(List.of(previousOwner));
        when(repository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserService.TelegramUserResolution resolution = service.getOrCreateByTelegramId(2L, "alice");

        assertThat(resolution.user()).isNotSameAs(previousOwner);
        assertThat(resolution.user().getUsername()).isEqualTo("alice_2");
        assertThat(resolution.user().getTelegramId()).isEqualTo(2L);
        assertThat(resolution.newPassword()).isNotBlank();
        assertThat(previousOwner.getTelegramId()).isEqualTo(1L);
    }

    @Test
    void theSeededSysadmLockedByTheMigrationIsNeverCapturedByWhoeverHoldsItsHandleOnTelegram() {
        // Migration 005 gives sysadm a negative telegram_id, which no real Telegram user can ever have.
        User sysadm = account("sysadm", -1L, "ADMIN");

        when(repository.findByTelegramId(5L)).thenReturn(Optional.empty());
        when(repository.findByUsername("sysadm")).thenReturn(List.of(sysadm));
        when(repository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserService.TelegramUserResolution resolution = service.getOrCreateByTelegramId(5L, "sysadm");

        assertThat(resolution.user()).isNotSameAs(sysadm);
        assertThat(resolution.user().getUsername()).isEqualTo("sysadm_5");
        assertThat(resolution.user().getRoles()).doesNotContain("ADMIN");
        assertThat(sysadm.getTelegramId()).isEqualTo(-1L);
    }

    @Test
    void aSuffixedHandleNeverExceedsTheColumnLength() {
        String longHandle = "a".repeat(40);
        User taken = account(longHandle, 1L, "PLAYER");

        when(repository.findByTelegramId(1234567890123L)).thenReturn(Optional.empty());
        when(repository.findByUsername(longHandle)).thenReturn(List.of(taken));
        when(repository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserService.TelegramUserResolution resolution = service.getOrCreateByTelegramId(1234567890123L, longHandle);

        assertThat(resolution.user().getUsername()).hasSize(50).endsWith("_1234567890123");
    }

    @Test
    void getOrCreateReturnsTheWinnerWhenItLosesARaceForTheTelegramId() {
        User winner = account("alice", 42L, "PLAYER");

        when(repository.findByTelegramId(42L)).thenReturn(Optional.empty(), Optional.of(winner));
        when(repository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate telegram_id"));

        UserService.TelegramUserResolution resolution = service.getOrCreateByTelegramId(42L, "alice");

        assertThat(resolution.user()).isSameAs(winner);
        assertThat(resolution.newPassword()).isNull();
    }

    @Test
    void getOrCreateRethrowsAConstraintViolationThatIsNotARace() {
        when(repository.findByTelegramId(42L)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("value too long"));

        assertThatThrownBy(() -> service.getOrCreateByTelegramId(42L, "alice"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static User account(String username, Long telegramId, String role) {
        return User.builder()
                .username(username)
                .telegramId(telegramId)
                .password("stored-hash")
                .roles(List.of(role))
                .enabled(true)
                .build();
    }

    private static User user(String username, String password) {
        return User.builder()
                .username(username)
                .password(password)
                .roles(List.of("PLAYER"))
                .enabled(true)
                .build();
    }
}
