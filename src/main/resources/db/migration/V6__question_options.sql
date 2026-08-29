-- No uniqueness constraint on (question_id, position): QuestionService.applyRequest replaces a
-- question's whole option list on every create/update (clear() + re-add with positions 1..N), and
-- Hibernate flushes those inserts before the orphan-removal deletes of the old rows, so the same
-- (question_id, position) pair transiently exists twice mid-flush. Ordering is a display concern
-- only (@OrderBy("position ASC") on Question.options), not a data integrity one.
CREATE TABLE question_options (
    id                 UUID PRIMARY KEY,
    question_id        UUID NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    position           INTEGER NOT NULL,
    option_text        TEXT NOT NULL,
    is_correct         BOOLEAN NOT NULL DEFAULT false,
    wrong_explanation  TEXT
);

CREATE INDEX idx_question_options_question_id ON question_options (question_id);

-- Move each existing question's fixed option_1..4 / correct_option / wrong_explanation_1..4
-- into one row per option (position 1-4), preserving order and correctness.
INSERT INTO question_options (id, question_id, position, option_text, is_correct, wrong_explanation)
SELECT gen_random_uuid(), id, 1, option_1, correct_option = 1, wrong_explanation_1 FROM questions
UNION ALL
SELECT gen_random_uuid(), id, 2, option_2, correct_option = 2, wrong_explanation_2 FROM questions
UNION ALL
SELECT gen_random_uuid(), id, 3, option_3, correct_option = 3, wrong_explanation_3 FROM questions
UNION ALL
SELECT gen_random_uuid(), id, 4, option_4, correct_option = 4, wrong_explanation_4 FROM questions;

ALTER TABLE questions
    DROP COLUMN option_1,
    DROP COLUMN option_2,
    DROP COLUMN option_3,
    DROP COLUMN option_4,
    DROP COLUMN correct_option,
    DROP COLUMN wrong_explanation_1,
    DROP COLUMN wrong_explanation_2,
    DROP COLUMN wrong_explanation_3,
    DROP COLUMN wrong_explanation_4;
