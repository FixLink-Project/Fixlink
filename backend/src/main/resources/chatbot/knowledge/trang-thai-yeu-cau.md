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
             → INSPECTING (Đang khảo sát – sau khi đã đặt lịch hẹn)
             → AWAITING_COST_APPROVAL (Chờ duyệt chi phí phát sinh) [Tùy chọn]
             → IN_PROGRESS (Đang sửa chữa)
             → AWAITING_ACCEPTANCE (Chờ đánh giá & nghiệm thu)
             → COMPLETED (Hoàn tất) hoặc CANCELLED (Đã hủy)
```

## 2. Giải nghĩa chi tiết từng trạng thái

| Mã trạng thái | Tên hiển thị | Ý nghĩa nghiệp vụ | Hành động tiếp theo |
|---|---|---|---|
| `DRAFT` | Bản nháp | Khách đã nhập thông tin nhưng chưa đăng | Khách bấm "Đăng yêu cầu" |
| `PENDING` | Chờ duyệt | Yêu cầu đang chờ hệ thống duyệt (nếu có cấu hình) | Chờ hệ thống chuyển sang `OPEN` |
| `OPEN` | Đang tìm thợ / nhận báo giá | Yêu cầu công khai trên bảng việc tối đa 3 ngày; thợ phù hợp gửi báo giá | Khách xem các báo giá và bấm "Chọn thợ này" |
| `ASSIGNED` | Đã chọn thợ | Khách đã chấp nhận một báo giá; giá chốt và tiền cọc 30% được ghi nhận | Thợ đặt lịch hẹn khảo sát trên hệ thống |
| `INSPECTING` | Đang khảo sát | Thợ đã đến hiện trường (sau khi có lịch hẹn) để kiểm tra và chẩn đoán | Thợ bắt đầu sửa hoặc đề xuất chi phí phát sinh |
| `AWAITING_COST_APPROVAL` | Chờ duyệt phát sinh | Thợ gửi mức chi phí mới do hư hỏng ngoài mô tả ban đầu | Khách xem giải trình và đồng ý, hoặc không đồng ý (có thể hủy đơn) |
| `IN_PROGRESS` | Đang sửa chữa | Thợ đang thay thế linh kiện, xử lý sự cố | Chờ thợ hoàn thành |
| `AWAITING_ACCEPTANCE` | Chờ nghiệm thu | Thợ đã sửa xong và báo hoàn thành | Khách kiểm tra, **chấm sao + nhận xét (bắt buộc)** rồi hoàn tất |
| `COMPLETED` | Đã hoàn thành | Khách đã đánh giá và nghiệm thu; phần còn lại được tất toán, giải ngân cho thợ | Đơn đóng lại; có vấn đề sau sửa chữa thì tạo phiếu hỗ trợ |
| `CANCELLED` | Đã hủy | Hết hạn 3 ngày chưa chọn được thợ, khách hủy trước khi sửa, hoặc khách không đồng ý phát sinh và hủy | Đơn đóng; có thể đăng yêu cầu mới |

## 3. Ai được thao tác gì?
| Người thao tác | Thao tác được phép |
|---|---|
| Khách hàng | Đăng đơn nháp; **chọn báo giá** (`OPEN` → `ASSIGNED`); đồng ý chi phí phát sinh (`AWAITING_COST_APPROVAL` → `IN_PROGRESS`); **đánh giá và nghiệm thu** (`AWAITING_ACCEPTANCE` → `COMPLETED`); hủy đơn khi chưa ở `IN_PROGRESS`/`AWAITING_ACCEPTANCE`/`COMPLETED`/`CANCELLED` |
| Thợ | Gửi/sửa/rút báo giá khi đơn `OPEN`; sau khi được chọn: đặt lịch hẹn, `ASSIGNED` → `INSPECTING`; `INSPECTING` → `IN_PROGRESS` hoặc `AWAITING_COST_APPROVAL`; `IN_PROGRESS` → `AWAITING_ACCEPTANCE` |
| Hệ thống | `OPEN` quá hạn 3 ngày → `CANCELLED` (tác vụ tự động chạy định kỳ khoảng 15 phút/lần) |

## 4. Trạng thái của một báo giá
- `PENDING` – chờ khách duyệt (thợ còn sửa hoặc rút được).
- `ACCEPTED` – khách đã chọn báo giá này.
- `REJECTED` – khách chọn thợ khác, hoặc đơn bị hủy.
- `WITHDRAWN` – thợ đã rút báo giá.

## 5. Tab hiển thị trên trang "Yêu cầu của tôi"
- **Chờ thợ nhận:** `DRAFT`, `PENDING`, `OPEN`.
- **Đang thực hiện:** `ASSIGNED`, `INSPECTING`, `AWAITING_COST_APPROVAL`, `IN_PROGRESS`, `AWAITING_ACCEPTANCE`.
- **Hoàn thành:** `COMPLETED`. **Đã hủy:** `CANCELLED`.

## 6. Trạng thái cũ chỉ còn trong dữ liệu
- `BIDDING_OPEN`, `MATCHED_AWAITING_DEPOSIT` thuộc phiên bản cũ, chỉ còn để đọc dữ liệu cũ; đơn mới dùng `OPEN` → `ASSIGNED`.
