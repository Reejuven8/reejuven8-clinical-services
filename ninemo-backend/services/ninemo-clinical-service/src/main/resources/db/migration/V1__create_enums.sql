-- NineMo domain enums (reejuven8_ninemo database)
CREATE TYPE biological_sex AS ENUM ('MALE', 'FEMALE', 'OTHER');
CREATE TYPE edd_calculation_method AS ENUM ('LMP', 'ULTRASOUND', 'IVF');
CREATE TYPE delivery_type AS ENUM ('NORMAL', 'CAESAREAN', 'ASSISTED');
CREATE TYPE vaccination_status AS ENUM ('PENDING', 'COMPLETED', 'SKIPPED', 'OVERDUE');
CREATE TYPE medication_schedule_time AS ENUM ('MORNING', 'BEFORE_LUNCH', 'AFTER_LUNCH', 'EVENING', 'BEDTIME');
CREATE TYPE bag_item_category AS ENUM ('DOCUMENTS', 'MOTHER', 'BABY', 'PARTNER', 'SNACKS', 'OTHER');
CREATE TYPE food_safety_rating AS ENUM ('SAFE', 'CAUTION', 'AVOID');

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';
