-- ============================================================
-- IT vocabulary update
-- Generated: 2026-04-28
-- ============================================================
-- TASK 1: Add missing words with tag 'IT'
-- TASK 2: Update existing related words to add tag 'IT'
-- ============================================================

-- ============================================================
-- TASK 1: Add missing IT words
-- New IDs start from 4237 (after the last used ID 4236)
-- ============================================================

INSERT INTO word (id, word_type, ru, de, tags, level) VALUES
    -- ---- NOUNS ----
    (4237, 'NOUN', 'требование', 'Anforderung', '{IT}', 'B2'),
    (4238, 'NOUN', 'реализация', 'Umsetzung', '{IT}', 'B2'),
    (4239, 'NOUN', 'сообщение об ошибке', 'Fehlermeldung', '{IT}', 'B1'),
    (4240, 'NOUN', 'интерфейс', 'Schnittstelle', '{IT}', 'B2'),
    (4241, 'NOUN', 'обмен', 'Austausch', '{IT}', 'B1'),
    (4242, 'NOUN', 'документация', 'Dokumentation', '{IT}', 'B1'),
    (4243, 'NOUN', 'уязвимость безопасности', 'Sicherheitslücke', '{IT}', 'B2'),
    (4244, 'NOUN', 'база данных', 'Datenbank', '{IT}', 'B1'),

    -- ---- ADJECTIVES ----
    (4245, 'ADJECTIVE', 'масштабируемый', 'skalierbar', '{IT}', 'C1'),
    (4246, 'ADJECTIVE', 'удобный для пользователя', 'benutzerfreundlich', '{IT}', 'B1'),

    -- ---- VERBS ----
    (4247, 'VERB', 'публиковать', 'veröffentlichen', '{IT}', 'B2'),
    (4248, 'VERB', 'согласовывать', 'abstimmen', '{IT}', 'B1'),
    (4249, 'VERB', 'вносить данные', 'einpflegen', '{IT}', 'B2'),
    (4250, 'VERB', 'устранять', 'beheben', '{IT}', 'B2'),
    (4251, 'VERB', 'обеспечивать', 'gewährleisten', '{IT}', 'C1'),
    (4252, 'VERB', 'ждать, обслуживать (поддерживать)', 'warten', '{IT}', 'A1'),
    (4253, 'VERB', 'зависать', 'hängen bleiben', '{IT}', 'B1')
ON CONFLICT (id) DO UPDATE SET
    word_type = EXCLUDED.word_type,
    ru        = EXCLUDED.ru,
    de        = EXCLUDED.de,
    tags      = CASE WHEN word.tags @> '{IT}' THEN word.tags ELSE array_append(COALESCE(word.tags, '{}'), 'IT') END,
    level     = EXCLUDED.level;

-- ============================================================
-- Noun table entries (gender, plural) for new nouns
-- ============================================================

INSERT INTO noun (id, gender, plural) VALUES
    (4237, 'F', 'Anforderungen'),
    (4238, 'F', 'Umsetzungen'),
    (4239, 'F', 'Fehlermeldungen'),
    (4240, 'F', 'Schnittstellen'),
    (4241, 'M', 'Austausche'),
    (4242, 'F', 'Dokumentationen'),
    (4243, 'F', 'Sicherheitslücken'),
    (4244, 'F', 'Datenbanken')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- Adjective table entries for new adjectives
-- ============================================================

INSERT INTO adjective (id) VALUES
    (4245),
    (4246)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- Verb table entries (conjugation) for new verbs
-- ============================================================

INSERT INTO verb (id, ich, du, er, ihr, wir, sie, partizip_2, root_verb, prefix, notes) VALUES
    -- veröffentlichen
    (4247, 'veröffentliche', 'veröffentlichst', 'veröffentlicht', 'veröffentlicht', 'veröffentlichen', 'veröffentlichen', 'veröffentlicht', 'veröffentlichen', NULL, NULL),
    -- abstimmen
    (4248, 'stimme ab', 'stimmst ab', 'stimmt ab', 'stimmt ab', 'stimmen ab', 'stimmen ab', 'abgestimmt', 'stimmen', 'ab', NULL),
    -- einpflegen
    (4249, 'pflege ein', 'pflegst ein', 'pflegt ein', 'pflegt ein', 'pflegen ein', 'pflegen ein', 'eingepflegt', 'pflegen', 'ein', NULL),
    -- beheben
    (4250, 'behebe', 'behebst', 'behebt', 'behebt', 'beheben', 'beheben', 'behoben', 'heben', 'be', NULL),
    -- gewährleisten
    (4251, 'gewährleiste', 'gewährleistest', 'gewährleistet', 'gewährleistet', 'gewährleisten', 'gewährleisten', 'gewährleistet', 'gewährleisten', NULL, NULL),
    -- warten
    (4252, 'warte', 'wartest', 'wartet', 'wartet', 'warten', 'warten', 'gewartet', 'warten', NULL, NULL),
    -- hängen bleiben
    (4253, 'bleibe hängen', 'bleibst hängen', 'bleibt hängen', 'bleibt hängen', 'bleiben hängen', 'bleiben hängen', 'hängen geblieben', 'bleiben', 'hängen', NULL)
ON CONFLICT (id) DO UPDATE SET
    ich        = EXCLUDED.ich,
    du         = EXCLUDED.du,
    er         = EXCLUDED.er,
    ihr        = EXCLUDED.ihr,
    wir        = EXCLUDED.wir,
    sie        = EXCLUDED.sie,
    partizip_2 = EXCLUDED.partizip_2,
    root_verb  = EXCLUDED.root_verb,
    prefix     = EXCLUDED.prefix,
    notes      = EXCLUDED.notes;


-- ============================================================
-- TASK 2: Update existing words – add tag 'IT'
-- ============================================================

-- Existing IT related words:
-- 1410: der Computer
-- 1540: die App
-- 1541: das Internet
-- 1567: klicken
-- 1568: herunterladen
-- 1569: hochladen
-- 2182: das System
-- 2317: das Programm
-- 2408: die Umgebung
-- 2469: die Maus
-- 2557: die Technologie
-- 2707: das Passwort
-- 2917: die Datei
-- 3446: die Computerfirma
-- 3989: die Tastatur
-- 4024: die Abnahme
UPDATE word 
SET tags = array_append(COALESCE(tags, '{}'), 'IT') 
WHERE id IN (1410, 1540, 1541, 1567, 1568, 1569, 2182, 2317, 2408, 2469, 2557, 2707, 2917, 3446, 3989, 4024) 
  AND NOT tags @> '{IT}';
