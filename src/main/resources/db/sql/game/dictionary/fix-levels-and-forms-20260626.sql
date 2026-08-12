-- ============================================================
-- Fix: word forms, translations, CEFR levels, verb conjugation
-- Generated: 2026-06-26
-- ============================================================

-- ============================================================
-- 1. Fix German word forms (de column)
-- ============================================================

-- 1703: "hohe" → "hoch" (dictionary/citation form)
UPDATE word SET de = 'hoch' WHERE id = 1703;

-- 1517: "Postbox" → "Postfach" (standard German)
UPDATE word SET de = 'Postfach' WHERE id = 1517;

-- 1445: "plastik" → "aus Plastik" (no German adjective "plastik")
UPDATE word SET de = 'aus Plastik' WHERE id = 1445;

-- ============================================================
-- 2. Fix Russian translations (ru column)
-- ============================================================

-- 1750: German text in Russian field
UPDATE word SET ru = 'Заранее благодарю Вас' WHERE id = 1750;

-- 1514: "Schalter" in post context means counter, not switch
UPDATE word SET ru = 'окошко, стойка' WHERE id = 1514;

-- ============================================================
-- 3. Fix CEFR levels: A1 → B1
-- ============================================================

UPDATE word SET level = 'B1' WHERE id IN (
    1694,   -- verwitwet (вдовец/вдова)
    2304,   -- Behandlung (лечение)
    2292,   -- Anwesenheit (присутствие)
    2294,   -- Asche (пепел)
    2296,   -- Champagner (шампанское)
    2306,   -- Genie (гений)
    2307,   -- Sage (легенда)
    2310,   -- Hirn (мозг)
    2311,   -- Rakete (ракета)
    2313,   -- Richter (судья)
    2331,   -- Führung (экскурсия/руководство)
    2332,   -- Schlacht (битва)
    2337,   -- Herausforderung (вызов)
    2303,   -- Schutz (защита)
    2342,   -- Wunde (рана)
    2328    -- Dummkopf (дурак)
);

-- ============================================================
-- 4. Fix CEFR levels: A1 → A2
-- ============================================================

UPDATE word SET level = 'A2' WHERE id IN (
    2150,   -- Krankheit (болезнь)
    2293,   -- Kofferraum (багажник)
    2305,   -- Schildkröte (черепаха)
    2325,   -- Pilot (пилот)
    2314,   -- Kohle (уголь/деньги)
    2316,   -- Schulter (плечо)
    2335,   -- Brieftasche (бумажник)
    2336,   -- Uniform (униформа)
    1357,   -- Dachboden (чердак)
    4130    -- rauchen (курить)
);

-- ============================================================
-- 5. Fix CEFR level: A2 → A1
-- ============================================================

-- aufstehen: core daily routine verb, belongs at A1
UPDATE word SET level = 'A1' WHERE id = 4144;

-- ============================================================
-- 6. Fix verb conjugation: regnen (id=4168)
--    All forms were incorrectly set to "regnet"
-- ============================================================

UPDATE verb SET
    ich  = 'regne',
    du   = 'regnest',
    er   = 'regnet',
    ihr  = 'regnet',
    wir  = 'regnen',
    sie  = 'regnen'
WHERE id = 4168;
