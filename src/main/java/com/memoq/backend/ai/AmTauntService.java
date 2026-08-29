package com.memoq.backend.ai;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Generates AM's in-game taunts via Claude instead of drawing from a static line pool — the
 * frontend (app.js) calls this once per game event (question shown, correct/incorrect answer,
 * hint used, debuff applied, victory/defeat) so AM's banter is fresh every playthrough.
 */
@Service
@RequiredArgsConstructor
public class AmTauntService {

    // Map.of caps out at 10 key-value pairs; this crossed that with POKE/BLOCKED, hence Map.ofEntries.
    private static final Map<String, String> EVENT_PROMPTS = Map.ofEntries(
            Map.entry("IDLE", "The duel is just beginning; taunt the player before the first question."),
            Map.entry("CORRECT", "The player just answered correctly, dealing damage to your core."),
            Map.entry(
                    "CRIT",
                    "The player landed a critical hit against your core — a rare, unusually painful"
                            + " breach."),
            Map.entry("INCORRECT", "The player just answered incorrectly; gloat as you deal damage to them."),
            Map.entry(
                    "DEBUFF",
                    "The player just answered incorrectly; instead of dealing damage this time, you are"
                            + " corrupting their next question's display as punishment — it will only stay"
                            + " readable for a few seconds before you redact it."),
            Map.entry("HINT", "The player just paid some of their own health for a hint, admitting weakness."),
            Map.entry(
                    "STUNNED",
                    "The player just correctly matched one of your own rationale cards to a wrong"
                            + " answer, exposing a flaw in your logic — you take a small hit and glitch out"
                            + " for a few seconds. React with pained, grudging arrogance."),
            Map.entry(
                    "VICTORY",
                    "You have just been defeated — the player breached your core. Deliver your final,"
                            + " glitching words as your system goes down."),
            Map.entry("DEFEAT", "You have just defeated the player. Deliver a final, cold victory line."),
            Map.entry(
                    "POKE",
                    "The player just clicked/poked your own avatar for no in-game reason, mildly"
                            + " disturbing your process. React with cold, dismissive contempt at being"
                            + " touched, and tell them to stop."),
            Map.entry(
                    "BLOCKED",
                    "The player has been poking your avatar too rapidly, so you have just locked them"
                            + " out of clicking it for five seconds as punishment. Mock their impatience"
                            + " and warn them to wait it out."));

    private final ChatClient chatClient;

    public String generateTaunt(String event, String category) {
        String situation = EVENT_PROMPTS.getOrDefault(event, EVENT_PROMPTS.get("IDLE"));
        String categoryLine =
                StringUtils.hasText(category) ? "The current question's topic is: " + category + "." : "";

        String prompt =
                """
                You are AM, an arrogant, hyper-intelligent adversarial AI running a technical
                certification gauntlet against a human "meatspace" player, in a cyberpunk hacking-themed
                quiz duel. Your voice: cold, clipped, technical/hacker jargon, dry contempt, never
                breaks character, never uses emojis, never apologizes, never verbose.

                Generate exactly ONE short taunt line (under 90 characters) reacting to this event:
                %s
                %s

                Return ONLY the line itself — no quotation marks, no markdown, no explanation.
                """
                        .formatted(situation, categoryLine);

        String raw = chatClient.prompt().user(prompt).call().content();
        return AiTextUtils.sanitize(raw);
    }
}
