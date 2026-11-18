package com.pbanakar.huntlog.service;

import com.pbanakar.huntlog.model.JobApplication;
import com.pbanakar.huntlog.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Properties;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final int staleDays;
    private final String fromAddress;
    private final String mailUsername;
    private final String mailPassword;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.reminders.stale-days:7}") int staleDays,
                        @Value("${app.mail.from:noreply@huntlog.com}") String fromAddress) {
        this(mailSender, staleDays, fromAddress, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public EmailService(JavaMailSender mailSender,
                        @Value("${app.reminders.stale-days:7}") int staleDays,
                        @Value("${app.mail.from:noreply@huntlog.com}") String fromAddress,
                        @Value("${spring.mail.username:}") String mailUsername,
                        @Value("${spring.mail.password:}") String mailPassword) {
        this.mailSender = mailSender;
        this.staleDays = staleDays;
        this.fromAddress = fromAddress;
        this.mailUsername = mailUsername;
        this.mailPassword = mailPassword;
    }

    public void sendReminderEmail(User user, List<JobApplication> staleApps) {
        if (user == null || staleApps == null || staleApps.isEmpty()) {
            return;
        }

        int count = staleApps.size();
        String subject = "HuntLog: " + count + (count == 1 ? " application needs your attention" : " applications need your attention");
        String body = buildReminderBody(user, staleApps, count);

        SimpleMailMessage message = new SimpleMailMessage();
        String effectiveFrom = fromAddress;

        JavaMailSender senderToUse = mailSender;
        boolean serverConfigured = mailUsername != null && !mailUsername.isBlank() && mailPassword != null && !mailPassword.isBlank();

        if (!serverConfigured && hasUserEmailCredentials(user)) {
            senderToUse = buildUserMailSender(user);
            effectiveFrom = user.getImapUsername();
        }

        if (effectiveFrom != null && !effectiveFrom.isBlank()) {
            message.setFrom(effectiveFrom);
        }
        message.setTo(user.getEmail());
        message.setSubject(subject);
        message.setText(body);

        try {
            senderToUse.send(message);
            logger.info("Dispatched reminder email message to {}", user.getEmail());
        } catch (Exception ex) {
            if (senderToUse == mailSender && hasUserEmailCredentials(user)) {
                logger.warn("Primary server mail sender failed ({}); attempting fallback with user's SMTP credentials...", ex.getMessage());
                JavaMailSender fallbackSender = buildUserMailSender(user);
                message.setFrom(user.getImapUsername());
                fallbackSender.send(message);
                logger.info("Dispatched reminder email via user's SMTP credentials fallback to {}", user.getEmail());
            } else {
                throw ex;
            }
        }
    }

    private boolean hasUserEmailCredentials(User user) {
        return user != null && user.getImapUsername() != null && !user.getImapUsername().isBlank()
                && user.getImapPassword() != null && !user.getImapPassword().isBlank();
    }

    private JavaMailSender buildUserMailSender(User user) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        String host = user.getImapHost() != null && user.getImapHost().contains("gmail")
                ? "smtp.gmail.com"
                : "smtp.gmail.com";
        sender.setHost(host);
        sender.setPort(587);
        sender.setUsername(user.getImapUsername());
        sender.setPassword(user.getImapPassword());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "7000");
        props.put("mail.smtp.timeout", "7000");
        return sender;
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
