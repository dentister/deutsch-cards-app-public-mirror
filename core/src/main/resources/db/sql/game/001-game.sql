CREATE SEQUENCE word_seq
    START WITH 1000
    INCREMENT BY 1
    CACHE 1;

CREATE TABLE word (
    id bigint DEFAULT nextval('word_seq') NOT NULL PRIMARY KEY,
    word_type varchar(20) NOT NULL,
    ru varchar(100) NOT NULL,
    de varchar(100) NOT NULL,
    anchor integer DEFAULT 3 NOT NULL,
    score integer DEFAULT 0 NOT NULL,
    
    UNIQUE(ru, word_type, de)
);

CREATE TYPE gender_type AS ENUM (
    'M',
    'F',
    'N');

CREATE TABLE noun (
    id bigint NOT NULL PRIMARY KEY,
    gender gender_type NOT NULL,
    plural varchar(100),
    
    CONSTRAINT nouns_to_word_fk FOREIGN KEY (id) REFERENCES word(id) ON DELETE CASCADE
);

CREATE TABLE verb (
    id bigint NOT NULL PRIMARY KEY,
    ich varchar(100) NOT NULL,
    du varchar(100) NOT NULL,
    er varchar(100) NOT NULL,
    ihr varchar(100) NOT NULL,
    wir varchar(100) NOT NULL,
    sie varchar(100) NOT NULL,
    
    CONSTRAINT verbs_to_word_fk FOREIGN KEY (id) REFERENCES word(id) ON DELETE CASCADE
);