package com.fixlink.adapter.in.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.adapter.in.web.dto.request.LoginRequest;
import com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity;
import com.fixlink.adapter.out.persistence.entity.TechnicianProfileJpaEntity;
import com.fixlink.adapter.out.persistence.entity.UserJpaEntity;
import com.fixlink.adapter.out.persistence.repository.SpringDataRepairRequestRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataTechnicianProfileRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataUserRepository;
import com.fixlink.domain.model.RequestStatus;
import org.junit.jupiter.api.BeforeEach;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử tích hợp tự động cho Jira RC-38:
 * Technician Search Requests by Service Area (Tìm kiếm yêu cầu sửa chữa theo khu vực hoạt động).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RepairRequestAreaSearchIntegrationTest {

    private static final String TECH_APPROVED = "tech01";
    private static final String TECH_PENDING = "tho_cho_duyet_01";
    private static final String CUSTOMER = "customer01";
    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataRepairRequestRepository repairRequestRepo;

    @Autowired
    private SpringDataUserRepository userRepo;

    @Autowired
    private SpringDataTechnicianProfileRepository techProfileRepo;

    private String techToken;
    private String pendingTechToken;
    private String customerToken;

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        assertNotNull(token, "Đăng nhập phải trả về accessToken hợp lệ");
        return token;
    }

    @BeforeEach
    void setUp() throws Exception {
        techToken = loginAndGetToken(TECH_APPROVED, PASSWORD);
        pendingTechToken = loginAndGetToken(TECH_PENDING, PASSWORD);
        customerToken = loginAndGetToken(CUSTOMER, PASSWORD);

        // Đảm bảo tech01 có category [1, 2, 3] và area [1, 2, 3]
        UserJpaEntity techUser = userRepo.findByUsernameAndDeletedAtIsNull(TECH_APPROVED).orElseThrow();
        TechnicianProfileJpaEntity profile = techProfileRepo.findById(techUser.getId()).orElseThrow();
        profile.setCategoryIds(new LinkedHashSet<>(List.of(1L, 2L, 3L)));
        profile.setAreaIds(new LinkedHashSet<>(List.of(1L, 2L, 3L)));
        techProfileRepo.save(profile);

        // Chuẩn bị các yêu cầu mở thầu BIDDING_OPEN tại các khu vực khác nhau
        UserJpaEntity custUser = userRepo.findByUsernameAndDeletedAtIsNull(CUSTOMER).orElseThrow();

        if (repairRequestRepo.findByRequestCode("REQ-TEST-RC38-Q1").isEmpty()) {
            RepairRequestJpaEntity reqQ1 = new RepairRequestJpaEntity();
            reqQ1.setRequestCode("REQ-TEST-RC38-Q1");
            reqQ1.setCustomerId(custUser.getId());
            reqQ1.setCategoryId(1L);
            reqQ1.setAreaId(1L); // Quận 1
            reqQ1.setTitle("Sửa máy giặt Electrolux kêu rung tại Quận 1");
            reqQ1.setDescription("Máy giặt vắt kêu to tại Nguyễn Huệ Quận 1");
            reqQ1.setAddress("12 Lê Lợi, Quận 1, TP.HCM");
            reqQ1.setRequestedTime(LocalDateTime.now().plusDays(2));
            reqQ1.setStatus(RequestStatus.BIDDING_OPEN);
            reqQ1.setBiddingDeadline(LocalDateTime.now().plusDays(5));
            reqQ1.setBudgetRef(new BigDecimal("600000"));
            repairRequestRepo.save(reqQ1);
        }

        if (repairRequestRepo.findByRequestCode("REQ-TEST-RC38-Q7").isEmpty()) {
            RepairRequestJpaEntity reqQ7 = new RepairRequestJpaEntity();
            reqQ7.setRequestCode("REQ-TEST-RC38-Q7");
            reqQ7.setCustomerId(custUser.getId());
            reqQ7.setCategoryId(2L);
            reqQ7.setAreaId(2L); // Quận 7
            reqQ7.setTitle("Sửa đường ống nước rò rỉ tại Quận 7");
            reqQ7.setDescription("Ống nước âm tường rò rỉ tại Phú Mỹ Hưng Quận 7");
            reqQ7.setAddress("88 Nguyễn Thị Thập, Quận 7, TP.HCM");
            reqQ7.setRequestedTime(LocalDateTime.now().plusDays(2));
            reqQ7.setStatus(RequestStatus.BIDDING_OPEN);
            reqQ7.setBiddingDeadline(LocalDateTime.now().plusDays(5));
            reqQ7.setBudgetRef(new BigDecimal("400000"));
            repairRequestRepo.save(reqQ7);
        }
    }

    @Test
    @Order(1)
    @DisplayName("RC-38 AC 1: Thợ tìm kiếm theo khu vực cụ thể (areaId=1) chỉ trả về các đơn tại Quận 1")
    void testSearchRequestsBySpecificArea() throws Exception {
        mockMvc.perform(get("/api/v1/repair-requests/matching")
                        .header("Authorization", "Bearer " + techToken)
                        .param("areaId", "1")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[*].areaId", everyItem(is(1))));
    }

    @Test
    @Order(2)
    @DisplayName("RC-38 AC 1b: Thợ tìm kiếm tại khu vực không có đơn mở thầu (areaId=5 - Thanh Xuân) trả về danh sách rỗng")
    void testSearchRequestsByEmptyArea() throws Exception {
        mockMvc.perform(get("/api/v1/repair-requests/matching")
                        .header("Authorization", "Bearer " + techToken)
                        .param("areaId", "5")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.meta.totalItems").value(0))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @Order(3)
    @DisplayName("RC-38 AC 2: Không truyền areaId thì mặc định trả về tất cả khu vực thợ đã đăng ký")
    void testSearchRequestsDefaultAllProfileAreas() throws Exception {
        mockMvc.perform(get("/api/v1/repair-requests/matching")
                        .header("Authorization", "Bearer " + techToken)
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @Order(4)
    @DisplayName("RC-38 AC 3: Tìm kiếm kết hợp khu vực và từ khóa (areaId=1 & search=Electrolux)")
    void testSearchRequestsByAreaAndKeyword() throws Exception {
        mockMvc.perform(get("/api/v1/repair-requests/matching")
                        .header("Authorization", "Bearer " + techToken)
                        .param("areaId", "1")
                        .param("search", "Electrolux")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].areaId").value(1))
                .andExpect(jsonPath("$.data[0].title", org.hamcrest.Matchers.containsString("Electrolux")));

        // Tìm từ khóa không khớp trong khu vực đó -> rỗng
        mockMvc.perform(get("/api/v1/repair-requests/matching")
                        .header("Authorization", "Bearer " + techToken)
                        .param("areaId", "1")
                        .param("search", "Từ khóa không tồn tại")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @Order(5)
    @DisplayName("RC-38 AC 4a: Thợ chưa KYC (PENDING) bị từ chối 403 UNVERIFIED_TECHNICIAN")
    void testUnverifiedTechnicianDenied() throws Exception {
        mockMvc.perform(get("/api/v1/repair-requests/matching")
                        .header("Authorization", "Bearer " + pendingTechToken)
                        .param("areaId", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("UNVERIFIED_TECHNICIAN"));
    }

    @Test
    @Order(6)
    @DisplayName("RC-38 AC 4b: Người dùng vai trò Khách hàng (CUSTOMER) bị từ chối 403 Forbidden")
    void testCustomerRoleDenied() throws Exception {
        mockMvc.perform(get("/api/v1/repair-requests/matching")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }
}
