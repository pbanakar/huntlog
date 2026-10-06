package com.pbanakar.huntlog.repository;

import com.pbanakar.huntlog.enums.ApplicationStatus;
import com.pbanakar.huntlog.model.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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

    /**
     * User-scoped filtered search — all application queries now filter by the
     * authenticated user's ID to enforce per-user data isolation.
     */
    @Query("SELECT j FROM JobApplication j WHERE j.user.id = :userId AND " +
           "(:status IS NULL OR j.status = :status) AND " +
           "(:company IS NULL OR LOWER(j.company) LIKE LOWER(CONCAT('%', :company, '%')))")
    Page<JobApplication> findAllByUserWithFilters(@Param("userId") Long userId,
                                                   @Param("status") ApplicationStatus status,
                                                   @Param("company") String company,
                                                   Pageable pageable);

    /**
     * Finds an application by its ID AND the owning user's ID.
     * Returns empty if the application doesn't exist OR belongs to another user,
     * ensuring we never reveal that another user's resource exists.
     */
    Optional<JobApplication> findByIdAndUserId(Long id, Long userId);

    Long countByUserId(Long userId);

    Long countByUserIdAndStatus(Long userId, ApplicationStatus status);

    Long countByUserIdAndAppliedDateBetween(Long userId, java.time.LocalDate start, java.time.LocalDate end);

    java.util.List<JobApplication> findByUserIdAndStatusIn(Long userId, java.util.List<ApplicationStatus> statuses);

    @Query("SELECT j.company AS company, COUNT(j) AS count FROM JobApplication j " +
           "WHERE j.user.id = :userId " +
           "GROUP BY j.company " +
           "ORDER BY COUNT(j) DESC, j.company ASC")
    java.util.List<com.pbanakar.huntlog.repository.projection.CompanyCount> findCompanyCountsByUserId(@Param("userId") Long userId);
}
