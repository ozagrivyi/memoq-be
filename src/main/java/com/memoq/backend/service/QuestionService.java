package com.memoq.backend.service;

import com.memoq.backend.ai.AnswerRationaleService;
import com.memoq.backend.ai.CategoryInferenceService;
import com.memoq.backend.config.CacheConfig;
import com.memoq.backend.dto.DtoMapper;
import com.memoq.backend.dto.QuestionDto;
import com.memoq.backend.dto.QuestionOptionRequest;
import com.memoq.backend.dto.QuestionPage;
import com.memoq.backend.dto.QuestionRequest;
import com.memoq.backend.entity.Category;
import com.memoq.backend.entity.Question;
import com.memoq.backend.entity.QuestionOption;
import com.memoq.backend.exception.InvalidRequestException;
import com.memoq.backend.exception.ResourceNotFoundException;
import com.memoq.backend.repository.QuestionRepository;
import com.memoq.backend.repository.QuestionSpecifications;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final CategoryInferenceService categoryInferenceService;
    private final AnswerRationaleService answerRationaleService;

    @Cacheable(
            cacheNames = CacheConfig.QUESTION_PAGES_CACHE,
            key = "(#categoryId != null ? #categoryId : 'any') + '|' + (#search != null ? #search : '')"
                    + " + '|' + #pageable.pageNumber + '|' + #pageable.pageSize + '|' + #pageable.sort")
    public QuestionPage getPage(UUID categoryId, String search, Pageable pageable) {
        Page<QuestionDto> page = questionRepository
                .findAll(QuestionSpecifications.filter(categoryId, search), pageable)
                .map(DtoMapper::toDto);
        return new QuestionPage(page.getContent(), page.getTotalElements());
    }

    @Cacheable(cacheNames = CacheConfig.QUESTIONS_CACHE, key = "#id")
    public QuestionDto getById(UUID id) {
        return DtoMapper.toDto(findQuestionOrThrow(id));
    }

    @Transactional
    @CacheEvict(cacheNames = CacheConfig.QUESTION_PAGES_CACHE, allEntries = true)
    public QuestionDto create(QuestionRequest request) {
        Question question = new Question();
        applyRequest(question, request);
        // flush so @CreationTimestamp/@UpdateTimestamp (VM-generated at flush time, and with a
        // UUID id there's no forced early flush like there would be with an IDENTITY id) are
        // populated on the instance before it's mapped to the response DTO.
        return DtoMapper.toDto(questionRepository.saveAndFlush(question));
    }

    @Transactional
    @Caching(
            put = @CachePut(cacheNames = CacheConfig.QUESTIONS_CACHE, key = "#id"),
            evict = @CacheEvict(cacheNames = CacheConfig.QUESTION_PAGES_CACHE, allEntries = true))
    public QuestionDto update(UUID id, QuestionRequest request) {
        Question question = findQuestionOrThrow(id);
        applyRequest(question, request);
        // flush so @CreationTimestamp/@UpdateTimestamp (VM-generated at flush time, and with a
        // UUID id there's no forced early flush like there would be with an IDENTITY id) are
        // populated on the instance before it's mapped to the response DTO.
        return DtoMapper.toDto(questionRepository.saveAndFlush(question));
    }

    @Transactional
    @Caching(
            evict = {
                @CacheEvict(cacheNames = CacheConfig.QUESTIONS_CACHE, key = "#id"),
                @CacheEvict(cacheNames = CacheConfig.QUESTION_PAGES_CACHE, allEntries = true)
            })
    public void delete(UUID id) {
        if (!questionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Question " + id + " not found.");
        }
        questionRepository.deleteById(id);
    }

    /**
     * Records one play result against a question's running stats. Deliberately bypasses
     * {@link #update} / {@link #applyRequest} — this fires once per answer during gameplay and
     * must not re-run category/rationale AI generation on every question.
     */
    @Transactional
    @Caching(
            put = @CachePut(cacheNames = CacheConfig.QUESTIONS_CACHE, key = "#id"),
            evict = @CacheEvict(cacheNames = CacheConfig.QUESTION_PAGES_CACHE, allEntries = true))
    public QuestionDto recordAnswer(UUID id, boolean correct) {
        Question question = findQuestionOrThrow(id);
        if (correct) {
            question.setCorrectCount(question.getCorrectCount() + 1);
        } else {
            question.setIncorrectCount(question.getIncorrectCount() + 1);
        }
        return DtoMapper.toDto(questionRepository.saveAndFlush(question));
    }

    @Transactional
    @Caching(
            put = @CachePut(cacheNames = CacheConfig.QUESTIONS_CACHE, key = "#id"),
            evict = @CacheEvict(cacheNames = CacheConfig.QUESTION_PAGES_CACHE, allEntries = true))
    public QuestionDto resetStats(UUID id) {
        Question question = findQuestionOrThrow(id);
        question.setCorrectCount(0);
        question.setIncorrectCount(0);
        return DtoMapper.toDto(questionRepository.saveAndFlush(question));
    }

    private Question findQuestionOrThrow(UUID id) {
        return questionRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question " + id + " not found."));
    }

    private void applyRequest(Question question, QuestionRequest request) {
        List<QuestionOptionRequest> options = request.options();
        if (options.stream().noneMatch(QuestionOptionRequest::correct)) {
            throw new InvalidRequestException("At least one option must be marked correct.");
        }

        Category category = categoryInferenceService.resolveCategory(request);
        question.setCategory(category);
        question.setQuestionText(request.questionText());
        question.setExplanation(request.explanation());

        List<String> rationales = answerRationaleService.generateRationales(request);

        question.getOptions().clear();
        for (int i = 0; i < options.size(); i++) {
            QuestionOptionRequest source = options.get(i);
            QuestionOption option = new QuestionOption();
            option.setQuestion(question);
            option.setPosition(i + 1);
            option.setOptionText(source.text());
            option.setCorrect(source.correct());
            option.setWrongExplanation(i < rationales.size() ? rationales.get(i) : null);
            question.getOptions().add(option);
        }
    }
}
