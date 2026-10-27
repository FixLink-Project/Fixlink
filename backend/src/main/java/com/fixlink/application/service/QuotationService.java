package com.fixlink.application.service;

import com.fixlink.adapter.in.web.dto.response.AcceptQuotationResponse;
import com.fixlink.adapter.in.web.dto.response.QuotationResponse;
import com.fixlink.adapter.out.persistence.entity.QuotationJpaEntity;
import com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity;
import com.fixlink.adapter.out.persistence.entity.TechnicianProfileJpaEntity;
import com.fixlink.adapter.out.persistence.repository.*;
import com.fixlink.application.port.in.QuotationUseCase;
import com.fixlink.domain.exception.DomainException;
import com.fixlink.domain.exception.ResourceNotFoundException;
import com.fixlink.domain.model.QuotationStatus;
import com.fixlink.domain.model.RequestStatus;
import com.fixlink.domain.model.VerificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuotationService implements QuotationUseCase {

    private static final BigDecimal DEPOSIT_RATE = new BigDecimal("0.30");

    private final SpringDataQuotationRepository quotationRepo;
    private final SpringDataRepairRequestRepository requestRepo;
    private final SpringDataTechnicianProfileRepository techProfileRepo;
    private final RepairRequestService repairRequestService;

    // ==================================================================================
    // MÔ HÌNH MỚI: "ai nhận trước được trước" (first-come-first-served)
    // ==================================================================================

    /**
     * Thợ bấm "Nhận việc". Toàn bộ nằm trong một transaction và mở đầu bằng khóa
     * ghi bi quan trên chính bản ghi yêu cầu, nên hai thợ bấm cùng lúc sẽ bị DB
     * xếp hàng: người sau chỉ đọc được bản ghi khi người trước đã commit
     * (status đã là ASSIGNED) và bị chặn bằng JOB_ALREADY_TAKEN.
     */
    @Override
    @Transactional
    public AcceptQuotationResponse apply(Long requestId, Long technicianId) {
        // 1. Khóa trước, đọc sau — không được đọc bằng findById rồi mới khóa.
        RepairRequestJpaEntity request = requestRepo.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu sửa chữa"));

        // 2. Job đã bị thợ khác nhận -> 409 rõ ràng, không để lọt thành 500.
        if (request.getStatus() != RequestStatus.OPEN) {
            throw new DomainException("JOB_ALREADY_TAKEN",
                    "Yêu cầu này vừa được thợ khác nhận. Vui lòng xem các yêu cầu khác.", 409);
        }
        if (request.getApplyDeadline() != null && LocalDateTime.now().isAfter(request.getApplyDeadline())) {
            throw new DomainException("INVALID_OPERATION", "Yêu cầu này đã hết hạn nhận việc");
        }

        TechnicianProfileJpaEntity tech = techProfileRepo.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ thợ"));
        if (tech.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new DomainException("UNVERIFIED_TECHNICIAN", "Tài khoản chưa được xác minh", 403);
        }
        if (!tech.getCategoryIds().contains(request.getCategoryId())) {
            throw new DomainException("INVALID_OPERATION", "Chuyên môn của bạn không khớp với danh mục yêu cầu");
        }
        if (request.getAreaId() != null && !tech.getAreaIds().contains(request.getAreaId())) {
            throw new DomainException("INVALID_OPERATION", "Khu vực của bạn không khớp với địa bàn yêu cầu");
        }
        if (quotationRepo.existsByRequestIdAndTechnicianId(requestId, technicianId)) {
            throw new DomainException("INVALID_OPERATION", "Bạn đã nhận yêu cầu này rồi");
        }

        // 3. Giá = ngân sách khách ấn định, thợ không nhập giá nữa.
        BigDecimal agreedPrice = request.getBudgetRef() != null ? request.getBudgetRef() : BigDecimal.ZERO;
        BigDecimal deposit = agreedPrice.multiply(DEPOSIT_RATE).setScale(0, RoundingMode.CEILING);
        LocalDateTime now = LocalDateTime.now();

        // 4. Bản ghi "nhận việc" — vẫn lưu ở bảng quotations nhưng đổi vai trò.
        QuotationJpaEntity record = new QuotationJpaEntity();
        record.setRequestId(requestId);
        record.setTechnicianId(technicianId);
        record.setSolution("Thợ nhận việc theo ngân sách khách đưa ra");
        record.setPriceLaborVnd(agreedPrice);
        record.setPriceMaterialsVnd(BigDecimal.ZERO);
        record.setStatus(QuotationStatus.ACCEPTED);
        record.setAcceptedAt(now);
        record.setCreatedBy(technicianId);
        record = quotationRepo.save(record);

        // 5. Gán thợ vào yêu cầu.
        RequestStatus oldStatus = request.getStatus();
        request.setTechnicianId(technicianId);
        request.setSelectedQuotationId(record.getId());
        request.setAgreedPrice(agreedPrice);
        request.setDepositAmount(deposit);
        request.setStatus(RequestStatus.ASSIGNED);
        request.setUpdatedBy(technicianId);
        requestRepo.save(request);

        repairRequestService.recordProgress(requestId, oldStatus, RequestStatus.ASSIGNED,
                "Thợ đã nhận việc", technicianId);

        return AcceptQuotationResponse.builder()
                .requestId(requestId)
                .selectedQuotationId(record.getId())
                .technicianName(tech.getFullName())
                .agreedPrice(agreedPrice)
                .depositAmount(deposit)
                .status(RequestStatus.ASSIGNED)
                .build();
    }

    // ==================================================================================
    // ĐẤU GIÁ NGƯỢC & BÁO GIÁ CẠNH TRANH (Bidding & Quotations)
    // ==================================================================================

    @Override
    @Transactional
    public QuotationResponse create(Long requestId, CreateQuotationCommand cmd, Long technicianId) {
        RepairRequestJpaEntity request = findRequest(requestId);

        if (request.getStatus() != RequestStatus.OPEN && request.getStatus() != RequestStatus.BIDDING_OPEN) {
            throw new DomainException("INVALID_OPERATION", "Yêu cầu không ở trạng thái nhận báo giá");
        }
        if (request.getApplyDeadline() != null && LocalDateTime.now().isAfter(request.getApplyDeadline())) {
            throw new DomainException("INVALID_OPERATION", "Đã hết hạn nhận báo giá cho yêu cầu này");
        }

        TechnicianProfileJpaEntity tech = techProfileRepo.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ thợ"));

        if (tech.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new DomainException("UNVERIFIED_TECHNICIAN", "Tài khoản chưa được xác minh", 403);
        }

        if (!tech.getCategoryIds().contains(request.getCategoryId())) {
            throw new DomainException("INVALID_OPERATION", "Chuyên môn của bạn không khớp với danh mục yêu cầu");
        }
        if (request.getAreaId() != null && !tech.getAreaIds().contains(request.getAreaId())) {
            throw new DomainException("INVALID_OPERATION", "Khu vực của bạn không khớp với địa bàn yêu cầu");
        }

        String solution = (cmd.getSolution() != null && !cmd.getSolution().isBlank())
                ? cmd.getSolution()
                : "Báo giá sửa chữa theo yêu cầu";
        BigDecimal priceMaterials = cmd.getPriceMaterialsVnd() != null ? cmd.getPriceMaterialsVnd() : BigDecimal.ZERO;

        BigDecimal totalQuoted = (cmd.getPriceLaborVnd() != null ? cmd.getPriceLaborVnd() : BigDecimal.ZERO)
                .add(priceMaterials);
        if (request.getBudgetRef() != null && request.getBudgetRef().compareTo(BigDecimal.ZERO) > 0
                && totalQuoted.compareTo(request.getBudgetRef()) > 0) {
            throw new DomainException("PRICE_EXCEEDS_BUDGET",
                    "Báo giá (" + totalQuoted + " VNĐ) không được vượt quá trần ngân sách (" + request.getBudgetRef() + " VNĐ)", 400);
        }

        var existingOpt = quotationRepo.findByRequestIdAndTechnicianId(requestId, technicianId);
        QuotationJpaEntity entity;
        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
            if (entity.getStatus() == QuotationStatus.ACCEPTED) {
                throw new DomainException("INVALID_OPERATION", "Báo giá của bạn đã được khách hàng chấp nhận");
            }
            entity.setSolution(solution);
            entity.setPriceLaborVnd(cmd.getPriceLaborVnd());
            entity.setPriceMaterialsVnd(priceMaterials);
            entity.setInspectionTime(cmd.getInspectionTime());
            entity.setEstimatedFinish(cmd.getEstimatedFinish());
            entity.setNote(cmd.getNote());
            entity.setStatus(QuotationStatus.PENDING);
            entity.setUpdatedBy(technicianId);
        } else {
            entity = new QuotationJpaEntity();
            entity.setRequestId(requestId);
            entity.setTechnicianId(technicianId);
            entity.setSolution(solution);
            entity.setPriceLaborVnd(cmd.getPriceLaborVnd());
            entity.setPriceMaterialsVnd(priceMaterials);
            entity.setInspectionTime(cmd.getInspectionTime());
            entity.setEstimatedFinish(cmd.getEstimatedFinish());
            entity.setNote(cmd.getNote());
            entity.setStatus(QuotationStatus.PENDING);
            entity.setCreatedBy(technicianId);
        }

        entity = quotationRepo.save(entity);
        return toResponse(entity);
    }

    @Override
    @Transactional
    public QuotationResponse update(Long requestId, Long quotationId, UpdateQuotationCommand cmd, Long technicianId) {
        QuotationJpaEntity entity = findOwnedQuotation(quotationId, technicianId);

        if (!entity.getRequestId().equals(requestId)) {
            throw new ResourceNotFoundException("Báo giá không thuộc yêu cầu này");
        }
        if (entity.getStatus() != QuotationStatus.PENDING) {
            throw new DomainException("INVALID_OPERATION", "Chỉ sửa được báo giá đang chờ duyệt");
        }

        RepairRequestJpaEntity request = findRequest(requestId);
        if (request.getStatus() != RequestStatus.OPEN && request.getStatus() != RequestStatus.BIDDING_OPEN) {
            throw new DomainException("INVALID_OPERATION", "Yêu cầu không ở trạng thái nhận báo giá");
        }

        String solution = (cmd.getSolution() != null && !cmd.getSolution().isBlank())
                ? cmd.getSolution()
                : (entity.getSolution() != null ? entity.getSolution() : "Báo giá sửa chữa theo yêu cầu");
        BigDecimal priceMaterials = cmd.getPriceMaterialsVnd() != null
                ? cmd.getPriceMaterialsVnd()
                : (entity.getPriceMaterialsVnd() != null ? entity.getPriceMaterialsVnd() : BigDecimal.ZERO);

        BigDecimal totalQuoted = (cmd.getPriceLaborVnd() != null ? cmd.getPriceLaborVnd() : BigDecimal.ZERO)
                .add(priceMaterials);
        if (request.getBudgetRef() != null && request.getBudgetRef().compareTo(BigDecimal.ZERO) > 0
                && totalQuoted.compareTo(request.getBudgetRef()) > 0) {
            throw new DomainException("PRICE_EXCEEDS_BUDGET",
                    "Báo giá (" + totalQuoted + " VNĐ) không được vượt quá trần ngân sách (" + request.getBudgetRef() + " VNĐ)", 400);
        }

        entity.setSolution(solution);
        entity.setPriceLaborVnd(cmd.getPriceLaborVnd());
        entity.setPriceMaterialsVnd(priceMaterials);
        entity.setInspectionTime(cmd.getInspectionTime());
        entity.setEstimatedFinish(cmd.getEstimatedFinish());
        entity.setNote(cmd.getNote());
        entity.setUpdatedBy(technicianId);

        entity = quotationRepo.save(entity);
        return toResponse(entity);
    }

    @Override
    @Transactional
    public void withdraw(Long requestId, Long quotationId, Long technicianId) {
        QuotationJpaEntity entity = findOwnedQuotation(quotationId, technicianId);

        if (!entity.getRequestId().equals(requestId)) {
            throw new ResourceNotFoundException("Báo giá không thuộc yêu cầu này");
        }
        if (entity.getStatus() != QuotationStatus.PENDING) {
            throw new DomainException("INVALID_OPERATION", "Chỉ rút được báo giá đang chờ duyệt");
        }

        entity.setStatus(QuotationStatus.WITHDRAWN);
        entity.setUpdatedBy(technicianId);
        quotationRepo.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuotationResponse> getQuotationsForRequest(Long requestId, Long customerId) {
        return getQuotationsForRequest(requestId, customerId, "CUSTOMER");
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuotationResponse> getQuotationsForRequest(Long requestId, Long userId, String role) {
        RepairRequestJpaEntity request = findRequest(requestId);
        boolean isOwner = request.getCustomerId().equals(userId);
        boolean isAdmin = "ADMIN".equals(role) || "STAFF".equals(role);
        boolean isTechnician = "TECHNICIAN".equals(role);

        if (isOwner || isAdmin) {
            boolean canSwitch = List.of(
                    RequestStatus.ASSIGNED,
                    RequestStatus.DEPOSIT_PAID,
                    RequestStatus.INSPECTING,
                    RequestStatus.AWAITING_COST_APPROVAL
            ).contains(request.getStatus());

            return quotationRepo.findByRequestIdOrderByCreatedAtDesc(requestId).stream()
                    .map(q -> {
                        QuotationResponse resp = toResponse(q);
                        boolean isOtherTech = !q.getTechnicianId().equals(request.getTechnicianId());
                        resp.setIsSwitchable(canSwitch && isOtherTech && q.getStatus() != QuotationStatus.WITHDRAWN);
                        return resp;
                    })
                    .toList();
        } else if (isTechnician) {
            List<QuotationJpaEntity> allQuotes = quotationRepo.findByRequestIdOrderByCreatedAtDesc(requestId);
            List<QuotationResponse> result = new ArrayList<>();
            int anonIndex = 1;
            for (QuotationJpaEntity q : allQuotes) {
                if (q.getTechnicianId().equals(userId)) {
                    QuotationResponse mine = toResponse(q);
                    mine.setIsMine(true);
                    result.add(mine);
                } else {
                    BigDecimal total = (q.getPriceLaborVnd() != null ? q.getPriceLaborVnd() : BigDecimal.ZERO)
                            .add(q.getPriceMaterialsVnd() != null ? q.getPriceMaterialsVnd() : BigDecimal.ZERO);
                    result.add(QuotationResponse.builder()
                            .id(q.getId())
                            .requestId(q.getRequestId())
                            .technicianId(null)
                            .technicianName("Thợ ẩn danh #" + anonIndex++)
                            .priceLaborVnd(q.getPriceLaborVnd())
                            .priceMaterialsVnd(q.getPriceMaterialsVnd())
                            .totalPrice(total)
                            .solution(q.getSolution())
                            .status(q.getStatus())
                            .createdAt(q.getCreatedAt())
                            .isMine(false)
                            .build());
                }
            }
            return result;
        } else {
            throw new DomainException("ACCESS_DENIED", "Bạn không có quyền xem báo giá của yêu cầu này", 403);
        }
    }

    @Override
    @Transactional
    public AcceptQuotationResponse accept(Long requestId, Long quotationId, Long customerId) {
        RepairRequestJpaEntity request = findRequest(requestId);
        if (!request.getCustomerId().equals(customerId)) {
            throw new DomainException("ACCESS_DENIED", "Bạn không phải chủ yêu cầu này", 403);
        }
        if (request.getStatus() != RequestStatus.OPEN && request.getStatus() != RequestStatus.BIDDING_OPEN) {
            throw new DomainException("INVALID_OPERATION", "Yêu cầu không ở trạng thái nhận báo giá");
        }

        QuotationJpaEntity chosen = quotationRepo.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo giá"));
        if (!chosen.getRequestId().equals(requestId)) {
            throw new ResourceNotFoundException("Báo giá không thuộc yêu cầu này");
        }
        if (chosen.getStatus() != QuotationStatus.PENDING) {
            throw new DomainException("INVALID_OPERATION", "Báo giá này không ở trạng thái chờ duyệt");
        }

        // 1. Accept chosen
        chosen.setStatus(QuotationStatus.ACCEPTED);
        chosen.setAcceptedAt(LocalDateTime.now());
        chosen.setUpdatedBy(customerId);
        quotationRepo.save(chosen);

        // 2. Reject all others
        List<QuotationJpaEntity> others = quotationRepo.findByRequestIdAndStatus(requestId, QuotationStatus.PENDING);
        for (QuotationJpaEntity other : others) {
            if (!other.getId().equals(quotationId)) {
                other.setStatus(QuotationStatus.REJECTED);
                other.setUpdatedBy(customerId);
                quotationRepo.save(other);
            }
        }

        // 3. Update request
        BigDecimal agreedPrice = (chosen.getPriceLaborVnd() != null ? chosen.getPriceLaborVnd() : BigDecimal.ZERO)
                .add(chosen.getPriceMaterialsVnd() != null ? chosen.getPriceMaterialsVnd() : BigDecimal.ZERO);
        BigDecimal deposit = agreedPrice.multiply(DEPOSIT_RATE).setScale(0, RoundingMode.CEILING);

        RequestStatus oldStatus = request.getStatus();
        request.setSelectedQuotationId(quotationId);
        request.setTechnicianId(chosen.getTechnicianId());
        request.setAgreedPrice(agreedPrice);
        request.setDepositAmount(deposit);
        request.setStatus(RequestStatus.ASSIGNED);
        request.setUpdatedBy(customerId);
        requestRepo.save(request);

        // 4. Work progress
        String techName = techProfileRepo.findById(chosen.getTechnicianId())
                .map(TechnicianProfileJpaEntity::getFullName).orElse("Thợ");

        repairRequestService.recordProgress(requestId, oldStatus, RequestStatus.ASSIGNED,
                "Khách hàng đã chọn báo giá của thợ " + techName, customerId);

        return AcceptQuotationResponse.builder()
                .requestId(requestId)
                .selectedQuotationId(quotationId)
                .technicianName(techName)
                .agreedPrice(agreedPrice)
                .depositAmount(deposit)
                .status(RequestStatus.ASSIGNED)
                .build();
    }

    @Override
    @Transactional
    public AcceptQuotationResponse switchTechnician(Long requestId, Long quotationId, Long customerId) {
        RepairRequestJpaEntity request = findRequest(requestId);
        if (!request.getCustomerId().equals(customerId)) {
            throw new DomainException("ACCESS_DENIED", "Bạn không phải chủ yêu cầu này", 403);
        }

        // Nhóm trạng thái cho phép đổi thợ (chưa hoàn thành thi công)
        List<RequestStatus> switchableStatuses = List.of(
                RequestStatus.ASSIGNED,
                RequestStatus.DEPOSIT_PAID,
                RequestStatus.INSPECTING,
                RequestStatus.AWAITING_COST_APPROVAL
        );
        if (!switchableStatuses.contains(request.getStatus())) {
            throw new DomainException("INVALID_OPERATION",
                    "Không thể đổi thợ khi đơn đang ở trạng thái: " + request.getStatus());
        }

        QuotationJpaEntity targetQuote = quotationRepo.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo giá của thợ muốn đổi"));
        if (!targetQuote.getRequestId().equals(requestId)) {
            throw new ResourceNotFoundException("Báo giá không thuộc yêu cầu này");
        }
        if (targetQuote.getTechnicianId().equals(request.getTechnicianId())) {
            throw new DomainException("INVALID_OPERATION", "Thợ này đang là thợ phụ trách yêu cầu hiện tại");
        }
        if (targetQuote.getStatus() == QuotationStatus.WITHDRAWN) {
            throw new DomainException("INVALID_OPERATION", "Báo giá này đã bị rút lại");
        }

        // Cập nhật báo giá của thợ cũ thành REJECTED (nếu có)
        if (request.getSelectedQuotationId() != null) {
            quotationRepo.findById(request.getSelectedQuotationId()).ifPresent(oldQuote -> {
                oldQuote.setStatus(QuotationStatus.REJECTED);
                oldQuote.setUpdatedBy(customerId);
                quotationRepo.save(oldQuote);
            });
        }

        // Cập nhật báo giá mới thành ACCEPTED
        targetQuote.setStatus(QuotationStatus.ACCEPTED);
        targetQuote.setAcceptedAt(LocalDateTime.now());
        targetQuote.setUpdatedBy(customerId);
        quotationRepo.save(targetQuote);

        BigDecimal agreedPrice = (targetQuote.getPriceLaborVnd() != null ? targetQuote.getPriceLaborVnd() : BigDecimal.ZERO)
                .add(targetQuote.getPriceMaterialsVnd() != null ? targetQuote.getPriceMaterialsVnd() : BigDecimal.ZERO);
        BigDecimal deposit = agreedPrice.multiply(DEPOSIT_RATE).setScale(0, RoundingMode.CEILING);

        RequestStatus oldStatus = request.getStatus();
        // Nếu đã thanh toán cọc thì giữ ở DEPOSIT_PAID để thợ mới nhận việc và bắt đầu khảo sát
        RequestStatus newStatus = (oldStatus == RequestStatus.ASSIGNED) ? RequestStatus.ASSIGNED : RequestStatus.DEPOSIT_PAID;

        request.setSelectedQuotationId(quotationId);
        request.setTechnicianId(targetQuote.getTechnicianId());
        request.setAgreedPrice(agreedPrice);
        request.setDepositAmount(deposit);
        request.setStatus(newStatus);
        request.setUpdatedBy(customerId);
        requestRepo.save(request);

        String techName = techProfileRepo.findById(targetQuote.getTechnicianId())
                .map(TechnicianProfileJpaEntity::getFullName).orElse("Thợ mới");

        repairRequestService.recordProgress(requestId, oldStatus, newStatus,
                "Khách hàng đã đổi sang thợ " + techName, customerId);

        return AcceptQuotationResponse.builder()
                .requestId(requestId)
                .selectedQuotationId(quotationId)
                .technicianName(techName)
                .agreedPrice(agreedPrice)
                .depositAmount(deposit)
                .status(newStatus)
                .build();
    }

    // ── helpers ──

    private RepairRequestJpaEntity findRequest(Long requestId) {
        return requestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu sửa chữa"));
    }

    private QuotationJpaEntity findOwnedQuotation(Long quotationId, Long technicianId) {
        QuotationJpaEntity entity = quotationRepo.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo giá"));
        if (!entity.getTechnicianId().equals(technicianId)) {
            throw new DomainException("ACCESS_DENIED", "Bạn không phải chủ báo giá này", 403);
        }
        return entity;
    }

    private QuotationResponse toResponse(QuotationJpaEntity q) {
        String techName = null;
        BigDecimal avgRating = null;
        Integer completedJobs = null;
        Integer yearsExp = null;

        var techOpt = techProfileRepo.findById(q.getTechnicianId());
        if (techOpt.isPresent()) {
            var tp = techOpt.get();
            techName = tp.getFullName();
            avgRating = tp.getAvgRating();
            completedJobs = tp.getCompletedJobs();
            yearsExp = tp.getYearsExperience();
        }

        BigDecimal total = (q.getPriceLaborVnd() != null ? q.getPriceLaborVnd() : BigDecimal.ZERO)
                .add(q.getPriceMaterialsVnd() != null ? q.getPriceMaterialsVnd() : BigDecimal.ZERO);

        return QuotationResponse.builder()
                .id(q.getId())
                .requestId(q.getRequestId())
                .technicianId(q.getTechnicianId())
                .technicianName(techName)
                .avgRating(avgRating)
                .completedJobs(completedJobs)
                .yearsExperience(yearsExp)
                .solution(q.getSolution())
                .priceLaborVnd(q.getPriceLaborVnd())
                .priceMaterialsVnd(q.getPriceMaterialsVnd())
                .totalPrice(total)
                .inspectionTime(q.getInspectionTime())
                .estimatedFinish(q.getEstimatedFinish())
                .note(q.getNote())
                .status(q.getStatus())
                .createdAt(q.getCreatedAt())
                .build();
    }
}
