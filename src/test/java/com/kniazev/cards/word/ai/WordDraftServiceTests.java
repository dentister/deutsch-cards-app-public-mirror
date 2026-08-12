package com.kniazev.cards.word.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kniazev.cards.word.db.model.word.Noun.GenderType;
import com.kniazev.cards.word.db.model.word.Word.WordLevel;
import com.kniazev.cards.word.db.model.word.Word.WordType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class WordDraftServiceTests {

    @Mock
    private ChatModel chatModel;

    @Test
    void parsesFreshDraftFromRawInput() {
        String json = """
                {"wordType":"VERB","de":"überkommen","ru":"преодолеть","level":"C1",
                 "gender":null,"plural":null,
                 "ich":"überkomme","du":"überkommst","er":"überkommt","wir":"überkommen","ihr":"überkommt","sie":"überkommen",
                 "partizip2":"überkommen","prefix":null,"rootVerb":"kommen","notes":null,
                 "sample":"Die Angst überkommt ihn oft.","sampleRu":"Страх часто одолевает его.",
                 "translationCorrectionNote":null}
                """;
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(json)))));

        WordDraftService service = new WordDraftService(ChatClient.builder(chatModel));

        Optional<WordDraft> result = service.generateDraft("überkommen преодолеть");

        assertThat(result).isPresent();
        WordDraft draft = result.get();
        assertThat(draft.wordType()).isEqualTo(WordType.VERB);
        assertThat(draft.de()).isEqualTo("überkommen");
        assertThat(draft.level()).isEqualTo(WordLevel.C1);
        assertThat(draft.ich()).isEqualTo("überkomme");
        assertThat(draft.sample()).isEqualTo("Die Angst überkommt ihn oft.");
    }

    @Test
    void revisionPromptEmbedsPreviousDraftAndCorrectionNote() {
        String json = """
                {"wordType":"NOUN","de":"der Tisch","ru":"стол","level":"A1",
                 "gender":"M","plural":"Tische",
                 "ich":null,"du":null,"er":null,"wir":null,"ihr":null,"sie":null,
                 "partizip2":null,"prefix":null,"rootVerb":null,"notes":null,
                 "sample":"Der Tisch ist aus Holz.","sampleRu":"Стол сделан из дерева.",
                 "translationCorrectionNote":null}
                """;
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(json)))));

        WordDraftService service = new WordDraftService(ChatClient.builder(chatModel));
        WordDraft previous = new WordDraft(WordType.NOUN, "Tisch", "стол", WordLevel.A1,
                GenderType.M, "Tische", null, null, null, null, null, null, null, null, null, null,
                "Der Tisch ist alt.", "Стол старый.", null);

        Optional<WordDraft> result = service.reviseDraft(previous, "gender should stay M, but fix the sample sentence");

        assertThat(result).isPresent();
        assertThat(result.get().de()).isEqualTo("der Tisch");

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        String sentPrompt = captor.getValue().getContents();
        assertThat(sentPrompt).contains("gender should stay M, but fix the sample sentence");
        assertThat(sentPrompt).contains("Tisch");
    }

    @Test
    void swallowsAnyChatModelFailureAndReturnsEmptyForFreshDraft() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("simulated outage"));

        WordDraftService service = new WordDraftService(ChatClient.builder(chatModel));

        assertThat(service.generateDraft("überkommen")).isEmpty();
    }

    @Test
    void swallowsAnyChatModelFailureAndReturnsEmptyForRevision() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("simulated outage"));

        WordDraftService service = new WordDraftService(ChatClient.builder(chatModel));
        WordDraft previous = new WordDraft(WordType.VERB, "gehen", "идти", WordLevel.A1,
                null, null, "gehe", "gehst", "geht", "gehen", "geht", "gehen", "gegangen", null, null, null,
                "Ich gehe nach Hause.", "Я иду домой.", null);

        assertThat(service.reviseDraft(previous, "fix something")).isEmpty();
    }
}
