package com.fixlink;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.dto.request.LoginRequest;
import com.fixlink.dto.request.UpdateUserStatusRequest;
import com.fixlink.dto.request.VerifyTechnicianRequest;
import com.fixlink.entity.TechnicianProfile;
import com.fixlink.entity.User;
import com.fixlink.enums.UserStatus;
import com.fixlink.enums.VerificationStatus;
import com.fixlink.repository.TechnicianProfileRepository;
import com.fixlink.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TechnicianProfileRepository technicianProfileRepository;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        LoginRequest loginRequest = new LoginRequest("admin", "admin123");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        adminToken = "Bearer " + jsonNode.path("data").path("accessToken").asText();
    }

    // =========================================================================
    // Security Checks
    // =========================================================================

    @Test
    @DisplayName("Admin endpoint without token returns 401 Unauthorized")
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    // =========================================================================
    // RC-19: List Users with Pagination, Filter, Search, Sort
    // =========================================================================

    @Test
    @DisplayName("RC-19: Get user list with default pagination")
    void testGetUsersDefaultPagination() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", adminToken)
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(5))))
                .andExpect(jsonPath("$.meta.currentPage").value(1))
                .andExpect(jsonPath("$.meta.limit").value(10))
                .andExpect(jsonPath("$.meta.totalItems", greaterThanOrEqualTo(5)));
    }

    @Test
    @DisplayName("RC-19: Filter user list by role=TECHNICIAN")
    void testFilterByRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", adminToken)
                        .param("role", "TECHNICIAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].role", everyItem(is("TECHNICIAN"))));
    }

    @Test
    @DisplayName("RC-19: Filter user list by status=BLOCKED")
    void testFilterByStatus() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", adminToken)
                        .param("status", "BLOCKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].status", everyItem(is("BLOCKED"))));
    }

    @Test
    @DisplayName("RC-19: Search user by name or phone keyword")
    void testSearchUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", adminToken)
                        .param("search", "Bình"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].technicianProfile.fullName", containsString("Bình")));
    }

    @Test
    @DisplayName("RC-19: Sort user list by createdAt DESC")
    void testSortUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", adminToken)
                        .param("sortBy", "createdAt")
                        .param("sortOrder", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));
    }

    // =========================================================================
    // RC-20: User Detail View
    // =========================================================================

    @Test
    @DisplayName("RC-20: View detailed info of a Technician (includes KYC details)")
    void testGetTechnicianDetail() throws Exception {
        User techUser = userRepository.findByUsername("tech_an").orElseThrow();

        mockMvc.perform(get("/api/v1/admin/users/" + techUser.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value("usr_" + techUser.getId()))
                .andExpect(jsonPath("$.data.username").value("tech_an"))
                .andExpect(jsonPath("$.data.role").value("TECHNICIAN"))
                .andExpect(jsonPath("$.data.technicianProfile").isNotEmpty())
                .andExpect(jsonPath("$.data.technicianProfile.fullName").value("Nguyễn Văn An"))
                .andExpect(jsonPath("$.data.technicianProfile.phone").value("0901234567"))
                .andExpect(jsonPath("$.data.technicianProfile.verificationStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.technicianProfile.citizenId").value("079090123456"));
    }

    @Test
    @DisplayName("RC-20: View non-existent user returns 404 Not Found")
    void testGetNonExistentUserDetail() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/999999")
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    // =========================================================================
    // RC-21: Block and Unblock User Account
    // =========================================================================

    @Test
    @DisplayName("RC-21: Block active user with reason and audit log")
    void testBlockUserWithReason() throws Exception {
        User cust = userRepository.findByUsername("cust_dung").orElseThrow();

        UpdateUserStatusRequest blockRequest = new UpdateUserStatusRequest(
                "BLOCKED",
                "Khách hàng spam đặt lịch ảo nhiều lần"
        );

        mockMvc.perform(patch("/api/v1/admin/users/" + cust.getId() + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.status").value("BLOCKED"))
                .andExpect(jsonPath("$.data.userId").isNotEmpty());

        // Verify in database
        User updated = userRepository.findById(cust.getId()).orElseThrow();
        assertEquals(UserStatus.BANNED, updated.getStatus());
        assertEquals("Khách hàng spam đặt lịch ảo nhiều lần", updated.getStatusReason());
        assertNotNull(updated.getStatusChangedAt());
    }

    @Test
    @DisplayName("RC-21: Unblock banned user")
    void testUnblockUser() throws Exception {
        User bannedTech = userRepository.findByUsername("tech_cuong").orElseThrow();

        UpdateUserStatusRequest unblockRequest = new UpdateUserStatusRequest(
                "ACTIVE",
                "Đã cam kết khắc phục vi phạm và hoàn tiền cho khách"
        );

        mockMvc.perform(patch("/api/v1/admin/users/" + bannedTech.getId() + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unblockRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("RC-21: Blocking user with reason < 10 characters returns 400 Bad Request")
    void testBlockUserValidationFailure() throws Exception {
        User cust = userRepository.findByUsername("cust_dung").orElseThrow();

        UpdateUserStatusRequest invalidRequest = new UpdateUserStatusRequest(
                "BLOCKED",
                "Ngắn quá" // Less than 10 characters
        );

        mockMvc.perform(patch("/api/v1/admin/users/" + cust.getId() + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    // =========================================================================
    // RC-22: Verify Technician Profile (KYC)
    // =========================================================================

    @Test
    @DisplayName("RC-22: Approve pending technician KYC verification")
    void testApproveTechnicianKYC() throws Exception {
        User pendingTech = userRepository.findByUsername("tech_binh").orElseThrow();

        VerifyTechnicianRequest approveRequest = new VerifyTechnicianRequest(
                "APPROVED",
                "Đã đối soát CCCD và bằng cấp điện tử thành công",
                null
        );

        mockMvc.perform(patch("/api/v1/admin/technicians/" + pendingTech.getId() + "/verify")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.verificationStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.verifiedBy").isNotEmpty())
                .andExpect(jsonPath("$.data.verifiedAt").isNotEmpty());

        // Verify in DB
        TechnicianProfile profile = technicianProfileRepository.findByUserId(pendingTech.getId()).orElseThrow();
        assertEquals(VerificationStatus.APPROVED, profile.getVerificationStatus());
        assertTrue(profile.getIsVerified());
    }

    @Test
    @DisplayName("RC-22: Reject pending technician KYC with reason")
    void testRejectTechnicianKYC() throws Exception {
        User pendingTech = userRepository.findByUsername("tech_binh").orElseThrow();

        VerifyTechnicianRequest rejectRequest = new VerifyTechnicianRequest(
                "REJECTED",
                "Yêu cầu thợ chụp lại 2 mặt CCCD rõ nét",
                "Ảnh chụp CCCD bị mờ, không thấy rõ số định danh cá nhân"
        );

        mockMvc.perform(patch("/api/v1/admin/technicians/" + pendingTech.getId() + "/verify")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.verificationStatus").value("REJECTED"));

        // Verify in DB
        TechnicianProfile profile = technicianProfileRepository.findByUserId(pendingTech.getId()).orElseThrow();
        assertEquals(VerificationStatus.REJECTED, profile.getVerificationStatus());
        assertFalse(profile.getIsVerified());
        assertEquals("Ảnh chụp CCCD bị mờ, không thấy rõ số định danh cá nhân", profile.getRejectionReason());
    }

    @Test
    @DisplayName("RC-22: Reject technician without reason returns 400 Bad Request")
    void testRejectTechnicianWithoutReason() throws Exception {
        User pendingTech = userRepository.findByUsername("tech_binh").orElseThrow();

        VerifyTechnicianRequest invalidReject = new VerifyTechnicianRequest(
                "REJECTED",
                "Note",
                null // Missing rejection reason
        );

        mockMvc.perform(patch("/api/v1/admin/technicians/" + pendingTech.getId() + "/verify")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReject)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errorCode").value("INVALID_VERIFICATION_REQUEST"));
    }
}
