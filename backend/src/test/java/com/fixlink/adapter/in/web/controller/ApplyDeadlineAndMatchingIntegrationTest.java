package com.fixlink.adapter.in.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.adapter.in.web.dto.request.LoginRequest;
import com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity;
import com.fixlink.adapter.out.persistence.entity.TechnicianProfileJpaEntity;
import com.fixlink.adapter.out.persistence.entity.UserJpaEntity;
import com.fixlink.adapter.out.persistence.repository.SpringDataRepairRequestRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataTechnicianProfileRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataUserRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataWorkProgressRepository;
import com.fixlink.domain.model.RequestStatus;
import com.fixlink.domain.model.VerificationStatus;
import com.fixlink.infrastructure.scheduler.ExpiredRequestScheduler;
import org.junit.jupiter.api.AfterEach;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pivot first-come-first-served: hạn 3 ngày và việc ẩn job khỏi danh sách thợ.
 *
 * <p>Phủ hai ràng buộc của mô hình mới:
 * <ul>
 *   <li>Job không ai nhận sau 3 ngày → tự hủy và không còn hiện với thợ.</li>
 *   <li>Job đã có thợ nhận → biến mất khỏi danh sách của các thợ khác.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplyDeadlineAndMatchingIntegrationTest {

    private static final String TECH = "tech01";
    private static final String PASSWORD = "Password@123";
    private static final long CATEGORY_ID = 1L;
    private static final long AREA_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataRepairRequestRepository requestRepo;

    @Autowired
    private SpringDataUserRepository userRepo;

    @Autowired
    private SpringDataTechnicianProfileRepository techProfileRepo;

    @Autowired
    private SpringDataWorkProgressRepository progressRepo;

    @Autowired
    private ExpiredRequestScheduler expiredRequestScheduler;

    private String techToken;
    private Long customerId;

    /**
     * H2 in-memory dùng chung cho cả JVM test, lớp này không bọc
     * {@code @Transactional} nên mọi thay đổi commit thật. Phải trả nguyên trạng
     * hồ sơ thợ và dọn các yêu cầu đã tạo, nếu không sẽ làm hỏng các test khác
     * theo thứ tự chạy.
     */
    private VerificationStatus originalVerification;
    private LinkedHashSet<Long> originalCategories;
    private LinkedHashSet<Long> originalAreas;
    private Long techId;
    private final List<Long> createdRequestIds = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        techToken = login(TECH, PASSWORD);

        UserJpaEntity techUser = userRepo.findByUsernameAndDeletedAtIsNull(TECH).orElseThrow();
        techId = techUser.getId();
        TechnicianProfileJpaEntity profile = techProfileRepo.findById(techId).orElseThrow();
        originalVerification = profile.getVerificationStatus();
        originalCategories = new LinkedHashSet<>(profile.getCategoryIds());
        originalAreas = new LinkedHashSet<>(profile.getAreaIds());
        profile.setVerificationStatus(VerificationStatus.APPROVED);
        profile.setCategoryIds(new LinkedHashSet<>(List.of(CATEGORY_ID)));
        profile.setAreaIds(new LinkedHashSet<>(List.of(AREA_ID)));
        techProfileRepo.save(profile);

        customerId = userRepo.findByUsernameAndDeletedAtIsNull("customer01").orElseThrow().getId();
    }

    @AfterEach
    void tearDown() {
        for (Long requestId : createdRequestIds) {
            // work_progress có FK trỏ về repair_requests nên phải xóa trước.
            progressRepo.deleteAll(progressRepo.findByRequestIdOrderByCreatedAtAsc(requestId));
            requestRepo.findById(requestId).ifPresent(requestRepo::delete);
        }
        createdRequestIds.clear();

        TechnicianProfileJpaEntity profile = techProfileRepo.findById(techId).orElseThrow();
        profile.setVerificationStatus(originalVerification);
        profile.setCategoryIds(originalCategories);
        profile.setAreaIds(originalAreas);
        techProfileRepo.save(profile);
    }

    private String login(String username, String password) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
        assertNotNull(token);
        return token;
    }

    private RepairRequestJpaEntity saveRequest(String code, RequestStatus status, LocalDateTime applyDeadline) {
        RepairRequestJpaEntity request = new RepairRequestJpaEntity();
        request.setRequestCode(code + System.nanoTime());
        request.setCustomerId(customerId);
        request.setCategoryId(CATEGORY_ID);
        request.setAreaId(AREA_ID);
        request.setTitle("Job kiểm thử hạn nhận việc");
        request.setDescription("Dữ liệu dựng cho test hạn 3 ngày");
        request.setAddress("2 Hai Bà Trưng, Quận 1");
        request.setBudgetRef(new BigDecimal("450000"));
        request.setStatus(status);
        request.setApplyDeadline(applyDeadline);
        request.setRequestedTime(LocalDateTime.now().plusDays(1));
        request.setCreatedBy(customerId);
        RepairRequestJpaEntity saved = requestRepo.save(request);
        createdRequestIds.add(saved.getId());
        return saved;
    }

    @Test
    @DisplayName("Pivot: job quá hạn 3 ngày → job quét tự hủy và không còn hiện trong /matching")
    void expiredRequest_isCancelledAndHiddenFromMatching() throws Exception {
        RepairRequestJpaEntity expired =
                saveRequest("REQ-EXPIRED-", RequestStatus.OPEN, LocalDateTime.now().minusMinutes(1));

        // Lọc kép: kể cả trước khi job quét chạy, thợ đã không thấy job hết hạn.
        assertMatchingDoesNotContain(expired.getRequestCode());

        expiredRequestScheduler.cancelExpiredOpenRequests();

        RepairRequestJpaEntity after = requestRepo.findById(expired.getId()).orElseThrow();
        assertEquals(RequestStatus.CANCELLED, after.getStatus(),
                "Job hết hạn 3 ngày phải tự chuyển sang CANCELLED");
        assertEquals("Hết hạn 3 ngày không có thợ nhận", after.getCancelReason());

        assertMatchingDoesNotContain(expired.getRequestCode());
    }

    @Test
    @DisplayName("Pivot: job đã có thợ nhận (ASSIGNED) → không còn hiện trong /matching của thợ khác")
    void takenRequest_isHiddenFromMatching() throws Exception {
        RepairRequestJpaEntity taken =
                saveRequest("REQ-TAKEN-", RequestStatus.ASSIGNED, LocalDateTime.now().plusDays(2));

        assertMatchingDoesNotContain(taken.getRequestCode());
    }

    @Test
    @DisplayName("Pivot: job còn OPEN và trong hạn → vẫn hiện trong /matching")
    void openRequestWithinDeadline_isVisibleInMatching() throws Exception {
        RepairRequestJpaEntity open =
                saveRequest("REQ-OPEN-", RequestStatus.OPEN, LocalDateTime.now().plusDays(2));

        mockMvc.perform(get("/api/v1/repair-requests/matching")
                        .header("Authorization", "Bearer " + techToken)
                        .param("limit", "100"))
                .andExpect(status().isOk())
                // Mọi job hiện ra cho thợ đều phải đang ở trạng thái OPEN.
                .andExpect(jsonPath("$.data[*].status", everyItem(org.hamcrest.Matchers.is("OPEN"))))
                .andExpect(jsonPath("$.data[?(@.requestCode == '" + open.getRequestCode() + "')]")
                        .exists());
    }

    private void assertMatchingDoesNotContain(String requestCode) throws Exception {
        mockMvc.perform(get("/api/v1/repair-requests/matching")
                        .header("Authorization", "Bearer " + techToken)
                        .param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].requestCode", everyItem(not(requestCode))));
    }
}
