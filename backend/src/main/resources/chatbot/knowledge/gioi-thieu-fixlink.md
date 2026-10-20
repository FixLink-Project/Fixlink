---
title: "Giới thiệu nền tảng FixLink"
audience: [guest, customer, technician, admin]
updated: "2026-10-17"
confidence: verified
---

# Giới thiệu nền tảng FixLink

## 1. FixLink là gì?
FixLink là nền tảng công nghệ kết nối trực tiếp **khách hàng có thiết bị gia dụng bị hư hỏng** với **kỹ thuật viên (thợ) đã được xác minh danh tính (KYC)** tại Việt Nam.

Hệ thống tập trung giải quyết nhu cầu sửa chữa nhanh chóng, minh bạch và an toàn cho các hộ gia đình và cá nhân.

## 2. Các lĩnh vực dịch vụ chính của FixLink
FixLink hiện phục vụ 4 nhóm ngành dịch vụ cốt lõi:
1. **Sửa Chữa Điện Lạnh (Mã: `DIEN_LANH`):**
   - Vệ sinh máy lạnh treo tường (1.0 - 2.5 HP).
   - Nạp ga máy lạnh R32 / R410A trọn gói.
   - Kiểm tra & sửa tủ lạnh không lạnh, đóng tuyết, kêu to, hỏng block.
   - Sửa máy giặt không vắt, không xả nước, lỗi bo mạch.
2. **Sửa Chữa Điện Nước (Mã: `DIEN_NUOC`):**
   - Thông nghẹt & xử lý rò rỉ đường ống nước sinh hoạt, bồn rửa chén.
   - Lắp đặt, thay thế thiết bị vệ sinh: bồn cầu, Lavabo, vòi sen, vòi nước.
   - Dò tìm sự cố chập điện âm tường, nhảy CB / Aptomat, mất điện cục bộ.
3. **Sửa Điện Gia Dụng (Mã: `DO_GIA_DUNG`):**
   - Sửa bếp từ đơn/đôi không nóng, chập nguồn, hỏng IGBT, lỗi cảm biến.
   - Sửa lò vi sóng không nóng, đánh lửa, hỏng đèn phát sóng.
   - Sửa máy hút mùi, nồi chiên không dầu, quạt điện gia đình.
4. **Sửa Khóa & Cửa Cuốn (Mã: `KHOA_CUA`):**
   - Mở khóa cửa nhà, khóa phòng cấp tốc 24/7 khi mất chìa hoặc kẹt khóa.
   - Sửa chữa cửa cuốn kẹt nan, hỏng motor, sao chép remote cửa cuốn.

## 3. Khu vực hoạt động
Hiện tại, FixLink cung cấp dịch vụ trên địa bàn **TP. Hồ Chí Minh**, bao gồm:
- Quận 1 (`Q1`)
- Quận 3 (`Q3`)
- Quận 7 (`Q7`)
- Quận Bình Thạnh (`BINH_THANH`)
- Thành phố Thủ Đức (`THU_DUC`)
- Quận Tân Bình (`TAN_BINH`)
- Quận Gò Vấp (`GO_VAP`)
- Quận Phú Nhuận (`PHU_NHUAN`)

## 4. Mô hình hoạt động: Ai nhận trước được trước (First-Come-First-Served)
- Khách hàng đăng yêu cầu kèm mô tả chi tiết, ảnh hiện trường, địa chỉ và **mức giá/ngân sách cố định**.
- Khi yêu cầu ở trạng thái `OPEN`, các thợ phù hợp về chuyên môn và khu vực sẽ nhìn thấy yêu cầu.
- Thợ nào bấm **"Nhận việc"** trước tiên sẽ là người nhận công việc. Cơ chế sử dụng khóa bi quan (Pessimistic Locking) ở máy chủ để đảm bảo tính công bằng tuyệt đối.
- **Lưu ý quan trọng:** FixLink **không áp dụng mô hình đấu giá ngược** hay báo giá cạnh tranh. Giá dịch vụ được ấn định rõ ràng ngay từ đầu giúp tiết kiệm thời gian cho cả khách hàng và thợ.
