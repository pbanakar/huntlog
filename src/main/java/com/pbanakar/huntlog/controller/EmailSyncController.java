package com.pbanakar.huntlog.controller;

import com.pbanakar.huntlog.dto.response.UserPreferencesResponse;
import com.pbanakar.huntlog.dto.sync.EmailSyncSummary;
import com.pbanakar.huntlog.security.SecurityUtils;
import com.pbanakar.huntlog.service.EmailSyncService;
import com.pbanakar.huntlog.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sync")
@Tag(name = "Email Sync", description = "Endpoints for managing automated email ingestion and manual sync triggers")
public class EmailSyncController {

    private final EmailSyncService emailSyncService;
    private final UserService userService;

    public EmailSyncController(EmailSyncService emailSyncService, UserService userService) {
        this.emailSyncService = emailSyncService;
        this.userService = userService;
    }

    @PostMapping("/emails")
    @Operation(summary = "Sync applications from mailbox",
            description = "Triggers an on-demand scan of the authenticated user's mailbox via IMAP to auto-import or update application statuses.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponse(responseCode = "200", description = "Email sync completed successfully")
    @ApiResponse(responseCode = "400", description = "IMAP credentials not configured")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<EmailSyncSummary> syncEmails() {
        Long userId = SecurityUtils.getCurrentUserId();
        EmailSyncSummary summary = emailSyncService.syncEmailsForUser(userId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/status")
    @Operation(summary = "Get email sync configuration status",
            description = "Retrieves current email sync status and last synchronization timestamp for the authenticated user.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponse(responseCode = "200", description = "Email sync status retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<UserPreferencesResponse> getSyncStatus() {
        return ResponseEntity.ok(userService.getPreferences());
    }
}

