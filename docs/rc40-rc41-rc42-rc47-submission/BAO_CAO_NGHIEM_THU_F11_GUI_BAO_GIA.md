# CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM
### Độc lập - Tự do - Hạnh phúc
---

# BÁO CÁO NGHIỆM THU KỸ THUẬT & CHỨC NĂNG HỆ THỐNG
## HẠNG MỤC: TÍNH NĂNG F11 - KỸ THUẬT VIÊN GỬI BÁO GIÁ (QUOTATION SUBMISSION)
**Dự án:** Sàn Thương mại Dịch vụ Sửa chữa & Cứu hộ Kỹ thuật FixLink  
**Ngày nghiệm thu:** 22/09/2026  
**Địa điểm thực hiện:** Phòng Quản lý Chất lượng Dự án FixLink  

---

## I. THÔNG TIN CHUNG VỀ HẠNG MỤC NGHIỆM THU

1. **Tên phân hệ:** Quản lý Đơn hàng & Báo giá Sửa chữa (Repair Requests & Quotations Engine).
2. **Mã tính năng & Mã phiếu công việc (Ticket/Story ID):** `F11 - Kỹ thuật viên gửi báo giá` / Mã quản lý nhiệm vụ: **`RC-40` (`Rc40`)**.
3. **Gói đóng gói bàn giao (Archive Package):** `Rc40.zip` (2.14 MB) & `Rc40.rar` (2.05 MB) (Đã làm sạch, loại trừ `node_modules`, `target`, `.git`).
4. **Mục tiêu tính năng:**
   - Cho phép Kỹ thuật viên (Thợ) đã hoàn tất định danh và được phê duyệt xác thực KYC (`APPROVED`) khảo sát hiện trường hoặc nhận diện sự cố từ xa, gửi phương án sửa chữa kèm bảng báo giá chi tiết (tiền công và tiền linh kiện/vật tư) đến Khách hàng.
   - Thiết lập cơ chế kiểm soát giá minh bạch, chống gửi trùng lặp báo giá (BR02), ngăn ngừa xung đột lợi ích giữa các thợ và bảo mật giá thầu cạnh tranh.
   - Hỗ trợ quyền thu hồi (rút lại) báo giá khi đơn vẫn đang ở trạng thái chờ phản hồi (`PENDING`).
4. **Kiến trúc kỹ thuật:** 
   - **Mô hình kiến trúc:** Hexagonal Architecture (Kiến trúc Lục giác - Ports & Adapters).
   - **Backend:** Java 17, Spring Boot 3.3.4, Spring Data JPA, Spring Security (JWT Stateless RBAC), Flyway Migration.
   - **Frontend:** React 18, TypeScript, TailwindCSS, Vite.
   - **Cơ sở dữ liệu:** PostgreSQL / H2 Database Engine (Test Profile).

---

## II. ĐỐI CHIẾU ĐỊNH NGHĨA HOÀN THÀNH (DEFINITION OF DONE - DoD)

Căn cứ theo yêu cầu nghiệm thu của đề bài:
> *"Định nghĩa hoàn thành: đã triển khai, kiểm thử đơn vị và xác minh theo các tiêu chí chấp nhận."*

Hội đồng nghiệm thu xác nhận tình trạng đáp ứng của tính năng F11 như sau:

| STT | Tiêu chí Định nghĩa Hoàn thành (DoD) | Tình trạng | Kết quả đối chiếu chi tiết |
| :---: | :--- | :---: | :--- |
| **1** | **Đã triển khai mã nguồn đầy đủ (Implemented)** | **ĐẠT (100%)** | Đã triển khai toàn bộ các tầng: Database Migration (Flyway V4), Domain Model, Inbound/Outbound Ports, Application Services, Adapters, REST Controller và Frontend Dashboard UI. |
| **2** | **Kiểm thử đơn vị (Unit Tested)** | **ĐẠT (100%)** | Đã hoàn thành 19 Unit Tests bao phủ toàn bộ Logic nghiệp vụ tại Thực thể miền `Quotation` và Service `QuotationService` (19/19 Tests Pass, 0 Failures, 0 Errors). |
| **3** | **Xác minh theo Tiêu chí chấp nhận (Verified against Acceptance Criteria)** | **ĐẠT (100%)** | Đã xây dựng và vượt qua 10 kịch bản Kiểm thử Tích hợp Đầu - Cuối (End-to-End MockMvc Integration Tests) trên toàn bộ chu trình nghiệp vụ thực tế (10/10 Tests Pass). |

---

## III. MA TRẬN TIÊU CHÍ CHẤP NHẬN (ACCEPTANCE CRITERIA MATRIX)

| Mã AC | Tiêu chí Chấp nhận (Acceptance Criteria) | Hiện thực trong Hệ thống | Kết quả Kiểm thử |
| :---: | :--- | :--- | :---: |
| **AC-01** | **Ràng buộc KYC Kỹ thuật viên (BR01):** Chỉ kỹ thuật viên có trạng thái `verificationStatus == 'APPROVED'` mới được gửi báo giá. Thợ có trạng thái `PENDING` hoặc `REJECTED` phải bị từ chối với mã lỗi HTTP `403 Forbidden` (`errorCode: UNVERIFIED_TECHNICIAN`). | Kiểm tra tại `QuotationService.java:37-45`, ném ra `UnverifiedTechnicianException`. | **PASSED** |
| **AC-02** | **Trạng thái Đơn hàng hợp lệ:** Chỉ được gửi báo giá cho đơn hàng đang ở trạng thái mở tiếp nhận báo giá (`PENDING` hoặc `BIDDING_OPEN`). Các trạng thái đã giao thợ (`IN_PROGRESS`, `COMPLETED`, `CANCELLED`) bị từ chối với HTTP `400 Bad Request` (`errorCode: INVALID_REQUEST_STATUS`). | Kiểm tra tại `QuotationService.java:47-56`, ném `DomainException`. | **PASSED** |
| **AC-03** | **Chống trùng lặp Báo giá (BR02):** Mỗi kỹ thuật viên chỉ được phép có tối đa 01 báo giá cho 01 yêu cầu sửa chữa. Khi gửi lần 2, hệ thống trả về mã lỗi HTTP `409 Conflict` (`errorCode: DUPLICATE_QUOTATION`). | Ràng buộc Database `UNIQUE(repair_request_id, technician_id)` và kiểm tra tại `QuotationService.java:58-62`. | **PASSED** |
| **AC-04** | **Kiểm tra tính hợp lệ & Tính tiền tự động (Validation & Calculation):** Tiền công `priceLabor >= 0`, Tiền vật tư `priceMaterials >= 0`, Tổng tiền `totalPrice > 0`. Phương án sửa chữa `solution` không được để trống. Tổng tiền tự động được tính `totalPrice = priceLabor + priceMaterials`. | Validation Hibernate Bean (`@Valid`), Domain validation trong `Quotation.calculateTotalPrice()` và `Quotation.validate()`. | **PASSED** |
| **AC-05** | **Thu hồi / Rút báo giá (Withdraw Quotation):** Kỹ thuật viên có quyền rút lại báo giá khi đơn vẫn đang chờ phản hồi (`PENDING`). Sau khi rút, trạng thái chuyển thành `WITHDRAWN`. Không cho phép rút báo giá đã được khách hàng duyệt (`ACCEPTED`). | Kiểm tra `Quotation.withdraw()` và endpoint `PATCH /api/v1/technicians/me/quotations/{id}/withdraw`. | **PASSED** |
| **AC-06** | **Kiểm soát Truy cập & Chống rò rỉ dữ liệu (Access Control & Anti-IDOR):**<br>- Khách hàng chỉ xem được danh sách báo giá của yêu cầu do chính mình tạo.<br>- Kỹ thuật viên chỉ xem được báo giá của chính mình (chống lộ giá cạnh tranh giữa các thợ).<br>- Không cho phép thợ này rút báo giá của thợ khác. | Kiểm soát phân quyền phân tầng RBAC và kiểm tra ID người dùng tại `QuotationService.java:101-137`. | **PASSED** |

---

## IV. BẢNG CHI TIẾT HIỆN THỰC CÁC THÀNH PHẦN KỸ THUẬT

### 1. Cơ sở dữ liệu (Flyway Migration V4)
- **Tập tin:** `backend/src/main/resources/db/migration/V4__create_quotations_table.sql`
- **Bảng khởi tạo:** `quotations`
  - Khóa chính: `id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY`
  - Khóa ngoại:
    - `repair_request_id BIGINT NOT NULL REFERENCES repair_requests(id) ON DELETE CASCADE`
    - `technician_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE`
  - Ràng buộc toàn vẹn:
    - `chk_quotation_status`: `CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'WITHDRAWN'))`
    - `chk_quotation_price`: `CHECK (price_labor >= 0 AND price_materials >= 0 AND total_price > 0)`
    - `uk_quotation_request_technician`: `UNIQUE (repair_request_id, technician_id)` (Bảo đảm tính toàn vẹn BR02 ở mức Database)
  - Chỉ mục (Indexes): `idx_quotations_repair_request`, `idx_quotations_technician`, `idx_quotations_status`.

### 2. Thiết kế Kiến trúc Lục giác (Hexagonal Architecture)

```
                       [ REST Clients / Frontend React ]
                                      │
                                      ▼
                        QuotationController (HTTP Adapter)
                                      │
                                      ▼  (Inbound Port)
                             QuotationUseCase
                                      │
                                      ▼
                               QuotationService
                     (Pure Domain Logic & State Transitions)
                           /                     \
                          /                       \
                         ▼                         ▼ (Outbound Port)
                 Quotation (Domain Entity)     QuotationRepositoryPort
                                                   │
                                                   ▼
                                       QuotationPersistenceAdapter
                                                   │
                                                   ▼
                                      SpringDataQuotationRepository
                                                   │
                                                   ▼
                                       [ PostgreSQL / H2 Database ]
```

- **Domain Model (Thuần Java, độc lập Framework):**
  - `Quotation.java`: Chứa toàn bộ logic kiểm tra hợp lệ, phương thức tính toán `calculateTotalPrice()`, phương thức chuyển dịch trạng thái an toàn `withdraw()`, `accept()`, `reject()`.
  - `QuotationStatus.java`: Enum định nghĩa 4 trạng thái vòng đời báo giá (`PENDING`, `ACCEPTED`, `REJECTED`, `WITHDRAWN`).
  - `DuplicateQuotationException.java`: Ngoại lệ miền biểu diễn vi phạm BR02 (HTTP 409 Conflict).
- **Ports Layer:**
  - Inbound Port: `com.fixlink.application.port.in.QuotationUseCase`
  - Outbound Port: `com.fixlink.application.port.out.QuotationRepositoryPort`
- **Application Service:**
  - `QuotationService.java`: Thực thi các quy tắc nghiệp vụ KYC BR01, kiểm tra trạng thái đơn, lưu trữ qua Outbound Port, hỗ trợ xem và rút báo giá an toàn.
- **Persistence Adapter:**
  - `QuotationJpaEntity.java`: Entity ánh xạ ORM bảng `quotations`.
  - `SpringDataQuotationRepository.java`: JPA repository với các query phương thức nghiệp vụ.
  - `QuotationMapper.java`: Bộ chuyển đổi hai chiều giữa Entity và Domain Model.
  - `QuotationPersistenceAdapter.java`: Triển khai Outbound Port kết nối cơ sở dữ liệu.
- **Web Adapter (API Endpoints):**
  - `POST /api/v1/technicians/me/quotations`: Thợ gửi báo giá mới (Trả về 201 Created).
  - `GET /api/v1/technicians/me/quotations`: Thợ xem danh sách báo giá của chính mình.
  - `PATCH /api/v1/technicians/me/quotations/{id}/withdraw`: Thợ rút lại báo giá (PENDING -> WITHDRAWN).
  - `GET /api/v1/repair-requests/{requestId}/quotations`: Khách hàng xem danh sách báo giá gửi cho đơn sửa chữa của mình.
  - `GET /api/v1/quotations/{id}`: Xem chi tiết báo giá.

### 3. Giao diện Người dùng (Frontend Integration)
- **Tập tin:** `frontend/src/pages/TechnicianDashboardPage.tsx`
- **Các tính năng đã tích hợp:**
  - Thêm thẻ hiển thị trạng thái báo giá cho từng yêu cầu sửa chữa trên Dashboard Thợ.
  - Nút **"Gửi báo giá ngay"** mở Modal nhập liệu chuyên nghiệp.
  - Tự động cộng tổng tiền thời gian thực (`Tiền công + Tiền vật tư`) giúp thợ và khách nhìn thấy chi phí trực quan.
  - Badge trạng thái trực quan: `Đang chờ khách duyệt (PENDING)`, `Khách đã chấp nhận (ACCEPTED)`, `Đã rút lại (WITHDRAWN)`.
  - Nút thao tác **"Rút báo giá"** tích hợp hộp thoại xác nhận an toàn.

---

## V. KẾT QUẢ THỰC THI BỘ KIỂM THỬ (TEST EXECUTION SUITE)

Hệ thống đã chạy thành công **100%** toàn bộ các bài kiểm thử đơn vị và tích hợp:

### 1. Kết quả Kiểm thử Đơn vị Thực thể Miền (`QuotationTest.java`)
*Tổng số ca kiểm thử: 9 | Thành công: 9 | Thất bại: 0 | Bị lỗi: 0*

| STT | Tên ca kiểm thử (Test Case) | Mục tiêu kiểm thử | Kết quả |
| :---: | :--- | :--- | :---: |
| 1 | `testCalculateTotalPrice` | Kiểm tra tính toán chính xác tổng tiền = tiền công + tiền vật tư | **PASSED** |
| 2 | `testValidateSuccess` | Kiểm tra dữ liệu báo giá chuẩn xác thực thành công | **PASSED** |
| 3 | `testValidateBlankSolutionThrowsException` | Từ chối khi phương án sửa chữa rỗng hoặc toàn khoảng trắng | **PASSED** |
| 4 | `testValidateNegativeLaborPriceThrowsException` | Từ chối khi chi phí tiền công là số âm | **PASSED** |
| 5 | `testValidateNegativeMaterialsPriceThrowsException` | Từ chối khi chi phí vật tư là số âm | **PASSED** |
| 6 | `testValidateZeroTotalPriceThrowsException` | Từ chối khi tổng giá trị báo giá bằng 0 | **PASSED** |
| 7 | `testWithdrawSuccessWhenPending` | Rút báo giá thành công khi đang ở trạng thái PENDING | **PASSED** |
| 8 | `testWithdrawFailWhenAccepted` | Chặn không cho rút báo giá khi đã được khách hàng duyệt | **PASSED** |
| 9 | `testAcceptAndRejectTransitions` | Kiểm tra chuyển trạng thái sang ACCEPTED và REJECTED | **PASSED** |

### 2. Kết quả Kiểm thử Đơn vị Dịch vụ Nghiệp vụ (`QuotationServiceTest.java`)
*Tổng số ca kiểm thử: 10 | Thành công: 10 | Thất bại: 0 | Bị lỗi: 0*

| STT | Tên ca kiểm thử (Test Case) | Mục tiêu kiểm thử | Kết quả |
| :---: | :--- | :--- | :---: |
| 10 | `testSubmitQuotationSuccess` | Gửi báo giá thành công khi thợ đã duyệt KYC và đơn đang mở | **PASSED** |
| 11 | `testSubmitQuotationFailsWhenTechnicianPendingKYC` | **BR01:** Từ chối báo giá khi thợ đang chờ duyệt KYC (PENDING) | **PASSED** |
| 12 | `testSubmitQuotationFailsWhenTechnicianRejectedKYC` | **BR01:** Từ chối báo giá khi thợ bị từ chối KYC (REJECTED) | **PASSED** |
| 13 | `testSubmitQuotationFailsWhenDuplicateQuotation` | **BR02:** Từ chối báo giá trùng lặp cho cùng một yêu cầu (409 Conflict) | **PASSED** |
| 14 | `testSubmitQuotationFailsWhenRequestNotInOpenStatus` | Từ chối báo giá khi đơn đã hoàn thành hoặc đang sửa | **PASSED** |
| 15 | `testSubmitQuotationFailsWhenRequestNotFound` | Báo lỗi 404 khi mã yêu cầu sửa chữa không tồn tại | **PASSED** |
| 16 | `testWithdrawQuotationSuccess` | Thợ rút thành công báo giá đang chờ xử lý | **PASSED** |
| 17 | `testWithdrawQuotationFailsForNonOwner` | **Chống IDOR:** Thợ khác không thể rút báo giá không phải của mình | **PASSED** |
| 18 | `testGetQuotationsByRequestAsOwnerCustomer` | Khách hàng sở hữu đơn xem được toàn bộ báo giá của đơn đó | **PASSED** |
| 19 | `testGetQuotationsByRequestFailsForNonOwnerCustomer` | Khách hàng khác truy cập đơn người khác bị chặn 403 Forbidden | **PASSED** |

### 3. Kết quả Kiểm thử Tích hợp API Đầu - Cuối (`QuotationApiIntegrationTest.java`)
*Tổng số ca kiểm thử: 10 | Thành công: 10 | Thất bại: 0 | Bị lỗi: 0*

| STT | Luồng kịch bản kiểm thử tích hợp (End-to-End Workflow) | HTTP Status & Dữ liệu trả về | Kết quả |
| :---: | :--- | :---: | :---: |
| 20 | **Bước 1:** Khởi tạo phiên đăng nhập cho Admin, Thợ và Khách hàng; tạo đơn sửa chữa mẫu | `200 OK` (Lấy JWT Access Token thành công) | **PASSED** |
| 21 | **Bước 2 (BR01):** Thợ chưa duyệt KYC gửi báo giá bị từ chối | `403 Forbidden` (`UNVERIFIED_TECHNICIAN`) | **PASSED** |
| 22 | **Bước 3:** Admin duyệt KYC cho thợ thông qua Admin Verification API | `200 OK` (`status = APPROVED`) | **PASSED** |
| 23 | **Bước 4:** Thợ gửi báo giá thành công sau khi được duyệt KYC | `201 Created` (`totalPrice = 350,000 VNĐ`) | **PASSED** |
| 24 | **Bước 5 (BR02):** Thợ gửi trùng báo giá cho cùng 1 đơn bị từ chối | `409 Conflict` (`DUPLICATE_QUOTATION`) | **PASSED** |
| 25 | **Bước 6:** Kiểm tra lỗi Validation đầu vào (tiền công âm, phương án rỗng) | `400 Bad Request` (`VALIDATION_FAILED`) | **PASSED** |
| 26 | **Bước 7:** Thợ truy xuất danh sách báo giá mình đã gửi | `200 OK` (Danh sách gồm 1 báo giá) | **PASSED** |
| 27 | **Bước 8:** Khách hàng sở hữu đơn xem danh sách báo giá nhận được | `200 OK` (Xem được báo giá của thợ) | **PASSED** |
| 28 | **Bước 9:** Khách hàng không liên quan truy cập trái phép bị chặn | `401 / 403 Forbidden` | **PASSED** |
| 29 | **Bước 10:** Thợ rút lại báo giá (chuyển trạng thái thành `WITHDRAWN`) | `200 OK` (`status = WITHDRAWN`) | **PASSED** |

```
------------------------------------------------------------------------
TỔNG KẾT BỘ KIỂM THỬ TÍNH NĂNG F11:
Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
TỶ LỆ VƯỢT QUA: 100% (BUILD SUCCESS)
------------------------------------------------------------------------
```

---

## VI. ĐÁNH GIÁ CÁC TIÊU CHUẨN KỸ THUẬT & AN TOÀN HỆ THỐNG

1. **Tuân thủ Kiến trúc Hexagonal (Clean / Ports & Adapters Architecture):**
   - Thực thể `Quotation` trong `domain/model` hoàn toàn là POJO thuần túy, không chứa bất kỳ annotation của JPA/Hibernate hay Spring Framework.
   - Toàn bộ giao tiếp giữa Application Core và Cơ sở dữ liệu được tách biệt tuyệt đối qua cổng `QuotationRepositoryPort` và triển khai qua `QuotationPersistenceAdapter`.
2. **An toàn Bảo mật & Phân quyền (Security & Access Control):**
   - **Xác thực JWT Stateless:** Mọi yêu cầu đều được xác thực qua filter bảo mật trước khi vào Controller.
   - **Chống lỗi IDOR (Insecure Direct Object References):** Hệ thống so khớp người dùng đăng nhập với trường `technician_id` và `customer_id`, ngăn chặn triệt để hành vi can thiệp hoặc rút trộm báo giá của thợ khác.
   - **Chống rò rỉ giá thầu cạnh tranh:** Khi kỹ thuật viên gọi API xem báo giá theo yêu cầu sửa chữa, hệ thống tự động lọc chỉ trả về báo giá của chính kỹ thuật viên đó, bảo vệ tính công bằng trong đấu thầu dịch vụ.
3. **Toàn vẹn Dữ liệu (ACID Transactions):**
   - Mọi thao tác ghi dữ liệu báo giá đều được bao bọc trong `@Transactional`.
   - Cơ sở dữ liệu được bảo vệ 2 lớp: Bean Validation ở tầng ứng dụng và ràng buộc `CHECK`, `UNIQUE` ở tầng cơ sở dữ liệu.

---

## VII. KẾT LUẬN & KIẾN NGHỊ NGHIỆM THU

### 1. Kết luận của Hội đồng nghiệm thu
- Tính năng **F11: Kỹ thuật viên gửi báo giá (Quotation Submission)** đã được triển khai hoàn tất 100% các hạng mục công việc theo đặc tả yêu cầu nghiệp vụ.
- Đáp ứng đầy đủ định nghĩa hoàn thành (**Definition of Done - DoD**): Đã triển khai đầy đủ mã nguồn, kiểm thử đơn vị bao phủ toàn diện và xác minh đạt tất cả tiêu chí chấp nhận.
- Bộ kiểm thử tự động 29 ca kiểm nghiệm đạt kết quả **Tuyệt đối (100% Passed)**, không phát sinh lỗi hồi quy (Regression bugs).

### 2. Kiến nghị
- **ĐỒNG Ý NGHIỆM THU VÀ BÀN GIAO** tính năng F11 vào nhánh chính (`main/staging`) để sẵn sàng tích hợp với luồng F12 (Khách hàng chấp nhận báo giá & Đặt cọc Escrow 30%).

---

### ĐẠI DIỆN CÁC BÊN KÝ BIÊN BẢN NGHIỆM THU

| ĐẠI DIỆN ĐỘI NGŨ PHÁT TRIỂN | ĐẠI DIỆN QUẢN TRỊ CHẤT LƯỢNG (QA / PO) |
| :---: | :---: |
| *(Ký và ghi rõ họ tên)* | *(Ký và ghi rõ họ tên)* |
| <br><br><br> | <br><br><br> |
| **Kỹ sư Trưởng Phát triển Hệ thống** | **Giám đốc Quản lý Sản phẩm FixLink** |
