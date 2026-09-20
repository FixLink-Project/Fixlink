package com.fixlink.adapter.in.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.adapter.in.web.dto.request.LoginRequest;
import com.fixlink.adapter.in.web.dto.request.RegisterCustomerRequest;
import com.fixlink.adapter.in.web.dto.request.RegisterTechnicianRequest;
import com.fixlink.adapter.in.web.dto.request.UpdateCustomerProfileRequest;
import com.fixlink.adapter.in.web.dto.request.VerifyTechnicianRequest;
import com.fixlink.domain.model.VerificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FixLinkApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.fixlink.adapter.out.persistence.repository.SpringDataRepairRequestRepository repairRequestRepository;

    @Autowired
    private com.fixlink.adapter.out.persistence.repository.SpringDataPasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private com.fixlink.adapter.out.persistence.repository.SpringDataRefreshTokenRepository refreshTokenRepository;

    @Autowired
    private com.fixlink.adapter.out.persistence.repository.SpringDataServiceCategoryRepository serviceCategoryRepository;

    @Autowired
    private com.fixlink.adapter.out.persistence.repository.SpringDataServiceAreaRepository serviceAreaRepository;

    private static String adminToken;
    private static String technicianUserId;
    private static Long rc17CustomerId;

    /** Dang nhap va tra ve gia tri header Authorization dung dinh dang Bearer. */
    private String bearerTokenOf(String username, String password) throws Exception {
        LoginRequest login = new LoginRequest();
        login.setUsername(username);
        login.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        return "Bearer " + objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    @Test
    @Order(1)
    @DisplayName("API 1: Đăng ký tài khoản Khách hàng thành công")
    void testRegisterCustomerSuccess() throws Exception {
        RegisterCustomerRequest request = new RegisterCustomerRequest();
        request.setUsername("test_customer");
        request.setPassword("Password@123");
        request.setFullName("Lê Thị Khách");
        request.setPhone("0912345678");
        request.setEmail("lethikhach@gmail.com");

        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("Đăng ký tài khoản thành công"))
                .andExpect(jsonPath("$.data.username").value("test_customer"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.fullName").value("Lê Thị Khách"));
    }

    @Test
    @Order(2)
    @DisplayName("API 2: Đăng ký tài khoản Thợ thành công với hồ sơ PENDING")
    void testRegisterTechnicianSuccess() throws Exception {
        RegisterTechnicianRequest request = new RegisterTechnicianRequest();
        request.setUsername("test_technician");
        request.setPassword("Password@123");
        request.setFullName("Phạm Văn Thợ");
        request.setPhone("0978123456");
        request.setEmail("phamvantho@gmail.com");
        request.setCitizenId("079123456789");
        request.setBio("Chuyên sửa ống nước, máy bơm");
        request.setYearsExperience(3);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register/technician")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.role").value("TECHNICIAN"))
                .andExpect(jsonPath("$.data.verificationStatus").value("PENDING"))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        technicianUserId = root.path("data").path("userId").asText();
        assertNotNull(technicianUserId);
    }

    @Test
    @Order(3)
    @DisplayName("API 2.1: Đăng ký Thợ thất bại nếu mật khẩu thiếu chữ hoa hoặc ký tự đặc biệt")
    void testRegisterTechnicianWeakPassword() throws Exception {
        RegisterTechnicianRequest request = new RegisterTechnicianRequest();
        request.setUsername("test_weak_pass");
        request.setPassword("weakpass123"); // Thiếu chữ hoa và ký tự đặc biệt
        request.setFullName("Thợ Mật Khẩu Yếu");
        request.setPhone("0978999888");
        request.setEmail("weakpass@gmail.com");
        request.setCitizenId("079999888777");

        mockMvc.perform(post("/api/v1/auth/register/technician")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @Order(4)
    @DisplayName("API 3: Đăng nhập hệ thống (Admin & Customer)")
    void testLoginSuccess() throws Exception {
        // Đăng nhập Admin
        LoginRequest adminLogin = new LoginRequest();
        adminLogin.setUsername("admin");
        adminLogin.setPassword("Admin@123");

        MvcResult adminResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"))
                .andReturn();

        JsonNode root = objectMapper.readTree(adminResult.getResponse().getContentAsString());
        adminToken = root.path("data").path("accessToken").asText();
        assertNotNull(adminToken);

        // Đăng nhập Khách hàng
        LoginRequest custLogin = new LoginRequest();
        custLogin.setUsername("test_customer");
        custLogin.setPassword("Password@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(custLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.user.fullName").value("Lê Thị Khách"));
    }

    @Test
    @Order(5)
    @DisplayName("API 3b: Đăng nhập sai mật khẩu trả về 401 INVALID_CREDENTIALS")
    void testLoginWrongPassword() throws Exception {
        LoginRequest wrongLogin = new LoginRequest();
        wrongLogin.setUsername("admin");
        wrongLogin.setPassword("SaiMatKhau123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    @Order(6)
    @DisplayName("API 4: Admin xem danh sách người dùng có phân trang và bộ lọc")
    void testAdminGetUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "1")
                        .param("limit", "10")
                        .param("role", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.meta.currentPage").value(1))
                .andExpect(jsonPath("$.meta.totalItems", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(3))));
    }

    @Test
    @Order(7)
    @DisplayName("API 5: Admin duyệt hồ sơ Thợ (KYC APPROVED)")
    void testAdminVerifyTechnician() throws Exception {
        VerifyTechnicianRequest verifyReq = new VerifyTechnicianRequest();
        verifyReq.setVerificationStatus(VerificationStatus.APPROVED);
        verifyReq.setNote("CCCD và bằng nghề hợp lệ");

        mockMvc.perform(patch("/api/v1/admin/technicians/" + technicianUserId + "/verify")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.verificationStatus").value("APPROVED"));
    }

    @Test
    @Order(8)
    @DisplayName("Thợ mới đăng nhập thấy verificationStatus PENDING và isVerified false")
    void testTechnicianLogin_VerificationStatusPending() throws Exception {
        LoginRequest techLogin = new LoginRequest();
        techLogin.setUsername("tho_dien_lanh_01"); // Thợ mẫu mặc định ở trạng thái PENDING
        techLogin.setPassword("Password@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(techLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.user.role").value("TECHNICIAN"))
                .andExpect(jsonPath("$.data.user.verificationStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.user.isVerified").value(false));
    }

    @Test
    @Order(9)
    @DisplayName("Thợ chưa duyệt (PENDING) bị từ chối nhận yêu cầu sửa chữa (403 Forbidden)")
    void testTechnicianUnverified_CannotReceiveRepairRequests() throws Exception {
        // 1. Thử bật chế độ nhận việc online
        String pendingTechToken = bearerTokenOf("tho_dien_lanh_01", "Password@123");

        mockMvc.perform(patch("/api/v1/technicians/me/status/online")
                        .header("Authorization", pendingTechToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isOnline\": true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.errorCode").value("UNVERIFIED_TECHNICIAN"));

        // 2. Thử truy cập danh sách yêu cầu sửa chữa
        mockMvc.perform(get("/api/v1/technicians/me/repair-requests")
                        .header("Authorization", pendingTechToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.errorCode").value("UNVERIFIED_TECHNICIAN"));
    }

    @Test
    @Order(10)
    @DisplayName("Thợ sau khi được Admin phê duyệt (APPROVED) được phép nhận yêu cầu sửa chữa (200 OK)")
    void testTechnicianApproved_CanReceiveRepairRequests() throws Exception {
        String approvedTechToken = bearerTokenOf("test_technician", "Password@123");

        // 1. Bật chế độ online thành công
        mockMvc.perform(patch("/api/v1/technicians/me/status/online")
                        .header("Authorization", approvedTechToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isOnline\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.isOnline").value(true));

        // 2. Lấy danh sách yêu cầu sửa chữa thành công
        mockMvc.perform(get("/api/v1/technicians/me/repair-requests")
                        .header("Authorization", approvedTechToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @Order(11)
    @DisplayName("Tài khoản bị khóa (BLOCKED) từ chối đăng nhập với mã 403 và thông báo rõ ràng")
    void testLoginBlockedAccount() throws Exception {
        LoginRequest blockedLogin = new LoginRequest();
        blockedLogin.setUsername("user_blocked");
        blockedLogin.setPassword("Password@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockedLogin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_BLOCKED"))
                .andExpect(jsonPath("$.message", containsString("bị khóa")));
    }

    @Test
    @Order(12)
    @DisplayName("Tài khoản ngừng hoạt động (INACTIVE) từ chối đăng nhập với mã 403")
    void testLoginInactiveAccount() throws Exception {
        LoginRequest inactiveLogin = new LoginRequest();
        inactiveLogin.setUsername("user_inactive");
        inactiveLogin.setPassword("Password@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inactiveLogin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_INACTIVE"))
                .andExpect(jsonPath("$.message", containsString("ngừng hoạt động")));
    }

    @Test
    @Order(13)
    @DisplayName("Đăng nhập sai trả về Generic Error, không tiết lộ tài khoản có tồn tại hay không")
    void testLoginGenericError() throws Exception {
        LoginRequest nonExistent = new LoginRequest();
        nonExistent.setUsername("tai_khoan_hoan_toan_khong_ton_tai");
        nonExistent.setPassword("MatKhauRandom@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistent)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không chính xác"));
    }

    @Test
    @Order(14)
    @DisplayName("Nhập sai mật khẩu liên tiếp đạt ngưỡng 5 lần thì tài khoản bị tạm khóa (429 Rate Limited)")
    void testLoginRateLimitingLocked() throws Exception {
        String testUser = "brute_force_victim";
        LoginRequest badLogin = new LoginRequest();
        badLogin.setUsername(testUser);
        badLogin.setPassword("SaiMatKhau@1");

        // Gửi 4 lần đầu -> 401 UNAUTHORIZED
        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badLogin)))
                    .andExpect(status().isUnauthorized());
        }

        // Lần thứ 5 -> Đạt ngưỡng 5 lần -> 429 TOO_MANY_REQUESTS
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.statusCode").value(429))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_TEMPORARILY_LOCKED"))
                .andExpect(jsonPath("$.message", containsString("quá 5 lần liên tiếp")));
    }

    @Test
    @Order(15)
    @DisplayName("Khách hàng cập nhật hồ sơ chính mình thành công và chỉ bản ghi của user đó được thay đổi")
    void testCustomerUpdateOwnProfileSuccess() throws Exception {
        // Đăng ký một khách hàng riêng cho test này
        RegisterCustomerRequest regRequest = new RegisterCustomerRequest();
        regRequest.setUsername("rc17_customer_test");
        regRequest.setPassword("Password@123");
        regRequest.setFullName("Nguyễn Văn Khách 17");
        regRequest.setPhone("0933111222");
        regRequest.setEmail("customer17@gmail.com");

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(regResult.getResponse().getContentAsString());
        String rawUserId = root.path("data").path("userId").asText();
        rc17CustomerId = Long.parseLong(rawUserId.replace("usr_", ""));

        // Cập nhật hồ sơ
        UpdateCustomerProfileRequest updateRequest = new UpdateCustomerProfileRequest();
        updateRequest.setFullName("Nguyễn Văn Khách Đã Đổi Tên");
        updateRequest.setPhone("0933999888");
        updateRequest.setEmail("customer17_new@gmail.com");
        updateRequest.setAvatarUrl("https://example.com/avatar17.jpg");

        String ownerToken = bearerTokenOf("rc17_customer_test", "Password@123");

        mockMvc.perform(put("/api/v1/customers/" + rc17CustomerId + "/profile")
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Cập nhật thông tin hồ sơ thành công"))
                .andExpect(jsonPath("$.data.fullName").value("Nguyễn Văn Khách Đã Đổi Tên"))
                .andExpect(jsonPath("$.data.phone").value("0933999888"))
                .andExpect(jsonPath("$.data.email").value("customer17_new@gmail.com"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://example.com/avatar17.jpg"));
    }

    @Test
    @Order(16)
    @DisplayName("Cố tình chỉnh sửa hồ sơ người khác (IDOR) thì bị từ chối 403 Forbidden")
    void testCustomerUpdateAnotherUserProfileForbidden() throws Exception {
        // User hiện tại là rc17CustomerId nhưng cố tình gọi sửa hồ sơ của user khác (ví dụ user ID 999999 hoặc 1)
        UpdateCustomerProfileRequest hackRequest = new UpdateCustomerProfileRequest();
        hackRequest.setFullName("Hacker Cố Tình Sửa Hồ Sơ");
        hackRequest.setPhone("0988777666");
        hackRequest.setEmail("hacker@test.com");

        Long otherUserId = 999999L;
        String attackerToken = bearerTokenOf("rc17_customer_test", "Password@123");

        mockMvc.perform(put("/api/v1/customers/" + otherUserId + "/profile")
                        .header("Authorization", attackerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hackRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message", containsString("không có quyền chỉnh sửa hồ sơ của người dùng khác")));
    }

    @Test
    @Order(17)
    @DisplayName("Gửi dữ liệu không hợp lệ hiển thị lỗi per-field validation và không lưu vào CSDL")
    void testCustomerUpdateProfileInvalidData() throws Exception {
        UpdateCustomerProfileRequest invalidRequest = new UpdateCustomerProfileRequest();
        invalidRequest.setFullName(""); // Rỗng
        invalidRequest.setPhone("123456"); // Sai format điện thoại
        invalidRequest.setEmail("email_khong_hop_le"); // Sai format email

        String token = bearerTokenOf("rc17_customer_test", "Password@123");

        mockMvc.perform(put("/api/v1/customers/" + rc17CustomerId + "/profile")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.fullName").exists())
                .andExpect(jsonPath("$.errors.phone").exists())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @Order(18)
    @DisplayName("Cập nhật hồ sơ thành công được ghi nhận đầy đủ vào hệ thống Audit Trail")
    void testCustomerUpdateProfileAuditTrailRecorded() throws Exception {
        String token = bearerTokenOf("rc17_customer_test", "Password@123");

        mockMvc.perform(get("/api/v1/customers/" + rc17CustomerId + "/audit-trail")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].action").value("UPDATE_CUSTOMER_PROFILE"))
                .andExpect(jsonPath("$.data[0].entityName").value("customer_profiles"))
                .andExpect(jsonPath("$.data[0].entityId").value(String.valueOf(rc17CustomerId)))
                .andExpect(jsonPath("$.data[0].oldValues", containsString("Nguyễn Văn Khách 17")))
                .andExpect(jsonPath("$.data[0].newValues", containsString("Nguyễn Văn Khách Đã Đổi Tên")));
    }

    @Test
    @Order(19)
    @DisplayName("Admin mở và xem chi tiết hồ sơ người dùng (GET /api/v1/admin/users/{id})")
    void testAdminGetUserDetail() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/" + rc17CustomerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value("usr_" + rc17CustomerId))
                .andExpect(jsonPath("$.data.username").value("rc17_customer_test"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"));
    }

    @Test
    @Order(20)
    @DisplayName("Admin khóa và mở khóa người dùng (PATCH /api/v1/admin/users/{id}/status)")
    void testAdminBlockAndUnblockUser() throws Exception {
        // 1. Khóa tài khoản -> BLOCKED
        java.util.Map<String, String> blockBody = java.util.Map.of("status", "BLOCKED");
        mockMvc.perform(patch("/api/v1/admin/users/" + rc17CustomerId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Cập nhật trạng thái người dùng thành công"));

        // Xác nhận người dùng đã ở trạng thái BLOCKED
        mockMvc.perform(get("/api/v1/admin/users/" + rc17CustomerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BLOCKED"));

        // 2. Mở khóa tài khoản -> ACTIVE
        java.util.Map<String, String> unblockBody = java.util.Map.of("status", "ACTIVE");
        mockMvc.perform(patch("/api/v1/admin/users/" + rc17CustomerId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unblockBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        // Xác nhận người dùng đã quay lại trạng thái ACTIVE
        mockMvc.perform(get("/api/v1/admin/users/" + rc17CustomerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @Order(21)
    @DisplayName("Kỹ thuật viên tự cập nhật hồ sơ cá nhân và tay nghề (PUT /api/v1/technicians/me/profile)")
    void testTechnicianUpdateProfileSuccess() throws Exception {
        com.fixlink.adapter.in.web.dto.request.UpdateTechnicianProfileRequest req =
                new com.fixlink.adapter.in.web.dto.request.UpdateTechnicianProfileRequest();
        req.setFullName("Trần Văn B (Đã Cập Nhật Tay Nghề)");
        req.setPhone("0987654321");
        req.setEmail("tranvanb_updated@gmail.com");
        req.setBio("Chuyên sửa máy lạnh, máy giặt, điều hòa âm trần 7 năm kinh nghiệm");
        req.setYearsExperience(7);
        req.setAvatarUrl("https://s3.fixlink.vn/avatars/tech_b_updated.jpg");

        String techToken = bearerTokenOf("tho_dien_lanh_01", "Password@123");

        mockMvc.perform(put("/api/v1/technicians/me/profile")
                        .header("Authorization", techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.fullName").value("Trần Văn B (Đã Cập Nhật Tay Nghề)"))
                .andExpect(jsonPath("$.data.yearsExperience").value(7))
                .andExpect(jsonPath("$.data.bio", containsString("7 năm kinh nghiệm")));
    }

    @Test
    @Order(22)
    @DisplayName("Flyway Master Data - Lấy danh mục dịch vụ (Service Categories) và gói dịch vụ (Services)")
    void testRC8_ServiceCategoriesMasterData() throws Exception {
        // 1. Kiểm tra API lấy danh mục dịch vụ (GET /api/v1/categories)
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data", not(empty())))
                .andExpect(jsonPath("$.data[0].code").value("DIEN_LANH"));

        // 2. Kiểm tra API lấy dịch vụ theo danh mục (GET /api/v1/services?categoryId=1)
        mockMvc.perform(get("/api/v1/services").param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data", not(empty())));
    }

    @Test
    @Order(23)
    @DisplayName("Core Entity & BaseEntity Audit Columns - Tạo yêu cầu sửa chữa (RepairRequest) với đủ 6 cột audit")
    void testRC8_RepairRequestAndAuditColumns() {
        com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity req =
                new com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity();
        req.setRequestCode("REQ-20260918-TEST");
        req.setCustomerId(2L);
        req.setCategoryId(1L);
        req.setStatus(com.fixlink.domain.model.RequestStatus.PENDING);
        req.setTitle("Sửa máy lạnh chảy nước");
        req.setDescription("Máy lạnh Daikin 1.5HP bị chảy nước ở dàn lạnh");
        req.setAddress("123 Nguyễn Thị Minh Khai, Quận 1, TP.HCM");
        req.setRequestedTime(java.time.LocalDateTime.now().plusDays(1));
        req.setAgreedPrice(new java.math.BigDecimal("200000"));

        com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity saved = repairRequestRepository.save(req);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt(), "Cột audit created_at phải tự động được gán bởi BaseEntity");
        assertNotNull(saved.getUpdatedAt(), "Cột audit updated_at phải tự động được gán bởi BaseEntity");
    }

    @Test
    @Order(24)
    @DisplayName("Đổi mật khẩu (Change Password) - Kiểm tra lỗi mật khẩu cũ sai, mật khẩu mới trùng, mật khẩu yếu và đổi thành công")
    void testRC13_ChangePassword() throws Exception {
        // Đăng nhập lấy token của test_customer
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("test_customer");
        loginReq.setPassword("Password@123");
        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();
        String customerToken = "Bearer " + objectMapper.readTree(loginRes.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // 1. Mật khẩu hiện tại sai -> 400
        com.fixlink.adapter.in.web.dto.request.ChangePasswordRequest wrongOld =
                new com.fixlink.adapter.in.web.dto.request.ChangePasswordRequest("WrongOld@123", "NewPass@456", "NewPass@456");
        mockMvc.perform(put("/api/v1/auth/change-password")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongOld)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("hiện tại không chính xác")));

        // 2. Mật khẩu mới trùng mật khẩu cũ -> 400
        com.fixlink.adapter.in.web.dto.request.ChangePasswordRequest samePass =
                new com.fixlink.adapter.in.web.dto.request.ChangePasswordRequest("Password@123", "Password@123", "Password@123");
        mockMvc.perform(put("/api/v1/auth/change-password")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(samePass)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("không được trùng")));

        // 3. Đổi mật khẩu thành công -> 200
        com.fixlink.adapter.in.web.dto.request.ChangePasswordRequest validChange =
                new com.fixlink.adapter.in.web.dto.request.ChangePasswordRequest("Password@123", "CustomerNew@456", "CustomerNew@456");
        mockMvc.perform(put("/api/v1/auth/change-password")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validChange)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        // Đổi lại về Password@123 để không ảnh hưởng các luồng khác
        LoginRequest newLogin = new LoginRequest();
        newLogin.setUsername("test_customer");
        newLogin.setPassword("CustomerNew@456");
        MvcResult newLoginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLogin)))
                .andExpect(status().isOk())
                .andReturn();
        String newCustToken = "Bearer " + objectMapper.readTree(newLoginRes.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        com.fixlink.adapter.in.web.dto.request.ChangePasswordRequest revertPass =
                new com.fixlink.adapter.in.web.dto.request.ChangePasswordRequest("CustomerNew@456", "Password@123", "Password@123");
        mockMvc.perform(put("/api/v1/auth/change-password")
                        .header("Authorization", newCustToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revertPass)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(25)
    @DisplayName("Quên và đặt lại mật khẩu (Forgot/Reset Password) - Anti-enumeration & Token validation")
    void testRC14_ForgotAndResetPassword() throws Exception {
        // 1. Quên mật khẩu với email không tồn tại -> Vẫn trả về 200 (Anti-enumeration)
        com.fixlink.adapter.in.web.dto.request.ForgotPasswordRequest nonExistReq =
                new com.fixlink.adapter.in.web.dto.request.ForgotPasswordRequest("nonexistent@domain.com");
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        // 2. Quên mật khẩu với email hợp lệ của khách hàng
        com.fixlink.adapter.in.web.dto.request.ForgotPasswordRequest validForgot =
                new com.fixlink.adapter.in.web.dto.request.ForgotPasswordRequest("lethikhach@gmail.com");
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validForgot)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        // Lấy token vừa sinh ra trong DB
        var tokens = passwordResetTokenRepository.findAll();
        var tokenEntity = tokens.stream()
                .filter(t -> "lethikhach@gmail.com".equalsIgnoreCase(t.getEmail()))
                .reduce((first, second) -> second)
                .orElseThrow();
        String validToken = tokenEntity.getToken();

        // 3. Reset với xác nhận mật khẩu không khớp -> 400
        com.fixlink.adapter.in.web.dto.request.ResetPasswordRequest mismatchReq =
                new com.fixlink.adapter.in.web.dto.request.ResetPasswordRequest(validToken, "NewPass@123", "Mismatch@456");
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mismatchReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("không khớp")));

        // 4. Reset với token giả -> 400
        com.fixlink.adapter.in.web.dto.request.ResetPasswordRequest fakeTokenReq =
                new com.fixlink.adapter.in.web.dto.request.ResetPasswordRequest("fake-token-uuid", "NewPass@123", "NewPass@123");
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fakeTokenReq)))
                .andExpect(status().isBadRequest());

        // 5. Reset thành công với token hợp lệ và mật khẩu mới -> 200
        com.fixlink.adapter.in.web.dto.request.ResetPasswordRequest successReq =
                new com.fixlink.adapter.in.web.dto.request.ResetPasswordRequest(validToken, "ResetPass@999", "ResetPass@999");
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(successReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        // 6. Thử tái sử dụng token đã dùng -> 400
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(successReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(26)
    @DisplayName("Refresh Token Rotation và Đăng xuất Blacklist Token")
    void testRC15_RefreshTokenAndLogout() throws Exception {
        // 1. Đăng nhập Admin lấy accessToken và refreshToken
        LoginRequest adminLogin = new LoginRequest();
        adminLogin.setUsername("admin");
        adminLogin.setPassword("Admin@123");
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(res.getResponse().getContentAsString()).path("data");
        String accessJwt = data.path("accessToken").asText();
        String refreshJwt = data.path("refreshToken").asText();

        // 2. Refresh Token Rotation
        com.fixlink.adapter.in.web.dto.request.RefreshTokenRequest refreshReq =
                new com.fixlink.adapter.in.web.dto.request.RefreshTokenRequest(refreshJwt);
        MvcResult rotateRes = mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andReturn();

        JsonNode rotateData = objectMapper.readTree(rotateRes.getResponse().getContentAsString()).path("data");
        String newAccessJwt = rotateData.path("accessToken").asText();
        String newRefreshJwt = rotateData.path("refreshToken").asText();

        // 3. Đăng xuất và kiểm tra Blacklist
        com.fixlink.adapter.in.web.dto.request.LogoutRequest logoutReq =
                new com.fixlink.adapter.in.web.dto.request.LogoutRequest(newRefreshJwt);
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + newAccessJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("thành công")));

        // 4. Dùng token đã bị blacklist để truy cập endpoint bảo mật -> 401 Unauthorized
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + newAccessJwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(27)
    @DisplayName("Kỹ thuật viên xem hồ sơ, cập nhật danh mục dịch vụ và khu vực hoạt động")
    void testRC18_TechnicianProfileAndAreas() throws Exception {
        // Xem hồ sơ kỹ thuật viên
        String techToken = bearerTokenOf("tho_dien_lanh_01", "Password@123");

        mockMvc.perform(get("/api/v1/technicians/me/profile")
                        .header("Authorization", techToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.userId").value(3))
                .andExpect(jsonPath("$.data.categories").isArray())
                .andExpect(jsonPath("$.data.areas").isArray());

        // Cập nhật hồ sơ kèm chuyên môn và khu vực phục vụ
        com.fixlink.adapter.in.web.dto.request.UpdateTechnicianProfileRequest updateReq =
                com.fixlink.adapter.in.web.dto.request.UpdateTechnicianProfileRequest.builder()
                        .fullName("Trần Văn B (Thợ Điện Lạnh Chuyên Nghiệp)")
                        .phone("0987654321")
                        .email("tech_b_rc18@fixlink.vn")
                        .bio("Chuyên sửa chữa điện lạnh, máy giặt, điều hòa dân dụng 8 năm kinh nghiệm")
                        .yearsExperience(8)
                        .avatarUrl("https://s3.fixlink.vn/avatars/tech_b_pro.jpg")
                        .categoryIds(java.util.List.of(1L))
                        .areaIds(java.util.List.of(1L, 2L))
                        .build();

        mockMvc.perform(put("/api/v1/technicians/me/profile")
                        .header("Authorization", techToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.fullName").value("Trần Văn B (Thợ Điện Lạnh Chuyên Nghiệp)"))
                .andExpect(jsonPath("$.data.yearsExperience").value(8))
                .andExpect(jsonPath("$.data.categories[0].id").value(1))
                .andExpect(jsonPath("$.data.areas", hasSize(2)));

        // Đọc lại từ máy chủ: trước đây service chỉ dựng response từ chính request
        // rồi bỏ đi, nên chỉ kiểm tra response của PUT là không đủ để biết đã lưu hay chưa.
        mockMvc.perform(get("/api/v1/technicians/me/profile")
                        .header("Authorization", techToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories", hasSize(1)))
                .andExpect(jsonPath("$.data.categories[0].id").value(1))
                .andExpect(jsonPath("$.data.areas", hasSize(2)))
                .andExpect(jsonPath("$.data.areas[*].id", containsInAnyOrder(1, 2)));
    }

    @Test
    @Order(28)
    @DisplayName("Quản trị danh mục dịch vụ Admin CRUD & Đánh giá tác động (Impact Assessment)")
    void testRC23_AdminCategoryCRUDAndImpact() throws Exception {
        // 1. Đăng nhập Admin lấy token
        LoginRequest adminLogin = new LoginRequest();
        adminLogin.setUsername("admin");
        adminLogin.setPassword("Admin@123");
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();
        String currentAdminToken = "Bearer " + objectMapper.readTree(res.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // 2. Lấy danh sách danh mục Admin
        mockMvc.perform(get("/api/v1/admin/categories")
                        .header("Authorization", currentAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data", not(empty())));

        // 3. Tạo mới danh mục dịch vụ
        com.fixlink.adapter.in.web.dto.request.CategoryRequest newCat =
                com.fixlink.adapter.in.web.dto.request.CategoryRequest.builder()
                        .code("DIEN_TU_AM_THANH")
                        .name("Điện Tử Âm Thanh Gia Đình")
                        .description("Sửa loa, tivi, âm ly số")
                        .iconUrl("https://s3.fixlink.vn/icons/sound.png")
                        .displayOrder(10)
                        .isActive(true)
                        .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", currentAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCat)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.code").value("DIEN_TU_AM_THANH"))
                .andReturn();

        Long createdCatId = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asLong();

        // 4. Đánh giá tác động (Impact Assessment)
        mockMvc.perform(get("/api/v1/admin/categories/" + createdCatId + "/impact")
                        .header("Authorization", currentAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.canDeleteDirectly").value(true));

        // 5. Cập nhật danh mục
        newCat.setName("Điện Tử Âm Thanh & Nhạc Cụ");
        mockMvc.perform(put("/api/v1/admin/categories/" + createdCatId)
                        .header("Authorization", currentAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCat)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Điện Tử Âm Thanh & Nhạc Cụ"));

        // 6. Xóa danh mục
        mockMvc.perform(delete("/api/v1/admin/categories/" + createdCatId)
                        .header("Authorization", currentAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));
    }

    @Test
    @Order(29)
    @DisplayName("Phân quyền: không token trả 401, sai vai trò trả 403, đúng vai trò trả 200")
    void testAdminEndpointRequiresAdminRole() throws Exception {
        // 1. Không gửi token -> 401 Unauthorized
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));

        // 2. Token của khách hàng -> 403 Forbidden
        String customerToken = bearerTokenOf("customer01", "Password@123");
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));

        // 3. Token quản trị viên -> 200 OK
        String adminBearer = bearerTokenOf("admin", "Admin@123");
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));
    }

    @Test
    @Order(30)
    @DisplayName("Phân quyền: khách hàng không vào được API của thợ và ngược lại")
    void testRoleSeparationBetweenCustomerAndTechnician() throws Exception {
        String customerToken = bearerTokenOf("customer01", "Password@123");
        String technicianToken = bearerTokenOf("tho_dien_lanh_01", "Password@123");

        // Khách hàng gọi API dành cho thợ -> 403
        mockMvc.perform(get("/api/v1/technicians/me/profile")
                        .header("Authorization", customerToken))
                .andExpect(status().isForbidden());

        // Thợ gọi API hồ sơ khách hàng -> 403
        mockMvc.perform(get("/api/v1/customers/2/profile")
                        .header("Authorization", technicianToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(31)
    @DisplayName("Các endpoint công khai vẫn truy cập được khi chưa đăng nhập")
    void testPublicEndpointsStayReachable() throws Exception {
        mockMvc.perform(get("/api/v1/categories")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/services")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/areas")).andExpect(status().isOk());
    }

    @Test
    @Order(32)
    @DisplayName("IDOR: khách hàng xem hồ sơ của khách hàng khác bị từ chối 403")
    void testCustomerCannotReadAnotherCustomerProfile() throws Exception {
        // Tạo thêm một khách hàng thứ hai để làm mục tiêu
        RegisterCustomerRequest victim = new RegisterCustomerRequest();
        victim.setUsername("customer02");
        victim.setPassword("Password@123");
        victim.setFullName("Trần Thị Nạn Nhân");
        victim.setPhone("0944555666");
        victim.setEmail("customer02@gmail.com");

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(victim)))
                .andExpect(status().isCreated())
                .andReturn();

        Long victimId = Long.parseLong(objectMapper.readTree(regResult.getResponse().getContentAsString())
                .path("data").path("userId").asText().replace("usr_", ""));

        String attackerToken = bearerTokenOf("customer01", "Password@123");

        // 1. Xem hồ sơ người khác -> 403
        mockMvc.perform(get("/api/v1/customers/" + victimId + "/profile")
                        .header("Authorization", attackerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));

        // 2. Xem nhật ký kiểm toán người khác -> 403
        mockMvc.perform(get("/api/v1/customers/" + victimId + "/audit-trail")
                        .header("Authorization", attackerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));

        // 3. Nạn nhân tự xem hồ sơ của mình -> 200
        String victimToken = bearerTokenOf("customer02", "Password@123");
        mockMvc.perform(get("/api/v1/customers/" + victimId + "/profile")
                        .header("Authorization", victimToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Trần Thị Nạn Nhân"));

        // 4. Quản trị viên xem được hồ sơ bất kỳ để quản lý -> 200
        String adminBearer = bearerTokenOf("admin", "Admin@123");
        mockMvc.perform(get("/api/v1/customers/" + victimId + "/profile")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Trần Thị Nạn Nhân"));

        mockMvc.perform(get("/api/v1/customers/" + victimId + "/audit-trail")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk());
    }

    @Test
    @Order(33)
    @DisplayName("Phân trang: khối meta đủ sáu trường và hasNext/hasPrevious đúng ở từng trang")
    void testPaginationMetaShape() throws Exception {
        String adminBearer = bearerTokenOf("admin", "Admin@123");

        // Trang đầu với limit 2: còn trang sau, không có trang trước
        mockMvc.perform(get("/api/v1/admin/users?page=1&limit=2")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.currentPage").value(1))
                .andExpect(jsonPath("$.meta.limit").value(2))
                .andExpect(jsonPath("$.meta.totalItems").isNumber())
                .andExpect(jsonPath("$.meta.totalPages").isNumber())
                .andExpect(jsonPath("$.meta.hasPrevious").value(false))
                .andExpect(jsonPath("$.meta.hasNext").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)));

        // Trang hai: có trang trước
        mockMvc.perform(get("/api/v1/admin/users?page=2&limit=2")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.currentPage").value(2))
                .andExpect(jsonPath("$.meta.hasPrevious").value(true));

        // Limit đủ lớn để gói hết trong một trang: không còn trang nào khác
        mockMvc.perform(get("/api/v1/admin/users?page=1&limit=100")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.totalPages").value(1))
                .andExpect(jsonPath("$.meta.hasNext").value(false))
                .andExpect(jsonPath("$.meta.hasPrevious").value(false));
    }

    @Test
    @Order(34)
    @DisplayName("Phân trang: danh sách danh mục của quản trị viên trả kèm meta và lọc được theo tên")
    void testCategoryListPagination() throws Exception {
        String adminBearer = bearerTokenOf("admin", "Admin@123");

        mockMvc.perform(get("/api/v1/admin/categories?page=1&limit=2")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.currentPage").value(1))
                .andExpect(jsonPath("$.meta.limit").value(2))
                .andExpect(jsonPath("$.meta.hasPrevious").value(false))
                .andExpect(jsonPath("$.data", hasSize(2)));

        // Tìm kiếm theo tên thu hẹp kết quả và meta tính lại theo đó
        mockMvc.perform(get("/api/v1/admin/categories?page=1&limit=10&search=Điện Lạnh")
                        .header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.totalItems").value(1))
                .andExpect(jsonPath("$.meta.totalPages").value(1))
                .andExpect(jsonPath("$.meta.hasNext").value(false))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }
}
