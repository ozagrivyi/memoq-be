package com.memoq.backend.ai;

/** Structured output from {@link CategoryInferenceService}: the category name Claude picked. */
public record CategorySuggestion(String categoryName) {}
