package com.pbanakar.huntlog.service;

import com.pbanakar.huntlog.enums.ApplicationStatus;
import com.pbanakar.huntlog.model.JobApplication;
import com.pbanakar.huntlog.model.User;
import com.pbanakar.huntlog.repository.JobApplicationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private JobApplicationRepository repository;

    @Mock
    private EmailService emailService;

    private User createUser(Long id, String email, boolean remindersEnabled) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName("User " + id);
        user.setEmailRemindersEnabled(remindersEnabled);
        return user;
    }

    private JobApplication createApp(Long id, User user, String company, ApplicationStatus status) {
        JobApplication app = new JobApplication();
        app.setId(id);
        app.setUser(user);
        app.setCompany(company);
        app.setRole("Engineer");
        app.setStatus(status);
        app.setLastUpdated(LocalDateTime.now().minusDays(10));
        return app;
    }

    @Test
    @DisplayName("1. Applications updated within 7 days are NOT included in reminders")
    void testRecentApplicationsNotIncluded() {
        ReminderService service = new ReminderService(repository, emailService, 7, true);
        
        // When repository returns empty list (because recent apps filtered out by cutoff)
        when(repository.findStaleApplications(anyList(), any(LocalDateTime.class))).thenReturn(List.of());

        service.sendPendingApplicationReminders();

        verify(emailService, never()).sendReminderEmail(any(), any());
    }

    @Test
    @DisplayName("2. Applications in terminal status (REJECTED, ACCEPTED, WITHDRAWN, OFFER) are NOT included")
    void testTerminalStatusApplicationsExcluded() {
        ReminderService service = new ReminderService(repository, emailService, 7, true);

        when(repository.findStaleApplications(anyList(), any(LocalDateTime.class))).thenReturn(List.of());

        service.sendPendingApplicationReminders();

        ArgumentCaptor<List<ApplicationStatus>> statusesCaptor = ArgumentCaptor.forClass(List.class);
        verify(repository).findStaleApplications(statusesCaptor.capture(), any(LocalDateTime.class));

        List<ApplicationStatus> queriedStatuses = statusesCaptor.getValue();
        assertThat(queriedStatuses).containsExactlyInAnyOrder(
                ApplicationStatus.APPLIED,
                ApplicationStatus.SCREENING,
                ApplicationStatus.INTERVIEW
        );
        assertThat(queriedStatuses).doesNotContain(
                ApplicationStatus.OFFER,
                ApplicationStatus.ACCEPTED,
                ApplicationStatus.REJECTED,
                ApplicationStatus.WITHDRAWN
        );
    }

    @Test
    @DisplayName("3. Users with emailRemindersEnabled=false are NOT sent reminders")
    void testDisabledPreferencesDoNotReceiveEmails() {
        ReminderService service = new ReminderService(repository, emailService, 7, true);

        User disabledUser = createUser(1L, "optout@example.com", false);
        JobApplication app = createApp(10L, disabledUser, "Uber", ApplicationStatus.APPLIED);

        when(repository.findStaleApplications(anyList(), any(LocalDateTime.class))).thenReturn(List.of(app));

        service.sendPendingApplicationReminders();

        verify(emailService, never()).sendReminderEmail(eq(disabledUser), anyList());
    }

    @Test
    @DisplayName("4. Multiple stale apps for same user -> ONE email, not multiple")
    void testMultipleStaleAppsGroupedIntoSingleEmail() {
        ReminderService service = new ReminderService(repository, emailService, 7, true);

        User user = createUser(1L, "user@example.com", true);
        JobApplication app1 = createApp(10L, user, "Google", ApplicationStatus.APPLIED);
        JobApplication app2 = createApp(11L, user, "Apple", ApplicationStatus.SCREENING);
        JobApplication app3 = createApp(12L, user, "Netflix", ApplicationStatus.INTERVIEW);

        when(repository.findStaleApplications(anyList(), any(LocalDateTime.class))).thenReturn(List.of(app1, app2, app3));

        service.sendPendingApplicationReminders();

        verify(emailService, times(1)).sendReminderEmail(eq(user), argThat(list -> list.size() == 3));
    }

    @Test
    @DisplayName("5. Email send failure for one user -> other users still get their emails without exception propagation")
    void testFailureForOneUserDoesNotBlockOtherUsers() {
        ReminderService service = new ReminderService(repository, emailService, 7, true);

        User user1 = createUser(1L, "failing@example.com", true);
        User user2 = createUser(2L, "successful@example.com", true);

        JobApplication app1 = createApp(10L, user1, "Google", ApplicationStatus.APPLIED);
        JobApplication app2 = createApp(20L, user2, "Microsoft", ApplicationStatus.APPLIED);

        when(repository.findStaleApplications(anyList(), any(LocalDateTime.class))).thenReturn(List.of(app1, app2));
        doThrow(new RuntimeException("SMTP Connection Timeout")).when(emailService).sendReminderEmail(eq(user1), anyList());

        // Should complete cleanly without throwing exception
        service.sendPendingApplicationReminders();

        verify(emailService).sendReminderEmail(eq(user1), anyList());
        verify(emailService).sendReminderEmail(eq(user2), anyList());
    }

    @Test
    @DisplayName("6. When reminders disabled via config -> no emails sent, method returns early")
    void testRemindersDisabledInConfigReturnsEarly() {
        ReminderService service = new ReminderService(repository, emailService, 7, false);

        service.sendPendingApplicationReminders();

        verifyNoInteractions(repository);
        verifyNoInteractions(emailService);
    }
}
