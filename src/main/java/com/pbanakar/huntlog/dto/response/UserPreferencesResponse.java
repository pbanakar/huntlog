package com.pbanakar.huntlog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "User notification and sync preferences")
public class UserPreferencesResponse {

    @Schema(description = "Whether email reminders are enabled", example = "true")
    private Boolean emailRemindersEnabled;

    @Schema(description = "Whether email auto-sync is enabled", example = "true")
    private Boolean emailSyncEnabled;

    @Schema(description = "IMAP server hostname", example = "imap.gmail.com")
    private String imapHost;

    @Schema(description = "IMAP server port", example = "993")
    private Integer imapPort;

    @Schema(description = "IMAP username / email address", example = "user@gmail.com")
    private String imapUsername;

    @Schema(description = "Whether an IMAP password or App Password is saved", example = "true")
    private Boolean hasImapPassword;

    @Schema(description = "Timestamp of the last successful email sync")
    private LocalDateTime lastEmailSyncedAt;

    public UserPreferencesResponse() {
    }

    public UserPreferencesResponse(Boolean emailRemindersEnabled) {
        this.emailRemindersEnabled = emailRemindersEnabled;
    }

    public UserPreferencesResponse(Boolean emailRemindersEnabled, Boolean emailSyncEnabled,
                                  String imapHost, Integer imapPort, String imapUsername,
                                  Boolean hasImapPassword, LocalDateTime lastEmailSyncedAt) {
        this.emailRemindersEnabled = emailRemindersEnabled;
        this.emailSyncEnabled = emailSyncEnabled;
        this.imapHost = imapHost;
        this.imapPort = imapPort;
        this.imapUsername = imapUsername;
        this.hasImapPassword = hasImapPassword;
        this.lastEmailSyncedAt = lastEmailSyncedAt;
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

    public Boolean getHasImapPassword() {
        return hasImapPassword;
    }

    public void setHasImapPassword(Boolean hasImapPassword) {
        this.hasImapPassword = hasImapPassword;
    }

    public LocalDateTime getLastEmailSyncedAt() {
        return lastEmailSyncedAt;
    }

    public void setLastEmailSyncedAt(LocalDateTime lastEmailSyncedAt) {
        this.lastEmailSyncedAt = lastEmailSyncedAt;
    }
}
