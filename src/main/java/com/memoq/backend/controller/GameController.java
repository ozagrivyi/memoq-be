package com.memoq.backend.controller;

import com.memoq.backend.ai.AmTauntService;
import com.memoq.backend.ai.AskAmService;
import com.memoq.backend.ai.ExplanationSimplifierService;
import com.memoq.backend.ai.QuestionRephraseService;
import com.memoq.backend.ai.RephrasedQuestion;
import com.memoq.backend.dto.AskAmRequest;
import com.memoq.backend.dto.AskAmResponse;
import com.memoq.backend.dto.RephraseQuestionRequest;
import com.memoq.backend.dto.RephraseQuestionResponse;
import com.memoq.backend.dto.SimplifyExplanationRequest;
import com.memoq.backend.dto.SimplifyExplanationResponse;
import com.memoq.backend.dto.TauntRequest;
import com.memoq.backend.dto.TauntResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/game")
@RequiredArgsConstructor
public class GameController {

    private final AmTauntService amTauntService;
    private final AskAmService askAmService;
    private final ExplanationSimplifierService explanationSimplifierService;
    private final QuestionRephraseService questionRephraseService;

    @PostMapping("/taunt")
    public TauntResponse taunt(@Valid @RequestBody TauntRequest request) {
        return new TauntResponse(amTauntService.generateTaunt(request.event(), request.category()));
    }

    @PostMapping("/ask")
    public AskAmResponse ask(@Valid @RequestBody AskAmRequest request) {
        return new AskAmResponse(askAmService.ask(request));
    }

    @PostMapping("/explain")
    public SimplifyExplanationResponse explain(@Valid @RequestBody SimplifyExplanationRequest request) {
        String simplified = explanationSimplifierService.simplify(
                request.questionText(), request.correctAnswerText(), request.explanation());
        return new SimplifyExplanationResponse(simplified);
    }

    @PostMapping("/rephrase")
    public RephraseQuestionResponse rephrase(@Valid @RequestBody RephraseQuestionRequest request) {
        RephrasedQuestion rephrased = questionRephraseService.rephrase(request);
        return new RephraseQuestionResponse(rephrased.questionText(), rephrased.options());
    }
}
