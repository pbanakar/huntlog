package com.pbanakar.huntlog.dto.sync;

import java.time.LocalDate;

public class RawEmailMessage {

    private String messageId;
    private String from;
    private String subject;
    private String body;
    private LocalDate receivedDate;

    public RawEmailMessage() {
    }

    public RawEmailMessage(String messageId, String from, String subject, String body, LocalDate receivedDate) {
        this.messageId = messageId;
        this.from = from;
        this.subject = subject;
        this.body = body;
        this.receivedDate = receivedDate;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public LocalDate getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDate receivedDate) {
        this.receivedDate = receivedDate;
    }
}

