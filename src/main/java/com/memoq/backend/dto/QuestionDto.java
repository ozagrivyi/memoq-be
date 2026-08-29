package com.memoq.backend.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuestionDto(
        UUID id,
        UUID categoryId,
        String categoryName,
        String questionText,
        List<QuestionOptionDto> options,
        String explanation,
        int correctCount,
        int incorrectCount,
        Instant createdAt,
        Instant updatedAt) {}
