package com.pbanakar.huntlog.service;

import com.pbanakar.huntlog.enums.ApplicationStatus;
import com.pbanakar.huntlog.model.JobApplication;
import com.pbanakar.huntlog.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, 7, "noreply@huntlog.com");
    }

    private User createUser(String name, String email) {
        User user = new User();
        user.setId(1L);
        user.setName(name);
        user.setEmail(email);
        user.setEmailRemindersEnabled(true);
        return user;
    }

    private JobApplication createApp(String company, String role, ApplicationStatus status, String jobUrl, int daysAgo) {
        JobApplication app = new JobApplication();
        app.setCompany(company);
        app.setRole(role);
        app.setStatus(status);
        app.setJobUrl(jobUrl);
        app.setAppliedDate(LocalDate.now().minusDays(daysAgo + 5));
        app.setLastUpdated(LocalDateTime.now().minusDays(daysAgo));
        return app;
    }

    @Test
    @DisplayName("Email subject contains the correct count")
    void testEmailSubjectContainsCorrectCount() {
        User user = createUser("Alice", "alice@example.com");
        JobApplication app1 = createApp("Google", "SWE III", ApplicationStatus.APPLIED, "https://google.com/job", 8);
        JobApplication app2 = createApp("Meta", "E5 Lead", ApplicationStatus.SCREENING, "https://meta.com/job", 10);

        emailService.sendReminderEmail(user, List.of(app1, app2));

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getTo()).containsExactly("alice@example.com");
        assertThat(sentMessage.getSubject()).contains("2");
        assertThat(sentMessage.getSubject()).contains("applications need your attention");
    }

    @Test
    @DisplayName("Email body contains company, role, status for each stale application")
    void testEmailBodyContainsCompanyRoleStatus() {
        User user = createUser("Bob", "bob@example.com");
        JobApplication app1 = createApp("Stripe", "Staff Backend", ApplicationStatus.SCREENING, null, 9);
        JobApplication app2 = createApp("Netflix", "Senior Infra", ApplicationStatus.INTERVIEW, null, 14);

        emailService.sendReminderEmail(user, List.of(app1, app2));

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        String body = messageCaptor.getValue().getText();
        assertThat(body).isNotNull();
        assertThat(body).contains("Hi Bob,");
        assertThat(body).contains("- Stripe — Staff Backend (SCREENING)");
        assertThat(body).contains("- Netflix — Senior Infra (INTERVIEW)");
        assertThat(body).contains("7 days");
    }

    @Test
    @DisplayName("Job URL is included when present, omitted when null")
    void testJobUrlIncludedWhenPresentOmittedWhenNull() {
        User user = createUser("Charlie", "charlie@example.com");
        JobApplication appWithUrl = createApp("Apple", "CoreOS Lead", ApplicationStatus.APPLIED, "https://apple.com/jobs/101", 8);
        JobApplication appWithoutUrl = createApp("Amazon", "SDE II", ApplicationStatus.APPLIED, null, 12);

        emailService.sendReminderEmail(user, List.of(appWithUrl, appWithoutUrl));

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        String body = messageCaptor.getValue().getText();
        assertThat(body).isNotNull();
        assertThat(body).contains("https://apple.com/jobs/101");
        assertThat(body).contains("Amazon — SDE II (APPLIED)");
        // Verify Amazon doesn't have an empty/broken URL line or other URL
        assertThat(body).doesNotContain("null");
    }
}
