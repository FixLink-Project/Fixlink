# FixLink Platform

[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg?style=flat&logo=openjdk)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)
[![Node.js](https://img.shields.io/badge/Node.js-20%2B-green.svg?style=flat&logo=nodedotjs)](https://nodejs.org/)
[![Vite](https://img.shields.io/badge/Frontend-Vite%20SPA-646CFF.svg?style=flat&logo=vite)](https://vitejs.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16%20Alpine-blue.svg?style=flat&logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Multi--stage%20Containers-2496ED.svg?style=flat&logo=docker)](https://www.docker.com/)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-2088FF.svg?style=flat&logo=githubactions)](#-cicd-pipeline--staging-deployment)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0%20Swagger-green.svg?style=flat&logo=swagger)](http://localhost:8080/swagger-ui/index.html)

**FixLink** là nền tảng số kết nối khách hàng có nhu cầu sửa chữa với đội ngũ kỹ thuật viên lành nghề (Điện, Nước, Điện lạnh, Sơn, Điện tử,...). Hệ thống cung cấp cơ chế đấu giá minh bạch, quản lý tiến độ thời gian thực, bảo hành và thanh toán an toàn.

> [!NOTE]
> **Dự án đã tích hợp toàn bộ các Release Candidate (Sprint 0)** theo chuẩn Kiến trúc Lục giác (Hexagonal Architecture / Ports & Adapters) theo tài liệu thiết kế hệ thống (`FixLink_PhanTich_ThietKe.docx`):
> - **RC-1, RC-8, RC-9**: Hạ tầng Monorepo, Database Flyway migration, BaseEntity 6 cột audit, API Contract & OpenAPI Swagger.
> - **RC-10, RC-11, RC-29**: Đăng ký Khách hàng, Đăng ký Kỹ thuật viên & eKYC CCCD 2 mặt.
> - **RC-12, RC-25**: Đăng nhập đa vai trò, Rate limiting chống Brute-force & Khóa tài khoản tạm thời.
> - **RC-13**: Đổi mật khẩu (Change Password) & Chính sách mật khẩu mạnh (PasswordValidator regex).
> - **RC-14**: Quên mật khẩu (Forgot/Reset Password) chống User Enumeration, Token UUID 15 phút, gửi email/URL reset.
> - **RC-15**: Quản lý phiên, Refresh Token Rotation & Đăng xuất Blacklist JWT.
> - **RC-17**: Hồ sơ Khách hàng & Nhật ký kiểm toán (Audit Trail).
> - **RC-18**: Hồ sơ Kỹ thuật viên (Chuyên môn danh mục & Khu vực hoạt động Service Areas).
> - **RC-23**: Quản trị Danh mục Dịch vụ Admin CRUD & Đánh giá mức độ ảnh hưởng (Impact Assessment).
> - **RC-3**: Phê duyệt & Từ chối KYC Thợ dành cho Quản trị viên (Admin KYC Verification).

### 🚀 Khởi chạy Nhanh Trong 1 Cú Click (Windows)
- **Chạy toàn bộ Backend & Giao diện Web:** Double-click file `run-project.bat` trong thư mục gốc `FixLink/`. Hệ thống tự động thiết lập bộ nhớ, khởi động H2 database và Spring Boot server trên cổng **8080**.
- **Chạy 31 bài kiểm thử tự động (Integration Tests):** Double-click file `run-test.bat`.
- **Cổng thông tin Web trực tiếp:** [http://localhost:8080/index.html](http://localhost:8080/index.html)
- **Tài liệu API Swagger:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## 📁 Cấu trúc Thư mục Toàn diện (Repository Structure)

```
FixLink/
├── .editorconfig                       # Quy chuẩn formatting đa IDE (UTF-8, LF, 4-space indent)
├── .env.example                        # Mẫu biến môi trường an toàn
├── .gitignore                          # Cấu hình bỏ qua file nhạy cảm, build artifacts, cache
├── docker-compose.yml                  # Khởi chạy fullstack cục bộ (Database + Backend + Frontend)
├── docker-compose.staging.yml          # Cấu hình container điều phối môi trường Staging
├── README.md                           # Cẩm nang dự án (Definition of Done)
│
├── .github/                            # CI/CD Workflows & GitHub Configurations
│   ├── workflows/
│   │   ├── ci.yml                      # CI: Build & Test Backend + Frontend + Container Image (mỗi PR)
│   │   └── deploy-staging.yml          # CD: Tự động deploy Staging khi merge vào main
│   ├── PULL_REQUEST_TEMPLATE.md        # Template chuẩn cho Pull Request
│   └── branch-protection-rules.md      # Quy định Branch Protection bảo vệ nhánh main
│
├── .githooks/                          # Git hooks tự động cho dự án
│   └── pre-commit                      # Kiểm tra format (Spotless) trước khi cho phép commit
│
├── .vscode/                            # Cấu hình Visual Studio Code
│   ├── extensions.json                 # Khuyến nghị extension tiêu chuẩn
│   └── settings.json                   # Tự động format khi lưu file
│
├── backend/                            # Dịch vụ Spring Boot 3 Backend
│   ├── Dockerfile                      # Multi-stage Dockerfile (Maven -> JRE 21 Alpine)
│   ├── mvnw & mvnw.cmd                 # Maven Wrapper (chạy không cần cài sẵn Maven)
│   ├── pom.xml                         # Quản lý dependencies, Spotless, Checkstyle
│   ├── checkstyle.xml                  # Bộ quy tắc kiểm tra định dạng code
│   └── src/
│       ├── main/java/com/fixlink/      # Mã nguồn Java (Controller, Service, Repository, DTO, Config)
│       ├── main/resources/             # Cấu hình application.yml, migrations Flyway, Static UI
│       └── test/java/com/fixlink/      # Unit & Integration Tests (MockMvc, SpringBootTest)
│
├── frontend/                           # Ứng dụng Giao diện Người dùng (Vite SPA)
│   ├── Dockerfile                      # Multi-stage Dockerfile (Node -> Nginx Alpine)
│   ├── nginx.conf                      # Nginx config & reverse proxy /api/ về backend
│   ├── package.json                    # Scripts: dev, build, lint, test
│   ├── vite.config.js                  # Cấu hình Vite & Vitest
│   ├── src/                            # Giao diện, API Client, Style
│   └── tests/                          # Frontend Unit Tests (Vitest)
│
├── docker/                             # Dữ liệu khởi tạo database
│   └── init/
│       ├── 01-schema.sql               # Định nghĩa 21+ bảng cơ sở dữ liệu
│       └── 02-seed.sql                 # Dữ liệu mẫu hoàn chỉnh (users, categories, deals)
│
├── docs/                               # Tài liệu thiết kế & vận hành chuyên sâu
│   ├── architecture.md                 # Kiến trúc hệ thống, phân tầng & quy ước package
│   ├── database-conceptual-model.md    # Mô hình ERD Phase 1 & kiến trúc mở rộng Phase 2+
│   ├── local-development-setup.md      # Cẩm nang cài đặt chi tiết cho người mới
│   ├── ci-secrets-and-security.md      # Hướng dẫn cấu hình GitHub Secrets an toàn
│   ├── branch-protection-rules.md      # Thiết lập Branch Protection Rules trên GitHub
│   └── staging-deployment-guide.md     # Hướng dẫn vận hành hạ tầng Staging
│
└── scripts/                            # Tiện ích tự động hóa
    ├── setup-hooks.bat                 # Kích hoạt git pre-commit hook trên Windows
    └── setup-hooks.sh                  # Kích hoạt git pre-commit hook trên macOS/Linux
```

---

## ⚡ Khởi chạy Nhanh (Quick Start)

Dự án thỏa mãn tiêu chí **Definition of Done**: Một kỹ sư mới có thể clone, build và chạy toàn bộ service cục bộ chỉ bằng file README này.

### Yêu cầu tiên quyết
- **JDK 21 LTS** ([Tải tại đây](https://adoptium.net/))
- **Node.js 20+ & npm** ([Tải tại đây](https://nodejs.org/))
- **Docker & Docker Compose** ([Tải Docker Desktop](https://www.docker.com/products/docker-desktop/))
- **Git**

---

### Cách 1: Chạy Toàn Bộ Stack Bằng Docker Compose (Khuyên dùng)

Chỉ với 2 câu lệnh, toàn bộ Database, Backend và Frontend sẽ cùng khởi động:

```bash
# 1. Tạo tệp môi trường
cp .env.example .env   # (Windows: Copy-Item .env.example .env)

# 2. Khởi chạy toàn bộ hệ sinh thái
docker compose up --build -d
```

- **Frontend Web UI**: [http://localhost](http://localhost) (cổng 80)
- **Backend REST API**: [http://localhost:8080](http://localhost:8080)
- **Swagger Documentation**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

---

### Cách 2: Chạy Từng Phần Cho Quá Trình Phát Triển (Local Dev Mode)

#### 1. Khởi động PostgreSQL Database
```bash
docker compose up -d db
```

#### 2. Khởi động Backend (Spring Boot 3)
```bash
cd backend
# Windows:
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"
.\mvnw.cmd spring-boot:run

# macOS / Linux:
./mvnw spring-boot:run
```

#### 3. Khởi động Frontend (Vite Dev Server)
```bash
cd frontend
npm install
npm run dev
```
> Giao diện frontend sẽ chạy tại: **[http://localhost:5173](http://localhost:5173)** với tính năng Hot Module Replacement (HMR) và tự động proxy các request `/api/` về Backend cổng 8080.

---

### 🌐 Chế độ Chạy Không Cần Docker (In-memory H2 Profile)

Nếu máy tính của bạn không có Docker hoặc cần chạy kiểm thử nhanh offline:

- **Windows:** `cd backend && .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local`
- **macOS/Linux:** `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`

---

## 👥 Tài khoản Thử nghiệm Mặc định

Tất cả tài khoản mẫu đều sử dụng chung mật khẩu: **`password123`**

| Vai trò | Username | Dashboard truy cập | Quyền hạn |
| :--- | :--- | :--- | :--- |
| **Quản trị viên (Admin)** | `admin` | [admin-dashboard.html](http://localhost:8080/admin-dashboard.html) | Quản lý người dùng, duyệt thợ, danh mục dịch vụ |
| **Khách hàng (Customer)** | `nguyenvana` hoặc `tranthib` | [customer-dashboard.html](http://localhost:8080/customer-dashboard.html) | Đăng yêu cầu sửa chữa, chọn báo giá, đánh giá |
| **Kỹ thuật viên (Technician)** | `levanduc` hoặc `phamvanminh` | [technician-dashboard.html](http://localhost:8080/technician-dashboard.html) | Đấu giá, cập nhật tiến độ, nhận thanh toán ví |

---

## 🚀 CI/CD Pipeline & Staging Deployment

Dự án áp dụng quy trình tự động hóa kiểm thử và triển khai chuẩn DevOps:

```
[ Pull Request to 'main' ]
         │
         ▼ Trigger .github/workflows/ci.yml
   ┌───────────────────────────────────────────────┐
   │ • Backend: Spotless, Checkstyle, Tests, JAR   │
   │ • Frontend: Lint, Vitest Tests, Build Assets  │
   │ • Docker: Build Backend & Frontend Images     │
   └───────────────────────┬───────────────────────┘
                           │ (All Checks Pass & Approved)
                           ▼
                 [ Merge to 'main' ]
                           │
                           ▼ Trigger .github/workflows/deploy-staging.yml
   ┌───────────────────────────────────────────────┐
   │ • Push Release Images to GHCR (ghcr.io)       │
   │ • SSH Connect to Staging Host                 │
   │ • Zero-Downtime Rolling Container Restart     │
   │ • Automated Smoke Healthcheck Tests           │
   └───────────────────────────────────────────────┘
```

- **Definition of Done**: Mọi Pull Request tự động kích hoạt CI kiểm tra toàn diện; khi merge vào `main`, hệ thống tự động deploy lên môi trường Staging.
- **Bảo mật Secret**: Mọi bí mật kết nối và mật khẩu được lưu trữ an toàn trong kho bí mật **GitHub Secrets Store** ([Xem hướng dẫn cấu hình](docs/ci-secrets-and-security.md)), cam kết tuyệt đối không commit vào mã nguồn.
- **Branch Protection**: Nhánh `main` được bảo vệ bằng luật bắt buộc PR và bắt buộc toàn bộ CI jobs phải PASS trước khi merge ([Xem quy định Branch Protection](docs/branch-protection-rules.md)).

---

## 🛠️ Code Quality, Formatting & Testing

```bash
# Backend (chạy trong thư mục backend/)
./mvnw spotless:apply     # Tự động căn chỉnh & format mã nguồn Java
./mvnw spotless:check     # Kiểm tra format Spotless
./mvnw checkstyle:check   # Kiểm tra cú pháp linter Checkstyle
./mvnw test               # Chạy toàn bộ Unit & Integration Test

# Frontend (chạy trong thư mục frontend/)
npm test                  # Chạy unit tests với Vitest
npm run lint              # Kiểm tra linting code
npm run build             # Đóng gói bản phân phối production
```

---

## 📖 Tài liệu Chuyên Sâu Tham Khảo

- 📊 [Mô Hình Dữ Liệu Phase 1 & Khả Năng Mở Rộng Phase 2+ (docs/database-conceptual-model.md)](docs/database-conceptual-model.md)
- 🏛️ [Kiến trúc Phân tầng & Quy ước Base Packages (docs/architecture.md)](docs/architecture.md)
- 💻 [Cẩm nang Chi tiết Cài đặt Môi trường Cục bộ (docs/local-development-setup.md)](docs/local-development-setup.md)
- 🔐 [Quản lý CI Secrets & Bảo Mật Kho Bí Mật (docs/ci-secrets-and-security.md)](docs/ci-secrets-and-security.md)
- 🛡️ [Quy tắc Bảo vệ Nhánh Branch Protection (docs/branch-protection-rules.md)](docs/branch-protection-rules.md)
- 🌐 [Cẩm nang Vận hành & Triển khai Staging (docs/staging-deployment-guide.md)](docs/staging-deployment-guide.md)
