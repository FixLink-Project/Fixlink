package com.fixlink.adapter.in.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.adapter.in.web.dto.request.CancelAppointmentRequest;
import com.fixlink.adapter.in.web.dto.request.CreateAppointmentRequest;
import com.fixlink.adapter.in.web.dto.request.LoginRequest;
import com.fixlink.adapter.in.web.dto.request.RescheduleAppointmentRequest;
import com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity;
import com.fixlink.adapter.out.persistence.entity.UserJpaEntity;
import com.fixlink.adapter.out.persistence.repository.SpringDataAppointmentRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử tích hợp tự động cho Jira RC-48:
 * Appointment Status Lifecycle (Vòng đời trạng thái cuộc hẹn giữa Khách và Kỹ thuật viên).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AppointmentLifecycleIntegrationTest {

    private static final String TECH_01 = "tech01";
    private static final String TECH_OTHER = "tho_dien_lanh_01";
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

    @Autowired
    private SpringDataAppointmentRepository appointmentRepo;

    private static String customerToken;
    private static String tech01Token;
    private static String techOtherToken;

    private static Long customer01Id;
    private static Long tech01Id;
    private static Long techOtherId;

    private static Long testRequestId;
    private static Long sharedAppointmentId;

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
        if (techOtherToken == null) {
            techOtherToken = loginAndGetToken(TECH_OTHER, PASSWORD);
            techOtherId = userRepo.findByUsernameAndDeletedAtIsNull(TECH_OTHER).map(UserJpaEntity::getId).orElseThrow();
        }

        if (testRequestId == null) {
            RepairRequestJpaEntity req = new RepairRequestJpaEntity();
            req.setRequestCode("REQ-APPT-" + System.currentTimeMillis());
            req.setCustomerId(customer01Id);
            req.setTechnicianId(tech01Id);
            req.setCategoryId(1L);
            req.setAreaId(1L);
            req.setTitle("Sửa bình nóng lạnh rò nước");
            req.setDescription("Bình nóng lạnh chảy nước ngấm tường");
            req.setAddress("123 Nguyễn Thị Minh Khai, Q1, TP.HCM");
            req.setRequestedTime(LocalDateTime.now().plusDays(2));
            req.setStatus(RequestStatus.MATCHED_AWAITING_DEPOSIT);
            req.setAgreedPrice(new BigDecimal("350000"));
            req.setDepositAmount(new BigDecimal("105000"));
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
    @DisplayName("TC-48-01: Tạo lịch hẹn mới thành công -> Trạng thái ban đầu CONFIRMED")
    void test01_CreateAppointment_Success_StatusConfirmed() throws Exception {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalTime appointmentTime = LocalTime.of(9, 30);

        CreateAppointmentRequest req = CreateAppointmentRequest.builder()
                .repairRequestId(testRequestId)
                .technicianId(tech01Id)
                .scheduledDate(tomorrow)
                .scheduledTime(appointmentTime)
                .notes("Khách ở nhà buổi sáng từ 9h đến 11h")
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.data.statusLabel", is("Đã xác nhận")))
                .andExpect(jsonPath("$.data.repairRequestId", is(testRequestId.intValue())))
                .andExpect(jsonPath("$.data.scheduledDate", is(tomorrow.toString())))
                .andReturn();

        String json = res.getResponse().getContentAsString();
        sharedAppointmentId = objectMapper.readTree(json).path("data").path("id").asLong();
        assertNotNull(sharedAppointmentId);
    }

    @Test
    @Order(2)
    @DisplayName("TC-48-02: Xem chi tiết và danh sách lịch hẹn của đơn sửa chữa")
    void test02_GetAppointmentDetailAndList_Success() throws Exception {
        assertNotNull(sharedAppointmentId, "sharedAppointmentId phải có từ test01");

        // Xem chi tiết lịch hẹn
        mockMvc.perform(get("/api/v1/appointments/" + sharedAppointmentId)
                        .header("Authorization", "Bearer " + tech01Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(sharedAppointmentId.intValue())))
                .andExpect(jsonPath("$.data.technicianId", is(tech01Id.intValue())));

        // Lấy danh sách lịch hẹn theo đơn
        mockMvc.perform(get("/api/v1/repair-requests/" + testRequestId + "/appointments")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @Order(3)
    @DisplayName("TC-48-03: Đổi thời gian hẹn (Reschedule) thành công -> Chuyển sang RESCHEDULED")
    void test03_RescheduleAppointment_Success_StatusRescheduled() throws Exception {
        assertNotNull(sharedAppointmentId);

        LocalDate newDate = LocalDate.now().plusDays(3);
        LocalTime newTime = LocalTime.of(15, 0);

        RescheduleAppointmentRequest req = RescheduleAppointmentRequest.builder()
                .scheduledDate(newDate)
                .scheduledTime(newTime)
                .notes("Đổi sang chiều do sáng bận")
                .build();

        mockMvc.perform(patch("/api/v1/appointments/" + sharedAppointmentId + "/reschedule")
                        .header("Authorization", "Bearer " + tech01Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("RESCHEDULED")))
                .andExpect(jsonPath("$.data.statusLabel", is("Đã dời lịch")))
                .andExpect(jsonPath("$.data.scheduledDate", is(newDate.toString())));
    }

    @Test
    @Order(4)
    @DisplayName("TC-48-04: Hoàn thành cuộc hẹn (Complete) thành công -> Chuyển sang COMPLETED")
    void test04_CompleteAppointment_Success_StatusCompleted() throws Exception {
        assertNotNull(sharedAppointmentId);

        mockMvc.perform(patch("/api/v1/appointments/" + sharedAppointmentId + "/complete")
                        .header("Authorization", "Bearer " + tech01Token)
                        .param("note", "Kỹ thuật viên đã đến khảo sát và sửa chữa xong đúng hẹn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("COMPLETED")))
                .andExpect(jsonPath("$.data.statusLabel", is("Đã hoàn thành")));
    }

    @Test
    @Order(5)
    @DisplayName("TC-48-05: Chặn thay đổi cuộc hẹn khi đã COMPLETED -> 400 Bad Request")
    void test05_CannotAlterTerminalAppointment_WhenCompleted() throws Exception {
        assertNotNull(sharedAppointmentId);

        // Thử dời lịch hẹn đã completed
        RescheduleAppointmentRequest reschedReq = RescheduleAppointmentRequest.builder()
                .scheduledDate(LocalDate.now().plusDays(4))
                .scheduledTime(LocalTime.of(10, 0))
                .build();

        mockMvc.perform(patch("/api/v1/appointments/" + sharedAppointmentId + "/reschedule")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reschedReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", notNullValue()));

        // Thử hủy cuộc hẹn đã completed
        CancelAppointmentRequest cancelReq = CancelAppointmentRequest.builder()
                .cancelReason("Muốn hủy cuộc hẹn này")
                .build();

        mockMvc.perform(patch("/api/v1/appointments/" + sharedAppointmentId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    @Order(6)
    @DisplayName("TC-48-06: Hủy lịch hẹn kèm lý do -> Chuyển sang CANCELLED")
    void test06_CancelAppointment_Success_StatusCancelled() throws Exception {
        // Tạo một lịch hẹn mới để test hủy
        LocalDate apptDate = LocalDate.now().plusDays(2);
        LocalTime apptTime = LocalTime.of(11, 0);

        CreateAppointmentRequest createReq = CreateAppointmentRequest.builder()
                .repairRequestId(testRequestId)
                .technicianId(tech01Id)
                .scheduledDate(apptDate)
                .scheduledTime(apptTime)
                .notes("Hẹn kiểm tra máy tính")
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long cancelApptId = objectMapper.readTree(res.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Khách hủy cuộc hẹn kèm lý do
        CancelAppointmentRequest cancelReq = CancelAppointmentRequest.builder()
                .cancelReason("Khách có việc đột xuất phải đi công tác gấp")
                .build();

        mockMvc.perform(patch("/api/v1/appointments/" + cancelApptId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("CANCELLED")))
                .andExpect(jsonPath("$.data.statusLabel", is("Đã hủy")))
                .andExpect(jsonPath("$.data.cancelReason", is("Khách có việc đột xuất phải đi công tác gấp")));

        // Chặn không cho đổi lịch hoặc hoàn thành cuộc hẹn đã hủy
        mockMvc.perform(patch("/api/v1/appointments/" + cancelApptId + "/complete")
                        .header("Authorization", "Bearer " + tech01Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    @Order(7)
    @DisplayName("TC-48-07: Chặn đặt lịch hẹn trong quá khứ -> 400 Bad Request")
    void test07_CannotCreateAppointmentWithPastDate() throws Exception {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalTime pastTime = LocalTime.of(9, 0);

        CreateAppointmentRequest req = CreateAppointmentRequest.builder()
                .repairRequestId(testRequestId)
                .technicianId(tech01Id)
                .scheduledDate(yesterday)
                .scheduledTime(pastTime)
                .notes("Thử đặt lịch ngày hôm qua")
                .build();

        mockMvc.perform(post("/api/v1/appointments")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    @Order(8)
    @DisplayName("TC-48-08: Người dùng không liên quan bị chặn truy cập hoặc chỉnh sửa -> 403 Forbidden")
    void test08_UnauthorizedUser_Forbidden_403() throws Exception {
        assertNotNull(sharedAppointmentId);

        // Thợ không liên quan (tho_dien_lanh_01) cố xem chi tiết cuộc hẹn của Tech01 & Customer01
        mockMvc.perform(get("/api/v1/appointments/" + sharedAppointmentId)
                        .header("Authorization", "Bearer " + techOtherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", notNullValue()));

        // Thợ lạ cố can thiệp hủy lịch hẹn của người khác
        CancelAppointmentRequest cancelReq = CancelAppointmentRequest.builder()
                .cancelReason("Thợ lạ cố hủy lịch")
                .build();

        mockMvc.perform(patch("/api/v1/appointments/" + sharedAppointmentId + "/cancel")
                        .header("Authorization", "Bearer " + techOtherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", notNullValue()));
    }
}
