package com.memoq.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Sent by the game client on demand, per question, when the player's "Rephrase questions" setting is on. */
public record RephraseQuestionRequest(
        @NotBlank String questionText,
        @NotEmpty @Size(min = 2, max = 8) List<@Valid QuestionOptionRequest> options) {}
