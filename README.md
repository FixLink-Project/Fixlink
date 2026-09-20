# FixLink

Nền tảng kết nối khách hàng có đồ gia dụng hỏng với thợ sửa chữa đã xác minh danh tính
tại Việt Nam: điện lạnh, điện dân dụng, ống nước, đồ gia dụng, khóa cửa.

Khách đăng yêu cầu kèm ảnh và khu vực, thợ quanh đó gửi báo giá, khách so rồi chọn.
Tiền cọc giữ ở tài khoản trung gian cho tới khi khách nghiệm thu.

---

## Kiến trúc

| Tầng | Công nghệ |
| :--- | :--- |
| Backend | Spring Boot 3.3.4, Java 21, Spring Security + JWT, Spring Data JPA, Flyway |
| Cơ sở dữ liệu | PostgreSQL 16 (production), H2 chế độ tương thích PostgreSQL (chạy cục bộ) |
| Frontend | React 18, TypeScript, Vite, Tailwind CSS, React Router |
| Hạ tầng | Docker Compose, Nginx, GitHub Actions |

Backend theo kiến trúc Hexagonal (Ports & Adapters):

```
com.fixlink
├── domain/          Mô hình nghiệp vụ thuần, không phụ thuộc framework
├── application/     Use case và các cổng vào/ra (port in / port out)
├── adapter/
│   ├── in/web/      REST controller, DTO request/response
│   └── out/         JPA entity, repository, bộ chuyển đổi, bảo mật
└── infrastructure/  Cấu hình Spring, xử lý ngoại lệ, dịch vụ hạ tầng
```

---

## Quy ước API

Base URL: `http://localhost:8080`, mọi đường dẫn bắt đầu bằng `/api/v1`.

Response thành công:

```json
{ "statusCode": 200, "message": "Lấy danh sách thành công", "data": { } }
```

Danh sách có thêm khối `meta` phân trang:

```json
{
  "statusCode": 200,
  "message": "Lấy danh sách người dùng thành công",
  "meta": { "currentPage": 1, "limit": 10, "totalItems": 45, "totalPages": 5,
            "hasNext": true, "hasPrevious": false },
  "data": []
}
```

Response lỗi:

```json
{ "statusCode": 400, "errorCode": "VALIDATION_FAILED",
  "message": "Dữ liệu đầu vào không hợp lệ",
  "errors": { "phone": "Số điện thoại không đúng định dạng" } }
```

Đặc tả đầy đủ: [docs/API_SPECIFICATION.md](docs/API_SPECIFICATION.md).
Swagger UI khi chạy cục bộ: http://localhost:8080/swagger-ui.html

---

## Cài đặt và chạy

### Yêu cầu

- JDK 21 trở lên
- Node.js 20 trở lên
- Docker Desktop (chỉ cần nếu muốn chạy PostgreSQL hoặc toàn bộ stack bằng container)

### Cách 1 — Chạy cục bộ để phát triển

Backend dùng H2 in-memory, không cần Docker:

```bash
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Trên Windows có thể bấm đúp `run-project.bat` thay cho lệnh trên.

Frontend chạy ở cửa sổ terminal khác:

```bash
cd frontend && npm install && npm run dev
```

- Giao diện: http://localhost:5173 (Vite tự chuyển tiếp `/api` về cổng 8080)
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console — JDBC `jdbc:h2:mem:fixlink_db`, user `sa`, mật khẩu để trống

### Cách 2 — Chạy toàn bộ stack bằng Docker

```bash
cp .env.example .env
docker compose up --build -d
```

- Giao diện: http://localhost
- API: http://localhost:8080

Chỉ khởi động riêng PostgreSQL: `docker compose up -d db`, rồi chạy backend không kèm
profile `local`.

---

## Tài khoản mẫu

Được nạp sẵn khi khởi động ở profile `local`.

| Vai trò | Tên đăng nhập | Mật khẩu |
| :--- | :--- | :--- |
| Quản trị viên | `admin` | `Admin@123` |
| Khách hàng | `customer01` | `Password@123` |
| Thợ (chờ duyệt KYC) | `tho_dien_lanh_01` | `Password@123` |
| Tài khoản bị khóa | `user_blocked` | `Password@123` |
| Tài khoản ngừng hoạt động | `user_inactive` | `Password@123` |

Hai tài khoản cuối dùng để kiểm thử luồng từ chối đăng nhập.

---

## Cấu trúc thư mục

```
FixLink/
├── backend/              Dịch vụ Spring Boot
│   ├── src/main/java/    Mã nguồn theo kiến trúc Hexagonal
│   ├── src/main/resources/
│   │   ├── db/migration/ Flyway migration
│   │   └── static/       Trang HTML tĩnh (đang được thay dần bằng frontend React)
│   ├── checkstyle.xml    Bộ quy tắc linter
│   └── pom.xml
├── frontend/             Ứng dụng React + TypeScript + Vite
│   ├── src/lib/          Client API và kiểu dữ liệu dùng chung
│   ├── src/pages/        Các trang theo route
│   └── tailwind.config.js
├── docker/init/          Script khởi tạo PostgreSQL cho container
├── docs/                 Tài liệu thiết kế, API, vận hành
├── scripts/              Kích hoạt git hook
└── docker-compose.yml
```

---

## Kiểm thử và chất lượng mã

```bash
# Backend
cd backend
./mvnw test               # 31 bài unit + integration test
./mvnw spotless:apply     # Tự căn chỉnh định dạng Java
./mvnw spotless:check     # Kiểm tra định dạng (git hook pre-commit gọi lệnh này)
./mvnw checkstyle:check   # Linter
```

```bash
# Frontend
cd frontend
npm test                  # Vitest
npm run typecheck         # Kiểm tra kiểu TypeScript
npm run build             # Đóng gói bản production
```

Trên Windows có thể bấm đúp `run-test.bat` để chạy kiểm thử backend.

Bật git hook kiểm tra định dạng trước khi commit:

```bash
./scripts/setup-hooks.sh   # Windows: scripts\setup-hooks.bat
```

---

## Tài liệu

- [Đặc tả API](docs/API_SPECIFICATION.md)
- [Kiến trúc phân tầng](docs/architecture.md)
- [Thiết kế cơ sở dữ liệu](docs/database-design-erd.md) — [sơ đồ ERD](docs/database-erd.jpg)
- [Mô hình dữ liệu và hướng mở rộng](docs/database-conceptual-model.md)
- [Cài đặt môi trường cục bộ](docs/local-development-setup.md)
- [Quản lý secret cho CI](docs/ci-secrets-and-security.md)
- [Quy tắc bảo vệ nhánh](docs/branch-protection-rules.md)
- [Vận hành môi trường staging](docs/staging-deployment-guide.md)
