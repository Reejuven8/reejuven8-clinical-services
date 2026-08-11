-- The notification_channel / notification_status PG enum types are owned by this
-- service and were never created anywhere (not here, not in postgres-init.sql),
-- so this migration failed on any clean database and the service could not start.
-- Values mirror NotificationChannel / NotificationStatus. See IS-038.
-- Guarded DO blocks: PostgreSQL has no CREATE TYPE IF NOT EXISTS.
DO $$ BEGIN
    CREATE TYPE notification_channel AS ENUM ('WHATSAPP', 'SMS', 'PUSH', 'EMAIL');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE notification_status AS ENUM ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'SKIPPED');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

CREATE TABLE IF NOT EXISTS notification_logs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    channel             notification_channel NOT NULL,
    event_type          VARCHAR(100) NOT NULL,
    status              notification_status NOT NULL DEFAULT 'PENDING',
    title               VARCHAR(500),
    message_body        TEXT NOT NULL,
    metadata            JSONB,
    retry_count         INTEGER NOT NULL DEFAULT 0,
    max_retries         INTEGER NOT NULL DEFAULT 4,
    external_message_id VARCHAR(255),
    failure_reason      TEXT,
    sent_at             TIMESTAMPTZ,
    delivered_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_user ON notification_logs(user_id);
CREATE INDEX idx_notification_status ON notification_logs(status) WHERE status IN ('PENDING', 'FAILED');
CREATE INDEX idx_notification_created ON notification_logs(created_at);
