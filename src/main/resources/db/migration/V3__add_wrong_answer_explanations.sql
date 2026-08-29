-- AI-generated "why this option is wrong" text per option slot, used by the frontend's
-- card-matching mini-game. Nullable: only the 3 wrong-option slots are ever populated; existing
-- rows (created before this feature) start out NULL until edited/regenerated.
ALTER TABLE questions
    ADD COLUMN wrong_explanation_1 TEXT,
    ADD COLUMN wrong_explanation_2 TEXT,
    ADD COLUMN wrong_explanation_3 TEXT,
    ADD COLUMN wrong_explanation_4 TEXT;
