CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE categories (
    id          UUID PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    slug        VARCHAR(120) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_categories_name UNIQUE (name),
    CONSTRAINT uq_categories_slug UNIQUE (slug)
);

CREATE TABLE questions (
    id              UUID PRIMARY KEY,
    category_id     UUID NOT NULL REFERENCES categories (id) ON DELETE RESTRICT,
    question_text   TEXT NOT NULL,
    option_1        TEXT NOT NULL,
    option_2        TEXT NOT NULL,
    option_3        TEXT NOT NULL,
    option_4        TEXT NOT NULL,
    correct_option  INTEGER NOT NULL,
    explanation     TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_questions_correct_option CHECK (correct_option BETWEEN 1 AND 4)
);

CREATE INDEX idx_questions_category_id ON questions (category_id);
CREATE INDEX idx_questions_question_text_trgm ON questions USING gin (question_text gin_trgm_ops);
