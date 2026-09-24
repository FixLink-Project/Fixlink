-- ============================================================
-- V7: Tạo bảng appointments (Lịch hẹn khảo sát / thi công)
-- Thuộc Jira RC-48: Appointment Status Lifecycle (Parent RC-43)
-- Chuẩn hóa 100% theo thiết kế ERD database-erd.jpg
-- ============================================================

CREATE TABLE IF NOT EXISTS appointments (
    id                  BIGSERIAL       PRIMARY KEY,
    repair_request_id   BIGINT          NOT NULL REFERENCES repair_requests(id),
    customer_id         BIGINT          NOT NULL REFERENCES customer_profiles(user_id),
    technician_id       BIGINT          NOT NULL REFERENCES technician_profiles(user_id),
    appointment_type    VARCHAR(30)     NOT NULL DEFAULT 'REPAIR'
                        CHECK (appointment_type IN ('SURVEY', 'REPAIR', 'WARRANTY')),
    scheduled_date      DATE            NOT NULL,
    scheduled_time      TIME            NOT NULL,
    actual_start_at     TIMESTAMP,
    actual_end_at       TIMESTAMP,
    status              VARCHAR(20)     NOT NULL DEFAULT 'CONFIRMED'
                        CHECK (status IN ('CONFIRMED', 'RESCHEDULED', 'COMPLETED', 'CANCELLED')),
    address             VARCHAR(255),
    notes               TEXT,
    cancel_reason       TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          BIGINT,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          BIGINT,
    deleted_at          TIMESTAMP,
    deleted_by          BIGINT
);

CREATE INDEX IF NOT EXISTS idx_appointments_request ON appointments(repair_request_id);
CREATE INDEX IF NOT EXISTS idx_appointments_customer ON appointments(customer_id);
CREATE INDEX IF NOT EXISTS idx_appointments_technician ON appointments(technician_id);
CREATE INDEX IF NOT EXISTS idx_appointments_date ON appointments(scheduled_date);
CREATE INDEX IF NOT EXISTS idx_appointments_status ON appointments(status);
