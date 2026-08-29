package com.memoq.backend.ai;

import java.util.List;

/** Structured output shape for {@link QuestionAiService#generateQuestion}. */
public record GeneratedQuestion(String questionText, List<String> options, int correctOption, String explanation) {}
