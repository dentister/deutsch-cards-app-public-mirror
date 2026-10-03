package com.kniazev.cards.word.telegram.bot;

import com.kniazev.cards.word.game.representation.TaskEnum;
import com.kniazev.cards.word.game.representation.WordCard;
import com.kniazev.cards.word.model.dictionary.Noun;
import com.kniazev.cards.word.model.dictionary.Verb;
import com.kniazev.cards.word.model.dictionary.Word;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import static com.kniazev.cards.word.telegram.bot.Emoji.*;

/**
 * Shows the learning game in Telegram: the prompt for a task, the feedback to an answer, the hint with the forms of a
 * word and the announcement of new words. The game ({@link WordCard}) knows nothing about how it is displayed.
 * <p>
 * The markup is the part of MarkdownV2 that the game texts use ({@code *bold*}, a code block in three backticks);
 * {@link TelegramMessenger#sendMarkdown} escapes the other special characters.
 */
@Component
public class GameCardPresenter {

    /** The prompt for the task the card asks. */
    public String task(WordCard card, Locale locale) {
        Word word = card.getWord();

        return Messages.get(taskKey(card.getTaskId()), locale,
                Map.of("ru", word.getRu(), "level", word.getLevel().name()));
    }

    /** The verdict on the answer, the forms of the word and, if the word has one, the usage example. */
    public String feedback(WordCard card, Locale locale) {
        Word word = card.getWord();

        return verdict(card, locale) + "\n\n" + hint(word) + usageExample(word);
    }

    /** A code block with the level of the word and the forms to learn. */
    public String hint(Word word) {
        String forms = switch (word) {
            case Noun noun -> nounForms(noun);
            case Verb verb -> verbForms(verb);
            default -> word.getDe();
        };

        return "```" + word.getLevel().name() + "\n" + forms + "```";
    }

    /** The announcement of the words that a restarted game is going to ask. */
    public String newWordsPreview(Collection<WordCard> cards, Locale locale) {
        StringBuilder sb = new StringBuilder(Messages.get("bot.new_game_announce", locale)).append("\n\n");

        for (WordCard card : cards) {
            Word word = card.getWord();

            sb.append(GER_FLAG).append('*').append(word.getDe()).append("* | ");
            sb.append(RUS_FLAG).append('*').append(word.getRu()).append('*');

            if (StringUtils.isNotBlank(word.getEn())) {
                sb.append(" | ").append(ENG_FLAG).append('*').append(word.getEn()).append('*');
            }

            sb.append(" \n").append(hint(word)).append("\n\n");
        }

        return sb.toString();
    }

    private String verdict(WordCard card, Locale locale) {
        if (card.isRightAnswered()) {
            return Messages.get("game.right_answer", locale);
        }

        String rightAnswerToken = Messages.get("game.right_answer_token", locale,
                Map.of("rightAnswer", card.getRightAnswer().get(), "emojiSuffix", genderMark(card.getWord())));

        return Messages.get("game.wrong_answer", locale, Map.of("rightAnswerToken", rightAnswerToken));
    }

    /** The colored circle after the right answer: it tells the gender of a noun. */
    private static String genderMark(Word word) {
        if (word instanceof Noun noun && noun.getGender() != null) {
            return switch (noun.getGender()) {
                case F, PL -> " " + RED_CIRCLE_ICON;
                case M -> " " + BLUE_CIRCLE_ICON;
                case N -> " " + GREEN_CIRCLE_ICON;
            };
        }

        return "";
    }

    private static String nounForms(Noun noun) {
        String plural = noun.getFullPlural();

        return plural == null ? noun.getFullDe() : noun.getFullDe() + ", " + plural;
    }

    private static String verbForms(Verb verb) {
        return "ich | " + verb.getIch() + "\n"
                + "du  | " + verb.getDu() + "\n"
                + "er  | " + verb.getEr() + "\n"
                + "wir | " + verb.getWir() + "\n"
                + "ihr | " + verb.getIhr() + "\n"
                + "Sie | " + verb.getSie() + "\n"
                + "Partizip II: " + verb.getPartizip2();
    }

    /** The example sentence in the languages it is translated to; a missing translation is left out. */
    private static String usageExample(Word word) {
        if (StringUtils.isBlank(word.getSample())) {
            return "";
        }

        StringBuilder sb = new StringBuilder("\n");

        appendSentence(sb, GER_FLAG, word.getSample());
        appendSentence(sb, RUS_FLAG, word.getSampleRu());
        appendSentence(sb, ENG_FLAG, word.getSampleEn());

        return sb.toString();
    }

    private static void appendSentence(StringBuilder sb, String flag, String sentence) {
        if (StringUtils.isNotBlank(sentence)) {
            sb.append('\n').append(flag).append(' ').append(sentence);
        }
    }

    private static String taskKey(TaskEnum task) {
        return switch (task) {
            case SINGULAR_NOUN -> "game.task.singular_noun";
            case PLURAL_NOUN -> "game.task.plural_noun";
            case VERB -> "game.task.verb";
            case ICH_VERB -> "game.task.ich_verb";
            case DU_VERB -> "game.task.du_verb";
            case ER_VERB -> "game.task.er_verb";
            case WIR_VERB -> "game.task.wir_verb";
            case IHR_VERB -> "game.task.ihr_verb";
            case SIE_VERB -> "game.task.sie_verb";
            case PARTIZIP2 -> "game.task.partizip2";
            case ADJECTIVE -> "game.task.adjective";
            case ADVERB -> "game.task.adverb";
            case PHRASE -> "game.task.phrase";
        };
    }
}
