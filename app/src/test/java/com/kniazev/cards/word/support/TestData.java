package com.kniazev.cards.word.support;

import com.kniazev.cards.word.model.WordScore;
import com.kniazev.cards.word.model.dictionary.Adjective;
import com.kniazev.cards.word.model.dictionary.Adverb;
import com.kniazev.cards.word.model.dictionary.Noun;
import com.kniazev.cards.word.model.dictionary.Noun.GenderType;
import com.kniazev.cards.word.model.dictionary.Phrase;
import com.kniazev.cards.word.model.dictionary.Verb;
import com.kniazev.cards.word.model.dictionary.Word;
import com.kniazev.cards.word.model.dictionary.Word.WordLevel;
import com.kniazev.cards.word.model.user.GameConfiguration;
import com.kniazev.cards.word.model.user.User;
import com.kniazev.cards.word.repository.WordScoreRepository;
import com.kniazev.cards.word.service.UserService;
import com.kniazev.cards.word.service.WordService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds the rows integration tests need, through the real services and repositories, so mapping bugs surface in the
 * test instead of being hidden behind hand-written SQL. Everything created here is removed again by {@link #reset()};
 * the seeded dictionary and the {@code sysadm} account (id 1) are never touched.
 * <p>
 * Add a factory method here when another test needs a new kind of fixture (nouns, verbs, telegram users, ...), so that
 * all integration tests share one vocabulary of test data.
 */
@TestComponent
public class TestData {

    /** Marks every word created here; {@link #reset()} deletes by it. Also usable as a game-configuration tag. */
    public static final String FIXTURE_TAG = "it-fixture";

    /**
     * Words created through the UI or the JSON dialog cannot all carry {@link #FIXTURE_TAG}, so a German spelling that
     * starts with this prefix marks a fixture too.
     */
    public static final String FIXTURE_PREFIX = "uitest-";

    private static final long SEEDED_SYSADM_ID = 1L;
    private static final List<String> PLAYER_ROLES = List.of("ALL", "PLAYER", "LEARNER");
    private static final List<String> ADMIN_ROLES = List.of("ALL", "PLAYER", "LEARNER", "ADMIN");

    @Autowired
    private UserService userService;
    @Autowired
    private WordService wordService;
    @Autowired
    private WordScoreRepository wordScoreRepository;
    @Autowired
    private JdbcTemplate jdbc;

    public User user(String username) {
        return userService.saveAndFlush(newUser(username, username + "-password"));
    }

    public User admin(String username) {
        User user = newUser(username, username + "-password");

        user.setRoles(ADMIN_ROLES);

        return userService.saveAndFlush(user);
    }

    public User newUser(String username, String password) {
        User user = User.builder()
                .username(username)
                .password(password)
                .roles(PLAYER_ROLES)
                .enabled(true)
                .build();

        user.getUserSettings().setUser(user);

        return user;
    }

    public Adjective adjective(String de, String ru, WordLevel level, String... extraTags) {
        return (Adjective) wordService.saveAndFlush(Adjective.builder()
                .de(de)
                .ru(ru)
                .level(level)
                .tags(fixtureTags(extraTags))
                .build());
    }

    public Noun noun(String de, String ru, String plural, GenderType gender, String... extraTags) {
        Noun noun = newNoun(de, ru, plural, gender);

        noun.getTags().addAll(Arrays.asList(extraTags));

        return (Noun) wordService.saveAndFlush(noun);
    }

    public Adverb adverb(String de, String ru, String... extraTags) {
        return (Adverb) wordService.saveAndFlush(Adverb.builder()
                .de(de)
                .ru(ru)
                .level(WordLevel.A1)
                .tags(fixtureTags(extraTags))
                .build());
    }

    public Phrase phrase(String de, String ru, String... extraTags) {
        return (Phrase) wordService.saveAndFlush(Phrase.builder()
                .de(de)
                .ru(ru)
                .level(WordLevel.A1)
                .tags(fixtureTags(extraTags))
                .build());
    }

    /**
     * The Nouns, Verbs, Adjectives, Adverbs and Phrases pages read from in-memory caches that nothing refreshes after a
     * save. Call this after seeding words the pages are expected to show.
     */
    public void refreshWordCaches() {
        wordService.findAllCachedNouns(true);
        wordService.findAllCachedVerbs(true);
        wordService.findAllCachedAdjectives(true);
        wordService.findAllCachedAdverbs(true);
        wordService.findAllCachedPhrases(true);
    }

    public Noun newNoun(String de, String ru, String plural, GenderType gender) {
        return Noun.builder()
                .de(de)
                .ru(ru)
                .plural(plural)
                .gender(gender)
                .level(WordLevel.A1)
                .tags(new ArrayList<>(List.of(FIXTURE_TAG)))
                .build();
    }

    public Verb newVerb(String de, String ru, String ich, String du, String er, String wir, String ihr, String sie) {
        return Verb.builder()
                .de(de)
                .ru(ru)
                .ich(ich)
                .du(du)
                .er(er)
                .wir(wir)
                .ihr(ihr)
                .sie(sie)
                .level(WordLevel.A1)
                .tags(new ArrayList<>(List.of(FIXTURE_TAG)))
                .build();
    }

    public Verb verb(String de, String ru, String ich, String du, String er, String wir, String ihr, String sie) {
        return (Verb) wordService.saveAndFlush(newVerb(de, ru, ich, du, er, wir, ihr, sie));
    }

    public GameConfiguration configFor(Word... words) {
        GameConfiguration configuration = new GameConfiguration();

        configuration.setSpecificWordIds(Arrays.stream(words).map(Word::getId).toList());

        return configuration;
    }

    public WordScore score(User user, Word word, int anchor, int score) {
        return wordScoreRepository.saveAndFlush(WordScore.builder()
                .user(user)
                .word(word)
                .anchor(anchor)
                .score(score)
                .build());
    }

    private static List<String> fixtureTags(String... extraTags) {
        List<String> tags = new ArrayList<>(List.of(FIXTURE_TAG));

        tags.addAll(Arrays.asList(extraTags));

        return tags;
    }

    public void reset() {
        moveWordSequencePastSeededIds();

        jdbc.update("delete from word_score");
        jdbc.update("delete from user_settings where user_id <> ?", SEEDED_SYSADM_ID);
        jdbc.update("delete from users where id <> ?", SEEDED_SYSADM_ID);
        jdbc.update("delete from word where ? = any(tags) or de like ?", FIXTURE_TAG, FIXTURE_PREFIX + "%");
    }

    private void moveWordSequencePastSeededIds() {
        jdbc.queryForObject("select setval('word_seq', greatest((select max(id) from word), (select last_value from word_seq)))",
                Long.class);
    }
}
