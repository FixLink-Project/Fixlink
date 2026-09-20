# FIXLINK - NỀN TẢNG KẾT NỐI KHÁCH HÀNG & THỢ SỬA CHỮA TẠI NHÀ
> **Dự án tốt nghiệp / Đồ án chuyên ngành**  
> **Kiến trúc:** Hexagonal Architecture (Ports & Adapters)  
> **Công nghệ:** Spring Boot 3.3.4, Java 21, Spring Security + JWT, Spring Data JPA, H2 Database (PostgreSQL compatibility mode) / PostgreSQL, Vanilla JS/CSS Responsive UI.

---

## 📌 TỔNG HỢP TẤT CẢ CÁC RC ĐÃ TRIỂN KHAI (ALL MERGED RCs)

File nén này đã tích hợp đầy đủ 100% mã nguồn của toàn bộ các yêu cầu / ticket:

| Ticket | Tên Nghiệp Vụ / Chức Năng | Giao Diện (Frontend) | API Backend Chính |
| :--- | :--- | :--- | :--- |
| **RC-10** | **Customer Registration**<br>Đăng ký tài khoản Khách hàng cá nhân | `customer-register.html` | `POST /api/v1/auth/register/customer` |
| **RC-29** | **Technician Registration (UI)**<br>Giao diện đăng ký Thợ sửa chữa hiện đại | `technician-register.html` | Kết nối API đăng ký thợ |
| **RC-11** | **Technician Registration - Story**<br>Đăng ký thợ kèm eKYC CCCD 2 mặt & thông tin tay nghề | `technician-register.html` | `POST /api/v1/auth/register/technician` |
| **RC-12** | **Login for All Roles**<br>Đăng nhập đa vai trò (Khách hàng, Thợ, Quản trị viên) | `login.html` | `POST /api/v1/auth/login`<br>`GET /api/v1/auth/me` |
| **RC-17** | **Customer Update Profile**<br>Khách hàng tự xem và cập nhật hồ sơ cá nhân | `customer-dashboard.html` | `GET /api/v1/customers/{userId}/profile`<br>`PUT /api/v1/customers/{userId}/profile`<br>`GET /api/v1/customers/{userId}/audit-trail` |
| **RC-3** | **Epic User Management Module**<br>Tự quản lý hồ sơ Thợ & Giám sát Admin toàn diện | `technician-dashboard.html`<br>`admin-dashboard.html` | `GET /PUT /api/v1/technicians/me/profile`<br>`GET /api/v1/admin/users`<br>`GET /api/v1/admin/users/{id}`<br>`PATCH /api/v1/admin/users/{id}/status`<br>`PATCH /api/v1/admin/technicians/{userId}/verify` |
| **RC-8** | **Design Concept Database (extensible for future)**<br>Thiết kế CSDL 8 thực thể lõi, ERD mở rộng, Flyway Migration & Audit Columns | `index.html`<br>[`DATABASE_DESIGN_ERD.md`](docs/DATABASE_DESIGN_ERD.md) | `GET /api/v1/categories`<br>`GET /api/v1/services`<br>Flyway: `V1__...` & `V2__...` |

---

## 🔑 TÀI KHOẢN THỬ NGHIỆM ĐƯỢC SEED SẴN (PRE-SEEDED ACCOUNTS)

Hệ thống đã nạp sẵn dữ liệu mẫu phục vụ kiểm thử và chấm điểm ngay khi khởi động:

| Vai trò | Tên đăng nhập (Username) | Mật khẩu | Mục đích kiểm thử |
| :--- | :--- | :--- | :--- |
| **Quản trị viên (Admin)** | `admin` | `Admin@123` | Quản lý người dùng, tìm kiếm, xem chi tiết, khóa/mở khóa user, duyệt hồ sơ thợ |
| **Khách hàng (Customer)** | `customer01` | `Password@123` | Xem và cập nhật thông tin cá nhân, kiểm tra lịch sử audit trail |
| **Thợ sửa chữa (Technician)** | `tho_dien_lanh_01` | `Password@123` | Xem và cập nhật hồ sơ tay nghề, năm kinh nghiệm, tiểu sử, trạng thái duyệt KYC |
| **Tài khoản bị khóa** | `user_blocked` | `Password@123` | Kiểm thử từ chối đăng nhập (403 Forbidden - `ACCOUNT_BLOCKED`) |
| **Tài khoản chưa kích hoạt** | `user_inactive` | `Password@123` | Kiểm thử từ chối đăng nhập (403 Forbidden - `ACCOUNT_INACTIVE`) |

---

## 🚀 HƯỚNG DẪN CHẠY DỰ ÁN (QUICK START)

### 1. Yêu Cầu Môi Trường
- **Java:** JDK 21 trở lên (Đã tích hợp sẵn Maven Wrapper `mvnw`).
- **Cổng mặc định:** `8080`.

### 2. Khởi Động Ứng Dụng
Mở PowerShell hoặc Terminal tại thư mục gốc của dự án và chạy:
```powershell
.\mvnw spring-boot:run
```
*(Hoặc đóng gói file JAR và chạy trực tiếp:)*
```powershell
.\mvnw clean package -DskipTests
java -jar target/fixlink-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### 3. Truy Cập Hệ Thống
Sau khi server báo khởi động thành công, mở trình duyệt và truy cập:
- **Cổng điều hướng trung tâm (FixLink Hub):**  
  👉 [http://localhost:8080/index.html](http://localhost:8080/index.html)
- **Đăng ký Khách hàng:** `http://localhost:8080/customer-register.html`
- **Đăng ký Thợ (eKYC CCCD 2 mặt):** `http://localhost:8080/technician-register.html`
- **Đăng nhập đa vai trò:** `http://localhost:8080/login.html`
- **Trang Khách hàng (Profile Self-Service):** `http://localhost:8080/customer-dashboard.html`
- **Trang Kỹ thuật viên (Profile & Status):** `http://localhost:8080/technician-dashboard.html`
- **Trang Quản trị viên (Admin Oversight & Verification):** `http://localhost:8080/admin-dashboard.html`
- **Swagger / OpenAPI UI:** `http://localhost:8080/swagger-ui/index.html`
- **H2 Database Console:** `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:fixlink_db`, User: `sa`, Password: *(trống)*)

### 4. Chạy Kiểm Thử Tự Động (Automated Integration Tests)
```powershell
.\mvnw test
```
- **Kết quả kiểm thử:** 26/26 Integration Tests đạt chuẩn **PASS 100% (0 Failures, 0 Errors)**.

---

## 🏛️ CẤU TRÚC KIẾN TRÚC HEXAGONAL (HEXAGONAL ARCHITECTURE)

```text
src/main/java/com/fixlink/
├── domain/                      # Domain Core (Thuần Java, không phụ thuộc framework)
│   ├── model/                  # Entities & Value Objects: User, CustomerProfile, TechnicianProfile, AuditLog, Role, UserStatus...
│   └── exception/              # Domain Exceptions: InvalidCredentialsException, AccountBlockedException...
├── application/                # Application Layer (Use Cases & Business Logic)
│   ├── port/
│   │   ├── in/                 # Input Ports (UseCases): AuthUseCase, CustomerProfileUseCase, TechnicianUseCase, AdminUserUseCase
│   │   └── out/                # Output Ports: UserRepositoryPort, CustomerProfileRepositoryPort, TechnicianProfileRepositoryPort, AuditLogRepositoryPort
│   └── service/                # Domain Services điều phối nghiệp vụ
├── adapter/                    # Adapters Layer
│   ├── in/web/                 # Inbound Web Adapters: REST Controllers & DTOs
│   │   ├── controller/         # AuthController, CustomerController, TechnicianController, AdminUserController
│   │   └── dto/                # Request & Response DTOs chuẩn RESTful
│   └── out/persistence/        # Outbound Persistence Adapters: Spring Data JPA Repositories & Entities
│       ├── entity/             # JpaEntities kế thừa BaseEntity (6 trường audit: created_at, created_by, updated_at, updated_by, deleted_at, deleted_by)
│       ├── repository/         # Spring Data Interfaces
│       └── adapter/            # Implementations của Output Ports
└── infrastructure/             # Infrastructure Layer
    ├── config/                 # SecurityConfig, WebConfig, AuditAwareConfig
    └── security/               # JwtTokenProvider, JwtAuthenticationFilter, UserPrincipal
```
