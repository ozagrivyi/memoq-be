package com.memoq.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SettingsUpdateRequest(
        @NotNull Boolean timerEnabled,
        @NotNull @Min(10) @Max(600) Integer timerSeconds,
        @NotNull Boolean rephraseEnabled) {}
