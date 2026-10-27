---
title: "Quy định sửa đơn, hủy đơn, chi phí phát sinh và lịch hẹn"
audience: [guest, customer, technician, admin]
updated: "2026-10-23"
confidence: verified
---

# Quy định sửa đơn, hủy đơn, chi phí phát sinh và lịch hẹn

## 1. Sửa nội dung yêu cầu sửa chữa
- Khách hàng chỉ sửa được đơn khi đơn đang là **Bản nháp (`DRAFT`)** hoặc **Đang tìm thợ (`OPEN`) và chưa có thợ nào gửi báo giá**.
- Khi đã có báo giá hoặc đã chọn thợ, nội dung đơn được khóa để bảo vệ thỏa thuận giữa hai bên. Nếu cần thay đổi lớn (địa chỉ, thiết bị khác), hãy trao đổi với thợ; trường hợp không thống nhất được thì hủy đơn (nếu còn được hủy) và đăng đơn mới.
- Danh mục được chọn phải đang hoạt động. Có thể bổ sung ảnh sau khi tạo đơn nhưng tổng số tệp không vượt quá **6**.

## 2. Hủy yêu cầu sửa chữa (khách hàng)
| Trạng thái đơn | Được hủy? |
|---|---|
| Bản nháp (`DRAFT`), Đang tìm thợ (`OPEN`) | Có |
| Đã chọn thợ (`ASSIGNED`), Đã đặt cọc (`DEPOSIT_PAID`), Đang khảo sát (`INSPECTING`) | Có (hoặc dùng "Đổi thợ khác") |
| Chờ duyệt chi phí phát sinh (`AWAITING_COST_APPROVAL`) | Có (ví dụ khi không đồng ý mức phát sinh) |
| Đang sửa chữa (`IN_PROGRESS`), Chờ nghiệm thu (`AWAITING_ACCEPTANCE`) | **Không** |
| Đã hoàn thành (`COMPLETED`), Đã hủy (`CANCELLED`) | **Không** |
- Khi hủy cần nhập **lý do hủy**. Lý do được lưu trong lịch sử tiến độ của đơn.
- Cách hủy: **Yêu cầu của tôi** (`/yeu-cau-cua-toi`) → mở đơn → **Hủy yêu cầu** → nhập lý do → xác nhận.
- Trợ lý AI **không hủy đơn hộ** người dùng.
- Phí/khoản hoàn cọc khi hủy sau khi đã chọn thợ: `[CẦN XÁC NHẬN]` – nếu phát sinh tranh chấp tiền cọc, tạo phiếu hỗ trợ để Admin thẩm định.
- Đơn `OPEN` quá **3 ngày** không có thợ nhận sẽ được hệ thống **tự động hủy**.

## 3. Chi phí phát sinh
- Chỉ phát sinh sau khi thợ đã đến khảo sát (`INSPECTING`) và phát hiện hư hỏng ngoài mô tả ban đầu.
- Thợ gửi mức giá mới kèm giải trình; đơn chuyển sang **Chờ duyệt chi phí phát sinh** (`AWAITING_COST_APPROVAL`).
- Khách vào chi tiết đơn để xem giải trình:
  - **Đồng ý** → đơn chuyển sang `IN_PROGRESS`; khoản phát sinh được cộng vào đợt thanh toán cuối, **tiền cọc đã đóng không tính lại**.
  - **Không đồng ý** → bấm **"Từ chối giá & Đổi thợ khác"**, trao đổi lại với thợ, hoặc **hủy đơn**.
- Thợ cũng có thể thêm từng khoản trong mục **"Chi phí phát sinh"**; khách duyệt hoặc từ chối từng khoản.
- Thợ **không được tự ý sửa phần phát sinh** khi khách chưa đồng ý, và không được thu tiền ngoài hệ thống.

## 4. Lịch hẹn khảo sát / sửa chữa
- Lịch hẹn do **thợ đặt** cho đơn **đã chọn thợ**; chỉ khách hàng chủ đơn và thợ phụ trách mới xem và thao tác được lịch hẹn của đơn đó.
- **Chỉ khách hàng** được bấm **"✓ Xác nhận lịch hẹn"** hoặc từ chối. Khách xác nhận xong thì mã QR đặt cọc 30% tự hiện; khách đặt cọc thì thợ mới được bắt đầu khảo sát.
- **Chỉ thợ phụ trách** được bấm hoàn thành lịch hẹn/công việc.
- Ngày và giờ hẹn là bắt buộc và phải **từ thời điểm hiện tại trở đi**.
- Trạng thái lịch hẹn:
  - `PENDING_APPROVAL` (Chờ xác nhận) – lịch thợ vừa đặt, chờ khách xác nhận.
  - `CONFIRMED` (Đã xác nhận) – khách đã xác nhận.
  - `REJECTED` (Bị từ chối) – khách từ chối lịch; thợ đặt lịch khác.
  - `RESCHEDULED` (Đã dời lịch) – có thể dời nhiều lần.
  - `COMPLETED` (Đã hoàn thành) và `CANCELLED` (Đã hủy) là trạng thái cuối, không thao tác tiếp được.
- **Hủy lịch hẹn bắt buộc nhập lý do.** Hủy lịch hẹn không tự động hủy yêu cầu sửa chữa; hai bên có thể tạo lịch hẹn mới.
- Thợ đến trễ hoặc không đến đúng hẹn: khách nên liên hệ thợ để dời lịch; nếu thợ không đến, tạo phiếu hỗ trợ (ưu tiên High) để CSKH can thiệp.

## 5. Những việc Trợ lý AI không làm thay người dùng
Đăng/sửa/hủy đơn, gửi báo giá, chọn thợ, duyệt chi phí phát sinh, nghiệm thu, đặt/dời/hủy lịch hẹn, đổi mật khẩu, thanh toán. Trợ lý chỉ tra cứu thông tin, hướng dẫn từng bước và tạo phiếu hỗ trợ khi người dùng đồng ý.
