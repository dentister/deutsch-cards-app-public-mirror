package com.kniazev.cards.word.rest;

import static com.kniazev.cards.word.support.TestData.FIXTURE_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kniazev.cards.word.WordCardsApplication;
import com.kniazev.cards.word.api.model.AdjectiveDto;
import com.kniazev.cards.word.api.model.AdverbDto;
import com.kniazev.cards.word.api.model.NounDto;
import com.kniazev.cards.word.api.model.NounDto.GenderEnum;
import com.kniazev.cards.word.api.model.PhraseDto;
import com.kniazev.cards.word.api.model.VerbDto;
import com.kniazev.cards.word.api.model.WordCrudApiV1GetWordRsInner;
import com.kniazev.cards.word.model.dictionary.Noun;
import com.kniazev.cards.word.model.dictionary.Word;
import com.kniazev.cards.word.model.dictionary.Word.WordType;
import com.kniazev.cards.word.service.WordService;
import com.kniazev.cards.word.support.AbstractIntegrationTest;
import com.kniazev.cards.word.test.client.api.WordCrudApi;
import com.kniazev.test.cards.word.test.client.invoker.ApiClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@SpringBootTest(
        classes = WordCardsApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {
                "telegram.bot.enabled=false",
                "admin.bot.enabled=false",
                "word.job.enabled=false",
                "spring.ai.google.genai.api-key=test-key-not-used"})
class WordCrudControllerIT extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;
    @Autowired
    private WordService wordService;

    private WordCrudApi api;

    @BeforeEach
    void createClient() {
        api = new WordCrudApi(new ApiClient(RestClient.create()).setBasePath("http://localhost:" + port));
    }

    @Test
    void aNounSurvivesTheRoundTrip() {
        api.createWord(List.of(noun("Hund", "собака", "Hunde", GenderEnum.M)));

        NounDto found = (NounDto) onlyResultFor("Hund").getWord();

        assertThat(found.getDe()).isEqualTo(FIXTURE_PREFIX + "Hund");
        assertThat(found.getRu()).isEqualTo("собака");
        assertThat(found.getPlural()).isEqualTo("Hunde");
        assertThat(found.getGender()).isEqualTo(GenderEnum.M);
        assertThat(found.getLevel()).isEqualTo(NounDto.LevelEnum.A1);
    }

    @Test
    void aNounIsStoredAsANounInTheDatabase() {
        api.createWord(List.of(noun("Katze", "кошка", "Katzen", GenderEnum.F)));

        Word stored = wordService.findOneByDeAndWordType(FIXTURE_PREFIX + "Katze", WordType.NOUN).orElseThrow();

        assertThat(stored).isInstanceOf(Noun.class);
        assertThat(((Noun) stored).getGender()).isEqualTo(Noun.GenderType.F);
        assertThat(stored.getRu()).isEqualTo("кошка");
    }

    @Test
    void everyWordTypeCanBeCreatedInOneRequestAndComesBackAsItsOwnType() {
        VerbDto verb = new VerbDto().ich("laufe").du("läufst").er("läuft").wir("laufen").ihr("lauft").sie("laufen");
        verb.de(FIXTURE_PREFIX + "laufen").ru("бежать").wordType("VERB").level(VerbDto.LevelEnum.A1);

        api.createWord(List.of(
                noun("Baum", "дерево", "Bäume", GenderEnum.M),
                verb,
                adjective("schnell", "быстрый"),
                adverb("immer", "всегда"),
                phrase("Guten Tag", "добрый день")));

        List<WordCrudApiV1GetWordRsInner> found = api.search(100, FIXTURE_PREFIX);

        assertThat(found).extracting(WordCrudApiV1GetWordRsInner::getWord).satisfiesExactlyInAnyOrder(
                word -> assertThat(word).isInstanceOf(NounDto.class),
                word -> assertThat(word).isInstanceOfSatisfying(VerbDto.class, v -> {
                    assertThat(v.getEr()).isEqualTo("läuft");
                    assertThat(v.getIhr()).isEqualTo("lauft");
                }),
                word -> assertThat(word).isInstanceOf(AdjectiveDto.class),
                word -> assertThat(word).isInstanceOf(AdverbDto.class),
                word -> assertThat(word).isInstanceOf(PhraseDto.class));
    }

    @Test
    void creatingTheSameWordTwiceKeepsTheFirstOne() {
        api.createWord(List.of(adjective("langsam", "медленный")));
        api.createWord(List.of(adjective("langsam", "очень медленный")));

        AdjectiveDto found = (AdjectiveDto) onlyResultFor("langsam").getWord();

        assertThat(found.getRu()).isEqualTo("медленный");
    }

    @Test
    void aWordOfAnUnknownTypeIsRejectedAndNothingIsSaved() {
        AdverbDto unknown = new AdverbDto();
        unknown.de(FIXTURE_PREFIX + "Unbekannt").ru("неизвестное").wordType("SPACESHIP");

        assertThatThrownBy(() -> api.createWord(List.of(unknown)))
                .isInstanceOfSatisfying(RestClientResponseException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThat(api.search(100, FIXTURE_PREFIX)).isEmpty();
    }

    @Test
    void aWordWithoutALevelIsRejectedAndNothingIsSaved() {
        AdverbDto noLevel = adverb("ohneLevel", "без уровня").level(null);

        assertThatThrownBy(() -> api.createWord(List.of(noLevel)))
                .isInstanceOfSatisfying(RestClientResponseException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThat(api.search(100, FIXTURE_PREFIX)).isEmpty();
    }

    @Test
    void searchMatchesTheStartOfTheGermanOrTheRussianSpelling() {
        api.createWord(List.of(adjective("Suchwort", "поисковое слово")));

        assertThat(api.search(100, FIXTURE_PREFIX + "such")).hasSize(1);
        assertThat(api.search(100, "ПОИСКОВОЕ")).extracting(r -> ((AdjectiveDto) r.getWord()).getDe()).contains(FIXTURE_PREFIX + "Suchwort");
        assertThat(api.search(100, FIXTURE_PREFIX + "nichts")).isEmpty();
    }

    @Test
    void searchHonoursTheLimit() {
        api.createWord(List.of(adjective("eins", "один"), adjective("zwei", "два"), adjective("drei", "три")));

        assertThat(api.search(100, FIXTURE_PREFIX)).hasSize(3);
        assertThat(api.search(2, FIXTURE_PREFIX)).hasSize(2);
    }

    @Test
    void searchWithoutAValueReturnsTheCatalogUpToTheLimit() {
        assertThat(api.search(5, null)).hasSize(5);
    }

    @Test
    void aDeletedWordIsGone() {
        api.createWord(List.of(adjective("alt", "старый")));
        Long id = onlyResultFor("alt").getId();

        api.delete(id);

        assertThat(api.search(100, FIXTURE_PREFIX)).isEmpty();
        assertThat(wordService.findOneByDeAndWordType(FIXTURE_PREFIX + "alt", WordType.ADJECTIVE)).isEmpty();
    }

    private WordCrudApiV1GetWordRsInner onlyResultFor(String de) {
        List<WordCrudApiV1GetWordRsInner> found = api.search(100, FIXTURE_PREFIX + de);

        assertThat(found).hasSize(1);

        return found.get(0);
    }

    private static NounDto noun(String de, String ru, String plural, GenderEnum gender) {
        NounDto dto = new NounDto().plural(plural).gender(gender);

        dto.de(FIXTURE_PREFIX + de).ru(ru).wordType("NOUN").level(NounDto.LevelEnum.A1);

        return dto;
    }

    private static AdjectiveDto adjective(String de, String ru) {
        AdjectiveDto dto = new AdjectiveDto();

        dto.de(FIXTURE_PREFIX + de).ru(ru).wordType("ADJECTIVE").level(AdjectiveDto.LevelEnum.A1);

        return dto;
    }

    private static AdverbDto adverb(String de, String ru) {
        AdverbDto dto = new AdverbDto();

        dto.de(FIXTURE_PREFIX + de).ru(ru).wordType("ADVERB").level(AdverbDto.LevelEnum.A1);

        return dto;
    }

    private static PhraseDto phrase(String de, String ru) {
        PhraseDto dto = new PhraseDto();

        dto.de(FIXTURE_PREFIX + de).ru(ru).wordType("PHRASE").level(PhraseDto.LevelEnum.A1);

        return dto;
    }
}
