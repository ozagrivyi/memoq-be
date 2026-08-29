package com.memoq.backend.ai;

import java.util.List;

/** Structured output shape for {@link QuestionRephraseService} — same option count as the request, reworded. */
public record RephrasedQuestion(String questionText, List<String> options) {}
