CREATE TABLE adjective (
    id bigint NOT NULL PRIMARY KEY,
    
    CONSTRAINT adjective_to_word_fk FOREIGN KEY (id) REFERENCES word(id) ON DELETE CASCADE
);

CREATE TABLE adverb (
    id bigint NOT NULL PRIMARY KEY,
    
    CONSTRAINT adverb_to_word_fk FOREIGN KEY (id) REFERENCES word(id) ON DELETE CASCADE
);

CREATE TABLE phrase (
    id bigint NOT NULL PRIMARY KEY,
    
    CONSTRAINT phrase_to_word_fk FOREIGN KEY (id) REFERENCES word(id) ON DELETE CASCADE
);