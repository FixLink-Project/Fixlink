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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service xử lý vòng đời cuộc hẹn (Jira RC-48: Appointment Status Lifecycle).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService implements AppointmentUseCase {

    private final SpringDataAppointmentRepository appointmentRepo;
    private final SpringDataRepairRequestRepository requestRepo;
    private final SpringDataCustomerProfileRepository customerProfileRepo;
    private final SpringDataTechnicianProfileRepository techProfileRepo;
    private final SpringDataQuotationRepository quotationRepo;

    @Override
    @Transactional
    public AppointmentResponse createAppointment(CreateAppointmentCommand cmd, Long actorId, String role) {
        validateDateTimeInFuture(cmd.getScheduledDate(), cmd.getScheduledTime());

        RepairRequestJpaEntity request = requestRepo.findById(cmd.getRepairRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu sửa chữa"));

        Long customerId = request.getCustomerId();
        Long technicianId = cmd.getTechnicianId();

        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isTechnicianRole = "TECHNICIAN".equalsIgnoreCase(role);

        if (isTechnicianRole) {
            // Khi Kỹ thuật viên tạo lịch hẹn, kỹ thuật viên phụ trách là chính người đang đăng nhập
            technicianId = actorId;
        } else if (technicianId == null) {
            technicianId = request.getTechnicianId();
        }

        if (technicianId == null) {
            throw new DomainException("INVALID_OPERATION", "Chưa xác định kỹ thuật viên cho cuộc hẹn này", 400);
        }

        boolean isCustomer = actorId != null && actorId.equals(customerId);
        boolean isTechnician = isTechnicianRole && (actorId != null && (actorId.equals(technicianId) || actorId.equals(request.getTechnicianId()) || request.getTechnicianId() == null));

        // Nếu đơn đã giao cho thợ khác, chặn thợ lạ không được can thiệp
        if (request.getTechnicianId() != null && !request.getTechnicianId().equals(technicianId) && !isAdmin && !isCustomer) {
            throw new DomainException("ACCESS_DENIED", "Yêu cầu này đã được gán cho một kỹ thuật viên khác", 403);
        }

        if (!isAdmin && !isCustomer && !isTechnician) {
            throw new DomainException("ACCESS_DENIED", "Bạn không có quyền tạo lịch hẹn cho yêu cầu này", 403);
        }

        // Nếu đơn chưa có thợ và thợ lên lịch hẹn với khách hàng, tự động gán thợ cho đơn
        if (request.getTechnicianId() == null && isTechnicianRole) {
            request.setTechnicianId(actorId);
            requestRepo.save(request);
        }

        String apptType = (cmd.getAppointmentType() != null && !cmd.getAppointmentType().isBlank())
                ? cmd.getAppointmentType().toUpperCase() : "REPAIR";
        String address = (cmd.getAddress() != null && !cmd.getAddress().isBlank())
                ? cmd.getAddress() : request.getAddress();

        AppointmentJpaEntity entity = AppointmentJpaEntity.builder()
                .repairRequestId(request.getId())
                .customerId(customerId)
                .technicianId(technicianId)
                .appointmentType(apptType)
                .scheduledDate(cmd.getScheduledDate())
                .scheduledTime(cmd.getScheduledTime())
                .address(address)
                .status(AppointmentStatus.CONFIRMED)
                .notes(cmd.getNotes())
                .build();
        entity.setCreatedBy(actorId);

        entity = appointmentRepo.save(entity);
        log.info("Created appointment #{} (type: {}) for request #{} between customer #{} and technician #{}",
                entity.getId(), apptType, request.getId(), customerId, technicianId);

        return toResponse(entity, request);
    }

    @Override
    @Transactional
    public AppointmentResponse rescheduleAppointment(Long appointmentId, RescheduleAppointmentCommand cmd, Long actorId, String role) {
        validateDateTimeInFuture(cmd.getScheduledDate(), cmd.getScheduledTime());

        AppointmentJpaEntity appointment = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch hẹn"));

        checkParticipantAccess(appointment, actorId, role);

        if (!appointment.getStatus().canTransitionTo(AppointmentStatus.RESCHEDULED)) {
            throw new DomainException("INVALID_OPERATION",
                    "Không thể dời lịch hẹn khi đang ở trạng thái: " + appointment.getStatus().getLabel(), 400);
        }

        appointment.setScheduledDate(cmd.getScheduledDate());
        appointment.setScheduledTime(cmd.getScheduledTime());
        appointment.setStatus(AppointmentStatus.RESCHEDULED);
        if (cmd.getNotes() != null && !cmd.getNotes().isBlank()) {
            appointment.setNotes(cmd.getNotes());
        }
        appointment.setUpdatedBy(actorId);

        appointment = appointmentRepo.save(appointment);
        log.info("Appointment #{} rescheduled to {} {} by actor #{}",
                appointment.getId(), appointment.getScheduledDate(), appointment.getScheduledTime(), actorId);

        return toResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse completeAppointment(Long appointmentId, String note, Long actorId, String role) {
        AppointmentJpaEntity appointment = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch hẹn"));

        checkParticipantAccess(appointment, actorId, role);

        if (!appointment.getStatus().canTransitionTo(AppointmentStatus.COMPLETED)) {
            throw new DomainException("INVALID_OPERATION",
                    "Không thể hoàn thành cuộc hẹn khi đang ở trạng thái: " + appointment.getStatus().getLabel(), 400);
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        if (appointment.getActualStartAt() == null) {
            appointment.setActualStartAt(appointment.getCreatedAt());
        }
        appointment.setActualEndAt(LocalDateTime.now());
        if (note != null && !note.isBlank()) {
            appointment.setNotes(note);
        }
        appointment.setUpdatedBy(actorId);

        appointment = appointmentRepo.save(appointment);
        log.info("Appointment #{} completed by actor #{}", appointment.getId(), actorId);

        return toResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse cancelAppointment(Long appointmentId, String reason, Long actorId, String role) {
        if (reason == null || reason.trim().isBlank()) {
            throw new DomainException("INVALID_OPERATION", "Lý do hủy cuộc hẹn không được để trống", 400);
        }

        AppointmentJpaEntity appointment = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch hẹn"));

        checkParticipantAccess(appointment, actorId, role);

        if (!appointment.getStatus().canTransitionTo(AppointmentStatus.CANCELLED)) {
            throw new DomainException("INVALID_OPERATION",
                    "Không thể hủy cuộc hẹn khi đang ở trạng thái: " + appointment.getStatus().getLabel(), 400);
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelReason(reason.trim());
        appointment.setUpdatedBy(actorId);

        appointment = appointmentRepo.save(appointment);
        log.info("Appointment #{} cancelled by actor #{} with reason: {}", appointment.getId(), actorId, reason);

        return toResponse(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentDetail(Long appointmentId, Long actorId, String role) {
        AppointmentJpaEntity appointment = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch hẹn"));

        checkParticipantAccess(appointment, actorId, role);
        return toResponse(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsForRequest(Long requestId, Long actorId, String role) {
        RepairRequestJpaEntity request = requestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu sửa chữa"));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isCustomer = actorId != null && actorId.equals(request.getCustomerId());
        boolean isTechnician = actorId != null && actorId.equals(request.getTechnicianId());

        if (!isAdmin && !isCustomer && !isTechnician) {
            boolean isQuotingTech = quotationRepo.existsByRequestIdAndTechnicianId(requestId, actorId);
            if (!isQuotingTech) {
                throw new DomainException("ACCESS_DENIED", "Bạn không có quyền xem lịch hẹn của yêu cầu này", 403);
            }
        }

        List<AppointmentJpaEntity> list = appointmentRepo.findByRepairRequestIdOrderByScheduledDateAscScheduledTimeAsc(requestId);
        return list.stream().map(a -> toResponse(a, request)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getMyAppointments(Long actorId, String role) {
        List<AppointmentJpaEntity> list;
        if ("TECHNICIAN".equalsIgnoreCase(role)) {
            list = appointmentRepo.findByTechnicianIdOrderByScheduledDateDescScheduledTimeDesc(actorId);
        } else {
            list = appointmentRepo.findByCustomerIdOrderByScheduledDateDescScheduledTimeDesc(actorId);
        }
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Helpers ──

    private void validateDateTimeInFuture(LocalDate date, LocalTime time) {
        if (date == null || time == null) {
            throw new DomainException("INVALID_OPERATION", "Ngày và giờ hẹn không được để trống", 400);
        }
        LocalDateTime scheduledDateTime = LocalDateTime.of(date, time);
        if (scheduledDateTime.isBefore(LocalDateTime.now().minusMinutes(2))) {
            throw new DomainException("INVALID_OPERATION", "Thời gian hẹn phải từ hiện tại trở đi", 400);
        }
    }

    private void checkParticipantAccess(AppointmentJpaEntity appointment, Long actorId, String role) {
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isCustomer = actorId != null && actorId.equals(appointment.getCustomerId());
        boolean isTechnician = actorId != null && actorId.equals(appointment.getTechnicianId());
        if (!isAdmin && !isCustomer && !isTechnician) {
            throw new DomainException("ACCESS_DENIED", "Bạn không có quyền truy cập hoặc chỉnh sửa lịch hẹn này", 403);
        }
    }

    private AppointmentResponse toResponse(AppointmentJpaEntity a) {
        RepairRequestJpaEntity req = requestRepo.findById(a.getRepairRequestId()).orElse(null);
        return toResponse(a, req);
    }

    private AppointmentResponse toResponse(AppointmentJpaEntity a, RepairRequestJpaEntity req) {
        String reqCode = req != null ? req.getRequestCode() : null;
        String reqTitle = req != null ? req.getTitle() : null;
        String reqAddress = req != null ? req.getAddress() : null;

        var cust = customerProfileRepo.findById(a.getCustomerId()).orElse(null);
        String custName = cust != null ? cust.getFullName() : null;
        String custPhone = cust != null ? cust.getPhone() : null;

        var tech = techProfileRepo.findById(a.getTechnicianId()).orElse(null);
        String techName = tech != null ? tech.getFullName() : null;
        String techPhone = tech != null ? tech.getPhone() : null;

        return AppointmentResponse.builder()
                .id(a.getId())
                .repairRequestId(a.getRepairRequestId())
                .requestCode(reqCode)
                .requestTitle(reqTitle)
                .requestAddress(reqAddress)
                .customerId(a.getCustomerId())
                .customerName(custName)
                .customerPhone(custPhone)
                .technicianId(a.getTechnicianId())
                .technicianName(techName)
                .technicianPhone(techPhone)
                .appointmentType(a.getAppointmentType())
                .scheduledDate(a.getScheduledDate())
                .scheduledTime(a.getScheduledTime())
                .actualStartAt(a.getActualStartAt())
                .actualEndAt(a.getActualEndAt())
                .status(a.getStatus())
                .statusLabel(a.getStatus() != null ? a.getStatus().getLabel() : null)
                .address(a.getAddress() != null ? a.getAddress() : reqAddress)
                .notes(a.getNotes())
                .cancelReason(a.getCancelReason())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
