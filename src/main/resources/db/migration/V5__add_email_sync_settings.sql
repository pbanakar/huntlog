-- ============================================================================
-- V5__add_email_sync_settings.sql
-- ============================================================================
-- Automated Email Ingestion & Application Tracking
--
-- Adds configuration columns to the `users` table for IMAP email auto-sync
-- (host, port, credentials, toggle, last sync checkpoint) and a `source`
-- column on `job_applications` to record the origin of each application.
-- ============================================================================

ALTER TABLE users
    ADD COLUMN email_sync_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN imap_host VARCHAR(150) DEFAULT 'imap.gmail.com',
    ADD COLUMN imap_port INT DEFAULT 993,
    ADD COLUMN imap_username VARCHAR(150),
    ADD COLUMN imap_password VARCHAR(255),
    ADD COLUMN last_email_synced_at TIMESTAMP;

ALTER TABLE job_applications
    ADD COLUMN source VARCHAR(50) DEFAULT 'MANUAL';

