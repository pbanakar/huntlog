package com.pbanakar.huntlog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class CreateApplicationRequest {

    @NotBlank(message = "Company is required")
    @Size(max = 100, message = "Company must be at most 100 characters")
    private String company;

    @NotBlank(message = "Role is required")
    @Size(max = 100, message = "Role must be at most 100 characters")
    private String role;

    @Size(max = 500, message = "Job URL must be at most 500 characters")
    private String jobUrl;

    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    private String notes;

    @Size(max = 100, message = "Location must be at most 100 characters")
    private String location;

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
