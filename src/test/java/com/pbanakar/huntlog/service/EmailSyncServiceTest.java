package com.pbanakar.huntlog.service;

import com.pbanakar.huntlog.dto.sync.EmailSyncSummary;
import com.pbanakar.huntlog.dto.sync.ParsedEmailApplication;
import com.pbanakar.huntlog.dto.sync.RawEmailMessage;
import com.pbanakar.huntlog.enums.ApplicationStatus;
import com.pbanakar.huntlog.model.JobApplication;
import com.pbanakar.huntlog.model.User;
import com.pbanakar.huntlog.repository.JobApplicationRepository;
import com.pbanakar.huntlog.repository.UserRepository;
import com.pbanakar.huntlog.statemachine.ApplicationStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailSyncServiceTest {

    @Mock
    private ImapClient imapClient;

    @Mock
    private EmailParserService emailParserService;

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private UserRepository userRepository;

    private ApplicationStateMachine applicationStateMachine;
    private EmailSyncService emailSyncService;

    private User testUser;
    private static final Long USER_ID = 100L;

    @BeforeEach
    void setUp() {
        applicationStateMachine = new ApplicationStateMachine();
        emailSyncService = new EmailSyncService(
                imapClient,
                emailParserService,
                jobApplicationRepository,
                userRepository,
                applicationStateMachine,
                true,
                14,
                "",
                ""
        );

        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setEmail("applicant@example.com");
        testUser.setImapUsername("applicant@example.com");
        testUser.setImapPassword("app-password-123");
        testUser.setImapHost("imap.gmail.com");
        testUser.setImapPort(993);
    }

    @Test
    @DisplayName("syncEmails creates a new application when email is relevant and application does not exist")
    void testSyncEmailsCreatesNewApplication() throws Exception {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        RawEmailMessage rawMsg = new RawEmailMessage(
                "msg-1",
                "BeBetta Hiring team <no-reply@kekamail.com>",
                "Application for Software Engineering Intern received",
                "Thank you for applying to BeBetta",
                LocalDate.of(2026, 11, 9)
        );
        when(imapClient.fetchMessages(anyString(), anyInt(), anyString(), anyString(), any(), anyInt()))
                .thenReturn(List.of(rawMsg));

        ParsedEmailApplication parsed = new ParsedEmailApplication();
        parsed.setRelevant(true);
        parsed.setCompany("BeBetta");
        parsed.setPosition("Software Engineering Intern");
        parsed.setStatus(ApplicationStatus.APPLIED);
        parsed.setEmailDate(LocalDate.of(2026, 11, 9));
        parsed.setNotes("Imported from email");
        when(emailParserService.parse(any(), any(), any(), any())).thenReturn(parsed);

        when(jobApplicationRepository.findByUserId(USER_ID)).thenReturn(new ArrayList<>());

        EmailSyncSummary summary = emailSyncService.syncEmailsForUser(USER_ID);

        assertThat(summary.getScannedCount()).isEqualTo(1);
        assertThat(summary.getCreatedCount()).isEqualTo(1);
        assertThat(summary.getUpdatedCount()).isEqualTo(0);

        ArgumentCaptor<JobApplication> captor = ArgumentCaptor.forClass(JobApplication.class);
        verify(jobApplicationRepository).save(captor.capture());
        JobApplication saved = captor.getValue();
        assertThat(saved.getCompany()).isEqualTo("BeBetta");
        assertThat(saved.getRole()).isEqualTo("Software Engineering Intern");
        assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(saved.getSource()).isEqualTo("EMAIL_SYNC");

        verify(userRepository).save(testUser);
        assertThat(testUser.getLastEmailSyncedAt()).isNotNull();
    }

    @Test
    @DisplayName("syncEmails updates existing application status when valid transition received")
    void testSyncEmailsUpdatesExistingApplication() throws Exception {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        JobApplication existingApp = new JobApplication();
        existingApp.setId(500L);
        existingApp.setUser(testUser);
        existingApp.setCompany("Atlassian");
        existingApp.setRole("Data Engineer Intern");
        existingApp.setStatus(ApplicationStatus.APPLIED);

        List<JobApplication> existingList = new ArrayList<>(List.of(existingApp));
        when(jobApplicationRepository.findByUserId(USER_ID)).thenReturn(existingList);

        RawEmailMessage rawMsg = new RawEmailMessage(
                "msg-2",
                "Atlassian @ icims <atlassian@icims.com>",
                "Application Status",
                "Unfortunately we are not moving forward",
                LocalDate.of(2026, 11, 4)
        );
        when(imapClient.fetchMessages(anyString(), anyInt(), anyString(), anyString(), any(), anyInt()))
                .thenReturn(List.of(rawMsg));

        ParsedEmailApplication parsed = new ParsedEmailApplication();
        parsed.setRelevant(true);
        parsed.setCompany("Atlassian");
        parsed.setPosition("Data Engineer Intern");
        parsed.setStatus(ApplicationStatus.REJECTED);
        parsed.setEmailDate(LocalDate.of(2026, 11, 4));
        when(emailParserService.parse(any(), any(), any(), any())).thenReturn(parsed);

        EmailSyncSummary summary = emailSyncService.syncEmailsForUser(USER_ID);

        assertThat(summary.getCreatedCount()).isEqualTo(0);
        assertThat(summary.getUpdatedCount()).isEqualTo(1);
        assertThat(existingApp.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(existingApp.getNotes()).contains("Status updated to REJECTED");
        verify(jobApplicationRepository).save(existingApp);
    }

    @Test
    @DisplayName("syncEmails skips irrelevant emails")
    void testSyncEmailsSkipsIrrelevantEmails() throws Exception {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        RawEmailMessage rawMsg = new RawEmailMessage(
                "msg-3",
                "Mastercard <promo@mastercard.com>",
                "Browse careers",
                "Explore all jobs",
                LocalDate.now()
        );
        when(imapClient.fetchMessages(anyString(), anyInt(), anyString(), anyString(), any(), anyInt()))
                .thenReturn(List.of(rawMsg));

        when(emailParserService.parse(any(), any(), any(), any()))
                .thenReturn(ParsedEmailApplication.irrelevant());

        when(jobApplicationRepository.findByUserId(USER_ID)).thenReturn(new ArrayList<>());

        EmailSyncSummary summary = emailSyncService.syncEmailsForUser(USER_ID);

        assertThat(summary.getScannedCount()).isEqualTo(1);
        assertThat(summary.getCreatedCount()).isEqualTo(0);
        assertThat(summary.getUpdatedCount()).isEqualTo(0);
        verify(jobApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("syncEmails throws IllegalStateException when credentials missing")
    void testSyncEmailsThrowsWhenNoCredentials() {
        testUser.setImapPassword("");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> emailSyncService.syncEmailsForUser(USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("credentials are not configured");
    }

    @Test
    @DisplayName("scheduledSync runs for all users with emailSyncEnabled true")
    void testScheduledSyncRunsForEnabledUsers() throws Exception {
        User enabledUser = new User();
        enabledUser.setId(200L);
        enabledUser.setEmail("enabled@example.com");
        enabledUser.setImapUsername("enabled@example.com");
        enabledUser.setImapPassword("pass123");
        enabledUser.setEmailSyncEnabled(true);

        when(userRepository.findByEmailSyncEnabledTrue()).thenReturn(List.of(enabledUser));
        when(userRepository.findById(200L)).thenReturn(Optional.of(enabledUser));
        when(imapClient.fetchMessages(anyString(), anyInt(), anyString(), anyString(), any(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(jobApplicationRepository.findByUserId(200L)).thenReturn(Collections.emptyList());

        emailSyncService.scheduledSync();

        verify(userRepository).findByEmailSyncEnabledTrue();
        verify(imapClient).fetchMessages(anyString(), anyInt(), anyString(), anyString(), any(), anyInt());
    }
}

