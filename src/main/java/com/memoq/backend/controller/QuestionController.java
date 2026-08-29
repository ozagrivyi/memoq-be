package com.memoq.backend.controller;

import com.memoq.backend.dto.AnswerResultRequest;
import com.memoq.backend.dto.QuestionDto;
import com.memoq.backend.dto.QuestionPage;
import com.memoq.backend.dto.QuestionRequest;
import com.memoq.backend.service.QuestionService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/editor/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    public PagedModel<QuestionDto> getPage(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "questionText") Pageable pageable) {
        QuestionPage result = questionService.getPage(categoryId, search, pageable);
        Page<QuestionDto> page = new PageImpl<>(result.content(), pageable, result.totalElements());
        return new PagedModel<>(page);
    }

    @GetMapping("/{id}")
    public QuestionDto getById(@PathVariable UUID id) {
        return questionService.getById(id);
    }

    @PostMapping
    public ResponseEntity<QuestionDto> create(@Valid @RequestBody QuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionService.create(request));
    }

    @PutMapping("/{id}")
    public QuestionDto update(@PathVariable UUID id, @Valid @RequestBody QuestionRequest request) {
        return questionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        questionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/answer")
    public QuestionDto recordAnswer(@PathVariable UUID id, @Valid @RequestBody AnswerResultRequest request) {
        return questionService.recordAnswer(id, request.correct());
    }

    @PostMapping("/{id}/reset-stats")
    public QuestionDto resetStats(@PathVariable UUID id) {
        return questionService.resetStats(id);
    }
}
