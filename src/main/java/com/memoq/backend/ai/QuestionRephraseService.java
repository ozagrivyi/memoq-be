package com.memoq.backend.ai;

import com.memoq.backend.dto.QuestionOptionRequest;
import com.memoq.backend.dto.RephraseQuestionRequest;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Rewords a question's text and all of its options via Claude, generated fresh on demand each
 * time a question is shown — only invoked when the player's "Rephrase questions" setting is on.
 * The stored DB text is never modified; this is display-only, same as {@link ExplanationSimplifierService}.
 */
@Service
@RequiredArgsConstructor
public class QuestionRephraseService {

    private final ChatClient chatClient;

    public RephrasedQuestion rephrase(RephraseQuestionRequest request) {
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
                Reword this multiple-choice quiz question for an IT/DevOps/networking certification-prep
                game. Keep the exact same meaning and the exact same correct answer(s) — you are only
                changing the wording, not the facts being tested.

                Question: %s
                %s
                The correct option(s): %s

                Rewrite the question text and all %d options using different phrasing than the
                original, while keeping each option in its same numbered slot and preserving which
                one(s) are correct. Keep the options a similar length to the originals so none of
                them stands out as obviously right or wrong just from length. Return exactly %d
                reworded options, in the same order.
                """
                        .formatted(
                                request.questionText(),
                                numberedOptions,
                                correctPositions,
                                options.size(),
                                options.size());

        return chatClient.prompt().user(prompt).call().entity(RephrasedQuestion.class);
    }
}
