---
title: "Vòng đời và trạng thái yêu cầu sửa chữa trên FixLink"
audience: [customer, technician, admin]
updated: "2026-10-17"
confidence: verified
---

# Vòng đời và trạng thái yêu cầu sửa chữa

## 1. Sơ đồ chu trình trạng thái hiện hành
Mô hình nghiệp vụ FixLink vận hành theo luồng **"ai nhận trước được trước" (First-Come-First-Served)**:

```
DRAFT (Nháp) → OPEN (Đang tìm thợ) → ASSIGNED (Đã có thợ nhận)
             → INSPECTING (Đang khảo sát) 
             → AWAITING_COST_APPROVAL (Chờ duyệt chi phí phát sinh) [Tùy chọn]
             → IN_PROGRESS (Đang sửa chữa) 
             → AWAITING_ACCEPTANCE (Chờ nghiệm thu) 
             → COMPLETED (Hoàn tất) hoặc CANCELLED (Đã hủy)
```

## 2. Giải nghĩa chi tiết từng trạng thái hiện hành

| Mã trạng thái | Tên hiển thị | Ý nghĩa nghiệp vụ | Hành động tiếp theo |
|---|---|---|---|
| `DRAFT` | Bản nháp | Khách hàng đã nhập thông tin nhưng chưa bấm đăng công khai | Khách bấm "Đăng yêu cầu" để chuyển sang `OPEN` |
| `PENDING` | Chờ duyệt | Yêu cầu mới tạo đang qua bộ lọc kiểm duyệt nội dung (nếu có cấu hình) | Chờ hệ thống tự động duyệt lên `OPEN` |
| `OPEN` | Đang tìm thợ | Yêu cầu đã công khai trên bảng việc, có hiệu lực tối đa 3 ngày | Thợ phù hợp xem và bấm "Nhận việc" |
| `ASSIGNED` | Đã có thợ nhận | Một thợ kỹ thuật đã bấm nhận việc thành công | Thợ liên hệ khách để hẹn giờ đến sửa (`/appointments`) |
| `INSPECTING` | Đang khảo sát | Thợ đã đến hiện trường kiểm tra thực tế thiết bị và chẩn đoán lỗi | Thợ lập biên bản khảo sát và xác nhận phạm vi công việc |
| `AWAITING_COST_APPROVAL` | Chờ duyệt phát sinh | Thợ phát hiện hư hỏng phát sinh ngoài thỏa thuận ban đầu và gửi mức chi phí mới | Khách hàng vào chi tiết yêu cầu để Bấm Đồng ý hoặc Từ chối |
| `IN_PROGRESS` | Đang sửa chữa | Thợ đang trực tiếp thay thế linh kiện, xử lý sự cố thiết bị | Chờ thợ hoàn thành việc sửa chữa |
| `AWAITING_ACCEPTANCE` | Chờ nghiệm thu | Thợ đã sửa xong, dọn dẹp và gửi yêu cầu nghiệm thu lên hệ thống | Khách hàng kiểm tra thiết bị hoạt động rồi bấm "Nghiệm thu" |
| `COMPLETED` | Đã hoàn thành | Khách hàng đã nghiệm thu, 70% còn lại được tất toán và giải ngân cho thợ | Đơn đóng lại; nếu có vấn đề sau sửa chữa, tạo phiếu hỗ trợ |
| `CANCELLED` | Đã hủy | Yêu cầu bị hủy (do hết hạn 3 ngày không ai nhận, khách hủy trước khi sửa, hoặc khách không đồng ý chi phí phát sinh và hủy) | Yêu cầu đóng lại, không thể thao tác tiếp; có thể đăng yêu cầu mới |

## 3. Ai được chuyển trạng thái nào?
| Người thao tác | Chuyển trạng thái được phép |
|---|---|
| Khách hàng | `DRAFT` → đăng yêu cầu; `AWAITING_COST_APPROVAL` → `IN_PROGRESS` (đồng ý chi phí phát sinh); `AWAITING_ACCEPTANCE` → `COMPLETED` (nghiệm thu); hủy đơn khi chưa ở `IN_PROGRESS`/`AWAITING_ACCEPTANCE`/`COMPLETED`/`CANCELLED` |
| Thợ phụ trách | `OPEN` → `ASSIGNED` (bấm Nhận việc); `ASSIGNED` → `INSPECTING`; `INSPECTING` → `IN_PROGRESS` hoặc `AWAITING_COST_APPROVAL`; `IN_PROGRESS` → `AWAITING_ACCEPTANCE` |
| Hệ thống | `OPEN` quá hạn 3 ngày → `CANCELLED` (tác vụ tự động chạy định kỳ khoảng 15 phút/lần) |

## 4. Tab hiển thị trên trang "Yêu cầu của tôi"
- **Chờ thợ nhận:** `DRAFT`, `PENDING`, `OPEN`.
- **Đang thực hiện:** `ASSIGNED`, `INSPECTING`, `AWAITING_COST_APPROVAL`, `IN_PROGRESS`, `AWAITING_ACCEPTANCE`.
- **Hoàn thành:** `COMPLETED`. **Đã hủy:** `CANCELLED`.

## 5. Các trạng thái cũ ĐÃ BỎ (Deprecated - Tuyệt đối không dùng)
- `BIDDING_OPEN`: Thuộc mô hình đấu giá ngược cũ (nhiều thợ gửi báo giá cạnh tranh). **Mô hình hiện hành KHÔNG DÙNG trạng thái này.**
- `MATCHED_AWAITING_DEPOSIT`: Thuộc mô hình cũ (khách chọn thợ xong mới chờ cọc). **Mô hình hiện hành chuyển thẳng từ `OPEN` sang `ASSIGNED` ngay khi thợ bấm nhận việc.**
- Các trạng thái này chỉ tồn tại trong cơ sở dữ liệu để tương thích ngược với dữ liệu cũ từ các phiên bản trước. Bot tuyệt đối không giới thiệu các trạng thái này như một phần quy trình hiện tại.
