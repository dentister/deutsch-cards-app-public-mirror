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
                {"wordType":"VERB","de":"überkommen","ru":"преодолеть","en":"to overcome","level":"C1",
                 "gender":null,"plural":null,
                 "ich":"überkomme","du":"überkommst","er":"überkommt","wir":"überkommen","ihr":"überkommt","sie":"überkommen",
                 "partizip2":"überkommen","prefix":null,"rootVerb":"kommen","notes":null,
                 "sample":"Die Angst überkommt ihn oft.","sampleRu":"Страх часто одолевает его.",
                 "sampleEn":"Fear overcomes him often.",
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
        assertThat(draft.en()).isEqualTo("to overcome");
        assertThat(draft.level()).isEqualTo(WordLevel.C1);
        assertThat(draft.ich()).isEqualTo("überkomme");
        assertThat(draft.sample()).isEqualTo("Die Angst überkommt ihn oft.");
        assertThat(draft.sampleEn()).isEqualTo("Fear overcomes him often.");
    }

    @Test
    void revisionPromptEmbedsPreviousDraftAndCorrectionNote() {
        String json = """
                {"wordType":"NOUN","de":"der Tisch","ru":"стол","en":"table","level":"A1",
                 "gender":"M","plural":"Tische",
                 "ich":null,"du":null,"er":null,"wir":null,"ihr":null,"sie":null,
                 "partizip2":null,"prefix":null,"rootVerb":null,"notes":null,
                 "sample":"Der Tisch ist aus Holz.","sampleRu":"Стол сделан из дерева.",
                 "sampleEn":"The table is made of wood.",
                 "translationCorrectionNote":null}
                """;
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(json)))));

        WordDraftService service = new WordDraftService(ChatClient.builder(chatModel));
        WordDraft previous = new WordDraft(WordType.NOUN, "Tisch", "стол", "table", WordLevel.A1,
                GenderType.M, "Tische", null, null, null, null, null, null, null, null, null, null,
                "Der Tisch ist alt.", "Стол старый.", "The table is old.", null);

        Optional<WordDraft> result = service.reviseDraft(previous, "gender should stay M, but fix the sample sentence");

        assertThat(result).isPresent();
        assertThat(result.get().de()).isEqualTo("der Tisch");
        assertThat(result.get().en()).isEqualTo("table");
        assertThat(result.get().sampleEn()).isEqualTo("The table is made of wood.");

        ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        String sentPrompt = captor.getValue().getContents();
        assertThat(sentPrompt).contains("gender should stay M, but fix the sample sentence");
        assertThat(sentPrompt).contains("Tisch");
        assertThat(sentPrompt).contains("Correct/normalize the German spelling");
        assertThat(sentPrompt).contains("Fill \"en\" with the English translation");
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
        WordDraft previous = new WordDraft(WordType.VERB, "gehen", "идти", "to go", WordLevel.A1,
                null, null, "gehe", "gehst", "geht", "gehen", "geht", "gehen", "gegangen", null, null, null,
                "Ich gehe nach Hause.", "Я иду домой.", "I am going home.", null);

        assertThat(service.reviseDraft(previous, "fix something")).isEmpty();
    }
}
