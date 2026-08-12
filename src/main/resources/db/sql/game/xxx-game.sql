CREATE TABLE article (
    id bigint NOT NULL,
    genitiv character varying(10) NOT NULL,
    dativ character varying(10) NOT NULL,
    akkusativ character varying(10) NOT NULL
);

CREATE TABLE pronoun (
    id bigint NOT NULL,
    genitiv character varying(10) NOT NULL,
    dativ character varying(10),
    akkusativ character varying(10) NOT NULL
);

ALTER TABLE ONLY article
    ADD CONSTRAINT article_pk PRIMARY KEY (id);


    ALTER TABLE ONLY pronoun
    ADD CONSTRAINT pronoun_pk PRIMARY KEY (id);

ALTER TABLE ONLY article
    ADD CONSTRAINT article_word_fk FOREIGN KEY (id) REFERENCES word(id) ON DELETE CASCADE;


    
    ALTER TABLE ONLY pronoun
    ADD CONSTRAINT pronoun_word_fk FOREIGN KEY (id) REFERENCES word(id) ON DELETE CASCADE;


