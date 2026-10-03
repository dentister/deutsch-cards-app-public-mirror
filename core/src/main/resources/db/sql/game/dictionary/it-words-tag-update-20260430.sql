-- ============================================================
-- IT vocabulary: add tag 'IT' to existing words
-- Generated: 2026-04-30
-- ============================================================
-- Scanned all dictionary files and identified words related to
-- the IT domain that are missing the 'IT' tag.
-- Source file: dump-words-db-202602211426.sql
-- ============================================================

-- ============================================================
-- Words found in the dump that are clearly IT-related
-- but did NOT have the 'IT' tag yet.
-- ============================================================

-- 1411  | NOUN | ноутбук         | Laptop     | {home}
-- 1538  | NOUN | электронная почта| E-Mail     | {post&communication}
-- 1539  | NOUN | чат             | Chat       | {post&communication}
-- 1546  | NOUN | веб-камера      | Webcam     | {post&communication}
-- 1547  | NOUN | скриншот        | Screenshot | {post&communication}
-- 1571  | VERB | общаться в чате | chatten    | {post&communication}
-- 1574  | VERB | сканировать     | scannen    | {post&communication}
-- 2095  | NOUN | вирус           | Virus      | NULL
-- 2907  | NOUN | пароль          | Kennwort   | NULL
-- 3443  | NOUN | наушники        | Kopfhörer  | NULL
-- 3726  | NOUN | каталог         | Verzeichnis| NULL
-- 4067  | NOUN | пин-код         | Geheimzahl | NULL

UPDATE word
SET tags = array_append(COALESCE(tags, '{}'), 'IT')
WHERE id IN (
    1411,   -- Laptop         (ноутбук)
    1538,   -- E-Mail         (электронная почта)
    1539,   -- Chat           (чат)
    1546,   -- Webcam         (веб-камера)
    1547,   -- Screenshot     (скриншот)
    1571,   -- chatten        (общаться в чате)
    1574,   -- scannen        (сканировать)
    2095,   -- Virus          (вирус)
    2907,   -- Kennwort       (пароль / секретное слово)
    3443,   -- Kopfhörer      (наушники)
    3726,   -- Verzeichnis    (каталог / директория)
    4067    -- Geheimzahl     (пин-код)
)
  AND NOT tags @> '{IT}';

-- ============================================================
-- Fix noun table: Internet has no plural form
-- ============================================================

-- 1541 | Internet | gender=N, plural=NULL (das Internet — keine Pluralform)
UPDATE noun
SET plural = NULL
WHERE id = 1541;
