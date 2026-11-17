package com.pbanakar.huntlog.service;

import com.pbanakar.huntlog.dto.sync.ParsedEmailApplication;
import com.pbanakar.huntlog.enums.ApplicationStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailParserService {

    // --- Promotional / Marketing Negative Matchers ---
    private static final List<String> MARKETING_PHRASES = List.of(
            "browse all jobs",
            "weekly digest",
            "job alert",
            "recommended jobs",
            "jobs you may be interested in",
            "matches your search",
            "career opportunities await",
            "unsubscribe from job alerts"
    );

    // --- Status Classification Keywords ---
    private static final List<String> OFFER_KEYWORDS = List.of(
            "offer letter",
            "offer of employment",
            "pleased to offer",
            "congratulations on your offer",
            "delighted to offer",
            "welcome to the team"
    );

    private static final List<String> REJECTION_KEYWORDS = List.of(
            "not moving forward",
            "unable to proceed",
            "other candidates",
            "does not meet the eligibility",
            "unfortunately",
            "will not be moving forward",
            "will not be progressing",
            "not be progressing",
            "decided to pursue other",
            "pursue other candidates",
            "decided not to proceed",
            "not moving to the next"
    );

    private static final List<String> INTERVIEW_KEYWORDS = List.of(
            "interview invitation",
            "schedule an interview",
            "schedule your interview",
            "interview with",
            "coding challenge",
            "hackerrank",
            "technical assessment",
            "online assessment",
            "completed two interview rounds",
            "invitation to interview",
            "next steps in our interview process",
            "interview scheduled"
    );

    private static final List<String> APPLICATION_MARKERS = List.of(
            "application received",
            "thank you for applying",
            "thanks for applying",
            "we've received your application",
            "we have received your application",
            "thanks for completing your application",
            "application for",
            "applied to",
            "application status",
            "recent application",
            "internship application",
            "talent community"
    );

    // --- Heuristic Regular Expressions ---
    private static final Pattern SENDER_NAME_CLEANER = Pattern.compile(
            "(?i)\\s*(hiring team|recruiting team|recruiting|careers|talent acquisition|talent|campus recruiting|jobs|hr team|via bob|@ icims).*"
    );

    private static final List<Pattern> COMPANY_SUBJECT_PATTERNS = List.of(
            Pattern.compile("(?i)thank you for applying to\\s+([A-Z0-9&._ -]{2,40}?)(?:\\s+for|!|\\.|$)"),
            Pattern.compile("(?i)application received:.*?\\s+(?:at|@)\\s+([A-Z0-9&._ -]{2,40}?)(?:!|\\.|$)"),
            Pattern.compile("(?i)interest in\\s+([A-Z0-9&._ -]{2,40}?)(?:!|\\.|$)"),
            Pattern.compile("(?i)^([A-Z0-9&._ -]{2,30}?):\\s+thanks for completing")
    );

    private static final List<Pattern> COMPANY_BODY_PATTERNS = List.of(
            Pattern.compile("(?i)thank you for applying to\\s+([A-Z0-9&._ -]{2,40}?)\\s+for"),
            Pattern.compile("(?i)position at\\s+([A-Z0-9&._ -]{2,40}?)(?:!|\\.|,)"),
            Pattern.compile("(?i)application to\\s+([A-Z0-9&._ -]{2,40}?)\\s+for the role of"),
            Pattern.compile("(?i)application to the.*?position at\\s+([A-Z0-9&._ -]{2,40}?)(?:!|\\.|,)"),
            Pattern.compile("(?i)talent community at\\s+([A-Z0-9&._ -]{2,40}?)(?:!|\\.|,)")
    );

    private static final List<Pattern> ROLE_PATTERNS = List.of(
            Pattern.compile("(?i)application for\\s+(.+?)\\s+received"),
            Pattern.compile("(?i)application received:\\s+(.+?)(?:\\s+(?:at|@|for)|$)"),
            Pattern.compile("(?i)for the role of\\s+([A-Za-z0-9&._, /()-]{2,100})"),
            Pattern.compile("(?i)for the\\s+([A-Za-z0-9&._, /()-]{2,100})\\s+position"),
            Pattern.compile("(?i)application to the\\s+([A-Za-z0-9&._, /()-]{2,100})\\s+position"),
            Pattern.compile("(?i)application for\\s+(.+?)(?:\\s*[–-]\\s*Prajwal|$)"),
            Pattern.compile("(?i)applying for the\\s+([A-Za-z0-9&._, /()-]{2,100})\\s+at")
    );

    private static final Pattern PORTAL_URL_PATTERN = Pattern.compile(
            "https?://[a-zA-Z0-9.-]+\\.(?:careers\\.[a-z]+|greenhouse\\.io|lever\\.co|myworkday\\.com|hibob\\.com)[^\\s\"'<)]+"
    );

    /**
     * Parses email metadata and content into a normalized job application representation.
     */
    public ParsedEmailApplication parse(String from, String subject, String body, LocalDate emailDate) {
        if (subject == null) subject = "";
        if (body == null) body = "";
        if (from == null) from = "";

        String fullContent = (subject + "\n" + body).toLowerCase();

        // 1. Check for marketing / spam filter
        for (String marketing : MARKETING_PHRASES) {
            if (fullContent.contains(marketing)) {
                return ParsedEmailApplication.irrelevant();
            }
        }

        // 2. Check for relevant application indicators
        boolean isApplicationRelated = false;
        for (String marker : APPLICATION_MARKERS) {
            if (fullContent.contains(marker)) {
                isApplicationRelated = true;
                break;
            }
        }
        for (String interviewMarker : INTERVIEW_KEYWORDS) {
            if (fullContent.contains(interviewMarker)) {
                isApplicationRelated = true;
                break;
            }
        }
        for (String rejectionMarker : REJECTION_KEYWORDS) {
            if (fullContent.contains(rejectionMarker)) {
                isApplicationRelated = true;
                break;
            }
        }

        if (!isApplicationRelated) {
            return ParsedEmailApplication.irrelevant();
        }

        // 3. Classify Status
        ApplicationStatus status = classifyStatus(fullContent);

        // 4. Extract Company
        String company = extractCompany(from, subject, body);
        if (company == null || company.isBlank()) {
            return ParsedEmailApplication.irrelevant();
        }

        // 5. Extract Position / Role
        String position = extractRole(subject, body);
        if (position == null || position.isBlank()) {
            position = "General Application";
        }

        // 6. Extract Portal / Job URL
        String jobUrl = extractJobUrl(body);

        ParsedEmailApplication result = new ParsedEmailApplication();
        result.setRelevant(true);
        result.setCompany(cleanText(company));
        result.setPosition(cleanText(position));
        result.setStatus(status);
        result.setEmailDate(emailDate != null ? emailDate : LocalDate.now());
        result.setJobUrl(jobUrl);
        result.setNotes("Imported automatically from email: \"" + cleanText(subject) + "\"");

        return result;
    }

    private ApplicationStatus classifyStatus(String fullContent) {
        for (String keyword : OFFER_KEYWORDS) {
            if (fullContent.contains(keyword)) {
                return ApplicationStatus.OFFER;
            }
        }
        for (String keyword : REJECTION_KEYWORDS) {
            if (fullContent.contains(keyword)) {
                return ApplicationStatus.REJECTED;
            }
        }
        for (String keyword : INTERVIEW_KEYWORDS) {
            if (fullContent.contains(keyword)) {
                return ApplicationStatus.INTERVIEW;
            }
        }
        return ApplicationStatus.APPLIED;
    }

    private String extractCompany(String from, String subject, String body) {
        // Try subject first
        for (Pattern pattern : COMPANY_SUBJECT_PATTERNS) {
            Matcher m = pattern.matcher(subject);
            if (m.find()) {
                String match = m.group(1).trim();
                if (!match.isBlank()) {
                    return cleanCompanyCandidate(match);
                }
            }
        }

        // Try body patterns
        for (Pattern pattern : COMPANY_BODY_PATTERNS) {
            Matcher m = pattern.matcher(body);
            if (m.find()) {
                String match = m.group(1).trim();
                if (!match.isBlank()) {
                    return cleanCompanyCandidate(match);
                }
            }
        }

        // Fallback to sender display name
        String displayName = extractDisplayName(from);
        if (!displayName.isBlank()) {
            String cleaned = SENDER_NAME_CLEANER.matcher(displayName).replaceAll("").trim();
            cleaned = cleaned.replace("\"", "").replace("'", "").trim();
            if (!cleaned.isBlank() && !cleaned.equalsIgnoreCase("no-reply") && !cleaned.equalsIgnoreCase("notifications")) {
                return cleanCompanyCandidate(cleaned);
            }
        }

        return null;
    }

    private String extractRole(String subject, String body) {
        for (Pattern pattern : ROLE_PATTERNS) {
            Matcher m = pattern.matcher(subject);
            if (m.find()) {
                String match = m.group(1).trim();
                if (isValidRole(match)) {
                    return match;
                }
            }
        }

        for (Pattern pattern : ROLE_PATTERNS) {
            Matcher m = pattern.matcher(body);
            if (m.find()) {
                String match = m.group(1).trim();
                if (isValidRole(match)) {
                    return match;
                }
            }
        }

        return null;
    }

    private String extractJobUrl(String body) {
        Matcher m = PORTAL_URL_PATTERN.matcher(body);
        if (m.find()) {
            return m.group(0);
        }
        return null;
    }

    private String extractDisplayName(String from) {
        if (from == null) return "";
        int angleBracket = from.indexOf('<');
        if (angleBracket > 0) {
            return from.substring(0, angleBracket).trim();
        }
        int atIndex = from.indexOf('@');
        if (atIndex > 0) {
            return from.substring(0, atIndex).trim();
        }
        return from.trim();
    }

    private String cleanCompanyCandidate(String company) {
        String cleaned = company.replaceAll("(?i)<[^>]*>", "")
                .replaceAll("(?i)\\b(ltd|gmbh|inc|corp|llc|co)\\b\\.?", "")
                .trim();
        return cleaned;
    }

    private boolean isValidRole(String role) {
        if (role == null || role.isBlank()) return false;
        String lower = role.toLowerCase();
        return !lower.contains("http") && !lower.contains("www.") && role.length() <= 100;
    }

    private String cleanText(String text) {
        if (text == null) return null;
        return text.replaceAll("<[^>]*>", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
