-- ============================================================
-- FLYWAY MIGRATION V1: Add verification & audit columns
-- For RC-21 (Block/Unblock audit) and RC-22 (KYC verification)
-- ============================================================

-- Add verification_status column to technician_profiles
ALTER TABLE technician_profiles
ADD COLUMN IF NOT EXISTS verification_status VARCHAR(20) DEFAULT 'PENDING';

-- Add check constraint for verification_status
ALTER TABLE technician_profiles
ADD CONSTRAINT chk_verification_status
CHECK (verification_status IN ('PENDING', 'APPROVED', 'REJECTED'));

-- Add audit columns for verification
ALTER TABLE technician_profiles ADD COLUMN IF NOT EXISTS verified_by BIGINT;
ALTER TABLE technician_profiles ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP;
ALTER TABLE technician_profiles ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
ALTER TABLE technician_profiles ADD COLUMN IF NOT EXISTS verification_note TEXT;

-- Sync existing data: is_verified = true → APPROVED, false → PENDING
UPDATE technician_profiles SET verification_status = 'APPROVED' WHERE is_verified = TRUE AND verification_status = 'PENDING';

-- Add audit columns to users for block/unblock tracking
ALTER TABLE users ADD COLUMN IF NOT EXISTS status_changed_by BIGINT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS status_changed_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS status_reason TEXT;
