-- =============================================================================
-- FIXLINK - MEDIA ATTACHMENTS ENHANCEMENT (Firebase Storage metadata)
-- Bổ sung media_type và uploaded_by cho bảng media để hỗ trợ tệp đính kèm Firebase Storage
-- =============================================================================

ALTER TABLE media ADD COLUMN IF NOT EXISTS media_type VARCHAR(30) DEFAULT 'IMAGE';
ALTER TABLE media ADD COLUMN IF NOT EXISTS uploaded_by BIGINT;

CREATE INDEX IF NOT EXISTS idx_media_uploaded_by ON media(uploaded_by);
