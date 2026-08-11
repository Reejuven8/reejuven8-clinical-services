CREATE TABLE pregnancy_profiles (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL,
    lmp_date                DATE,
    ultrasound_date         DATE,
    ivf_transfer_date       DATE,
    edd_date                DATE NOT NULL,
    edd_calculation_method  edd_calculation_method NOT NULL,
    height_cm               DECIMAL(5,2) NOT NULL,
    pre_pregnancy_weight_kg DECIMAL(5,2) NOT NULL,
    baseline_bmi            DECIMAL(4,1) NOT NULL,
    blood_group             VARCHAR(5)   NOT NULL,
    high_risk_flags         JSONB,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    delivery_date           DATE,
    delivery_type           delivery_type,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_at_least_one_date CHECK (
        lmp_date IS NOT NULL OR ultrasound_date IS NOT NULL OR ivf_transfer_date IS NOT NULL
    )
);

CREATE TRIGGER update_pregnancy_profiles_updated_at
    BEFORE UPDATE ON pregnancy_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_pregnancy_user_id ON pregnancy_profiles(user_id);
CREATE INDEX idx_pregnancy_active ON pregnancy_profiles(user_id, is_active) WHERE is_active = TRUE;
CREATE INDEX idx_pregnancy_edd ON pregnancy_profiles(edd_date);
