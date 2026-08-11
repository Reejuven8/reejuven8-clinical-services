CREATE TABLE addresses (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    address_type   address_type NOT NULL,
    address_line_1 VARCHAR(255) NOT NULL,
    address_line_2 VARCHAR(255),
    city           VARCHAR(100) NOT NULL,
    state          VARCHAR(100) NOT NULL,
    pincode        VARCHAR(10) NOT NULL,
    country        VARCHAR(100) NOT NULL DEFAULT 'India',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER update_addresses_updated_at
    BEFORE UPDATE ON addresses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_addresses_user_id ON addresses(user_id);
CREATE INDEX idx_addresses_pincode ON addresses(pincode);
CREATE INDEX idx_addresses_city_state ON addresses(city, state);

CREATE TABLE user_consents (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    doctor_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    consent_status   consent_status NOT NULL,
    abdm_consent_id  VARCHAR(255),
    granted_at       TIMESTAMPTZ NOT NULL,
    expires_at       TIMESTAMPTZ NOT NULL,
    revoked_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_consent_dates CHECK (expires_at > granted_at)
);

CREATE TRIGGER update_user_consents_updated_at
    BEFORE UPDATE ON user_consents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_consents_patient_doctor ON user_consents(patient_id, doctor_id);
CREATE INDEX idx_consents_status ON user_consents(consent_status);
CREATE INDEX idx_consents_expires_at ON user_consents(expires_at) WHERE consent_status = 'GRANTED';
