package com.fixlink.adapter.in.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.adapter.in.web.dto.request.LoginRequest;
import com.fixlink.adapter.out.persistence.entity.QuotationJpaEntity;
import com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity;
import com.fixlink.adapter.out.persistence.entity.TechnicianProfileJpaEntity;
import com.fixlink.adapter.out.persistence.entity.UserJpaEntity;
import com.fixlink.adapter.out.persistence.repository.SpringDataQuotationRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataRepairRequestRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataTechnicianProfileRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataUserRepository;
import com.fixlink.domain.model.QuotationStatus;
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

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử tích hợp tự động cho Jira RC-39:
 * Technician View Request Detail (Kỹ thuật viên xem chi tiết yêu cầu sửa chữa).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RepairRequestTechnicianDetailIntegrationTest {

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

    @Autowired
    private SpringDataQuotationRepository quotationRepo;

    private String techToken;
    private String pendingTechToken;
    private String customerToken;

    private Long openRequestId;
    private Long assignedRequestId;
    private Long draftRequestId;

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

        UserJpaEntity techUser = userRepo.findByUsernameAndDeletedAtIsNull(TECH_APPROVED).orElseThrow();
        TechnicianProfileJpaEntity profile = techProfileRepo.findById(techUser.getId()).orElseThrow();
        profile.setCategoryIds(new LinkedHashSet<>(List.of(1L, 2L, 3L)));
        profile.setAreaIds(new LinkedHashSet<>(List.of(1L, 2L, 3L)));
        techProfileRepo.save(profile);

        UserJpaEntity custUser = userRepo.findByUsernameAndDeletedAtIsNull(CUSTOMER).orElseThrow();

        // 1. Đơn mở thầu BIDDING_OPEN chưa có báo giá
        RepairRequestJpaEntity reqOpen = repairRequestRepo.findByRequestCode("REQ-TEST-RC39-OPEN")
                .orElseGet(() -> {
                    RepairRequestJpaEntity r = new RepairRequestJpaEntity();
                    r.setRequestCode("REQ-TEST-RC39-OPEN");
                    r.setTitle("Sửa máy rửa chén Bosch lỗi E15 tràn nước");
                    r.setDescription("Máy báo lỗi E15 nhấp nháy, có nước dưới đáy máy cần thợ tới kiểm tra gấp.");
                    r.setAddress("456 Nguyễn Đình Chiểu, Phường 4, Quận 3, TP.HCM");
                    r.setCustomerId(custUser.getId());
                    r.setCategoryId(1L);
                    r.setAreaId(1L);
                    r.setStatus(RequestStatus.BIDDING_OPEN);
                    r.setBudgetRef(new BigDecimal("500000"));
                    r.setBiddingDeadline(LocalDateTime.now().plusDays(3));
                    r.setRequestedTime(LocalDateTime.now().plusDays(1));
                    return repairRequestRepo.save(r);
                });
        openRequestId = reqOpen.getId();

        // 2. Đơn đã giao cho tech01
        RepairRequestJpaEntity reqAssigned = repairRequestRepo.findByRequestCode("REQ-TEST-RC39-ASSIGNED")
                .orElseGet(() -> {
                    RepairRequestJpaEntity r = new RepairRequestJpaEntity();
                    r.setRequestCode("REQ-TEST-RC39-ASSIGNED");
                    r.setTitle("Bảo dưỡng điều hòa Daikin Inverter");
                    r.setDescription("Vệ sinh lưới lọc và nạp gas bổ sung.");
                    r.setAddress("789 Hai Bà Trưng, Quận 1, TP.HCM");
                    r.setCustomerId(custUser.getId());
                    r.setTechnicianId(techUser.getId());
                    r.setCategoryId(1L);
                    r.setAreaId(1L);
                    r.setStatus(RequestStatus.ASSIGNED);
                    r.setAgreedPrice(new BigDecimal("450000"));
                    r.setRequestedTime(LocalDateTime.now().plusDays(1));
                    return repairRequestRepo.save(r);
                });
        assignedRequestId = reqAssigned.getId();

        // 3. Đơn bản nháp DRAFT của khách
        RepairRequestJpaEntity reqDraft = repairRequestRepo.findByRequestCode("REQ-TEST-RC39-DRAFT")
                .orElseGet(() -> {
                    RepairRequestJpaEntity r = new RepairRequestJpaEntity();
                    r.setRequestCode("REQ-TEST-RC39-DRAFT");
                    r.setTitle("Bản nháp sửa bình nóng lạnh");
                    r.setDescription("Khách chưa phát sóng đơn này.");
                    r.setAddress("123 Lê Duẩn, Quận 1, TP.HCM");
                    r.setCustomerId(custUser.getId());
                    r.setCategoryId(1L);
                    r.setAreaId(1L);
                    r.setStatus(RequestStatus.DRAFT);
                    r.setRequestedTime(LocalDateTime.now().plusDays(1));
                    return repairRequestRepo.save(r);
                });
        draftRequestId = reqDraft.getId();
    }

    @Test
    @Order(1)
    @DisplayName("RC-39 AC 1: Thợ đã duyệt KYC xem chi tiết đơn mở thầu thành công khi chưa gửi báo giá")
    void testVerifiedTechViewsOpenBiddingRequestWithoutQuotation() throws Exception {
        mockMvc.perform(get("/api/v1/repair-requests/{id}", openRequestId)
                        .header("Authorization", "Bearer " + techToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.request.id", is(openRequestId.intValue())))
                .andExpect(jsonPath("$.data.request.requestCode", is("REQ-TEST-RC39-OPEN")))
                .andExpect(jsonPath("$.data.request.title", is("Sửa máy rửa chén Bosch lỗi E15 tràn nước")))
                .andExpect(jsonPath("$.data.request.status", is("BIDDING_OPEN")))
                .andExpect(jsonPath("$.data.request.address", notNullValue()))
                .andExpect(jsonPath("$.data.request.budgetRef", is(500000)))
                .andExpect(jsonPath("$.data.quotations", empty()));
    }

    @Test
    @Order(2)
    @DisplayName("RC-39 AC 2: Thợ xem đơn có báo giá chỉ thấy báo giá của chính mình, bảo mật báo giá thợ khác")
    void testVerifiedTechViewsRequestWithOwnQuotationOnly() throws Exception {
        UserJpaEntity techUser = userRepo.findByUsernameAndDeletedAtIsNull(TECH_APPROVED).orElseThrow();

        // Tạo 1 báo giá của tech01 trên openRequestId
        if (quotationRepo.findByRequestIdAndTechnicianId(openRequestId, techUser.getId()).isEmpty()) {
            QuotationJpaEntity myQuote = new QuotationJpaEntity();
            myQuote.setRequestId(openRequestId);
            myQuote.setTechnicianId(techUser.getId());
            myQuote.setPriceLaborVnd(new BigDecimal("300000"));
            myQuote.setPriceMaterialsVnd(new BigDecimal("150000"));
            myQuote.setSolution("Xử lý vi mạch cảm biến ngập nước đáy máy.");
            myQuote.setStatus(QuotationStatus.PENDING);
            quotationRepo.save(myQuote);
        }

        mockMvc.perform(get("/api/v1/repair-requests/{id}", openRequestId)
                        .header("Authorization", "Bearer " + techToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quotations", hasSize(1)))
                .andExpect(jsonPath("$.data.quotations[0].technicianId", is(techUser.getId().intValue())))
                .andExpect(jsonPath("$.data.quotations[0].solution", is("Xử lý vi mạch cảm biến ngập nước đáy máy.")))
                .andExpect(jsonPath("$.data.quotations[0].priceLaborVnd", is(300000)))
                .andExpect(jsonPath("$.data.quotations[0].priceMaterialsVnd", is(150000)));
    }

    @Test
    @Order(3)
    @DisplayName("RC-39 AC 3: Thợ chưa duyệt KYC (PENDING) bị từ chối 403 UNVERIFIED_TECHNICIAN")
    void testUnverifiedTechDeniedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/repair-requests/{id}", openRequestId)
                        .header("Authorization", "Bearer " + pendingTechToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("UNVERIFIED_TECHNICIAN")));
    }

    @Test
    @Order(4)
    @DisplayName("RC-39 AC 4: Thợ được giao việc (ASSIGNED) xem chi tiết đơn thành công")
    void testAssignedTechViewsRequest() throws Exception {
        mockMvc.perform(get("/api/v1/repair-requests/{id}", assignedRequestId)
                        .header("Authorization", "Bearer " + techToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.request.id", is(assignedRequestId.intValue())))
                .andExpect(jsonPath("$.data.request.requestCode", is("REQ-TEST-RC39-ASSIGNED")))
                .andExpect(jsonPath("$.data.request.status", is("ASSIGNED")))
                .andExpect(jsonPath("$.data.request.agreedPrice", is(450000)));
    }

    @Test
    @Order(5)
    @DisplayName("RC-39 AC 5: Thợ không được phép xem đơn bản nháp (DRAFT) của khách hàng khác (403 ACCESS_DENIED)")
    void testTechCannotViewDraftRequestOfOtherCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/repair-requests/{id}", draftRequestId)
                        .header("Authorization", "Bearer " + techToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @Order(6)
    @DisplayName("RC-39 AC 6: Yêu cầu không tồn tại trả về 404 NOT_FOUND")
    void testNonExistentRequestReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/repair-requests/999999")
                        .header("Authorization", "Bearer " + techToken))
                .andExpect(status().isNotFound());
    }
}
