---
title: "Quy định sửa đơn, hủy đơn, chi phí phát sinh và lịch hẹn"
audience: [guest, customer, technician, admin]
updated: "2026-10-23"
confidence: verified
---

# Quy định sửa đơn, hủy đơn, chi phí phát sinh và lịch hẹn

## 1. Sửa nội dung yêu cầu sửa chữa
- Khách hàng chỉ sửa được đơn khi đơn đang là **Bản nháp (`DRAFT`)** hoặc **Đang tìm thợ (`OPEN`) và chưa có thợ nhận**.
- Khi thợ đã nhận việc, nội dung đơn được khóa để bảo vệ thỏa thuận giữa hai bên. Nếu cần thay đổi lớn (địa chỉ, thiết bị khác), hãy trao đổi với thợ; trường hợp không thống nhất được thì hủy đơn (nếu còn được hủy) và đăng đơn mới.
- Danh mục được chọn phải đang hoạt động. Có thể bổ sung ảnh sau khi tạo đơn nhưng tổng số tệp không vượt quá **6**.

## 2. Hủy yêu cầu sửa chữa (khách hàng)
| Trạng thái đơn | Được hủy? |
|---|---|
| Bản nháp (`DRAFT`), Đang tìm thợ (`OPEN`) | Có |
| Đã có thợ nhận (`ASSIGNED`), Đang khảo sát (`INSPECTING`) | Có |
| Chờ duyệt chi phí phát sinh (`AWAITING_COST_APPROVAL`) | Có (ví dụ khi không đồng ý mức phát sinh) |
| Đang sửa chữa (`IN_PROGRESS`), Chờ nghiệm thu (`AWAITING_ACCEPTANCE`) | **Không** |
| Đã hoàn thành (`COMPLETED`), Đã hủy (`CANCELLED`) | **Không** |
- Khi hủy cần nhập **lý do hủy**. Lý do được lưu trong lịch sử tiến độ của đơn.
- Cách hủy: **Yêu cầu của tôi** (`/yeu-cau-cua-toi`) → mở đơn → **Hủy yêu cầu** → nhập lý do → xác nhận.
- Trợ lý AI **không hủy đơn hộ** người dùng.
- Phí/khoản hoàn cọc khi hủy sau khi thợ đã nhận việc: `[CẦN XÁC NHẬN]` – nếu phát sinh tranh chấp tiền cọc, tạo phiếu hỗ trợ để Admin thẩm định.
- Đơn `OPEN` quá **3 ngày** không có thợ nhận sẽ được hệ thống **tự động hủy**.

## 3. Chi phí phát sinh
- Chỉ phát sinh sau khi thợ đã đến khảo sát (`INSPECTING`) và phát hiện hư hỏng ngoài mô tả ban đầu.
- Thợ gửi mức giá mới kèm giải trình; đơn chuyển sang **Chờ duyệt chi phí phát sinh** (`AWAITING_COST_APPROVAL`).
- Khách vào chi tiết đơn để xem giải trình:
  - **Đồng ý** → đơn chuyển sang `IN_PROGRESS`, giá chốt cập nhật theo giá mới và tiền cọc tính lại bằng 30% giá mới.
  - **Không đồng ý** → không bấm đồng ý; khách có thể trao đổi lại với thợ hoặc **hủy đơn**.
- Thợ **không được tự ý sửa phần phát sinh** khi khách chưa đồng ý, và không được thu tiền ngoài hệ thống.

## 4. Lịch hẹn khảo sát / sửa chữa
- Lịch hẹn được tạo cho đơn **đã có thợ nhận**; chỉ khách hàng chủ đơn và thợ phụ trách mới xem và thao tác được lịch hẹn của đơn đó.
- Ngày và giờ hẹn là bắt buộc và phải **từ thời điểm hiện tại trở đi**.
- Trạng thái lịch hẹn:
  - `CONFIRMED` (Đã xác nhận) – lịch vừa tạo.
  - `RESCHEDULED` (Đã dời lịch) – có thể dời nhiều lần.
  - `COMPLETED` (Đã hoàn thành) và `CANCELLED` (Đã hủy) là trạng thái cuối, không thao tác tiếp được.
- **Hủy lịch hẹn bắt buộc nhập lý do.** Hủy lịch hẹn không tự động hủy yêu cầu sửa chữa; hai bên có thể tạo lịch hẹn mới.
- Thợ đến trễ hoặc không đến đúng hẹn: khách nên liên hệ thợ để dời lịch; nếu thợ không đến, tạo phiếu hỗ trợ (ưu tiên High) để CSKH can thiệp.

## 5. Những việc Trợ lý AI không làm thay người dùng
Đăng/sửa/hủy đơn, nhận việc, duyệt chi phí phát sinh, nghiệm thu, đặt/dời/hủy lịch hẹn, đổi mật khẩu, thanh toán. Trợ lý chỉ tra cứu thông tin, hướng dẫn từng bước và tạo phiếu hỗ trợ khi người dùng đồng ý.
