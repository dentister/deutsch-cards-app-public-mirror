package com.kniazev.cards.word.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kniazev.cards.word.db.model.word.Word;
import com.kniazev.cards.word.db.model.word.Word.WordLevel;
import com.kniazev.cards.word.db.services.WordService;

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
class WordExampleServiceTests {

    @Mock
    private ChatModel chatModel;

    @Mock
    private WordService wordService;

    @Test
    void parsesValidJsonIntoRecord() {
        String json = """
                {"sentence":"Der Hund läuft schnell.","translationRu":"Собака бежит быстро."}
                """;
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(json)))));

        WordExampleService service = new WordExampleService(ChatClient.builder(chatModel), wordService);
        Word word = Word.builder().de("laufen").ru("бежать").level(WordLevel.A1).build();

        Optional<WordUsageExample> result = service.generateExample(word);

        assertThat(result).contains(new WordUsageExample("Der Hund läuft schnell.", "Собака бежит быстро."));
    }

    @Test
    void stripsMarkdownSignificantCharactersFromModelOutput() {
        String json = """
                {"sentence":"Der *Hund* läuft `schnell`.","translationRu":"Собака ~бежит~ быстро."}
                """;
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(json)))));

        WordExampleService service = new WordExampleService(ChatClient.builder(chatModel), wordService);
        Word word = Word.builder().de("laufen").ru("бежать").level(WordLevel.A1).build();

        Optional<WordUsageExample> result = service.generateExample(word);

        assertThat(result).contains(new WordUsageExample("Der Hund läuft schnell.", "Собака бежит быстро."));
    }

    @Test
    void swallowsAnyChatModelFailureAndReturnsEmpty() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("simulated outage"));

        WordExampleService service = new WordExampleService(ChatClient.builder(chatModel), wordService);
        Word word = Word.builder().de("laufen").ru("бежать").level(WordLevel.A1).build();

        assertThat(service.generateExample(word)).isEmpty();
    }

    @Test
    void doesNothingWhenSampleAlreadyCached() {
        WordExampleService service = new WordExampleService(ChatClient.builder(chatModel), wordService);
        Word word = Word.builder().de("laufen").ru("бежать").level(WordLevel.A1)
                .sample("Der Hund läuft schnell.").sampleRu("Собака бежит быстро.").build();

        service.cacheExampleIfMissing(word);

        verify(chatModel, never()).call(any(Prompt.class));
        verify(wordService, never()).save(any(Word.class));
    }

    @Test
    void generatesAndPersistsWhenSampleMissing() {
        String json = """
                {"sentence":"Der Hund läuft schnell.","translationRu":"Собака бежит быстро."}
                """;
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(json)))));

        WordExampleService service = new WordExampleService(ChatClient.builder(chatModel), wordService);
        Word word = Word.builder().de("laufen").ru("бежать").level(WordLevel.A1).build();

        service.cacheExampleIfMissing(word);

        assertThat(word.getSample()).isEqualTo("Der Hund läuft schnell.");
        assertThat(word.getSampleRu()).isEqualTo("Собака бежит быстро.");
        verify(wordService).save(word);
    }

    @Test
    void doesNotPersistWhenGenerationFails() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("simulated outage"));

        WordExampleService service = new WordExampleService(ChatClient.builder(chatModel), wordService);
        Word word = Word.builder().de("laufen").ru("бежать").level(WordLevel.A1).build();

        service.cacheExampleIfMissing(word);

        assertThat(word.getSample()).isNull();
        verify(wordService, never()).save(any(Word.class));
    }
}
