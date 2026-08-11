CREATE TABLE medication_schedules (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL,
    pregnancy_profile_id    UUID REFERENCES pregnancy_profiles(id) ON DELETE SET NULL,
    medication_name         VARCHAR(200) NOT NULL,
    dosage                  VARCHAR(100) NOT NULL,
    dosage_instructions     TEXT,
    schedule_time           medication_schedule_time NOT NULL,
    reminder_time           TIME,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    current_inventory_count INTEGER NOT NULL DEFAULT 0,
    refill_threshold        INTEGER NOT NULL DEFAULT 5,
    start_date              DATE NOT NULL,
    end_date                DATE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER update_medication_schedules_updated_at
    BEFORE UPDATE ON medication_schedules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_medication_user ON medication_schedules(user_id);
CREATE INDEX idx_medication_active ON medication_schedules(user_id, is_active) WHERE is_active = TRUE;
