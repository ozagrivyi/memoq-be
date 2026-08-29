package com.memoq.backend.ai;

import com.memoq.backend.dto.QuestionOptionRequest;
import com.memoq.backend.dto.QuestionRequest;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Scaffolding for future AI-assisted question authoring (PROMPT.md section 1/6): not wired to
 * any controller yet, since the CRUD API in PROMPT.md section 3 doesn't call for one.
 */
@Service
@RequiredArgsConstructor
public class QuestionAiService {

    private final ChatClient chatClient;

    public GeneratedQuestion generateQuestion(String category, String difficulty) {
        String prompt =
                """
                Generate one multiple-choice certification-prep question for the category "%s"
                at difficulty "%s". Provide between 2 and 8 answer options and mark which one(s)
                are correct, plus a short explanation of the correct answer.
                """
                        .formatted(category, difficulty);
        return chatClient.prompt().user(prompt).call().entity(GeneratedQuestion.class);
    }

    public boolean validateQuestion(QuestionRequest question) {
        List<QuestionOptionRequest> options = question.options();
        String numberedOptions = IntStream.range(0, options.size())
                .mapToObj(i -> (i + 1) + ") " + options.get(i).text())
                .collect(Collectors.joining("\n"));
        String correctPositions = IntStream.range(0, options.size())
                .filter(i -> options.get(i).correct())
                .mapToObj(i -> String.valueOf(i + 1))
                .collect(Collectors.joining(", "));

        String prompt =
                """
                Is the following multiple-choice question factually correct, with the marked
                option(s) being unambiguously correct and every other option unambiguously wrong?
                Answer with only "true" or "false".

                Question: %s
                %s
                Marked correct option(s): %s
                Explanation: %s
                """
                        .formatted(question.questionText(), numberedOptions, correctPositions, question.explanation());
        String response = chatClient.prompt().user(prompt).call().content();
        return response != null && response.trim().equalsIgnoreCase("true");
    }
}
