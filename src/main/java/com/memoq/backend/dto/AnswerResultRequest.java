package com.memoq.backend.dto;

import jakarta.validation.constraints.NotNull;

public record AnswerResultRequest(@NotNull Boolean correct) {}
