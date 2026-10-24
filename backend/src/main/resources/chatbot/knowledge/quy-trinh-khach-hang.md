---
title: "Quy trình dành cho khách hàng trên FixLink"
audience: [guest, customer]
updated: "2026-10-24"
confidence: verified
---

# Quy trình dành cho khách hàng

## 1. Tóm tắt các bước sử dụng dịch vụ
1. **Đăng ký / Đăng nhập:** Đăng ký miễn phí tại `/dang-ky`, đăng nhập tại `/dang-nhap`.
2. **Đăng yêu cầu sửa chữa:** Chọn danh mục, mô tả sự cố, đính kèm ảnh hiện trường, chọn khu vực, nhập địa chỉ và **ngân sách dự kiến**.
3. **Nhận báo giá:** Yêu cầu mở (`OPEN`) trong 3 ngày. Các thợ phù hợp gửi **báo giá** (tiền công + vật tư, phương án sửa).
4. **So sánh và chọn thợ:** Khách xem các báo giá trong chi tiết đơn và bấm **"Chọn thợ này"**. Đơn chuyển sang `ASSIGNED`, giá chốt = báo giá được chọn, tiền cọc 30% được ghi nhận.
5. **Hẹn lịch & khảo sát:** Thợ đặt lịch hẹn khảo sát trên hệ thống rồi mới đến nơi kiểm tra (`INSPECTING`). Có phát sinh thì thợ gửi chi phí mới để khách duyệt (`AWAITING_COST_APPROVAL`). Thợ sửa (`IN_PROGRESS`).
6. **Đánh giá & nghiệm thu:** Thợ báo xong (`AWAITING_ACCEPTANCE`). Khách kiểm tra thiết bị, **chấm sao (1–5) và viết nhận xét (bắt buộc)** rồi hoàn tất nghiệm thu (`COMPLETED`); phần còn lại được tất toán và giải ngân cho thợ.

## 2. Hướng dẫn chi tiết cách đăng yêu cầu sửa chữa
- **Bước 1:** Đăng nhập bằng tài khoản khách hàng, vào **"Đăng yêu cầu"** (`/dang-yeu-cau`). Chỉ tài khoản Khách hàng đang hoạt động mới đăng được yêu cầu.
- **Bước 2:** Chọn **danh mục dịch vụ** (Điện lạnh, Điện nước, Điện gia dụng, Khóa & Cửa cuốn). Chưa biết chọn gì thì mô tả triệu chứng cho Trợ lý FixLink.
- **Bước 3:** Nhập **tiêu đề** (4–200 ký tự) và **mô tả sự cố** (5–5000 ký tự) – càng chi tiết (hãng, model, mã lỗi, hiện tượng) thợ càng báo giá sát.
- **Bước 4:** Tải lên **tối đa 6 ảnh/video** hiện trường (ảnh JPG, PNG, WebP tối đa 5 MB mỗi ảnh). Không bắt buộc nhưng rất nên có.
- **Bước 5:** Chọn **khu vực phục vụ** (quận tại TP.HCM) và nhập địa chỉ cụ thể.
- **Bước 6:** Nhập **ngân sách dự kiến** (bắt buộc, > 0 đồng) để thợ tham khảo khi báo giá. Bấm **"Đăng yêu cầu"** hoặc **lưu nháp**.
- Mỗi yêu cầu có mã dạng `REQ-YYYYMMDD-XXXXXX` để tra cứu.

## 3. Nhận và chọn báo giá
- Yêu cầu nhận báo giá tối đa **3 ngày (72 giờ)** ở trạng thái `OPEN`. Chỉ thợ đã duyệt KYC, **đúng chuyên môn và khu vực** mới báo giá được.
- Mỗi báo giá gồm: tiền công, tiền vật tư (nếu có), tổng giá, phương án sửa; kèm thông tin thợ: tên, điểm đánh giá trung bình, số việc đã hoàn thành.
- Thợ có thể điều chỉnh (trả giá lại) hoặc rút báo giá khi khách chưa chọn.
- Khách tự quyết định chọn thợ (không bắt buộc chọn giá thấp nhất). Khi bấm **"Chọn thợ này"**: báo giá đó được chấp nhận, các báo giá còn lại tự động bị từ chối. Trợ lý FixLink có thể giúp so sánh nhưng **không chọn thay khách**.
- Quá 3 ngày chưa chọn được thợ, yêu cầu **tự động chuyển sang `CANCELLED`**; khách có thể đăng lại, mô tả rõ hơn hoặc điều chỉnh ngân sách.
- Khách chỉ **sửa nội dung đơn** khi đơn là Nháp hoặc đang `OPEN` **và chưa có thợ nào gửi báo giá**.

## 4. Theo dõi đơn
- Xem tất cả đơn tại **"Yêu cầu của tôi"** (`/yeu-cau-cua-toi`), chia tab: Tất cả, Chờ thợ nhận, Đang thực hiện, Hoàn thành, Đã hủy.
- Chi tiết đơn (`/yeu-cau-cua-toi/{id}`): trạng thái, danh sách báo giá, thợ phụ trách, giá chốt, tiền cọc, lịch hẹn, tiến độ.

## 5. Đánh giá, nghiệm thu và giải ngân
- Khách không giao tiền mặt trực tiếp cho thợ ngoài hệ thống.
- Khi thợ báo hoàn thành (`AWAITING_ACCEPTANCE`), khách kiểm tra, vận hành thử thiết bị.
- **Bắt buộc chấm điểm 1–5 sao và viết nhận xét** trước khi hoàn tất. Điểm đánh giá được cộng vào điểm trung bình hiển thị trên hồ sơ thợ.
- Chỉ khi khách hoàn tất nghiệm thu, hệ thống mới giải ngân cho thợ. Chưa hài lòng thì đừng nghiệm thu – hãy trao đổi với thợ hoặc tạo phiếu hỗ trợ.
