# BÁO CÁO THIẾT KẾ KIẾN TRÚC HEXAGONAL (PORTS & ADAPTERS)
## HỆ THỐNG NỀN TẢNG KẾT NỐI DỊCH VỤ SỬA CHỮA FIXLINK

---

## 1. TỔNG QUAN KIẾN TRÚC HEXAGONAL (PORTS & ADAPTERS)

Kiến trúc Hexagonal (hay còn gọi là **Kiến trúc Cổng và Bộ thích ứng - Ports & Adapters**) được khởi xướng bởi Alistair Cockburn (2005) với mục tiêu tối thượng:
> **"Tách biệt hoàn toàn logic nghiệp vụ cốt lõi (Domain Core & Application) khỏi các chi tiết công nghệ bên ngoài (Spring Framework, HTTP Controllers, Database, Third-party APIs)."**

### 1.1. Nguyên tắc Bất biến (Inward Dependency Rule)
- Mọi sự phụ thuộc **chỉ được phép hướng từ ngoài vào trong**.
- Lớp bên trong **không được phép biết hoặc import** bất kỳ class nào của lớp bên ngoài.
- Tầng **Domain Core** là trung tâm bất biến, hoàn toàn là Java thuần (POJO), không dính dáng đến JPA/Hibernate, Spring Boot annotations, hay HTTP Web.

---

## 2. SƠ ĐỒ KHỐI KIẾN TRÚC LỤC GIÁC TRONG FIXLINK

```mermaid
flowchart TB
    subgraph OUTSIDE ["🌍 THẾ GIỚI BÊN NGOÀI"]
        UI["💻 React Frontend (Vite / Tailwind / Axios)"]
        DB[("🗄️ MySQL Database / PostgreSQL")]
        EXT["💳 VietQR Gateway / Firebase Storage / Email"]
    end

    subgraph INBOUND_ADAPTER ["1️⃣ INBOUND ADAPTER (Primary / Driving Adapters)"]
        direction TB
        C_AUTH["AuthController"]
        C_REQ["RepairRequestController"]
        C_QUOTE["QuotationController"]
        DTO["DTO Request / Response Validation"]
    end

    subgraph APPLICATION_LAYER ["2️⃣ APPLICATION CORE (Use Cases & Ports)"]
        direction TB
        subgraph INBOUND_PORTS ["📥 Inbound Ports (Driving Ports)"]
            P_AUTH["AuthUseCase"]
            P_REQ["RepairRequestUseCase"]
            P_QUOTE["QuotationUseCase"]
        end

        subgraph SERVICES ["⚙️ Application Services"]
            S_AUTH["AuthService"]
            S_REQ["RepairRequestService"]
            S_QUOTE["QuotationService"]
        end

        subgraph OUTBOUND_PORTS ["📤 Outbound Ports (Driven Ports)"]
            P_USER_REPO["UserRepositoryPort"]
            P_ENCODER["PasswordEncoderPort"]
            P_TOKEN["TokenProviderPort"]
            P_TECH_REPO["TechnicianProfileRepositoryPort"]
        end
    end

    subgraph DOMAIN_CORE ["💎 3️⃣ DOMAIN CORE (Trọng tâm Lục giác - Thuần Java)"]
        direction TB
        M_USER["User"]
        M_REQ["RepairRequest"]
        M_STATUS["RequestStatus (State Machine)"]
        M_QUOTE["Quotation"]
        EXC["DomainException / ValidationFailedException"]
    end

    subgraph OUTBOUND_ADAPTER ["4️⃣ OUTBOUND ADAPTER (Secondary / Driven Adapters)"]
        direction TB
        subgraph PERSISTENCE ["📂 adapter.out.persistence"]
            A_USER["UserRepositoryAdapter"]
            A_PASS["PasswordEncoderAdapter"]
            MAPPER["UserMapper / RepairRequestMapper"]
            JPA_REPO["SpringDataUserRepository"]
            JPA_ENTITY["UserJpaEntity / RepairRequestJpaEntity"]
        end
    end

    %% Flow connections
    UI -->|HTTP REST JSON| INBOUND_ADAPTER
    INBOUND_ADAPTER -->|Gọi qua Interface / Command Record| INBOUND_PORTS
    INBOUND_PORTS -.->|implements| SERVICES
    SERVICES -->|Thao tác nghiệp vụ| DOMAIN_CORE
    SERVICES -->|Đòi hỏi dữ liệu qua Port| OUTBOUND_PORTS
    A_USER -.->|implements| P_USER_REPO
    A_PASS -.->|implements| P_ENCODER
    A_USER --> JPA_REPO
    JPA_REPO --> JPA_ENTITY
    A_USER --> MAPPER
    MAPPER -.->|Ánh xạ 2 chiều| DOMAIN_CORE
    JPA_REPO -->|SQL Query| DB
    SERVICES -.-> EXT
```

---

## 3. BẢN ĐỒ ÁNH XẠ CODE TRONG SOURCE CODE FIXLINK

Dưới đây là bảng đối chiếu chi tiết giữa **Khái niệm Hexagonal** và **Đường dẫn tệp mã nguồn cụ thể** trong thư mục `backend/src/main/java/com/fixlink`:

| Phân vùng Hexagonal | Vai trò & Trách nhiệm | Tệp mã nguồn cụ thể trong dự án |
| :--- | :--- | :--- |
| **💎 DOMAIN CORE** | Thực thể và quy tắc nghiệp vụ thuần túy. Không có Spring, không có JPA. | • `domain/model/User.java`<br>• `domain/model/RepairRequest.java`<br>• `domain/model/RequestStatus.java` *(State Machine)*<br>• `domain/model/Quotation.java`<br>• `domain/exception/DomainException.java` |
| **📥 INBOUND PORT** | Interface định nghĩa các Use Case mà bên ngoài có thể gọi vào hệ thống. | • `application/port/in/AuthUseCase.java`<br>• `application/port/in/RepairRequestUseCase.java`<br>• `application/port/in/QuotationUseCase.java`<br>• `application/port/in/TechnicianProfileUseCase.java` |
| **⚙️ APPLICATION SERVICE** | Điều phối nghiệp vụ, thực thi Use Case, quản lý `@Transactional`. | • `application/service/AuthService.java`<br>• `application/service/RepairRequestService.java`<br>• `application/service/QuotationService.java`<br>• `application/service/PaymentService.java` |
| **📤 OUTBOUND PORT** | Interface quy định dữ liệu và dịch vụ hạ tầng mà Application cần từ bên ngoài. | • `application/port/out/UserRepositoryPort.java`<br>• `application/port/out/CustomerProfileRepositoryPort.java`<br>• `application/port/out/TechnicianProfileRepositoryPort.java`<br>• `application/port/out/PasswordEncoderPort.java`<br>• `application/port/out/TokenProviderPort.java` |
| **🌐 INBOUND ADAPTER** | REST Controllers tiếp nhận HTTP request từ Client, gọi Inbound Port. | • `adapter/in/web/controller/AuthController.java`<br>• `adapter/in/web/controller/RepairRequestController.java`<br>• `adapter/in/web/controller/QuotationController.java`<br>• `adapter/in/web/dto/*` *(Request / Response DTOs)* |
| **🔌 OUTBOUND ADAPTER** | Triển khai các Outbound Port, giao tiếp với CSDL qua Spring Data JPA. | • `adapter/out/persistence/adapter/UserRepositoryAdapter.java`<br>• `adapter/out/persistence/adapter/PasswordEncoderAdapter.java`<br>• `adapter/out/persistence/entity/*JpaEntity.java`<br>• `adapter/out/persistence/mapper/*Mapper.java`<br>• `adapter/out/persistence/repository/SpringData*Repository.java` |
| **🛡️ INFRASTRUCTURE** | Bộ lọc an ninh, cấu hình hệ thống, quản lý ngoại lệ toàn cục. | • `infrastructure/security/JwtAuthenticationFilter.java`<br>• `infrastructure/config/SecurityConfig.java`<br>• `infrastructure/exception/GlobalExceptionHandler.java` |

---

## 4. SƠ ĐỒ TUẦN TỰ (SEQUENCE DIAGRAM) LUỒNG THỰC THI END-TO-END

### Luồng 1: Xác thực người dùng (Login Flow)

```mermaid
sequenceDiagram
    autonumber
    actor Client as 💻 React Frontend
    participant Ctrl as 🌐 AuthController<br/>(Inbound Adapter)
    participant PortIn as 📥 AuthUseCase<br/>(Inbound Port)
    participant Svc as ⚙️ AuthService<br/>(Application Core)
    participant PortOutUser as 📤 UserRepositoryPort<br/>(Outbound Port)
    participant UserAdapter as 🔌 UserRepositoryAdapter<br/>(Outbound Adapter)
    participant Mapper as 🔄 UserMapper
    participant JpaRepo as 🗄️ SpringDataUserRepo
    participant DB as 💾 MySQL Database
    participant PortOutPass as 📤 PasswordEncoderPort

    Client->>Ctrl: POST /api/v1/auth/login { username, password }
    Note over Ctrl: Chuyển DTO thành LoginCommand
    Ctrl->>PortIn: login(command)
    PortIn->>Svc: login(command)
    
    Note over Svc: Gọi Outbound Port để tìm User
    Svc->>PortOutUser: findByUsername(username)
    PortOutUser->>UserAdapter: findByUsername(username)
    UserAdapter->>JpaRepo: findByUsernameAndDeletedAtIsNull(username)
    JpaRepo->>DB: SELECT * FROM users WHERE username = ?
    DB-->>JpaRepo: Trả về UserJpaEntity
    JpaRepo-->>UserAdapter: UserJpaEntity
    UserAdapter->>Mapper: toDomain(entity)
    Mapper-->>UserAdapter: User (Pure Domain Model)
    UserAdapter-->>Svc: Optional<User>

    Note over Svc: Kiểm tra mật khẩu qua Outbound Port
    Svc->>PortOutPass: matches(rawPassword, encodedPassword)
    PortOutPass-->>Svc: true / false

    Note over Svc: Sinh JWT Token & đóng gói AuthResult
    Svc-->>PortIn: AuthResult(token, user)
    PortIn-->>Ctrl: AuthResult
    Ctrl-->>Client: HTTP 200 OK { token, userResponse }
```

---

### Luồng 2: Tạo đơn sửa chữa & Đặt cọc 30% (Repair Request & Escrow Deposit)

```mermaid
sequenceDiagram
    autonumber
    actor Customer as 👤 Khách hàng (Client)
    participant Ctrl as 🌐 RepairRequestController
    participant UseCase as 📥 RepairRequestUseCase
    participant Svc as ⚙️ RepairRequestService
    participant Domain as 💎 RepairRequest & RequestStatus
    participant PaymentSvc as 💳 PaymentService
    participant VietQR as 📲 VietQR Service
    participant Repo as 🗄️ Persistence Layer

    Customer->>Ctrl: POST /api/v1/repair-requests (Title, Category, Budget, 6 Files)
    Ctrl->>UseCase: createRequest(CreateRepairRequestCommand)
    UseCase->>Svc: createRequest(command)

    Note over Svc: 1. Thẩm tra tài khoản ACTIVE<br/>2. Hạn nhận việc cố định 3 ngày<br/>3. Khởi tạo trạng thái OPEN
    Svc->>Domain: Khởi tạo RepairRequest (status = OPEN)
    Svc->>Repo: Lưu RepairRequestJpaEntity & Media
    Repo-->>Svc: Saved Entity
    Svc-->>Ctrl: RepairRequestDetail
    Ctrl-->>Customer: HTTP 201 Created (REQ-xxxx)

    Note over Customer: Thợ nhận việc -> Trạng thái sang ASSIGNED
    Customer->>PaymentSvc: Yêu cầu đặt cọc 30% bảo chứng
    PaymentSvc->>VietQR: Sinh mã VietQR chuyển khoản
    VietQR-->>Customer: Hiển thị mã QR thanh toán 30%
    Customer->>PaymentSvc: Xác nhận thanh toán cọc thành công
    PaymentSvc->>Svc: Chuyển trạng thái -> DEPOSIT_PAID
    Note over Svc: Tiền cọc được khóa bảo chứng trong Sàn,<br/>Thợ được phép tiến hành Khảo sát (INSPECTING)
```

---

## 5. BA NGUYÊN TẮC VÀNG CHỐNG "ĐỨT LUỒNG" VÀ "NUỐT DỮ LIỆU" TRONG DỰ ÁN

1. **State Machine duy nhất (Single Source of Truth):**
   - Vòng đời trạng thái được bảo vệ tập trung tại `domain/model/RequestStatus.java`.
   - Mọi chuyển đổi trạng thái phải qua `RepairRequestService.updateStatus()`, chặn tuyệt đối tình trạng nhảy cóc trạng thái hoặc thao tác ngoài quyền hạn.
2. **Tính bất biến của dòng tiền (Financial Immutability):**
   - Tiền cọc 30% ban đầu sau khi thanh toán được hệ thống ghi nhận là bất biến.
   - Khi nghiệm thu tiền mặt, sàn khấu trừ 10% hoa hồng trực tiếp từ khoản cọc 30% đang giữ, và **cộng dồn 20% còn lại vào Ví FixLink của Thợ**, giúp thợ không bị khấu trừ âm và nhận trọn vẹn 90% giá trị hợp đồng.
3. **Ánh xạ 2 chiều chặt chẽ (Entity $\leftrightarrow$ Domain Model):**
   - Không để rò rỉ JPA Entity ra Controller và không nhận DTO trực tiếp vào Domain.
   - Mọi trường dữ liệu đều được ánh xạ qua các Mapper chuyên biệt (`UserMapper`, `RepairRequestMapper`), loại bỏ hoàn toàn hiện tượng "nuốt dữ liệu" (Data Swallowing).

---

## 6. KỊCH BẢN MẪU THUYẾT TRÌNH VỀ KIẾN TRÚC HEXAGONAL (DÀNH CHO BẢO VỆ ĐỒ ÁN)

> **Gợi ý thuyết trình 2 phút trước Hội đồng:**
> 
> *"Kính thưa Thầy/Cô trong Hội đồng, dự án FixLink được xây dựng hoàn toàn dựa trên **Kiến trúc Hexagonal (Ports & Adapters)**.*
>
> *Điểm mấu chốt của kiến trúc này trong hệ thống của nhóm em thể hiện ở 3 điểm:*
> 1. ***Lõi Domain hoàn toàn độc lập:** Trong package `com.fixlink.domain`, các model như `User`, `RepairRequest` và `RequestStatus` là các Java class thuần túy, không hề chứa bất kỳ annotation nào của JPA hay Spring. Điều này giúp toàn bộ quy tắc nghiệp vụ cốt lõi không bao giờ bị phụ thuộc vào công nghệ bên ngoài.*
> 2. ***Giao tiếp qua Cổng (Ports):** Tầng Application điều phối thông qua 2 loại cổng:
>    - **Inbound Ports (`port.in`):** Như `AuthUseCase`, `RepairRequestUseCase`, đóng vai trò là giao kèo để `adapter.in.web` (các REST Controllers) gọi vào. Controller của tụi em chỉ inject interface Port chứ không hề phụ thuộc trực tiếp vào Service implementation.
>    - **Outbound Ports (`port.out`):** Như `UserRepositoryPort`, `PasswordEncoderPort`, đảo ngược sự phụ thuộc (DIP). Tầng Service chỉ yêu cầu interface, còn việc lưu xuống MySQL qua JPA hay mã hóa mật khẩu bằng BCrypt sẽ do `adapter.out.persistence` đảm nhiệm.*
> 3. ***Khả năng mở rộng và Kiểm thử:** Nhờ cách ly bằng Ports, nhóm em đã viết được hơn **190 Unit Tests** chạy độc lập mà không cần khởi động CSDL hay Spring Context nặng nề. Sau này nếu hệ thống muốn thay đổi CSDL sang NoSQL hay đổi nhà cung cấp thanh toán, tầng nghiệp vụ lõi được bảo toàn 100% không cần chỉnh sửa."*
