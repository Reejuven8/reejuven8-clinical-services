CREATE TABLE vaccination_records (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id           UUID NOT NULL REFERENCES child_profiles(id) ON DELETE CASCADE,
    vaccine_name       VARCHAR(100) NOT NULL,
    vaccine_code       VARCHAR(20),
    dose_number        INTEGER NOT NULL DEFAULT 1,
    scheduled_date     DATE NOT NULL,
    administered_date  DATE,
    status             vaccination_status NOT NULL DEFAULT 'PENDING',
    certificate_s3_url VARCHAR(500),
    administered_by    VARCHAR(200),
    notes              TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE (child_id, vaccine_name, dose_number)
);

CREATE TRIGGER update_vaccination_records_updated_at
    BEFORE UPDATE ON vaccination_records
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_vaccination_child ON vaccination_records(child_id);
CREATE INDEX idx_vaccination_status ON vaccination_records(child_id, status) WHERE status = 'PENDING';
CREATE INDEX idx_vaccination_scheduled ON vaccination_records(scheduled_date) WHERE status = 'PENDING';
