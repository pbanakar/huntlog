-- ============================================================================
-- V3__add_email_preferences.sql
-- ============================================================================
-- Phase 4a: Scheduled Follow-up Email Reminders & User Preferences (PostgreSQL)
--
-- This migration adds the `email_reminders_enabled` column to the `users` table
-- so users can toggle daily follow-up email notifications on or off.
-- ============================================================================

ALTER TABLE users
    ADD COLUMN email_reminders_enabled BOOLEAN NOT NULL DEFAULT TRUE;
