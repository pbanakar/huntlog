package com.pbanakar.huntlog.service;

import com.pbanakar.huntlog.dto.response.AnalyticsResponse;
import com.pbanakar.huntlog.dto.response.CompanyCountResponse;
import com.pbanakar.huntlog.enums.ApplicationStatus;
import com.pbanakar.huntlog.model.JobApplication;
import com.pbanakar.huntlog.repository.JobApplicationRepository;
import com.pbanakar.huntlog.repository.projection.CompanyCount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private JobApplicationRepository repository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private static class TestCompanyCount implements CompanyCount {
        private final String company;
        private final Long count;

        TestCompanyCount(String company, Long count) {
            this.company = company;
            this.count = count;
        }

        @Override
        public String getCompany() {
            return company;
        }

        @Override
        public Long getCount() {
            return count;
        }
    }

    @Test
    @DisplayName("1. Empty user with no applications returns all zeros, nulls, and empty lists")
    void getAnalytics_emptyUser_returnsDefaultZerosAndNulls() {
        Long userId = 1L;
        when(repository.countByUserId(userId)).thenReturn(0L);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            when(repository.countByUserIdAndStatus(userId, status)).thenReturn(0L);
        }
        when(repository.countByUserIdAndAppliedDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class))).thenReturn(0L);
        when(repository.findByUserIdAndStatusIn(eq(userId), anyList())).thenReturn(Collections.emptyList());
        when(repository.findCompanyCountsByUserId(userId)).thenReturn(Collections.emptyList());

        AnalyticsResponse response = analyticsService.getAnalytics(userId);

        assertNotNull(response);
        assertEquals(0L, response.getTotalApplications());
        assertEquals(0L, response.getAppliedThisWeek());
        assertEquals(0L, response.getAppliedThisMonth());
        assertEquals(0.0, response.getResponseRate());
        assertNull(response.getAverageDaysToFirstUpdate());
        assertNull(response.getOldestPendingDays());
        assertTrue(response.getTopCompaniesByApplications().isEmpty());
        assertEquals(7, response.getByStatus().size());
        for (ApplicationStatus status : ApplicationStatus.values()) {
            assertEquals(0L, response.getByStatus().get(status));
        }
    }

    @Test
    @DisplayName("2. Mixed applications: correctly computes total, byStatus, responseRate, averageDays, oldestPendingDays, topCompanies")
    void getAnalytics_mixedApplications_computesAllMetricsCorrectly() {
        Long userId = 1L;
        LocalDate today = LocalDate.now();

        // 10 total: 3 APPLIED, 2 SCREENING, 2 INTERVIEW, 1 OFFER, 1 ACCEPTED, 1 WITHDRAWN
        when(repository.countByUserId(userId)).thenReturn(10L);
        when(repository.countByUserIdAndStatus(userId, ApplicationStatus.APPLIED)).thenReturn(3L);
        when(repository.countByUserIdAndStatus(userId, ApplicationStatus.SCREENING)).thenReturn(2L);
        when(repository.countByUserIdAndStatus(userId, ApplicationStatus.INTERVIEW)).thenReturn(2L);
        when(repository.countByUserIdAndStatus(userId, ApplicationStatus.OFFER)).thenReturn(1L);
        when(repository.countByUserIdAndStatus(userId, ApplicationStatus.ACCEPTED)).thenReturn(1L);
        when(repository.countByUserIdAndStatus(userId, ApplicationStatus.REJECTED)).thenReturn(0L);
        when(repository.countByUserIdAndStatus(userId, ApplicationStatus.WITHDRAWN)).thenReturn(1L);

        when(repository.countByUserIdAndAppliedDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class))).thenReturn(4L);

        // Updated applications (non-APPLIED): 7 apps
        // App 1: applied 10 days ago, updated 6 days ago -> 4 days
        // App 2: applied 20 days ago, updated 12 days ago -> 8 days
        JobApplication app1 = new JobApplication();
        app1.setStatus(ApplicationStatus.SCREENING);
        app1.setAppliedDate(today.minusDays(10));
        app1.setLastUpdated(today.minusDays(6).atStartOfDay());

        JobApplication app2 = new JobApplication();
        app2.setStatus(ApplicationStatus.INTERVIEW);
        app2.setAppliedDate(today.minusDays(20));
        app2.setLastUpdated(today.minusDays(12).atStartOfDay());

        List<JobApplication> updatedApps = List.of(app1, app2);

        // Pending applications (APPLIED, SCREENING, INTERVIEW)
        JobApplication pending1 = new JobApplication();
        pending1.setStatus(ApplicationStatus.APPLIED);
        pending1.setAppliedDate(today.minusDays(25));

        JobApplication pending2 = new JobApplication();
        pending2.setStatus(ApplicationStatus.SCREENING);
        pending2.setAppliedDate(today.minusDays(15));

        List<JobApplication> pendingApps = List.of(pending1, pending2);

        when(repository.findByUserIdAndStatusIn(eq(userId), argThat(list -> list.contains(ApplicationStatus.SCREENING) && list.contains(ApplicationStatus.OFFER))))
                .thenReturn(updatedApps);

        when(repository.findByUserIdAndStatusIn(eq(userId), argThat(list -> list.contains(ApplicationStatus.APPLIED) && list.contains(ApplicationStatus.SCREENING) && !list.contains(ApplicationStatus.OFFER))))
                .thenReturn(pendingApps);

        List<CompanyCount> companyProjections = List.of(
                new TestCompanyCount("Google", 4L),
                new TestCompanyCount("Amazon", 3L),
                new TestCompanyCount("Meta", 2L),
                new TestCompanyCount("Netflix", 1L)
        );
        when(repository.findCompanyCountsByUserId(userId)).thenReturn(companyProjections);

        AnalyticsResponse response = analyticsService.getAnalytics(userId);

        assertEquals(10L, response.getTotalApplications());
        // responseRate = (10 - 3 APPLIED - 1 WITHDRAWN) / 10 * 100 = 6 / 10 * 100 = 60.0
        assertEquals(60.0, response.getResponseRate());
        // averageDaysToFirstUpdate = (4 + 8) / 2 = 6.0
        assertEquals(6.0, response.getAverageDaysToFirstUpdate());
        // oldestPendingDays = today - (today - 25) = 25
        assertEquals(25L, response.getOldestPendingDays());
        assertEquals(4, response.getTopCompaniesByApplications().size());
        assertEquals("Google", response.getTopCompaniesByApplications().get(0).getCompany());
        assertEquals(4L, response.getTopCompaniesByApplications().get(0).getCount());
    }

    @Test
    @DisplayName("3. responseRate with only APPLIED and WITHDRAWN is 0.0")
    void getAnalytics_onlyAppliedAndWithdrawn_responseRateIsZero() {
        Long userId = 1L;
        when(repository.countByUserId(userId)).thenReturn(5L);
        when(repository.countByUserIdAndStatus(userId, ApplicationStatus.APPLIED)).thenReturn(3L);
        when(repository.countByUserIdAndStatus(userId, ApplicationStatus.WITHDRAWN)).thenReturn(2L);
        when(repository.countByUserIdAndStatus(userId, ApplicationStatus.SCREENING)).thenReturn(0L);
        when(repository.countByUserIdAndStatus(userId, ApplicationStatus.INTERVIEW)).thenReturn(0L);
        when(repository.countByUserIdAndStatus(userId, ApplicationStatus.OFFER)).thenReturn(0L);
        when(repository.countByUserIdAndStatus(userId, ApplicationStatus.ACCEPTED)).thenReturn(0L);
        when(repository.countByUserIdAndStatus(userId, ApplicationStatus.REJECTED)).thenReturn(0L);

        when(repository.findByUserIdAndStatusIn(eq(userId), anyList())).thenReturn(Collections.emptyList());
        when(repository.findCompanyCountsByUserId(userId)).thenReturn(Collections.emptyList());

        AnalyticsResponse response = analyticsService.getAnalytics(userId);

        // (5 - 3 - 2) / 5 * 100 = 0.0
        assertEquals(0.0, response.getResponseRate());
    }

    @Test
    @DisplayName("4. averageDaysToFirstUpdate is null when all applications are in APPLIED status")
    void getAnalytics_allApplied_averageDaysToFirstUpdateIsNull() {
        Long userId = 1L;
        when(repository.countByUserId(userId)).thenReturn(4L);
        when(repository.countByUserIdAndStatus(userId, ApplicationStatus.APPLIED)).thenReturn(4L);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            if (status != ApplicationStatus.APPLIED) {
                when(repository.countByUserIdAndStatus(userId, status)).thenReturn(0L);
            }
        }

        // No updated apps found
        when(repository.findByUserIdAndStatusIn(eq(userId), argThat(list -> list.contains(ApplicationStatus.SCREENING) && list.contains(ApplicationStatus.OFFER))))
                .thenReturn(Collections.emptyList());

        JobApplication appliedApp = new JobApplication();
        appliedApp.setStatus(ApplicationStatus.APPLIED);
        appliedApp.setAppliedDate(LocalDate.now().minusDays(5));
        when(repository.findByUserIdAndStatusIn(eq(userId), argThat(list -> list.contains(ApplicationStatus.APPLIED) && !list.contains(ApplicationStatus.OFFER))))
                .thenReturn(List.of(appliedApp));

        when(repository.findCompanyCountsByUserId(userId)).thenReturn(Collections.emptyList());

        AnalyticsResponse response = analyticsService.getAnalytics(userId);

        assertNull(response.getAverageDaysToFirstUpdate());
        assertEquals(5L, response.getOldestPendingDays());
    }

    @Test
    @DisplayName("5. topCompaniesByApplications returns at most 5 even if user has 10 distinct companies")
    void getAnalytics_manyCompanies_limitsToTop5() {
        Long userId = 1L;
        when(repository.countByUserId(userId)).thenReturn(10L);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            when(repository.countByUserIdAndStatus(userId, status)).thenReturn(0L);
        }
        when(repository.findByUserIdAndStatusIn(eq(userId), anyList())).thenReturn(Collections.emptyList());

        List<CompanyCount> tenCompanies = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            tenCompanies.add(new TestCompanyCount("Company" + i, (long) (11 - i)));
        }
        when(repository.findCompanyCountsByUserId(userId)).thenReturn(tenCompanies);

        AnalyticsResponse response = analyticsService.getAnalytics(userId);

        List<CompanyCountResponse> top5 = response.getTopCompaniesByApplications();
        assertEquals(5, top5.size());
        assertEquals("Company1", top5.get(0).getCompany());
        assertEquals(10L, top5.get(0).getCount());
        assertEquals("Company5", top5.get(4).getCompany());
        assertEquals(6L, top5.get(4).getCount());
    }

    @Test
    @DisplayName("6. oldestPendingDays correctly identifies the oldest APPLIED/SCREENING/INTERVIEW application")
    void getAnalytics_oldestPendingDays_identifiesOldestAcrossPendingStages() {
        Long userId = 1L;
        LocalDate today = LocalDate.now();

        when(repository.countByUserId(userId)).thenReturn(3L);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            when(repository.countByUserIdAndStatus(userId, status)).thenReturn(0L);
        }
        when(repository.findByUserIdAndStatusIn(eq(userId), argThat(list -> list.contains(ApplicationStatus.SCREENING) && list.contains(ApplicationStatus.OFFER))))
                .thenReturn(Collections.emptyList());

        JobApplication appApplied = new JobApplication();
        appApplied.setStatus(ApplicationStatus.APPLIED);
        appApplied.setAppliedDate(today.minusDays(14));

        JobApplication appScreening = new JobApplication();
        appScreening.setStatus(ApplicationStatus.SCREENING);
        appScreening.setAppliedDate(today.minusDays(30)); // oldest

        JobApplication appInterview = new JobApplication();
        appInterview.setStatus(ApplicationStatus.INTERVIEW);
        appInterview.setAppliedDate(today.minusDays(7));

        when(repository.findByUserIdAndStatusIn(eq(userId), argThat(list -> list.contains(ApplicationStatus.APPLIED) && !list.contains(ApplicationStatus.OFFER))))
                .thenReturn(List.of(appApplied, appScreening, appInterview));

        when(repository.findCompanyCountsByUserId(userId)).thenReturn(Collections.emptyList());

        AnalyticsResponse response = analyticsService.getAnalytics(userId);

        assertEquals(30L, response.getOldestPendingDays());
    }
}
