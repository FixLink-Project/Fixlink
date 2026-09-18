-- ============================================================
-- FLYWAY MIGRATION V2: Create Service Catalog tables
-- RC-4: Service Catalog Management
-- ============================================================

-- Service Categories: add missing columns if not exist
ALTER TABLE service_categories ADD COLUMN IF NOT EXISTS sort_order INT DEFAULT 0;
ALTER TABLE service_categories ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Services (dịch vụ thuộc danh mục)
CREATE TABLE services (
    id              BIGSERIAL PRIMARY KEY,
    category_id     BIGINT NOT NULL REFERENCES service_categories(id),
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
    estimated_price DECIMAL(15,2),
    unit            VARCHAR(50),
    icon_url        VARCHAR(500),
    sort_order      INT DEFAULT 0,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    deleted_at      TIMESTAMP,
    CONSTRAINT uq_service_category_name UNIQUE(category_id, name)
);

-- Index for faster lookups
CREATE INDEX idx_services_category_id ON services(category_id);
CREATE INDEX idx_service_categories_active ON service_categories(is_active) WHERE deleted_at IS NULL;
CREATE INDEX idx_services_active ON services(is_active) WHERE deleted_at IS NULL;
