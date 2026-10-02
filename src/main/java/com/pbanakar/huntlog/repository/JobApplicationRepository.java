package com.pbanakar.huntlog.repository;

import com.pbanakar.huntlog.enums.ApplicationStatus;
import com.pbanakar.huntlog.model.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    /**
     * Filtered search that handles null params gracefully:
     * - When status is null, the status filter is bypassed.
     * - When company is null, the company filter is bypassed.
     * - Company matching is case-insensitive LIKE.
     */
    @Query("SELECT j FROM JobApplication j WHERE " +
           "(:status IS NULL OR j.status = :status) AND " +
           "(:company IS NULL OR LOWER(j.company) LIKE LOWER(CONCAT('%', :company, '%')))")
    Page<JobApplication> findAllWithFilters(@Param("status") ApplicationStatus status,
                                            @Param("company") String company,
                                            Pageable pageable);
}
