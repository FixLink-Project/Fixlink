package com.fixlink.application.service;

import com.fixlink.adapter.out.persistence.entity.AdditionalCostJpaEntity;
import com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity;
import com.fixlink.adapter.out.persistence.repository.SpringDataAdditionalCostRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataRepairRequestRepository;
import com.fixlink.domain.exception.DomainException;
import com.fixlink.domain.model.CostStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdditionalCostService {

    private final SpringDataAdditionalCostRepository costRepo;
    private final SpringDataRepairRequestRepository requestRepo;

    @Transactional
    public AdditionalCostJpaEntity addCost(Long requestId, Long technicianId,
                                           String description, BigDecimal amount) {
        RepairRequestJpaEntity request = findRequest(requestId);
        if (!technicianId.equals(request.getTechnicianId())) {
            throw new DomainException("COST_UNAUTHORIZED",
                    "Bạn không phải kỹ thuật viên phụ trách yêu cầu này.", 403);
        }

        var cost = AdditionalCostJpaEntity.builder()
                .requestId(requestId)
                .description(description)
                .amount(amount)
                .build();
        cost.setCreatedBy(technicianId);
        return costRepo.save(cost);
    }

    @Transactional
    public AdditionalCostJpaEntity approveCost(Long costId, Long customerId) {
        AdditionalCostJpaEntity cost = findCost(costId);
        verifyCustomerOwnership(cost, customerId);

        if (cost.getStatus() != CostStatus.PENDING) {
            throw new DomainException("COST_NOT_PENDING",
                    "Chi phí này đã được xử lý.", 409);
        }

        cost.setStatus(CostStatus.APPROVED);
        cost.setApprovedAt(LocalDateTime.now());
        cost.setUpdatedBy(customerId);
        return costRepo.save(cost);
    }

    @Transactional
    public AdditionalCostJpaEntity rejectCost(Long costId, Long customerId) {
        AdditionalCostJpaEntity cost = findCost(costId);
        verifyCustomerOwnership(cost, customerId);

        if (cost.getStatus() != CostStatus.PENDING) {
            throw new DomainException("COST_NOT_PENDING",
                    "Chi phí này đã được xử lý.", 409);
        }

        cost.setStatus(CostStatus.REJECTED);
        cost.setUpdatedBy(customerId);
        return costRepo.save(cost);
    }

    @Transactional(readOnly = true)
    public List<AdditionalCostJpaEntity> getCostsByRequest(Long requestId) {
        return costRepo.findByRequestIdAndDeletedAtIsNull(requestId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getApprovedTotalByRequest(Long requestId) {
        return costRepo.findByRequestIdAndStatusAndDeletedAtIsNull(requestId, CostStatus.APPROVED)
                .stream()
                .map(AdditionalCostJpaEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private AdditionalCostJpaEntity findCost(Long costId) {
        return costRepo.findById(costId)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new DomainException("COST_NOT_FOUND",
                        "Không tìm thấy chi phí phát sinh.", 404));
    }

    private RepairRequestJpaEntity findRequest(Long requestId) {
        return requestRepo.findById(requestId)
                .filter(r -> r.getDeletedAt() == null)
                .orElseThrow(() -> new DomainException("REQUEST_NOT_FOUND",
                        "Không tìm thấy yêu cầu sửa chữa.", 404));
    }

    private void verifyCustomerOwnership(AdditionalCostJpaEntity cost, Long customerId) {
        RepairRequestJpaEntity request = findRequest(cost.getRequestId());
        if (!request.getCustomerId().equals(customerId)) {
            throw new DomainException("COST_UNAUTHORIZED",
                    "Bạn không có quyền thao tác chi phí phát sinh cho yêu cầu này.", 403);
        }
    }
}
