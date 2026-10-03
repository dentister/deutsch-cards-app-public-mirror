-- ============================================================
-- Gesundheit (Health) vocabulary update
-- Generated: 2026-03-28
-- ============================================================
-- TASK 1: Add missing words with tag 'Gesundheit'
-- TASK 2: Update existing related words to add tag 'Gesundheit'
-- ============================================================

-- ============================================================
-- TASK 1: Add missing health words
-- New IDs start from 4194 (after the last used ID 4193)
-- ============================================================

INSERT INTO word (id, word_type, ru, de, tags, level) VALUES
    -- ---- NOUNS ----
    -- die Ernährung – nutrition/diet
    (4194, 'NOUN', 'питание, диета', 'Ernährung', '{Gesundheit}', 'B1'),
    -- die Lebensweise – lifestyle
    (4195, 'NOUN', 'образ жизни', 'Lebensweise', '{Gesundheit}', 'B1'),
    -- das Immunsystem – immune system
    (4196, 'NOUN', 'иммунная система', 'Immunsystem', '{Gesundheit}', 'B1'),
    -- das Vitamin – vitamin
    (4197, 'NOUN', 'витамин', 'Vitamin', '{Gesundheit}', 'A2'),
    -- der Nährstoff – nutrient
    (4198, 'NOUN', 'питательное вещество', 'Nährstoff', '{Gesundheit}', 'B1'),
    -- das Eiweiß / Protein – protein
    (4199, 'NOUN', 'белок', 'Eiweiß', '{Gesundheit}', 'B1'),
    (4200, 'NOUN', 'протеин', 'Protein', '{Gesundheit}', 'B1'),
    -- das Kohlenhydrat – carbohydrate
    (4201, 'NOUN', 'углевод', 'Kohlenhydrat', '{Gesundheit}', 'B1'),
    -- das Fett – fat
    (4202, 'NOUN', 'жир', 'Fett', '{Gesundheit}', 'B1'),
    -- der Ballaststoff – dietary fibre
    (4203, 'NOUN', 'пищевое волокно', 'Ballaststoff', '{Gesundheit}', 'B2'),
    -- das Übergewicht – excess weight / overweight
    (4204, 'NOUN', 'лишний вес, ожирение', 'Übergewicht', '{Gesundheit}', 'B1'),
    -- die Diät – diet (specific diet regimen)
    (4205, 'NOUN', 'диета', 'Diät', '{Gesundheit}', 'B1'),
    -- die Entspannung – relaxation
    (4206, 'NOUN', 'расслабление, отдых', 'Entspannung', '{Gesundheit}', 'B1'),
    -- das Fast Food
    (4207, 'NOUN', 'фаст-фуд', 'Fast Food', '{Gesundheit}', 'A2'),
    -- die Kalorie – calorie
    (4208, 'NOUN', 'калория', 'Kalorie', '{Gesundheit}', 'A2'),

    -- ---- ADVERBS / ADJECTIVES ----
    -- offensichtlich – obviously / evidently
    (4209, 'ADJECTIVE', 'очевидный, очевидно', 'offensichtlich', '{Gesundheit}', 'B1'),
    -- gesund – healthy
    (4210, 'ADJECTIVE', 'здоровый', 'gesund', '{Gesundheit}', 'A2'),
    -- ungesund – unhealthy
    (4211, 'ADJECTIVE', 'нездоровый', 'ungesund', '{Gesundheit}', 'A2'),
    -- ausgewogen – balanced
    (4212, 'ADJECTIVE', 'сбалансированный', 'ausgewogen', '{Gesundheit}', 'B1'),
    -- fettig – fatty / greasy
    (4213, 'ADJECTIVE', 'жирный', 'fettig', '{Gesundheit}', 'A2'),
    -- süß – sweet
    (4214, 'ADJECTIVE', 'сладкий', 'süß', '{Gesundheit}', 'A1'),
    -- salzig – salty
    (4215, 'ADJECTIVE', 'солёный', 'salzig', '{Gesundheit}', 'A2'),
    -- frisch – fresh
    (4216, 'ADJECTIVE', 'свежий', 'frisch', '{Gesundheit}', 'A1'),
    -- verarbeitet – processed
    (4217, 'ADJECTIVE', 'переработанный, обработанный', 'verarbeitet', '{Gesundheit}', 'B1'),
    -- natürlich – natural
    (4218, 'ADJECTIVE', 'натуральный, естественный', 'natürlich', '{Gesundheit}', 'A2'),
    -- regelmäßig – regular / regularly
    (4219, 'ADJECTIVE', 'регулярный, регулярно', 'regelmäßig', '{Gesundheit}', 'B1'),
    -- körperlich – physical / bodily
    (4220, 'ADJECTIVE', 'физический, телесный', 'körperlich', '{Gesundheit}', 'B1'),
    -- geistig – mental / intellectual
    (4221, 'ADJECTIVE', 'умственный, духовный', 'geistig', '{Gesundheit}', 'B1'),
    -- stressfrei – stress-free
    (4222, 'ADJECTIVE', 'без стресса, безстрессовый', 'stressfrei', '{Gesundheit}', 'B1'),
    -- fit – fit / in shape
    (4223, 'ADJECTIVE', 'в форме, физически здоровый', 'fit', '{Gesundheit}', 'A2'),

    -- ---- VERBS ----
    -- sich ernähren – to eat / to nourish oneself
    (4224, 'VERB', 'питаться, кормиться', 'sich ernähren', '{Gesundheit}', 'B1'),
    -- vermeiden – to avoid
    (4225, 'VERB', 'избегать', 'vermeiden', '{Gesundheit}', 'B1'),
    -- reduzieren – to reduce
    (4226, 'VERB', 'уменьшать, сокращать', 'reduzieren', '{Gesundheit}', 'B1'),
    -- zunehmen – to gain weight / to increase
    (4227, 'VERB', 'набирать вес, прибавлять', 'zunehmen', '{Gesundheit}', 'B1'),
    -- abnehmen – to lose weight / to decrease
    (4228, 'VERB', 'худеть, терять вес, убавлять', 'abnehmen', '{Gesundheit}', 'B1'),
    -- sich bewegen – to move / to exercise
    (4229, 'VERB', 'двигаться, заниматься спортом', 'sich bewegen', '{Gesundheit}', 'A2'),
    -- sich entspannen – to relax
    (4230, 'VERB', 'расслабляться, отдыхать', 'sich entspannen', '{Gesundheit}', 'B1'),
    -- verzichten auf – to do without / to abstain from
    (4231, 'VERB', 'отказываться от', 'verzichten auf', '{Gesundheit}', 'B1'),
    -- enthalten – to contain
    (4232, 'VERB', 'содержать', 'enthalten', '{Gesundheit}', 'B1'),
    -- fördern – to promote / to support
    (4233, 'VERB', 'поддерживать, способствовать', 'fördern', '{Gesundheit}', 'B1'),
    -- stärken – to strengthen
    (4234, 'VERB', 'укреплять, усиливать', 'stärken', '{Gesundheit}', 'B1'),
    -- verbessern – to improve
    (4235, 'VERB', 'улучшать', 'verbessern', '{Gesundheit}', 'B1'),
    -- schwächen – to weaken
    (4236, 'VERB', 'ослаблять', 'schwächen', '{Gesundheit}', 'B1')
ON CONFLICT (id) DO UPDATE SET
    word_type = EXCLUDED.word_type,
    ru        = EXCLUDED.ru,
    de        = EXCLUDED.de,
    tags      = EXCLUDED.tags,
    level     = EXCLUDED.level;

-- ============================================================
-- Noun table entries (gender, plural) for new nouns
-- ============================================================

INSERT INTO noun (id, gender, plural) VALUES
    (4194, 'F', 'Ernährungen'),
    (4195, 'F', 'Lebensweisen'),
    (4196, 'N', 'Immunsysteme'),
    (4197, 'N', 'Vitamine'),
    (4198, 'M', 'Nährstoffe'),
    (4199, 'N', 'Eiweiße'),
    (4200, 'N', 'Proteine'),
    (4201, 'N', 'Kohlenhydrate'),
    (4202, 'N', 'Fette'),
    (4203, 'M', 'Ballaststoffe'),
    (4204, 'N', NULL),
    (4205, 'F', 'Diäten'),
    (4206, 'F', 'Entspannungen'),
    (4207, 'N', NULL),
    (4208, 'F', 'Kalorien')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- Adjective table entries for new adjectives
-- ============================================================

INSERT INTO adjective (id) VALUES
    (4209),
    (4210),
    (4211),
    (4212),
    (4213),
    (4214),
    (4215),
    (4216),
    (4217),
    (4218),
    (4219),
    (4220),
    (4221),
    (4222),
    (4223)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- Verb table entries (conjugation) for new verbs
-- ============================================================

INSERT INTO verb (id, ich, du, er, ihr, wir, sie, partizip_2, root_verb, prefix, notes) VALUES
    -- sich ernähren (reflexiv)
    (4224, 'ernähre mich', 'ernährst dich', 'ernährt sich', 'ernährt euch', 'ernähren uns', 'ernähren sich', 'ernährt', 'ernähren', NULL, 'reflexiv + von'),
    -- vermeiden
    (4225, 'vermeide', 'vermeidest', 'vermeidet', 'vermeidet', 'vermeiden', 'vermeiden', 'vermieden', 'meiden', 'ver', NULL),
    -- reduzieren
    (4226, 'reduziere', 'reduzierst', 'reduziert', 'reduziert', 'reduzieren', 'reduzieren', 'reduziert', 'reduzieren', NULL, NULL),
    -- zunehmen
    (4227, 'nehme zu', 'nimmst zu', 'nimmt zu', 'nehmt zu', 'nehmen zu', 'nehmen zu', 'zugenommen', 'nehmen', 'zu', NULL),
    -- abnehmen
    (4228, 'nehme ab', 'nimmst ab', 'nimmt ab', 'nehmt ab', 'nehmen ab', 'nehmen ab', 'abgenommen', 'nehmen', 'ab', NULL),
    -- sich bewegen (reflexiv)
    (4229, 'bewege mich', 'bewegst dich', 'bewegt sich', 'bewegt euch', 'bewegen uns', 'bewegen sich', 'bewegt', 'bewegen', NULL, 'reflexiv'),
    -- sich entspannen (reflexiv)
    (4230, 'entspanne mich', 'entspannst dich', 'entspannt sich', 'entspannt euch', 'entspannen uns', 'entspannen sich', 'entspannt', 'entspannen', NULL, 'reflexiv'),
    -- verzichten auf
    (4231, 'verzichte auf', 'verzichtest auf', 'verzichtet auf', 'verzichtet auf', 'verzichten auf', 'verzichten auf', 'verzichtet', 'verzichten', NULL, NULL),
    -- enthalten
    (4232, 'enthalte', 'enthältst', 'enthält', 'enthaltet', 'enthalten', 'enthalten', 'enthalten', 'halten', 'ent', NULL),
    -- fördern
    (4233, 'fördere', 'förderst', 'fördert', 'fördert', 'fördern', 'fördern', 'gefördert', 'fördern', NULL, NULL),
    -- stärken
    (4234, 'stärke', 'stärkst', 'stärkt', 'stärkt', 'stärken', 'stärken', 'gestärkt', 'stärken', NULL, NULL),
    -- verbessern
    (4235, 'verbessere', 'verbesserst', 'verbessert', 'verbessert', 'verbessern', 'verbessern', 'verbessert', 'bessern', 'ver', NULL),
    -- schwächen
    (4236, 'schwäche', 'schwächst', 'schwächt', 'schwächt', 'schwächen', 'schwächen', 'geschwächt', 'schwächen', NULL, NULL)
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
-- TASK 2: Update existing words – add tag 'Gesundheit'
-- ============================================================
-- These words already exist in the dictionary but have no
-- health tag. We update only the tags column, preserving all
-- other data.
-- NOTE: array_append is used to safely add the tag without
--       overwriting other existing tags.
-- ============================================================

-- die Gesundheit (id=2365) – currently '{}', set to '{Gesundheit}'
UPDATE word SET tags = '{Gesundheit}' WHERE id = 2365;

-- die Bewegung (id=1952) – nutrition/exercise context
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 1952;

-- die Mahlzeit (id=2589) – meal
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2589;

-- die Gewohnheit (id=2798) – habit
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2798;

-- der Zucker (id=2416) – sugar
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2416;

-- aktiv (id=1305) – active; already has '{person}', add 'Gesundheit'
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 1305;

-- essen (id=4118) – to eat (from missing-verbs file)
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 4118;

-- trinken (id=4119) – to drink (from missing-verbs file)
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 4119;

-- die Krankheit (id=2150) – illness, Gesundheit topic
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2150;

-- die Medizin (id=2024) – medicine
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2024;

-- der Arzt (id=1924) – doctor
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 1924;

-- die Klinik (id=2173) – clinic
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2173;

-- das Krankenhaus (id=1889) – hospital
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 1889;

-- der Hunger (id=2099) – hunger
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2099;

-- der Durst (id=2485) – thirst
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2485;

-- der Schmerz (id=2175) – pain
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2175;

-- die Erkältung (id=2882) – cold (illness)
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2882;

-- das Fieber (id=2591) – fever
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2591;

-- der Sport (id=2434) – sport/exercise
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2434;

-- der Körper (id=1960) – body
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 1960;

-- die Energie (id=2017) – energy
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2017;

-- das Wohlbefinden/Wohl (id=1988) – well-being
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 1988;

-- der Salat (id=2419) – salad
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2419;

-- das Gemüse (id=2624) – vegetables
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2624;

-- der Puls (id=2443) – pulse
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2443;

-- der Blutdruck (id=2555) – blood pressure
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2555;

-- das Symptom (id=2787) – symptom
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2787;

-- die Behandlung (id=2304) – treatment
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2304;

-- der Alkohol (id=2494) – alcohol
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2494;

-- rauchen (id=4130) – to smoke
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 4130;

-- der Zahnarzt (id=2704) – dentist
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2704;

-- Kopfschmerzen (id=2429) – headache
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2429;

-- die Ruhe (id=1943) – rest / calm
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 1943;

-- das Fleisch (id=1994) – meat
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 1994;

-- das Wasser (id=1895) – water
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 1895;

-- kochen (id=1266) – to cook
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 1266;

-- das Essen (id=1245) – food / eating
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 1245;

-- die Depression (id=3319) – depression
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 3319;

-- die Sucht (id=2272) – addiction
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2272;

-- sich erholen (id=4172) – to recover / to rest
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 4172;

-- der Vitaminsaft / der Saft (id=2650) – juice
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 2650;

-- die Pищевая привычка / Essgewohnheit (id=3236) – eating habit
UPDATE word SET tags = array_append(COALESCE(tags, '{}'), 'Gesundheit') WHERE id = 3236;
