package com.memoq.backend.dto;

public record SettingsDto(boolean timerEnabled, int timerSeconds, boolean rephraseEnabled) {}
