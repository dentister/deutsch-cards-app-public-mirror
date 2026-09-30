# Deutsch Cards App

An app for learning German vocabulary: word cards (nouns, verbs, adjectives, adverbs, phrases)
with spaced-repetition-style games, accessible via a Telegram bot, a Telegram Mini App, and a
Vaadin-based admin web UI. Words can be added manually or via a separate Telegram bot that drafts
cards with AI (Spring AI + Gemini).

## Live demo

The Vaadin admin UI is deployed at **[germancards.run.place](https://germancards.run.place/)** —
log in with `testuser` / `testuser` to try it out.

Telegram bot is available with link **[https://t.me/GermanCardsBot](https://t.me/GermanCardsBot)**

## Ways to interact with the app

### 1. Learning Telegram bot — `@GermanCardsBot`
The main way to play. The bot sends a word/task, the user answers in the chat as plain text.

Commands:
- `/start`, `/restart` — start/restart the game
- `/setlevel` — choose the max CEFR level (A1–C2)
- `/setwords` — open the Mini App to pick specific words to train
- `/clearwords` — go back to training on all words
- `/switchpreview` — toggle showing the new-words list at the start of a round
- `/website` — get (or reissue) the link and password for the web app

When a round finishes on its own (not via `/restart` or similar), the bot may also offer a bonus
**AI blitz round** via yes/no inline buttons: if you opt in, Gemini writes 3 short Russian
sentences built from the words you just practiced, you reply with all 3 German translations in
one message, and the AI checks them and gives feedback before the next round starts. The word
selection is scoped to that just-finished round only and never affects the main per-word score
used to pick future rounds.

Implementation: [`GermanCardsBot`](src/main/java/com/kniazev/cards/word/telegram/GermanCardsBot.java),
[`BlitzGameService`](src/main/java/com/kniazev/cards/word/ai/BlitzGameService.java).

### 2. Telegram Mini App — "pick specific words"
A static web page (`/miniapp/`, see [`MiniAppWebConfig`](src/main/java/com/kniazev/cards/word/config/MiniAppWebConfig.java) and
[`src/main/resources/static/miniapp/`](src/main/resources/static/miniapp)), opened via a button in the bot
(`WebAppInfo`). Lets the user check specific words from the whole catalog for targeted practice.

Backend — REST API `/api/miniapp/**` ([`MiniAppController`](src/main/java/com/kniazev/cards/word/controller/MiniAppController.java)):
- `GET /api/miniapp/words` — the whole word catalog (for client-side search/filter)
- `GET /api/miniapp/selection` — the user's current selection
- `POST /api/miniapp/selection` — save the selection and restart the game with it

Authentication — the `X-Telegram-Init-Data` header (signed Telegram WebApp data, see
[`TelegramInitDataValidator`](src/main/java/com/kniazev/cards/word/telegram/TelegramInitDataValidator.java)); the
request body itself is treated as untrusted.

### 3. Admin Telegram bot — AI-assisted word adding
A separate bot (its own token, `admin.bot.token`), open to every account whose `roles` contain
`ADMIN`, matched by Telegram user id (see [Accounts & authentication](#accounts--authentication)).
The admin sends a German word/phrase (optionally with a proposed
translation), the bot uses Gemini to draft a complete word card (gender/plural, conjugation,
Rektion, a usage example, translation) and shows it with **Accept / Reject / Recheck** buttons.
Nothing is written to the database until Accept.

Implementation: [`AdminCardsBot`](src/main/java/com/kniazev/cards/word/telegram/AdminCardsBot.java),
[`WordDraftService`](src/main/java/com/kniazev/cards/word/ai/WordDraftService.java).

### 4. Admin web UI (Vaadin)
Available when Vaadin is enabled (profile without `no-vaadin`), after logging in via
[`LoginView`](src/main/java/com/kniazev/cards/word/ui/view/LoginView.java).

| Route              | Purpose                        |
|--------------------|----------------------------------|
| `/login`           | Login form                       |
| `/`                | Landing page: forwards to the first page the account may open (`/dictionary` for admins, `/nouns` for everyone else) |
| `/dictionary` | Dictionary overview (admin only) |
| `/nouns`      | Noun CRUD                        |
| `/verbs`      | Verb CRUD                        |
| `/adjectives` | Adjective CRUD                   |
| `/adverbs`    | Adverb CRUD                      |
| `/phrases`    | Phrase CRUD                      |
| `/users`      | User management (admin only)     |

Routes are defined in [`UIRoute`](src/main/java/com/kniazev/cards/word/constant/UIRoute.java), screens live under
[`src/main/java/.../ui/view`](src/main/java/com/kniazev/cards/word/ui/view).

### 5. Dictionary REST API — `/api/word-cards/v1/**`
Programmatic CRUD/search over words, generated with the delegate pattern from the OpenAPI spec
[`word-cards.yaml`](src/main/resources/swagger/word-cards.yaml) (`openapi-generator-maven-plugin`,
implemented in [`WordCrudController`](src/main/java/com/kniazev/cards/word/controller/WordCrudController.java)):

- `GET /api/word-cards/v1/words/search?value=&limit=` — search words
- `POST /api/word-cards/v1/words` — bulk-add words
- `DELETE /api/word-cards/v1/words/{id}` — delete a word

Swagger UI: `/swagger-ui/**` (springdoc, `springdoc.api-docs.path=/swagger-ui/api-docs`).

## Accounts & authentication

Accounts are created only by `@GermanCardsBot`; the web UI, the Mini App and the admin bot never
create one.

- **Identity is the numeric Telegram user id** (`users.telegram_id`, unique), checked on every
  entry point: the bot, the Mini App, "Log in with Telegram" and the admin bot. A Telegram
  `@username` is optional, can be changed and can be re-claimed by someone else, so it is never
  used to recognise a returning user.
- **`users.username` is an internal, immutable handle**: the web login name and the key under which
  game state is stored. It starts as the Telegram `@username`; without one (or if that name is
  already taken) it is `tg-<id>` / `<username>_<id>`. `/website` shows it as "Login".
- **Account creation**: the first message a user sends to the bot creates the row (roles `ALL`,
  `PLAYER`, `LEARNER`) with a random password. See
  [`UserService.getOrCreateByTelegramId`](src/main/java/com/kniazev/cards/word/db/service/UserService.java)
  and [`GermanCardsBot.getOrCreate`](src/main/java/com/kniazev/cards/word/telegram/GermanCardsBot.java).
- **Getting your web credentials**: the password is sent back privately in the bot's welcome
  message, together with a link to the web app. Lost it? Send `/website` to the bot any time to get
  a new one — this rotates the password, since the old plaintext can't be recovered once sent.
- **Accounts created before `telegram_id` existed** are bound lazily: an unbound account whose handle
  equals the sender's current `@username` (exact match, case included) is bound to their Telegram id
  on first contact (bot, admin bot, Mini App or "Log in with Telegram") and the event is logged
  (`WARN` for an `ADMIN` account). From then on the lookup is by id only. Whoever holds the handle
  at that moment gets the account, so an `ADMIN` owner should write to a bot soon after this was
  deployed, before releasing the username.
- **`sysadm`, the seeded admin, is locked against Telegram**: migration `005` gives it a negative
  `telegram_id`, which no real Telegram user can have, so nobody can claim it by holding
  `@sysadm`. It signs in with its password only. The same trick (`telegram_id = -id`) works for any
  account that must never be reachable through Telegram.
- **Granting `ADMIN`** is done in the database (the `/users` page does not edit roles yet):
  `UPDATE users SET roles = array_append(roles, 'ADMIN') WHERE id = <row>;`
- **A duplicate account** (a handle like `<username>_<id>` next to the original) means the original
  could not be bound, for example because its handle differs by case. Merge by hand:
  ```sql
  BEGIN;
  DELETE FROM user_settings WHERE user_id = <duplicate id>;
  DELETE FROM users WHERE id = <duplicate id>;   -- word_score is removed by cascade
  UPDATE users SET telegram_id = <telegram id of the duplicate> WHERE id = <original id> AND telegram_id IS NULL;
  COMMIT;
  ```
- **Logging in on the web** (`/login`, [`LoginView`](src/main/java/com/kniazev/cards/word/ui/view/LoginView.java)) — two options:
  - **Handle + password**, using the credentials from the bot.
  - **"Log in with Telegram"** — Telegram's [Login Widget](https://core.telegram.org/widgets/login),
    verified server-side by [`TelegramLoginWidgetValidator`](src/main/java/com/kniazev/cards/word/telegram/TelegramLoginWidgetValidator.java)
    and handled by [`TelegramLoginController`](src/main/java/com/kniazev/cards/word/controller/TelegramLoginController.java)
    (`GET /telegram-login/callback`). It only signs in an **existing** account (or one that can be
    bound as described above) — it never creates one, so you still need to have messaged the bot
    at least once first.
- The `/users` page (accounts, roles, passwords) is restricted to `ADMIN`.

(The `testuser` demo login in the [Live demo](#live-demo) section above predates this flow and
isn't bot-created.)

## Tech stack

| Layer                      | Technologies |
|-----------------------------|------------|
| Language / platform          | Java 17, Spring Boot 3.4.11 |
| Web UI (admin)                | Vaadin Flow 24.7.3 + `crudui`, `line-awesome`, `vaadin-chip-combobox`, `bootstrap-for-vaadin` |
| Security                     | Spring Security (`VaadinWebSecurity` for the UI, a separate filter chain for headless mode) |
| Data                          | PostgreSQL, Spring Data JPA / Hibernate, HikariCP, Liquibase (migrations) |
| Caching                       | Redis (`spring-boot-starter-data-redis`) — TTL-based cache for in-progress game state, survives app restarts; see [`GameCache`](src/main/java/com/kniazev/cards/word/game/GameCache.java) |
| Telegram                     | `telegrambots-springboot-longpolling-starter` — two independent long-polling bots |
| AI                            | Spring AI 1.1.8 + `spring-ai-starter-model-google-genai` (Google Gemini, `gemini-3.1-flash-lite`), structured output (`BeanOutputConverter`) straight into Java records (`WordDraft`, `WordUsageExample`, `BlitzSentences`/`BlitzCheckResult` for the blitz round) |
| REST / API-first              | `openapi-generator-maven-plugin` (generates delegate interfaces and models from YAML specs), springdoc-openapi (Swagger UI) |
| Mini App                      | Static HTML/CSS/JS, served directly by Spring Boot |
| Build / misc                  | Maven (`base`, `production` profiles), Lombok, ModelMapper, Testcontainers (PostgreSQL, Redis) for tests |
| Containerization / deployment | Docker (multi-stage: Maven+Node → JRE Alpine), GitHub Actions → deployed to a VPS (`scp` + `nohup java -jar`) |


## Building the production image

```bash
docker build -t deutsch-cards-app .
```

Builds the JAR with the `-Pbase,production` profiles (Vaadin production frontend build) and
packages it into `eclipse-temurin:17-jre-alpine`. Deployment to the VPS happens automatically on
every push to `master` (see [`.github/workflows/deploy-vps.yml`](.github/workflows/deploy-vps.yml)).
