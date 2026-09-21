# FixLink - Hướng dẫn Cài đặt & Phát triển Môi trường Cục bộ (Local Setup Guide)

Chào mừng bạn gia nhập đội ngũ phát triển **FixLink Platform**! Tài liệu này cung cấp hướng dẫn chi tiết từng bước giúp bạn thiết lập, chạy thử và phát triển mã nguồn trên máy tính cá nhân.

---

## 1. Yêu cầu Hệ thống (Prerequisites)

Hãy đảm bảo máy tính của bạn đã cài đặt các công cụ sau:

| Công cụ | Phiên bản tối thiểu | Mục đích | Hướng dẫn cài đặt |
| :--- | :--- | :--- | :--- |
| **Java JDK** | **21 LTS** | Biên dịch và chạy Spring Boot backend | [Oracle JDK 21](https://www.oracle.com/java/technologies/downloads/#java21) hoặc [Eclipse Temurin 21](https://adoptium.net/) |
| **Docker & Docker Compose** | Docker Desktop 4.x+ | Chạy container PostgreSQL database cục bộ | [Docker Desktop](https://www.docker.com/products/docker-desktop/) |
| **Git** | 2.30+ | Quản lý mã nguồn & pre-commit hooks | [Git SCM](https://git-scm.com/) |
| **IDE khuyên dùng** | VS Code hoặc IntelliJ | Lập trình | Đọc mục cấu hình IDE bên dưới |

---

## 2. Các Bước Khởi Chạy Nhanh (3 Bước)

### Bước 1: Khởi tạo tệp môi trường `.env`

Từ thư mục gốc dự án (`FixLink/`), sao chép tệp cấu hình mẫu:

- **Trên Windows (PowerShell / CMD):**
  ```powershell
  Copy-Item .env.example .env
  ```
- **Trên macOS / Linux / Git Bash:**
  ```bash
  cp .env.example .env
  ```

### Bước 2: Khởi chạy Cơ sở dữ liệu PostgreSQL

Khởi chạy container PostgreSQL 16 trong nền bằng Docker Compose:

```bash
docker compose up -d
```

Kiểm tra container đang chạy:
```bash
docker compose ps
```
> Kết quả `fixlink_db` ở trạng thái `Up (healthy)` là thành công. Toàn bộ 23 bảng và dữ liệu mẫu (seed data) đã được tự động khởi tạo.

### Bước 3: Chạy ứng dụng Backend

Di chuyển vào thư mục `backend/` và khởi chạy với Maven Wrapper:

- **Trên Windows (PowerShell):**
  ```powershell
  cd backend
  $env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"   # (thay bằng đường dẫn JDK 21 của bạn nếu cần)
  .\mvnw.cmd spring-boot:run
  ```
- **Trên macOS / Linux / Git Bash:**
  ```bash
  cd backend
  ./mvnw spring-boot:run
  ```

---

## 3. Chế độ Chạy Độc Lập Không Cần Docker (Offline H2 Profile)

Nếu máy tính của bạn chưa có Docker hoặc cần chạy test nhanh offline, dự án hỗ trợ sẵn **Profile `local`** sử dụng cơ sở dữ liệu **H2 In-Memory**:

- **Windows (PowerShell):**
  ```powershell
  cd backend
  .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
  ```
- **macOS / Linux:**
  ```bash
  cd backend
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
  ```

---

## 4. Kiểm tra Dịch vụ sau khi Khởi chạy

Khi Spring Boot hoàn tất khởi động trên cổng **8080**, bạn có thể truy cập các đường dẫn sau bằng trình duyệt:

Backend chỉ phục vụ API và tài liệu; giao diện chạy riêng bằng Vite (xem README).

| Thành phần | Đường dẫn URL | Mô tả |
| :--- | :--- | :--- |
| **Giao diện (Vite dev server)** | [http://localhost:5173](http://localhost:5173) | Chạy `cd frontend && npm run dev` |
| **Health check** | [http://localhost:8080/api/v1/health](http://localhost:8080/api/v1/health) | Trả `{ "status": "UP" }` khi backend sẵn sàng |
| **Tài liệu Swagger UI** | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) | OpenAPI 3.0 Specs & Thử nghiệm API |
| **OpenAPI Schema (JSON)** | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) | Bản mô tả API máy đọc |
| **H2 Console** *(nếu dùng profile local)* | [http://localhost:8080/h2-console](http://localhost:8080/h2-console) | JDBC URL: `jdbc:h2:mem:fixlink_db` |

---

## 5. Danh sách Tài khoản Kiểm thử Mặc định

Được nạp sẵn khi khởi động ở profile `local`.

| Vai trò | Username | Mật khẩu |
| :--- | :--- | :--- |
| **Admin (Quản trị viên)** | `admin` | `Admin@123` |
| **Customer (Khách hàng)** | `customer01` | `Password@123` |
| **Technician (Kỹ thuật viên, chờ duyệt)** | `tho_dien_lanh_01` | `Password@123` |
| **Tài khoản bị khoá** | `user_blocked` | `Password@123` |
| **Tài khoản ngừng hoạt động** | `user_inactive` | `Password@123` |

---

## 6. Cấu hình Chuẩn Mã nguồn & Pre-commit Hooks

Dự án áp dụng cơ chế tự động kiểm tra định dạng và chất lượng mã nguồn trước mỗi commit.

### Kích hoạt Git Pre-commit Hook (Chỉ làm 1 lần sau khi clone)

- **Cách 1 (Tự động với Script có sẵn):**
  - Windows: Chạy file `scripts/setup-hooks.bat`
  - macOS/Linux: Chạy `./scripts/setup-hooks.sh`
- **Cách 2 (Thủ công qua lệnh Git):**
  ```bash
  git config core.hooksPath .githooks
  ```

### Các lệnh Định dạng & Kiểm tra Code (Spotless & Checkstyle)

Thực hiện trong thư mục `backend/`:

```bash
# 1. Tự động sửa định dạng toàn bộ mã nguồn (thụt lề, xóa import thừa, xóa khoảng trắng)
./mvnw spotless:apply     # (hoặc .\mvnw.cmd spotless:apply trên Windows)

# 2. Kiểm tra xem mã nguồn có vi phạm định dạng không
./mvnw spotless:check

# 3. Chạy kiểm tra quy chuẩn Checkstyle
./mvnw checkstyle:check

# 4. Chạy toàn bộ Unit & Integration Test
./mvnw test
```

---

## 7. Khắc phục Sự cố Thường Gặp (Troubleshooting)

### Q1: Lỗi `JAVA_HOME not found in your environment`
- **Nguyên nhân**: Hệ thống chưa thiết lập biến môi trường `JAVA_HOME` trỏ tới JDK 21.
- **Khắc phục**:
  - Windows PowerShell: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"` (hoặc thêm vào System Environment Variables trong Windows Settings).
  - Linux/macOS: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` (thêm vào `~/.bashrc` hoặc `~/.zshrc`).

### Q2: Cổng 8080 hoặc 5432 bị chiếm (Port already in use)
- **Cổng 8080**: Đổi cổng trong `backend/src/main/resources/application.yml` bằng cách sửa `server.port: 8081`.
- **Cổng 5432**: Nếu máy bạn đã có sẵn PostgreSQL cục bộ, bạn có thể đổi `POSTGRES_PORT=5433` trong file `.env` và cập nhật cổng tương ứng trong `application.yml`.

### Q3: Muốn xóa sạch cơ sở dữ liệu để tạo lại từ đầu
```bash
docker compose down -v
docker compose up -d
```
Lệnh trên sẽ xóa volume dữ liệu và tự động chạy lại toàn bộ script `01-schema.sql` và `02-seed.sql`.
