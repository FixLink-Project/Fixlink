package com.fixlink.domain;

import com.fixlink.domain.exception.DomainException;
import com.fixlink.domain.model.Appointment;
import com.fixlink.domain.model.AppointmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Domain Test: Appointment Model (Feature RC-47)")
class AppointmentTest {

    private Appointment createValidAppointment() {
        return Appointment.builder()
                .id(1L)
                .customerId(2L)
                .technicianId(3L)
                .scheduledDate(LocalDate.now().plusDays(2))
                .scheduledTime(LocalTime.of(10, 0))
                .appointmentDateTime(LocalDateTime.now().plusDays(2).withHour(10).withMinute(0))
                .address("123 Cầu Giấy, Hà Nội")
                .notes("Kiểm tra và sửa chữa điều hòa không mát")
                .status(AppointmentStatus.CONFIRMED)
                .build();
    }

    @Test
    @DisplayName("Thẩm định thành công với dữ liệu hợp lệ")
    void validate_validAppointment_success() {
        Appointment app = createValidAppointment();
        app.validate();
        assertThat(app.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Thẩm định thất bại khi thiếu mã khách hàng")
    void validate_missingCustomerId_throwsException() {
        Appointment app = createValidAppointment();
        app.setCustomerId(null);

        assertThatThrownBy(app::validate)
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Mã khách hàng không được để trống");
    }

    @Test
    @DisplayName("Thẩm định thất bại khi thiếu mã kỹ thuật viên")
    void validate_missingTechnicianId_throwsException() {
        Appointment app = createValidAppointment();
        app.setTechnicianId(null);

        assertThatThrownBy(app::validate)
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Mã kỹ thuật viên không được để trống");
    }

    @Test
    @DisplayName("Thẩm định thất bại khi khách hàng tự đặt lịch với chính mình")
    void validate_selfAppointment_throwsException() {
        Appointment app = createValidAppointment();
        app.setTechnicianId(app.getCustomerId());

        assertThatThrownBy(app::validate)
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Khách hàng không thể tự đặt lịch hẹn với chính mình");
    }

    @Test
    @DisplayName("Thẩm định thất bại khi thời gian hẹn trong quá khứ")
    void validate_pastAppointmentTime_throwsException() {
        Appointment app = createValidAppointment();
        app.setScheduledDate(LocalDate.now().minusDays(1));
        app.setAppointmentDateTime(LocalDateTime.now().minusDays(1));

        assertThatThrownBy(app::validate)
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Thời gian hẹn phải ở trong tương lai");
    }

    @Test
    @DisplayName("Thẩm định thất bại khi địa chỉ để trống")
    void validate_blankAddress_throwsException() {
        Appointment app = createValidAppointment();
        app.setAddress("   ");

        assertThatThrownBy(app::validate)
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Địa chỉ cuộc hẹn không được để trống");
    }

    @Test
    @DisplayName("Thẩm định thất bại khi ghi chú để trống")
    void validate_blankNotes_throwsException() {
        Appointment app = createValidAppointment();
        app.setNotes("");

        assertThatThrownBy(app::validate)
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Mô tả công việc hoặc ghi chú không được để trống");
    }

    @Test
    @DisplayName("Khách hàng hủy cuộc hẹn thành công kèm lý do")
    void cancel_byCustomer_success() {
        Appointment app = createValidAppointment();
        app.cancel(2L, "Khách bận đột xuất", "ROLE_CUSTOMER");

        assertThat(app.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(app.getCancellationReason()).isEqualTo("Khách bận đột xuất");
        assertThat(app.getUpdatedBy()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Kỹ thuật viên hủy cuộc hẹn thành công")
    void cancel_byTechnician_success() {
        Appointment app = createValidAppointment();
        app.cancel(3L, "KTV có việc gia đình đột xuất", "ROLE_TECHNICIAN");

        assertThat(app.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(app.getCancellationReason()).isEqualTo("KTV có việc gia đình đột xuất");
        assertThat(app.getUpdatedBy()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Quản trị viên (Admin) hủy cuộc hẹn thành công")
    void cancel_byAdmin_success() {
        Appointment app = createValidAppointment();
        app.cancel(99L, "Quản trị viên hủy theo yêu cầu tổng đài", "ROLE_ADMIN");

        assertThat(app.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(app.getCancellationReason()).isEqualTo("Quản trị viên hủy theo yêu cầu tổng đài");
    }

    @Test
    @DisplayName("Người lạ không có quyền hủy cuộc hẹn")
    void cancel_byStranger_throwsException() {
        Appointment app = createValidAppointment();

        assertThatThrownBy(() -> app.cancel(999L, "Lý do hủy", "ROLE_CUSTOMER"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Bạn không có quyền hủy lịch hẹn này");
    }

    @Test
    @DisplayName("Hủy cuộc hẹn thất bại khi không cung cấp lý do")
    void cancel_withoutReason_throwsException() {
        Appointment app = createValidAppointment();

        assertThatThrownBy(() -> app.cancel(2L, "   ", "ROLE_CUSTOMER"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Vui lòng cung cấp lý do hủy lịch hẹn");
    }

    @Test
    @DisplayName("Không thể hủy cuộc hẹn đã bị hủy trước đó")
    void cancel_alreadyCancelled_throwsException() {
        Appointment app = createValidAppointment();
        app.setStatus(AppointmentStatus.CANCELLED);

        assertThatThrownBy(() -> app.cancel(2L, "Hủy lần hai", "ROLE_CUSTOMER"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Lịch hẹn này đã bị hủy trước đó");
    }

    @Test
    @DisplayName("Không thể hủy cuộc hẹn đã hoàn thành")
    void cancel_alreadyCompleted_throwsException() {
        Appointment app = createValidAppointment();
        app.setStatus(AppointmentStatus.COMPLETED);

        assertThatThrownBy(() -> app.cancel(2L, "Hủy cuộc hẹn", "ROLE_CUSTOMER"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Không thể hủy lịch hẹn đã hoàn thành");
    }

    @Test
    @DisplayName("Xác nhận cuộc hẹn cập nhật trạng thái CONFIRMED")
    void confirm_success() {
        Appointment app = createValidAppointment();
        app.confirm();
        assertThat(app.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Hoàn thành cuộc hẹn cập nhật trạng thái COMPLETED")
    void complete_success() {
        Appointment app = createValidAppointment();
        app.complete();
        assertThat(app.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }
}
