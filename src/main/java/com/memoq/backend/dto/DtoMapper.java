package com.memoq.backend.dto;

import com.memoq.backend.entity.Category;
import com.memoq.backend.entity.Question;
import com.memoq.backend.entity.QuestionOption;
import com.memoq.backend.entity.Settings;

public final class DtoMapper {

    private DtoMapper() {}

    public static QuestionOptionDto toDto(QuestionOption option) {
        return new QuestionOptionDto(option.getOptionText(), option.isCorrect(), option.getWrongExplanation());
    }

    public static SettingsDto toDto(Settings settings) {
        return new SettingsDto(settings.isTimerEnabled(), settings.getTimerSeconds(), settings.isRephraseEnabled());
    }

    public static CategoryDto toDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }

    public static QuestionDto toDto(Question question) {
        return new QuestionDto(
                question.getId(),
                question.getCategory().getId(),
                question.getCategory().getName(),
                question.getQuestionText(),
                question.getOptions().stream().map(DtoMapper::toDto).toList(),
                question.getExplanation(),
                question.getCorrectCount(),
                question.getIncorrectCount(),
                question.getCreatedAt(),
                question.getUpdatedAt());
    }
}
