package com.memoq.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record SimplifyExplanationRequest(
        @NotBlank String questionText, @NotBlank String correctAnswerText, @NotBlank String explanation) {}
