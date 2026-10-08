package com.pbanakar.huntlog.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbanakar.huntlog.dto.request.RegisterRequest;
import com.pbanakar.huntlog.dto.request.UpdatePreferencesRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserPreferencesIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("huntlog")
            .withUsername("huntlog")
            .withPassword("huntlog");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("app.reminders.enabled", () -> "false"); // disable background scheduler runs during tests
    }

    @MockBean
    private JavaMailSender mailSender;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetToken(String email) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("Pref User");
        req.setEmail(email);
        req.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("token").asText();
    }

    @Test
    @DisplayName("PUT and GET /api/v1/users/me/preferences toggles and persists email reminder settings")
    void testUpdateAndFetchUserPreferences() throws Exception {
        String token = registerAndGetToken("user_pref_" + UUID.randomUUID() + "@example.com");

        // 1. Initial GET -> should be default true
        mockMvc.perform(get("/api/v1/users/me/preferences")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailRemindersEnabled").value(true));

        // 2. PUT preferences -> update to false
        UpdatePreferencesRequest updateReq = new UpdatePreferencesRequest(false);
        mockMvc.perform(put("/api/v1/users/me/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailRemindersEnabled").value(false));

        // 3. GET preferences -> verify persisted as false
        mockMvc.perform(get("/api/v1/users/me/preferences")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailRemindersEnabled").value(false));

        // 4. PUT preferences -> update back to true
        updateReq = new UpdatePreferencesRequest(true);
        mockMvc.perform(put("/api/v1/users/me/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailRemindersEnabled").value(true));
    }

    @Test
    @DisplayName("Accessing /api/v1/users/me/preferences without token returns 401 Unauthorized")
    void testUnauthenticatedPreferencesAccessFails() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/preferences"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/users/me/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailRemindersEnabled\":false}"))
                .andExpect(status().isUnauthorized());
    }
}
