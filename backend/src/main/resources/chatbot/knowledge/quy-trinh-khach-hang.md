---
title: "Quy trình dành cho khách hàng trên FixLink"
audience: [guest, customer]
updated: "2026-10-17"
confidence: verified
---

# Quy trình dành cho khách hàng

## 1. Tóm tắt các bước sử dụng dịch vụ
Khách hàng sử dụng dịch vụ FixLink theo quy trình 6 bước khép kín:
1. **Đăng ký / Đăng nhập tài khoản:** Khách hàng đăng ký tài khoản bằng SĐT/Username và mật khẩu.
2. **Đăng yêu cầu sửa chữa:** Chọn danh mục dịch vụ, mô tả triệu chứng hư hỏng, tải ảnh hiện trường, chọn khu vực và đặt mức giá cố định (ngân sách dự kiến).
3. **Thợ nhận việc (First-Come-First-Served):** Yêu cầu được mở (`OPEN`). Thợ phù hợp bấm "Nhận việc". Người bấm trước nhận việc thành công, trạng thái chuyển sang `ASSIGNED`.
4. **Hẹn lịch làm việc (Appointments):** Khách hàng và thợ thống nhất lịch hẹn khảo sát/sửa chữa qua tính năng đặt lịch.
5. **Khảo sát & Thực hiện sửa chữa:** Thợ đến tận nơi kiểm tra (`INSPECTING`). Nếu có chi phí phát sinh, thợ báo để khách duyệt (`AWAITING_COST_APPROVAL`). Sau khi khách đồng ý, thợ tiến hành sửa chữa (`IN_PROGRESS`).
6. **Nghiệm thu, Thanh toán & Đánh giá:** Khách hàng kiểm tra thiết bị hoạt động tốt thì bấm Nghiệm thu (`COMPLETED`), tiền được giải ngân từ tài khoản trung gian sang ví thợ, và khách đánh giá chất lượng phục vụ.

## 2. Hướng dẫn chi tiết cách đăng yêu cầu sửa chữa
- **Bước 1:** Đăng nhập vào FixLink và vào mục **"Đăng yêu cầu"** (hoặc truy cập `/dang-yeu-cau`).
- **Bước 2:** Chọn **Danh mục dịch vụ** phù hợp với thiết bị (Điện lạnh, Điện nước, Gia dụng, Khóa & Cửa cuốn). Nếu chưa biết chọn danh mục nào, hãy mô tả triệu chứng cho Trợ lý FixLink để được gợi ý chính xác.
- **Bước 3:** Nhập tiêu đề và nội dung mô tả sự cố (càng chi tiết càng giúp thợ chẩn đoán đúng lỗi).
- **Bước 4:** Tải lên tối thiểu 1 hình ảnh hoặc video ngắn chụp rõ hiện trường sự cố thiết bị.
- **Bước 5:** Chọn **Khu vực phục vụ** (Quận/Huyện tại TP.HCM) và nhập địa chỉ cụ thể.
- **Bước 6:** Nhập mức giá mong muốn (ngân sách cố định) và bấm **"Đăng yêu cầu"**.

## 3. Thời gian chờ thợ nhận việc
- Một yêu cầu sửa chữa sau khi đăng sẽ có hiệu lực tối đa **3 ngày (72 giờ)** ở trạng thái `OPEN`.
- Trong thời gian này, bất kỳ thợ lành nghề nào thuộc đúng khu vực và chuyên môn đều có thể bấm nhận việc.
- Nếu sau 3 ngày không có thợ nhận, yêu cầu sẽ tự động hết hạn và chuyển sang trạng thái `CANCELLED`. Khách hàng có thể đăng lại yêu cầu hoặc điều chỉnh mức ngân sách hợp lý hơn.

## 4. Nghiệm thu và giải ngân
- Khách hàng không giao tiền mặt trực tiếp trước khi công việc hoàn thành.
- Tiền cọc/chi phí được bảo đảm trong tài khoản trung gian của FixLink.
- Khi thợ hoàn thành và báo `AWAITING_ACCEPTANCE`, khách hàng có quyền kiểm tra, vận hành thử thiết bị.
- Chỉ khi khách hàng bấm **Nghiệm thu**, hệ thống mới chính thức giải ngân cho thợ và kích hoạt thời hạn bảo hành.
