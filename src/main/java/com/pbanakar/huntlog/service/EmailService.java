package com.pbanakar.huntlog.service;

import com.pbanakar.huntlog.model.JobApplication;
import com.pbanakar.huntlog.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final int staleDays;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.reminders.stale-days:7}") int staleDays,
                        @Value("${spring.mail.username:noreply@huntlog.com}") String fromAddress) {
        this.mailSender = mailSender;
        this.staleDays = staleDays;
        this.fromAddress = fromAddress;
    }

    public void sendReminderEmail(User user, List<JobApplication> staleApps) {
        if (user == null || staleApps == null || staleApps.isEmpty()) {
            return;
        }

        int count = staleApps.size();
        String subject = "HuntLog: " + count + (count == 1 ? " application needs your attention" : " applications need your attention");
        String body = buildReminderBody(user, staleApps, count);

        SimpleMailMessage message = new SimpleMailMessage();
        if (fromAddress != null && !fromAddress.isBlank()) {
            message.setFrom(fromAddress);
        }
        message.setTo(user.getEmail());
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
        logger.debug("Dispatched reminder email message to {}", user.getEmail());
    }

    public String buildReminderBody(User user, List<JobApplication> staleApps, int count) {
        StringBuilder sb = new StringBuilder();
        String name = user != null && user.getName() != null && !user.getName().isBlank()
                ? user.getName()
                : "there";

        sb.append("Hi ").append(name).append(",\n\n");
        sb.append("You have ").append(count)
          .append(" job application(s) that haven't been updated in over ")
          .append(staleDays).append(" days:\n\n");

        for (JobApplication app : staleApps) {
            long daysSinceUpdate = app.getLastUpdated() != null
                    ? ChronoUnit.DAYS.between(app.getLastUpdated().toLocalDate(), LocalDate.now())
                    : (app.getAppliedDate() != null ? ChronoUnit.DAYS.between(app.getAppliedDate(), LocalDate.now()) : staleDays);

            sb.append("- ").append(app.getCompany()).append(" — ").append(app.getRole())
              .append(" (").append(app.getStatus()).append(")\n");
            sb.append("  Applied: ").append(app.getAppliedDate()).append("\n");
            sb.append("  Last updated: ").append(daysSinceUpdate).append(" days ago\n");
            if (app.getJobUrl() != null && !app.getJobUrl().isBlank()) {
                sb.append("  ").append(app.getJobUrl().trim()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("Log into HuntLog to update your progress.\n\n");
        sb.append("— HuntLog\n");

        return sb.toString();
    }
}
