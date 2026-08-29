package com.memoq.backend;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.memoq.backend.ai.RephrasedQuestion;
import com.memoq.backend.dto.AskAmRequest;
import com.memoq.backend.dto.LoginRequest;
import com.memoq.backend.dto.QuestionOptionRequest;
import com.memoq.backend.dto.RephraseQuestionRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class GameApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatClient chatClient;

    @BeforeEach
    void stubRephrase() {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(eq(RephrasedQuestion.class)))
                .thenReturn(new RephrasedQuestion(
                        "Which protocol underlies most web traffic?",
                        List.of(
                                "HyperText Transfer Protocol",
                                "High Transfer Text Protocol",
                                "Host Transfer Protocol",
                                "Hyperlink Text Protocol")));
        when(callResponseSpec.content()).thenReturn("Focus, meatspace unit. HTTP is the answer.");
    }

    @Test
    void gameEndpointsRejectAnonymousRequests() throws Exception {
        RephraseQuestionRequest request = new RephraseQuestionRequest(
                "What does HTTP stand for?",
                List.of(
                        new QuestionOptionRequest("HyperText Transfer Protocol", true),
                        new QuestionOptionRequest("High Transfer Text Protocol", false),
                        new QuestionOptionRequest("Host Transfer Protocol", false),
                        new QuestionOptionRequest("Hyperlink Text Protocol", false)));
        mockMvc.perform(post("/api/v1/game/rephrase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rephraseReturnsReworkedQuestionAndOptions() throws Exception {
        String token = login();

        RephraseQuestionRequest request = new RephraseQuestionRequest(
                "What does HTTP stand for?",
                List.of(
                        new QuestionOptionRequest("HyperText Transfer Protocol", true),
                        new QuestionOptionRequest("High Transfer Text Protocol", false),
                        new QuestionOptionRequest("Host Transfer Protocol", false),
                        new QuestionOptionRequest("Hyperlink Text Protocol", false)));

        mockMvc.perform(post("/api/v1/game/rephrase")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionText").value("Which protocol underlies most web traffic?"))
                .andExpect(jsonPath("$.options[0]").value("HyperText Transfer Protocol"));
    }

    @Test
    void askRejectsAnonymousRequests() throws Exception {
        AskAmRequest request = new AskAmRequest(
                "What does HTTP stand for?",
                List.of(
                        new QuestionOptionRequest("HyperText Transfer Protocol", true),
                        new QuestionOptionRequest("High Transfer Text Protocol", false)),
                "HTTP is the protocol underlying web traffic.",
                "Networking",
                "Why is the first option correct?");
        mockMvc.perform(post("/api/v1/game/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void askReturnsAmsInCharacterReply() throws Exception {
        String token = login();

        AskAmRequest request = new AskAmRequest(
                "What does HTTP stand for?",
                List.of(
                        new QuestionOptionRequest("HyperText Transfer Protocol", true),
                        new QuestionOptionRequest("High Transfer Text Protocol", false)),
                "HTTP is the protocol underlying web traffic.",
                "Networking",
                "Why is the first option correct?");

        mockMvc.perform(post("/api/v1/game/ask")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Focus, meatspace unit. HTTP is the answer."));
    }

    private String login() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest("admin", "changeit"));
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
