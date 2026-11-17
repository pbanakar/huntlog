package com.pbanakar.huntlog.service;

import com.pbanakar.huntlog.dto.sync.EmailSyncSummary;
import com.pbanakar.huntlog.dto.sync.ParsedEmailApplication;
import com.pbanakar.huntlog.dto.sync.RawEmailMessage;
import com.pbanakar.huntlog.enums.ApplicationStatus;
import com.pbanakar.huntlog.exception.ResourceNotFoundException;
import com.pbanakar.huntlog.model.JobApplication;
import com.pbanakar.huntlog.model.User;
import com.pbanakar.huntlog.repository.JobApplicationRepository;
import com.pbanakar.huntlog.repository.UserRepository;
import com.pbanakar.huntlog.statemachine.ApplicationStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class EmailSyncService {

    private static final Logger logger = LoggerFactory.getLogger(EmailSyncService.class);

    private final ImapClient imapClient;
    private final EmailParserService emailParserService;
    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;
    private final ApplicationStateMachine applicationStateMachine;

    private final boolean syncEnabled;
    private final int daysToScan;
    private final String defaultMailUsername;
    private final String defaultMailPassword;

    public EmailSyncService(ImapClient imapClient,
                            EmailParserService emailParserService,
                            JobApplicationRepository jobApplicationRepository,
                            UserRepository userRepository,
                            ApplicationStateMachine applicationStateMachine,
                            @Value("${app.email-sync.enabled:true}") boolean syncEnabled,
                            @Value("${app.email-sync.days-to-scan:14}") int daysToScan,
                            @Value("${spring.mail.username:}") String defaultMailUsername,
                            @Value("${spring.mail.password:}") String defaultMailPassword) {
        this.imapClient = imapClient;
        this.emailParserService = emailParserService;
        this.jobApplicationRepository = jobApplicationRepository;
        this.userRepository = userRepository;
        this.applicationStateMachine = applicationStateMachine;
        this.syncEnabled = syncEnabled;
        this.daysToScan = daysToScan;
        this.defaultMailUsername = defaultMailUsername;
        this.defaultMailPassword = defaultMailPassword;
    }

    /**
     * Synchronizes emails for the given user, creating new job applications
     * or updating existing application statuses through the state machine.
     */
    @Transactional
    public EmailSyncSummary syncEmailsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String username = resolveUsername(user);
        String password = resolvePassword(user);

        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException("IMAP email credentials are not configured. Please configure your email and Google App Password in Settings.");
        }

        String host = user.getImapHost() != null && !user.getImapHost().isBlank()
                ? user.getImapHost()
                : "imap.gmail.com";
        int port = user.getImapPort() != null && user.getImapPort() > 0
                ? user.getImapPort()
                : 993;

        LocalDate sinceDate = user.getLastEmailSyncedAt() != null
                ? user.getLastEmailSyncedAt().toLocalDate()
                : LocalDate.now().minusDays(daysToScan);

        List<RawEmailMessage> rawMessages;
        try {
            rawMessages = imapClient.fetchMessages(host, port, username, password, sinceDate, 50);
        } catch (Exception ex) {
            logger.error("Failed to connect or fetch emails from IMAP for user {}: {}", user.getEmail(), ex.getMessage());
            throw new RuntimeException("Failed to connect to mail server: " + ex.getMessage(), ex);
        }

        EmailSyncSummary summary = new EmailSyncSummary();
        summary.setScannedCount(rawMessages.size());

        List<JobApplication> existingApplications = jobApplicationRepository.findByUserId(userId);

        for (RawEmailMessage msg : rawMessages) {
            ParsedEmailApplication parsed = emailParserService.parse(
                    msg.getFrom(),
                    msg.getSubject(),
                    msg.getBody(),
                    msg.getReceivedDate()
            );

            if (!parsed.isRelevant()) {
                continue;
            }

            Optional<JobApplication> matchOpt = findMatchingApplication(existingApplications, parsed.getCompany(), parsed.getPosition());

            if (matchOpt.isPresent()) {
                JobApplication app = matchOpt.get();
                boolean updated = processExistingApplication(app, parsed, summary);
                if (updated) {
                    jobApplicationRepository.save(app);
                }
            } else {
                JobApplication newApp = createNewApplication(user, parsed);
                jobApplicationRepository.save(newApp);
                existingApplications.add(newApp);
                summary.setCreatedCount(summary.getCreatedCount() + 1);
                summary.addDetail("Created: " + newApp.getCompany() + " — " + newApp.getRole() + " (" + newApp.getStatus() + ")");
            }
        }

        user.setLastEmailSyncedAt(LocalDateTime.now());
        userRepository.save(user);

        logger.info("Email sync complete for user {}: scanned {}, created {}, updated {}",
                user.getEmail(), summary.getScannedCount(), summary.getCreatedCount(), summary.getUpdatedCount());

        return summary;
    }

    /**
     * Background scheduled scanner to poll email for all users who enabled auto-sync.
     */
    @Scheduled(fixedDelayString = "${app.email-sync.scan-interval-ms:3600000}",
               initialDelayString = "${app.email-sync.initial-delay-ms:120000}")
    public void scheduledSync() {
        if (!syncEnabled) {
            logger.info("Email auto-sync is disabled globally in configuration.");
            return;
        }

        List<User> enabledUsers = userRepository.findByEmailSyncEnabledTrue();
        logger.info("Starting scheduled email sync for {} users with email sync enabled", enabledUsers.size());

        for (User user : enabledUsers) {
            try {
                syncEmailsForUser(user.getId());
            } catch (Exception ex) {
                logger.warn("Scheduled email sync failed for user {}: {}", user.getEmail(), ex.getMessage());
            }
        }
    }

    private boolean processExistingApplication(JobApplication app, ParsedEmailApplication parsed, EmailSyncSummary summary) {
        boolean modified = false;

        if (app.getStatus() != parsed.getStatus()) {
            Set<ApplicationStatus> allowed = applicationStateMachine.getAllowedTransitions(app.getStatus());

            if (allowed.contains(parsed.getStatus())) {
                app.setStatus(parsed.getStatus());
                app.setLastUpdated(LocalDateTime.now());
                appendNote(app, "Status updated to " + parsed.getStatus() + " via email sync on " + parsed.getEmailDate());
                summary.setUpdatedCount(summary.getUpdatedCount() + 1);
                summary.addDetail("Updated status: " + app.getCompany() + " — " + app.getRole() + " to " + parsed.getStatus());
                modified = true;
            } else if (app.getStatus() == ApplicationStatus.APPLIED && parsed.getStatus() == ApplicationStatus.INTERVIEW) {
                // Bridge APPLIED -> SCREENING -> INTERVIEW if direct invite received
                app.setStatus(ApplicationStatus.INTERVIEW);
                app.setLastUpdated(LocalDateTime.now());
                appendNote(app, "Direct interview invite received; updated to INTERVIEW via email sync on " + parsed.getEmailDate());
                summary.setUpdatedCount(summary.getUpdatedCount() + 1);
                summary.addDetail("Updated status: " + app.getCompany() + " — " + app.getRole() + " to INTERVIEW");
                modified = true;
            }
        }

        if (parsed.getJobUrl() != null && (app.getJobUrl() == null || app.getJobUrl().isBlank())) {
            app.setJobUrl(parsed.getJobUrl());
            modified = true;
        }

        return modified;
    }

    private JobApplication createNewApplication(User user, ParsedEmailApplication parsed) {
        JobApplication app = new JobApplication();
        app.setUser(user);
        app.setCompany(parsed.getCompany());
        app.setRole(parsed.getPosition());
        app.setStatus(parsed.getStatus() != null ? parsed.getStatus() : ApplicationStatus.APPLIED);
        app.setAppliedDate(parsed.getEmailDate() != null ? parsed.getEmailDate() : LocalDate.now());
        app.setJobUrl(parsed.getJobUrl());
        app.setSource("EMAIL_SYNC");
        app.setNotes(parsed.getNotes());
        app.setLastUpdated(LocalDateTime.now());
        return app;
    }

    private Optional<JobApplication> findMatchingApplication(List<JobApplication> existing, String company, String role) {
        if (company == null || role == null) {
            return Optional.empty();
        }

        String normCompany = company.trim().toLowerCase();
        String normRole = role.trim().toLowerCase();

        return existing.stream()
                .filter(app -> {
                    String existingCompany = app.getCompany().trim().toLowerCase();
                    if (!existingCompany.equals(normCompany) && !existingCompany.contains(normCompany) && !normCompany.contains(existingCompany)) {
                        return false;
                    }

                    String existingRole = app.getRole().trim().toLowerCase();
                    return existingRole.equals(normRole)
                            || existingRole.contains(normRole)
                            || normRole.contains(existingRole)
                            || normRole.equals("general application");
                })
                .findFirst();
    }

    private void appendNote(JobApplication app, String newNote) {
        if (app.getNotes() == null || app.getNotes().isBlank()) {
            app.setNotes(newNote);
        } else {
            app.setNotes(app.getNotes() + "\n" + newNote);
        }
    }

    private String resolveUsername(User user) {
        if (user.getImapUsername() != null && !user.getImapUsername().isBlank()) {
            return user.getImapUsername().trim();
        }
        if (defaultMailUsername != null && !defaultMailUsername.isBlank()) {
            return defaultMailUsername.trim();
        }
        return user.getEmail() != null ? user.getEmail().trim() : "";
    }

    private String resolvePassword(User user) {
        if (user.getImapPassword() != null && !user.getImapPassword().isBlank()) {
            return user.getImapPassword().trim();
        }
        if (defaultMailPassword != null && !defaultMailPassword.isBlank()) {
            return defaultMailPassword.trim();
        }
        return "";
    }
}

