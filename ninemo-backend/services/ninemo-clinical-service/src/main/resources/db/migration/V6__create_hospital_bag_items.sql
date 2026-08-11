CREATE TABLE hospital_bag_items (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pregnancy_profile_id UUID NOT NULL REFERENCES pregnancy_profiles(id) ON DELETE CASCADE,
    item_name            VARCHAR(200) NOT NULL,
    category             bag_item_category NOT NULL DEFAULT 'OTHER',
    is_packed            BOOLEAN NOT NULL DEFAULT FALSE,
    is_custom_item       BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order           INTEGER NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER update_hospital_bag_items_updated_at
    BEFORE UPDATE ON hospital_bag_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE INDEX idx_hospital_bag_pregnancy ON hospital_bag_items(pregnancy_profile_id);
