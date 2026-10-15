-- =============================================================================
-- V11 — Phone unique per role (documentation), SLA timezone Asia/Ho_Chi_Minh
-- =============================================================================

-- Phone is already unique per profile table (customer_profiles and
-- technician_profiles are separate tables), so the same phone number
-- can be registered as both customer and technician.
-- No constraint change needed.

-- Add indexes on phone for faster lookup when supporting phone-based login.
CREATE INDEX IF NOT EXISTS idx_customer_profiles_phone
    ON customer_profiles(phone);

CREATE INDEX IF NOT EXISTS idx_technician_profiles_phone
    ON technician_profiles(phone);
