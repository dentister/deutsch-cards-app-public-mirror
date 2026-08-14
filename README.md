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
- `/settag` — filter by topic tags (inline keyboard with checkboxes)
- `/cleartag` — clear the tag filter
- `/setwords` — open the Mini App to pick specific words to train
- `/clearwords` — go back to training on all words
- `/switchpreview` — toggle showing the new-words list at the start of a round

Implementation: [`GermanCardsBot`](src/main/java/com/kniazev/cards/word/telegram/GermanCardsBot.java).

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
A separate bot (its own token, `admin.bot.token`), restricted to a single user
(`admin.bot.allowed-username`). The admin sends a German word/phrase (optionally with a proposed
translation), the bot uses Gemini to draft a complete word card (gender/plural, conjugation,
Rektion, a usage example, translation) and shows it with **Accept / Reject / Recheck** buttons.
Nothing is written to the database until Accept.

Implementation: [`AdminCardsBot`](src/main/java/com/kniazev/cards/word/telegram/AdminCardsBot.java),
[`WordDraftService`](src/main/java/com/kniazev/cards/word/ai/WordDraftService.java).

### 4. Admin web UI (Vaadin)
Available when Vaadin is enabled (profile without `no-vaadin`), after logging in via
[`LoginView`](src/main/java/com/kniazev/cards/word/ui/view/LoginView.java).

| Route         | Purpose                        |
|---------------|----------------------------------|
| `/login`      | Login form                       |
| `/` , `/game` | Play in the browser              |
| `/dictionary` | Dictionary overview               |
| `/nouns`      | Noun CRUD                        |
| `/verbs`      | Verb CRUD                        |
| `/adjectives` | Adjective CRUD                   |
| `/adverbs`    | Adverb CRUD                      |
| `/phrases`    | Phrase CRUD                      |
| `/users`      | User management                  |

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

## Tech stack

| Layer                      | Technologies |
|-----------------------------|------------|
| Language / platform          | Java 17, Spring Boot 3.4.11 |
| Web UI (admin)                | Vaadin Flow 24.7.3 + `crudui`, `line-awesome`, `vaadin-chip-combobox`, `bootstrap-for-vaadin` |
| Security                     | Spring Security (`VaadinWebSecurity` for the UI, a separate filter chain for headless mode) |
| Data                          | PostgreSQL, Spring Data JPA / Hibernate, HikariCP, Liquibase (migrations) |
| Telegram                     | `telegrambots-springboot-longpolling-starter` — two independent long-polling bots |
| AI                            | Spring AI 1.1.8 + `spring-ai-starter-model-google-genai` (Google Gemini, `gemini-3.1-flash-lite`), structured output (`BeanOutputConverter`) straight into the `WordDraft` Java record |
| REST / API-first              | `openapi-generator-maven-plugin` (generates delegate interfaces and models from YAML specs), springdoc-openapi (Swagger UI) |
| Mini App                      | Static HTML/CSS/JS, served directly by Spring Boot |
| Build / misc                  | Maven (`base`, `production` profiles), Lombok, ModelMapper, Testcontainers (PostgreSQL) for tests |
| Containerization / deployment | Docker (multi-stage: Maven+Node → JRE Alpine), GitHub Actions → deployed to a VPS (`scp` + `nohup java -jar`) |


## Building the production image

```bash
docker build -t deutsch-cards-app .
```

Builds the JAR with the `-Pbase,production` profiles (Vaadin production frontend build) and
packages it into `eclipse-temurin:17-jre-alpine`. Deployment to the VPS happens automatically on
every push to `master` (see [`.github/workflows/deploy-vps.yml`](.github/workflows/deploy-vps.yml)).
