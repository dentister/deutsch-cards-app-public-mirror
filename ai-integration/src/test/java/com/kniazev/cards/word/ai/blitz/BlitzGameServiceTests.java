package com.kniazev.cards.word.ai.blitz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.kniazev.cards.word.model.dictionary.Word;
import com.kniazev.cards.word.model.dictionary.Word.WordLevel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class BlitzGameServiceTests {

    @Mock
    private ChatModel chatModel;

    private static List<Word> oneWord() {
        return List.of(Word.builder().de("laufen").ru("бежать").level(WordLevel.A1).build());
    }

    @Test
    void parsesValidSentencesIntoRecord() {
        String json = """
                {"sentences":["Der Hund läuft schnell.","Die Katze schläft.","Ich trinke Wasser."]}
                """;
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(json)))));

        BlitzGameService service = new BlitzGameService(ChatClient.builder(chatModel));

        Optional<BlitzSentences> result = service.generateSentences(oneWord());

        assertThat(result).contains(new BlitzSentences(
                List.of("Der Hund läuft schnell.", "Die Katze schläft.", "Ich trinke Wasser.")));
    }

    @Test
    void stripsMarkdownSignificantCharactersFromSentences() {
        String json = """
                {"sentences":["*Der* Hund läuft.","Die `Katze` schläft.","Ich ~trinke~ Wasser."]}
                """;
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(json)))));

        BlitzGameService service = new BlitzGameService(ChatClient.builder(chatModel));

        Optional<BlitzSentences> result = service.generateSentences(oneWord());

        assertThat(result).contains(new BlitzSentences(
                List.of("Der Hund läuft.", "Die Katze schläft.", "Ich trinke Wasser.")));
    }

    @Test
    void rejectsResponseWithWrongSentenceCount() {
        String json = """
                {"sentences":["Only one sentence."]}
                """;
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(json)))));

        BlitzGameService service = new BlitzGameService(ChatClient.builder(chatModel));

        assertThat(service.generateSentences(oneWord())).isEmpty();
    }

    @Test
    void swallowsAnyChatModelFailureAndReturnsEmptyForSentences() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("simulated outage"));

        BlitzGameService service = new BlitzGameService(ChatClient.builder(chatModel));

        assertThat(service.generateSentences(oneWord())).isEmpty();
    }

    @Test
    void parsesValidCheckResult() {
        String json = """
                {"allCorrect":true,"feedback":"Отлично, всё верно!"}
                """;
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(json)))));

        BlitzGameService service = new BlitzGameService(ChatClient.builder(chatModel));

        Optional<BlitzCheckResult> result = service.checkTranslations(
                List.of("Собака бежит.", "Кошка спит.", "Я пью воду."),
                List.of("Der Hund läuft.", "Die Katze schläft.", "Ich trinke Wasser."));

        assertThat(result).contains(new BlitzCheckResult(true, "Отлично, всё верно!"));
    }

    @Test
    void swallowsAnyChatModelFailureAndReturnsEmptyForCheck() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("simulated outage"));

        BlitzGameService service = new BlitzGameService(ChatClient.builder(chatModel));

        Optional<BlitzCheckResult> result = service.checkTranslations(
                List.of("Собака бежит."), List.of("Der Hund läuft."));

        assertThat(result).isEmpty();
    }
}
