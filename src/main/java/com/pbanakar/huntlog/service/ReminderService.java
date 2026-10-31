package com.pbanakar.huntlog.service;

import com.pbanakar.huntlog.enums.ApplicationStatus;
import com.pbanakar.huntlog.model.JobApplication;
import com.pbanakar.huntlog.model.User;
import com.pbanakar.huntlog.repository.JobApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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

    @Scheduled(fixedDelayString = "${app.reminders.scan-interval-ms:3600000}",
            initialDelayString = "${app.reminders.initial-delay-ms:60000}")
    @Transactional
    public void sendPendingApplicationReminders() {
        sendPendingApplicationReminders(null);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void sendRemindersOnStartup() {
        sendPendingApplicationReminders();
    }

    @Transactional
    public int sendPendingApplicationReminders(Long userId) {
        if (!remindersEnabled) {
            logger.info("Reminders disabled, skipping");
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleCutoff = now.minusDays(staleDays);
        LocalDateTime reminderCutoff = now.minusDays(1);
        List<JobApplication> staleApplications = userId == null
                ? jobApplicationRepository.findStaleApplications(PENDING_STATUSES, staleCutoff, reminderCutoff)
                : jobApplicationRepository.findStaleApplicationsForUser(
                        userId, PENDING_STATUSES, staleCutoff, reminderCutoff);

        if (staleApplications.isEmpty()) {
            logger.info("No stale pending applications found for reminders");
            return 0;
        }

        Map<User, List<JobApplication>> appsByUser = staleApplications.stream()
                .filter(app -> app.getUser() != null && Boolean.TRUE.equals(app.getUser().getEmailRemindersEnabled()))
                .collect(Collectors.groupingBy(JobApplication::getUser));

        for (Map.Entry<User, List<JobApplication>> entry : appsByUser.entrySet()) {
            User user = entry.getKey();
            List<JobApplication> userApps = entry.getValue();

            try {
                emailService.sendReminderEmail(user, userApps);
                userApps.forEach(app -> app.setLastReminderSentAt(now));
                jobApplicationRepository.saveAll(userApps);
                logger.info("Sent reminder to {} for {} stale applications", user.getEmail(), userApps.size());
            } catch (Exception e) {
                logger.error("Failed to send reminder email to {}: {}", user.getEmail(), e.getMessage(), e);
            }
        }
        return appsByUser.values().stream().mapToInt(List::size).sum();
    }

    public int getStaleDays() {
        return staleDays;
    }

    public boolean isRemindersEnabled() {
        return remindersEnabled;
    }
}
