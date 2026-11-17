package com.pbanakar.huntlog.dto.sync;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Summary result of an email sync operation")
public class EmailSyncSummary {

    @Schema(description = "Number of emails scanned from the mailbox", example = "15")
    private int scannedCount;

    @Schema(description = "Number of new job applications created", example = "2")
    private int createdCount;

    @Schema(description = "Number of existing job applications updated", example = "1")
    private int updatedCount;

    @Schema(description = "Timestamp when sync finished")
    private LocalDateTime syncTime;

    @Schema(description = "Detailed log of created and updated applications")
    private List<String> details = new ArrayList<>();

    public EmailSyncSummary() {
        this.syncTime = LocalDateTime.now();
    }

    public EmailSyncSummary(int scannedCount, int createdCount, int updatedCount) {
        this.scannedCount = scannedCount;
        this.createdCount = createdCount;
        this.updatedCount = updatedCount;
        this.syncTime = LocalDateTime.now();
    }

    public void addDetail(String detail) {
        this.details.add(detail);
    }

    public int getScannedCount() {
        return scannedCount;
    }

    public void setScannedCount(int scannedCount) {
        this.scannedCount = scannedCount;
    }

    public int getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(int createdCount) {
        this.createdCount = createdCount;
    }

    public int getUpdatedCount() {
        return updatedCount;
    }

    public void setUpdatedCount(int updatedCount) {
        this.updatedCount = updatedCount;
    }

    public LocalDateTime getSyncTime() {
        return syncTime;
    }

    public void setSyncTime(LocalDateTime syncTime) {
        this.syncTime = syncTime;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }
}

