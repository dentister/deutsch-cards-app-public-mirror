CREATE TABLE tasks (
    id bigserial PRIMARY KEY,
    de_word varchar(100) NOT NULL,
    word_type varchar(20) NOT NULL,
    status varchar(10) NOT NULL DEFAULT 'NEW',
    attempts int NOT NULL DEFAULT 0,
    error text,
    UNIQUE (de_word, word_type)
);
