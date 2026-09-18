-- ============================================================
-- FIXLINK SEED DATA v2.0
-- Dữ liệu mẫu để phát triển và kiểm thử
-- ============================================================

-- ============================================================
-- USERS (5 tài khoản: 2 customers, 2 technicians, 1 admin)
-- Password cho tất cả: "password123" (bcrypt hash)
-- ============================================================
INSERT INTO users (id, username, password_hash, role, status, created_at) VALUES
(1, 'admin',       '$2a$12$z5YEMKxyQJD.IM4fAbneE.Q9h5jV8LEj4AQ6UVRa3879kqGUROn5S', 'ADMIN',      'ACTIVE', NOW()),
(2, 'nguyenvana',   '$2a$12$z5YEMKxyQJD.IM4fAbneE.Q9h5jV8LEj4AQ6UVRa3879kqGUROn5S', 'CUSTOMER',   'ACTIVE', NOW()),
(3, 'tranthib',     '$2a$12$z5YEMKxyQJD.IM4fAbneE.Q9h5jV8LEj4AQ6UVRa3879kqGUROn5S', 'CUSTOMER',   'ACTIVE', NOW()),
(4, 'levanduc',     '$2a$12$z5YEMKxyQJD.IM4fAbneE.Q9h5jV8LEj4AQ6UVRa3879kqGUROn5S', 'TECHNICIAN', 'ACTIVE', NOW()),
(5, 'phamvanminh',  '$2a$12$z5YEMKxyQJD.IM4fAbneE.Q9h5jV8LEj4AQ6UVRa3879kqGUROn5S', 'TECHNICIAN', 'ACTIVE', NOW());

-- Reset sequence
SELECT setval('users_id_seq', 5);

-- ============================================================
-- CUSTOMER_PROFILES
-- ============================================================
INSERT INTO customer_profiles (user_id, name, phone, email, loyalty_points) VALUES
(2, 'Nguyễn Văn A',  '0901234567', 'nguyenvana@gmail.com',  150),
(3, 'Trần Thị B',    '0912345678', 'tranthib@gmail.com',    80);

-- ============================================================
-- CUSTOMER_ADDRESSES
-- ============================================================
INSERT INTO customer_addresses (customer_id, label, address_line, city, district, lat, lng, is_default) VALUES
(2, 'Nhà',       '123 Nguyễn Huệ, Phường Bến Nghé',       'TP. Hồ Chí Minh', 'Quận 1',    10.7731000, 106.7030000, TRUE),
(2, 'Công ty',   '456 Nguyễn Văn Linh, Phường Tân Phong',  'TP. Hồ Chí Minh', 'Quận 7',    10.7295000, 106.7218000, FALSE),
(3, 'Nhà',       '789 Võ Văn Ngân, Phường Linh Chiểu',     'TP. Hồ Chí Minh', 'Thủ Đức',   10.8510000, 106.7590000, TRUE);

-- ============================================================
-- TECHNICIAN_PROFILES
-- ============================================================
INSERT INTO technician_profiles (user_id, name, phone, email, id_card_number, bio, years_experience, is_verified, status, avg_rating, completed_jobs) VALUES
(4, 'Lê Văn Đức',    '0923456789', 'levanduc@gmail.com',    '079200012345',
    'Thợ điện lạnh với 8 năm kinh nghiệm, chuyên sửa máy lạnh, tủ lạnh các hãng.',
    8, TRUE, 'ACTIVE', 4.75, 156),
(5, 'Phạm Văn Minh', '0934567890', 'phamvanminh@gmail.com', '079200054321',
    'Thợ điện dân dụng và công nghiệp, sửa chữa hệ thống điện, đèn, ổ cắm.',
    5, TRUE, 'ACTIVE', 4.50, 89);

-- ============================================================
-- SERVICE_CATEGORIES - 5 ngành nghề chính
-- ============================================================
INSERT INTO service_categories (id, name, description, icon_url, is_active) VALUES
(1, 'Điện dân dụng',   'Sửa chữa hệ thống điện, ổ cắm, công tắc, đèn chiếu sáng',          'icons/electric.svg',       TRUE),
(2, 'Nước - Ống nước', 'Sửa ống nước, vòi nước, bồn cầu, máy bơm, thoát nước',               'icons/plumbing.svg',       TRUE),
(3, 'Điện lạnh',       'Sửa chữa, bảo trì máy lạnh, tủ lạnh, máy giặt các hãng',            'icons/cooling.svg',        TRUE),
(4, 'Sơn - Chống thấm','Sơn nhà, chống thấm tường, trần, sàn nhà',                           'icons/painting.svg',       TRUE),
(5, 'Điện tử',         'Sửa chữa TV, loa, thiết bị điện tử gia dụng',                        'icons/electronics.svg',    TRUE);

SELECT setval('service_categories_id_seq', 5);

-- ============================================================
-- SERVICE_AREAS - Khu vực TP.HCM
-- ============================================================
INSERT INTO service_areas (id, district, city, is_active) VALUES
(1, 'Quận 1',     'TP. Hồ Chí Minh', TRUE),
(2, 'Quận 7',     'TP. Hồ Chí Minh', TRUE),
(3, 'Thủ Đức',    'TP. Hồ Chí Minh', TRUE),
(4, 'Quận 3',     'TP. Hồ Chí Minh', TRUE),
(5, 'Bình Thạnh', 'TP. Hồ Chí Minh', TRUE);

SELECT setval('service_areas_id_seq', 5);

-- ============================================================
-- TECHNICIAN_SKILLS_AREAS - Kỹ năng & khu vực hoạt động
-- ============================================================
-- Thợ Đức: Điện lạnh + Điện dân dụng, hoạt động Q1, Q7
INSERT INTO technician_skills_areas (technician_id, skill_type, service_category_id, area_id, experience_years) VALUES
(4, 'SERVICE', 3, NULL, 8),  -- Điện lạnh
(4, 'SERVICE', 1, NULL, 5),  -- Điện dân dụng
(4, 'AREA',   NULL, 1, NULL),  -- Quận 1
(4, 'AREA',   NULL, 2, NULL);  -- Quận 7

-- Thợ Minh: Điện dân dụng + Điện tử, hoạt động Q1, Thủ Đức, Bình Thạnh
INSERT INTO technician_skills_areas (technician_id, skill_type, service_category_id, area_id, experience_years) VALUES
(5, 'SERVICE', 1, NULL, 5),  -- Điện dân dụng
(5, 'SERVICE', 5, NULL, 3),  -- Điện tử
(5, 'AREA',   NULL, 1, NULL),  -- Quận 1
(5, 'AREA',   NULL, 3, NULL),  -- Thủ Đức
(5, 'AREA',   NULL, 5, NULL);  -- Bình Thạnh

-- ============================================================
-- REPAIR_REQUESTS - 2 yêu cầu sửa chữa mẫu
-- ============================================================
INSERT INTO repair_requests (id, customer_id, category_id, technician_id, title, description, address_line, city, district, lat, lng, preferred_time, urgency_level, bidding_deadline, status, deposit_amount, created_by) VALUES
(1, 2, 3, 4,
    'Máy lạnh không lạnh - Daikin Inverter 1.5HP',
    'Máy lạnh Daikin Inverter 1.5HP, mua 2022. Gần đây bật lên không lạnh, quạt vẫn chạy nhưng dàn nóng không hoạt động. Đã vệ sinh lưới lọc nhưng không cải thiện.',
    '123 Nguyễn Huệ, Phường Bến Nghé', 'TP. Hồ Chí Minh', 'Quận 1',
    10.7731000, 106.7030000,
    'Sáng T7-CN (8h-12h)', 'HIGH',
    NOW() + INTERVAL '3 days', 'DEAL', 150000, 2),
(2, 3, 1, NULL,
    'Chập điện ổ cắm phòng khách',
    'Ổ cắm phòng khách bị chập, có tia lửa khi cắm thiết bị. Cần kiểm tra và thay thế gấp.',
    '789 Võ Văn Ngân, Phường Linh Chiểu', 'TP. Hồ Chí Minh', 'Thủ Đức',
    10.8510000, 106.7590000,
    'Chiều T2-T6 (14h-18h)', 'URGENT',
    NOW() + INTERVAL '2 days', 'BIDDING', 0, 3);

SELECT setval('repair_requests_id_seq', 2);

-- ============================================================
-- BIDDING_SESSIONS
-- ============================================================
-- Request 1: Đã đóng (có deal)
INSERT INTO bidding_sessions (id, repair_request_id, start_time, end_time, max_bids, current_bid_count, auto_close_on_accept, status, created_by) VALUES
(1, 1, NOW() - INTERVAL '5 days', NOW() - INTERVAL '2 days', 5, 2, TRUE, 'CLOSED', 2);

-- Request 2: Đang mở
INSERT INTO bidding_sessions (id, repair_request_id, start_time, end_time, max_bids, current_bid_count, auto_close_on_accept, status, created_by) VALUES
(2, 2, NOW() - INTERVAL '1 day', NOW() + INTERVAL '2 days', 10, 2, FALSE, 'OPEN', 3);

SELECT setval('bidding_sessions_id_seq', 2);

-- ============================================================
-- BIDDING_BIDS
-- ============================================================
-- Bids cho Request 1 (máy lạnh)
INSERT INTO bidding_bids (id, bidding_session_id, repair_request_id, technician_id, price, message, estimated_duration_hours, status, created_by) VALUES
(1, 1, 1, 4, 500000,
    'Chào anh/chị, em có 8 năm kinh nghiệm sửa Daikin. Giá 500k bao gồm kiểm tra + nạp gas nếu cần. Bảo hành 3 tháng.',
    2.0, 'ACCEPTED', 4),
(2, 1, 1, 5, 650000,
    'Em báo giá 650k, bao gồm kiểm tra toàn bộ hệ thống và thay linh kiện nếu hỏng.',
    3.0, 'REJECTED', 5);

-- Bids cho Request 2 (chập điện)
INSERT INTO bidding_bids (id, bidding_session_id, repair_request_id, technician_id, price, message, estimated_duration_hours, status, created_by) VALUES
(3, 2, 2, 5, 300000,
    'Em chuyên sửa điện, sẽ kiểm tra toàn bộ hệ thống điện phòng khách. Bao gồm thay ổ cắm mới.',
    1.5, 'PENDING', 5),
(4, 2, 2, 4, 350000,
    'Em có thể đến chiều nay. Giá bao gồm kiểm tra + thay ổ cắm Panasonic chính hãng.',
    1.0, 'PENDING', 4);

SELECT setval('bidding_bids_id_seq', 4);

-- ============================================================
-- DEAL_SESSIONS - Deal cho Request 1
-- ============================================================
INSERT INTO deal_sessions (id, repair_request_id, bid_id, technician_id, agreed_price, status, created_by) VALUES
(1, 1, 1, 4, 500000, 'COMPLETED', 2);

SELECT setval('deal_sessions_id_seq', 1);

-- Update active_deal_id cho repair_request 1
UPDATE repair_requests SET active_deal_id = 1 WHERE id = 1;

-- ============================================================
-- APPOINTMENTS - Lịch hẹn cho Request 1
-- ============================================================
INSERT INTO appointments (id, repair_request_id, customer_id, technician_id, scheduled_date, scheduled_time, status, notes, created_by) VALUES
(1, 1, 2, 4, CURRENT_DATE + INTERVAL '1 day', '09:00', 'CONFIRMED',
    'Khách hàng ở nhà buổi sáng. Gọi trước 30 phút khi đến.', 4);

SELECT setval('appointments_id_seq', 1);

-- ============================================================
-- WORK_PROGRESS - Lịch sử trạng thái Request 1
-- ============================================================
INSERT INTO work_progress (repair_request_id, from_status, to_status, note, created_by) VALUES
(1, 'OPEN',       'BIDDING',     'Phiên đấu giá được mở',                          2),
(1, 'BIDDING',    'DEAL',        'Khách hàng chọn báo giá của thợ Lê Văn Đức',    2),
(1, 'DEAL',       'IN_PROGRESS', 'Thợ xác nhận và bắt đầu công việc',             4);

-- ============================================================
-- INSPECTION_RECORDS - Khảo sát Request 1
-- ============================================================
INSERT INTO inspection_records (id, repair_request_id, technician_id, inspection_notes, findings, created_by) VALUES
(1, 1, 4,
    'Kiểm tra máy lạnh Daikin FTKC35TVMV - 1.5HP Inverter, sản xuất 2022.',
    'Dàn nóng không quay do hết gas R32. Board mạch hoạt động bình thường. Cần nạp gas và kiểm tra rò rỉ đường ống.',
    4);

SELECT setval('inspection_records_id_seq', 1);

-- ============================================================
-- ADDITIONAL_COST_REQUESTS - Phát sinh
-- ============================================================
INSERT INTO additional_cost_requests (id, repair_request_id, technician_id, description, amount, status, reason, created_by) VALUES
(1, 1, 4,
    'Phát hiện rò rỉ gas tại mối nối ống đồng, cần hàn lại',
    150000, 'APPROVED',
    'Mối hàn cũ bị nứt gây rò rỉ gas R32, cần hàn bạc lại để đảm bảo kín.',
    4);

SELECT setval('additional_cost_requests_id_seq', 1);

-- ============================================================
-- PAYMENTS - Thanh toán
-- ============================================================
-- Đặt cọc 30% cho Request 1
INSERT INTO payments (id, repair_request_id, customer_id, technician_id, amount, type, method, status, transaction_ref, paid_at, created_by) VALUES
(1, 1, 2, 4, 150000, 'DEPOSIT', 'MOMO', 'COMPLETED', 'MOMO_TXN_20240915_001', NOW() - INTERVAL '3 days', 2),
-- Thanh toán phần còn lại + phát sinh
(2, 1, 2, 4, 500000, 'FINAL',   'CASH', 'COMPLETED', NULL, NOW() - INTERVAL '1 day', 2);

SELECT setval('payments_id_seq', 2);

-- ============================================================
-- WARRANTIES - Bảo hành
-- ============================================================
INSERT INTO warranties (id, repair_request_id, technician_id, warranty_terms, start_date, end_date, status, created_by) VALUES
(1, 1, 4,
    'Bảo hành 3 tháng cho dịch vụ nạp gas và hàn ống đồng máy lạnh Daikin. Không bao gồm hư hỏng do thiên tai hoặc sử dụng sai cách.',
    CURRENT_DATE, CURRENT_DATE + INTERVAL '3 months', 'ACTIVE', 4);

SELECT setval('warranties_id_seq', 1);

-- ============================================================
-- REVIEWS - Đánh giá
-- ============================================================
INSERT INTO reviews (id, repair_request_id, customer_id, technician_id, rating, comment, created_by) VALUES
(1, 1, 2, 4, 5,
    'Thợ Đức rất chuyên nghiệp, đến đúng giờ, sửa nhanh và giải thích rõ ràng nguyên nhân hỏng. Giá cả hợp lý. Sẽ gọi lại lần sau!',
    2);

SELECT setval('reviews_id_seq', 1);

-- ============================================================
-- TECHNICIAN_WALLET_TRANSACTIONS - Giao dịch ví
-- ============================================================
INSERT INTO technician_wallet_transactions (technician_id, amount, type, ref_id, created_by) VALUES
(4, 500000,  'EARNING',    1, 1),   -- Thu nhập từ Request 1
(4, 150000,  'EARNING',    1, 1),   -- Thu nhập phát sinh từ Request 1
(4, -200000, 'WITHDRAWAL', NULL, 4); -- Rút tiền

-- ============================================================
-- NOTIFICATIONS - Thông báo mẫu
-- ============================================================
INSERT INTO notifications (user_id, type, title, message, ref_id, ref_type, is_read, created_by) VALUES
(2, 'BID_RECEIVED',     'Có báo giá mới',                 'Thợ Lê Văn Đức đã gửi báo giá 500,000đ cho yêu cầu "Máy lạnh không lạnh".',       1, 'BIDDING_BID',    TRUE,  1),
(2, 'BID_RECEIVED',     'Có báo giá mới',                 'Thợ Phạm Văn Minh đã gửi báo giá 650,000đ cho yêu cầu "Máy lạnh không lạnh".',    2, 'BIDDING_BID',    TRUE,  1),
(4, 'BID_ACCEPTED',     'Báo giá được chấp nhận',         'Khách hàng Nguyễn Văn A đã chọn báo giá của bạn cho yêu cầu "Máy lạnh không lạnh".',  1, 'DEAL_SESSION',   TRUE,  1),
(4, 'PAYMENT_RECEIVED', 'Nhận thanh toán',                 'Bạn đã nhận thanh toán 500,000đ cho công việc "Máy lạnh không lạnh".',             2, 'PAYMENT',        TRUE,  1),
(2, 'REVIEW_REMINDER',  'Đánh giá dịch vụ',               'Hãy đánh giá thợ Lê Văn Đức cho dịch vụ vừa hoàn thành.',                        1, 'REPAIR_REQUEST', TRUE,  1),
(3, 'BID_RECEIVED',     'Có báo giá mới',                 'Thợ Phạm Văn Minh đã gửi báo giá 300,000đ cho yêu cầu "Chập điện ổ cắm".',       3, 'BIDDING_BID',    FALSE, 1),
(3, 'BID_RECEIVED',     'Có báo giá mới',                 'Thợ Lê Văn Đức đã gửi báo giá 350,000đ cho yêu cầu "Chập điện ổ cắm".',          4, 'BIDDING_BID',    FALSE, 1);

-- ============================================================
-- MEDIA - Ảnh đính kèm
-- ============================================================
INSERT INTO media (owner_type, owner_id, media_type, url, uploaded_by) VALUES
('REPAIR_REQUEST', 1, 'IMAGE', 'uploads/repair_requests/1/daikin_front.jpg',    2),
('REPAIR_REQUEST', 1, 'IMAGE', 'uploads/repair_requests/1/daikin_error.jpg',    2),
('REPAIR_REQUEST', 2, 'IMAGE', 'uploads/repair_requests/2/outlet_damage.jpg',   3),
('REPAIR_REQUEST', 2, 'VIDEO', 'uploads/repair_requests/2/spark_video.mp4',     3),
('INSPECTION_RECORD', 1, 'IMAGE', 'uploads/inspections/1/gas_leak_point.jpg',   4),
('INSPECTION_RECORD', 1, 'IMAGE', 'uploads/inspections/1/pipe_crack.jpg',       4);

-- ============================================================
-- DONE: Seed data loaded successfully
-- ============================================================
