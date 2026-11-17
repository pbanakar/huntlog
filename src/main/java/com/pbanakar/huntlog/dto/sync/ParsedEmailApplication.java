package com.pbanakar.huntlog.dto.sync;

import com.pbanakar.huntlog.enums.ApplicationStatus;

import java.time.LocalDate;

/**
 * Data Transfer Object representing the structured information extracted
 * from a raw incoming or synced email.
 */
public class ParsedEmailApplication {

    private boolean relevant;
    private String company;
    private String position;
    private ApplicationStatus status;
    private LocalDate emailDate;
    private String jobUrl;
    private String notes;

    public ParsedEmailApplication() {
    }

    public static ParsedEmailApplication irrelevant() {
        ParsedEmailApplication parsed = new ParsedEmailApplication();
        parsed.setRelevant(false);
        return parsed;
    }

    public boolean isRelevant() {
        return relevant;
    }

    public void setRelevant(boolean relevant) {
        this.relevant = relevant;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDate getEmailDate() {
        return emailDate;
    }

    public void setEmailDate(LocalDate emailDate) {
        this.emailDate = emailDate;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public void setJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

