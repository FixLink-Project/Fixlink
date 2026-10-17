---
title: "Xử lý sự cố và thắc mắc thường gặp trên FixLink"
audience: [guest, customer, technician]
updated: "2026-10-17"
confidence: verified
---

# Xử lý sự cố và thắc mắc thường gặp (FAQ)

## 1. Sự cố đăng nhập và tài khoản
- **Tôi đăng nhập báo "Tên đăng nhập hoặc mật khẩu không chính xác" (401)?**
  - Hãy kiểm tra lại phím CapsLock và kiểu gõ tiếng Việt trên bàn phím.
  - Nếu không nhớ mật khẩu, bấm vào liên kết **"Quên mật khẩu?"** tại trang đăng nhập hoặc vào `/quen-mat-khau` để lấy lại mật khẩu qua email.
- **Tài khoản bị báo "Tài khoản bị tạm khóa" (429)?**
  - Đây là cơ chế chống dò mật khẩu. Tài khoản của bạn đã bị khóa tạm thời 15 phút do nhập sai quá 5 lần. Vui lòng chờ hết thời gian đếm ngược rồi đăng nhập lại.
- **Tài khoản bị báo "Tài khoản bị khóa" (403)?**
  - Tài khoản bị quản trị viên khóa do vi phạm quy chế hoặc có nghi vấn bảo mật. Vui lòng tạo phiếu hỗ trợ (Ticket) để nhân viên CSKH kiểm tra và hỗ trợ mở lại.

## 2. Sự cố của kỹ thuật viên (Thợ)
- **Tại sao tôi bấm "Nhận việc" lại báo lỗi "Thợ chưa được xác minh danh tính" (403)?**
  - Bạn chỉ có thể nhận việc khi hồ sơ KYC đã được Admin phê duyệt (`APPROVED`).
  - Vui lòng vào mục **Hồ sơ thợ** (`/tho/ho-so`), kiểm tra xem đã tải đủ 2 mặt CCCD và chọn đủ chuyên môn/khu vực chưa. Nếu trạng thái là `PENDING`, vui lòng đợi quản trị viên duyệt; nếu là `REJECTED`, hãy đọc kỹ lý do từ chối để bổ sung giấy tờ.
- **Tại sao tôi bấm nhận việc bị báo lỗi 409 "JOB_ALREADY_TAKEN"?**
  - Hệ thống FixLink vận hành theo cơ chế **ai nhận trước được việc trước**. Có một kỹ thuật viên khác đã bấm nhận việc trước bạn. Vui lòng quay lại danh sách việc và chọn yêu cầu khác.
- **Tại sao tôi không thấy công việc nào mới trong bảng việc?**
  - Kiểm tra xem bạn đã bật trạng thái **Trực tuyến (is_online)** chưa.
  - Đảm bảo danh mục chuyên môn và các khu vực bạn đăng ký có khách hàng đang đăng yêu cầu.

## 3. Sự cố của khách hàng
- **Tôi đã đăng yêu cầu nhưng chưa thấy thợ nào nhận?**
  - Yêu cầu sẽ mở trên hệ thống tối đa trong 3 ngày.
  - Hãy kiểm tra xem mức giá cố định bạn đưa ra có quá thấp so với tính chất công việc không. Bạn có thể hủy đơn cũ và tạo đơn mới với mức giá phù hợp hơn kèm mô tả rõ ràng, đầy đủ hình ảnh.
- **Thợ đến kiểm tra và yêu cầu thêm tiền phát sinh thì xử lý thế nào?**
  - Thợ phải cập nhật yêu cầu chi phí phát sinh trên hệ thống (`AWAITING_COST_APPROVAL`).
  - Bạn vào chi tiết đơn hàng trên ứng dụng để kiểm tra nội dung giải trình của thợ. Nếu thấy hợp lý, bạn bấm "Đồng ý"; nếu không hợp lý, bạn có quyền từ chối. Tuyệt đối không giao tiền mặt ngoài hệ thống.

## 4. Cảnh báo an toàn khẩn cấp (Nguy hiểm cháy nổ, điện giật)
- Nếu thiết bị có dấu hiệu **cháy khét, bốc khói, xẹt lửa, rò rỉ gas nồng nặc, hoặc nước ngập gần nguồn điện**:
  - **Hành động ngay lập tức:**
    1. Cúp ngay cầu dao tổng (Aptomat) của ngôi nhà.
    2. Khóa van bình gas an toàn.
    3. Sơ tán người già, trẻ nhỏ ra khỏi khu vực nguy hiểm.
    4. Trong tình huống khẩn cấp, gọi ngay **114 (Cứu hỏa & Cứu nạn)** hoặc **115 (Cấp cứu y tế)**.
  - Tuyệt đối không tự ý dùng tay không kiểm tra rò điện hoặc châm lửa thử gas. Hãy đặt thợ chuyên nghiệp của FixLink đến xử lý khi đã đảm bảo môi trường an toàn.
