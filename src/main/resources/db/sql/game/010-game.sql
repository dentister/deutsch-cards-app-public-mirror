CREATE TABLE user_settings (
    user_id bigint NOT NULL,
    game_configuration json NULL,
    CONSTRAINT user_settings_users_fk FOREIGN KEY (user_id) REFERENCES users(id)
);

ALTER TABLE user_settings ADD CONSTRAINT user_settings_pk PRIMARY KEY (user_id);
