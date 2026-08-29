# memoq Backend

Spring Boot backend for the [memoq](https://am.neuromancerdream.com) quiz game's question editor
(`editor.html` in the frontend repo). Implements the spec in `PROMPT.md`.

## Stack

- **Java 25** (LTS) — PROMPT.md asked for Java 24, but Java 24's support window has already ended
  in favor of 25 LTS by the time this was built; 25 is the closest currently-supported version.
- **Spring Boot 4.1.1** / Spring Framework 7 — the current stable line as of this writing (3.5.x
  reached end of OSS support 2026-06-30).
- Spring Data JPA + Hibernate 7, Flyway, PostgreSQL
- Spring Security 7 with stateless JWT (`io.jsonwebtoken` / jjwt)
- Spring Cache: Caffeine locally, Redis in the `prod` profile
- Spring AI 2.0 (Anthropic/Claude starter) — drives category assignment on question writes, see [AI](#ai)
- springdoc-openapi 3.1 (Swagger UI)
- Gradle (Kotlin DSL)

## Schema decision

Questions have a normalized `question_options` child table (2-8 options per question, any number
correct — see `V6__question_options.sql`), matching the frontend's `options: [{text, isCorrect}]`
shape and its multi-select questions. `category` is still a `category_id` FK rather than the
frontend's free-string category, and `hint`/`difficulty`/`code` (fields the frontend's
`editor.html`/`api.js` also read/write) aren't modeled here at all — wiring the real frontend up
to this API still needs either an `api.js` mapping layer or frontend changes for those two gaps.
See `CLAUDE.md` for details.

## Running locally

```bash
docker compose up -d          # Postgres on localhost:5432
./gradlew bootRun             # starts the app on :8080 against it
```

Or skip Docker entirely for tests — `./gradlew test` spins up its own throwaway Postgres via
Testcontainers (needs a working Docker daemon, nothing else).

Swagger UI: http://localhost:8080/swagger-ui.html · OpenAPI JSON: http://localhost:8080/v3/api-docs

### Default dev credentials

There's no user table (PROMPT.md's domain model only has `categories`/`questions` — this is a
single-editor tool), so auth is one configured admin identity. The local dev default is
`admin` / `changeit` (see `security.admin.*` in `application.yml`) — **always** override
`ADMIN_USERNAME`/`ADMIN_PASSWORD_HASH` (a BCrypt hash) outside local dev. Likewise `JWT_SECRET`
has a dev-only default that must be overridden everywhere else.

```bash
curl -X POST localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"changeit"}'
# => {"accessToken": "...", "tokenType": "Bearer", "expiresInSeconds": 3600}

curl localhost:8080/api/v1/editor/questions -H "Authorization: Bearer <token>"
```

## Build & test commands

```bash
./gradlew build                 # compile + test + assemble
./gradlew test                  # unit + Testcontainers integration tests
./gradlew test --tests "com.memoq.backend.QuestionApiIntegrationTest"   # single test class
./gradlew bootJar               # build the runnable jar (build/libs/*.jar)
```

No local JDK/Gradle install is required — `./gradlew` bootstraps its own JDK-agnostic wrapper,
but you do need a JDK 25 available, or use the `gradle:9.7.1-jdk25` Docker image:

```bash
docker run --rm -v "$PWD":/workspace -w /workspace -v /var/run/docker.sock:/var/run/docker.sock \
  --network host gradle:9.7.1-jdk25 ./gradlew --no-daemon build
```

## Docker image

```bash
docker build -t memoq-backend:latest .
docker run -p 8080:8080 \
  -e DB_HOST=host.docker.internal -e DB_USER=memoq -e DB_PASSWORD=memoq \
  memoq-backend:latest
```

Multistage build: `gradle:9.7.1-jdk25` to compile, `eclipse-temurin:25-jre-alpine` to run, as a
non-root user.

## Kubernetes

`k8s/` targets the same `memoq` namespace/kind cluster the frontend uses (see the frontend
repo's `CLAUDE.md`). That cluster has no ingress controller installed — the frontend is instead
exposed to `https://am.neuromancerdream.com` via an external Traefik file-provider route straight
to a NodePort. `k8s/ingress.yaml` is provided because PROMPT.md asks for one, but
`k8s/service.yaml` (NodePort 30081) is the path that actually works against this environment
today; see the comments in both files.

```bash
docker build -t memoq-backend:latest .
kind load docker-image memoq-backend:latest --name memoq
kubectl --context kind-memoq apply -f k8s/configmap.yaml
kubectl --context kind-memoq apply -f k8s/deployment.yaml -f k8s/service.yaml
```

**Do not `kubectl apply -f k8s/secret.yaml`** — every value in it is the literal string
`REPLACE_ME`, and applying it as-is overwrites the live `memoq-backend-secret` with those
placeholders (this has actually happened: a routine redeploy that included this step broke JWT
signing and locked the DB out from under itself). Generate and apply the real Secret out-of-band
instead, per that file's own header comment — a normal redeploy only ever needs the two commands
above plus a `rollout restart`, never touching the Secret.

Postgres and Redis aren't provisioned by these manifests — point `DB_HOST`/`REDIS_HOST` in
`k8s/configmap.yaml` at wherever they actually run.

## AI

Uses Claude (`spring-ai-starter-model-anthropic`, model `claude-haiku-4-5-20251001` by default)
throughout:

- **Category assignment is AI-driven, not client-supplied.** `QuestionRequest` has no
  `categoryId` field — `ai/CategoryInferenceService.resolveCategory` runs on every
  `POST`/`PUT /api/v1/editor/questions` call, asks Claude to pick the best-fitting existing
  category (or propose a new one) from the question's text/options/explanation, and either
  reuses a matching category or creates one. This means **question create/update needs a real
  `ANTHROPIC_API_KEY` to work at all** — without one, every create/update call fails.
- **Wrong-answer rationales are AI-generated too**, in the same request: `ai/AnswerRationaleService`
  writes one short "why this is wrong" line per incorrect option, stored on each
  `QuestionOption` and used by the frontend's card-matching mini-game.
- `GameController` (`/api/v1/game`) exposes four more Claude-backed, on-demand endpoints used
  during actual gameplay (not question editing): `POST /taunt`, `POST /ask` (real-time chat with
  AM about the current question — deliberately never given the correct answer, and instructed to
  refuse ever confirming/hinting at one, in-scope only for general AWS infra/IaC-template
  discussion), `POST /explain`, `POST /rephrase`. None of these persist their output.
- `config/AiConfig.java` and `ai/QuestionAiService.java` (`generateQuestion`/`validateQuestion`)
  are separate, still-unwired scaffolding for PROMPT.md section 6's "future question
  generation/validation" ask — not on the critical path.

`ANTHROPIC_API_KEY` defaults to a placeholder so the app still boots without one; only the AI
calls themselves fail until a real key is set (see `k8s/secret.yaml`).
