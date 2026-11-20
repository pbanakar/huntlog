package com.pbanakar.huntlog.service;

import com.pbanakar.huntlog.dto.sync.ParsedEmailApplication;
import com.pbanakar.huntlog.enums.ApplicationStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
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
            "unsubscribe from job alerts",
            "[discuss] fip-", // Apache mailing lists (e.g. Flink/Iceberg proposals)
            "apache.org"
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
            "invitation to explore opportunities",
            "invitation to explore",
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
            "thank you for taking the time to apply",
            "we've received your application",
            "we have received your application",
            "thanks for completing your application",
            "application for",
            "applied to",
            "application status",
            "recent application",
            "internship application",
            "talent community",
            "regarding your",
            "application update",
            "interest for the",
            "interest in the",
            "explore opportunities at"
    );

    private static final Set<String> INVALID_COMPANIES = Set.of(
            "human resources",
            "hr",
            "recruiting",
            "recruiting team",
            "talent",
            "talent acquisition",
            "careers",
            "jobs",
            "no-reply",
            "notifications",
            "entry-level",
            "entry-level talent recruiting",
            "candidate",
            "applicant",
            "people team"
    );

    // --- Heuristic Regular Expressions ---
    private static final Pattern SENDER_NAME_CLEANER = Pattern.compile(
            "(?i)\\s*(people team|hiring team|recruiting team|talent recruiting|talent acquisition|talent team|campus recruiting|jobs|hr team|human resources|hr|via bob|@ icims|candidate center).*"
    );

    private static final List<Pattern> COMPANY_SUBJECT_PATTERNS = List.of(
            // Regarding your Honeywell Application for... / Update on your Humana Application... / Your Humana application
            Pattern.compile("(?i)(?:regarding\\s+(?:your\\s+)?|an\\s+update\\s+on\\s+(?:your\\s+)?|update\\s+on\\s+(?:your\\s+)?|update\\s+regarding\\s+(?:your\\s+)?|your\\s+)([A-Za-z0-9&._ -]{2,40}?)\\s+application"),
            // Thank you for applying to / at Chainlink Labs
            Pattern.compile("(?i)thank you for applying\\s+(?:to|at)\\s+([A-Za-z0-9&._ -]{2,40}?)(?:\\s+for|!|\\.|$)"),
            // Application received: ... at / @ Ververica
            Pattern.compile("(?i)application received:.*?\\s+(?:at|@)\\s+([A-Za-z0-9&._ -]{2,40}?)(?:!|\\.|$)"),
            // Interest in PromptQL / Cloudera
            Pattern.compile("(?i)interest in\\s+([A-Za-z0-9&._ -]{2,40}?)(?:!|\\.|$)"),
            // position with Cloudera
            Pattern.compile("(?i)position with\\s+([A-Za-z0-9&._ -]{2,40}?)(?:!|\\.|$)"),
            // opportunities at Wyra Labs
            Pattern.compile("(?i)opportunities at\\s+([A-Za-z0-9&._ -]{2,40}?)(?:!|\\.|$)"),
            // Visa - Application Update / Cisco - Application Status Update
            Pattern.compile("(?i)^([A-Za-z0-9&._ -]{2,30}?)\\s*-\\s*application(?:\\s+update|\\s+status)"),
            // Entry-level Talent Recruiting - Cisco Application Status Update
            Pattern.compile("(?i)(?:recruiting|talent|careers|team)\\s*-\\s*([A-Za-z0-9&._ -]{2,30}?)\\s+application"),
            // Company: thanks for completing
            Pattern.compile("(?i)^([A-Za-z0-9&._ -]{2,30}?):\\s+thanks for completing")
    );

    private static final List<Pattern> COMPANY_BODY_PATTERNS = List.of(
            Pattern.compile("(?i)thank you for applying\\s+(?:to|at)\\s+([A-Za-z0-9&._ -]{2,40}?)(?:\\s+for|\\s+to|!|\\.|,)"),
            Pattern.compile("(?i)position\\s+(?:at|with)\\s+([A-Za-z0-9&._ -]{2,40}?)(?:!|\\.|,)"),
            Pattern.compile("(?i)interest in\\s+([A-Za-z0-9&._ -]{2,40}?)(?:!|\\.|,)"),
            Pattern.compile("(?i)application to\\s+([A-Za-z0-9&._ -]{2,40}?)\\s+for the role of"),
            Pattern.compile("(?i)application to the.*?position at\\s+([A-Za-z0-9&._ -]{2,40}?)(?:!|\\.|,)"),
            Pattern.compile("(?i)talent community at\\s+([A-Za-z0-9&._ -]{2,40}?)(?:!|\\.|,)")
    );

    private static final List<Pattern> ROLE_PATTERNS = List.of(
            // Regarding your Honeywell Application for Intern Bachelors Software Eng - 160491
            Pattern.compile("(?i)application for\\s+([A-Za-z0-9&._, /()#-]{2,100}?)(?:\\s+received|\\s*[-–]\\s*Prajwal|$|!)"),
            // An Update on Your Humana Application: R-434927 Data Engineer
            Pattern.compile("(?i)application:\\s*([A-Za-z0-9&._, /()#-]{2,100})"),
            // Application received: Technical Support Engineer at...
            Pattern.compile("(?i)application received:\\s+([A-Za-z0-9&._, /()#-]{2,100}?)(?:\\s+(?:at|@|for)|$)"),
            // interest for the Software Engineering Intern - Cloud and Platform Systems 261521 position
            // interest in the Intern position
            // applying to the Software QA Engineer position
            // apply for the Data Platform Engineer position
            Pattern.compile("(?i)(?:interest (?:for|in)|applying (?:to|for)|apply (?:to|for))\\s+(?:the\\s+)?([A-Za-z0-9&._, /()#-]{2,100}?)\\s+(?:position|role)"),
            // for the role of Software Engineering Intern...
            Pattern.compile("(?i)for the role of\\s+([A-Za-z0-9&._, /()#-]{2,100})"),
            // for the Software Engineering Intern position
            Pattern.compile("(?i)for the\\s+([A-Za-z0-9&._, /()#-]{2,100}?)\\s+(?:position|role)"),
            // application to the Data Engineer Intern position
            Pattern.compile("(?i)application to the\\s+([A-Za-z0-9&._, /()#-]{2,100}?)\\s+(?:position|role)"),
            // applying for R-433591 Summer 2027 MBA Internship
            Pattern.compile("(?i)applying for\\s+(?:the\\s+)?([A-Za-z0-9&._, /()#-]{2,100}?)(?:\\s+at|\\s+position|\\.|!|,|\\n|$)"),
            // assessment for the role of ...
            Pattern.compile("(?i)assessment for (?:the role of\\s+)?([A-Za-z0-9&._, /()#-]{2,100}?)(?:\\.|!|,|\\n|$)"),
            // explore opportunities at Wyra Labs -> Opportunity
            Pattern.compile("(?i)explore (opportunities(?:\\s+at\\s+[A-Za-z0-9&._ -]+)?)"),
            // Talent community at PromptQL
            Pattern.compile("(?i)(talent community(?:\\s+at\\s+[A-Za-z0-9&._ -]+)?)")
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
                String cleaned = cleanCompanyCandidate(match);
                if (cleaned != null) {
                    return cleaned;
                }
            }
        }

        // Try body patterns
        for (Pattern pattern : COMPANY_BODY_PATTERNS) {
            Matcher m = pattern.matcher(body);
            if (m.find()) {
                String match = m.group(1).trim();
                String cleaned = cleanCompanyCandidate(match);
                if (cleaned != null) {
                    return cleaned;
                }
            }
        }

        // Fallback to sender display name
        String displayName = extractDisplayName(from);
        if (!displayName.isBlank()) {
            String cleaned = SENDER_NAME_CLEANER.matcher(displayName).replaceAll("").trim();
            cleaned = cleaned.replace("\"", "").replace("'", "").trim();
            String candidate = cleanCompanyCandidate(cleaned);
            if (candidate != null) {
                return candidate;
            }
        }

        // Fallback to sender email address / domain
        return extractCompanyFromEmailAddress(from);
    }

    private String extractCompanyFromEmailAddress(String from) {
        if (from == null) return null;
        Matcher m = Pattern.compile("<([^@>]+)@([^>]+)>").matcher(from);
        if (!m.find()) {
            m = Pattern.compile("([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})").matcher(from);
            if (!m.find()) return null;
        }
        String userPart = m.group(1).toLowerCase();
        String domainPart = m.group(2).toLowerCase();

        // Check if ATS portal like honeywell@myworkday.com
        if (domainPart.contains("myworkday") || domainPart.contains("greenhouse") || domainPart.contains("lever") || domainPart.contains("icims") || domainPart.contains("ashby")) {
            if (!INVALID_COMPANIES.contains(userPart) && userPart.length() >= 3 && !userPart.equals("no-reply")) {
                return capitalize(userPart);
            }
        }

        // Direct company domain like no-reply@visa.com, jobs@cloudera.com
        String[] parts = domainPart.split("\\.");
        if (parts.length >= 2) {
            String base = parts[parts.length - 2];
            if (!INVALID_COMPANIES.contains(base) && !base.equals("gmail") && !base.equals("yahoo") && !base.equals("outlook") && base.length() >= 3) {
                return capitalize(base);
            }
        }
        return null;
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private String extractRole(String subject, String body) {
        for (Pattern pattern : ROLE_PATTERNS) {
            Matcher m = pattern.matcher(subject);
            if (m.find()) {
                String match = m.group(1).trim();
                String cleaned = cleanRole(match);
                if (isValidRole(cleaned)) {
                    return cleaned;
                }
            }
        }

        for (Pattern pattern : ROLE_PATTERNS) {
            Matcher m = pattern.matcher(body);
            if (m.find()) {
                String match = m.group(1).trim();
                String cleaned = cleanRole(match);
                if (isValidRole(cleaned)) {
                    return cleaned;
                }
            }
        }

        return null;
    }

    private String cleanRole(String role) {
        if (role == null) return null;
        String cleaned = role.replaceAll("(?i)\\s+(?:at|with)\\s+[A-Za-z0-9&._ -]{2,40}$", "")
                .replaceAll("^[-–—:;!\\s]+|[-–—:;!\\s]+$", "")
                .trim();
        return cleanText(cleaned);
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
        if (company == null) return null;
        String cleaned = company.replaceAll("(?i)<[^>]*>", "")
                .replaceAll("(?i)\\b(ltd|gmbh|inc|corp|llc|co)\\b\\.?", "")
                .replaceAll("(?i)\\b(people team|hiring team|recruiting team|talent recruiting|talent acquisition|talent team|campus recruiting|jobs|hr team|human resources|hr)\\b", "")
                .replaceAll("(?i)^an\\s+update\\s+on\\s+(?:your\\s+)?", "")
                .replaceAll("(?i)^your\\s+", "")
                .replaceAll("\\s*\\d+$", "")
                .replaceAll("^[-–—.,:;!\\s]+|[-–—.,:;!\\s]+$", "")
                .trim();

        if (cleaned.isBlank() || INVALID_COMPANIES.contains(cleaned.toLowerCase())) {
            return null;
        }
        return cleaned;
    }

    private boolean isValidRole(String role) {
        if (role == null || role.isBlank()) return false;
        String lower = role.toLowerCase();
        return !lower.contains("http") && !lower.contains("www.") && role.length() <= 100 && !INVALID_COMPANIES.contains(lower);
    }

    private String cleanText(String text) {
        if (text == null) return null;
        return text.replaceAll("<[^>]*>", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
