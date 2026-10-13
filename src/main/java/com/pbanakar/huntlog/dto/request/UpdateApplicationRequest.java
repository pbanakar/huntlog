package com.pbanakar.huntlog.dto.request;

import com.pbanakar.huntlog.enums.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body to update an existing job application")
public class UpdateApplicationRequest {

    @Schema(description = "Target application status. Transition is validated by the state machine.", example = "SCREENING")
    private ApplicationStatus status;

    @Size(max = 100, message = "Company must be at most 100 characters")
    @Schema(example = "Google", description = "Updated company name")
    private String company;

    @Size(max = 100, message = "Role must be at most 100 characters")
    @Schema(example = "Software Engineer I", description = "Updated job role or title")
    private String role;

    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    @Schema(example = "Completed technical phone screen", description = "Updated notes")
    private String notes;

    @Size(max = 500, message = "Job URL must be at most 500 characters")
    @Schema(example = "https://careers.google.com/123", description = "Updated job posting URL")
    private String jobUrl;

    @Size(max = 100, message = "Location must be at most 100 characters")
    @Schema(example = "Remote", description = "Updated work location")
    private String location;

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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
