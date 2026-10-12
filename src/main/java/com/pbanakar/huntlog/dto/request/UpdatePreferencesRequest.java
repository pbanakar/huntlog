package com.pbanakar.huntlog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body to update user notification preferences")
public class UpdatePreferencesRequest {

    @NotNull(message = "emailRemindersEnabled is required")
    @Schema(description = "Whether to receive email reminders for stale pending applications", example = "true")
    private Boolean emailRemindersEnabled;

    public UpdatePreferencesRequest() {
    }

    public UpdatePreferencesRequest(Boolean emailRemindersEnabled) {
        this.emailRemindersEnabled = emailRemindersEnabled;
    }

    public Boolean getEmailRemindersEnabled() {
        return emailRemindersEnabled;
    }

    public void setEmailRemindersEnabled(Boolean emailRemindersEnabled) {
        this.emailRemindersEnabled = emailRemindersEnabled;
    }
}
