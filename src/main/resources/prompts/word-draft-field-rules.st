- PART OF SPEECH HINT: the admin may state the part of speech in the raw input, in any language
  or abbreviation (e.g. "adjective: häufig", "Прилагательное völlig", "adj. schnell", "глагол ...",
  "noun ...", "наречие ..."). If such a hint is present, set "wordType" to exactly that part of
  speech, even if the word is also commonly used as another one (many German words are both
  adjective and adverb). Never override the hint with your own classification, and never put the
  hint itself into "de". Only if there is no hint, classify the word yourself.
- Everything else must match the chosen "wordType": "ru", "en", "sample", "sampleRu" and "sampleEn"
  all have to represent the word in that part of speech.
  - ADJECTIVE: "ru" is an adjective in masculine nominative singular (e.g. "частый", "полный" -
    NOT an adverb like "часто", "полностью"); "en" is an adjective (e.g. "frequent", "complete" -
    NOT an "-ly" adverb). "sample" must use the word as an adjective: attributively before a noun,
    preferably declined ("Das ist ein häufiger Fehler."), or predicatively after "sein"/"werden".
    Never use it adverbially to modify a verb, adjective or the whole sentence. "sampleRu" and
    "sampleEn" must translate it with an adjective as well.
  - ADVERB: "ru"/"en" are adverbs and "sample" uses the word adverbially.
  - Before answering, re-read "sample" and check that the word really is used as the chosen
    "wordType"; if not, rewrite the sentence.
- Correct/normalize the German spelling (fix typos, capitalization, umlauts).
- If the input already includes a Russian translation proposed by the admin, verify it is
  actually correct for the German word. If it is wrong, replace "ru" with the correct
  translation and explain the correction in one short sentence in "translationCorrectionNote".
  If no translation was proposed, or the proposed one was already correct, leave
  "translationCorrectionNote" null.
- Fill "en" with the English translation of the German word itself.
- If wordType is NOUN: fill "gender" and "plural" (the plural form, or null if the word has
  no plural).
- If wordType is VERB: fill "ich", "du", "er", "wir", "ihr", "sie" with the present-tense
  conjugations and "partizip2" with the Partizip II form; fill "prefix"/"rootVerb" only if the
  verb is separable, otherwise leave them null.
  "notes" captures the verb's government (Rektion) in compact dictionary shorthand: whether
  it's reflexive ("sich") and which preposition + case it takes with a noun. Examples:
    "interessieren" (reflexive, + für/Akkusativ) -> "interessieren sich für Akk."
    "einladen" (separable, + zu/Dativ + Akkusativ object) -> "lade Akk. zur Dat. ein"
  Leave "notes" null if the verb has no such reflexive/prepositional government worth noting
  (e.g. plain transitive/intransitive verbs like "sehen", "gehen"). One short line, well
  under 50 characters - never a full sentence or explanation.
- For any field that does not apply to the classified wordType, leave it null.
- Write one short, natural example sentence using the word in "sample", its Russian
  translation in "sampleRu", and its English translation in "sampleEn".
- Every field is a short word or phrase, never a full sentence, except "sample"/"sampleRu"/
  "sampleEn"/"translationCorrectionNote".
