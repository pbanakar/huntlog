package com.pbanakar.huntlog.service;

import com.pbanakar.huntlog.dto.response.AnalyticsResponse;
import com.pbanakar.huntlog.dto.response.CompanyCountResponse;
import com.pbanakar.huntlog.enums.ApplicationStatus;
import com.pbanakar.huntlog.model.JobApplication;
import com.pbanakar.huntlog.repository.JobApplicationRepository;
import com.pbanakar.huntlog.repository.projection.CompanyCount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class AnalyticsService {

    private final JobApplicationRepository repository;

    private static final List<ApplicationStatus> NON_APPLIED_STATUSES = List.of(
            ApplicationStatus.SCREENING,
            ApplicationStatus.INTERVIEW,
            ApplicationStatus.OFFER,
            ApplicationStatus.ACCEPTED,
            ApplicationStatus.REJECTED,
            ApplicationStatus.WITHDRAWN
    );

    private static final List<ApplicationStatus> PENDING_STATUSES = List.of(
            ApplicationStatus.APPLIED,
            ApplicationStatus.SCREENING,
            ApplicationStatus.INTERVIEW
    );

    public AnalyticsService(JobApplicationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(Long userId) {
        LocalDate today = LocalDate.now();

        // 1. Total Applications
        Long totalApplications = repository.countByUserId(userId);
        if (totalApplications == null) {
            totalApplications = 0L;
        }

        // 2. Count by each of the 7 statuses
        Map<ApplicationStatus, Long> byStatus = new LinkedHashMap<>();
        for (ApplicationStatus status : ApplicationStatus.values()) {
            Long count = repository.countByUserIdAndStatus(userId, status);
            byStatus.put(status, count != null ? count : 0L);
        }

        // 3. Applied this week (Monday to today)
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Long appliedThisWeek = repository.countByUserIdAndAppliedDateBetween(userId, startOfWeek, today);
        if (appliedThisWeek == null) {
            appliedThisWeek = 0L;
        }

        // 4. Applied this month (First day to last day of current month)
        LocalDate startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth());
        Long appliedThisMonth = repository.countByUserIdAndAppliedDateBetween(userId, startOfMonth, endOfMonth);
        if (appliedThisMonth == null) {
            appliedThisMonth = 0L;
        }

        // 5. Response Rate: (total - APPLIED - WITHDRAWN) / total * 100
        double responseRate = 0.0;
        if (totalApplications > 0) {
            long appliedCount = byStatus.getOrDefault(ApplicationStatus.APPLIED, 0L);
            long withdrawnCount = byStatus.getOrDefault(ApplicationStatus.WITHDRAWN, 0L);
            double rawRate = ((double) (totalApplications - appliedCount - withdrawnCount) / totalApplications) * 100.0;
            responseRate = BigDecimal.valueOf(rawRate).setScale(1, RoundingMode.HALF_UP).doubleValue();
        }

        // 6. Average days to first update for applications with status != APPLIED
        List<JobApplication> updatedApps = repository.findByUserIdAndStatusIn(userId, NON_APPLIED_STATUSES);
        Double averageDaysToFirstUpdate = null;
        if (updatedApps != null && !updatedApps.isEmpty()) {
            double avg = updatedApps.stream()
                    .mapToLong(app -> {
                        LocalDate updatedDate = app.getLastUpdated() != null
                                ? app.getLastUpdated().toLocalDate()
                                : app.getAppliedDate();
                        return ChronoUnit.DAYS.between(app.getAppliedDate(), updatedDate);
                    })
                    .average()
                    .orElse(0.0);
            averageDaysToFirstUpdate = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).doubleValue();
        }

        // 7. Oldest pending days (APPLIED, SCREENING, INTERVIEW)
        List<JobApplication> pendingApps = repository.findByUserIdAndStatusIn(userId, PENDING_STATUSES);
        Long oldestPendingDays = null;
        if (pendingApps != null && !pendingApps.isEmpty()) {
            LocalDate oldestDate = pendingApps.stream()
                    .map(JobApplication::getAppliedDate)
                    .filter(Objects::nonNull)
                    .min(LocalDate::compareTo)
                    .orElse(null);
            if (oldestDate != null) {
                oldestPendingDays = ChronoUnit.DAYS.between(oldestDate, today);
            }
        }

        // 8. Top 5 companies by applications
        List<CompanyCount> companyCounts = repository.findCompanyCountsByUserId(userId);
        List<CompanyCountResponse> topCompanies = companyCounts != null
                ? companyCounts.stream()
                .limit(5)
                .map(c -> new CompanyCountResponse(c.getCompany(), c.getCount()))
                .toList()
                : Collections.emptyList();

        return new AnalyticsResponse(
                totalApplications,
                byStatus,
                appliedThisWeek,
                appliedThisMonth,
                responseRate,
                averageDaysToFirstUpdate,
                oldestPendingDays,
                topCompanies
        );
    }
}
