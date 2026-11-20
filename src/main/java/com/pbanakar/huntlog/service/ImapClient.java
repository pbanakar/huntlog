package com.pbanakar.huntlog.service;

import com.pbanakar.huntlog.dto.sync.RawEmailMessage;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Component
public class ImapClient {

    private static final Logger logger = LoggerFactory.getLogger(ImapClient.class);

    private static final List<String> SEARCH_KEYWORDS = List.of(
            "application",
            "applied",
            "applying",
            "interview",
            "assessment",
            "status",
            "offer",
            "career",
            "careers",
            "recruiting",
            "recruiter",
            "intern",
            "internship",
            "position",
            "role",
            "opportunity",
            "opportunities",
            "interest",
            "hiring",
            "invitation",
            "job",
            "update"
    );

    private static final List<String> SENDER_KEYWORDS = List.of(
            "greenhouse",
            "lever",
            "workday",
            "icims",
            "ashby",
            "smartrecruiters",
            "keka",
            "hibob",
            "recruiting",
            "careers"
    );

    /**
     * Connects to the specified IMAP server over SSL/TLS, searches recent matching emails,
     * and returns normalized raw email message representations.
     */
    public List<RawEmailMessage> fetchMessages(String host, int port, String username, String password, LocalDate sinceDate, int maxMessages) throws Exception {
        if (host == null || host.isBlank()) {
            host = "imap.gmail.com";
        }
        if (port <= 0) {
            port = 993;
        }

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", host);
        props.put("mail.imaps.port", String.valueOf(port));
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.timeout", "15000");
        props.put("mail.imaps.connectiontimeout", "15000");

        Session session = Session.getInstance(props);
        Store store = null;
        Folder inbox = null;
        List<RawEmailMessage> results = new ArrayList<>();

        try {
            store = session.getStore("imaps");
            store.connect(host, port, username, password);

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Message[] messages;
            if (sinceDate != null) {
                Date comparisonDate = Date.from(sinceDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                SearchTerm dateTerm = new SentDateTerm(ComparisonTerm.GE, comparisonDate);

                SearchTerm keywordOr = buildKeywordSearchTerm();
                SearchTerm combinedTerm = new AndTerm(dateTerm, keywordOr);

                messages = inbox.search(combinedTerm);
            } else {
                SearchTerm keywordOr = buildKeywordSearchTerm();
                messages = inbox.search(keywordOr);
            }

            int count = messages.length;
            int start = Math.max(0, count - maxMessages);

            for (int i = count - 1; i >= start; i--) {
                Message message = messages[i];
                try {
                    String from = "";
                    Address[] fromAddrs = message.getFrom();
                    if (fromAddrs != null && fromAddrs.length > 0) {
                        from = fromAddrs[0].toString();
                    }

                    String subject = message.getSubject() != null ? message.getSubject() : "";
                    LocalDate receivedDate = message.getSentDate() != null
                            ? message.getSentDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                            : LocalDate.now();

                    String body = extractTextFromMessage(message);
                    String messageId = message.getHeader("Message-ID") != null && message.getHeader("Message-ID").length > 0
                            ? message.getHeader("Message-ID")[0]
                            : UUID.randomUUID().toString();

                    results.add(new RawEmailMessage(messageId, from, subject, body, receivedDate));
                } catch (Exception ex) {
                    logger.warn("Failed to parse individual IMAP message: {}", ex.getMessage());
                }
            }
        } finally {
            if (inbox != null && inbox.isOpen()) {
                try {
                    inbox.close(false);
                } catch (Exception ignored) {
                }
            }
            if (store != null && store.isConnected()) {
                try {
                    store.close();
                } catch (Exception ignored) {
                }
            }
        }

        return results;
    }

    private SearchTerm buildKeywordSearchTerm() {
        SearchTerm term = new SubjectTerm(SEARCH_KEYWORDS.get(0));
        for (int i = 1; i < SEARCH_KEYWORDS.size(); i++) {
            term = new OrTerm(term, new SubjectTerm(SEARCH_KEYWORDS.get(i)));
        }
        for (String sender : SENDER_KEYWORDS) {
            term = new OrTerm(term, new FromStringTerm(sender));
        }
        return term;
    }

    private String extractTextFromMessage(Part part) throws MessagingException, IOException {
        if (part.isMimeType("text/plain")) {
            return (String) part.getContent();
        }

        if (part.isMimeType("text/html")) {
            String html = (String) part.getContent();
            return html.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        }

        if (part.isMimeType("multipart/*")) {
            MimeMultipart multipart = (MimeMultipart) part.getContent();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                sb.append(" ").append(extractTextFromMessage(bodyPart));
            }
            return sb.toString().trim();
        }

        return "";
    }
}

