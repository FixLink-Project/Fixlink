-- =============================================================================
-- FIXLINK - CHUYÊN MÔN VÀ KHU VỰC HOẠT ĐỘNG CỦA KỸ THUẬT VIÊN
-- =============================================================================
-- Hai bảng nối này đã có trong thiết kế ERD nhưng chưa được tạo. Thiếu chúng,
-- API cập nhật hồ sơ thợ nhận categoryIds/areaIds, kiểm tra hợp lệ, trả về
-- trong response rồi bỏ đi: dữ liệu không bao giờ được lưu.
-- =============================================================================

CREATE TABLE IF NOT EXISTS technician_categories (
    technician_id BIGINT NOT NULL,
    category_id   BIGINT NOT NULL,
    PRIMARY KEY (technician_id, category_id),
    CONSTRAINT fk_tech_categories_technician
        FOREIGN KEY (technician_id) REFERENCES technician_profiles (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_tech_categories_category
        FOREIGN KEY (category_id) REFERENCES service_categories (id)
);

CREATE TABLE IF NOT EXISTS technician_areas (
    technician_id BIGINT NOT NULL,
    area_id       BIGINT NOT NULL,
    PRIMARY KEY (technician_id, area_id),
    CONSTRAINT fk_tech_areas_technician
        FOREIGN KEY (technician_id) REFERENCES technician_profiles (user_id) ON DELETE CASCADE
);

-- Tra cứu ngược: tìm thợ phù hợp theo nhóm việc và theo địa bàn.
CREATE INDEX IF NOT EXISTS idx_tech_categories_category ON technician_categories (category_id);
CREATE INDEX IF NOT EXISTS idx_tech_areas_area ON technician_areas (area_id);
