package com.pbanakar.huntlog.dto.response;

import com.pbanakar.huntlog.enums.ApplicationStatus;

import java.util.List;
import java.util.Map;

public class AnalyticsResponse {

    private Long totalApplications;
    private Map<ApplicationStatus, Long> byStatus;
    private Long appliedThisWeek;
    private Long appliedThisMonth;
    private Double responseRate;
    private Double averageDaysToFirstUpdate;
    private Long oldestPendingDays;
    private List<CompanyCountResponse> topCompaniesByApplications;

    public AnalyticsResponse() {
    }

    public AnalyticsResponse(Long totalApplications,
                             Map<ApplicationStatus, Long> byStatus,
                             Long appliedThisWeek,
                             Long appliedThisMonth,
                             Double responseRate,
                             Double averageDaysToFirstUpdate,
                             Long oldestPendingDays,
                             List<CompanyCountResponse> topCompaniesByApplications) {
        this.totalApplications = totalApplications;
        this.byStatus = byStatus;
        this.appliedThisWeek = appliedThisWeek;
        this.appliedThisMonth = appliedThisMonth;
        this.responseRate = responseRate;
        this.averageDaysToFirstUpdate = averageDaysToFirstUpdate;
        this.oldestPendingDays = oldestPendingDays;
        this.topCompaniesByApplications = topCompaniesByApplications;
    }

    public Long getTotalApplications() {
        return totalApplications;
    }

    public void setTotalApplications(Long totalApplications) {
        this.totalApplications = totalApplications;
    }

    public Map<ApplicationStatus, Long> getByStatus() {
        return byStatus;
    }

    public void setByStatus(Map<ApplicationStatus, Long> byStatus) {
        this.byStatus = byStatus;
    }

    public Long getAppliedThisWeek() {
        return appliedThisWeek;
    }

    public void setAppliedThisWeek(Long appliedThisWeek) {
        this.appliedThisWeek = appliedThisWeek;
    }

    public Long getAppliedThisMonth() {
        return appliedThisMonth;
    }

    public void setAppliedThisMonth(Long appliedThisMonth) {
        this.appliedThisMonth = appliedThisMonth;
    }

    public Double getResponseRate() {
        return responseRate;
    }

    public void setResponseRate(Double responseRate) {
        this.responseRate = responseRate;
    }

    public Double getAverageDaysToFirstUpdate() {
        return averageDaysToFirstUpdate;
    }

    public void setAverageDaysToFirstUpdate(Double averageDaysToFirstUpdate) {
        this.averageDaysToFirstUpdate = averageDaysToFirstUpdate;
    }

    public Long getOldestPendingDays() {
        return oldestPendingDays;
    }

    public void setOldestPendingDays(Long oldestPendingDays) {
        this.oldestPendingDays = oldestPendingDays;
    }

    public List<CompanyCountResponse> getTopCompaniesByApplications() {
        return topCompaniesByApplications;
    }

    public void setTopCompaniesByApplications(List<CompanyCountResponse> topCompaniesByApplications) {
        this.topCompaniesByApplications = topCompaniesByApplications;
    }
}
