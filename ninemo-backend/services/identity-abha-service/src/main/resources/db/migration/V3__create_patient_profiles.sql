CREATE TABLE patient_profiles (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                  UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date_of_birth            DATE NOT NULL,
    biological_sex           biological_sex NOT NULL,
    emergency_contact_name   VARCHAR(200),
    emergency_contact_number VARCHAR(15),
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER update_patient_profiles_updated_at
    BEFORE UPDATE ON patient_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE UNIQUE INDEX idx_patient_profiles_user_id ON patient_profiles(user_id);
