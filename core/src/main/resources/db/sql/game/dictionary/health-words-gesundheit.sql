INSERT INTO word (id, word_type, ru, de, tags, level) VALUES
    (1889, 'NOUN', 'больница',               'Krankenhaus',  '{Gesundheit}', 'A1'),
    (1924, 'NOUN', 'врач',                   'Arzt',         '{Gesundheit}', 'A1'),
    (2150, 'NOUN', 'болезнь',                'Krankheit',    '{Gesundheit}', 'A1'),
    (2213, 'NOUN', 'обследование',           'Untersuchung', '{Gesundheit}', 'B1'),
    (2287, 'NOUN', 'рецепт',                 'Rezept',       '{Gesundheit}', 'A1'),
    (2304, 'NOUN', 'лечение',                'Behandlung',   '{Gesundheit}', 'A1'),
    (2365, 'NOUN', 'здоровье',               'Gesundheit',   '{Gesundheit}', 'A1'),
    (2555, 'NOUN', 'кровяное давление',      'Blutdruck',    '{Gesundheit}', 'B1'),
    (2984, 'NOUN', 'аптека',                 'Apotheke',     '{Gesundheit}', 'A1'),
    (3309, 'NOUN', 'семейный врач',          'Hausarzt',     '{Gesundheit}', 'A2'),
    (3748, 'NOUN', 'воспаление',             'Entzündung',   '{Gesundheit}', 'B1'),
    (3931, 'NOUN', 'направление к врачу, перевод денег',  'Überweisung',  '{Gesundheit}', 'B1')
ON CONFLICT (id) DO UPDATE SET
    tags = EXCLUDED.tags,
    ru   = EXCLUDED.ru,
    level = EXCLUDED.level;

INSERT INTO word (id, word_type, ru, de, tags, level) VALUES
    (4194, 'NOUN', 'больничный лист',        'Krankschreibung',     '{Gesundheit}', 'B1'),
    (4195, 'NOUN', 'регистратор врача',      'Sprechstundenhilfe',  '{Gesundheit}', 'B1')
ON CONFLICT (id) DO UPDATE SET
    word_type = EXCLUDED.word_type,
    ru        = EXCLUDED.ru,
    de        = EXCLUDED.de,
    tags      = EXCLUDED.tags,
    level     = EXCLUDED.level;

INSERT INTO noun (id, gender, plural) VALUES
    (4194, 'F', 'Krankschreibungen'),
    (4195, 'F', 'Sprechstundenhilfen')
ON CONFLICT (id) DO UPDATE SET
    gender = EXCLUDED.gender,
    plural = EXCLUDED.plural;

INSERT INTO word (id, word_type, ru, de, tags, level) VALUES
    (4196, 'VERB', 'простудиться',                  'sich erkälten',  '{Gesundheit}', 'A2'),
    (4197, 'VERB', 'кашлять',                        'husten',         '{Gesundheit}', 'A2'),
    (4198, 'VERB', 'чихать',                         'niesen',         '{Gesundheit}', 'A2'),
    (4199, 'VERB', 'выписывать (лекарство)',          'verschreiben',   '{Gesundheit}', 'B1'),
    (4200, 'VERB', 'отдыхать / отлёживаться',        'sich ausruhen',  '{Gesundheit}', 'A2'),
    (4201, 'VERB', 'измерять (давление/температуру)', 'messen',         '{Gesundheit}', 'A2'),
    (4202, 'VERB', 'болеть (причинять боль)',         'wehtun',         '{Gesundheit}', 'A2'),
    (4203, 'VERB', 'кровоточить',                    'bluten',         '{Gesundheit}', 'B1')
ON CONFLICT (id) DO UPDATE SET
    word_type = EXCLUDED.word_type,
    ru        = EXCLUDED.ru,
    de        = EXCLUDED.de,
    tags      = EXCLUDED.tags,
    level     = EXCLUDED.level;

INSERT INTO verb (id, ich, du, er, ihr, wir, sie, partizip_2, root_verb, prefix, notes) VALUES
    (4196, 'erkälte mich',  'erkältest dich', 'erkältet sich',  'erkältet euch',  'erkälten uns',  'erkälten sich',  'erkältet',      NULL,        NULL,   'reflexiv'),
    (4197, 'huste',         'hustest',        'hustet',         'hustet',         'husten',         'husten',         'gehustet',      NULL,        NULL,   NULL),
    (4198, 'niese',         'niest',          'niest',          'niest',          'niesen',         'niesen',         'geniest',       NULL,        NULL,   NULL),
    (4199, 'verschreibe',   'verschreibst',   'verschreibt',    'verschreibt',    'verschreiben',   'verschreiben',   'verschrieben',  'schreiben', 'ver',  NULL),
    (4200, 'ruhe mich aus', 'ruhst dich aus', 'ruht sich aus',  'ruht euch aus',  'ruhen uns aus',  'ruhen sich aus', 'ausgeruht',     'ruhen',     'aus',  'reflexiv'),
    (4201, 'messe',         'misst',          'misst',          'messt',          'messen',         'messen',         'gemessen',      NULL,        NULL,   NULL),
    (4202, 'tue weh',       'tust weh',       'tut weh',        'tut weh',        'tun weh',        'tun weh',        'wehgetan',      'tun',       'weh',  NULL),
    (4203, 'blute',         'blutest',        'blutet',         'blutet',         'bluten',         'bluten',         'geblutet',      NULL,        NULL,   NULL)
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

INSERT INTO word (id, word_type, ru, de, tags, level) VALUES
    (4204, 'ADJECTIVE', 'головокружительный', 'schwindelig',  '{Gesundheit}', 'B1'),
    (4205, 'ADVERB',    'тошно',              'übel',         '{Gesundheit}', 'B1'),
    (4206, 'ADJECTIVE', 'простуженный',       'erkältet',     '{Gesundheit}', 'A2'),
    (4207, 'ADJECTIVE', 'слабый',             'schwach',      '{Gesundheit}', 'A2'),
    (4208, 'ADJECTIVE', 'натощак',            'nüchtern',     '{Gesundheit}', 'B1'),
    (4209, 'ADJECTIVE', 'заразный',           'ansteckend',   '{Gesundheit}', 'B1')
ON CONFLICT (id) DO UPDATE SET
    word_type = EXCLUDED.word_type,
    ru        = EXCLUDED.ru,
    de        = EXCLUDED.de,
    tags      = EXCLUDED.tags,
    level     = EXCLUDED.level;

INSERT INTO adjective (id) VALUES
    (4204),
    (4206),
    (4207),
    (4208),
    (4209)
ON CONFLICT (id) DO NOTHING;

INSERT INTO phrase (id) VALUES
    (4205)
ON CONFLICT (id) DO NOTHING;
