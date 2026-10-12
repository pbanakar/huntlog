package com.pbanakar.huntlog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User notification preferences")
public class UserPreferencesResponse {

    @Schema(description = "Whether email reminders are enabled", example = "true")
    private Boolean emailRemindersEnabled;

    public UserPreferencesResponse() {
    }

    public UserPreferencesResponse(Boolean emailRemindersEnabled) {
        this.emailRemindersEnabled = emailRemindersEnabled;
    }

    public Boolean getEmailRemindersEnabled() {
        return emailRemindersEnabled;
    }

    public void setEmailRemindersEnabled(Boolean emailRemindersEnabled) {
        this.emailRemindersEnabled = emailRemindersEnabled;
    }
}
