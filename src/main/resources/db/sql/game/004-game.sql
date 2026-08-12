CREATE SEQUENCE word_score_seq
    START WITH 1000
    INCREMENT BY 1
    CACHE 1;

CREATE TABLE word_score (
    id bigint DEFAULT nextval('word_score_seq') NOT NULL,
    word_id bigint NOT NULL,
    user_id bigint NOT NULL,
    anchor int NOT NULL,
    score int NOT NULL,
    CONSTRAINT word_score_pk PRIMARY KEY (id),
    CONSTRAINT word_score_unique UNIQUE (word_id,user_id),
    CONSTRAINT word_score_word_fk FOREIGN KEY (word_id) REFERENCES word(id) ON DELETE CASCADE,
    CONSTRAINT word_score_users_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX word_score_word_id_idx ON word_score (word_id,user_id);
CREATE INDEX word_score_user_id_idx ON word_score (user_id);

ALTER TABLE word DROP COLUMN anchor;
ALTER TABLE word DROP COLUMN score;

CREATE OR REPLACE FUNCTION custom_random()
RETURNS float
LANGUAGE plpgsql AS $function$
	BEGIN
	    RETURN random() * 0.4 + 0.6;
	END;
$function$;

CREATE OR REPLACE FUNCTION get_word_scores(p_user_id bigint)
RETURNS TABLE(word_id bigint, score double precision, user_id bigint) AS
$$
BEGIN
    RETURN QUERY
    SELECT 
        w.id AS word_id,
        CASE 
            WHEN ws.id IS NULL THEN custom_random()
            ELSE custom_random()*(ws.anchor + ws.score)/ws.anchor ::float
        END AS score,
        ws.user_id
    FROM word w
    LEFT JOIN word_score ws 
        ON ws.word_id = w.id 
        AND (p_user_id IS NULL OR ws.user_id = p_user_id)
    ORDER BY score desc, word_id asc
    LIMIT 30;
END;
$$ LANGUAGE plpgsql;

