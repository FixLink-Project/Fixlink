---
title: "Chính sách đặt cọc và thanh toán qua mã QR"
audience: [guest, customer, technician, admin]
updated: "2026-10-28"
confidence: verified
---

# Chính sách đặt cọc và thanh toán qua mã QR

## 1. Giá dịch vụ được chốt như thế nào?
- Khách hàng đặt **ngân sách dự kiến** (bắt buộc, lớn hơn 0 đồng) khi đăng yêu cầu. Đây là **trần giá**: báo giá của thợ (tiền công + vật tư) **không được vượt quá ngân sách** này.
- Khi khách bấm **"Chọn thợ này"**, giá chốt (agreed price) của đơn **bằng tổng báo giá được chọn**.
- Nếu khi khảo sát thợ phát hiện hư hỏng ngoài mô tả ban đầu, thợ gửi **đề xuất chi phí phát sinh** (trạng thái `AWAITING_COST_APPROVAL`, hoặc từng khoản trong mục "Chi phí phát sinh"). Chỉ khoản nào khách bấm **Đồng ý** mới được tính.

## 2. Hai đợt thanh toán: đặt cọc 30% và thanh toán phần còn lại
Tiền cọc thanh toán bằng **mã QR chuyển khoản ngân hàng (VietQR)** trong trang chi tiết đơn. Phần còn lại trả bằng **mã QR** hoặc **tiền mặt trực tiếp cho thợ** (xác nhận trên hệ thống).

**Đợt 1 – Đặt cọc 30%:**
1. Khách chọn thợ → đơn `ASSIGNED`.
2. Thợ đặt lịch hẹn khảo sát → lịch ở trạng thái **Chờ xác nhận**.
3. Khách bấm **"✓ Xác nhận lịch hẹn"** → mã QR đặt cọc **30% giá chốt** tự động hiện ra.
4. Khách quét QR, chuyển khoản đúng số tiền và nội dung, rồi bấm xác nhận đã chuyển khoản → đơn chuyển sang **Đã đặt cọc (`DEPOSIT_PAID`)**.
5. Thợ chỉ được bắt đầu khảo sát khi đơn đã đặt cọc.

**Đợt 2 – Thanh toán phần còn lại:**
- Khi thợ báo hoàn thành (`AWAITING_ACCEPTANCE`), hệ thống tự tạo mã QR thanh toán:
  **Phần còn lại = giá chốt + chi phí phát sinh đã duyệt − tiền cọc đã đóng.**
- Khách thanh toán khoản này theo một trong hai cách:
  1. **Quét mã QR** chuyển khoản rồi bấm xác nhận đã chuyển khoản.
  2. **Tiền mặt**: bấm **"💵 Tôi thanh toán tiền mặt trực tiếp cho thợ"**, đưa đúng số tiền hiển thị cho thợ rồi bấm **"Xác nhận đã gửi tiền mặt"**.
- Thanh toán xong, khách **chấm 1–5 sao + viết nhận xét** (bắt buộc) thì mới hoàn tất nghiệm thu (`COMPLETED`); hệ thống kích hoạt bảo hành điện tử.
- Tiền cọc **không** trả bằng tiền mặt: luôn qua mã QR.

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
- Chỉ trả **đúng số tiền hệ thống hiển thị** (kể cả khi trả tiền mặt phần còn lại). Thợ đòi thêm bất kỳ khoản nào ngoài hệ thống (phí đi lại, tiền "bồi dưỡng", phát sinh chưa duyệt) thì khách từ chối và tạo phiếu hỗ trợ.

## 5. Phí nền tảng và Ví FixLink của thợ
- Khi đơn **hoàn thành** (`COMPLETED`), hệ thống tự trích **10% phí nền tảng** trên giá chốt và cộng **90% còn lại** vào **Ví FixLink** của thợ.
- Thợ xem số dư ví trên bảng điều khiển thợ (/tho). Khi thợ nhận tiền mặt trực tiếp từ khách, số dư ví có thể **tạm âm** (công nợ phí nền tảng) và được tự động cấn trừ vào doanh thu các đơn hoàn thành tiếp theo; thợ vẫn nhận việc bình thường.
- Khách hàng không trả thêm phí nền tảng: khách chỉ trả giá chốt + phát sinh đã duyệt.

## 6. Các nội dung chưa công bố chính thức
- **Cách rút tiền từ Ví FixLink:** `[CẦN XÁC NHẬN]`.
- **Tỷ lệ phạt hoặc hoàn cọc khi hủy đơn:** `[CẦN XÁC NHẬN]` – mọi trường hợp hoàn cọc (ví dụ thợ không đến, vi phạm thỏa thuận) do Admin thẩm định qua phiếu hỗ trợ.
- **Ví điện tử, thẻ tín dụng, trả góp:** chưa hỗ trợ/chưa công bố; hiện thanh toán qua mã QR chuyển khoản ngân hàng (phần còn lại có thể trả tiền mặt cho thợ).
- Khi được hỏi các nội dung trên, Trợ lý trả lời rõ là FixLink chưa công bố thông tin chính thức và đề nghị tạo phiếu hỗ trợ.
