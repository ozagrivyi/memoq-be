package com.memoq.backend.dto;

import jakarta.validation.constraints.NotBlank;

/** event is one of AmTauntService's known keys (IDLE, CORRECT, CRIT, INCORRECT, DEBUFF, HINT, VICTORY, DEFEAT). */
public record TauntRequest(@NotBlank String event, String category) {}
