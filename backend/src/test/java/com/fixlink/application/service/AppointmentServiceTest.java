package com.fixlink.application.service;

import com.fixlink.adapter.in.web.dto.response.AppointmentResponse;
import com.fixlink.adapter.out.persistence.entity.AppointmentJpaEntity;
import com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity;
import com.fixlink.adapter.out.persistence.repository.SpringDataAppointmentRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataCustomerProfileRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataQuotationRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataRepairRequestRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataTechnicianProfileRepository;
import com.fixlink.application.port.in.AppointmentUseCase;
import com.fixlink.domain.exception.DomainException;
import com.fixlink.domain.exception.ResourceNotFoundException;
import com.fixlink.domain.model.AppointmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests - AppointmentService (RC-47, RC-48)")
class AppointmentServiceTest {

    @Mock
    private SpringDataAppointmentRepository appointmentRepo;

    @Mock
    private SpringDataRepairRequestRepository requestRepo;

    @Mock
    private SpringDataCustomerProfileRepository customerProfileRepo;

    @Mock
    private SpringDataTechnicianProfileRepository techProfileRepo;

    @Mock
    private SpringDataQuotationRepository quotationRepo;

    @InjectMocks
    private AppointmentService appointmentService;

    private RepairRequestJpaEntity testRequest;
    private AppointmentJpaEntity testAppointment;
    private AppointmentUseCase.CreateAppointmentCommand createCmd;

    @BeforeEach
    void setUp() {
        testRequest = new RepairRequestJpaEntity();
        testRequest.setId(10L);
        testRequest.setCustomerId(1L);
        testRequest.setTechnicianId(2L);

        testAppointment = AppointmentJpaEntity.builder()
                .id(100L)
                .repairRequestId(10L)
                .customerId(1L)
                .technicianId(2L)
                .scheduledDate(LocalDate.now().plusDays(2))
                .scheduledTime(LocalTime.of(14, 0))
                .status(AppointmentStatus.CONFIRMED)
                .address("123 Phố Huế, Hà Nội")
                .notes("Sửa điều hòa")
                .build();

        createCmd = AppointmentUseCase.CreateAppointmentCommand.builder()
                .repairRequestId(10L)
                .technicianId(2L)
                .appointmentType("REPAIR")
                .scheduledDate(LocalDate.now().plusDays(3))
                .scheduledTime(LocalTime.of(9, 30))
                .address("123 Phố Huế, Hà Nội")
                .notes("Khảo sát")
                .build();
    }

    @Test
    @DisplayName("RC-47: Tạo lịch hẹn thành công (status = CONFIRMED)")
    void createAppointment_success() {
        when(requestRepo.findById(10L)).thenReturn(Optional.of(testRequest));
        when(appointmentRepo.save(any(AppointmentJpaEntity.class))).thenAnswer(invocation -> {
            AppointmentJpaEntity app = invocation.getArgument(0);
            app.setId(100L);
            return app;
        });

        AppointmentResponse res = appointmentService.createAppointment(createCmd, 1L, "CUSTOMER");

        assertThat(res).isNotNull();
        assertThat(res.getId()).isEqualTo(100L);
        assertThat(res.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        verify(appointmentRepo).save(any(AppointmentJpaEntity.class));
    }

    @Test
    @DisplayName("RC-47: Từ chối tạo lịch hẹn với ngày giờ trong quá khứ")
    void createAppointment_pastDate_throwsException() {
        createCmd.setScheduledDate(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> appointmentService.createAppointment(createCmd, 1L, "CUSTOMER"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Thời gian hẹn phải từ hiện tại trở đi");
    }

    @Test
    @DisplayName("RC-47: Từ chối tạo lịch hẹn khi yêu cầu sửa chữa không tồn tại")
    void createAppointment_requestNotFound_throwsException() {
        when(requestRepo.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.createAppointment(createCmd, 1L, "CUSTOMER"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("RC-47: Chặn người dùng không liên quan tạo lịch hẹn")
    void createAppointment_unauthorizedUser_throwsForbidden() {
        when(requestRepo.findById(10L)).thenReturn(Optional.of(testRequest));

        assertThatThrownBy(() -> appointmentService.createAppointment(createCmd, 999L, "CUSTOMER"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Bạn không có quyền tạo lịch hẹn cho yêu cầu này");
    }

    @Test
    @DisplayName("RC-47/RC-48: Dời lịch hẹn thành công (status = RESCHEDULED)")
    void rescheduleAppointment_success() {
        when(appointmentRepo.findById(100L)).thenReturn(Optional.of(testAppointment));
        when(appointmentRepo.save(any(AppointmentJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var rescheduleCmd = AppointmentUseCase.RescheduleAppointmentCommand.builder()
                .scheduledDate(LocalDate.now().plusDays(5))
                .scheduledTime(LocalTime.of(15, 0))
                .notes("Dời lịch do bận")
                .build();

        AppointmentResponse res = appointmentService.rescheduleAppointment(100L, rescheduleCmd, 1L, "CUSTOMER");

        assertThat(res).isNotNull();
        assertThat(res.getStatus()).isEqualTo(AppointmentStatus.RESCHEDULED);
        verify(appointmentRepo).save(testAppointment);
    }

    @Test
    @DisplayName("RC-47/RC-48: Không thể dời lịch hẹn đã bị hủy (CANCELLED)")
    void rescheduleAppointment_alreadyCancelled_throwsException() {
        testAppointment.setStatus(AppointmentStatus.CANCELLED);
        when(appointmentRepo.findById(100L)).thenReturn(Optional.of(testAppointment));

        var rescheduleCmd = AppointmentUseCase.RescheduleAppointmentCommand.builder()
                .scheduledDate(LocalDate.now().plusDays(5))
                .scheduledTime(LocalTime.of(15, 0))
                .build();

        assertThatThrownBy(() -> appointmentService.rescheduleAppointment(100L, rescheduleCmd, 1L, "CUSTOMER"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("RC-47/RC-48: Hoàn thành lịch hẹn thành công (status = COMPLETED)")
    void completeAppointment_success() {
        when(appointmentRepo.findById(100L)).thenReturn(Optional.of(testAppointment));
        when(appointmentRepo.save(any(AppointmentJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse res = appointmentService.completeAppointment(100L, "Đã sửa xong", 2L, "TECHNICIAN");

        assertThat(res).isNotNull();
        assertThat(res.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        verify(appointmentRepo).save(testAppointment);
    }

    @Test
    @DisplayName("RC-47/RC-48: Hủy lịch hẹn thành công (status = CANCELLED)")
    void cancelAppointment_success() {
        when(appointmentRepo.findById(100L)).thenReturn(Optional.of(testAppointment));
        when(appointmentRepo.save(any(AppointmentJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse res = appointmentService.cancelAppointment(100L, "Khách đi công tác", 1L, "CUSTOMER");

        assertThat(res).isNotNull();
        assertThat(res.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(appointmentRepo).save(testAppointment);
    }

    @Test
    @DisplayName("RC-47/RC-48: Hủy lịch hẹn thất bại khi không cung cấp lý do")
    void cancelAppointment_withoutReason_throwsException() {
        assertThatThrownBy(() -> appointmentService.cancelAppointment(100L, "   ", 1L, "CUSTOMER"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Lý do hủy cuộc hẹn không được để trống");
    }

    @Test
    @DisplayName("RC-47/RC-48 (Anti-IDOR): Người dùng không liên quan không được xem chi tiết lịch hẹn")
    void getAppointmentDetail_notAuthorized_throwsForbidden() {
        when(appointmentRepo.findById(100L)).thenReturn(Optional.of(testAppointment));

        assertThatThrownBy(() -> appointmentService.getAppointmentDetail(100L, 999L, "CUSTOMER"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Bạn không có quyền truy cập");
    }
}
