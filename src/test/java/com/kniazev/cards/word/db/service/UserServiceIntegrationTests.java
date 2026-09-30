package com.kniazev.cards.word.db.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kniazev.cards.word.WordCardsApplication;
import com.kniazev.cards.word.db.service.UserService.TelegramUserResolution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Runs the Telegram-identity logic of {@link UserService} against a real Postgres, with the full Liquibase changelog
 * (migrations 004 and 005 included). Unit tests mock the repository, so they cannot show that binding a legacy row
 * really works on the database. Deliberately not {@code @Transactional}: a shared persistence context would hide the
 * detached-entity merge that the bot performs in production.
 */
@SpringBootTest(
        classes = WordCardsApplication.class,
        webEnvironment = WebEnvironment.NONE,
        properties = {"telegram.bot.enabled=false", "admin.bot.enabled=false", "spring.ai.google.genai.api-key=test-key-not-used"})
@ActiveProfiles({"prod", "no-vaadin"})
class UserServiceIntegrationTests {

    // The sysadm hash committed in 002-common-data.sql: any valid bcrypt string will do for a fixture row.
    private static final String PASSWORD_HASH = "$2a$10$2dOaDw8KFRSsi03szCIaKex9ksUxAq.uKNS75tpe8Q4j4DhZ/xGoa";
    private static final String PLAYER_ROLES = "{ALL,PLAYER,LEARNER}";
    private static final String ADMIN_ROLES = "{ALL,PLAYER,LEARNER,ADMIN}";

    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.1")
            .withUsername("test")
            .withPassword("test")
            .withDatabaseName("test-db");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.liquibase.default-schema", () -> "test");
    }

    @Autowired
    private UserService userService;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void keepOnlyTheSeededSysadm() {
        // user_settings has no ON DELETE CASCADE; word_score has.
        jdbc.update("delete from user_settings where user_id <> 1");
        jdbc.update("delete from users where id <> 1");
    }

    @Test
    void aLegacyAccountIsBoundWithoutCreatingRowsOrTouchingItsPassword() {
        long id = legacyAccount("alice", PLAYER_ROLES, null, true);
        int rowsBefore = count("select count(*) from users");

        Long resolved = userService.findOrBindByTelegramId(42L, "alice").orElseThrow().getId();

        assertThat(resolved).isEqualTo(id);
        assertThat(count("select count(*) from users")).isEqualTo(rowsBefore);
        assertThat(jdbc.queryForObject("select telegram_id from users where id = ?", Long.class, id)).isEqualTo(42L);
        assertThat(jdbc.queryForObject("select password from users where id = ?", String.class, id)).isEqualTo(PASSWORD_HASH);
    }

    @Test
    void aLegacyAccountWithoutASettingsRowIsBoundToo() {
        long id = legacyAccount("bella", PLAYER_ROLES, null, false);

        assertThat(userService.findOrBindByTelegramId(43L, "bella").orElseThrow().getId()).isEqualTo(id);
        assertThat(jdbc.queryForObject("select telegram_id from users where id = ?", Long.class, id)).isEqualTo(43L);
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
        assertThat(jdbc.queryForObject("select telegram_id from users where id = ?", Long.class, id)).isEqualTo(574239939L);
        assertThat(jdbc.queryForObject("select roles::text from users where id = ?", String.class, id)).isEqualTo(ADMIN_ROLES);
    }

    @Test
    void migration005LocksTheSeededSysadmAgainstWhoeverHoldsItsHandle() {
        assertThat(jdbc.queryForObject("select telegram_id from users where username = 'sysadm'", Long.class)).isEqualTo(-1L);

        TelegramUserResolution resolution = userService.getOrCreateByTelegramId(5L, "sysadm");

        assertThat(resolution.user().getUsername()).isEqualTo("sysadm_5");
        assertThat(resolution.newPassword()).isNotBlank();
        assertThat(jdbc.queryForObject("select telegram_id from users where username = 'sysadm'", Long.class)).isEqualTo(-1L);
    }

    @Test
    void anAccountBoundToAnotherTelegramIdIsNeverTaken() {
        long id = legacyAccount("bob", PLAYER_ROLES, 1L, true);

        TelegramUserResolution resolution = userService.getOrCreateByTelegramId(2L, "bob");

        assertThat(resolution.user().getId()).isNotEqualTo(id);
        assertThat(resolution.user().getUsername()).isEqualTo("bob_2");
        assertThat(jdbc.queryForObject("select telegram_id from users where id = ?", Long.class, id)).isEqualTo(1L);
    }

    @Test
    void aHandleThatDiffersOnlyByCaseIsNotBound() {
        long id = legacyAccount("Dave", PLAYER_ROLES, null, true);

        TelegramUserResolution resolution = userService.getOrCreateByTelegramId(11L, "dave");

        // Known limitation: the match is exact, so the person gets a second account. See the README for merging by hand.
        assertThat(resolution.user().getId()).isNotEqualTo(id);
        assertThat(resolution.user().getUsername()).isEqualTo("dave");
        assertThat(jdbc.queryForObject("select telegram_id from users where id = ?", Long.class, id)).isNull();
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

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }
}
