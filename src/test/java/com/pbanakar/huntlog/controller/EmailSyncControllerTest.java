package com.pbanakar.huntlog.controller;

import com.pbanakar.huntlog.dto.response.UserPreferencesResponse;
import com.pbanakar.huntlog.dto.sync.EmailSyncSummary;
import com.pbanakar.huntlog.exception.GlobalExceptionHandler;
import com.pbanakar.huntlog.service.EmailSyncService;
import com.pbanakar.huntlog.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EmailSyncControllerTest {

    @Mock
    private EmailSyncService emailSyncService;

    @Mock
    private UserService userService;

    @InjectMocks
    private EmailSyncController emailSyncController;

    private MockMvc mockMvc;
    private static final Long USER_ID = 55L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(emailSyncController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("user@example.com", null, Collections.emptyList());
        auth.setDetails(USER_ID);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/v1/sync/emails calls emailSyncService and returns 200 with summary")
    void testSyncEmailsEndpoint() throws Exception {
        EmailSyncSummary summary = new EmailSyncSummary(10, 2, 1);
        summary.addDetail("Created: Test Company");
        when(emailSyncService.syncEmailsForUser(USER_ID)).thenReturn(summary);

        mockMvc.perform(post("/api/v1/sync/emails"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scannedCount").value(10))
                .andExpect(jsonPath("$.createdCount").value(2))
                .andExpect(jsonPath("$.updatedCount").value(1))
                .andExpect(jsonPath("$.details[0]").value("Created: Test Company"));

        verify(emailSyncService).syncEmailsForUser(USER_ID);
    }

    @Test
    @DisplayName("POST /api/v1/sync/emails returns 400 when service throws IllegalStateException")
    void testSyncEmailsUnconfiguredCredentials() throws Exception {
        when(emailSyncService.syncEmailsForUser(USER_ID))
                .thenThrow(new IllegalStateException("IMAP email credentials are not configured."));

        mockMvc.perform(post("/api/v1/sync/emails"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("IMAP email credentials are not configured."));
    }

    @Test
    @DisplayName("GET /api/v1/sync/status calls userService.getPreferences and returns 200")
    void testGetSyncStatusEndpoint() throws Exception {
        UserPreferencesResponse prefs = new UserPreferencesResponse();
        prefs.setEmailSyncEnabled(true);
        prefs.setImapHost("imap.gmail.com");
        prefs.setImapUsername("candidate@gmail.com");
        prefs.setHasImapPassword(true);
        when(userService.getPreferences()).thenReturn(prefs);

        mockMvc.perform(get("/api/v1/sync/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailSyncEnabled").value(true))
                .andExpect(jsonPath("$.imapHost").value("imap.gmail.com"))
                .andExpect(jsonPath("$.hasImapPassword").value(true));

        verify(userService).getPreferences();
    }
}

