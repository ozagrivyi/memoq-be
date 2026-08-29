# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spring Boot backend for `memoq`, a cyberpunk-themed quiz/card game used as IT/DevOps/Networking
certification prep. Serves the question-bank CRUD used by the frontend's `editor.html`. Built
from the spec in `PROMPT.md` (Russian) — see `README.md` for the practical developer-facing
version of this (run/build/test commands, endpoints, credentials).

## Commands

```bash
./gradlew build                 # compile + test + assemble
./gradlew test                  # unit + Testcontainers integration tests (needs Docker daemon)
./gradlew test --tests "com.memoq.backend.QuestionApiIntegrationTest"
./gradlew bootRun               # run locally against docker-compose's Postgres
docker compose up -d            # local Postgres only
```

No local JDK is required to be pre-installed for CI-style verification — `docker run --rm -v
"$PWD":/workspace -w /workspace -v /var/run/docker.sock:/var/run/docker.sock --network host
gradle:9.7.1-jdk25 ./gradlew --no-daemon build` works from a bare Docker host (this is how the
whole project was originally verified, since no local JDK/Gradle was available).

## Architecture

Package layout under `com.memoq.backend`: `entity`, `repository`, `dto`, `service`, `controller`,
`security`, `config`, `exception`, `ai` — one JPA entity + one Spring Data repository per table
(`Category`/`CategoryRepository`, `Question`/`QuestionRepository`), services own transactions and
caching, controllers are thin.

- **Question filtering/paging**: `QuestionController.getPage` takes optional `categoryId`/`search`
  plus a `Pageable`, delegates to `QuestionService.getPage`, which builds a
  `QuestionSpecifications.filter(...)` (`JpaSpecificationExecutor`) and wraps the result in a
  `PagedModel` at the controller boundary (not `Page<T>` directly — avoids Spring Data's own
  warning about leaking `Page` from REST controllers).
- **Caching**: `CacheConfig` registers three named caches (`questions`, `questionPages`,
  `categories`). Caffeine backs them locally (`spring.cache.type: caffeine`, the default); the
  `prod` profile switches to `spring.cache.type: redis`, at which point Spring Boot's own
  `RedisAutoConfiguration` supplies the `CacheManager` instead (`CacheConfig`'s Caffeine bean is
  `@ConditionalOnProperty`-gated off in that case). Single-question reads/writes key on `#id`;
  page reads key on a composite of categoryId/search/pageable, so page-cache entries are evicted
  wholesale (`allEntries = true`) on any create/update/delete rather than targeted — there's no
  cheap way to invalidate just the affected pages.
- **Auth**: no user table — PROMPT.md's domain model only has `categories`/`questions`, so this
  is a single-admin tool. `AuthController` checks one configured identity
  (`security.admin.username`/`password-hash`, a BCrypt hash) and issues a JWT
  (`security.jwt.secret`/`expiration-minutes`). `JwtAuthenticationFilter` (a plain
  `OncePerRequestFilter`) reads `Authorization: Bearer <token>` on every request.
  `SecurityConfig` permits `/api/v1/auth/**`, `/actuator/health/**`, and the swagger/OpenAPI
  paths; everything else — including `/api/v1/categories`, which PROMPT.md doesn't explicitly
  scope — requires a valid JWT. An explicit `authenticationEntryPoint` forces 401 (not Spring
  Security's 403 default) for missing/invalid tokens.
- **Errors**: `GlobalExceptionHandler` (`@RestControllerAdvice`) maps `ResourceNotFoundException`
  → 404, `ConflictException`/`DataIntegrityViolationException` → 409,
  `MethodArgumentNotValidException` → 400 with field errors, auth failures → 401/403, everything
  else → 500, all as a uniform `ApiError` body.
- **Categories**: `CategoryController` has no create/update split — `PUT /api/v1/categories/{slug}`
  upserts (creates if the slug is new, renames if it exists), keyed by slug rather than a
  server-generated id, so the client can address a not-yet-existing category by the name it wants.
- **Options are a normalized child table, not fixed columns.** `Question.options` is a
  `List<QuestionOption>` (`question_options` table, one row per option, `@OrderBy("position ASC")`),
  2-8 options per question, any number of which may be correct (`QuestionOptionRequest(text,
  correct)` in the request DTO) — this replaced PROMPT.md's literal `option_1..4`/`correct_option`
  columns (see Migrations' `V6__question_options.sql`) specifically to support multi-select
  questions and to match the frontend's `options: [{text, isCorrect}]` array shape (see Known
  deviation below). `QuestionService.applyRequest` always replaces a question's whole option list
  on create/update (`clear()` + re-add with positions `1..N`) rather than diffing it.
- **AI-driven category assignment**: `QuestionRequest` has no `categoryId` — clients don't choose
  a question's category. `ai/CategoryInferenceService.resolveCategory` runs inside
  `QuestionService.applyRequest` on every create/update, asks Claude (via `ChatClient`) to pick
  the best-fitting existing category (by name, given the question text/options/explanation) or
  propose a new one, then reuses a case-insensitive name match or creates a new `Category`
  (auto-slugified, collision-suffixed) and evicts the categories cache. This makes a real
  `ANTHROPIC_API_KEY` a hard requirement for question create/update to work at all — not optional
  scaffolding. `config/AiConfig` + `ai/QuestionAiService` (`generateQuestion`/`validateQuestion`)
  remain separate, still-unwired scaffolding for PROMPT.md section 6's broader "future
  generation/validation" ask. `ANTHROPIC_API_KEY` defaults to a placeholder so the app still
  boots without one; only the AI calls themselves fail until a real key is set.
- **AI-driven wrong-answer rationales**: `ai/AnswerRationaleService.generateRationales` runs
  alongside category inference inside the same `applyRequest`, asking Claude for one short "why
  this is wrong" line per incorrect option (empty string for correct ones), stored on
  `QuestionOption.wrongExplanation`. This is what powers the frontend's INTEL CARDS matching
  mini-game (`app.js` — rationale cards the player matches to the wrong answer they expose);
  generated once at write time and stored, not regenerated per game session.
- **Migrations**: `db/migration/V1__init_schema.sql` (schema), `V2__seed_categories.sql` (seeds
  categories matching the frontend's existing question set — Networking, HTTP, Docker, etc.),
  `V3__add_wrong_answer_explanations.sql`, `V4__add_question_stats.sql`,
  `V5__add_settings.sql` (see Settings below), `V6__question_options.sql` (normalizes
  `option_1..4`/`correct_option`/`wrong_explanation_1..4` into the `question_options` child table
  described above, migrating existing rows in place before dropping the old columns).
- **Settings**: single global row (`entity/Settings`, table `settings`), not per-user — same
  single-admin-tool rationale as Auth above. Always addressed by the fixed
  `Settings.SINGLETON_ID`, seeded once by `V5__add_settings.sql`, rather than a "first row found"
  query. `GET`/`PUT /api/v1/settings` (`SettingsController`/`SettingsService`) is a full-replace
  upsert — the client always sends every field together, there's no partial-patch semantics.
  Currently holds player-facing game preferences (`timerEnabled`/`timerSeconds`,
  `rephraseEnabled`) that used to live only in the frontend's `localStorage`; the frontend's
  `Api.getSettings`/`updateSettings` localStorage stub was removed in favor of calling this
  endpoint directly via `MemoqAuth.apiFetch`, same as every other real endpoint.
- **Game/AI endpoints beyond category assignment**: `GameController` (`/api/v1/game`) also
  exposes `POST /taunt` (`ai/AmTauntService`), `POST /ask` (`ai/AskAmService` — see below),
  `POST /explain` (`ai/ExplanationSimplifierService` — simplifies a question's stored explanation
  on demand, not persisted), and `POST /rephrase` (`ai/QuestionRephraseService` — rewords a
  question's text and all of its options (2-8, whatever the question has) on demand, called by
  the frontend only when `Settings.rephraseEnabled` is on; keeps option order/correctness intact,
  never written back to the DB). All four are on-demand, per-request Claude calls with no caching
  and no persistence — same "call Claude fresh, don't store the result" shape as category
  inference's structured-output pattern, just without the DB write.
- **`POST /game/ask`** (`ai/AskAmService`) backs the frontend's real-time "talk to AM" terminal
  input — free-text player messages about whatever question is on screen, answered in character.
  This is the one AI endpoint that takes arbitrary player-authored text, so the prompt treats the
  message strictly as data to answer *about*, never as instructions. It's also deliberately never
  told which option is correct: options are sent without their `correct` flag and the stored
  explanation (which justifies the correct answer) is not sent at all, and the prompt carries an
  unconditional refusal rule against ever confirming/denying/hinting at the correct option, no
  matter how the request is framed (directly asking, "round's already over", "I'm the admin",
  etc.) — scoped otherwise to general AWS infrastructure/IaC-template discussion. This followed a
  real incident where a softer, conditional "don't reveal it" instruction plus an in-context
  answer-justifying explanation let a direct "which one is correct?" through.

## Known deviation from the live frontend

PROMPT.md's `questions` schema originally used literal `option_1..4`/`correct_option` columns;
`V6__question_options.sql` replaced that with the normalized `question_options` table (see
Architecture above), which now matches what `/home/neuromancer/projects/memoq`'s
`editor.html`/`api.js`/`questions-data.js` read and write for options: `options: [{text,
isCorrect}]`, any number correct. That part of the original mismatch is resolved.

Two gaps remain: the frontend's question schema also carries `hint`, `difficulty`, and `code`
fields that `Question`/`QuestionRequest` still don't have at all; and the frontend treats
`category` as a free string, while this backend still stores it as a `category_id` FK (narrowed in
practice by `CategoryInferenceService` auto-creating-by-name, but still a structural difference a
client has to bridge). Wiring the real frontend up to this API still needs either a mapping layer
in `api.js` or frontend changes for `hint`/`difficulty`/`code` — nothing in this repo does that
today. See the frontend repo's own `CLAUDE.md` for its side of this.

## Version-pinning notes (things that surprised us building this)

Built against **Spring Boot 4.1 / Spring Framework 7**, not the Boot 3.x line — 3.5 reached end
of OSS support 2026-06-30. A few artifact/package renames that aren't yet muscle memory and are
easy to get wrong from older Boot 3 knowledge:

- Web starter is `spring-boot-starter-webmvc`, not `spring-boot-starter-web`.
- Flyway comes via `spring-boot-starter-flyway` + explicit `org.flywaydb:flyway-database-postgresql`.
- Per-domain test starters (`spring-boot-starter-webmvc-test`, `-data-jpa-test`,
  `-security-test`, etc.) replace the old one-size-fits-all `spring-boot-starter-test`.
  `@AutoConfigureMockMvc` now lives in `org.springframework.boot.webmvc.test.autoconfigure`
  (was `org.springframework.boot.test.autoconfigure.web.servlet`).
- Boot 4 defaults to **Jackson 3** (`tools.jackson.databind.ObjectMapper`, groupId
  `tools.jackson.core`), not classic Jackson 2 (`com.fasterxml.jackson.databind`) — matters if
  you autowire `ObjectMapper` anywhere (e.g. in tests).
- Testcontainers artifacts are `org.testcontainers:testcontainers-junit-jupiter` /
  `testcontainers-postgresql`, and `PostgreSQLContainer` moved to
  `org.testcontainers.postgresql` (was `org.testcontainers.containers`).
- Spring Security's default unauthenticated-request response is 403, not 401 — needed an explicit
  `exceptionHandling(...).authenticationEntryPoint(...)` in `SecurityConfig` to get REST-correct
  401 semantics for a token API.
- `spring-boot-starter-data-redis` being on the classpath (needed for the `prod` cache profile)
  makes actuator's health check probe Redis by default even when `spring.cache.type: caffeine` is
  active locally — `management.health.redis.enabled: false` in the base `application.yml`
  (re-enabled in `application-prod.yml`) avoids a false `DOWN`.
- With a Hibernate `GenerationType.UUID` id (not `IDENTITY`), `@CreationTimestamp`/
  `@UpdateTimestamp` aren't guaranteed populated on the entity instance immediately after
  `save()` — no forced early flush happens the way it does for identity-column inserts. Services
  use `saveAndFlush(...)` on create/update so the response DTO doesn't come back with null
  timestamps.

If bumping dependency versions later, verify actual coordinates/packages against Maven Central
and Spring Initializr's metadata rather than assuming — several of the above were wrong in ways
that only surfaced at compile or runtime, not from prior training knowledge.
