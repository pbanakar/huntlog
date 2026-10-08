package com.pbanakar.huntlog.dto.request;

import jakarta.validation.constraints.NotNull;

public class UpdatePreferencesRequest {

    @NotNull(message = "emailRemindersEnabled is required")
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
