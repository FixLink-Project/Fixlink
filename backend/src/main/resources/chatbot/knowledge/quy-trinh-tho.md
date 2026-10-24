---
title: "Quy trình dành cho kỹ thuật viên (thợ) trên FixLink"
audience: [guest, technician, admin]
updated: "2026-10-23"
confidence: verified
---

# Quy trình dành cho kỹ thuật viên (thợ)

## 1. Đăng ký tài khoản và xác minh danh tính (KYC)
1. **Đăng ký tài khoản thợ** tại `/dang-ky-tho`: Họ tên, Số điện thoại (định dạng Việt Nam), Email, Mật khẩu (tối thiểu 8 ký tự, có ít nhất 1 chữ in hoa và 1 ký tự đặc biệt), Số CCCD, Địa chỉ, Chuyên môn, Số năm kinh nghiệm, Giới thiệu bản thân.
2. **Nộp giấy tờ KYC:** ảnh mặt trước và mặt sau CCCD/CMND (rõ nét, đủ 4 góc, không lóa).
3. Số điện thoại, email và số CCCD **không được trùng** với hồ sơ thợ khác.
4. **Chờ Admin duyệt:**
   - Sau khi đăng ký, tài khoản ở trạng thái **chờ duyệt** và **chưa đăng nhập được** cho tới khi Admin phê duyệt.
   - Hồ sơ KYC: `PENDING` (chờ xét duyệt) → `APPROVED` (đã xác minh) hoặc `REJECTED` (bị từ chối, kèm lý do cụ thể).
   - Bị từ chối: đọc lý do, cập nhật giấy tờ tại `/tho/ho-so` hoặc tạo phiếu hỗ trợ.
5. Sau khi được duyệt, thợ đăng nhập tại **`/dang-nhap-tho`**.

## 2. Điều kiện để nhận việc
Thợ chỉ bấm "Nhận việc" thành công khi **đồng thời** thỏa mãn:
- Hồ sơ KYC đã `APPROVED` (nếu chưa: lỗi `UNVERIFIED_TECHNICIAN`, HTTP 403).
- **Chuyên môn** của thợ khớp danh mục của yêu cầu.
- **Khu vực hoạt động** của thợ khớp khu vực của yêu cầu.
- Yêu cầu đang `OPEN` và **còn trong hạn 3 ngày**.
Bảng việc tại `/tho` chỉ hiển thị các yêu cầu đang mở khớp chuyên môn và khu vực của thợ.

## 3. Trạng thái Trực tuyến / Ngoại tuyến
- Thợ bật/tắt công tắc **Trực tuyến (Online)** tại trang hồ sơ `/tho/ho-so` để thể hiện mình đang sẵn sàng nhận việc.
- Trạng thái này **không ẩn/hiện bảng việc**: việc thợ nhìn thấy phụ thuộc vào KYC, chuyên môn và khu vực.

## 4. Quy trình nhận việc (ai nhận trước được trước)
- Thấy yêu cầu phù hợp trên bảng việc `/tho`, thợ bấm **"Nhận việc"**. Giá công việc = ngân sách khách đã đặt; thợ không trả giá.
- FixLink áp dụng nguyên tắc **ai bấm trước được việc trước**, dùng khóa bi quan (pessimistic lock) ở máy chủ nên hai thợ bấm cùng lúc chỉ một người thành công.
- **Lỗi `JOB_ALREADY_TAKEN` (HTTP 409):** thợ khác đã nhận trước. Hãy chọn việc khác.
- Thợ không thể nhận cùng một yêu cầu hai lần.

## 5. Thực hiện công việc
1. Nhận việc thành công → đơn `ASSIGNED`, tiền cọc 30% được ghi nhận vào tài khoản trung gian.
2. Liên hệ khách, **tạo lịch hẹn** khảo sát trong trang chi tiết công việc `/tho/yeu-cau/{id}` (thời gian hẹn phải từ hiện tại trở đi).
3. Đến hiện trường, chuyển trạng thái sang **Đang khảo sát** (`INSPECTING`).
4. Khảo sát xong:
   - Sửa theo ngân sách đã chốt → chuyển sang **Đang sửa chữa** (`IN_PROGRESS`); hoặc
   - Có lỗi phát sinh → gửi **đề xuất chi phí mới** kèm giải trình (`AWAITING_COST_APPROVAL`). Thợ **chỉ được sửa phần phát sinh khi khách đồng ý**.
5. Sửa xong, chạy thử, chụp ảnh kết quả rồi báo **hoàn thành** (`AWAITING_ACCEPTANCE`).
6. Khách bấm **Nghiệm thu** (`COMPLETED`) → tiền công được giải ngân vào ví của thợ; số việc hoàn thành và điểm đánh giá trung bình hiển thị trên hồ sơ thợ.
