package com.memoq.backend.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Rewrites a question's stored `explanation` (often technical/dense) into a short, plain-language
 * version via Claude, generated on demand when the player clicks the (i) button — the raw DB text
 * is never shown directly in the game UI.
 */
@Service
@RequiredArgsConstructor
public class ExplanationSimplifierService {

    private final ChatClient chatClient;

    public String simplify(String questionText, String correctAnswerText, String explanation) {
        String prompt =
                """
                Explain, in simple everyday language a beginner could follow, why the correct answer
                to this quiz question is correct. Base your explanation on the original explanation
                given, but rewrite it plainly — avoid jargon where you can, keep it to 2-3 short
                sentences, and don't just repeat the original wording.

                Question: %s
                Correct answer: %s
                Original explanation: %s

                Return ONLY the simplified explanation text — no preamble, no markdown, no quotes.
                """
                        .formatted(questionText, correctAnswerText, explanation);

        String raw = chatClient.prompt().user(prompt).call().content();
        return AiTextUtils.sanitize(raw);
    }
}
