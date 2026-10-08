package com.pbanakar.huntlog.service;

import com.pbanakar.huntlog.enums.ApplicationStatus;
import com.pbanakar.huntlog.model.JobApplication;
import com.pbanakar.huntlog.model.User;
import com.pbanakar.huntlog.repository.JobApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReminderService {

    private static final Logger logger = LoggerFactory.getLogger(ReminderService.class);

    private static final List<ApplicationStatus> PENDING_STATUSES = List.of(
            ApplicationStatus.APPLIED,
            ApplicationStatus.SCREENING,
            ApplicationStatus.INTERVIEW
    );

    private final JobApplicationRepository jobApplicationRepository;
    private final EmailService emailService;
    private final int staleDays;
    private final boolean remindersEnabled;

    public ReminderService(JobApplicationRepository jobApplicationRepository,
                           EmailService emailService,
                           @Value("${app.reminders.stale-days:7}") int staleDays,
                           @Value("${app.reminders.enabled:true}") boolean remindersEnabled) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.emailService = emailService;
        this.staleDays = staleDays;
        this.remindersEnabled = remindersEnabled;
    }

    @Scheduled(cron = "${app.reminders.cron:0 0 9 * * *}")
    @Transactional(readOnly = true)
    public void sendPendingApplicationReminders() {
        if (!remindersEnabled) {
            logger.info("Reminders disabled, skipping");
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusDays(staleDays);
        List<JobApplication> staleApplications = jobApplicationRepository.findStaleApplications(PENDING_STATUSES, cutoff);

        if (staleApplications.isEmpty()) {
            logger.info("No stale pending applications found for reminders");
            return;
        }

        Map<User, List<JobApplication>> appsByUser = staleApplications.stream()
                .filter(app -> app.getUser() != null && Boolean.TRUE.equals(app.getUser().getEmailRemindersEnabled()))
                .collect(Collectors.groupingBy(JobApplication::getUser));

        for (Map.Entry<User, List<JobApplication>> entry : appsByUser.entrySet()) {
            User user = entry.getKey();
            List<JobApplication> userApps = entry.getValue();

            try {
                emailService.sendReminderEmail(user, userApps);
                logger.info("Sent reminder to {} for {} stale applications", user.getEmail(), userApps.size());
            } catch (Exception e) {
                logger.error("Failed to send reminder email to {}: {}", user.getEmail(), e.getMessage(), e);
            }
        }
    }

    public int getStaleDays() {
        return staleDays;
    }

    public boolean isRemindersEnabled() {
        return remindersEnabled;
    }
}
