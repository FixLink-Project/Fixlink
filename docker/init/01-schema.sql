-- ============================================================
-- FIXLINK DATABASE SCHEMA v2.0
-- Nền tảng kết nối khách hàng và kỹ thuật viên sửa chữa
-- PostgreSQL 16
-- ============================================================

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- 1. USERS - Tài khoản chung (Customer, Technician, Staff, Admin)
-- ============================================================
CREATE TABLE users (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    role            VARCHAR(20)     NOT NULL CHECK (role IN ('CUSTOMER', 'TECHNICIAN', 'STAFF', 'ADMIN')),
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'BANNED')),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      BIGINT,
    deleted_at      TIMESTAMP,
    deleted_by      BIGINT
);

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_deleted_at ON users(deleted_at);

-- ============================================================
-- 2. CUSTOMER_PROFILES - Hồ sơ khách hàng
-- ============================================================
CREATE TABLE customer_profiles (
    user_id         BIGINT          PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(100)    NOT NULL,
    phone           VARCHAR(20),
    email           VARCHAR(100),
    avatar_url      VARCHAR(500),
    loyalty_points  INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      BIGINT,
    deleted_at      TIMESTAMP,
    deleted_by      BIGINT
);

CREATE INDEX idx_customer_profiles_phone ON customer_profiles(phone);
CREATE INDEX idx_customer_profiles_email ON customer_profiles(email);

-- ============================================================
-- 3. CUSTOMER_ADDRESSES - Địa chỉ khách hàng
-- ============================================================
CREATE TABLE customer_addresses (
    id              BIGSERIAL       PRIMARY KEY,
    customer_id     BIGINT          NOT NULL REFERENCES customer_profiles(user_id) ON DELETE CASCADE,
    label           VARCHAR(50),
    address_line    TEXT            NOT NULL,
    city            VARCHAR(100),
    district        VARCHAR(100),
    lat             DECIMAL(10,7),
    lng             DECIMAL(10,7),
    is_default      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      BIGINT,
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      BIGINT
);

CREATE INDEX idx_customer_addresses_customer ON customer_addresses(customer_id);

-- ============================================================
-- 4. TECHNICIAN_PROFILES - Hồ sơ kỹ thuật viên
-- ============================================================
CREATE TABLE technician_profiles (
    user_id             BIGINT          PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    name                VARCHAR(100)    NOT NULL,
    phone               VARCHAR(20),
    email               VARCHAR(100),
    avatar_url          VARCHAR(500),
    id_card_number      VARCHAR(20),
    bio                 TEXT,
    years_experience    SMALLINT        DEFAULT 0,
    is_verified         BOOLEAN         NOT NULL DEFAULT FALSE,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    avg_rating          DECIMAL(3,2)    DEFAULT 0.00,
    completed_jobs      INT             NOT NULL DEFAULT 0,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          BIGINT
);

CREATE INDEX idx_technician_profiles_status ON technician_profiles(status);
CREATE INDEX idx_technician_profiles_verified ON technician_profiles(is_verified);
CREATE INDEX idx_technician_profiles_rating ON technician_profiles(avg_rating DESC);

-- ============================================================
-- 5. TECHNICIAN_WALLET_TRANSACTIONS - Giao dịch ví kỹ thuật viên
-- ============================================================
CREATE TABLE technician_wallet_transactions (
    id              BIGSERIAL       PRIMARY KEY,
    technician_id   BIGINT          NOT NULL REFERENCES technician_profiles(user_id) ON DELETE CASCADE,
    amount          DECIMAL(15,2)   NOT NULL,
    type            VARCHAR(30)     NOT NULL CHECK (type IN ('EARNING', 'WITHDRAWAL', 'REFUND', 'BONUS', 'PENALTY')),
    ref_id          BIGINT,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      BIGINT
);

CREATE INDEX idx_wallet_tx_technician ON technician_wallet_transactions(technician_id);
CREATE INDEX idx_wallet_tx_type ON technician_wallet_transactions(type);
CREATE INDEX idx_wallet_tx_created ON technician_wallet_transactions(created_at);

-- ============================================================
-- 6. SERVICE_CATEGORIES - Danh mục ngành nghề / dịch vụ
-- ============================================================
CREATE TABLE service_categories (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL UNIQUE,
    description     TEXT,
    icon_url        VARCHAR(500),
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      BIGINT,
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      BIGINT
);

-- ============================================================
-- 7. SERVICE_AREAS - Khu vực hoạt động
-- ============================================================
CREATE TABLE service_areas (
    id              BIGSERIAL       PRIMARY KEY,
    district        VARCHAR(100)    NOT NULL,
    city            VARCHAR(100)    NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      BIGINT,
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by      BIGINT,
    UNIQUE(district, city)
);

CREATE INDEX idx_service_areas_city ON service_areas(city);

-- ============================================================
-- 8. TECHNICIAN_SKILLS_AREAS - Kỹ năng & khu vực hoạt động của thợ
-- ============================================================
CREATE TABLE technician_skills_areas (
    id                      BIGSERIAL       PRIMARY KEY,
    technician_id           BIGINT          NOT NULL REFERENCES technician_profiles(user_id) ON DELETE CASCADE,
    skill_type              VARCHAR(30)     NOT NULL CHECK (skill_type IN ('SERVICE', 'AREA')),
    service_category_id     BIGINT          REFERENCES service_categories(id) ON DELETE SET NULL,
    area_id                 BIGINT          REFERENCES service_areas(id) ON DELETE SET NULL,
    experience_years        SMALLINT        DEFAULT 0,
    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by              BIGINT
);

CREATE INDEX idx_tech_skills_technician ON technician_skills_areas(technician_id);
CREATE INDEX idx_tech_skills_category ON technician_skills_areas(service_category_id);
CREATE INDEX idx_tech_skills_area ON technician_skills_areas(area_id);

-- ============================================================
-- 9. REPAIR_REQUESTS - Yêu cầu sửa chữa (bảng trung tâm)
-- ============================================================
CREATE TABLE repair_requests (
    id                  BIGSERIAL       PRIMARY KEY,
    customer_id         BIGINT          NOT NULL REFERENCES customer_profiles(user_id),
    category_id         BIGINT          NOT NULL REFERENCES service_categories(id),
    technician_id       BIGINT          REFERENCES technician_profiles(user_id),
    active_deal_id      BIGINT,         -- FK added after deal_sessions is created
    title               VARCHAR(200)    NOT NULL,
    description         TEXT,
    address_line        TEXT            NOT NULL,
    city                VARCHAR(100),
    district            VARCHAR(100),
    lat                 DECIMAL(10,7),
    lng                 DECIMAL(10,7),
    preferred_time      VARCHAR(100),
    urgency_level       VARCHAR(20)     DEFAULT 'NORMAL' CHECK (urgency_level IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    bidding_deadline    TIMESTAMP,
    status              VARCHAR(20)     NOT NULL DEFAULT 'OPEN'
                        CHECK (status IN ('OPEN', 'BIDDING', 'DEAL', 'IN_PROGRESS', 'DONE', 'CANCELLED', 'CLOSED')),
    deposit_amount      DECIMAL(15,2)   DEFAULT 0,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          BIGINT
);

CREATE INDEX idx_repair_requests_customer ON repair_requests(customer_id);
CREATE INDEX idx_repair_requests_technician ON repair_requests(technician_id);
CREATE INDEX idx_repair_requests_category ON repair_requests(category_id);
CREATE INDEX idx_repair_requests_status ON repair_requests(status);
CREATE INDEX idx_repair_requests_created ON repair_requests(created_at DESC);
CREATE INDEX idx_repair_requests_city_district ON repair_requests(city, district);

-- ============================================================
-- 10. BIDDING_SESSIONS - Phiên đấu giá (1 per repair_request)
-- ============================================================
CREATE TABLE bidding_sessions (
    id                      BIGSERIAL       PRIMARY KEY,
    repair_request_id       BIGINT          NOT NULL UNIQUE REFERENCES repair_requests(id) ON DELETE CASCADE,
    start_time              TIMESTAMP       NOT NULL DEFAULT NOW(),
    end_time                TIMESTAMP       NOT NULL,
    max_bids                INT             DEFAULT 10,
    current_bid_count       INT             NOT NULL DEFAULT 0,
    auto_close_on_accept    BOOLEAN         NOT NULL DEFAULT FALSE,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'OPEN'
                            CHECK (status IN ('OPEN', 'CLOSED', 'EXPIRED')),
    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by              BIGINT
);

CREATE INDEX idx_bidding_sessions_status ON bidding_sessions(status);
CREATE INDEX idx_bidding_sessions_end_time ON bidding_sessions(end_time);

-- ============================================================
-- 11. BIDDING_BIDS - Báo giá từng thợ
-- ============================================================
CREATE TABLE bidding_bids (
    id                          BIGSERIAL       PRIMARY KEY,
    bidding_session_id          BIGINT          NOT NULL REFERENCES bidding_sessions(id) ON DELETE CASCADE,
    repair_request_id           BIGINT          NOT NULL REFERENCES repair_requests(id),
    technician_id               BIGINT          NOT NULL REFERENCES technician_profiles(user_id),
    price                       DECIMAL(15,2)   NOT NULL,
    message                     TEXT,
    estimated_duration_hours    DECIMAL(5,2),
    status                      VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                                CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'WITHDRAWN')),
    created_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by                  BIGINT,
    updated_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by                  BIGINT
);

CREATE INDEX idx_bids_session ON bidding_bids(bidding_session_id);
CREATE INDEX idx_bids_request ON bidding_bids(repair_request_id);
CREATE INDEX idx_bids_technician ON bidding_bids(technician_id);
CREATE INDEX idx_bids_status ON bidding_bids(status);
CREATE UNIQUE INDEX idx_bids_unique_per_session ON bidding_bids(bidding_session_id, technician_id);

-- ============================================================
-- 12. DEAL_SESSIONS - Deal đã quyết định (Thỏa thuận chính thức)
-- ============================================================
CREATE TABLE deal_sessions (
    id                  BIGSERIAL       PRIMARY KEY,
    repair_request_id   BIGINT          NOT NULL REFERENCES repair_requests(id),
    bid_id              BIGINT          NOT NULL REFERENCES bidding_bids(id),
    technician_id       BIGINT          NOT NULL REFERENCES technician_profiles(user_id),
    agreed_price        DECIMAL(15,2)   NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')),
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          BIGINT
);

CREATE INDEX idx_deal_sessions_request ON deal_sessions(repair_request_id);
CREATE INDEX idx_deal_sessions_technician ON deal_sessions(technician_id);
CREATE INDEX idx_deal_sessions_status ON deal_sessions(status);

-- Add FK from repair_requests.active_deal_id → deal_sessions.id
ALTER TABLE repair_requests
    ADD CONSTRAINT fk_repair_requests_active_deal
    FOREIGN KEY (active_deal_id) REFERENCES deal_sessions(id) ON DELETE SET NULL;

-- ============================================================
-- 13. APPOINTMENTS - Lịch hẹn
-- ============================================================
CREATE TABLE appointments (
    id                  BIGSERIAL       PRIMARY KEY,
    repair_request_id   BIGINT          NOT NULL REFERENCES repair_requests(id),
    customer_id         BIGINT          NOT NULL REFERENCES customer_profiles(user_id),
    technician_id       BIGINT          NOT NULL REFERENCES technician_profiles(user_id),
    scheduled_date      DATE            NOT NULL,
    scheduled_time      TIME            NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'CONFIRMED'
                        CHECK (status IN ('CONFIRMED', 'RESCHEDULED', 'CANCELLED')),
    notes               TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          BIGINT
);

CREATE INDEX idx_appointments_request ON appointments(repair_request_id);
CREATE INDEX idx_appointments_customer ON appointments(customer_id);
CREATE INDEX idx_appointments_technician ON appointments(technician_id);
CREATE INDEX idx_appointments_date ON appointments(scheduled_date);
CREATE INDEX idx_appointments_status ON appointments(status);

-- ============================================================
-- 14. WORK_PROGRESS - Tiến độ công việc
-- ============================================================
CREATE TABLE work_progress (
    id                  BIGSERIAL       PRIMARY KEY,
    repair_request_id   BIGINT          NOT NULL REFERENCES repair_requests(id),
    from_status         VARCHAR(30)     NOT NULL,
    to_status           VARCHAR(30)     NOT NULL,
    note                TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          BIGINT
);

CREATE INDEX idx_work_progress_request ON work_progress(repair_request_id);
CREATE INDEX idx_work_progress_created ON work_progress(created_at);

-- ============================================================
-- 15. INSPECTION_RECORDS - Biên bản khảo sát hiện trường
-- ============================================================
CREATE TABLE inspection_records (
    id                  BIGSERIAL       PRIMARY KEY,
    repair_request_id   BIGINT          NOT NULL REFERENCES repair_requests(id),
    technician_id       BIGINT          NOT NULL REFERENCES technician_profiles(user_id),
    inspection_notes    TEXT,
    findings            TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          BIGINT
);

CREATE INDEX idx_inspection_request ON inspection_records(repair_request_id);
CREATE INDEX idx_inspection_technician ON inspection_records(technician_id);

-- ============================================================
-- 16. ADDITIONAL_COST_REQUESTS - Chi phí phát sinh
-- ============================================================
CREATE TABLE additional_cost_requests (
    id                  BIGSERIAL       PRIMARY KEY,
    repair_request_id   BIGINT          NOT NULL REFERENCES repair_requests(id),
    technician_id       BIGINT          NOT NULL REFERENCES technician_profiles(user_id),
    description         TEXT            NOT NULL,
    amount              DECIMAL(15,2)   NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    reason              TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          BIGINT
);

CREATE INDEX idx_additional_cost_request ON additional_cost_requests(repair_request_id);
CREATE INDEX idx_additional_cost_status ON additional_cost_requests(status);

-- ============================================================
-- 17. PAYMENTS - Thanh toán (Escrow, COD, Quyết toán, Hoàn)
-- ============================================================
CREATE TABLE payments (
    id                  BIGSERIAL       PRIMARY KEY,
    repair_request_id   BIGINT          NOT NULL REFERENCES repair_requests(id),
    customer_id         BIGINT          NOT NULL REFERENCES customer_profiles(user_id),
    technician_id       BIGINT          NOT NULL REFERENCES technician_profiles(user_id),
    amount              DECIMAL(15,2)   NOT NULL,
    type                VARCHAR(20)     NOT NULL CHECK (type IN ('DEPOSIT', 'ESCROW', 'FINAL', 'REFUND')),
    method              VARCHAR(30)     CHECK (method IN ('CASH', 'BANK_TRANSFER', 'MOMO', 'ZALOPAY', 'VNPAY')),
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED')),
    transaction_ref     VARCHAR(100),
    paid_at             TIMESTAMP,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          BIGINT
);

CREATE INDEX idx_payments_request ON payments(repair_request_id);
CREATE INDEX idx_payments_customer ON payments(customer_id);
CREATE INDEX idx_payments_technician ON payments(technician_id);
CREATE INDEX idx_payments_type ON payments(type);
CREATE INDEX idx_payments_status ON payments(status);

-- ============================================================
-- 18. WARRANTIES - Phiếu bảo hành
-- ============================================================
CREATE TABLE warranties (
    id                  BIGSERIAL       PRIMARY KEY,
    repair_request_id   BIGINT          NOT NULL REFERENCES repair_requests(id),
    technician_id       BIGINT          NOT NULL REFERENCES technician_profiles(user_id),
    warranty_terms      TEXT,
    start_date          DATE            NOT NULL,
    end_date            DATE            NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE', 'EXPIRED', 'CLAIMED', 'VOID')),
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          BIGINT
);

CREATE INDEX idx_warranties_request ON warranties(repair_request_id);
CREATE INDEX idx_warranties_status ON warranties(status);
CREATE INDEX idx_warranties_end_date ON warranties(end_date);

-- ============================================================
-- 19. REVIEWS - Đánh giá (Khách hàng → Thợ)
-- ============================================================
CREATE TABLE reviews (
    id                  BIGSERIAL       PRIMARY KEY,
    repair_request_id   BIGINT          NOT NULL REFERENCES repair_requests(id),
    customer_id         BIGINT          NOT NULL REFERENCES customer_profiles(user_id),
    technician_id       BIGINT          NOT NULL REFERENCES technician_profiles(user_id),
    rating              SMALLINT        NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment             TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_by          BIGINT
);

CREATE UNIQUE INDEX idx_reviews_unique_per_request ON reviews(repair_request_id, customer_id);
CREATE INDEX idx_reviews_technician ON reviews(technician_id);
CREATE INDEX idx_reviews_rating ON reviews(rating);

-- ============================================================
-- 20. NOTIFICATIONS - Thông báo gửi tới Customer, Technician
-- ============================================================
CREATE TABLE notifications (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(50)     NOT NULL,
    title           VARCHAR(200)    NOT NULL,
    message         TEXT,
    ref_id          BIGINT,
    ref_type        VARCHAR(50),
    is_read         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_by      BIGINT
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_read ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_created ON notifications(created_at DESC);

-- ============================================================
-- 21. MEDIA - Ảnh, video đính kèm
-- ============================================================
CREATE TABLE media (
    id              BIGSERIAL       PRIMARY KEY,
    owner_type      VARCHAR(50)     NOT NULL,
    owner_id        BIGINT          NOT NULL,
    media_type      VARCHAR(30)     NOT NULL CHECK (media_type IN ('IMAGE', 'VIDEO', 'DOCUMENT')),
    url             VARCHAR(500)    NOT NULL,
    uploaded_by     BIGINT          REFERENCES users(id),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_media_owner ON media(owner_type, owner_id);
CREATE INDEX idx_media_uploaded_by ON media(uploaded_by);

-- ============================================================
-- TRIGGER: Auto-update updated_at
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to all tables with updated_at
DO $$
DECLARE
    tbl TEXT;
BEGIN
    FOR tbl IN
        SELECT table_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND column_name = 'updated_at'
        GROUP BY table_name
    LOOP
        EXECUTE format(
            'CREATE TRIGGER trg_%s_updated_at
             BEFORE UPDATE ON %I
             FOR EACH ROW
             EXECUTE FUNCTION update_updated_at_column()',
            tbl, tbl
        );
    END LOOP;
END;
$$;

-- ============================================================
-- DONE: Schema v2.0 created successfully
-- ============================================================
