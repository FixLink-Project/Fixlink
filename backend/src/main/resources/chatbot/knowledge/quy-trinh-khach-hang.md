---
title: "Quy trình dành cho khách hàng trên FixLink"
audience: [guest, customer]
updated: "2026-10-23"
confidence: verified
---

# Quy trình dành cho khách hàng

## 1. Tóm tắt các bước sử dụng dịch vụ
Khách hàng sử dụng dịch vụ FixLink theo quy trình 6 bước:
1. **Đăng ký / Đăng nhập:** Đăng ký miễn phí tại `/dang-ky`, đăng nhập tại `/dang-nhap`.
2. **Đăng yêu cầu sửa chữa:** Chọn danh mục, mô tả sự cố, đính kèm ảnh hiện trường, chọn khu vực, nhập địa chỉ và **ngân sách cố định**.
3. **Thợ nhận việc (ai nhận trước được trước):** Yêu cầu mở (`OPEN`) trong 3 ngày. Thợ phù hợp bấm "Nhận việc" trước sẽ nhận đơn, trạng thái chuyển sang `ASSIGNED`, tiền cọc 30% được ghi nhận.
4. **Hẹn lịch:** Khách và thợ thống nhất lịch hẹn khảo sát/sửa chữa trong trang chi tiết đơn; có thể dời lịch khi cần.
5. **Khảo sát & sửa chữa:** Thợ đến kiểm tra (`INSPECTING`). Nếu có chi phí phát sinh, thợ gửi để khách duyệt (`AWAITING_COST_APPROVAL`). Thợ sửa (`IN_PROGRESS`).
6. **Nghiệm thu & thanh toán:** Thợ báo xong (`AWAITING_ACCEPTANCE`), khách kiểm tra rồi bấm **Nghiệm thu** (`COMPLETED`); 70% còn lại được tất toán và giải ngân cho thợ.

## 2. Hướng dẫn chi tiết cách đăng yêu cầu sửa chữa
- **Bước 1:** Đăng nhập bằng tài khoản khách hàng, vào **"Đăng yêu cầu"** (`/dang-yeu-cau`). Chỉ tài khoản Khách hàng đang hoạt động mới đăng được yêu cầu; thợ không đăng yêu cầu được.
- **Bước 2:** Chọn **danh mục dịch vụ** (Điện lạnh, Điện nước, Điện gia dụng, Khóa & Cửa cuốn); có thể chọn thêm gói dịch vụ cụ thể. Chưa biết chọn gì thì mô tả triệu chứng cho Trợ lý FixLink để được gợi ý.
- **Bước 3:** Nhập **tiêu đề** (4–200 ký tự) và **mô tả sự cố** (5–5000 ký tự) – càng chi tiết (hãng, model, mã lỗi, hiện tượng) thợ càng chẩn đoán đúng.
- **Bước 4:** Tải lên **tối đa 6 ảnh/video** hiện trường (ảnh JPG, PNG, WebP tối đa 5 MB mỗi ảnh). Không bắt buộc nhưng rất nên có.
- **Bước 5:** Chọn **khu vực phục vụ** (quận tại TP.HCM) và nhập địa chỉ cụ thể.
- **Bước 6:** Nhập **ngân sách** (bắt buộc, > 0 đồng). Bấm **"Đăng yêu cầu"** để mở ngay cho thợ, hoặc **lưu nháp** để đăng sau.
- Mỗi yêu cầu có mã dạng `REQ-YYYYMMDD-XXXXXX` (ví dụ `REQ-20261023-A1B2C3`) để tra cứu.

## 3. Thời gian chờ thợ nhận việc
- Yêu cầu sau khi đăng có hiệu lực tối đa **3 ngày (72 giờ)** ở trạng thái `OPEN`.
- Chỉ thợ đã được duyệt KYC, **đúng chuyên môn và đúng khu vực** của đơn mới thấy và nhận được đơn.
- Quá 3 ngày không có thợ nhận, yêu cầu **tự động chuyển sang `CANCELLED`**. Khách có thể đăng lại, mô tả rõ hơn hoặc điều chỉnh ngân sách hợp lý hơn.
- Trong lúc chờ (đơn Nháp hoặc OPEN chưa có thợ nhận), khách có thể **sửa nội dung đơn** hoặc **hủy đơn**.

## 4. Theo dõi đơn
- Xem tất cả đơn tại **"Yêu cầu của tôi"** (`/yeu-cau-cua-toi`), chia tab: Tất cả, Chờ thợ nhận, Đang thực hiện, Hoàn thành, Đã hủy.
- Bấm vào một đơn (`/yeu-cau-cua-toi/{id}`) để xem trạng thái, thợ phụ trách, giá chốt, tiền cọc, lịch hẹn và tiến độ.
- Có thể hỏi Trợ lý: "đơn của tôi đang ở trạng thái nào?" – Trợ lý tra cứu dữ liệu thật khi bạn đã đăng nhập.

## 5. Nghiệm thu và giải ngân
- Khách không giao tiền mặt trực tiếp cho thợ ngoài hệ thống.
- Khi thợ báo hoàn thành (`AWAITING_ACCEPTANCE`), khách có quyền kiểm tra, vận hành thử thiết bị.
- Chỉ khi khách bấm **Nghiệm thu**, hệ thống mới giải ngân cho thợ. Nếu chưa hài lòng, đừng bấm nghiệm thu – hãy trao đổi với thợ hoặc tạo phiếu hỗ trợ.
