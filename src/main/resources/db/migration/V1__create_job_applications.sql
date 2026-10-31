-- ============================================================================
-- V1__create_job_applications.sql
-- ============================================================================
-- Phase 1: Core Job Application Schema (Updated for PostgreSQL & Railway)
--
-- Why Flyway over hibernate.ddl-auto=create?
--   1. Version-controlled schema: every migration is a numbered file in SCM,
--      so we know exactly what schema version any environment is running.
--   2. Safe for production: Flyway applies only NEW migrations, never drops
--      existing tables or data.
--   3. Rollback possible: we can write compensating V*__undo migrations or
--      use Flyway Teams' built-in undo support.
--   4. Team-friendly: multiple developers can add migrations without
--      conflicts, and CI ensures they apply cleanly.
-- ============================================================================

CREATE TABLE job_applications (
    id           BIGSERIAL PRIMARY KEY,
    company      VARCHAR(100) NOT NULL,
    role         VARCHAR(100) NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    applied_date DATE         NOT NULL,
    last_updated TIMESTAMP,
    job_url      VARCHAR(500),
    notes        TEXT,
    location     VARCHAR(100)
);
