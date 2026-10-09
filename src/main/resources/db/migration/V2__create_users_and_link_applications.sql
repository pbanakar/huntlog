-- ============================================================================
-- V2__create_users_and_link_applications.sql
-- ============================================================================
-- Phase 2: JWT Authentication & Per-User Data Isolation (Updated for PostgreSQL)
--
-- This migration:
--   1. Creates the `users` table for authentication
--   2. Adds a `user_id` foreign key to `job_applications`
--   3. Indexes `user_id` on `job_applications` for query performance
--
-- Index rationale: Every authenticated query filters applications by
-- user_id (WHERE user_id = ?). Without this index, PostgreSQL would perform
-- a full table scan on every GET /applications request. With the index,
-- lookups are O(log N) via B-tree, which keeps query latency low.
-- ============================================================================

CREATE TABLE users (
    id         BIGSERIAL    PRIMARY KEY,
    email      VARCHAR(150) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMP    NOT NULL
);

-- Add user ownership to job applications.
-- Since this is a development migration with no production data,
-- we truncate existing rows to allow NOT NULL constraint.
TRUNCATE TABLE job_applications;

ALTER TABLE job_applications
    ADD COLUMN user_id BIGINT NOT NULL;

ALTER TABLE job_applications
    ADD CONSTRAINT fk_job_applications_user
    FOREIGN KEY (user_id) REFERENCES users(id);

-- Index on user_id for fast per-user queries.
CREATE INDEX idx_job_applications_user_id ON job_applications(user_id);
