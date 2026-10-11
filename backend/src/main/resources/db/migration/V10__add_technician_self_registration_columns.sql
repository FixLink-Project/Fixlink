-- =============================================================================
-- V10 — Technician Self-Registration & Status Approvals
-- =============================================================================

-- 1. Cho phép citizen_id có thể để trống (NULL) khi thợ tự đăng ký ban đầu
ALTER TABLE technician_profiles ALTER COLUMN citizen_id DROP NOT NULL;

-- 2. Thêm địa chỉ và chuyên môn cho hồ sơ thợ
ALTER TABLE technician_profiles ADD COLUMN IF NOT EXISTS address VARCHAR(255);
ALTER TABLE technician_profiles ADD COLUMN IF NOT EXISTS specialization VARCHAR(100);
