-- =============================================================================
-- V12 — Mo khoa tai khoan Admin mac dinh (set status = ACTIVE)
-- =============================================================================

UPDATE users
SET status = 'ACTIVE'
WHERE username = 'admin';
