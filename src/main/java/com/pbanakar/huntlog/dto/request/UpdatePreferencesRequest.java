package com.pbanakar.huntlog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body to update user notification and sync preferences")
public class UpdatePreferencesRequest {

    @Schema(description = "Whether to receive email reminders for stale pending applications", example = "true")
    private Boolean emailRemindersEnabled;

    @Schema(description = "Whether to automatically sync applications from email", example = "true")
    private Boolean emailSyncEnabled;

    @Size(max = 150)
    @Schema(description = "IMAP server hostname", example = "imap.gmail.com")
    private String imapHost;

    @Schema(description = "IMAP server port", example = "993")
    private Integer imapPort;

    @Size(max = 150)
    @Schema(description = "IMAP username / email address", example = "user@gmail.com")
    private String imapUsername;

    @Size(max = 255)
    @Schema(description = "IMAP password or Google App Password (leave blank to keep existing password)", example = "abcd efgh ijkl mnop")
    private String imapPassword;

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

    public Boolean getEmailSyncEnabled() {
        return emailSyncEnabled;
    }

    public void setEmailSyncEnabled(Boolean emailSyncEnabled) {
        this.emailSyncEnabled = emailSyncEnabled;
    }

    public String getImapHost() {
        return imapHost;
    }

    public void setImapHost(String imapHost) {
        this.imapHost = imapHost;
    }

    public Integer getImapPort() {
        return imapPort;
    }

    public void setImapPort(Integer imapPort) {
        this.imapPort = imapPort;
    }

    public String getImapUsername() {
        return imapUsername;
    }

    public void setImapUsername(String imapUsername) {
        this.imapUsername = imapUsername;
    }

    public String getImapPassword() {
        return imapPassword;
    }

    public void setImapPassword(String imapPassword) {
        this.imapPassword = imapPassword;
    }
}
