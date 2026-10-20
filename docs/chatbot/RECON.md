# BÁO CÁO KHẢO SÁT HỆ THỐNG FIXLINK & NGUỒN TÁI SỬ DỤNG (RECON)

**Thời gian khảo sát:** 2026-10-17  
**Người thực hiện:** Principal Engineer kiêm AI Engineer  
**Nhánh:** `feature/chatbot-cskh`

---

## 1. Khảo Sát Hệ Thống FixLink (Target System)

### 1.1 Backend Kiến Trúc Hexagonal
- **Framework & Runtime:** Java 21 LTS, Spring Boot 3.3.4, Maven Wrapper (`backend/mvnw.cmd`).
- **Cấu trúc Package:**
  - `com.fixlink.domain`: Entity cốt lõi (`User`, `Role`, `RepairRequest`, `RequestStatus`, `ServiceCategory`, `ServiceArea`, `TechnicianProfile`), Value Object, Exception miền nghiệp vụ.
  - `com.fixlink.application`: Port vào/ra (`RepairRequestUseCase`, `TechnicianUseCase`, `MasterDataUseCase`, `AppointmentUseCase`, v.v.) và Service cài đặt use case.
  - `com.fixlink.adapter.in.web`: REST Controller (`AuthController`, `CategoryController`, `RepairRequestController`, `AppointmentController`, v.v.) và DTO Request/Response.
  - `com.fixlink.adapter.out.persistence`: JPA Entity, Repository, Adapter chuyển đổi dữ liệu.
  - `com.fixlink.infrastructure`: Bảo mật (`SecurityConfig`, `JwtAuthenticationFilter`, `JwtTokenProvider`), Cấu hình (`DataInitializer`), Xử lý lỗi toàn cục (`GlobalExceptionHandler`).

### 1.2 Bảo Mật & Xác Thực (Security & Auth)
- **SecurityConfig (`SecurityConfig.java`):**
  - Cơ chế Stateless JWT qua `JwtAuthenticationFilter`.
  - **Route công khai (`permitAll`):**
    - Tài nguyên tĩnh: `/`, `/*.html`, `/css/**`, `/js/**`, `/images/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/h2-console/**`.
    - Healthcheck: `GET /api/v1/health`.
    - Auth công khai: `POST /api/v1/auth/login`, `POST /api/v1/auth/register`, `POST /api/v1/auth/register/technician`, `POST /api/v1/auth/forgot-password`, `POST /api/v1/auth/reset-password`.
    - Master Data công khai: `GET /api/v1/categories/**`, `GET /api/v1/services/**`, `GET /api/v1/areas/**`.
  - **Phân quyền vai trò:**
    - `ADMIN`: `/api/v1/admin/**`.
    - `STAFF`, `ADMIN`: `/api/v1/staff/**`.
    - `TECHNICIAN`, `ADMIN`: `/api/v1/technicians/**`.
    - `CUSTOMER`, `STAFF`, `ADMIN`: `/api/v1/customers/**`.
    - Mọi route khác: `.anyRequest().authenticated()`.
  - **Lấy danh tính người dùng hiện tại:**
    - Lấy trực tiếp từ `SecurityContextHolder.getContext().getAuthentication()` (hoặc inject `Authentication` vào Controller).
    - Email/Username được lưu trong `authentication.getName()`. Cấm nhận `userId` từ client hoặc do LLM tự suy diễn.

### 1.3 Quy Ước Phản Hồi (Envelope Response & Error Format)
- **Thành công:** Được bọc trong `ApiResponse<T>`:
  ```json
  {
    "statusCode": 200,
    "message": "Thông báo thành công",
    "data": { ... }
  }
  ```
- **Lỗi nghiệp vụ (`GlobalExceptionHandler.java`):**
  ```json
  {
    "statusCode": 400,
    "errorCode": "VALIDATION_FAILED",
    "message": "Mô tả lỗi thân thiện",
    "errors": { "field": "chi tiết lỗi" }
  }
  ```
- Các mã HTTP chuẩn: 400 (Bad Request), 401 (Unauthorized), 403 (Forbidden), 404 (Not Found), 409 (Conflict - e.g. `JOB_ALREADY_TAKEN`), 429 (Rate Limited / Account Temporarily Locked).

### 1.4 Thực Thể Nghiệp Vụ & Flyway Migrations
- Migration hiện hành đã chạy đến **`V11`** (`V11__phone_unique_per_role_and_timezone_note.sql`).
- **Thực thể chính:**
  - `categories`, `services`, `service_areas`: Master data ngành nghề, khu vực.
  - `repair_requests`: Yêu cầu sửa chữa.
  - `appointments`: Lịch hẹn giữa thợ và khách hàng (Jira RC-48).
  - `technician_profiles`, `technician_areas`, `technician_categories`: Hồ sơ thợ, trạng thái KYC (`PENDING`, `APPROVED`, `REJECTED`), trạng thái `is_online`.
- **RequestStatus hiện hành (`RequestStatus.java`):**
  - Luồng hoạt động chuẩn FCFS (First-Come First-Served):
    `DRAFT → OPEN → ASSIGNED → INSPECTING → AWAITING_COST_APPROVAL → IN_PROGRESS → AWAITING_ACCEPTANCE → COMPLETED | CANCELLED`.
  - Trạng thái `@Deprecated` (không dùng cho nghiệp vụ mới): `BIDDING_OPEN`, `MATCHED_AWAITING_DEPOSIT`.

### 1.5 Frontend
- **Công nghệ:** React 18, TypeScript 5.9, Vite 5.4, Tailwind CSS 3.4, React Router DOM v6.
- **Tổ chức mã nguồn:**
  - `frontend/src/App.tsx`: Định nghĩa toàn bộ route ứng dụng. Chatbot Widget sẽ được mount tại đây để luôn hiển thị ở mọi trang.
  - `frontend/src/lib/api.ts`: Wrapper fetch HTTP, quản lý JWT token trong `localStorage`, xử lý refresh/401 redirect, chuẩn hóa `ApiError`.
  - `frontend/src/lib/auth.tsx`: React Context quản lý trạng thái đăng nhập (`user`, `role`, `login`, `logout`).
  - `frontend/src/components/DashboardLayout.tsx`: Layout chung cho trang quản trị/dashboard.
- **Trang HTML tĩnh:** `backend/src/main/resources/static/*.html` (customer-dashboard, technician-dashboard, admin-dashboard, index, login, v.v.). Cần cung cấp widget vanilla JS `static/js/chatbot-widget.js` để nhúng đồng bộ.

---

## 2. Khảo Sát Nguồn Tái Sử Dụng

### 2.1 EvShop RAG Assistant (`rag-javaspring2`)
- **LangChain4j & Model Resiliency:**
  - `ResilientGeminiChatModel.java`: Quản lý danh sách model ứng viên, failover tự động khi gặp lỗi 429/503/RESOURCE_EXHAUSTED với cooldown backoff (`modelExhaustedUntil`), fallback về local context synthesizer nếu toàn bộ cloud model lỗi.
  - `RerankingContentRetriever.java`: Cơ chế Hybrid Retrieval kết hợp Vector Search và BM25 Keyword Search, hợp nhất qua Reciprocal Rank Fusion (RRF), kèm mở rộng cửa sổ ngữ cảnh lân cận (window i±1).
  - `DocumentContextRegistry.java`: Quản lý mối quan hệ ngữ cảnh giữa các đoạn tài liệu liền kề.
  - `DeterministicFastEmbeddingModel.java`: Embedding cục bộ tốc độ cao trong bộ nhớ, không phụ thuộc mạng ngoài khi kiểm thử hoặc offline.

### 2.2 Ticket MCP Server (`MCPSERVER`)
- Cài đặt MCP server chuẩn trên Node.js dùng `@modelcontextprotocol/sdk`.
- Cung cấp 2 tool:
  - `read_ticket(ticket_id?)`: Đọc 1 ticket hoặc liệt kê toàn bộ.
  - `create_ticket(title, description, priority, assignee)`: Tự sinh mã `TCK-xxxx` và lưu vào `tickets.json`.
- Có `test-client.js` để kiểm thử handshake và gọi tool qua stdio transport.

---

## 3. Kết Quả Kiểm Thử Baseline FixLink

| Bài kiểm tra | Lệnh thực thi | Kết quả | Ghi chú |
|---|---|---|---|
| **Backend Unit & Integration Tests** | `mvnw.cmd test` | **PASS (117/117 tests)** | 100% xanh, thời gian chạy ~31.5s, `ApplyForJobConcurrencyTest` chạy ổn định |
| **Backend Checkstyle & Spotless** | `mvnw.cmd spotless:check checkstyle:check` | **PASS (0 violations)** | Tuân thủ tuyệt đối định dạng mã nguồn và quy tắc checkstyle |
| **Frontend Unit Tests** | `pnpm test` (`vitest run`) | **PASS (18/18 tests, 4 files)** | 100% xanh, thời gian chạy ~32s |
| **Frontend TypeScript & Vite Build** | `pnpm build` (`tsc --noEmit && vite build`) | **PASS** | Build thành công trong 1.69s, bundle sạch sẽ |

**Kết luận Baseline:** Hệ thống FixLink trước khi tích hợp chatbot hoàn toàn khỏe mạnh, không có lỗi tiềm ẩn.
