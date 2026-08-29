package com.memoq.backend;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.memoq.backend.ai.CategorySuggestion;
import com.memoq.backend.dto.LoginRequest;
import com.memoq.backend.dto.QuestionOptionRequest;
import com.memoq.backend.dto.QuestionRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class QuestionApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Real category assignment calls Claude; stub it so the test doesn't need a live API key.
    @MockitoBean
    private ChatClient chatClient;

    @BeforeEach
    void stubCategoryInference() {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(eq(CategorySuggestion.class))).thenReturn(new CategorySuggestion("Networking"));
        when(callResponseSpec.entity(any(ParameterizedTypeReference.class)))
                .thenAnswer(invocation -> List.of(
                        "", "wrong 1", "wrong 2", "wrong 3", "wrong 4", "wrong 5", "wrong 6", "wrong 7"));
    }

    @Test
    void editorEndpointsRejectAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/v1/editor/questions")).andExpect(status().isUnauthorized());
    }

    @Test
    void fullEditorCrudFlowPersistsAcrossRequests() throws Exception {
        String token = login();

        QuestionRequest createRequest = new QuestionRequest(
                "What does HTTP stand for?",
                List.of(
                        new QuestionOptionRequest("HyperText Transfer Protocol", true),
                        new QuestionOptionRequest("High Transfer Text Protocol", false),
                        new QuestionOptionRequest("Host Transfer Protocol", false),
                        new QuestionOptionRequest("Hyperlink Text Protocol", false)),
                "HTTP is the foundational protocol of the web.");
        String createResponse = mockMvc.perform(post("/api/v1/editor/questions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.options", hasSize(4)))
                .andExpect(jsonPath("$.options[0].correct").value(true))
                .andExpect(jsonPath("$.categoryName").value("Networking"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID questionId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asText());
        UUID categoryId = UUID.fromString(objectMapper.readTree(createResponse).get("categoryId").asText());

        mockMvc.perform(get("/api/v1/editor/questions/" + questionId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionText").value("What does HTTP stand for?"));

        mockMvc.perform(get("/api/v1/editor/questions")
                        .header("Authorization", "Bearer " + token)
                        .param("categoryId", categoryId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        QuestionRequest updateRequest = new QuestionRequest(
                "What does HTTP stand for?",
                List.of(
                        new QuestionOptionRequest("HyperText Transfer Protocol", true),
                        new QuestionOptionRequest("High Transfer Text Protocol", false),
                        new QuestionOptionRequest("Host Transfer Protocol", false),
                        new QuestionOptionRequest("Hyperlink Text Protocol", false)),
                "Updated explanation.");
        mockMvc.perform(put("/api/v1/editor/questions/" + questionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.explanation").value("Updated explanation."));

        mockMvc.perform(delete("/api/v1/editor/questions/" + questionId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/editor/questions/" + questionId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void createQuestionSupportsMoreThanFourOptionsWithMultipleCorrectAnswers() throws Exception {
        String token = login();

        QuestionRequest createRequest = new QuestionRequest(
                "Which of these are AWS compute services? (Select TWO.)",
                List.of(
                        new QuestionOptionRequest("Amazon EC2", true),
                        new QuestionOptionRequest("AWS Lambda", true),
                        new QuestionOptionRequest("Amazon S3", false),
                        new QuestionOptionRequest("Amazon RDS", false),
                        new QuestionOptionRequest("Amazon Route 53", false),
                        new QuestionOptionRequest("Amazon CloudFront", false)),
                "EC2 and Lambda are compute services; the others are storage, database, DNS, and CDN.");

        String createResponse = mockMvc.perform(post("/api/v1/editor/questions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.options", hasSize(6)))
                .andExpect(jsonPath("$.options[0].correct").value(true))
                .andExpect(jsonPath("$.options[1].correct").value(true))
                .andExpect(jsonPath("$.options[2].correct").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID questionId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asText());
        mockMvc.perform(get("/api/v1/editor/questions/" + questionId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options", hasSize(6)));
    }

    @Test
    void createQuestionRejectsRequestWithNoCorrectOption() throws Exception {
        String token = login();

        QuestionRequest createRequest = new QuestionRequest(
                "What does HTTP stand for?",
                List.of(
                        new QuestionOptionRequest("HyperText Transfer Protocol", false),
                        new QuestionOptionRequest("High Transfer Text Protocol", false)),
                "HTTP is the foundational protocol of the web.");

        mockMvc.perform(post("/api/v1/editor/questions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
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
