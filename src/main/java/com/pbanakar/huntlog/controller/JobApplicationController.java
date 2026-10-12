package com.pbanakar.huntlog.controller;

import com.pbanakar.huntlog.dto.request.CreateApplicationRequest;
import com.pbanakar.huntlog.dto.request.UpdateApplicationRequest;
import com.pbanakar.huntlog.dto.response.ApplicationResponse;
import com.pbanakar.huntlog.enums.ApplicationStatus;
import com.pbanakar.huntlog.service.JobApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/applications")
@Tag(name = "Job Applications",
        description = "Create and manage job applications with state machine-validated status transitions")
public class JobApplicationController {

    private final JobApplicationService service;

    public JobApplicationController(JobApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a new job application",
            description = "Creates an application with status=APPLIED. The response includes allowedNextStatuses showing valid next states.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponse(responseCode = "201", description = "Application created successfully")
    public ResponseEntity<ApplicationResponse> create(@Valid @RequestBody CreateApplicationRequest request) {
        ApplicationResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all applications",
            description = "Returns paginated applications for the authenticated user. Filter by status or company name.")
    @Parameter(name = "status",
            description = "Filter by application status",
            schema = @Schema(implementation = ApplicationStatus.class))
    @Parameter(name = "company",
            description = "Case-insensitive company name search")
    @Parameter(name = "page", example = "0")
    @Parameter(name = "size", example = "10")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Page<ApplicationResponse>> getAll(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String company,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") @Max(value = 50, message = "Page size must not exceed 50") int size,
            @RequestParam(defaultValue = "lastUpdated,desc") String sort) {

        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1
                ? Sort.Direction.fromString(sortParts[1])
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));

        Page<ApplicationResponse> result = service.findAll(status, company, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get application by ID")
    @ApiResponse(responseCode = "200", description = "Application found")
    @ApiResponse(responseCode = "404",
            description = "Application not found or belongs to another user")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApplicationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an application",
            description = "Update status, notes, or other fields. Status transitions are validated — invalid transitions return 422 with allowed transitions listed.")
    @ApiResponse(responseCode = "200", description = "Application updated successfully")
    @ApiResponse(responseCode = "404", description = "Application not found")
    @ApiResponse(responseCode = "422",
            description = "Invalid status transition")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<ApplicationResponse> update(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateApplicationRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an application")
    @ApiResponse(responseCode = "204",
            description = "Application deleted")
    @ApiResponse(responseCode = "404", description = "Application not found")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
