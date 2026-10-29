---
title: "Vòng đời và trạng thái yêu cầu sửa chữa trên FixLink"
audience: [customer, technician, admin]
updated: "2026-10-24"
confidence: verified
---

# Vòng đời và trạng thái yêu cầu sửa chữa

## 1. Sơ đồ chu trình trạng thái hiện hành
Mô hình FixLink: **thợ gửi báo giá cạnh tranh, khách hàng so sánh và chọn thợ**.

```
DRAFT (Nháp) → OPEN (Đang nhận báo giá) → ASSIGNED (Đã chọn thợ)
             → DEPOSIT_PAID (Đã đặt cọc – khách xác nhận lịch hẹn và trả cọc 30% qua QR)
             → INSPECTING (Đang khảo sát)
             → AWAITING_COST_APPROVAL (Chờ duyệt chi phí phát sinh) [Tùy chọn]
             → IN_PROGRESS (Đang sửa chữa)
             → AWAITING_ACCEPTANCE (Chờ thanh toán phần còn lại, đánh giá & nghiệm thu)
             → COMPLETED (Hoàn tất) hoặc CANCELLED (Đã hủy)
```

## 2. Giải nghĩa chi tiết từng trạng thái

| Mã trạng thái | Tên hiển thị | Ý nghĩa nghiệp vụ | Hành động tiếp theo |
|---|---|---|---|
| `DRAFT` | Bản nháp | Khách đã nhập thông tin nhưng chưa đăng | Khách bấm "Đăng yêu cầu" |
| `PENDING` | Chờ duyệt | Yêu cầu đang chờ hệ thống duyệt (nếu có cấu hình) | Chờ hệ thống chuyển sang `OPEN` |
| `OPEN` | Đang tìm thợ / nhận báo giá | Yêu cầu công khai trên bảng việc tối đa 3 ngày; thợ phù hợp gửi báo giá | Khách xem các báo giá và bấm "Chọn thợ này" |
| `ASSIGNED` | Đã chọn thợ | Khách đã chấp nhận một báo giá; giá chốt được ghi nhận | Thợ đặt lịch hẹn → khách xác nhận lịch → khách quét QR đặt cọc 30% |
| `DEPOSIT_PAID` | Đã đặt cọc | Khách đã thanh toán cọc 30% qua QR; FixLink giữ bảo chứng | Thợ đến theo lịch và bắt đầu khảo sát |
| `INSPECTING` | Đang khảo sát | Thợ đã đến hiện trường để kiểm tra và chẩn đoán | Thợ bắt đầu sửa hoặc đề xuất chi phí phát sinh |
| `AWAITING_COST_APPROVAL` | Chờ duyệt phát sinh | Thợ gửi mức chi phí mới do hư hỏng ngoài mô tả ban đầu | Khách xem giải trình và đồng ý, hoặc "Từ chối giá & Đổi thợ khác" / hủy đơn |
| `IN_PROGRESS` | Đang sửa chữa | Thợ đang thay thế linh kiện, xử lý sự cố | Chờ thợ hoàn thành |
| `AWAITING_ACCEPTANCE` | Chờ nghiệm thu | Thợ đã sửa xong và báo hoàn thành; hệ thống tạo QR thanh toán phần còn lại | Khách kiểm tra, **thanh toán phần còn lại**, **chấm sao + nhận xét (bắt buộc)** rồi hoàn tất |
| `COMPLETED` | Đã hoàn thành | Khách đã thanh toán đủ, đánh giá và nghiệm thu; tiền giải ngân cho thợ | Đơn đóng lại; có vấn đề sau sửa chữa thì tạo phiếu hỗ trợ |
| `CANCELLED` | Đã hủy | Hết hạn 3 ngày chưa chọn được thợ, khách hủy trước khi sửa, hoặc khách không đồng ý phát sinh và hủy | Đơn đóng; có thể đăng yêu cầu mới |

## 3. Ai được thao tác gì?
| Người thao tác | Thao tác được phép |
|---|---|
| Khách hàng | Đăng đơn nháp; **chọn báo giá** (`OPEN` → `ASSIGNED`); **xác nhận lịch hẹn** và **đặt cọc qua QR** (`ASSIGNED` → `DEPOSIT_PAID`); **đổi thợ** khi `ASSIGNED`/`DEPOSIT_PAID`/`INSPECTING`/`AWAITING_COST_APPROVAL` (chọn thợ dự phòng, hoặc mở lại đơn về `OPEN`); đồng ý chi phí phát sinh (`AWAITING_COST_APPROVAL` → `IN_PROGRESS`); **thanh toán phần còn lại, đánh giá và nghiệm thu** (`AWAITING_ACCEPTANCE` → `COMPLETED`); hủy đơn khi chưa ở `IN_PROGRESS`/`AWAITING_ACCEPTANCE`/`COMPLETED`/`CANCELLED` |
| Thợ | Gửi/sửa/rút báo giá khi đơn `OPEN`; sau khi được chọn: đặt lịch hẹn; khi khách đã đặt cọc thì `DEPOSIT_PAID` → `INSPECTING`; `INSPECTING` → `IN_PROGRESS` hoặc `AWAITING_COST_APPROVAL`; `IN_PROGRESS` → `AWAITING_ACCEPTANCE` |
| Hệ thống | `OPEN` quá hạn 3 ngày → `CANCELLED` (tác vụ tự động chạy định kỳ khoảng 15 phút/lần) |

## 4. Trạng thái của một báo giá
- `PENDING` – chờ khách duyệt (thợ còn sửa hoặc rút được).
- `ACCEPTED` – khách đã chọn báo giá này.
- `REJECTED` – khách chọn thợ khác, khách đổi thợ, hoặc đơn bị hủy.
- `WITHDRAWN` – thợ đã rút báo giá.

## 5. Tab hiển thị trên trang "Yêu cầu của tôi"
- **Chờ thợ nhận:** `DRAFT`, `PENDING`, `OPEN`.
- **Đang thực hiện:** `ASSIGNED`, `DEPOSIT_PAID`, `INSPECTING`, `AWAITING_COST_APPROVAL`, `IN_PROGRESS`, `AWAITING_ACCEPTANCE`.
- **Hoàn thành:** `COMPLETED`. **Đã hủy:** `CANCELLED`.

## 6. Trạng thái cũ chỉ còn trong dữ liệu
- `BIDDING_OPEN`, `MATCHED_AWAITING_DEPOSIT` thuộc phiên bản cũ, chỉ còn để đọc dữ liệu cũ; đơn mới dùng `OPEN` → `ASSIGNED`.
