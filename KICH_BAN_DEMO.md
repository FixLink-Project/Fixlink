# Báo Cáo Triển Khai Chuyên Nghiệp Dự Án FixLink (RC-32, RC-33, RC-34, RC-37)

---

## 1. Tổng Quan Kết Quả Đạt Được

Chúng tôi đã hoàn thành toàn bộ các yêu cầu từ Jira và tài liệu đặc tả của dự án FixLink theo đúng chuẩn **Clean Hexagonal Architecture (Ports and Adapters)**, chuẩn chất lượng đồ án tốt nghiệp / nộp giảng viên:
- **Biên dịch**: 100% sạch, 0 cảnh báo/lỗi (`BUILD SUCCESS`).
- **Chuẩn định dạng mã nguồn (Spotless)**: Đã format và kiểm tra 155/155 file nguồn chuẩn xác (`BUILD SUCCESS`).
- **Tổng số bài kiểm tra**: **80/80 tests ĐẠT 100%** (0 Failures, 0 Errors, 0 Skipped).
  - 16/16 Unit Tests mới trong [RepairRequestServiceTest.java](file:///c:/Users/GIGABYTE/fixlink%20new/backend/src/test/java/com/fixlink/application/service/RepairRequestServiceTest.java)
  - 61/61 Integration Tests trong [FixLinkApiIntegrationTest.java](file:///c:/Users/GIGABYTE/fixlink%20new/backend/src/test/java/com/fixlink/adapter/in/web/controller/FixLinkApiIntegrationTest.java)
  - 3/3 Domain Model Tests trong [TechnicianProfileTest.java](file:///c:/Users/GIGABYTE/fixlink%20new/backend/src/test/java/com/fixlink/domain/TechnicianProfileTest.java)

---

## 2. Chi Tiết Các Hạng Mục Kỹ Thuật Đã Xử Lý

### A. Phát hiện và Khắc phục Lỗi Gốc trong `.gitignore`
- **Nguyên nhân cốt lõi**: File `.gitignore` ở thư mục gốc có dòng `out/`, khiến Git tự động bỏ qua toàn bộ thư mục `com/fixlink/adapter/out/**` và `com/fixlink/application/port/out/**`. Khi người khác clone repository về, toàn bộ tầng Outbound Persistence bị thiếu dẫn đến lỗi biên dịch.
- **Giải pháp**: Cập nhật `.gitignore` chỉ loại trừ thư mục `/out/` ở root dự án của IntelliJ, đồng thời thêm ngoại lệ `!**/src/**/out/**` để luôn theo dõi mã nguồn Outbound.

### B. Bổ sung Tầng Outbound (Hexagonal Persistence Layer)
1. **Application Outbound Ports** ([backend/src/main/java/com/fixlink/application/port/out/](file:///c:/Users/GIGABYTE/fixlink%20new/backend/src/main/java/com/fixlink/application/port/out)):
   - [UserRepositoryPort.java](file:///c:/Users/GIGABYTE/fixlink%20new/backend/src/main/java/com/fixlink/application/port/out/UserRepositoryPort.java)
   - [CustomerProfileRepositoryPort.java](file:///c:/Users/GIGABYTE/fixlink%20new/backend/src/main/java/com/fixlink/application/port/out/CustomerProfileRepositoryPort.java)
   - [TechnicianProfileRepositoryPort.java](file:///c:/Users/GIGABYTE/fixlink%20new/backend/src/main/java/com/fixlink/application/port/out/TechnicianProfileRepositoryPort.java)
   - [PasswordEncoderPort.java](file:///c:/Users/GIGABYTE/fixlink%20new/backend/src/main/java/com/fixlink/application/port/out/PasswordEncoderPort.java)
   - [TokenProviderPort.java](file:///c:/Users/GIGABYTE/fixlink%20new/backend/src/main/java/com/fixlink/application/port/out/TokenProviderPort.java)
2. **JPA Entities** ([backend/src/main/java/com/fixlink/adapter/out/persistence/entity/](file:///c:/Users/GIGABYTE/fixlink%20new/backend/src/main/java/com/fixlink/adapter/out/persistence/entity)):
   - Khởi tạo đầy đủ 16 thực thể JPA ánh xạ chính xác với PostgreSQL & Flyway migrations.
   - Triển khai `Persistable<Long>` cho `CustomerProfileJpaEntity` và `TechnicianProfileJpaEntity` để giải quyết triệt để lỗi `AssertionFailure: null identifier` trong Hibernate 6 đối với `@MapsId`.
3. **Flyway Migration V5**:
   - Tạo file [V5__create_security_tokens_tables.sql](file:///c:/Users/GIGABYTE/fixlink%20new/backend/src/main/resources/db/migration/V5__create_security_tokens_tables.sql) khởi tạo 3 bảng `password_reset_tokens`, `refresh_tokens`, `token_blacklist` để thỏa mãn cấu hình `hibernate.ddl-auto: validate`.

---

## 3. Triển Khai Chi Tiết Các Task Nghiệp Vụ Jira

### RC-32: Khách Hàng Tạo Yêu Cầu Sửa Chữa (Create Repair Request)
- **DTO Validation** trong [CreateRepairRequestRequest.java](file:///c:/Users/GIGABYTE/fixlink%20new/backend/src/main/java/com/fixlink/adapter/in/web/dto/request/CreateRepairRequestRequest.java):
  - `@NotBlank`, `@Size(max = 200)` cho `title`
  - `@NotBlank`, `@Size(max = 2000)` cho `description`
  - `@NotNull` cho `categoryId`
  - `@NotBlank`, `@Size(max = 255)` cho `addressLine`
  - `@DecimalMin("0.0")` cho `budgetRef`
  - `@Min(1)`, `@Max(30)` cho `biddingDeadlineDays`
  - `@Size(max = 10)` cho danh sách media
- **Business Logic** trong [RepairRequestService.java](file:///c:/Users/GIGABYTE/fixlink%20new/backend/src/main/java/com/fixlink/application/service/RepairRequestService.java):
  - Kiểm tra trạng thái tài khoản khách hàng (`UserStatus.ACTIVE`), chặn tài khoản bị khóa `BLOCKED` hoặc `INACTIVE` (`ACCOUNT_INACTIVE` - 403).
  - Kiểm tra danh mục dịch vụ tồn tại và đang hoạt động (`isActive = true`).
  - Kiểm tra khu vực dịch vụ tồn tại và đang hoạt động (`isActive = true`).
  - Chuẩn hóa mã yêu cầu: `REQ-yyyyMMdd-XXXX` (VD: `REQ-20260923-ABCD`).
  - Lưu danh sách tệp đính kèm (`MediaJpaEntity`).
  - Ghi nhận lịch sử tiến độ công việc (`WorkProgressJpaEntity` - `"Khách tạo yêu cầu mới"`).

### RC-33: Khách Hàng Cập Nhật Yêu Cầu Sửa Chữa (Update Repair Request)
- **DTO Validation** trong [UpdateRepairRequestRequest.java](file:///c:/Users/GIGABYTE/fixlink%20new/backend/src/main/java/com/fixlink/adapter/in/web/dto/request/UpdateRepairRequestRequest.java): Toàn bộ ràng buộc tương tự Create Request.
- **Business Logic** trong [RepairRequestService.java](file:///c:/Users/GIGABYTE/fixlink%20new/backend/src/main/java/com/fixlink/application/service/RepairRequestService.java):
  - Kiểm tra quyền sở hữu (`customerId`), trả về `ACCESS_DENIED` (403) nếu không phải chủ đơn.
  - Kiểm tra trạng thái đơn: Chỉ cho phép sửa khi `DRAFT` hoặc `BIDDING_OPEN`.
  - Quy tắc nghiệp vụ BR: Nếu ở trạng thái `BIDDING_OPEN` mà đã có ít nhất một thợ gửi báo giá (`quoteCount > 0`) -> Lập tức chặn với mã lỗi `INVALID_OPERATION` (400).
  - Kiểm tra danh mục và khu vực cập nhật phải hợp lệ và đang hoạt động.
  - Ghi nhận lịch sử cập nhật vào `work_progress` (`"Khách cập nhật thông tin yêu cầu"`).

### RC-34: Khách Hàng Hủy Yêu Cầu Sửa Chữa (Cancel Repair Request)
- **DTO Validation** trong [CancelRepairRequestRequest.java](file:///c:/Users/GIGABYTE/fixlink%20new/backend/src/main/java/com/fixlink/adapter/in/web/dto/request/CancelRepairRequestRequest.java):
  - `@NotBlank`, `@Size(min = 3, max = 500)` cho `reason`.
- **Business Logic** trong [RepairRequestService.java](file:///c:/Users/GIGABYTE/fixlink%20new/backend/src/main/java/com/fixlink/application/service/RepairRequestService.java):
  - Chặn hủy nếu đơn đang ở trạng thái không thể hủy theo BR07 (`IN_PROGRESS`, `AWAITING_ACCEPTANCE`, `COMPLETED`, `CANCELLED`).
  - Chuyển trạng thái sang `CANCELLED` và lưu `cancelReason`.
  - **Tự động Cascade**: Chuyển toàn bộ các báo giá `PENDING` của thợ trên yêu cầu này sang trạng thái `REJECTED` với ghi chú rõ ràng: `"[Hủy do khách hủy yêu cầu]"`.
  - Ghi nhận tiến độ công việc vào bảng `work_progress` (`"Khách hủy: <lý do>"`).

### RC-37: Thợ Tìm Kiếm Đơn & Gửi Báo Giá (Technician Discovery & Quoting)
- Đảm bảo thợ phải được KYC xác minh (`APPROVED`) mới được xem đơn phù hợp.
- Lọc đơn theo các danh mục và khu vực kỹ thuật viên đã đăng ký.
- Đã được bao phủ và xác minh 100% trong `FixLinkApiIntegrationTest` (Order 37-52).

---

## 4. Bằng Chứng Kiểm Thử Đạt 100% (Test Run Evidence)

```
[INFO] Running com.fixlink.adapter.in.web.controller.FixLinkApiIntegrationTest
[INFO] Tests run: 61, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 12.22 s -- in com.fixlink.adapter.in.web.controller.FixLinkApiIntegrationTest
[INFO] Running com.fixlink.application.service.RepairRequestServiceTest
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.936 s -- in com.fixlink.application.service.RepairRequestServiceTest
[INFO] Running com.fixlink.domain.TechnicianProfileTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.007 s -- in com.fixlink.domain.TechnicianProfileTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 80, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

Git commit đã được tạo chuẩn Conventional Commits trên nhánh `main`:
`feat(repair-request): complete RC-32, RC-33, RC-34 and restore hexagonal persistence layer`
