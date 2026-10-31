# TÀI LIỆU CHỈ RÕ VỊ TRÍ MÃ NGUỒN KIẾN TRÚC HEXAGONAL
## DỰ ÁN FIXLINK — HỆ THỐNG DỊCH VỤ SỬA CHỮA (SPRING BOOT 3 & REACT)

---

> [!NOTE]
> Tài liệu này liệt kê chính xác **Đường dẫn tệp (File Path)**, **Dòng code (Line numbers)** và **Đoạn code minh chứng (Code Snippets)** cho từng phân tầng trong **Kiến trúc Hexagonal (Ports & Adapters)** của dự án FixLink. Dùng để tra cứu nhanh khi bảo vệ đồ án trước Hội đồng.

---

## 1. BẢNG TỔNG HỢP VỊ TRÍ CÁC TẦNG HEXAGONAL

| Phân tầng Hexagonal | Thư mục Package | Tệp tiêu biểu | Dòng code quan trọng |
| :--- | :--- | :--- | :--- |
| **💎 DOMAIN CORE** | `com.fixlink.domain.model` | `User.java`<br>`RequestStatus.java`<br>`RepairRequest.java` | • `User.java`: Dòng 8 - 18 (POJO thuần Java)<br>• `RequestStatus.java`: Dòng 10 - 44 (State Machine) |
| **📥 INBOUND PORT** | `com.fixlink.application.port.in` | `AuthUseCase.java`<br>`RepairRequestUseCase.java` | • `AuthUseCase.java`: Dòng 9 - 23 (Login Command)<br>• `RepairRequestUseCase.java`: Dòng 31 - 50 |
| **⚙️ APPLICATION SERVICE** | `com.fixlink.application.service` | `AuthService.java`<br>`RepairRequestService.java` | • `AuthService.java`: Dòng 28 - 49 (Inject Ports)<br>• `RepairRequestService.java`: Dòng 223 - 278 |
| **📤 OUTBOUND PORT** | `com.fixlink.application.port.out` | `UserRepositoryPort.java`<br>`PasswordEncoderPort.java` | • `UserRepositoryPort.java`: Dòng 9 - 15 (Interface DB)<br>• `PasswordEncoderPort.java`: Dòng 5 - 8 |
| **🌐 INBOUND ADAPTER** | `com.fixlink.adapter.in.web` | `AuthController.java`<br>`RepairRequestController.java` | • `AuthController.java`: Dòng 26 - 47 (Chỉ inject Port)<br>• `AuthController.java`: Dòng 62 - 85 (Gọi Port) |
| **🔌 OUTBOUND ADAPTER** | `com.fixlink.adapter.out.persistence` | `UserRepositoryAdapter.java`<br>`UserMapper.java`<br>`UserJpaEntity.java` | • `UserRepositoryAdapter.java`: Dòng 29 - 46 (Implement Port)<br>• `UserMapper.java`: Dòng 46 - 74 (Ánh xạ 2 chiều)<br>• `UserJpaEntity.java`: Dòng 13 - 34 (Mapping DB SQL) |

---

## 2. CHI TIẾT VỊ TRÍ VÀ ĐOẠN CODE MINH CHỨNG TỪNG TẦNG

### 2.1. DOMAIN CORE — Trọng tâm Lục giác (Thuần Java, không Framework)
> **Đặc điểm:** Không có annotation `@Entity`, `@Table` của JPA, không có Spring `@Service` hay `@Autowired`. Hoàn toàn độc lập.

#### 📍 Tệp 1: [`backend/src/main/java/com/fixlink/domain/model/User.java`](file:///d:/đồ%20án%20spring2/backend/src/main/java/com/fixlink/domain/model/User.java#L8-L18)
```java
// Dòng 8 - 18 trong User.java: POJO thuần túy
public class User {
    private Long id;
    private String username;
    private String passwordHash;
    private Role role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private CustomerProfile customerProfile;
    private TechnicianProfile technicianProfile;
    // Constructor, Getters & Setters thuần Java
}
```

#### 📍 Tệp 2: [`backend/src/main/java/com/fixlink/domain/model/RequestStatus.java`](file:///d:/đồ%20án%20spring2/backend/src/main/java/com/fixlink/domain/model/RequestStatus.java#L10-L44)
Vòng đời trạng thái (State Machine) của đơn sửa chữa nằm tại Domain Core:
```java
// Dòng 10 - 18 trong RequestStatus.java
// DRAFT → OPEN → ASSIGNED → INSPECTING → AWAITING_COST_APPROVAL
//       → IN_PROGRESS → AWAITING_ACCEPTANCE → COMPLETED | CANCELLED
public enum RequestStatus {
    PENDING,                    // Mới tạo chờ duyệt
    DRAFT,                      // Bản nháp khách lưu chưa đăng
    OPEN,                       // Đã đăng, đang chờ thợ nhận việc (tối đa 3 ngày)
    ASSIGNED,                   // Thợ đã nhận việc
    DEPOSIT_PAID,               // Khách đã đặt cọc 30% qua QR bảo chứng
    INSPECTING,                 // Thợ đến khảo sát hiện trường
    AWAITING_COST_APPROVAL,     // Chờ khách duyệt chi phí phát sinh linh kiện
    IN_PROGRESS,                // Thợ đang thực hiện sửa chữa
    AWAITING_ACCEPTANCE,        // Chờ nghiệm thu & tất toán phần còn lại
    COMPLETED,                  // Đã nghiệm thu xong, giải ngân ví thợ
    CANCELLED                   // Đơn bị hủy
}
```

---

### 2.2. INBOUND PORT (Driving Ports — Cổng vào)
> **Đặc điểm:** Là các `Java Interface` nằm ở `application/port/in`, quy định các Use Case cho phép bên ngoài gọi vào.

#### 📍 Tệp: [`backend/src/main/java/com/fixlink/application/port/in/AuthUseCase.java`](file:///d:/đồ%20án%20spring2/backend/src/main/java/com/fixlink/application/port/in/AuthUseCase.java#L9-L23)
```java
// Dòng 9 - 23 trong AuthUseCase.java
public interface AuthUseCase {

    AuthResult login(LoginCommand command);

    CustomerRegisterResult registerCustomer(RegisterCustomerCommand command);

    AuthResult selfRegisterCustomer(SelfRegisterCustomerCommand command);

    TechnicianRegisterResult registerTechnician(RegisterTechnicianCommand command);

    // Command Record đóng gói tham số đầu vào độc lập HTTP
    record LoginCommand(String username, String password, String deviceToken, Role requestedRole) {}
}
```

---

### 2.3. APPLICATION SERVICE (Hiện thực Use Case)
> **Đặc điểm:** Nằm ở `application/service`. Triển khai Inbound Port và chỉ phụ thuộc vào Domain Model + Outbound Ports.

#### 📍 Tệp: [`backend/src/main/java/com/fixlink/application/service/AuthService.java`](file:///d:/đồ%20án%20spring2/backend/src/main/java/com/fixlink/application/service/AuthService.java#L28-L49)
```java
// Dòng 28 - 49 trong AuthService.java
@Service
public class AuthService implements AuthUseCase, RegisterTechnicianUseCase {

    // CHỈ INJECT CÁC OUTBOUND PORT INTERFACE (Không inject JPA Repository!)
    private final UserRepositoryPort userRepository;
    private final CustomerProfileRepositoryPort customerProfileRepository;
    private final TechnicianProfileRepositoryPort technicianProfileRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;
    private final LoginAttemptService loginAttemptService;

    public AuthService(UserRepositoryPort userRepository,
                       CustomerProfileRepositoryPort customerProfileRepository,
                       TechnicianProfileRepositoryPort technicianProfileRepository,
                       PasswordEncoderPort passwordEncoder,
                       TokenProviderPort tokenProvider,
                       LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.technicianProfileRepository = technicianProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.loginAttemptService = loginAttemptService;
    }
```

---

### 2.4. OUTBOUND PORT (Driven Ports — Cổng ra CSDL & Hạ tầng)
> **Đặc điểm:** Là `Java Interface` nằm ở `application/port/out`, áp dụng nguyên lý **Đảo ngược phụ thuộc (DIP)**.

#### 📍 Tệp: [`backend/src/main/java/com/fixlink/application/port/out/UserRepositoryPort.java`](file:///d:/đồ%20án%20spring2/backend/src/main/java/com/fixlink/application/port/out/UserRepositoryPort.java#L9-L15)
```java
// Dòng 9 - 15 trong UserRepositoryPort.java
public interface UserRepositoryPort {
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    User save(User user);
    UserPageResult findAll(UserQuery query);
}
```

---

### 2.5. INBOUND ADAPTER (Bộ chuyển đổi hướng vào từ Web HTTP)
> **Đặc điểm:** Nằm ở `adapter/in/web/controller`. Nhận HTTP Request, chuyển thành Command của Port rồi gọi Inbound Port. **Không bao giờ inject trực tiếp Service!**

#### 📍 Tệp: [`backend/src/main/java/com/fixlink/adapter/in/web/controller/AuthController.java`](file:///d:/đồ%20án%20spring2/backend/src/main/java/com/fixlink/adapter/in/web/controller/AuthController.java#L26-L75)
```java
// Dòng 26 - 47 trong AuthController.java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    // CHỈ INJECT INBOUND PORT INTERFACE!
    private final AuthUseCase authUseCase;
    private final RegisterTechnicianUseCase registerTechnicianUseCase;
    private final UserManagementUseCase userManagementUseCase;

    // Dòng 62 - 75: Tiếp nhận HTTP POST, gọi Inbound Port
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authUseCase.login(new LoginCommand(
                request.getUsername(),
                request.getPassword(),
                request.getDeviceToken(),
                request.getRole()
        ));
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công",
                AuthResponseDto.fromAuthResult(result)));
    }
}
```

---

### 2.6. OUTBOUND ADAPTER (Bộ chuyển đổi hướng ra CSDL)
> **Đặc điểm:** Nằm ở `adapter/out/persistence`. Triển khai Outbound Port, tương tác trực tiếp với Spring Data JPA và chuyển đổi qua lại với Domain Model.

#### 📍 Tệp: [`backend/src/main/java/com/fixlink/adapter/out/persistence/adapter/UserRepositoryAdapter.java`](file:///d:/đồ%20án%20spring2/backend/src/main/java/com/fixlink/adapter/out/persistence/adapter/UserRepositoryAdapter.java#L27-L46)
```java
// Dòng 27 - 46 trong UserRepositoryAdapter.java
@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository userRepository;       // Spring Data JPA Repo
    private final UserMapper userMapper;                         // Mapper chuyển đổi

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsernameAndDeletedAtIsNull(username)
                .map(this::enrichWithProfile); // Lấy Entity JPA -> Dùng Mapper -> Trả về Domain User
    }
}
```

#### 📍 Tệp: [`backend/src/main/java/com/fixlink/adapter/out/persistence/mapper/UserMapper.java`](file:///d:/đồ%20án%20spring2/backend/src/main/java/com/fixlink/adapter/out/persistence/mapper/UserMapper.java#L46-L74)
Cầu nối 2 chiều giữa JPA Entity và Domain Model thuần túy:
```java
// Dòng 46 - 74 trong UserMapper.java
// 1. Chuyển từ UserJpaEntity (Database) sang User (Domain Model)
public User toDomain(UserJpaEntity entity) {
    if (entity == null) return null;
    return new User(
            entity.getId(),
            entity.getUsername(),
            entity.getPasswordHash(),
            entity.getRole(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
    );
}

// 2. Chuyển từ User (Domain Model) sang UserJpaEntity (Database)
public UserJpaEntity toJpaEntity(User domain) {
    if (domain == null) return null;
    UserJpaEntity entity = new UserJpaEntity();
    entity.setId(domain.getId());
    entity.setUsername(domain.getUsername());
    entity.setPasswordHash(domain.getPasswordHash());
    entity.setRole(domain.getRole());
    entity.setStatus(domain.getStatus());
    return entity;
}
```

#### 📍 Tệp: [`backend/src/main/java/com/fixlink/adapter/out/persistence/entity/UserJpaEntity.java`](file:///d:/đồ%20án%20spring2/backend/src/main/java/com/fixlink/adapter/out/persistence/entity/UserJpaEntity.java#L13-L34)
Entity gắn chặt với bảng `users` trong cơ sở dữ liệu MySQL:
```java
// Dòng 13 - 34 trong UserJpaEntity.java
@Entity
@Table(name = "users")
public class UserJpaEntity extends BaseJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", length = 50, nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private Role role;
}
```

---

## 3. TÓM TẮT DÀNH CHO THUYẾT TRÌNH TRƯỚC HỘI ĐỒNG (1 PHÚT)

Khi Thầy Cô hỏi: *"Em hãy mở code và chỉ cho Hội đồng xem Kiến trúc Hexagonal nằm ở đâu?"*, bạn thực hiện các bước:

1. **Bước 1 — Mở Domain:** Mở `User.java` (dòng 8) và `RequestStatus.java` (dòng 15), nói:
   > *"Đây là tầng Domain Core, hoàn toàn là POJO thuần Java, không dính JPA hay Spring."*
2. **Bước 2 — Mở Cổng Port:** Mở `AuthUseCase.java` (dòng 9) và `UserRepositoryPort.java` (dòng 9), nói:
   > *"Đây là Inbound Port và Outbound Port, định nghĩa ranh giới giao tiếp bằng Interface."*
3. **Bước 3 — Mở Service điều phối:** Mở `AuthService.java` (dòng 28), nói:
   > *"Tầng Service của em implement Inbound Port và chỉ inject Outbound Port, không inject trực tiếp JPA Repository."*
4. **Bước 4 — Mở Controller & Adapter:** Mở `AuthController.java` (dòng 26) và `UserRepositoryAdapter.java` (dòng 29), nói:
   > *"Controller chỉ phụ thuộc vào Inbound Port, còn việc lưu vào MySQL do Adapter kết hợp với UserMapper đảm nhiệm."*
