package com.pbanakar.huntlog.service;

import com.pbanakar.huntlog.dto.request.CreateApplicationRequest;
import com.pbanakar.huntlog.dto.request.UpdateApplicationRequest;
import com.pbanakar.huntlog.dto.response.ApplicationResponse;
import com.pbanakar.huntlog.enums.ApplicationStatus;
import com.pbanakar.huntlog.exception.InvalidStatusTransitionException;
import com.pbanakar.huntlog.exception.ResourceNotFoundException;
import com.pbanakar.huntlog.model.JobApplication;
import com.pbanakar.huntlog.repository.JobApplicationRepository;
import com.pbanakar.huntlog.statemachine.ApplicationStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @Mock
    private JobApplicationRepository repository;

    @Spy
    private ApplicationStateMachine stateMachine = new ApplicationStateMachine();

    @InjectMocks
    private JobApplicationService service;

    private JobApplication existingApp;

    @BeforeEach
    void setUp() {
        existingApp = new JobApplication();
        existingApp.setId(1L);
        existingApp.setCompany("Google");
        existingApp.setRole("SWE");
        existingApp.setStatus(ApplicationStatus.APPLIED);
        existingApp.setAppliedDate(LocalDate.of(2026, 9, 21));
    }

    @Test
    @DisplayName("Creating application sets status=APPLIED and appliedDate=today when not provided")
    void create_setsDefaultStatusAndDate() {
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setCompany("Meta");
        request.setRole("SDE");

        when(repository.save(any(JobApplication.class))).thenAnswer(invocation -> {
            JobApplication app = invocation.getArgument(0);
            app.setId(2L);
            return app;
        });

        ApplicationResponse response = service.create(request);

        ArgumentCaptor<JobApplication> captor = ArgumentCaptor.forClass(JobApplication.class);
        verify(repository).save(captor.capture());

        JobApplication saved = captor.getValue();
        assertEquals(ApplicationStatus.APPLIED, saved.getStatus());
        assertEquals(LocalDate.now(), saved.getAppliedDate());
        assertEquals("Meta", response.getCompany());
    }

    @Test
    @DisplayName("Valid status transition (APPLIED → SCREENING) succeeds and saves")
    void update_validTransition_succeeds() {
        when(repository.findById(1L)).thenReturn(Optional.of(existingApp));
        when(repository.save(any(JobApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateApplicationRequest request = new UpdateApplicationRequest();
        request.setStatus(ApplicationStatus.SCREENING);

        ApplicationResponse response = service.update(1L, request);

        assertEquals(ApplicationStatus.SCREENING, response.getStatus());
        verify(stateMachine).validate(ApplicationStatus.APPLIED, ApplicationStatus.SCREENING);
        verify(repository).save(existingApp);
    }

    @Test
    @DisplayName("Invalid transition (APPLIED → OFFER) throws InvalidStatusTransitionException")
    void update_invalidTransition_throws() {
        when(repository.findById(1L)).thenReturn(Optional.of(existingApp));

        UpdateApplicationRequest request = new UpdateApplicationRequest();
        request.setStatus(ApplicationStatus.OFFER);

        InvalidStatusTransitionException ex = assertThrows(
                InvalidStatusTransitionException.class,
                () -> service.update(1L, request)
        );
        assertEquals(ApplicationStatus.APPLIED, ex.getFrom());
        assertEquals(ApplicationStatus.OFFER, ex.getTo());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Invalid transition from terminal state (ACCEPTED → anything) throws")
    void update_terminalState_throws() {
        existingApp.setStatus(ApplicationStatus.ACCEPTED);
        when(repository.findById(1L)).thenReturn(Optional.of(existingApp));

        UpdateApplicationRequest request = new UpdateApplicationRequest();
        request.setStatus(ApplicationStatus.REJECTED);

        assertThrows(InvalidStatusTransitionException.class, () -> service.update(1L, request));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Fetching non-existent id throws ResourceNotFoundException")
    void findById_notFound_throws() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(999L));
    }

    @Test
    @DisplayName("allowedNextStatuses is empty for terminal states")
    void response_terminalState_emptyAllowed() {
        existingApp.setStatus(ApplicationStatus.ACCEPTED);
        when(repository.findById(1L)).thenReturn(Optional.of(existingApp));

        ApplicationResponse response = service.findById(1L);
        assertTrue(response.getAllowedNextStatuses().isEmpty());

        // Also test REJECTED
        existingApp.setStatus(ApplicationStatus.REJECTED);
        response = service.findById(1L);
        assertTrue(response.getAllowedNextStatuses().isEmpty());

        // Also test WITHDRAWN
        existingApp.setStatus(ApplicationStatus.WITHDRAWN);
        response = service.findById(1L);
        assertTrue(response.getAllowedNextStatuses().isEmpty());
    }

    @Test
    @DisplayName("allowedNextStatuses correctly lists options for APPLIED")
    void response_applied_listsCorrectStatuses() {
        when(repository.findById(1L)).thenReturn(Optional.of(existingApp));

        ApplicationResponse response = service.findById(1L);

        assertEquals(3, response.getAllowedNextStatuses().size());
        assertTrue(response.getAllowedNextStatuses().contains(ApplicationStatus.SCREENING));
        assertTrue(response.getAllowedNextStatuses().contains(ApplicationStatus.REJECTED));
        assertTrue(response.getAllowedNextStatuses().contains(ApplicationStatus.WITHDRAWN));
    }
}
