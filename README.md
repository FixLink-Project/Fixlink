# FixLink Platform

[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg?style=flat&logo=openjdk)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16%20Alpine-blue.svg?style=flat&logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg?style=flat&logo=docker)](https://www.docker.com/)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0%20Swagger-green.svg?style=flat&logo=swagger)](http://localhost:8080/swagger-ui/index.html)
[![Code Style](https://img.shields.io/badge/Code%20Style-Spotless%20%7C%20Checkstyle-blueviolet.svg)](#-code-quality--formatting)

**FixLink** là nền tảng số kết nối khách hàng có nhu cầu sửa chữa với đội ngũ kỹ thuật viên lành nghề (Điện, Nước, Điện lạnh, Sơn, Điện tử,...). Hệ thống cung cấp cơ chế đấu giá minh bạch, quản lý tiến độ thời gian thực, bảo hành và thanh toán an toàn.

---

## 📁 Cấu trúc Thư mục Dự án

```
FixLink/
├── .editorconfig                       # Quy chuẩn formatting đa IDE (UTF-8, LF, 4-space indent)
├── .env.example                        # Mẫu biến môi trường an toàn
├── .gitignore                          # Cấu hình bỏ qua file nhạy cảm, build artifacts, cache
├── docker-compose.yml                  # Cấu hình container PostgreSQL 16
├── README.md                           # Cẩm nang khởi chạy dự án (Definition of Done)
│
├── .githooks/                          # Git hooks tự động cho dự án
│   └── pre-commit                      # Kiểm tra format (Spotless) trước khi cho phép commit
│
├── .vscode/                            # Cấu hình Visual Studio Code
│   ├── extensions.json                 # Khuyến nghị extension tiêu chuẩn
│   └── settings.json                   # Tự động format khi lưu file
│
├── backend/                            # Dịch vụ Spring Boot 3 Backend
│   ├── mvnw & mvnw.cmd                 # Maven Wrapper (chạy không cần cài sẵn Maven)
│   ├── pom.xml                         # Quản lý dependencies, Spotless, Checkstyle
│   ├── checkstyle.xml                  # Bộ quy tắc kiểm tra định dạng code
│   └── src/
│       ├── main/java/com/fixlink/      # Mã nguồn Java (Controller, Service, Repository, DTO, Config)
│       ├── main/resources/             # Cấu hình application.yml, migrations Flyway, Static UI
│       └── test/java/com/fixlink/      # Unit & Integration Tests (MockMvc, SpringBootTest)
│
├── docker/                             # Dữ liệu khởi tạo database
│   └── init/
│       ├── 01-schema.sql               # Định nghĩa 21+ bảng cơ sở dữ liệu
│       └── 02-seed.sql                 # Dữ liệu mẫu hoàn chỉnh (users, categories, deals)
│
├── docs/                               # Tài liệu chi tiết
│   ├── architecture.md                 # Kiến trúc hệ thống, phân tầng & quy ước package
│   └── local-development-setup.md      # Hướng dẫn chi tiết môi trường phát triển cục bộ
│
└── scripts/                            # Tiện ích tự động hóa
    ├── setup-hooks.bat                 # Kích hoạt git pre-commit hook trên Windows
    └── setup-hooks.sh                  # Kích hoạt git pre-commit hook trên macOS/Linux
```

---

## ⚡ Khởi chạy Nhanh (Quick Start)

Dự án thỏa mãn tiêu chí: **Một kỹ sư mới có thể clone, build và chạy dịch vụ chỉ bằng file README này**.

### Yêu cầu tiên quyết
- **JDK 21 LTS** ([Tải tại đây](https://adoptium.net/))
- **Docker & Docker Compose** ([Tải Docker Desktop](https://www.docker.com/products/docker-desktop/))
- **Git**

---

### Bước 1: Thiết lập tệp môi trường `.env`

Sao chép file mẫu `.env.example` thành `.env`:

- **Windows (PowerShell):**
  ```powershell
  Copy-Item .env.example .env
  ```
- **macOS / Linux:**
  ```bash
  cp .env.example .env
  ```

---

### Bước 2: Khởi chạy Cơ sở dữ liệu (PostgreSQL Container)

Khởi động container cơ sở dữ liệu PostgreSQL trong nền:

```bash
docker compose up -d
```

> 💡 *Database sẽ tự động nạp 21+ bảng và dữ liệu mẫu (seed data) trong thư mục `docker/init/`.*

---

### Bước 3: Build & Chạy Backend Service

Di chuyển vào thư mục `backend/` và khởi chạy với Maven Wrapper:

- **Trên Windows (PowerShell):**
  ```powershell
  cd backend
  $env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"   # Đường dẫn JDK 21 của bạn
  .\mvnw.cmd spring-boot:run
  ```
- **Trên macOS / Linux:**
  ```bash
  cd backend
  ./mvnw spring-boot:run
  ```

Khi thấy dòng log `Started FixLinkApplication in X seconds` trên cổng **8080**, hệ thống đã sẵn sàng!

---

### 🌐 Chế độ Chạy Không Cần Docker (In-memory H2 Profile)

Nếu máy tính của bạn không có Docker hoặc cần chạy kiểm thử nhanh:

- **Windows:** `cd backend && .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local`
- **macOS/Linux:** `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`

---

## 🔗 Truy cập Ứng dụng & Tài liệu API

Sau khi service khởi chạy tại cổng `8080`:

| Thành phần | Đường dẫn URL | Mô tả |
| :--- | :--- | :--- |
| **Giao diện Web UI** | [http://localhost:8080/login.html](http://localhost:8080/login.html) | Màn hình đăng nhập & Dashboard người dùng |
| **Swagger UI (Interactive)** | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) | Thử nghiệm trực tiếp các REST API endpoints |
| **OpenAPI Schema (JSON)** | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) | Schema kỹ thuật chuẩn OpenAPI 3.0 |
| **H2 Database Console** | [http://localhost:8080/h2-console](http://localhost:8080/h2-console) | Bảng điều khiển H2 (khi bật profile `local`) |

---

## 👥 Tài khoản Thử nghiệm Mặc định

Tất cả tài khoản mẫu đều sử dụng chung mật khẩu: **`password123`**

| Vai trò | Username | Dashboard truy cập | Quyền hạn |
| :--- | :--- | :--- | :--- |
| **Quản trị viên (Admin)** | `admin` | [admin-dashboard.html](http://localhost:8080/admin-dashboard.html) | Quản lý người dùng, duyệt thợ, danh mục dịch vụ |
| **Khách hàng (Customer)** | `nguyenvana` hoặc `tranthib` | [customer-dashboard.html](http://localhost:8080/customer-dashboard.html) | Đăng yêu cầu sửa chữa, chọn báo giá, đánh giá |
| **Kỹ thuật viên (Technician)** | `levanduc` hoặc `phamvanminh` | [technician-dashboard.html](http://localhost:8080/technician-dashboard.html) | Đấu giá, cập nhật tiến độ, nhận thanh toán ví |

---

## 🛠️ Code Quality, Formatting & Git Hooks

Dự án thiết lập tiêu chuẩn định dạng mã nguồn tự động thông qua **Spotless** và kiểm tra cú pháp bằng **Checkstyle**.

### 1. Kích hoạt Git Pre-commit Hook
Chỉ cần thực hiện 1 lần sau khi clone dự án:
- **Windows**: Chạy file `scripts\setup-hooks.bat`
- **macOS / Linux**: Chạy lệnh `./scripts/setup-hooks.sh` (hoặc `git config core.hooksPath .githooks`)

Mỗi khi bạn thực hiện `git commit`, hook sẽ tự động chạy kiểm tra định dạng code.

### 2. Các lệnh hữu ích (chạy trong thư mục `backend/`)

```bash
# Tự động căn chỉnh, xóa import thừa và format code
./mvnw spotless:apply     # (Windows: .\mvnw.cmd spotless:apply)

# Kiểm tra định dạng code Spotless
./mvnw spotless:check

# Kiểm tra quy chuẩn linter Checkstyle
./mvnw checkstyle:check

# Chạy toàn bộ Unit & Integration Test
./mvnw test

# Đóng gói file JAR chạy production
./mvnw clean package -DskipTests
```

---

## 🗄️ Quản lý Cơ sở Dữ liệu (Docker)

```bash
# Kiểm tra trạng thái container
docker compose ps

# Xem logs container database
docker compose logs -f db

# Truy cập dòng lệnh psql
docker compose exec db psql -U fixlink -d fixlink_db

# Reset toàn bộ cơ sở dữ liệu về trạng thái ban đầu
docker compose down -v && docker compose up -d
```

---

## 📖 Tài liệu Tham khảo Thêm

- [Kiến trúc Phân tầng & Quy ước Base Packages (docs/architecture.md)](docs/architecture.md)
- [Cẩm nang Chi tiết Cài đặt Môi trường Cục bộ (docs/local-development-setup.md)](docs/local-development-setup.md)
