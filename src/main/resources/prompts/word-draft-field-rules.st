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
