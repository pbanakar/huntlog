package com.pbanakar.huntlog.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Request body to create a new job application")
public class CreateApplicationRequest {

    @NotBlank(message = "Company is required")
    @Size(max = 100, message = "Company must be at most 100 characters")
    @Schema(example = "Google", description = "Company name")
    private String company;

    @NotBlank(message = "Role is required")
    @Size(max = 100, message = "Role must be at most 100 characters")
    @Schema(example = "Software Engineer I", description = "Job role or title")
    private String role;

    @Size(max = 500, message = "Job URL must be at most 500 characters")
    @Schema(example = "https://careers.google.com/123", description = "URL of the job posting")
    private String jobUrl;

    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    @Schema(example = "Applied via referral", description = "Additional notes or context")
    private String notes;

    @Size(max = 100, message = "Location must be at most 100 characters")
    @Schema(example = "Mountain View, CA", description = "Work location or remote status")
    private String location;

    @Schema(description = "Date application was submitted (defaults to today if null)")
    private LocalDate appliedDate;

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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getAppliedDate() {
        return appliedDate;
    }

    public void setAppliedDate(LocalDate appliedDate) {
        this.appliedDate = appliedDate;
    }
}
