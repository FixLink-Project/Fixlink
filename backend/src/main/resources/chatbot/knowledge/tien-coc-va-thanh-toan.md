---
title: "Chính sách đặt cọc và thanh toán qua mã QR"
audience: [guest, customer, technician, admin]
updated: "2026-10-27"
confidence: verified
---

# Chính sách đặt cọc và thanh toán qua mã QR

## 1. Giá dịch vụ được chốt như thế nào?
- Khách hàng đặt **ngân sách dự kiến** (bắt buộc, lớn hơn 0 đồng) khi đăng yêu cầu. Đây là **trần giá**: báo giá của thợ (tiền công + vật tư) **không được vượt quá ngân sách** này.
- Khi khách bấm **"Chọn thợ này"**, giá chốt (agreed price) của đơn **bằng tổng báo giá được chọn**.
- Nếu khi khảo sát thợ phát hiện hư hỏng ngoài mô tả ban đầu, thợ gửi **đề xuất chi phí phát sinh** (trạng thái `AWAITING_COST_APPROVAL`, hoặc từng khoản trong mục "Chi phí phát sinh"). Chỉ khoản nào khách bấm **Đồng ý** mới được tính.

## 2. Hai đợt thanh toán: đặt cọc 30% và thanh toán phần còn lại
Mọi khoản thanh toán đều bằng **mã QR chuyển khoản ngân hàng (VietQR)** hiện trong trang chi tiết đơn.

**Đợt 1 – Đặt cọc 30%:**
1. Khách chọn thợ → đơn `ASSIGNED`.
2. Thợ đặt lịch hẹn khảo sát → lịch ở trạng thái **Chờ xác nhận**.
3. Khách bấm **"✓ Xác nhận lịch hẹn"** → mã QR đặt cọc **30% giá chốt** tự động hiện ra.
4. Khách quét QR, chuyển khoản đúng số tiền và nội dung, rồi bấm xác nhận đã chuyển khoản → đơn chuyển sang **Đã đặt cọc (`DEPOSIT_PAID`)**.
5. Thợ chỉ được bắt đầu khảo sát khi đơn đã đặt cọc.

**Đợt 2 – Thanh toán phần còn lại:**
- Khi thợ báo hoàn thành (`AWAITING_ACCEPTANCE`), hệ thống tự tạo mã QR thanh toán:
  **Phần còn lại = giá chốt + chi phí phát sinh đã duyệt − tiền cọc đã đóng.**
- Khách phải **thanh toán khoản này** và **chấm 1–5 sao + viết nhận xét** thì mới hoàn tất nghiệm thu (`COMPLETED`).

**Ví dụ minh họa:**
- Giá chốt 300.000 đ → cọc 90.000 đ, phần còn lại 210.000 đ.
- Giá chốt 500.000 đ → cọc 150.000 đ, phần còn lại 350.000 đ.
- Giá chốt 1.000.000 đ → cọc 300.000 đ, phần còn lại 700.000 đ.
- Giá chốt 500.000 đ, khách duyệt phát sinh thêm 300.000 đ → cọc **vẫn là 150.000 đ** (không tính lại), phần còn lại 650.000 đ.

### Tiền mua linh kiện thay thế có tính vào tiền đặt cọc không?
- Không. Tiền đặt cọc chỉ tính một lần bằng 30% giá chốt ban đầu và **không tính lại** khi có phát sinh.
- Khi cần mua thêm linh kiện ngoài thỏa thuận, thợ đưa tiền linh kiện vào **đề xuất chi phí phát sinh** để khách duyệt. Khách đồng ý thì khoản này được cộng vào **đợt thanh toán cuối**; khách không đồng ý thì thợ không được tự mua/thay linh kiện và thu thêm tiền (khách có thể "Từ chối giá & Đổi thợ khác" hoặc hủy đơn).

## 3. Hóa đơn
- Mỗi lần thanh toán được xác nhận, hệ thống tạo **hóa đơn điện tử** (đặt cọc / thanh toán cuối), xem và **in / xuất PDF** trong chi tiết đơn.
- Hóa đơn VAT: `[CẦN XÁC NHẬN]`.

## 4. Bảo chứng tiền cọc (Escrow) bảo vệ hai bên
- Tiền cọc được FixLink **giữ bảo chứng**; thợ yên tâm di chuyển và làm việc vì tiền công đã được bảo đảm.
- Nếu khách **đổi thợ** khi đơn chưa sửa, thợ cũ **không nhận** khoản cọc; cọc được bảo lưu và cấn trừ cho thợ mới.
- **Chỉ khi khách hoàn tất nghiệm thu**, tiền mới được giải ngân cho thợ.
- **Không giao tiền mặt cho thợ ngoài hệ thống.** Nếu thợ yêu cầu trả thêm tiền ngoài app, khách nên từ chối và tạo phiếu hỗ trợ.

## 5. Các nội dung chưa công bố chính thức
- **Tỷ lệ phần trăm hoa hồng/phí nền tảng:** `[CẦN XÁC NHẬN]`.
- **Tỷ lệ phạt hoặc hoàn cọc khi hủy đơn:** `[CẦN XÁC NHẬN]` – mọi trường hợp hoàn cọc (ví dụ thợ không đến, vi phạm thỏa thuận) do Admin thẩm định qua phiếu hỗ trợ.
- **Ví điện tử, thẻ tín dụng, trả góp:** chưa hỗ trợ/chưa công bố; hiện chỉ thanh toán qua mã QR chuyển khoản ngân hàng.
- Khi được hỏi các nội dung trên, Trợ lý trả lời rõ là FixLink chưa công bố thông tin chính thức và đề nghị tạo phiếu hỗ trợ.
