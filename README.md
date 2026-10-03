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

Implementation: [`GermanCardsBot`](telegram-bots/src/main/java/com/kniazev/cards/word/telegram/bot/GermanCardsBot.java),
[`BlitzGameService`](ai-integration/src/main/java/com/kniazev/cards/word/ai/blitz/BlitzGameService.java).

### 2. Telegram Mini App — "pick specific words"
A static web page (`/miniapp/`, see [`MiniAppWebConfig`](telegram-bots/src/main/java/com/kniazev/cards/word/telegram/web/MiniAppWebConfig.java) and
[`src/main/resources/static/miniapp/`](telegram-bots/src/main/resources/static/miniapp)), opened via a button in the bot
(`WebAppInfo`). Lets the user check specific words from the whole catalog for targeted practice.

Backend — REST API `/api/miniapp/**` ([`MiniAppController`](telegram-bots/src/main/java/com/kniazev/cards/word/telegram/web/MiniAppController.java)):
- `GET /api/miniapp/words` — the whole word catalog (for client-side search/filter)
- `GET /api/miniapp/selection` — the user's current selection
- `POST /api/miniapp/selection` — save the selection and restart the game with it

Authentication — the `X-Telegram-Init-Data` header (signed Telegram WebApp data, see
[`TelegramInitDataValidator`](telegram-bots/src/main/java/com/kniazev/cards/word/telegram/web/TelegramInitDataValidator.java)); the
request body itself is treated as untrusted.

### 3. Admin Telegram bot — AI-assisted word adding
A separate bot (its own token, `admin.bot.token`), open to every account whose `roles` contain
`ADMIN`, matched by Telegram user id (see [Accounts & authentication](#accounts--authentication)).
The admin sends a German word/phrase (optionally with a proposed
translation), the bot uses Gemini to draft a complete word card (gender/plural, conjugation,
Rektion, a usage example, translation) and shows it with **Accept / Reject / Recheck** buttons.
Nothing is written to the database until Accept.

Implementation: [`AdminCardsBot`](telegram-bots/src/main/java/com/kniazev/cards/word/telegram/bot/AdminCardsBot.java),
[`WordDraftService`](ai-integration/src/main/java/com/kniazev/cards/word/ai/draft/WordDraftService.java).

### 4. Admin web UI (Vaadin)
Available after logging in via
[`LoginView`](core/src/main/java/com/kniazev/cards/word/ui/view/LoginView.java).

| Route              | Purpose                        |
|--------------------|----------------------------------|
| `/login`           | Login form                       |
| `/`                | Landing page: forwards to `/dictionary` for `ADMIN` and `LEARNER` accounts |
| `/dictionary` | The whole dictionary, filterable by text, type and tags; `ADMIN` can also add, edit and delete words |
| `/nouns`      | Noun list (read-only)            |
| `/verbs`      | Verb list (read-only)            |
| `/adjectives` | Adjective list (read-only)       |
| `/adverbs`    | Adverb list (read-only)          |
| `/phrases`    | Phrase list (read-only)          |
| `/users`      | User management (admin only)     |

Every page except `/login` needs a login and the `ADMIN` or `LEARNER` role (`/users`: `ADMIN` only).

Routes are defined in [`UIRoute`](core/src/main/java/com/kniazev/cards/word/ui/UIRoute.java), screens live under
[`src/main/java/.../ui/view`](core/src/main/java/com/kniazev/cards/word/ui/view).

### 5. Dictionary REST API — `/api/word-cards/v1/**`
Programmatic CRUD/search over words, generated with the delegate pattern from the OpenAPI spec
[`word-cards.yaml`](core/src/main/resources/swagger/word-cards.yaml) (`openapi-generator-maven-plugin`,
implemented in [`WordCrudController`](core/src/main/java/com/kniazev/cards/word/dictionary/WordCrudController.java)):

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
  [`UserService.getOrCreateByTelegramId`](core/src/main/java/com/kniazev/cards/word/user/UserService.java)
  and [`GermanCardsBot.getOrCreate`](telegram-bots/src/main/java/com/kniazev/cards/word/telegram/bot/GermanCardsBot.java).
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
- **Logging in on the web** (`/login`, [`LoginView`](core/src/main/java/com/kniazev/cards/word/ui/view/LoginView.java)) — two options:
  - **Handle + password**, using the credentials from the bot.
  - **"Log in with Telegram"** — Telegram's [Login Widget](https://core.telegram.org/widgets/login),
    verified server-side by [`TelegramLoginWidgetValidator`](telegram-bots/src/main/java/com/kniazev/cards/word/telegram/web/TelegramLoginWidgetValidator.java)
    and handled by [`TelegramLoginController`](telegram-bots/src/main/java/com/kniazev/cards/word/telegram/web/TelegramLoginController.java)
    (`GET /telegram-login/callback`). It only signs in an **existing** account (or one that can be
    bound as described above) — it never creates one, so you still need to have messaged the bot
    at least once first.
- The `/users` page (accounts, roles, passwords) is restricted to `ADMIN`.

(The `testuser` demo login in the [Live demo](#live-demo) section above predates this flow and
isn't bot-created.)

## Code layout

A multi-module Maven build. Every module keeps the same root package `com.kniazev.cards.word`, so component scanning, JPA
and Vaadin routes work as they did in a single module.

| Module | What lives there | Depends on |
|---|---|---|
| [`core`](core) | The dictionary of words and phrases (`dictionary`), accounts and roles (`user`), the game (`game`), the dictionary REST API, Liquibase migrations, and the Vaadin admin UI (`ui`: views, dialogs, the `myapp` theme, the frontend build) | — |
| [`ai-integration`](ai-integration) | Spring AI / Gemini: word drafts and the background job (`ai.draft`), the blitz round (`ai.blitz`), usage examples (`ai.example`), the prompts | `core` |
| [`telegram-bots`](telegram-bots) | The learning bot and the admin bot (`telegram.bot`), how the game looks in Telegram (`GameCardPresenter`) and the bot texts (the `i18n` bundle), the Mini App API and the Telegram login (`telegram.web`), the static Mini App | `core`, `ai-integration` |
| [`app`](app) | The Spring Boot main class, `application*.properties`, the context tests and the executable JAR | all of the above |

Rules that keep the structure honest:

- Dependencies point one way: `app` → everything; `telegram-bots` → `ai-integration` → `core`. Inside `core` the UI and
  the game sit on top of the domain: `ui` → `user` → `dictionary` → `common` and `game` → `user` → `dictionary` →
  `common`. The domain never imports `ui`.
- Vaadin is used only in the `ui` package, `org.telegram` only in `telegram-bots`, Spring AI only in `ai-integration`.
  Vaadin is a dependency of the whole `core` module, so `ai-integration` and `telegram-bots` see it too: the Maven
  enforcer guards the declared Telegram and Spring AI dependencies, while `ArchitectureTests` (in `app`) checks the
  imports of all modules, including that nothing outside `ui` touches Vaadin and nothing outside it imports `ui`.
- `core` does not know about AI: the game asks for a word to be completed through the `WordEnrichment` port and the AI
  module implements it.
- `core` owns the database schema: all Liquibase changelogs live there, including the `tasks` table that
  `ai.draft.DraftTask` maps.
- `application*.properties` exist only in `app`: same-named files in several JARs would shadow each other.
- Where does a new class go? Ask which feature it belongs to (`core`'s `dictionary`, `user`, `game`), or how it
  reaches the user (a bot, the AI, the web UI in `core`'s `ui`), or whether everyone needs it (`core`'s `common`).

Building and testing:

```bash
mvn test -DexcludedGroups=docker   # unit and architecture tests, no Docker needed
mvn verify                         # everything, including the Testcontainers and browser tests (needs Docker)
mvn test -DexcludedGroups=ui       # everything except the browser tests (needs Docker)
mvn -Pproduction package           # app/target/deutsche-cards-app.jar with the Vaadin production bundle
```

The browser tests (`*UiTests` in `app`, tagged `ui`) start the whole application on a random port with Vaadin
switched on and click through it in a headless Chromium via Playwright: access control, navigation, filters and the
admin's add, edit and delete of words. The first run downloads Chromium (about 150 MB) and Vaadin builds its
development frontend bundle (a few seconds once `core/node_modules` exists, longer on a clean checkout, and it needs
network access); add `-Dplaywright.headed=true` to watch the browser. The deploy workflows pass
`-DexcludedGroups=ui`, so a missing browser can never block a deployment.

Vaadin development mode (run `WordCardsApplication` from an IDE or with `spring-boot:run`) needs no extra setup: `ui.config.VaadinProjectFolder` points Vaadin at the `core` folder, where the frontend
files and the `myapp` theme live (Vaadin would otherwise look in `app`, the first `target/classes` on the classpath).

## Tech stack

| Layer                      | Technologies |
|-----------------------------|------------|
| Language / platform          | Java 21, Spring Boot 3.4.11 |
| Web UI (admin)                | Vaadin Flow 24.7.3 + `crudui`, `line-awesome`, `vaadin-chip-combobox`, `bootstrap-for-vaadin` |
| Security                     | Spring Security (`VaadinWebSecurity` for the UI, a separate filter chain for headless mode) |
| Data                          | PostgreSQL, Spring Data JPA / Hibernate, HikariCP, Liquibase (migrations) |
| Caching                       | Redis (`spring-boot-starter-data-redis`) — TTL-based cache for in-progress game state, survives app restarts; see [`GameCache`](core/src/main/java/com/kniazev/cards/word/game/GameCache.java) |
| Telegram                     | `telegrambots-springboot-longpolling-starter` — two independent long-polling bots |
| AI                            | Spring AI 1.1.8 + `spring-ai-starter-model-google-genai` (Google Gemini, `gemini-3.1-flash-lite`), structured output (`BeanOutputConverter`) straight into Java records (`WordDraft`, `WordUsageExample`, `BlitzSentences`/`BlitzCheckResult` for the blitz round) |
| REST / API-first              | `openapi-generator-maven-plugin` (generates delegate interfaces and models from YAML specs), springdoc-openapi (Swagger UI) |
| Mini App                      | Static HTML/CSS/JS, served directly by Spring Boot |
| Build / misc                  | Maven (multi-module; the `production` profile builds the Vaadin frontend and the executable JAR), Lombok, ModelMapper, Testcontainers (PostgreSQL, Redis) for tests |
| Containerization / deployment | Docker (multi-stage: Maven+Node → JRE Alpine), GitHub Actions → deployed to a VPS (`scp` + `nohup java -jar`) |


## Building the production image

```bash
docker build -t deutsch-cards-app .
```

Builds the JAR with the `-Pproduction` profile (Vaadin production frontend build) and
packages it into `eclipse-temurin:21-jre-alpine`. Deployment to the VPS happens automatically on
every push to `master` (see [`.github/workflows/deploy-vps.yml`](.github/workflows/deploy-vps.yml)).
