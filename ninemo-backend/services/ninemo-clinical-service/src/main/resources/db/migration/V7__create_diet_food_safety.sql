CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE diet_food_safety (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ingredient_name       VARCHAR(200) UNIQUE NOT NULL,
    ingredient_name_hindi VARCHAR(200),
    safety_rating         food_safety_rating NOT NULL,
    medical_reasoning     TEXT NOT NULL,
    safe_quantity         TEXT,
    trimester_tags        JSONB NOT NULL DEFAULT '[1,2,3]',
    categories            JSONB,
    is_verified           BOOLEAN NOT NULL DEFAULT FALSE,
    verified_by           VARCHAR(200),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER update_diet_food_safety_updated_at
    BEFORE UPDATE ON diet_food_safety
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_diet_name_trgm ON diet_food_safety USING gin (ingredient_name gin_trgm_ops);
CREATE INDEX idx_diet_name_hindi_trgm ON diet_food_safety USING gin (ingredient_name_hindi gin_trgm_ops);
CREATE INDEX idx_diet_safety_rating ON diet_food_safety(safety_rating);
