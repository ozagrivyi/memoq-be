package com.memoq.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.memoq.backend.dto.LoginRequest;
import com.memoq.backend.dto.SettingsUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SettingsApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void settingsEndpointsRejectAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/v1/settings")).andExpect(status().isUnauthorized());
    }

    // GET-before-PUT and PUT-then-GET live in one test, in this order, because both read/write
    // the same global singleton row (there's only ever one) — splitting them across independent
    // @Test methods would make the "defaults" assertion order-dependent on whichever test runs
    // first, since JUnit doesn't guarantee method execution order.
    @Test
    void settingsCrudFlowPersistsAcrossRequests() throws Exception {
        String token = login();

        mockMvc.perform(get("/api/v1/settings").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timerEnabled").value(true))
                .andExpect(jsonPath("$.timerSeconds").value(120))
                .andExpect(jsonPath("$.rephraseEnabled").value(false));

        SettingsUpdateRequest update = new SettingsUpdateRequest(false, 90, true);

        mockMvc.perform(put("/api/v1/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timerEnabled").value(false))
                .andExpect(jsonPath("$.timerSeconds").value(90))
                .andExpect(jsonPath("$.rephraseEnabled").value(true));

        mockMvc.perform(get("/api/v1/settings").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timerEnabled").value(false))
                .andExpect(jsonPath("$.timerSeconds").value(90))
                .andExpect(jsonPath("$.rephraseEnabled").value(true));
    }

    @Test
    void updateSettingsRejectsOutOfRangeTimerSeconds() throws Exception {
        String token = login();

        SettingsUpdateRequest update = new SettingsUpdateRequest(true, 5, false);

        mockMvc.perform(put("/api/v1/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
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
