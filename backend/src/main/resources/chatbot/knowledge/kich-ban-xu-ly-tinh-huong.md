---
title: "Kịch bản xử lý tình huống thực tế của khách hàng và thợ"
audience: [guest, customer, technician, admin]
updated: "2026-10-23"
confidence: verified
---

# Kịch bản xử lý tình huống thực tế

Nguyên tắc chung: trả lời đúng quy định đang có, không hứa điều FixLink chưa công bố (bồi thường, hoàn tiền theo tỷ lệ, thời hạn bảo hành, giờ có mặt cam kết). Việc cần nhân viên xử lý được chuyển qua **phiếu hỗ trợ**.

## 1. Thợ trễ hẹn hoặc không đến
1. Xem lịch hẹn và tên thợ trong chi tiết đơn tại **Yêu cầu của tôi** (`/yeu-cau-cua-toi`).
2. Liên hệ thợ để xác nhận lại giờ; cần thì **dời lịch hẹn** (dời được nhiều lần, thời gian mới phải từ hiện tại trở đi).
3. Thợ không đến và đơn **chưa ở Đang sửa chữa (`IN_PROGRESS`)** → khách được **hủy đơn** (nhập lý do) rồi đăng lại.
4. Tạo phiếu hỗ trợ ưu tiên **High** để CSKH xem xét tiền cọc. Mức hoàn cọc cụ thể chưa được công bố.

## 2. Sửa xong nhưng chưa hài lòng / thiết bị vẫn hỏng
- Khi đơn đang **Chờ nghiệm thu (`AWAITING_ACCEPTANCE`)**: **chưa bấm Nghiệm thu**. Tiền vẫn được giữ trung gian; trao đổi để thợ kiểm tra lại.
- Đã nghiệm thu rồi mới hỏng lại: hệ thống kích hoạt bảo hành điện tử khi nghiệm thu, nhưng **thời hạn bảo hành cụ thể chưa công bố** → tạo phiếu hỗ trợ kèm mã đơn, mô tả và ảnh/video.

## 3. Thợ làm hư hỏng thêm tài sản, thái độ không đúng mực, nghi gian lận
- Ghi lại bằng chứng (ảnh, video, thời điểm), không bấm Nghiệm thu nếu đơn chưa hoàn tất.
- Tạo phiếu hỗ trợ: **Critical** nếu gian lận, mất tài sản hoặc tranh chấp tiền; **High** nếu hư hỏng hoặc thái độ.
- Chính sách bồi thường cụ thể chưa được công bố; CSKH sẽ thẩm định từng trường hợp.

## 4. Thợ đòi thêm tiền, đòi trả tiền mặt hoặc chuyển khoản riêng
- Mọi chi phí phát sinh phải được thợ **gửi đề xuất trên hệ thống** → đơn sang **Chờ duyệt chi phí phát sinh**.
- Khách xem giải trình: **Đồng ý** thì giá chốt và tiền cọc (30%) được tính lại theo giá mới; **không đồng ý** thì có thể hủy đơn.
- **Không trả tiền mặt/chuyển khoản riêng ngoài hệ thống.** Bị ép trả ngoài → phiếu hỗ trợ **Critical**.

## 5. Muốn chọn hoặc đổi thợ
- FixLink theo nguyên tắc **ai nhận trước được trước**, **không có tính năng chọn, chỉ định hay đổi thợ**.
- Mọi thợ nhận việc đều đã được duyệt KYC và đúng chuyên môn, khu vực.
- Không muốn tiếp tục với thợ hiện tại: nếu đơn chưa ở Đang sửa chữa thì hủy đơn (nhập lý do) và đăng đơn mới, hoặc tạo phiếu hỗ trợ.

## 6. Cần sửa gấp
- FixLink **không cam kết thời gian thợ có mặt** chung cho mọi đơn. Thời gian cụ thể do khách và thợ thống nhất qua lịch hẹn; đơn mở tối đa 3 ngày.
- Riêng gói **Mở khóa cấp tốc 24/7** có mô tả thợ khóa có mặt sau 15–30 phút.
- Cách tăng khả năng có thợ sớm: ghi rõ "cần gấp" và khung giờ mong muốn, chọn đúng danh mục và khu vực, đính kèm ảnh, đặt ngân sách hợp lý.

## 7. Khách vắng nhà / muốn đổi giờ (góc nhìn khách và thợ)
- Khách hoặc thợ dời lịch hẹn trong chi tiết đơn; hủy lịch hẹn phải nêu lý do và **không làm hủy yêu cầu sửa chữa**.
- Thợ đến nơi mà khách vắng: liên hệ khách, dời lịch hoặc hủy lịch hẹn kèm lý do; cần thì tạo phiếu hỗ trợ.

## 8. Thanh toán, hoàn tiền, hóa đơn
- Được xác nhận: cọc 30% giữ trung gian khi thợ nhận việc, 70% còn lại khi khách nghiệm thu, tiền được giải ngân cho thợ khi nghiệm thu.
- **Chưa công bố**: cổng/phương thức thanh toán cụ thể (ví điện tử, thẻ, trả góp), tỷ lệ hoàn cọc khi hủy, hóa đơn VAT.
- Trợ lý không xem được giao dịch ngân hàng. Nghi bị trừ tiền sai → tạo phiếu hỗ trợ **Critical** kèm mã đơn, thời điểm, số tiền.

## 9. Khu vực ngoài TP.HCM hoặc quận chưa phục vụ
- Chỉ phục vụ các khu vực có trong hệ thống (dữ liệu hiện tại: Q1, Q3, Q7, Bình Thạnh, Tân Bình, Gò Vấp, Phú Nhuận, TP Thủ Đức).
- Khu vực khác: trả lời rõ là chưa phục vụ, không khuyên chọn sai khu vực để lách.

## 10. Thiết bị ngoài phạm vi
- **Không hỗ trợ**: điện thoại, laptop/máy tính, xe máy/ô tô, xây sửa nhà, sơn, chống thấm, máy in, wifi.
- Thiết bị điện gia dụng chưa có gói riêng (tivi, loa, máy hút bụi...): có thể đăng ở **Điện gia dụng**, tự đặt ngân sách, nhưng **không có giá tham khảo riêng**.

## 11. Nhiều thiết bị cùng lúc
- Cùng danh mục (ví dụ 2 máy lạnh): có thể gộp một đơn, ghi rõ số lượng và đặt ngân sách tương ứng.
- Khác danh mục (máy lạnh + bồn cầu): đăng đơn riêng để đúng thợ chuyên môn.

## 12. Tài khoản và dữ liệu cá nhân
- Đổi số điện thoại/email trong hồ sơ cá nhân (không trùng tài khoản khác); thợ cập nhật tại `/tho/ho-so`.
- Tính năng tự xóa tài khoản chưa công bố → phiếu hỗ trợ.
- Nghi bị xâm nhập: đặt lại mật khẩu ngay (`/quen-mat-khau`) và tạo phiếu hỗ trợ **High**. Trợ lý không bao giờ hỏi mật khẩu/OTP.

## 13. Doanh nghiệp, hợp tác, tuyển dụng
- Chưa có chính sách công bố cho khách doanh nghiệp, đơn số lượng lớn, hợp tác hay tuyển nhân viên → tạo phiếu hỗ trợ ghi rõ nhu cầu.
- Muốn làm thợ: đăng ký tại `/dang-ky-tho`.

## 14. Câu hỏi của thợ về thu nhập
- Tiền công giải ngân vào ví khi khách nghiệm thu. **Tỷ lệ hoa hồng và cách rút tiền từ ví chưa công bố.**
- Muốn có nhiều việc hơn: đăng ký đúng chuyên môn, mở rộng khu vực hoạt động, bật Trực tuyến và nhận việc nhanh (ai nhận trước được trước).

## 15. Khách muốn tự sửa
- Trợ lý chỉ hướng dẫn bước kiểm tra an toàn (nguồn điện, phích cắm, chế độ cài đặt, vệ sinh lưới lọc máy lạnh, van nước).
- Không hướng dẫn tháo máy, nạp gas, sửa bo mạch, đụng điện áp cao, leo cao. Những việc này cần thợ chuyên môn.

## 16. Tư vấn đặt thợ từ đầu tới cuối (luồng chuẩn)
1. Hỏi thiết bị và hiện tượng (nếu khách chưa nói).
2. Nêu nguyên nhân thường gặp và bước kiểm tra an toàn.
3. Chốt danh mục và báo giá tham khảo của gói dịch vụ phù hợp.
4. Hỏi thêm quận, khung giờ mong muốn, hãng/model (tối đa 2 câu mỗi lượt).
5. Tạo nút **"Đăng yêu cầu với thông tin này"** để mở form đã điền sẵn; khách kiểm tra, bổ sung địa chỉ, ảnh, ngân sách rồi tự bấm đăng.
