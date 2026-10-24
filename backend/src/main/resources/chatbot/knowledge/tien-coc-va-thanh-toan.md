---
title: "Chính sách đặt cọc và thanh toán trung gian"
audience: [guest, customer, technician, admin]
updated: "2026-10-23"
confidence: verified
---

# Chính sách đặt cọc và thanh toán trung gian

## 1. Giá dịch vụ được chốt như thế nào?
- Khách hàng tự đặt **ngân sách cố định** (bắt buộc, lớn hơn 0 đồng) khi đăng yêu cầu sửa chữa.
- Khi một thợ bấm **"Nhận việc"**, giá chốt (agreed price) của đơn **bằng đúng ngân sách khách đã đặt**. Thợ không tự nhập giá khác và không có bước trả giá.
- Nếu khi khảo sát thợ phát hiện hư hỏng phát sinh ngoài mô tả ban đầu, thợ gửi **đề xuất chi phí mới** (trạng thái `AWAITING_COST_APPROVAL`). Giá chốt chỉ thay đổi khi khách bấm **Đồng ý**.

## 2. Tỷ lệ đặt cọc: 30% – tất toán: 70%
- Ngay khi thợ nhận việc, hệ thống ghi nhận **tiền cọc = 30% giá chốt** (làm tròn lên đến đồng), được giữ an toàn trong **tài khoản trung gian (escrow)** của FixLink.
- **70% còn lại** được tất toán khi khách hàng bấm **Nghiệm thu**.
- Khi khách duyệt chi phí phát sinh, giá chốt mới được cập nhật và **tiền cọc được tính lại bằng 30% giá mới**.
- Ví dụ minh họa:
  - Ngân sách 300.000 đ → cọc 90.000 đ, tất toán 210.000 đ.
  - Ngân sách 500.000 đ → cọc 150.000 đ, tất toán 350.000 đ.
  - Ngân sách 1.000.000 đ → cọc 300.000 đ, tất toán 700.000 đ.
  - Đơn 500.000 đ, khách duyệt phát sinh lên 800.000 đ → cọc mới 240.000 đ, tất toán 560.000 đ.

### Tiền mua linh kiện thay thế có tính vào tiền đặt cọc không?
- Ngân sách khách đặt ban đầu là giá trọn gói cho công việc đã mô tả. Khi cần mua thêm linh kiện thay thế ngoài thỏa thuận, thợ đưa tiền linh kiện vào **đề xuất chi phí phát sinh** để khách duyệt.
- Khách đồng ý thì giá chốt mới (đã gồm tiền linh kiện) được cập nhật và tiền đặt cọc được tính lại bằng 30% giá mới; khách không đồng ý thì thợ không được tự mua/thay linh kiện và thu thêm tiền.

## 3. Cơ chế tài khoản trung gian (Escrow) bảo vệ hai bên
- Thợ yên tâm di chuyển và làm việc vì tiền công đã được bảo đảm.
- Khách hàng không mất tiền nếu thợ không đến hoặc không hoàn thành như cam kết: tranh chấp được Admin thẩm định qua phiếu hỗ trợ.
- **Chỉ khi khách hàng bấm Nghiệm thu**, hệ thống mới chính thức giải ngân tiền công vào ví của thợ.
- **Không giao tiền mặt cho thợ ngoài hệ thống.** Nếu thợ yêu cầu trả thêm tiền ngoài app, khách nên từ chối và tạo phiếu hỗ trợ.

## 4. Các nội dung chưa công bố chính thức
- **Tỷ lệ phần trăm hoa hồng/phí nền tảng:** `[CẦN XÁC NHẬN]` – đang được ban quản trị cập nhật.
- **Tỷ lệ phạt hoặc hoàn cọc khi hủy đơn:** `[CẦN XÁC NHẬN]` – mọi trường hợp hoàn cọc (ví dụ thợ không đến, vi phạm thỏa thuận) do Admin thẩm định qua phiếu hỗ trợ.
- **Cổng/phương thức thanh toán trực tuyến cụ thể** (ví điện tử, thẻ ngân hàng...): `[CẦN XÁC NHẬN]`.
- Khi được hỏi các nội dung trên, Trợ lý trả lời rõ là FixLink chưa công bố thông tin chính thức và đề nghị tạo phiếu hỗ trợ để bộ phận kế toán giải đáp.
