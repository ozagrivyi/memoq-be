package com.memoq.backend.ai;

import com.memoq.backend.dto.QuestionOptionRequest;
import com.memoq.backend.dto.QuestionRequest;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

/**
 * Generates one short "why this option is wrong" line per incorrect option, via Claude — used by
 * the frontend's card-matching mini-game (see app.js's INTEL CARDS section). Runs once at
 * question create/update time (alongside CategoryInferenceService), not per game session, so the
 * cards are stable across replays.
 */
@Service
@RequiredArgsConstructor
public class AnswerRationaleService {

    private final ChatClient chatClient;

    /** One rationale string per option in {@link QuestionRequest#options()}, in the same order. */
    public List<String> generateRationales(QuestionRequest request) {
        List<QuestionOptionRequest> options = request.options();
        String numberedOptions = IntStream.range(0, options.size())
                .mapToObj(i -> (i + 1) + ") " + options.get(i).text())
                .collect(Collectors.joining("\n"));
        String correctPositions = IntStream.range(0, options.size())
                .filter(i -> options.get(i).correct())
                .mapToObj(i -> String.valueOf(i + 1))
                .collect(Collectors.joining(", "));

        String prompt =
                """
                You are generating short "why this is wrong" explanations for a multiple-choice quiz
                question. They'll be shown on cards a player matches to the wrong answers, so keep
                each one specific enough to identify which option it's about, without repeating the
                option's full text verbatim.

                Question: %s
                %s
                The correct option(s): %s

                Return a JSON array with exactly %d strings, one per option above in the same order.
                For each INCORRECT option, write one short sentence (under 110 characters) explaining
                specifically why that option is wrong. For each CORRECT option's slot, return an
                empty string — don't explain the correct answer(s) here.
                """
                        .formatted(request.questionText(), numberedOptions, correctPositions, options.size());

        return chatClient.prompt().user(prompt).call().entity(new ParameterizedTypeReference<List<String>>() {});
    }
}
