CREATE TABLE doctor_profiles (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    medical_license_number VARCHAR(50) UNIQUE NOT NULL,
    specialization         VARCHAR(100) NOT NULL,
    qualifications         VARCHAR(200) NOT NULL,
    years_of_experience    INTEGER,
    consultation_fee       DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    bio                    TEXT,
    digital_signature_url  VARCHAR(500),
    is_accepting_patients  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER update_doctor_profiles_updated_at
    BEFORE UPDATE ON doctor_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE UNIQUE INDEX idx_doctor_profiles_user_id ON doctor_profiles(user_id);
CREATE INDEX idx_doctor_profiles_specialization ON doctor_profiles(specialization);
CREATE INDEX idx_doctor_profiles_accepting ON doctor_profiles(is_accepting_patients) WHERE is_accepting_patients = TRUE;
