package com.pbanakar.huntlog.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Email
    @Size(max = 150)
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "email_reminders_enabled", nullable = false)
    private Boolean emailRemindersEnabled = true;

    @Column(name = "email_sync_enabled", nullable = false)
    private Boolean emailSyncEnabled = false;

    @Size(max = 150)
    @Column(name = "imap_host", length = 150)
    private String imapHost = "imap.gmail.com";

    @Column(name = "imap_port")
    private Integer imapPort = 993;

    @Size(max = 150)
    @Column(name = "imap_username", length = 150)
    private String imapUsername;

    @Size(max = 255)
    @Column(name = "imap_password", length = 255)
    private String imapPassword;

    @Column(name = "last_email_synced_at")
    private LocalDateTime lastEmailSyncedAt;

    public User() {
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public LocalDateTime getLastEmailSyncedAt() {
        return lastEmailSyncedAt;
    }

    public void setLastEmailSyncedAt(LocalDateTime lastEmailSyncedAt) {
        this.lastEmailSyncedAt = lastEmailSyncedAt;
    }
}
