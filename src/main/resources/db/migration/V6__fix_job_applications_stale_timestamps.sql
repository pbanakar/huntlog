-- Fix last_updated timestamps so that applications synced or created in the past correctly reflect their applied date
UPDATE job_applications
SET last_updated = applied_date
WHERE last_updated > applied_date
  AND last_reminder_sent_at IS NULL;

