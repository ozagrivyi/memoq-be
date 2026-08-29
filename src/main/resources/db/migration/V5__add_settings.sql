CREATE TABLE settings (
    id                UUID PRIMARY KEY,
    timer_enabled     BOOLEAN NOT NULL DEFAULT true,
    timer_seconds     INTEGER NOT NULL DEFAULT 120,
    rephrase_enabled  BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT chk_settings_timer_seconds CHECK (timer_seconds BETWEEN 10 AND 600)
);

-- Single-admin tool (see CLAUDE.md) — exactly one global settings row, addressed by a fixed id
-- (Settings.SINGLETON_ID) rather than "first row found", so SettingsService can look it up
-- deterministically without an ORDER BY ... LIMIT 1 query.
INSERT INTO settings (id, timer_enabled, timer_seconds, rephrase_enabled)
VALUES ('00000000-0000-0000-0000-000000000001', true, 120, false);
