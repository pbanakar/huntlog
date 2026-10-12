package com.pbanakar.huntlog.controller;

import com.pbanakar.huntlog.dto.response.AnalyticsResponse;
import com.pbanakar.huntlog.security.SecurityUtils;
import com.pbanakar.huntlog.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics",
        description = "Job search statistics for the authenticated user")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    @Operation(summary = "Get job search analytics",
            description = "Returns statistics including total applications, breakdown by status, response rate, weekly/monthly counts, and top companies.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully")
    public ResponseEntity<AnalyticsResponse> getAnalytics() {
        Long userId = SecurityUtils.getCurrentUserId();
        AnalyticsResponse response = analyticsService.getAnalytics(userId);
        return ResponseEntity.ok(response);
    }
}
