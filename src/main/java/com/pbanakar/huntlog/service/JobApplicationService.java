package com.pbanakar.huntlog.service;

import com.pbanakar.huntlog.dto.request.CreateApplicationRequest;
import com.pbanakar.huntlog.dto.request.UpdateApplicationRequest;
import com.pbanakar.huntlog.dto.response.ApplicationResponse;
import com.pbanakar.huntlog.enums.ApplicationStatus;
import com.pbanakar.huntlog.exception.ResourceNotFoundException;
import com.pbanakar.huntlog.model.JobApplication;
import com.pbanakar.huntlog.repository.JobApplicationRepository;
import com.pbanakar.huntlog.statemachine.ApplicationStateMachine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;

@Service
public class JobApplicationService {

    private final JobApplicationRepository repository;
    private final ApplicationStateMachine stateMachine;

    public JobApplicationService(JobApplicationRepository repository,
                                  ApplicationStateMachine stateMachine) {
        this.repository = repository;
        this.stateMachine = stateMachine;
    }

    @Transactional
    public ApplicationResponse create(CreateApplicationRequest request) {
        JobApplication app = new JobApplication();
        app.setCompany(request.getCompany());
        app.setRole(request.getRole());
        app.setStatus(ApplicationStatus.APPLIED);
        app.setAppliedDate(request.getAppliedDate() != null ? request.getAppliedDate() : LocalDate.now());
        app.setJobUrl(request.getJobUrl());
        app.setNotes(request.getNotes());
        app.setLocation(request.getLocation());

        JobApplication saved = repository.save(app);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> findAll(ApplicationStatus status, String company, Pageable pageable) {
        return repository.findAllWithFilters(status, company, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse findById(Long id) {
        JobApplication app = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
        return toResponse(app);
    }

    @Transactional
    public ApplicationResponse update(Long id, UpdateApplicationRequest request) {
        JobApplication app = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        if (request.getStatus() != null) {
            stateMachine.validate(app.getStatus(), request.getStatus());
            app.setStatus(request.getStatus());
        }

        if (request.getNotes() != null) {
            app.setNotes(request.getNotes());
        }
        if (request.getJobUrl() != null) {
            app.setJobUrl(request.getJobUrl());
        }
        if (request.getLocation() != null) {
            app.setLocation(request.getLocation());
        }

        JobApplication saved = repository.save(app);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        JobApplication app = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
        repository.delete(app);
    }

    private ApplicationResponse toResponse(JobApplication app) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(app.getId());
        response.setCompany(app.getCompany());
        response.setRole(app.getRole());
        response.setStatus(app.getStatus());
        response.setAppliedDate(app.getAppliedDate());
        response.setLastUpdated(app.getLastUpdated());
        response.setJobUrl(app.getJobUrl());
        response.setNotes(app.getNotes());
        response.setLocation(app.getLocation());
        response.setAllowedNextStatuses(
                new ArrayList<>(stateMachine.getAllowedTransitions(app.getStatus()))
        );
        return response;
    }
}
