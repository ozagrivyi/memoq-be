package com.memoq.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * No categoryId — CategoryInferenceService assigns the category via Claude instead. 2-8 options;
 * at least one must be marked correct, which QuestionService checks since Bean Validation can't
 * express a cross-element "at least one of these booleans is true" constraint.
 */
public record QuestionRequest(
        @NotBlank String questionText,
        @NotEmpty @Size(min = 2, max = 8) List<@Valid QuestionOptionRequest> options,
        @NotBlank String explanation) {}
