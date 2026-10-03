package com.kniazev.cards.word.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kniazev.cards.word.model.user.User;
import com.kniazev.cards.word.service.UserService;
import com.kniazev.cards.word.service.UserService.TelegramUserResolution;
import com.kniazev.cards.word.support.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Runs the password handling and the Telegram-identity logic of {@link UserService} against a real Postgres, with the
 * full Liquibase changelog (migrations 004 and 005 included), so that binding a legacy row and the unique indexes are
 * proven on the database rather than on a mocked repository. Legacy rows are inserted with plain SQL on purpose: they
 * predate the entity (no settings row, no Telegram id) and cannot be produced through the service any more.
 */
class UserServiceIT extends AbstractIntegrationTest {

    // The sysadm hash committed in 002-common-data.sql: any valid bcrypt string will do for a fixture row.
    private static final String PASSWORD_HASH = "$2a$10$2dOaDw8KFRSsi03szCIaKex9ksUxAq.uKNS75tpe8Q4j4DhZ/xGoa";
    private static final String PLAYER_ROLES = "{ALL,PLAYER,LEARNER}";
    private static final String ADMIN_ROLES = "{ALL,PLAYER,LEARNER,ADMIN}";

    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void saveEncodesAPlainPasswordExactlyOnce() {
        userService.save(testData.newUser("alice", "secret"));

        String stored = storedPassword("alice");

        assertThat(stored).isNotEqualTo("secret");
        assertThat(encoder.matches("secret", stored)).isTrue();
    }

    @Test
    void saveKeepsAnAlreadyHashedPassword() {
        String hash = encoder.encode("secret");

        userService.save(testData.newUser("alice", hash));

        assertThat(storedPassword("alice")).isEqualTo(hash);
    }

    @Test
    void everySaveOverloadKeepsAnAlreadyHashedPassword() {
        String hash = encoder.encode("secret");

        userService.save(testData.newUser("a", hash));
        userService.saveAndFlush(testData.newUser("b", hash));
        userService.save(List.of(testData.newUser("c", hash)));
        userService.saveAndFlush(List.of(testData.newUser("d", hash)));

        assertThat(List.of("a", "b", "c", "d")).extracting(this::storedPassword).containsOnly(hash);
    }

    @Test
    void theCommittedSeedHashSurvivesASaveOfTheSeededSysadm() {
        userService.save(userService.getOneByUsername("sysadm"));

        assertThat(storedPassword("sysadm")).isEqualTo(PASSWORD_HASH);
    }

    @Test
    void updatePasswordDoesNotEncodeTheAlreadyEncodedPasswordItReceives() {
        User stored = testData.user("alice");
        String newHash = encoder.encode("new");

        userService.updatePassword(stored, newHash);

        assertThat(storedPassword("alice")).isEqualTo(newHash);
    }

    @Test
    void aPasswordHandedOutByRegeneratePasswordSurvivesLaterSaves() {
        testData.user("alice");

        String plain = userService.regeneratePassword("alice");

        // GameService.saveGameConfiguration / restartGame load a user and save it again.
        userService.save(userService.getOneByUsername("alice"));
        userService.save(userService.getOneByUsername("alice"));

        assertThat(encoder.matches(plain, storedPassword("alice"))).isTrue();
    }

    @Test
    void aLegacyAccountIsBoundWithoutCreatingRowsOrTouchingItsPassword() {
        long id = legacyAccount("alice", PLAYER_ROLES, null, true);
        int rowsBefore = count("select count(*) from users");

        Long resolved = userService.findOrBindByTelegramId(42L, "alice").orElseThrow().getId();

        assertThat(resolved).isEqualTo(id);
        assertThat(count("select count(*) from users")).isEqualTo(rowsBefore);
        assertThat(telegramIdOf(id)).isEqualTo(42L);
        assertThat(jdbc.queryForObject("select password from users where id = ?", String.class, id)).isEqualTo(PASSWORD_HASH);
    }

    @Test
    void aLegacyAccountWithoutASettingsRowIsBoundToo() {
        long id = legacyAccount("bella", PLAYER_ROLES, null, false);

        assertThat(userService.findOrBindByTelegramId(43L, "bella").orElseThrow().getId()).isEqualTo(id);
        assertThat(telegramIdOf(id)).isEqualTo(43L);
    }

    @Test
    void anAdminAccountIsBoundInsteadOfGettingAShadowAccount() {
        long id = legacyAccount("mr_dentister", ADMIN_ROLES, null, true);
        int rowsBefore = count("select count(*) from users");

        TelegramUserResolution resolution = userService.getOrCreateByTelegramId(574239939L, "mr_dentister");

        assertThat(resolution.user().getId()).isEqualTo(id);
        assertThat(resolution.newPassword()).isNull();
        assertThat(count("select count(*) from users")).isEqualTo(rowsBefore);
        assertThat(count("select count(*) from users where username = 'mr_dentister_574239939'")).isZero();
        assertThat(telegramIdOf(id)).isEqualTo(574239939L);
        assertThat(jdbc.queryForObject("select roles::text from users where id = ?", String.class, id)).isEqualTo(ADMIN_ROLES);
    }

    @Test
    void theAccountAlreadyBoundToTheTelegramIdIsReturnedAsIs() {
        long id = legacyAccount("alice", PLAYER_ROLES, 42L, true);
        legacyAccount("somebody_else", PLAYER_ROLES, null, true);
        int rowsBefore = count("select count(*) from users");

        assertThat(userService.findOrBindByTelegramId(42L, "somebody_else").orElseThrow().getId()).isEqualTo(id);

        TelegramUserResolution resolution = userService.getOrCreateByTelegramId(42L, "alice");

        assertThat(resolution.user().getId()).isEqualTo(id);
        assertThat(resolution.newPassword()).isNull();
        assertThat(count("select count(*) from users")).isEqualTo(rowsBefore);
        assertThat(count("select count(*) from users where telegram_id is null")).isEqualTo(1);
    }

    @Test
    void migration005LocksTheSeededSysadmAgainstWhoeverHoldsItsHandle() {
        assertThat(jdbc.queryForObject("select telegram_id from users where username = 'sysadm'", Long.class)).isEqualTo(-1L);

        TelegramUserResolution resolution = userService.getOrCreateByTelegramId(5L, "sysadm");

        assertThat(resolution.user().getUsername()).isEqualTo("sysadm_5");
        assertThat(resolution.user().getRoles()).doesNotContain("ADMIN");
        assertThat(resolution.newPassword()).isNotBlank();
        assertThat(jdbc.queryForObject("select telegram_id from users where username = 'sysadm'", Long.class)).isEqualTo(-1L);
    }

    @Test
    void anAccountBoundToAnotherTelegramIdIsNeverTaken() {
        long id = legacyAccount("bob", PLAYER_ROLES, 1L, true);

        assertThat(userService.findOrBindByTelegramId(2L, "bob")).isEmpty();

        TelegramUserResolution resolution = userService.getOrCreateByTelegramId(2L, "bob");

        assertThat(resolution.user().getId()).isNotEqualTo(id);
        assertThat(resolution.user().getUsername()).isEqualTo("bob_2");
        assertThat(resolution.user().getTelegramId()).isEqualTo(2L);
        assertThat(telegramIdOf(id)).isEqualTo(1L);
    }

    @Test
    void aHandleThatDiffersOnlyByCaseIsNotBound() {
        long id = legacyAccount("Dave", PLAYER_ROLES, null, true);

        TelegramUserResolution resolution = userService.getOrCreateByTelegramId(11L, "dave");

        // Known limitation: the match is exact, so the person gets a second account. See the README for merging by hand.
        assertThat(resolution.user().getId()).isNotEqualTo(id);
        assertThat(resolution.user().getUsername()).isEqualTo("dave");
        assertThat(telegramIdOf(id)).isNull();
    }

    @Test
    void withoutATelegramUsernameNothingIsBoundByHandle() {
        legacyAccount("tg-42", PLAYER_ROLES, null, true);

        assertThat(userService.findOrBindByTelegramId(42L, null)).isEmpty();
        assertThat(userService.findOrBindByTelegramId(42L, "  ")).isEmpty();
        assertThat(count("select count(*) from users where telegram_id = 42")).isZero();
    }

    @Test
    void aNewTelegramUserGetsAPlayerAccountNamedAfterTheHandleAndAPasswordThatWorks() {
        TelegramUserResolution resolution = userService.getOrCreateByTelegramId(42L, "alice");

        assertThat(resolution.newPassword()).isNotBlank();
        assertThat(encoder.matches(resolution.newPassword(), storedPassword("alice"))).isTrue();
        assertThat(resolution.user()).satisfies(user -> {
            assertThat(user.getUsername()).isEqualTo("alice");
            assertThat(user.getTelegramId()).isEqualTo(42L);
            assertThat(user.getRoles()).containsExactly("ALL", "PLAYER", "LEARNER");
            assertThat(user.isEnabled()).isTrue();
        });
        assertThat(count("select count(*) from user_settings where user_id = " + resolution.user().getId())).isEqualTo(1);
    }

    @Test
    void aTelegramUserWithoutAUsernameGetsAHyphenatedHandle() {
        TelegramUserResolution resolution = userService.getOrCreateByTelegramId(42L, null);

        assertThat(resolution.user().getUsername()).isEqualTo("tg-42");
        assertThat(resolution.newPassword()).isNotBlank();
    }

    @Test
    void aSuffixedHandleNeverExceedsTheColumnLength() {
        String longHandle = "a".repeat(40);

        legacyAccount(longHandle, PLAYER_ROLES, 1L, false);

        TelegramUserResolution resolution = userService.getOrCreateByTelegramId(1234567890123L, longHandle);

        assertThat(resolution.user().getUsername()).hasSize(50).endsWith("_1234567890123");
    }

    @Test
    void aConstraintViolationThatIsNotARaceIsRethrown() {
        String tooLong = "a".repeat(51);

        assertThatThrownBy(() -> userService.getOrCreateByTelegramId(42L, tooLong))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(count("select count(*) from users where telegram_id = 42")).isZero();
    }

    @Test
    void aTelegramIdAndAHandleCanBelongToOneAccountOnly() {
        legacyAccount("first", PLAYER_ROLES, 7L, false);

        assertThatThrownBy(() -> legacyAccount("second", PLAYER_ROLES, 7L, false))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> legacyAccount("first", PLAYER_ROLES, 8L, false))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void twoFirstContactsOfTheSameTelegramUserCreateExactlyOneAccount() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<TelegramUserResolution>> results = new ArrayList<>();

            for (int i = 0; i < 2; i++) {
                results.add(pool.submit(() -> {
                    start.await();

                    return userService.getOrCreateByTelegramId(99L, "carol");
                }));
            }

            start.countDown();

            TelegramUserResolution first = results.get(0).get(30, TimeUnit.SECONDS);
            TelegramUserResolution second = results.get(1).get(30, TimeUnit.SECONDS);

            // The loser of the race gets the winner's row and no password: only one caller may hand out a credential.
            assertThat(first.user().getId()).isEqualTo(second.user().getId());
            assertThat(List.of(first, second)).filteredOn(r -> r.newPassword() != null).hasSize(1);
            assertThat(count("select count(*) from users where telegram_id = 99")).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private long legacyAccount(String username, String roles, Long telegramId, boolean withSettings) {
        Long id = jdbc.queryForObject(
                "insert into users (username, password, enabled, roles, telegram_id) values (?, ?, true, ?::text[], ?) returning id",
                Long.class, username, PASSWORD_HASH, roles, telegramId);

        if (withSettings) {
            jdbc.update("insert into user_settings (user_id, game_configuration) values (?, '{}'::json)", id);
        }

        return id;
    }

    private String storedPassword(String username) {
        return jdbc.queryForObject("select password from users where username = ?", String.class, username);
    }

    private Long telegramIdOf(long userId) {
        return jdbc.queryForObject("select telegram_id from users where id = ?", Long.class, userId);
    }

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }
}
