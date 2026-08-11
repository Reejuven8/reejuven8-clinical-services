CREATE TABLE child_profiles (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pregnancy_profile_id  UUID UNIQUE NOT NULL REFERENCES pregnancy_profiles(id) ON DELETE CASCADE,
    parent_user_id        UUID NOT NULL,
    child_name            VARCHAR(200),
    date_of_birth         DATE NOT NULL,
    birth_weight_kg       DECIMAL(4,2),
    birth_height_cm       DECIMAL(5,2),
    head_circumference_cm DECIMAL(5,2),
    biological_sex        biological_sex NOT NULL,
    blood_group           VARCHAR(5),
    is_active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER update_child_profiles_updated_at
    BEFORE UPDATE ON child_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE UNIQUE INDEX idx_child_pregnancy ON child_profiles(pregnancy_profile_id);
CREATE INDEX idx_child_parent ON child_profiles(parent_user_id);
