# FixLink — Kiến trúc backend và quy ước package

Tài liệu quy định cách tổ chức mã nguồn backend và các quy ước kỹ thuật chung.

---

## 1. Kiến trúc Hexagonal (Ports & Adapters)

Backend dựng trên Spring Boot 3 (Java 21) theo mô hình Ports & Adapters. Nguyên tắc
cốt lõi: **phụ thuộc luôn hướng vào trong**. Tầng `domain` không biết gì về Spring,
JPA hay HTTP; tầng `application` chỉ làm việc qua các interface (port); mọi chi tiết
kỹ thuật nằm ở `adapter` và `infrastructure`.

```
        [ Trình duyệt / Ứng dụng di động ]
                       │  HTTP
                       ▼
   ┌──────────────────────────────────────────────┐
   │ adapter/in/web        Adapter vào             │
   │   controller + DTO request/response           │
   └───────────────────────┬──────────────────────┘
                           │ gọi qua port in
                           ▼
   ┌──────────────────────────────────────────────┐
   │ application           Use case               │
   │   port/in   interface mô tả nghiệp vụ         │
   │   service   hiện thực use case, @Transactional│
   │   port/out  interface mô tả nhu cầu dữ liệu   │
   └───────────────────────┬──────────────────────┘
                           │ phụ thuộc hướng vào trong
                           ▼
   ┌──────────────────────────────────────────────┐
   │ domain                Mô hình nghiệp vụ       │
   │   model, exception, util — thuần Java         │
   └──────────────────────────────────────────────┘
                           ▲
                           │ hiện thực port out
   ┌───────────────────────┴──────────────────────┐
   │ adapter/out           Adapter ra              │
   │   persistence: JPA entity, repository, mapper │
   │   security:    mã hoá mật khẩu, sinh JWT      │
   └───────────────────────┬──────────────────────┘
                           ▼
        [ PostgreSQL 16 / H2 in-memory ]
```

Lợi ích thực tế: đổi PostgreSQL sang cơ sở dữ liệu khác, hay đổi cách sinh JWT, chỉ
cần viết adapter mới — `domain` và `application` không phải sửa.

---

## 2. Cấu trúc package

Toàn bộ mã nguồn backend nằm dưới `com.fixlink`:

```
com.fixlink/
├── FixLinkApplication.java         Điểm khởi động Spring Boot
│
├── domain/                         Nghiệp vụ thuần, không phụ thuộc framework
│   ├── model/                      User, CustomerProfile, TechnicianProfile,
│   │                               ServiceCategory, RepairRequest, các enum
│   │                               Role / UserStatus / VerificationStatus
│   ├── exception/                  Ngoại lệ nghiệp vụ: InvalidCredentials,
│   │                               AccountBlocked, ResourceNotFound, ...
│   └── util/                       Hàm thuần dùng chung (kiểm tra mật khẩu, ...)
│
├── application/                    Điều phối use case
│   ├── port/in/                    Interface mô tả nghiệp vụ mà adapter vào gọi
│   │                               AuthUseCase, ChangePasswordUseCase,
│   │                               PasswordResetUseCase, SessionUseCase,
│   │                               UserManagementUseCase, ...
│   ├── port/out/                   Interface mô tả thứ application cần từ bên ngoài
│   │                               UserRepositoryPort, TokenProviderPort,
│   │                               PasswordEncoderPort, ...
│   └── service/                    Hiện thực use case, quản lý @Transactional
│                                   AuthService, AccountSecurityService,
│                                   CustomerProfileService, ...
│
├── adapter/
│   ├── in/web/                     Adapter vào: HTTP
│   │   ├── controller/             REST controller theo module nghiệp vụ
│   │   └── dto/request|response/   Payload vào/ra kèm ràng buộc validation
│   └── out/                        Adapter ra
│       ├── persistence/entity/     JPA entity (hậu tố JpaEntity)
│       ├── persistence/repository/ Spring Data repository
│       ├── persistence/mapper/     Chuyển đổi giữa entity JPA và model domain
│       ├── persistence/adapter/    Hiện thực các port/out lưu trữ
│       └── security/               Hiện thực port mã hoá mật khẩu và JWT
│
└── infrastructure/                 Chi tiết hạ tầng và cấu hình Spring
    ├── config/                     SecurityConfig, OpenApiConfig, DataInitializer
    ├── exception/                  GlobalExceptionHandler (@RestControllerAdvice)
    ├── security/                   Filter JWT, UserDetailsService
    └── service/                    Dịch vụ hạ tầng (gửi email, chống brute-force)
```

### Quy ước đặt tên

| Loại | Quy ước | Ví dụ |
| :--- | :--- | :--- |
| Interface use case | `*UseCase` | `PasswordResetUseCase` |
| Hiện thực use case | `*Service` | `AccountSecurityService` |
| Interface cổng ra | `*Port` | `UserRepositoryPort` |
| Hiện thực cổng ra | `*Adapter` | `UserRepositoryAdapter` |
| Entity JPA | `*JpaEntity` | `PasswordResetTokenJpaEntity` |
| Model domain | tên nghiệp vụ thuần | `User`, `TechnicianProfile` |

---

## 3. Quy ước dùng chung

### 3.1. Lớp bọc response API

Mọi endpoint thành công trả về cùng một cấu trúc:

```json
{
  "statusCode": 200,
  "message": "Thao tác thành công",
  "data": { }
}
```

Danh sách có thêm khối `meta` phân trang:

```json
{
  "statusCode": 200,
  "message": "Lấy danh sách thành công",
  "meta": { "currentPage": 1, "limit": 10, "totalItems": 45, "totalPages": 5,
            "hasNext": true, "hasPrevious": false },
  "data": []
}
```

Khi có lỗi (4xx, 5xx), `GlobalExceptionHandler` trả về:

```json
{
  "statusCode": 400,
  "errorCode": "VALIDATION_FAILED",
  "message": "Chi tiết nguyên nhân lỗi",
  "errors": { "field": "Lý do không hợp lệ" }
}
```

Đường dẫn không tồn tại trả `404 RESOURCE_NOT_FOUND`. Lỗi ngoài dự kiến trả
`500 INTERNAL_SERVER_ERROR` với thông báo chung, chi tiết chỉ ghi vào log server.

### 3.2. Xác thực JWT (stateless)

- Thuật toán ký `HS512`.
- Access token sống mặc định 24 giờ, refresh token 7 ngày.
- Header: `Authorization: Bearer <token>`.
- Phân quyền theo vai trò với tiền tố `ROLE_ADMIN`, `ROLE_CUSTOMER`, `ROLE_TECHNICIAN`.

### 3.3. CORS

Cấu hình tập trung tại `com.fixlink.infrastructure.config.SecurityConfig`
(bean `corsConfigurationSource`): cho phép mọi origin ở môi trường phát triển, đủ các
method `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`, kèm credentials và header
`Authorization`.

> Trước khi lên production cần thu hẹp danh sách origin về đúng tên miền của hệ thống.

### 3.4. Flyway migration

- Mọi thay đổi schema đều qua file `src/main/resources/db/migration/V{n}__{mô_tả}.sql`.
- Không sửa trực tiếp cấu trúc bảng trên server mà không qua migration.
- Không sửa nội dung một migration đã chạy ở môi trường thật: Flyway đối chiếu
  checksum và sẽ báo lỗi. Muốn đổi thì thêm migration mới.
