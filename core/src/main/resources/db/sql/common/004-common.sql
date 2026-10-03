-- Telegram's numeric user id is the identity of a Telegram-originated account; users.username stays as the
-- internal, immutable login/handle. Existing rows are bound lazily on first contact (see UserService).
alter table users add column if not exists telegram_id bigint;

create unique index if not exists users_telegram_id_unique on users (telegram_id);
create unique index if not exists users_username_unique on users (username);
