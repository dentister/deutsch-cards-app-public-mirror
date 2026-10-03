DROP FUNCTION IF EXISTS get_word_scores(BIGINT, TEXT, TEXT[]);
DROP FUNCTION IF EXISTS get_word_scores(BIGINT, TEXT, TEXT, TEXT[]);
DROP FUNCTION IF EXISTS get_word_scores(BIGINT, TEXT, TEXT, TEXT[], BIGINT[]);
DROP FUNCTION IF EXISTS custom_random;

CREATE OR REPLACE FUNCTION get_word_scores(p_user_id BIGINT, p_max_level TEXT, p_word_type TEXT, p_tags TEXT[], p_word_ids BIGINT[])
 RETURNS TABLE(word_id bigint, score double precision, user_id bigint)
 LANGUAGE plpgsql
AS $function$
BEGIN
    RETURN QUERY
    WITH tmp_words AS
      (SELECT
           w.*,
           case when w."level" = 'C2' then 0.75
                when w."level" = 'C1' then 0.8
                when w."level" = 'B2' then 0.85
                when w."level" = 'B1' then 0.9
                when w."level" = 'A2' then 0.95
                else 1
           end prio
       FROM word w
       WHERE (p_word_type IS NULL OR w.word_type = p_word_type)
         AND (p_tags IS NULL OR (array_length(p_tags, 1) > 0 AND w.tags && p_tags ) )
         AND (p_max_level is NULL OR w.level <= p_max_level)
         AND (p_word_ids IS NULL OR w.id = ANY(p_word_ids))
      )
    SELECT
        w.id AS word_id,
        CASE
            WHEN ws.id IS NULL THEN w.prio * custom_random()
            ELSE w.prio * custom_random() * (1 - ws.score::float/(ws.anchor + ws.score) )
        END AS score,
        ws.user_id
    FROM tmp_words w
    LEFT JOIN word_score ws
        ON ws.word_id = w.id
        AND (p_user_id IS NULL OR ws.user_id = p_user_id)
    ORDER BY score desc, word_id asc
    LIMIT 20;
END;
$function$
;

CREATE OR REPLACE FUNCTION custom_random()
 RETURNS double precision
 LANGUAGE plpgsql
AS $function$
    BEGIN
        RETURN random() * 0.25 + 0.75;
    END;
$function$
;
