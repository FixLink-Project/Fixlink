-- =============================================================================
-- V8 — Pivot sang mô hình "ai nhận trước được trước" (first-come-first-served)
--
-- Trước: khách mở thầu, nhiều thợ gửi báo giá cạnh tranh, khách chọn 1 báo giá.
-- Sau:   khách ghi ngân sách cố định (= giá cuối), thợ nào bấm "Nhận việc"
--        trước thì nhận luôn, không cần khách xác nhận.
-- =============================================================================

-- 1. bidding_deadline -> apply_deadline
--    Đổi ý nghĩa: hạn chót để THỢ NHẬN VIỆC, cố định createdAt + 3 ngày.
ALTER TABLE repair_requests RENAME COLUMN bidding_deadline TO apply_deadline;

-- 2. Backfill cho dữ liệu cũ chưa có hạn: lấy created_at + 3 ngày.
UPDATE repair_requests
SET apply_deadline = created_at + INTERVAL '3' DAY
WHERE apply_deadline IS NULL;

-- 3. Yêu cầu đang mở thầu theo mô hình cũ -> chuyển sang OPEN của mô hình mới.
UPDATE repair_requests
SET status = 'OPEN'
WHERE status = 'BIDDING_OPEN';

-- 4. quotations: thêm mốc thời điểm thợ nhận việc.
--    Trong mô hình mới, mỗi yêu cầu chỉ có tối đa 1 bản ghi quotations và nó
--    mang nghĩa "bản ghi nhận việc" chứ không còn là báo giá cạnh tranh.
ALTER TABLE quotations ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMP WITH TIME ZONE;

-- 5. Bản ghi cũ đã ở trạng thái ACCEPTED thì coi như nhận việc lúc cập nhật cuối.
UPDATE quotations
SET accepted_at = COALESCE(updated_at, created_at)
WHERE status = 'ACCEPTED' AND accepted_at IS NULL;
