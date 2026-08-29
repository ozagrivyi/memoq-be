package com.memoq.backend.ai;

/** Shared cleanup for raw Claude text completions (as opposed to structured/.entity() output). */
final class AiTextUtils {

    private AiTextUtils() {}

    static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.strip();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).strip();
        }
        return trimmed;
    }
}
