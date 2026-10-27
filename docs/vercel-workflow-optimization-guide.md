# KẾ HOẠCH TỐI ƯU HÓA LUỒNG NGHIỆP VỤ & QUY TRÌNH DEPLOY VERCEL MƯỢT MÀ
**Dự án:** FixLink — Hệ thống kết nối thợ sửa chữa gia dụng  
**Ngày cập nhật:** 27/10/2026  
**Mục tiêu:** Tối ưu hóa hiển thị cho khách hàng & thợ kỹ thuật, loại bỏ triệt để xung đột logic giữa Frontend và Backend, đảm bảo toàn bộ luồng nghiệp vụ chạy liền mạch và deploy mượt mà trên Vercel.

---

## 1. TỔNG HỢP 5 VẤN ĐỀ VÀ GIẢI PHÁP TỐI ƯU HÓA CHI TIẾT

### Vấn đề 1: Thẻ lịch hẹn khảo sát vẫn hiện nút "Đổi lịch" & "Hủy lịch" thay vì "Đã khảo sát"
* **Hiện tượng:** Khách hàng hoặc Thợ đã vào giai đoạn khảo sát (`INSPECTING`, `AWAITING_COST_APPROVAL`, `IN_PROGRESS`), nhưng thẻ lịch hẹn cũ vẫn cho phép bấm [🔄 Đổi lịch hẹn] và [❌ Hủy cuộc hẹn].
* **Nguyên nhân gốc rễ:**
  - File `AppointmentCard.tsx` kiểm tra:
    ```typescript
    appt.appointmentType === 'INSPECTION'
    ```
  - Trong khi đó, Backend (`AppointmentJpaEntity.java`, `DataInitializer.java`) lưu loại lịch hẹn khảo sát thực tế là `'SURVEY'` (hoặc `'REPAIR'`), hoàn toàn không có chuỗi `'INSPECTION'`.
  - Do `'SURVEY' !== 'INSPECTION'`, điều kiện `isInspectionDone` bị tính sai thành `false`.
* **Giải pháp chuẩn hóa:**
  - Sửa `AppointmentCard.tsx` để chấp nhận cả `'SURVEY'`, `'INSPECTION'` hoặc không phân loại.
  - Quy tắc khóa cứng: Khi yêu cầu sửa chữa đã chuyển sang các trạng thái `INSPECTING`, `AWAITING_COST_APPROVAL`, `IN_PROGRESS`, `AWAITING_ACCEPTANCE`, `COMPLETED`:
    - Ẩn hoàn toàn 2 nút [Đổi lịch hẹn] và [Hủy cuộc hẹn].
    - Hiển thị huy hiệu xanh `✅ Đã hoàn thành khảo sát thực tế (Đã khóa cuộc hẹn)`.

---

### Vấn đề 2: Ảnh phụ thu linh kiện bị dính ảnh lỗi thiết bị ban đầu của khách
* **Hiện tượng:** Ở màn hình duyệt phụ thu của khách (`AWAITING_COST_APPROVAL`), khung *"Hình ảnh linh kiện hỏng / hiện trường do thợ chụp gửi"* lại hiển thị lại 2 ảnh chùa/hồ nước mà khách tải lên lúc tạo đơn.
* **Nguyên nhân gốc rễ:**
  - Ở các đơn hàng test có sẵn, bản ghi media trong database có `uploaded_by = NULL`.
  - Bộ lọc trong `RepairRequestDetailPage.tsx` kiểm tra `m.uploadedBy === req.technicianId` trả về rỗng, nhưng trường `req.mediaUrls` cũ hoặc fallback lại lấy nhầm toàn bộ ảnh ban đầu của khách.
* **Giải pháp chuẩn hóa:**
  - **Tách bạch 2 danh sách ảnh độc lập:**
    1. `customerDefectPhotos`: Ảnh ban đầu khách gửi lúc tạo đơn (hoặc có `uploadedBy === req.customerId`). Chỉ hiển thị ở khối *"Thông tin yêu cầu sửa chữa ban đầu"*.
    2. `technicianSurchargePhotos`: Ảnh chụp linh kiện hỏng do thợ đính kèm khi đề xuất phụ thu (có `uploadedBy === req.technicianId` hoặc tải lên lúc đơn đang khảo sát).
  - Nếu thợ chưa đính kèm ảnh linh kiện mới: Hiển thị thông báo rõ ràng *"Kỹ thuật viên chưa đính kèm ảnh linh kiện phát sinh"*, **tuyệt đối không lấy ảnh lỗi ban đầu của khách đắp vào**.

---

### Vấn đề 3: Thêm phụ thu xong muốn đổi/hẹn ngày khác thì không làm được
* **Hiện tượng:** Khi thợ phát hiện linh kiện hỏng cần mua mới và báo phụ thu, thợ muốn hẹn lại ngày thi công khác (ví dụ: ngày mai quay lại thay đồ) nhưng không có chỗ để chọn ngày. Sau khi gửi phụ thu đơn đứng ở `AWAITING_COST_APPROVAL` không thao tác được lịch.
* **Nguyên nhân gốc rễ:**
  - Modal *"Báo giá phát sinh sau khảo sát"* chỉ có ô nhập số tiền và lý do, thiếu tùy chọn ngày thi công mới.
  - Khối chờ duyệt `AWAITING_COST_APPROVAL` chưa có nút hẹn lịch.
* **Giải pháp chuẩn hóa:**
  - **Tích hợp ngay trong Modal Báo giá phát sinh:** Thêm tùy chọn 2 luồng:
    - `🔘 Sửa chữa ngay hôm nay (đúng tiến độ ban đầu)`
    - `🔘 Hẹn lại ngày thi công khác (cần đặt hàng linh kiện / chuẩn bị đồ nghề)` -> Mở ô chọn Ngày & Giờ thi công mới.
  - Khi gửi báo giá, hệ thống đồng thời tạo lịch hẹn thi công mới ở trạng thái chờ khách xác nhận.
  - Khách hàng khi xem báo giá phát sinh sẽ thấy cả: **Chi phí linh kiện mới** + **Thời gian thi công đề xuất mới**, bấm [Đồng ý chi phí mới] là cả giá và lịch hẹn được chốt đồng bộ.
  - Ở cả trạng thái `AWAITING_COST_APPROVAL` và `IN_PROGRESS`, thợ luôn có nút `📅 Hẹn lại ngày thi công` linh hoạt.

---

### Vấn đề 4: Xem đánh giá thợ và bảng đánh giá của khách hàng dành cho thợ
* **Hiện tượng:** Khách hàng khó tìm thấy nút xem các đánh giá cũ của thợ; khi đơn hoàn thành chưa có khối bảng đánh giá tổng kết trực quan.
* **Giải pháp chuẩn hóa:**
  - Bổ sung nút bấm **`⭐ Xem đánh giá khách hàng`** nổi bật ngay cạnh thông tin thợ trên thẻ `QuotationCard` và khối *"Thợ kỹ thuật thực hiện"*. Bấm vào sẽ mở pop-up hiển thị danh sách các đánh giá từ khách trước (số sao, ngày đánh giá, lời nhận xét).
  - Khi đơn hàng ở trạng thái `COMPLETED`: Hiển thị khối **Bảng đánh giá dịch vụ** ở vị trí trang trọng đầu trang, hiển thị số sao vàng và lời nhận xét của khách hàng.

---

### Vấn đề 5: Tối ưu hiển thị ô nhập ngân sách của khách khi đăng yêu cầu (Ảnh mới)
* **Hiện tượng:** 
  - Ô nhập ngân sách `4999995` hiển thị số trần trụi, không có dấu chấm phân cách hàng nghìn, khách rất dễ gõ nhầm thừa/thiếu số 0.
  - Câu giải thích cũ: *"Đây sẽ là giá cuối cùng cho công việc — thợ nhận việc theo đúng mức này, không thương lượng"* -> Mâu thuẫn với mô hình đấu giá thợ (thợ gửi báo giá cạnh tranh).
* **Giải pháp chuẩn hóa:**
  1. **Format tiền tệ trực tiếp khi gõ:** Tự động định dạng có dấu chấm phân cách hàng nghìn (VD: `5.000.000 đ`).
  2. **Đọc số tiền thành chữ tiếng Việt:** Hiển thị bên dưới (VD: `5.000.000 đ` -> *"Bằng chữ: Năm triệu đồng chẵn"*).
  3. **Thanh chọn nhanh ngân sách (Quick Chips):** `[100.000 đ]`, `[200.000 đ]`, `[500.000 đ]`, `[1.000.000 đ]`, `[2.000.000 đ]`, `[5.000.000 đ]` giúp khách bấm 1 chạm điền ngay.
  4. **Cập nhật câu gợi ý (Hint) chuẩn xác:**
     *"Mức ngân sách tối đa bạn dự kiến chi trả. Các thợ kỹ thuật uy tín sẽ căn cứ mức này để gửi báo giá cạnh tranh nhất cho bạn so sánh và lựa chọn."*
  5. **Tính toán minh bạch tiền cọc bảo đảm (30%):**
     *"🔒 Tiền đặt cọc bảo đảm ước tính (30%): 1.500.000 đ (Chỉ thanh toán sau khi bạn chọn được thợ ưng ý)"*.

---

## 2. MA TRẬN CHUYỂN TRẠNG THÁI VÀ BẢO ĐẢM KHÔNG ĐỨT LUỒNG

| Trạng thái đơn | Quyền hạn Khách hàng | Quyền hạn Thợ kỹ thuật | Trạng thái Thẻ Lịch hẹn |
| :--- | :--- | :--- | :--- |
| **OPEN** (Đang nhận báo giá) | Xem danh sách báo giá của các thợ, xem đánh giá từng thợ, chấp nhận thợ (chốt deal). | Gửi báo giá cạnh tranh (bằng hoặc thấp hơn ngân sách khách đưa). | Chưa có lịch hẹn. |
| **ASSIGNED** (Đã chọn thợ) | Xem thông tin thợ đã chọn, thanh toán cọc 30% bảo đảm qua QR VietQR. | Xem thông tin đơn, liên hệ khách hàng, chờ khách cọc. | Có thể đặt lịch hẹn khảo sát. |
| **INSPECTING** (Đang khảo sát) | Theo dõi khảo sát, có quyền đổi thợ nếu thợ không tới. | Đến khảo sát thực tế, chọn: Sửa ngay / Báo giá phát sinh linh kiện / Hẹn lại ngày thi công. | **ĐÃ KHÓA (Đã khảo sát)**, ẩn nút đổi/hủy. |
| **AWAITING_COST_APPROVAL** (Chờ duyệt phát sinh) | Xem chi tiết linh kiện phát sinh, ảnh linh kiện của thợ, ngày hẹn thi công mới đề xuất. Bấm: [Đồng ý] / [Từ chối & Đổi thợ khác] / [Hủy đơn]. | Tạm dừng thi công, có thể trao đổi với khách hoặc cập nhật lại ngày thi công nếu cần. | **ĐÃ KHÓA (Đã khảo sát)**. Có thể có lịch thi công mới chờ duyệt. |
| **IN_PROGRESS** (Đang sửa chữa) | Theo dõi quá trình sửa chữa thực tế. Không được tự ý hủy đơn (BR07). | Tiến hành sửa chữa, kiểm tra chạy thử (test run), có nút `📅 Hẹn lại ngày thi công` và `📋 Báo cáo hoàn thành & Mời nghiệm thu`. | Lịch khảo sát đã khóa. Lịch thi công hiển thị thời gian làm việc. |
| **AWAITING_ACCEPTANCE** (Chờ nghiệm thu) | Kiểm tra thiết bị thực tế, thanh toán 70% còn lại qua QR Pay, chấm sao (1-5 sao) và viết nhận xét bắt buộc. | Chờ khách nghiệm thu và nhận tiền quyết toán về ví. | Toàn bộ lịch đã hoàn tất. |
| **COMPLETED** (Hoàn thành) | Xem hóa đơn điện tử VAT/Biên lai, xem lại đánh giá đã gửi cho thợ. | Xem doanh thu ghi nhận trong ví, xem đánh giá khách chấm trong hồ sơ cá nhân. | Đã hoàn tất và lưu trữ. |

---

## 3. CHECKLIST KIỂM TRA & QUY TRÌNH DEPLOY VERCEL MƯỢT MÀ

### Bước 1: Kiểm thử cục bộ (Pre-flight Checks)
- [ ] Chạy `npm run lint` / `npx tsc --noEmit` không có lỗi Type.
- [ ] Chạy `npm test` bảo đảm 100% test Vitest PASS.
- [ ] Chạy `npm run build` kiểm tra Vite bundle thành công dưới 5s, không có import mồ côi.

### Bước 2: Commit và Tạo Pull Request Chuẩn
- [ ] Commit với thông điệp rõ ràng theo chuẩn Conventional Commits.
- [ ] Push lên nhánh tính năng và merge vào nhánh `main` thông qua PR.
- [ ] Kiểm tra GitHub Actions CI xanh 100%.

### Bước 3: Đảm bảo Vercel Build & Cache-Busting
- [ ] Vercel Production (`fixlink.vercel.app`) tự động kích hoạt deployment khi commit mới được merge vào `main`.
- [ ] Kiểm tra Vercel Deployment Dashboard để xác nhận build thành công (Status: Ready).
- [ ] **Khắc phục Cache Trình duyệt:** Hướng dẫn người dùng bấm tổ hợp phím **`Ctrl + Shift + R`** (Windows) hoặc **`Cmd + Shift + R`** (Mac), hoặc mở trong tab Ẩn danh (Incognito) để trình duyệt tải ngay các file chunk JS/CSS mới nhất mà không bị giữ lại bản cache cũ.
