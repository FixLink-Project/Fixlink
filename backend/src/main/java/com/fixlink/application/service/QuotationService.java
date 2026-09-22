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
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuotationService implements QuotationUseCase {

    private static final BigDecimal DEPOSIT_RATE = new BigDecimal("0.30");

    private final SpringDataQuotationRepository quotationRepo;
    private final SpringDataRepairRequestRepository requestRepo;
    private final SpringDataTechnicianProfileRepository techProfileRepo;
    private final RepairRequestService repairRequestService;

    @Override
    @Transactional
    public QuotationResponse create(Long requestId, CreateQuotationCommand cmd, Long technicianId) {
        RepairRequestJpaEntity request = findRequest(requestId);

        if (request.getStatus() != RequestStatus.BIDDING_OPEN) {
            throw new DomainException("INVALID_OPERATION", "Yêu cầu không ở trạng thái nhận báo giá");
        }
        if (request.getBiddingDeadline() != null && LocalDateTime.now().isAfter(request.getBiddingDeadline())) {
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

        if (quotationRepo.existsByRequestIdAndTechnicianId(requestId, technicianId)) {
            throw new DomainException("INVALID_OPERATION", "Bạn đã gửi báo giá cho yêu cầu này rồi (BR02)");
        }

        QuotationJpaEntity entity = new QuotationJpaEntity();
        entity.setRequestId(requestId);
        entity.setTechnicianId(technicianId);
        entity.setSolution(cmd.getSolution());
        entity.setPriceLaborVnd(cmd.getPriceLaborVnd());
        entity.setPriceMaterialsVnd(cmd.getPriceMaterialsVnd());
        entity.setInspectionTime(cmd.getInspectionTime());
        entity.setEstimatedFinish(cmd.getEstimatedFinish());
        entity.setNote(cmd.getNote());
        entity.setStatus(QuotationStatus.PENDING);
        entity.setCreatedBy(technicianId);

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
        if (request.getStatus() != RequestStatus.BIDDING_OPEN) {
            throw new DomainException("INVALID_OPERATION", "Yêu cầu không ở trạng thái nhận báo giá");
        }

        entity.setSolution(cmd.getSolution());
        entity.setPriceLaborVnd(cmd.getPriceLaborVnd());
        entity.setPriceMaterialsVnd(cmd.getPriceMaterialsVnd());
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
        RepairRequestJpaEntity request = findRequest(requestId);
        if (!request.getCustomerId().equals(customerId)) {
            throw new DomainException("ACCESS_DENIED", "Bạn không phải chủ yêu cầu này", 403);
        }

        return quotationRepo.findByRequestIdOrderByCreatedAtDesc(requestId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AcceptQuotationResponse accept(Long requestId, Long quotationId, Long customerId) {
        RepairRequestJpaEntity request = findRequest(requestId);
        if (!request.getCustomerId().equals(customerId)) {
            throw new DomainException("ACCESS_DENIED", "Bạn không phải chủ yêu cầu này", 403);
        }
        if (request.getStatus() != RequestStatus.BIDDING_OPEN) {
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
        request.setStatus(RequestStatus.MATCHED_AWAITING_DEPOSIT);
        request.setUpdatedBy(customerId);
        requestRepo.save(request);

        // 4. Work progress
        repairRequestService.recordProgress(requestId, oldStatus, RequestStatus.MATCHED_AWAITING_DEPOSIT,
                "Khách chọn thợ, chờ đặt cọc", customerId);

        // 5. Build response
        String techName = techProfileRepo.findById(chosen.getTechnicianId())
                .map(TechnicianProfileJpaEntity::getFullName).orElse(null);

        return AcceptQuotationResponse.builder()
                .requestId(requestId)
                .selectedQuotationId(quotationId)
                .technicianName(techName)
                .agreedPrice(agreedPrice)
                .depositAmount(deposit)
                .status(RequestStatus.MATCHED_AWAITING_DEPOSIT)
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
