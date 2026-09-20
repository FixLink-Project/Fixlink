-- =============================================================================
-- FIXLINK - DỮ LIỆU DANH MỤC GỐC
-- Initial categories, services, and default lookup data (ANSI SQL standard)
-- =============================================================================

-- 1. SEED SERVICE CATEGORIES
INSERT INTO service_categories (id, code, name, description, icon_url, display_order, is_active, created_at, updated_at)
VALUES 
(1, 'DIEN_LANH', 'Sửa Chữa Điện Lạnh', 'Chuyên sửa chữa, bảo dưỡng điều hòa, tủ lạnh, máy giặt, tủ đông tận nơi', 'snowflake', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'DIEN_NUOC', 'Sửa Chữa Điện Nước', 'Khắc phục chập điện, rò rỉ đường ống nước, lắp thiết bị vệ sinh, máy bơm nước', 'bolt', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'DO_GIA_DUNG', 'Sửa Điện Gia Dụng', 'Sửa chữa bếp từ, lò vi sóng, máy hút mùi, nồi chiên không dầu, quạt điện', 'plug', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'KHOA_CUA', 'Sửa Khóa & Cửa Cuốn', 'Mở khóa cấp tốc 24/7, làm chìa khóa thông minh, bảo trì sửa chữa cửa cuốn các loại', 'key', 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 2. SEED SERVICES
INSERT INTO services (id, category_id, code, name, description, base_price, estimated_duration_minutes, is_active, created_at, updated_at)
VALUES
(1, 1, 'VE_SINH_MAY_LANH', 'Vệ sinh máy lạnh treo tường (1.0 - 2.5 HP)', 'Bảo dưỡng, vệ sinh dàn nóng và dàn lạnh khử khuẩn toàn diện', 150000, 45, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, 'NAP_GA_R32', 'Nạp ga máy lạnh R32 / R410A trọn gói', 'Bổ sung ga lạnh đạt chuẩn áp suất làm lạnh nhanh và tiết kiệm điện', 250000, 30, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 1, 'SUA_TU_LANH', 'Kiểm tra & Sửa tủ lạnh không lạnh / kêu to', 'Xử lý tắc ẩm, hỏng quạt gió, hỏng rơ-le xả đá hoặc rò ga tủ lạnh', 200000, 60, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 1, 'SUA_MAY_GIAT', 'Sửa máy giặt không vắt / báo lỗi bo mạch', 'Kiểm tra van cấp nước, động cơ, phao áp lực và bo điều khiển', 250000, 60, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(5, 2, 'SUA_ONG_NUOC', 'Thông nghẹt & Xử lý rò rỉ đường ống nước', 'Dò tìm rò rỉ đường ống nước sinh hoạt, thay van khóa, xử lý thấm dột', 180000, 45, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 2, 'LAP_THIET_BI_VE_SINH', 'Lắp đặt bồn cầu, Lavabo, vòi hoa sen', 'Lắp đặt mới hoặc thay thế linh kiện bồn nước, sen vòi, bồn rửa chén', 200000, 60, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 2, 'SUA_CHAP_DIEN', 'Dò tìm sự cố chập điện âm tường & Nhảy CB', 'Dùng máy đo chuyên dụng phát hiện rò điện, thay thế CB/Attomat bị hỏng', 300000, 90, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(8, 3, 'SUA_BEP_TU', 'Sửa bếp từ không nóng / chập nguồn báo lỗi', 'Thay IGBT, tụ lọc nguồn, xử lý sensor cảm biến nhiệt cho bếp từ đơn/đôi', 200000, 45, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 3, 'SUA_LO_VI_SONG', 'Sửa lò vi sóng không nóng / đánh lửa', 'Thay đèn phát sóng Magnetron, cầu chì cao áp và kiểm tra tấm chắn sóng', 180000, 40, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(10, 4, 'MO_KHOA_CAP_TOC', 'Mở khóa cửa nhà / Khóa phòng cấp tốc 24/7', 'Thợ khóa có mặt sau 15-30 phút, mở khóa cơ và khóa tay gạt an toàn', 150000, 30, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(11, 4, 'SUA_CUA_CUON', 'Sửa chữa cửa cuốn kẹt nan / hỏng motor', 'Cân chỉnh lò xo, căn chỉnh lá cửa kẹt ray, cài đặt sao chép remote cửa cuốn', 350000, 90, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3. RESTART IDENTITY SEQUENCES BEYOND SEEDED IDS
ALTER TABLE service_categories ALTER COLUMN id RESTART WITH 100;
ALTER TABLE services ALTER COLUMN id RESTART WITH 100;

