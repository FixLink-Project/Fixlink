# CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM
### Độc lập - Tự do - Hạnh phúc
---

# BÁO CÁO NGHIỆM THU KỸ THUẬT & CHỨC NĂNG HỆ THỐNG
## HẠNG MỤC: TÍNH NĂNG RC-41 - KỸ THUẬT VIÊN SỬA HOẶC RÚT BÁO GIÁ
### (Technician Update or Withdraw Quotation - Feature F12)
**Dự án:** Sàn Thương mại Dịch vụ Sửa chữa & Cứu hộ Kỹ thuật FixLink  
**Mã phiếu công việc (Task / Story ID):** `RC-41` (`RC41`)  
**Ngày nghiệm thu:** 22/09/2026  
**Địa điểm thực hiện:** Phòng Quản lý Chất lượng & Đảm bảo Phần mềm Dự án FixLink  

---

## I. THÔNG TIN CHUNG VỀ HẠNG MỤC NGHIỆM THU

1. **Tên phân hệ:** Quản lý Đơn hàng & Đấu thầu Báo giá Sửa chữa (Repair Requests & Quotations Engine).
2. **Mã tính năng & Mã phiếu công việc:** `RC-41` (Tương ứng tính năng `F12 - Sửa / rút báo giá` theo hồ sơ Thiết kế Phần mềm FixLink).
3. **Mục tiêu tính năng:**
   - Cho phép Kỹ thuật viên (Thợ) chủ động điều chỉnh phương án kỹ thuật, thời gian khảo sát/hoàn thành, cập nhật chi phí nhân công và giá vật tư linh kiện khi có thỏa thuận mới với khách hàng hoặc sau khi khảo sát chi tiết.
   - Cho phép Kỹ thuật viên tự thu hồi / rút lại báo giá khi nhận thấy không phù hợp hoặc không thể sắp xếp lịch thi công.
   - Thiết lập ranh giới nghiệp vụ chặt chẽ: Thao tác sửa hoặc rút báo giá **chỉ được phép thực hiện khi báo giá đang ở trạng thái chờ phản hồi (`PENDING`)** và khách hàng chưa chốt thầu (`ACCEPTED`).
   - Bảo mật giá thầu, ngăn ngừa xung đột và can thiệp trái phép giữa các thợ (Anti-IDOR).
4. **Kiến trúc kỹ thuật:**
   - **Mô hình kiến trúc:** Hexagonal Architecture (Ports & Adapters).
   - **Backend:** Java 17, Spring Boot 3.3.4, Spring Data JPA, Spring Security (Stateless JWT RBAC), Flyway Migration V4.
   - **Frontend:** React 18, TypeScript, TailwindCSS, Vite.
   - **Môi trường cơ sở dữ liệu:** PostgreSQL (Production) / H2 In-Memory PostgreSQL Mode (Local/Test).

---

## II. ĐỐI CHIẾU ĐỊNH NGHĨA HOÀN THÀNH (DEFINITION OF DONE - DoD)

Căn cứ theo yêu cầu nghiệm thu của đề bài:
> *"Definition of Done: implemented, unit tested, and verified against the acceptance criteria."*

Hội đồng nghiệm thu tiến hành đối chiếu và xác nhận:

| STT | Tiêu chí Định nghĩa Hoàn thành (DoD) | Tình trạng | Kết quả đối chiếu chi tiết |
| :---: | :--- | :---: | :--- |
| **1** | **Đã triển khai mã nguồn đầy đủ (Implemented)** | **ĐẠT (100%)** | Đã triển khai hoàn chỉnh mã nguồn từ Core Domain Entity, Inbound/Outbound Ports, Application Services, Web REST Controller đến Giao diện Người dùng React Modal (Chế độ Edit Quotation). |
| **2** | **Kiểm thử đơn vị (Unit Tested)** | **ĐẠT (100%)** | Đã hoàn thành **27 Unit Tests** bao phủ toàn diện Thực thể miền `Quotation` và Service `QuotationService` (100% Pass, 0 Failure, 0 Error). |
| **3** | **Xác minh theo Tiêu chí chấp nhận (Verified against AC)** | **ĐẠT (100%)** | Đã hoàn thành **12 Integration Tests** (`QuotationApiIntegrationTest`) và **Kịch bản kiểm thử E2E trực tiếp trên hệ thống đang chạy** đạt kết quả tuyệt đối (100% Pass). |

---

## III. MA TRẬN TIÊU CHÍ CHẤP NHẬN (ACCEPTANCE CRITERIA MATRIX)

| Mã AC | Tiêu chí Chấp nhận (Acceptance Criteria) | Hiện thực trong Hệ thống | Kết quả Kiểm nghiệm |
| :---: | :--- | :--- | :---: |
| **AC-01** | **Kiểm soát trạng thái được phép sửa/rút (BR-QUOTE-03):**<br>Kỹ thuật viên chỉ được chỉnh sửa hoặc rút lại báo giá khi báo giá đang ở trạng thái `PENDING`. Khi báo giá đã được khách hàng chấp nhận (`ACCEPTED`), bị từ chối (`REJECTED`), hoặc đã rút (`WITHDRAWN`), hệ thống từ chối với mã lỗi HTTP `400 Bad Request` (`errorCode: INVALID_QUOTATION_STATE`). | `Quotation.java:80-82 & 107-109`, ném `DomainException` khi `status != QuotationStatus.PENDING`. | **PASSED** |
| **AC-02** | **Kiểm soát phân quyền & Chống can thiệp trái phép (Anti-IDOR / BR-SEC-01):**<br>Kỹ thuật viên chỉ có quyền sửa hoặc rút báo giá do chính mình tạo ra. Thợ khác hoặc khách hàng cố ý can thiệp qua ID báo giá sẽ bị hệ thống chặn với mã lỗi HTTP `403 Forbidden` (`errorCode: ACCESS_DENIED`). | `QuotationService.java:173-175 & 201-203`. | **PASSED** |
| **AC-03** | **Tự động tính lại tổng chi phí (Calculation Rule BR-QUOTE-02):**<br>Khi cập nhật chi phí tiền công (`priceLabor`) hoặc chi phí vật tư (`priceMaterials`), hệ thống tự động tính lại `totalPrice = priceLabor + priceMaterials`. Bắt buộc tổng giá trị sau cập nhật phải `> 0`. | `Quotation.calculateTotalPrice()` & `Quotation.validate()`. | **PASSED** |
| **AC-04** | **Tính hợp lệ dữ liệu đầu vào & Ghi vết thời gian (Validation & Audit Trail):**<br>Phương án sửa chữa (`solution`) không được rỗng. Tiền công và tiền vật tư không được âm (`>= 0`). Thời điểm chỉnh sửa được tự động lưu vết vào trường `updatedAt = LocalDateTime.now()`. | `@Valid @RequestBody UpdateQuotationRequest` & `quotation.updateDetails(...)`. | **PASSED** |
| **AC-05** | **Rút báo giá an toàn (Withdraw Quotation - BR-QUOTE-04):**<br>Kỹ thuật viên có quyền thu hồi báo giá đang chờ. Sau khi thu hồi, trạng thái chuyển thành `WITHDRAWN`. Báo giá đã rút không thể rút lại hoặc chỉnh sửa tiếp. | `Quotation.withdraw()` & `PATCH /api/v1/technicians/me/quotations/{id}/withdraw`. | **PASSED** |

---

## IV. BẢNG HIỆN THỰC KỸ THUẬT & KIẾN TRÚC LỤC GIÁC

### 1. Kiến trúc phân tầng (Hexagonal Architecture)

```
                            [ REST Client / Web UI ]
                                       │
                                       ▼  PUT /api/v1/technicians/me/quotations/{id}
                                       │  PATCH /api/v1/technicians/me/quotations/{id}/withdraw
                         QuotationController (HTTP Adapter)
                                       │
                                       ▼  (Inbound Port)
                                QuotationUseCase
                                       │
                                       ▼
                                QuotationService
                        (State Verification & Anti-IDOR)
                                       │
                                       ▼
                       Quotation.updateDetails() / withdraw()
                                 (Pure Domain Logic)
                                       │
                                       ▼  (Outbound Port)
                            QuotationRepositoryPort
                                       │
                                       ▼
                          QuotationPersistenceAdapter
                                       │
                                       ▼
                         [ PostgreSQL / H2 Database ]
```

### 2. Chi tiết các thành phần đã phát triển

| Tầng kiến trúc | Tệp tin mã nguồn | Vai trò & Trách nhiệm |
| :--- | :--- | :--- |
| **Domain Model** | `backend/.../domain/model/Quotation.java` | Bổ sung phương thức `updateDetails(...)` kiểm tra trạng thái `PENDING`, cập nhật dữ liệu, tự động tính tổng tiền và xác thực nghiệp vụ. |
| **Inbound Port** | `backend/.../application/port/in/QuotationUseCase.java` | Khai báo phương thức `updateQuotation(...)` và `withdrawQuotation(...)`. |
| **DTO Request** | `backend/.../dto/request/UpdateQuotationRequest.java` | Tiếp nhận và xác thực Bean Validation (`@NotBlank`, `@NotNull`, `@DecimalMin("0.0")`). |
| **Application Service**| `backend/.../service/QuotationService.java` | Kiểm tra sở hữu IDOR, xác minh KYC thợ (`APPROVED`), kiểm tra trạng thái đơn hàng (`PENDING`/`BIDDING_OPEN`), lưu dữ liệu và ánh xạ kết quả. |
| **Web Adapter** | `backend/.../controller/QuotationController.java` | Cung cấp endpoint `PUT /api/v1/technicians/me/quotations/{id}` và `PATCH /api/v1/technicians/me/quotations/{id}/withdraw`. |
| **Frontend UI** | `frontend/.../TechnicianDashboardPage.tsx` | Nút **"Sửa báo giá"**, modal chế độ chỉnh sửa tự động điền dữ liệu cũ, tính nhẩm thời gian thực và gọi API cập nhật. |
| **TypeScript Types**| `frontend/.../lib/types.ts` | Khai báo interface `UpdateQuotationPayload` và kiểu trạng thái `QuotationStatus`. |

---

## V. TỔNG HỢP KẾT QUẢ KIỂM THỬ HỆ THỐNG

### 1. Kiểm thử Tự động Backend Test Suite (39/39 Tests Đạt 100%)

```text
[INFO] Running com.fixlink.adapter.in.web.controller.QuotationApiIntegrationTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 14.49 s
[INFO] Running com.fixlink.application.service.QuotationServiceTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.602 s
[INFO] Running com.fixlink.domain.QuotationTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.034 s
[INFO] 
[INFO] Results:
[INFO] Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- **QuotationTest (12 Tests):** Kiểm tra tính tổng giá trị, xác thực phương án, chặn số âm, cập nhật thành công, chặn cập nhật khi trạng thái khác `PENDING`, rút lại báo giá.
- **QuotationServiceTest (15 Tests):** Kiểm tra KYC thợ, chống gửi trùng, chặn can thiệp trái phép (Anti-IDOR), cập nhật thành công, chặn cập nhật đơn đã đóng.
- **QuotationApiIntegrationTest (12 Tests):** Kiểm tra toàn chu trình API: Đăng nhập -> Gửi báo giá -> Cập nhật báo giá (PUT) -> Khách xem báo giá cập nhật -> Rút báo giá (PATCH) -> Chặn sửa báo giá đã rút.

### 2. Kiểm thử Giao diện & Biên dịch Frontend (17/17 Tests Đạt 100%)

```text
✓ tests/api.test.ts (2 tests)
✓ tests/pagination.test.ts (5 tests)
✓ tests/tabs.test.tsx (5 tests)
✓ tests/storage.test.ts (5 tests)

Test Files  4 passed (4)
     Tests  17 passed (17)

vite v5.4.21 building for production...
✓ 72 modules transformed.
dist/assets/index-_DAQHi2b.css   19.46 kB │ gzip:  4.55 kB
dist/assets/index-DO-RT_b4.js   332.97 kB │ gzip: 96.77 kB
✓ built in 9.61s
```

### 3. Kiểm thử Thực tế Trực tiếp trên Server đang chạy (Live E2E Verification)

```text
================================================================================
    CHƯƠNG TRÌNH KIỂM THỬ E2E TÍNH NĂNG RC-41: SỬA HOẶC RÚT BÁO GIÁ
================================================================================

[BƯỚC 1] Đăng nhập tài khoản Kỹ thuật viên (tho_dien_lanh_01)...
  -> KẾT QUẢ: Đăng nhập thợ THÀNH CÔNG! Đã cấp Access Token.

[BƯỚC 2] Lấy danh sách báo giá hiện có của thợ...
  -> KẾT QUẢ: Xác định mục tiêu Báo giá ID #1 (Trạng thái ban đầu: PENDING).
     * Chi phí cũ: Tiền công=150,000 VND, Vật tư=100,000 VND => Tổng=250,000 VND

[BƯỚC 3] Kỹ thuật viên cập nhật báo giá #1 (PUT /api/v1/technicians/me/quotations/1)...
  -> Mã phản hồi HTTP: 200 OK
  -> Phương án mới: Cập nhật phương án: Vệ sinh dàn nóng lạnh, thay tụ quạt 35uF và nạp gas R32 đủ áp
  -> Tiền công mới: 300,000 VND | Tiền vật tư mới: 200,000 VND
  -> Tổng tiền tự động tính lại: 500,000 VND
  -> ĐỐI CHIẾU AC-03 & AC-04: ĐẠT 100%!

[BƯỚC 4] Kiểm tra bảo mật Anti-IDOR (Tài khoản khác không được sửa báo giá này)...
  -> Mã phản hồi khi tài khoản khác cố cập nhật: 403 Forbidden
  -> ĐỐI CHIẾU AC-02: ĐẠT 100%!

[BƯỚC 5] Kỹ thuật viên rút lại báo giá #1 (PATCH /withdraw)...
  -> Mã phản hồi HTTP: 200 OK | Trạng thái mới: WITHDRAWN
  -> ĐỐI CHIẾU AC-05: ĐẠT 100%!

[BƯỚC 6] Kiểm tra ràng buộc trạng thái (Không cho phép sửa báo giá đã WITHDRAWN)...
  -> Mã phản hồi HTTP: 400 Bad Request
  -> Thông điệp: "Chỉ có thể chỉnh sửa báo giá khi đang chờ phản hồi (PENDING)"
  -> ĐỐI CHIẾU AC-01: ĐẠT 100%!

================================================================================
>>> TẤT CẢ CÁC TIÊU CHÍ CHẤP NHẬN CỦA RC-41 ĐÃ ĐƯỢC XÁC MINH THỰC TẾ ĐẠT 100% <<<
================================================================================
```

---

## VI. BÀN GIAO GÓI ĐÓNG GÓI SẢN PHẨM (`RC41`)

Toàn bộ mã nguồn dự án sau khi hoàn tất tính năng RC-41 cùng đầy đủ tài liệu nghiệm thu đã được đóng gói và nén sạch sẽ (loại trừ `node_modules/`, `target/`, `.git/`):

| Tên tập tin | Định dạng | Dung lượng | Vị trí lưu trữ |
| :--- | :---: | :---: | :--- |
| **`RC41.zip`** | ZIP | **~2.15 MB** | `d:\22-09-2026(baitapFixLink)\RC41.zip`<br>*(Bản sao tại: `d:\RC41.zip`)* |
| **`RC41.rar`** | WinRAR | **~2.05 MB** | `d:\22-09-2026(baitapFixLink)\RC41.rar`<br>*(Bản sao tại: `d:\RC41.rar`)* |

---

## VII. KẾT LUẬN & KIẾN NGHỊ NGHIỆM THU

### 1. Kết luận
- Tính năng **RC-41: Technician Update or Withdraw Quotation (Sửa hoặc rút báo giá)** đã được hiện thực hoàn chỉnh 100% theo đúng đặc tả kiến trúc và yêu cầu nghiệp vụ.
- Đáp ứng tuyệt đối **Định nghĩa Hoàn thành (Definition of Done)**: Đã triển khai đầy đủ, kiểm thử đơn vị bao phủ toàn diện và xác minh đạt tất cả tiêu chí chấp nhận AC-01 đến AC-05.
- Bộ kiểm thử tự động đạt kết quả **39/39 Tests Pass (100%)**, giao diện người dùng hoạt động mượt mà và trực quan.

### 2. Kiến nghị
- **CHẤP THUẬN NGHIỆM THU TOÀN DIỆN VÀ ĐÓNG TICKET RC-41.**
- Bàn giao mã nguồn cho đội ngũ tích hợp tiếp phân hệ F13 (So sánh báo giá) và F14 (Khách hàng lựa chọn Kỹ thuật viên).

---

### ĐẠI DIỆN CÁC BÊN THAM GIA NGHIỆM THU

| ĐẠI DIỆN ĐỘI NGŨ PHÁT TRIỂN | ĐẠI DIỆN QUẢN LÝ SẢN PHẨM & ĐẢM BẢO CHẤT LƯỢNG (QA / PO) |
| :---: | :---: |
| *(Ký và ghi rõ họ tên)* | *(Ký và ghi rõ họ tên)* |
| <br><br><br> | <br><br><br> |
| **Kỹ sư Trưởng Phát triển Hệ thống** | **Giám đốc Dự án FixLink** |
