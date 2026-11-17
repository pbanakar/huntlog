package com.pbanakar.huntlog.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbanakar.huntlog.dto.request.RegisterRequest;
import com.pbanakar.huntlog.dto.request.UpdatePreferencesRequest;
import com.pbanakar.huntlog.dto.sync.RawEmailMessage;
import com.pbanakar.huntlog.service.ImapClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EmailSyncIT {

    @MockBean
    private JavaMailSender mailSender;

    @MockBean
    private ImapClient imapClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetToken(String email) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("Sync Candidate");
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
    @DisplayName("Unauthenticated request to /api/v1/sync/emails returns 401 Unauthorized")
    void testUnauthenticatedSyncFails() throws Exception {
        mockMvc.perform(post("/api/v1/sync/emails"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated request to /api/v1/sync/status returns 401 Unauthorized")
    void testUnauthenticatedStatusFails() throws Exception {
        mockMvc.perform(get("/api/v1/sync/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Sync without configured IMAP credentials returns 400 Bad Request with helpful error")
    void testSyncWithoutCredentialsReturns400() throws Exception {
        String token = registerAndGetToken("user_nocreds_" + UUID.randomUUID() + "@example.com");

        mockMvc.perform(post("/api/v1/sync/emails")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("credentials are not configured")));
    }

    @Test
    @DisplayName("Sync with configured credentials succeeds and returns EmailSyncSummary")
    void testSyncWithCredentialsSucceeds() throws Exception {
        String token = registerAndGetToken("user_withcreds_" + UUID.randomUUID() + "@example.com");

        // 1. Configure IMAP preferences
        UpdatePreferencesRequest prefReq = new UpdatePreferencesRequest();
        prefReq.setEmailSyncEnabled(true);
        prefReq.setImapHost("imap.gmail.com");
        prefReq.setImapPort(993);
        prefReq.setImapUsername("candidate@example.com");
        prefReq.setImapPassword("app-password-secret");

        mockMvc.perform(put("/api/v1/users/me/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prefReq)))
                .andExpect(status().isOk());

        // 2. Mock IMAP client returning one application email
        RawEmailMessage msg = new RawEmailMessage(
                "msg-101",
                "BeBetta Hiring team <no-reply@kekamail.com>",
                "Application for Software Engineering Intern received",
                "Thank you for applying to BeBetta for the role of Software Engineering Intern.",
                LocalDate.now()
        );
        when(imapClient.fetchMessages(anyString(), anyInt(), anyString(), anyString(), any(), anyInt()))
                .thenReturn(List.of(msg));

        // 3. Trigger Sync
        mockMvc.perform(post("/api/v1/sync/emails")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scannedCount").value(1))
                .andExpect(jsonPath("$.createdCount").value(1))
                .andExpect(jsonPath("$.updatedCount").value(0))
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("BeBetta")));

        // 4. Verify status endpoint reflects updated sync
        mockMvc.perform(get("/api/v1/sync/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailSyncEnabled").value(true))
                .andExpect(jsonPath("$.hasImapPassword").value(true))
                .andExpect(jsonPath("$.lastEmailSyncedAt").isNotEmpty());
    }
}

