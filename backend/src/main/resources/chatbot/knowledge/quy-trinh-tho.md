---
title: "Quy trình dành cho kỹ thuật viên (thợ) trên FixLink"
audience: [guest, technician, admin]
updated: "2026-10-24"
confidence: verified
---

# Quy trình dành cho kỹ thuật viên (thợ)

## 1. Đăng ký tài khoản và xác minh danh tính (KYC)
1. **Đăng ký tài khoản thợ** tại `/dang-ky-tho`: Họ tên, Số điện thoại (định dạng Việt Nam), Email, Mật khẩu (tối thiểu 8 ký tự, có ít nhất 1 chữ in hoa và 1 ký tự đặc biệt), Số CCCD, Địa chỉ, Chuyên môn, Giới thiệu bản thân.
2. **Nộp giấy tờ KYC:** ảnh mặt trước và mặt sau CCCD/CMND (rõ nét, đủ 4 góc, không lóa).
3. Số điện thoại, email và số CCCD **không được trùng** với hồ sơ thợ khác.
4. **Chờ Admin duyệt:** tài khoản ở trạng thái chờ duyệt và **chưa đăng nhập được** cho tới khi Admin phê duyệt. Hồ sơ KYC: `PENDING` → `APPROVED` hoặc `REJECTED` (kèm lý do).
5. Sau khi được duyệt, thợ đăng nhập tại **`/dang-nhap-tho`**.

## 2. Điều kiện để gửi báo giá
Thợ chỉ gửi báo giá thành công khi **đồng thời**:
- Hồ sơ KYC đã `APPROVED` (nếu chưa: lỗi `UNVERIFIED_TECHNICIAN`, HTTP 403).
- **Chuyên môn** khớp danh mục và **khu vực hoạt động** khớp khu vực của yêu cầu.
- Yêu cầu đang `OPEN` và **còn trong hạn 3 ngày**.
Bảng việc tại `/tho` hiển thị các yêu cầu đang mở khớp chuyên môn và khu vực của thợ.

## 3. Gửi báo giá cạnh tranh
- Mở chi tiết yêu cầu (`/tho/yeu-cau/{id}`), nhập **giá tiền công** (bắt buộc) và tiền vật tư/phương án nếu có, bấm gửi.
- Mỗi thợ có **một báo giá** cho mỗi yêu cầu; khi báo giá còn **chờ khách duyệt** có thể **sửa (trả giá lại)** hoặc **rút báo giá**.
- **Sàn báo giá công khai (ẩn danh):** thợ thấy mức giá của các thợ khác dưới tên "Thợ ẩn danh #1, #2...", không thấy danh tính đối thủ, để chủ động cân nhắc giá.
- Khách hàng tự so sánh và chọn thợ (dựa trên giá, phương án, điểm đánh giá, số việc đã làm). Khi khách chọn báo giá của thợ: yêu cầu chuyển sang `ASSIGNED`, giá chốt = báo giá đó.
- **Báo giá (công + vật tư) không được vượt ngân sách khách đặt**, nếu vượt hệ thống báo lỗi.
- Báo giá của thợ không được chọn vẫn là **báo giá dự phòng**: nếu khách đổi thợ khi chưa sửa, khách có thể chuyển sang thợ dự phòng. Thợ đang phụ trách bị khách đổi thì **không nhận tiền cọc**.
- Muốn tăng khả năng được chọn: báo giá hợp lý, ghi rõ phương án sửa, giữ điểm đánh giá tốt và nhận việc đúng chuyên môn.

## 4. Trạng thái Trực tuyến / Ngoại tuyến
- Thợ bật/tắt **Trực tuyến (Online)** tại `/tho/ho-so` để thể hiện đang sẵn sàng. Trạng thái này **không ẩn/hiện bảng việc**.

## 5. Thực hiện công việc sau khi được chọn
1. **Đặt lịch hẹn khảo sát trên hệ thống** trong trang chi tiết công việc (thời gian hẹn từ hiện tại trở đi). Lịch ở trạng thái **Chờ xác nhận** cho đến khi khách xác nhận; khách từ chối thì đặt lịch khác.
2. Khách xác nhận lịch và **thanh toán cọc 30% qua QR** → đơn **Đã đặt cọc** (`DEPOSIT_PAID`). Chỉ khi đó mới đến hiện trường và chuyển sang **Đang khảo sát** (`INSPECTING`).
3. Khảo sát xong: sửa theo giá đã chốt → **Đang sửa chữa** (`IN_PROGRESS`); hoặc có lỗi phát sinh → gửi **đề xuất chi phí mới** (`AWAITING_COST_APPROVAL`) và **chỉ sửa phần phát sinh khi khách đồng ý**.
4. Sửa xong, chạy thử, báo **hoàn thành** (`AWAITING_ACCEPTANCE`).
5. Hệ thống tạo QR thanh toán phần còn lại cho khách. Khách **thanh toán, chấm sao + nhận xét** và hoàn tất nghiệm thu (`COMPLETED`) → tiền công được giải ngân cho thợ; điểm đánh giá trung bình và số việc hoàn thành cập nhật trên hồ sơ.
6. Thợ xem danh sách việc đã hoàn thành trong trang riêng của bảng điều khiển thợ.
