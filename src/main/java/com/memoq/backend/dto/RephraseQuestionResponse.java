package com.memoq.backend.dto;

import java.util.List;

public record RephraseQuestionResponse(String questionText, List<String> options) {}
