package com.pbanakar.huntlog.controller;

import com.pbanakar.huntlog.dto.response.AnalyticsResponse;
import com.pbanakar.huntlog.security.SecurityUtils;
import com.pbanakar.huntlog.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public ResponseEntity<AnalyticsResponse> getAnalytics() {
        Long userId = SecurityUtils.getCurrentUserId();
        AnalyticsResponse response = analyticsService.getAnalytics(userId);
        return ResponseEntity.ok(response);
    }
}
