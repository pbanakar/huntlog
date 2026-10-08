package com.pbanakar.huntlog.dto.response;

public class UserPreferencesResponse {

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
