---
title: "Quy trình dành cho kỹ thuật viên (thợ) trên FixLink"
audience: [guest, technician]
updated: "2026-10-17"
confidence: verified
---

# Quy trình dành cho kỹ thuật viên (thợ)

## 1. Đăng ký tài khoản và Quy trình xác minh danh tính (KYC)
Để trở thành thợ chính thức và có thể nhận việc trên FixLink, người dùng cần trải qua các bước:
1. **Đăng ký tài khoản thợ:** Truy cập `/dang-ky-tho`, điền thông tin cá nhân (Họ tên, SĐT, Email, Mật khẩu, Số CCCD, Địa chỉ cư trú).
2. **Nộp hồ sơ xác minh danh tính (KYC):**
   - Tải lên ảnh mặt trước CCCD / CMND.
   - Tải lên ảnh mặt sau CCCD / CMND.
   - Chọn chuyên môn ngành nghề (Điện lạnh, Điện nước, Gia dụng, Khóa & Cửa cuốn).
   - Chọn khu vực địa bàn hoạt động (các Quận tại TP.HCM).
3. **Chờ Admin duyệt hồ sơ:**
   - Trạng thái ban đầu: `PENDING` (Đang chờ xét duyệt).
   - Đội ngũ Quản trị viên (Admin) sẽ kiểm tra tính hợp lệ của giấy tờ tùy thân.
   - Khi được duyệt: trạng thái chuyển sang `APPROVED` (Đã xác minh).
   - Nếu hồ sơ bị từ chối: trạng thái là `REJECTED`, kèm lý do cụ thể hiển thị trên dashboard để thợ cập nhật lại.
4. **Quyền hạn khi chưa duyệt KYC:** Thợ ở trạng thái `PENDING` hoặc `REJECTED` **tuyệt đối không được bấm nhận việc**. Khi bấm nhận việc sẽ bị chặn bởi lỗi `UNVERIFIED_TECHNICIAN` (HTTP 403).

## 2. Trạng thái hoạt động (Online / Offline)
- Thợ có thể chủ động bật/tắt công tắc **Trực tuyến (is_online)** trên trang cá nhân `/tho/ho-so`.
- Khi ở trạng thái **Online (Trực tuyến)**, thợ sẽ nhận được thông báo về các yêu cầu sửa chữa mới xuất hiện trong khu vực và chuyên môn đã đăng ký.
- Khi **Offline (Ngoại tuyến)**, thợ không bị làm phiền bởi thông báo công việc mới.

## 3. Quy trình nhận việc (First-Come-First-Served)
- Khi thấy yêu cầu phù hợp ở trạng thái `OPEN` trong trang Thợ (`/tho`), thợ bấm nút **"Nhận việc"**.
- FixLink áp dụng nguyên tắc **ai bấm trước được việc trước**.
- **Lỗi `JOB_ALREADY_TAKEN` (HTTP 409):** Nếu có một thợ khác đã bấm nhận việc trước bạn dù chỉ vài phần trăm giây, hệ thống sẽ trả về thông báo lỗi "Công việc này vừa được thợ khác nhận trước. Vui lòng chọn công việc khác!". Đây là cơ chế khóa cạnh tranh bi quan an toàn của FixLink.

## 4. Thực hiện công việc và Khảo sát phát sinh
1. Sau khi nhận việc thành công, yêu cầu chuyển sang trạng thái `ASSIGNED`.
2. Thợ liên hệ khách hàng để tạo lịch hẹn khảo sát (`/appointments`).
3. Đến hiện trường kiểm tra thiết bị, chuyển trạng thái sang `INSPECTING`.
4. Nếu kiểm tra thấy có lỗi phát sinh ngoài thỏa thuận ban đầu, thợ báo chi phí bổ sung kèm ghi chú chi tiết (`AWAITING_COST_APPROVAL`). Thợ **chỉ được tiến hành sửa** khi khách hàng đã bấm duyệt chi phí này.
5. Tiến hành sửa chữa (`IN_PROGRESS`).
6. Hoàn tất sửa chữa và báo nghiệm thu (`AWAITING_ACCEPTANCE`).
7. Khách hàng nghiệm thu thành công (`COMPLETED`), tiền công được chuyển tự động vào ví của thợ.
