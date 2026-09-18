# FixLink Platform - System Architecture & Package Guidelines

Tài liệu quy định cấu trúc mã nguồn, phân tầng trách nhiệm (Layered Architecture) và các quy ước kỹ thuật chung cho dự án **FixLink**.

---

## 1. Kiến trúc phân tầng (Layered Architecture)

Hệ thống FixLink Backend được xây dựng trên nền tảng **Spring Boot 3 (Java 21)** theo mô hình kiến trúc phân tầng tiêu chuẩn:

```
[ Client / Web / Mobile App ]
             │
             ▼
┌─────────────────────────────────────────────────────────────┐
│ 1. Controller Layer (@RestController)                       │
│    - Tiếp nhận HTTP Request, validate dữ liệu (Jakarta)     │
│    - Đóng gói response với ApiResponse<T> & ErrorResponse   │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Service Layer (@Service)                                 │
│    - Xử lý nghiệp vụ chính, logic điều phối giao dịch       │
│    - Quản lý Transaction (@Transactional)                   │
│    - Chuyển đổi giữa Entity và DTO                          │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Repository Layer (@Repository - Spring Data JPA)         │
│    - Truy xuất dữ liệu PostgreSQL thông qua Hibernate JPA   │
│    - Hỗ trợ Dynamic Query với JpaSpecificationExecutor      │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Database Layer (PostgreSQL 16 / In-memory H2)            │
│    - Quản lý phiên bản bảng bằng Flyway Database Migration  │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Base Packages Structure

Toàn bộ mã nguồn backend nằm dưới package gốc: `com.fixlink`:

```
com.fixlink/
├── FixLinkApplication.java        # Main Spring Boot Entrypoint
│
├── config/                        # Shared System Configurations
│   ├── SecurityConfig.java        # Spring Security Filter Chain & RBAC
│   ├── OpenApiConfig.java         # Swagger / OpenAPI 3.0 Documentation
│   ├── WebConfig.java             # Shared CORS & WebMvc Registry
│   └── DataInitializer.java       # Database bootstrap & demo seeder
│
├── controller/                    # REST API Controllers (v1)
│   ├── AuthController.java        # Public Authentication (/api/v1/auth)
│   ├── ServiceCategoryController  # Public Category Catalog (/api/v1/categories)
│   ├── AdminUserController.java   # Admin User Management (/api/v1/admin/users)
│   └── AdminServiceCatalogCtrl    # Admin Catalog Management (/api/v1/admin/catalog)
│
├── dto/                           # Data Transfer Objects
│   ├── request/                   # Input payload DTOs with validation rules
│   │   ├── LoginRequest.java
│   │   ├── CreateCategoryRequest.java
│   │   ├── UpdateUserStatusRequest.java
│   │   └── ...
│   └── response/                  # Output envelope & entity representations
│       ├── ApiResponse.java       # Generic standard API envelope
│       ├── ErrorResponse.java     # RFC-compliant error payload
│       ├── PaginatedResponse.java # Pagination container
│       ├── UserDto.java
│       └── ...
│
├── entity/                        # JPA Database Entities
│   ├── User.java                  # Central accounts table (users)
│   ├── CustomerProfile.java       # 1:1 customer details
│   ├── TechnicianProfile.java     # 1:1 technician credentials & verification
│   ├── ServiceCategory.java       # Service parent categories
│   ├── ServiceItem.java           # Specific repair service catalog
│   └── ...
│
├── enums/                         # System-wide Enums
│   ├── UserRole.java              # ADMIN, CUSTOMER, TECHNICIAN, STAFF
│   ├── UserStatus.java            # ACTIVE, INACTIVE, BANNED
│   └── VerificationStatus.java    # PENDING, APPROVED, REJECTED
│
├── exception/                     # Centralized Error Handling
│   ├── GlobalExceptionHandler.java# @RestControllerAdvice handling exceptions
│   ├── ResourceNotFoundException  # 404 Not Found
│   ├── InvalidOperationException  # 400 Bad Request
│   └── TooManyRequestsException   # 429 Rate Limit
│
├── repository/                    # Data Access Interfaces
│   ├── UserRepository.java
│   ├── ServiceCategoryRepository
│   ├── ServiceItemRepository
│   └── ...
│
├── security/                      # Authentication & Authorization Engine
│   ├── JwtTokenProvider.java      # Token generation, signing (HS512), parsing
│   ├── JwtAuthenticationFilter    # OncePerRequestFilter extracting Bearer tokens
│   └── CustomUserDetailsService   # Load user by username for Spring Security
│
├── service/                       # Business Logic Layer
│   ├── AuthService.java           # Login, Token Issuance, Session Verification
│   ├── AdminUserService.java     # Account lock/unlock, technician verification
│   ├── ServiceCatalogService.java # Categories, services CRUD
│   └── LoginAttemptService.java   # Brute-force protection & rate limiter
│
└── specification/                 # Dynamic Query Builders
    └── UserSpecification.java     # JPA Criteria predicates (search, filter, role)
```

---

## 3. Shared Configurations & Standards

### 3.1. Chuẩn hóa API Response
Mọi endpoint trả về thành công đều tuân theo cấu trúc bao bọc (envelope):

```json
{
  "statusCode": 200,
  "message": "Thao tác thành công",
  "data": { ... }
}
```

Khi có lỗi xảy ra (4xx, 5xx), `GlobalExceptionHandler` bắt và trả về định dạng chuẩn:

```json
{
  "statusCode": 400,
  "errorCode": "INVALID_ARGUMENT",
  "message": "Chi tiết nguyên nhân lỗi",
  "errors": {
    "field": "Lý do không hợp lệ"
  },
  "timestamp": "2026-09-18T09:30:00"
}
```

### 3.2. Bảo mật & Xác thực (JWT Stateless)
- Sử dụng thuật toán ký `HS512` bảo đảm an toàn mật mã.
- Thời gian sống mặc định của access token: **24 giờ** (86,400,000 ms).
- Header yêu cầu: `Authorization: Bearer <token>`.
- Phân quyền theo vai trò (RBAC) với các tiền tố `ROLE_ADMIN`, `ROLE_CUSTOMER`, `ROLE_TECHNICIAN`, `ROLE_STAFF`.

### 3.3. Cross-Origin Resource Sharing (CORS)
Được cấu hình tập trung tại `com.fixlink.config.WebConfig`:
- Cho phép các nguồn client phổ biến trong môi trường dev: `http://localhost:3000`, `http://localhost:5173`, `http://localhost:8080`, `http://127.0.0.1:5500`.
- Hỗ trợ đầy đủ các method: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`.
- Cho phép gửi credentials và Authorization header.

### 3.4. Quản lý Cơ sở Dữ liệu (Flyway Migrations)
- Mọi thay đổi schema DDL đều được tạo file migration tại `src/main/resources/db/migration/V{version}__{description}.sql`.
- Tránh thay đổi cấu trúc bảng trực tiếp trên database server mà không qua migration script.
