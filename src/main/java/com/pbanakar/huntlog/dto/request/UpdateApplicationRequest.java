package com.pbanakar.huntlog.dto.request;

import com.pbanakar.huntlog.enums.ApplicationStatus;
import jakarta.validation.constraints.Size;

public class UpdateApplicationRequest {

    private ApplicationStatus status;

    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    private String notes;

    @Size(max = 500, message = "Job URL must be at most 500 characters")
    private String jobUrl;

    @Size(max = 100, message = "Location must be at most 100 characters")
    private String location;

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public void setJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
