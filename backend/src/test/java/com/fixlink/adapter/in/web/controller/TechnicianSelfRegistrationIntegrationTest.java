package com.fixlink.adapter.in.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.adapter.in.web.dto.request.LoginRequest;
import com.fixlink.adapter.in.web.dto.request.RegisterTechnicianRequest;
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
class TechnicianSelfRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String adminToken;
    private static String technicianUserId;
    private static final String TECH_PHONE = "0966554433";
    private static final String TECH_EMAIL = "thodienlanh.selfreg@example.com";
    private static final String TECH_PASSWORD = "Password@123";

    private String getAdminToken() throws Exception {
        if (adminToken != null) return adminToken;
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("Admin@123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        adminToken = root.path("data").path("accessToken").asText();
        return adminToken;
    }

    @Test
    @Order(1)
    @DisplayName("Thợ tự đăng ký tài khoản công khai (public, không cần JWT) thành công")
    void testTechnicianSelfRegistrationPublic() throws Exception {
        RegisterTechnicianRequest req = new RegisterTechnicianRequest();
        req.setFullName("Lê Văn Thợ Mới");
        req.setEmail(TECH_EMAIL);
        req.setPhone(TECH_PHONE);
        req.setPassword(TECH_PASSWORD);
        req.setAddress("123 Đường Điện Biên Phủ, Quận Bình Thạnh, TP.HCM");
        req.setSpecialization("Điện lạnh");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register/technician")
                        // KHÔNG truyền Authorization header
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("Đăng ký thành công, vui lòng chờ admin duyệt"))
                .andExpect(jsonPath("$.data.role").value("TECHNICIAN"))
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                // Đảm bảo KHÔNG trả JWT tokens
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        technicianUserId = root.path("data").path("userId").asText();
        assertNotNull(technicianUserId);
    }

    @Test
    @Order(2)
    @DisplayName("Thợ PENDING_APPROVAL đăng nhập sẽ bị từ chối (403 Forbidden)")
    void testPendingApprovalTechnician_LoginRejected() throws Exception {
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername(TECH_PHONE);
        loginReq.setPassword(TECH_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_PENDING_APPROVAL"))
                .andExpect(jsonPath("$.message").value(containsString("chờ quản trị viên xét duyệt")));
    }

    @Test
    @Order(3)
    @DisplayName("Admin lọc danh sách người dùng theo status=PENDING_APPROVAL")
    void testAdminGetPendingApprovalUsers() throws Exception {
        String token = getAdminToken();

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "PENDING_APPROVAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[?(@.username == '" + TECH_PHONE + "')].status").value("PENDING_APPROVAL"));
    }

    @Test
    @Order(4)
    @DisplayName("Admin phê duyệt thợ: PUT /api/v1/admin/users/{id}/approve chuyển PENDING_APPROVAL -> ACTIVE")
    void testAdminApproveTechnician() throws Exception {
        String token = getAdminToken();

        mockMvc.perform(put("/api/v1/admin/users/" + technicianUserId + "/approve")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @Order(5)
    @DisplayName("Sau khi được Admin duyệt, Thợ đăng nhập thành công (200 OK)")
    void testApprovedTechnician_LoginSuccess() throws Exception {
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername(TECH_PHONE);
        loginReq.setPassword(TECH_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.user.role").value("TECHNICIAN"))
                .andExpect(jsonPath("$.data.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @Order(6)
    @DisplayName("Admin từ chối thợ: PUT /api/v1/admin/users/{id}/reject chuyển PENDING_APPROVAL -> REJECTED")
    void testAdminRejectTechnician() throws Exception {
        // Đăng ký thợ thứ 2
        RegisterTechnicianRequest req = new RegisterTechnicianRequest();
        req.setFullName("Trần Văn Từ Chối");
        req.setEmail("tuchoi.tech@example.com");
        req.setPhone("0911223344");
        req.setPassword("Password@123");
        req.setAddress("456 Lê Lợi, TP.HCM");
        req.setSpecialization("Điện tử");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register/technician")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        String rejectUserId = root.path("data").path("userId").asText();

        // Admin từ chối
        String token = getAdminToken();
        mockMvc.perform(put("/api/v1/admin/users/" + rejectUserId + "/reject")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        // Thợ bị từ chối đăng nhập -> 403 ACCOUNT_REJECTED
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("0911223344");
        loginReq.setPassword("Password@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_REJECTED"));
    }
}
