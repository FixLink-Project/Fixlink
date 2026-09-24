package com.fixlink.adapter.in.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.adapter.in.web.dto.request.AttachMediaRequest;
import com.fixlink.adapter.in.web.dto.request.CreateRepairRequestRequest;
import com.fixlink.adapter.in.web.dto.request.LoginRequest;
import com.fixlink.adapter.in.web.dto.request.UpdateRepairRequestStatusRequest;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử tích hợp cho API Yêu cầu Sửa chữa:
 * tab trạng thái + phân trang đánh số + upload ảnh Firebase Storage + cập nhật trạng thái.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RepairRequestApiIntegrationTest {

    private static final String CUSTOMER_USERNAME = "customer01";
    private static final String CUSTOMER_PASSWORD = "Password@123";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "Admin@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String customerToken;
    private static String adminToken;
    private static Long createdRequestId;

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

    @Test
    @Order(1)
    @DisplayName("RC-30 AC 1: Khách hàng tạo yêu cầu sửa chữa kèm ảnh hiện trường (Firebase Storage URL)")
    void testCreateRepairRequestWithFirebaseMedia() throws Exception {
        customerToken = loginAndGetToken(CUSTOMER_USERNAME, CUSTOMER_PASSWORD);

        CreateRepairRequestRequest request = CreateRepairRequestRequest.builder()
                .title("Quạt trần kêu to và rung lắc mạnh")
                .description("Quạt trần phòng khách chạy số 3 thì rung lắc và phát ra tiếng kêu lạch cạch.")
                .address("25 Nguyễn Huệ, Quận 1, TP.HCM")
                .categoryId(1L)
                .requestedTime(LocalDateTime.now().plusDays(2))
                .saveAsDraft(false)
                .mediaUrls(List.of(
                        "https://firebasestorage.googleapis.com/v0/b/fixlink-app/o/repair-requests%2Fquat-tran-truoc.jpg?alt=media&token=abc123",
                        "https://firebasestorage.googleapis.com/v0/b/fixlink-app/o/repair-requests%2Fquat-tran-sau.jpg?alt=media&token=def456"
                ))
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/repair-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.requestCode", startsWith("REQ-")))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.statusLabel").value("Chờ báo giá"))
                .andExpect(jsonPath("$.data.media", hasSize(2)))
                .andExpect(jsonPath("$.data.media[0].mediaType").value("IMAGE"))
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        createdRequestId = payload.path("data").path("id").asLong();
        assertNotNull(createdRequestId);
    }

    @Test
    @Order(2)
    @DisplayName("RC-30 AC 2: Danh sách chia tab + phân trang đánh số trả về đầy đủ block meta")
    void testListRepairRequestsWithTabsAndNumberedPagination() throws Exception {
        mockMvc.perform(get("/api/v1/repair-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("page", "1")
                        .param("limit", "5")
                        .param("tab", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.meta.currentPage").value(1))
                .andExpect(jsonPath("$.meta.limit").value(5))
                .andExpect(jsonPath("$.meta.totalItems", greaterThanOrEqualTo(15)))
                .andExpect(jsonPath("$.meta.totalPages", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.meta.hasNext").value(true))
                .andExpect(jsonPath("$.meta.hasPrevious").value(false))
                .andExpect(jsonPath("$.data", hasSize(5)));

        // Tab "Chờ báo giá" chỉ trả về các trạng thái thuộc nhóm chờ thợ gửi báo giá
        mockMvc.perform(get("/api/v1/repair-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("tab", "AWAITING_QUOTE")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].status",
                        everyItem(oneOf("DRAFT", "PENDING", "BIDDING_OPEN", "MATCHED_AWAITING_DEPOSIT"))));

        // Tab "Hoàn thành" chỉ trả về trạng thái COMPLETED
        mockMvc.perform(get("/api/v1/repair-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("tab", "COMPLETED")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].status", everyItem(is("COMPLETED"))));

        // API danh sách tab phục vụ render Tab Bar
        mockMvc.perform(get("/api/v1/repair-requests/tabs")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.data[0].code").value("ALL"))
                .andExpect(jsonPath("$.data[0].label").value("Tất cả"));
    }

    @Test
    @Order(3)
    @DisplayName("RC-30 AC 3: Admin gắn ảnh bằng chứng và cập nhật trạng thái yêu cầu")
    void testAdminAttachEvidenceAndUpdateStatus() throws Exception {
        adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);

        AttachMediaRequest attachRequest = AttachMediaRequest.builder()
                .mediaUrls(List.of(
                        "https://firebasestorage.googleapis.com/v0/b/fixlink-app/o/repair-requests%2Fbien-ban-khao-sat.pdf?alt=media&token=xyz789"
                ))
                .build();

        mockMvc.perform(post("/api/v1/repair-requests/" + createdRequestId + "/media")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attachRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.media", hasSize(3)))
                .andExpect(jsonPath("$.data.media[2].mediaType").value("DOCUMENT"));

        // Admin chuyển yêu cầu sang trạng thái Đang thực hiện
        UpdateRepairRequestStatusRequest inProgress = UpdateRepairRequestStatusRequest.builder()
                .status("IN_PROGRESS")
                .note("Admin điều phối thợ Trần Văn B tiếp nhận")
                .build();

        mockMvc.perform(patch("/api/v1/repair-requests/" + createdRequestId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inProgress)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.statusLabel").value("Đang sửa chữa"));

        // Trạng thái không tồn tại trong hệ thống -> 400 VALIDATION_FAILED
        UpdateRepairRequestStatusRequest invalidStatus = UpdateRepairRequestStatusRequest.builder()
                .status("KHONG_TON_TAI")
                .build();

        mockMvc.perform(patch("/api/v1/repair-requests/" + createdRequestId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidStatus)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @Order(4)
    @DisplayName("RC-30 AC 4: Ràng buộc quyền và quy tắc trạng thái (khách hàng không thể hủy yêu cầu đã hoàn thành)")
    void testCustomerRulesAndSecuredEndpoints() throws Exception {
        // 1. Khách hàng không thể chuyển yêu cầu đã COMPLETED sang trạng thái khác
        MvcResult completedList = mockMvc.perform(get("/api/v1/repair-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("tab", "COMPLETED")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andReturn();

        Long completedRequestId = objectMapper.readTree(completedList.getResponse().getContentAsString())
                .path("data").path(0).path("id").asLong();

        UpdateRepairRequestStatusRequest cancelRequest = UpdateRepairRequestStatusRequest.builder()
                .status("CANCELLED")
                .note("Khách hàng cố hủy đơn đã hoàn thành")
                .build();

        mockMvc.perform(patch("/api/v1/repair-requests/" + completedRequestId + "/status")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATE_TRANSITION"));

        // 2. Gọi API khi chưa đăng nhập phải bị từ chối
        mockMvc.perform(get("/api/v1/repair-requests"))
                .andExpect(status().is4xxClientError());

        // 3. Dữ liệu đầu vào không hợp lệ -> 400 VALIDATION_FAILED (lỗi theo từng trường)
        CreateRepairRequestRequest invalidRequest = CreateRepairRequestRequest.builder()
                .title("abc")
                .description("quá ngắn")
                .address("")
                .categoryId(null)
                .build();

        mockMvc.perform(post("/api/v1/repair-requests")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.categoryId").exists());
    }
}
