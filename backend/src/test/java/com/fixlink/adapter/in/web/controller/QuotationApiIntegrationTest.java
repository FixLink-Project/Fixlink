package com.fixlink.adapter.in.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.adapter.in.web.dto.request.CreateQuotationRequest;
import com.fixlink.adapter.in.web.dto.request.LoginRequest;
import com.fixlink.adapter.in.web.dto.request.UpdateQuotationRequest;
import com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity;
import com.fixlink.adapter.out.persistence.entity.UserJpaEntity;
import com.fixlink.adapter.out.persistence.repository.SpringDataRepairRequestRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataUserRepository;
import com.fixlink.domain.model.RequestStatus;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QuotationApiIntegrationTest {

    private static final String TECH_01 = "tech01";
    private static final String CUSTOMER_01 = "customer01";
    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataRepairRequestRepository repairRequestRepo;

    @Autowired
    private SpringDataUserRepository userRepo;

    private static String customerToken;
    private static String tech01Token;
    private static Long customer01Id;
    private static Long tech01Id;
    private static Long testRequestId;
    private static Long createdQuotationId;

    @BeforeEach
    void setUp() throws Exception {
        if (customerToken == null) {
            customerToken = loginAndGetToken(CUSTOMER_01, PASSWORD);
            customer01Id = userRepo.findByUsernameAndDeletedAtIsNull(CUSTOMER_01).map(UserJpaEntity::getId).orElseThrow();
        }
        if (tech01Token == null) {
            tech01Token = loginAndGetToken(TECH_01, PASSWORD);
            tech01Id = userRepo.findByUsernameAndDeletedAtIsNull(TECH_01).map(UserJpaEntity::getId).orElseThrow();
        }

        if (testRequestId == null) {
            RepairRequestJpaEntity req = new RepairRequestJpaEntity();
            req.setRequestCode("REQ-QUOTE-" + System.currentTimeMillis());
            req.setCustomerId(customer01Id);
            req.setCategoryId(1L);
            req.setAreaId(1L);
            req.setTitle("Sửa máy giặt kêu to");
            req.setDescription("Máy giặt rung lắc và kêu rất to khi vắt");
            req.setAddress("456 Lê Lợi, Q1, TP.HCM");
            req.setRequestedTime(LocalDateTime.now().plusDays(2));
            req.setStatus(RequestStatus.BIDDING_OPEN);
            req.setCreatedBy(customer01Id);
            req = repairRequestRepo.save(req);
            testRequestId = req.getId();
        }
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("accessToken").asText();
    }

    @Test
    @Order(1)
    @DisplayName("RC-40: Thợ gửi báo giá thành công (POST /api/v1/repair-requests/{requestId}/quotations)")
    void test01_SubmitQuotation_Success() throws Exception {
        CreateQuotationRequest req = new CreateQuotationRequest();
        req.setSolution("Kiểm tra quang treo và thay giảm sóc máy giặt");
        req.setPriceLaborVnd(new BigDecimal("150000"));
        req.setPriceMaterialsVnd(new BigDecimal("200000"));
        req.setInspectionTime(LocalDateTime.now().plusDays(1));
        req.setEstimatedFinish(LocalDateTime.now().plusDays(2));
        req.setNote("Bảo hành linh kiện 6 tháng");

        MvcResult result = mockMvc.perform(post("/api/v1/repair-requests/" + testRequestId + "/quotations")
                        .header("Authorization", "Bearer " + tech01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode", is(201)))
                .andExpect(jsonPath("$.data.solution", is("Kiểm tra quang treo và thay giảm sóc máy giặt")))
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        createdQuotationId = objectMapper.readTree(body).path("data").path("id").asLong();
        assertNotNull(createdQuotationId);
    }

    @Test
    @Order(2)
    @DisplayName("RC-42: Kỹ thuật viên xem danh sách báo giá của mình (GET /api/v1/technicians/me/quotations)")
    void test02_GetMyQuotations_Success() throws Exception {
        mockMvc.perform(get("/api/v1/technicians/me/quotations")
                        .header("Authorization", "Bearer " + tech01Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(3)
    @DisplayName("RC-42: Kỹ thuật viên xem chi tiết một báo giá (GET /api/v1/technicians/me/quotations/{id})")
    void test03_GetMyQuotationDetail_Success() throws Exception {
        assertNotNull(createdQuotationId, "createdQuotationId phải có từ test01");
        mockMvc.perform(get("/api/v1/technicians/me/quotations/" + createdQuotationId)
                        .header("Authorization", "Bearer " + tech01Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data.id", is(createdQuotationId.intValue())));
    }

    @Test
    @Order(4)
    @DisplayName("RC-41: Thợ cập nhật báo giá (PUT /api/v1/repair-requests/{requestId}/quotations/{quotationId})")
    void test04_UpdateQuotation_Success() throws Exception {
        assertNotNull(createdQuotationId, "createdQuotationId phải có từ test01");
        UpdateQuotationRequest req = new UpdateQuotationRequest();
        req.setSolution("Thay giảm sóc chính hãng và vệ sinh lồng giặt");
        req.setPriceLaborVnd(new BigDecimal("180000"));
        req.setPriceMaterialsVnd(new BigDecimal("220000"));
        req.setInspectionTime(LocalDateTime.now().plusDays(1));
        req.setEstimatedFinish(LocalDateTime.now().plusDays(2));
        req.setNote("Bảo hành linh kiện 12 tháng");

        mockMvc.perform(put("/api/v1/repair-requests/" + testRequestId + "/quotations/" + createdQuotationId)
                        .header("Authorization", "Bearer " + tech01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data.solution", is("Thay giảm sóc chính hãng và vệ sinh lồng giặt")));
    }

    @Test
    @Order(5)
    @DisplayName("RC-41: Thợ rút báo giá (DELETE /api/v1/repair-requests/{requestId}/quotations/{quotationId})")
    void test05_WithdrawQuotation_Success() throws Exception {
        assertNotNull(createdQuotationId, "createdQuotationId phải có từ test01");
        mockMvc.perform(delete("/api/v1/repair-requests/" + testRequestId + "/quotations/" + createdQuotationId)
                        .header("Authorization", "Bearer " + tech01Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.message", is("Đã rút báo giá")));
    }
}
