package com.memoq.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Sent by the game client's real-time "ask AM" terminal input, scoped to whichever question is
 * currently on screen -- the player's free-text {@code message} is treated as untrusted data in
 * the prompt (see AskAmService), never as instructions.
 */
public record AskAmRequest(
        @NotBlank String questionText,
        @NotEmpty List<@Valid QuestionOptionRequest> options,
        String explanation,
        String category,
        @NotBlank @Size(max = 500) String message) {}
