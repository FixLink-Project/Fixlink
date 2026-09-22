package com.fixlink.application.service;

import com.fixlink.adapter.in.web.dto.response.*;
import com.fixlink.adapter.out.persistence.entity.*;
import com.fixlink.adapter.out.persistence.repository.*;
import com.fixlink.application.port.in.RepairRequestUseCase;
import com.fixlink.domain.exception.DomainException;
import com.fixlink.domain.exception.ResourceNotFoundException;
import com.fixlink.domain.model.RequestStatus;
import com.fixlink.domain.model.VerificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RepairRequestService implements RepairRequestUseCase {

    private final SpringDataRepairRequestRepository requestRepo;
    private final SpringDataMediaRepository mediaRepo;
    private final SpringDataWorkProgressRepository progressRepo;
    private final SpringDataQuotationRepository quotationRepo;
    private final SpringDataServiceCategoryRepository categoryRepo;
    private final SpringDataServiceAreaRepository areaRepo;
    private final SpringDataTechnicianProfileRepository techProfileRepo;

    @Override
    @Transactional
    public RepairRequestResponse create(CreateCommand cmd, Long customerId) {
        categoryRepo.findById(cmd.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục dịch vụ"));

        if (cmd.getAreaId() != null) {
            areaRepo.findById(cmd.getAreaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khu vực"));
        }

        RepairRequestJpaEntity entity = new RepairRequestJpaEntity();
        entity.setRequestCode(generateRequestCode());
        entity.setCustomerId(customerId);
        entity.setCategoryId(cmd.getCategoryId());
        entity.setAreaId(cmd.getAreaId());
        entity.setTitle(cmd.getTitle());
        entity.setDescription(cmd.getDescription());
        entity.setAddress(cmd.getAddressLine());
        entity.setLatitude(cmd.getLatitude());
        entity.setLongitude(cmd.getLongitude());
        entity.setRequestedTime(cmd.getPreferredTime() != null ? cmd.getPreferredTime() : LocalDateTime.now());
        entity.setBudgetRef(cmd.getBudgetRef() != null ? cmd.getBudgetRef() : BigDecimal.ZERO);
        entity.setStatus(RequestStatus.BIDDING_OPEN);

        int deadlineDays = cmd.getBiddingDeadlineDays() != null ? cmd.getBiddingDeadlineDays() : 3;
        entity.setBiddingDeadline(LocalDateTime.now().plusDays(deadlineDays));

        entity.setCreatedBy(customerId);
        entity = requestRepo.save(entity);

        saveMedia(entity.getId(), cmd.getMediaUrls(), customerId);

        recordProgress(entity.getId(), null, RequestStatus.BIDDING_OPEN, "Khách tạo yêu cầu mới", customerId);

        return toResponse(entity);
    }

    @Override
    @Transactional
    public RepairRequestResponse update(Long requestId, UpdateCommand cmd, Long customerId) {
        RepairRequestJpaEntity entity = findOwnedRequest(requestId, customerId);

        if (entity.getStatus() != RequestStatus.DRAFT && entity.getStatus() != RequestStatus.BIDDING_OPEN) {
            throw new DomainException("INVALID_OPERATION", "Chỉ có thể sửa đơn ở trạng thái Nháp hoặc Đang nhận báo giá");
        }

        if (entity.getStatus() == RequestStatus.BIDDING_OPEN) {
            long quoteCount = quotationRepo.countByRequestId(requestId);
            if (quoteCount > 0) {
                throw new DomainException("INVALID_OPERATION", "Không thể sửa đơn khi đã có thợ gửi báo giá");
            }
        }

        entity.setTitle(cmd.getTitle());
        entity.setDescription(cmd.getDescription());
        entity.setCategoryId(cmd.getCategoryId());
        entity.setAreaId(cmd.getAreaId());
        entity.setAddress(cmd.getAddressLine());
        entity.setLatitude(cmd.getLatitude());
        entity.setLongitude(cmd.getLongitude());
        if (cmd.getPreferredTime() != null) entity.setRequestedTime(cmd.getPreferredTime());
        entity.setBudgetRef(cmd.getBudgetRef() != null ? cmd.getBudgetRef() : BigDecimal.ZERO);

        if (cmd.getBiddingDeadlineDays() != null) {
            entity.setBiddingDeadline(LocalDateTime.now().plusDays(cmd.getBiddingDeadlineDays()));
        }

        entity.setUpdatedBy(customerId);
        entity = requestRepo.save(entity);

        if (cmd.getMediaUrls() != null) {
            mediaRepo.deleteByOwnerTypeAndOwnerId("REPAIR_REQUEST", requestId);
            saveMedia(requestId, cmd.getMediaUrls(), customerId);
        }

        return toResponse(entity);
    }

    @Override
    @Transactional
    public RepairRequestResponse cancel(Long requestId, String reason, Long customerId) {
        RepairRequestJpaEntity entity = findOwnedRequest(requestId, customerId);

        if (entity.getStatus() == RequestStatus.IN_PROGRESS
                || entity.getStatus() == RequestStatus.AWAITING_ACCEPTANCE
                || entity.getStatus() == RequestStatus.COMPLETED
                || entity.getStatus() == RequestStatus.CANCELLED) {
            throw new DomainException("INVALID_OPERATION",
                    "Không thể hủy đơn ở trạng thái hiện tại (BR07)");
        }

        RequestStatus oldStatus = entity.getStatus();
        entity.setStatus(RequestStatus.CANCELLED);
        entity.setCancelReason(reason);
        entity.setUpdatedBy(customerId);
        entity = requestRepo.save(entity);

        recordProgress(requestId, oldStatus, RequestStatus.CANCELLED, "Khách hủy: " + reason, customerId);

        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RepairRequestResponse> getMyRequests(Long customerId, String status,
                                                      int page, int limit,
                                                      String sortBy, String sortOrder) {
        Sort sort = buildSort(sortBy, sortOrder);
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), limit, sort);

        Page<RepairRequestJpaEntity> pageResult;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            RequestStatus rs = RequestStatus.valueOf(status.toUpperCase());
            pageResult = requestRepo.findByCustomerIdAndStatusAndDeletedAtIsNull(customerId, rs, pageable);
        } else {
            pageResult = requestRepo.findByCustomerIdAndDeletedAtIsNull(customerId, pageable);
        }

        return pageResult.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RepairRequestDetailResponse getDetail(Long requestId, Long userId, String role) {
        RepairRequestJpaEntity entity = requestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu sửa chữa"));

        boolean isOwner = entity.getCustomerId().equals(userId);
        boolean isAdmin = "ADMIN".equals(role) || "STAFF".equals(role);
        boolean isTechWithQuotation = "TECHNICIAN".equals(role)
                && quotationRepo.existsByRequestIdAndTechnicianId(requestId, userId);

        if (!isOwner && !isAdmin && !isTechWithQuotation) {
            throw new DomainException("ACCESS_DENIED", "Bạn không có quyền xem yêu cầu này", 403);
        }

        List<QuotationResponse> quotations = Collections.emptyList();
        if (isOwner || isAdmin) {
            quotations = quotationRepo.findByRequestIdOrderByCreatedAtDesc(requestId).stream()
                    .map(this::toQuotationResponse)
                    .toList();
        } else if (isTechWithQuotation) {
            quotations = quotationRepo.findByRequestIdAndTechnicianId(requestId, userId)
                    .map(this::toQuotationResponse)
                    .stream().toList();
        }

        List<WorkProgressResponse> progress = progressRepo.findByRequestIdOrderByCreatedAtAsc(requestId).stream()
                .map(wp -> WorkProgressResponse.builder()
                        .id(wp.getId())
                        .fromStatus(wp.getFromStatus())
                        .toStatus(wp.getToStatus())
                        .note(wp.getNote())
                        .createdAt(wp.getCreatedAt())
                        .build())
                .toList();

        return RepairRequestDetailResponse.builder()
                .request(toResponse(entity))
                .quotations(quotations)
                .workProgress(progress)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RepairRequestResponse> getMatchingForTechnician(Long technicianId, int page, int limit,
                                                                 String search, String sortBy, String sortOrder) {
        TechnicianProfileJpaEntity profile = techProfileRepo.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ thợ"));

        if (profile.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new DomainException("UNVERIFIED_TECHNICIAN",
                    "Tài khoản chưa được xác minh. Hãy hoàn tất KYC trước.", 403);
        }

        Set<Long> categoryIds = profile.getCategoryIds();
        Set<Long> areaIds = profile.getAreaIds();

        if (categoryIds.isEmpty() || areaIds.isEmpty()) {
            return Page.empty();
        }

        Sort sort = buildSort(sortBy, sortOrder);
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), limit, sort);

        return requestRepo.findMatchingForTechnician(categoryIds, areaIds, technicianId, pageable)
                .map(this::toResponse);
    }

    // ── helpers ──

    private RepairRequestJpaEntity findOwnedRequest(Long requestId, Long customerId) {
        RepairRequestJpaEntity entity = requestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu sửa chữa"));
        if (!entity.getCustomerId().equals(customerId)) {
            throw new DomainException("ACCESS_DENIED", "Bạn không phải chủ yêu cầu này", 403);
        }
        return entity;
    }

    private RepairRequestResponse toResponse(RepairRequestJpaEntity e) {
        List<String> urls = mediaRepo.findByOwnerTypeAndOwnerId("REPAIR_REQUEST", e.getId())
                .stream().map(MediaJpaEntity::getUrl).toList();

        String categoryName = categoryRepo.findById(e.getCategoryId())
                .map(ServiceCategoryJpaEntity::getName).orElse(null);
        String areaName = e.getAreaId() != null
                ? areaRepo.findById(e.getAreaId()).map(ServiceAreaJpaEntity::getName).orElse(null)
                : null;

        int quoteCount = (int) quotationRepo.countByRequestId(e.getId());

        return RepairRequestResponse.builder()
                .id(e.getId())
                .requestCode(e.getRequestCode())
                .customerId(e.getCustomerId())
                .technicianId(e.getTechnicianId())
                .categoryId(e.getCategoryId())
                .categoryName(categoryName)
                .areaId(e.getAreaId())
                .areaName(areaName)
                .status(e.getStatus())
                .title(e.getTitle())
                .description(e.getDescription())
                .addressLine(e.getAddress())
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())
                .preferredTime(e.getRequestedTime())
                .agreedPrice(e.getAgreedPrice())
                .depositAmount(e.getDepositAmount())
                .budgetRef(e.getBudgetRef())
                .biddingDeadline(e.getBiddingDeadline())
                .cancelReason(e.getCancelReason())
                .selectedQuotationId(e.getSelectedQuotationId())
                .mediaUrls(urls)
                .quotationCount(quoteCount)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private QuotationResponse toQuotationResponse(QuotationJpaEntity q) {
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

    private void saveMedia(Long requestId, List<String> urls, Long userId) {
        if (urls == null || urls.isEmpty()) return;
        for (String url : urls) {
            MediaJpaEntity m = new MediaJpaEntity();
            m.setOwnerType("REPAIR_REQUEST");
            m.setOwnerId(requestId);
            m.setUrl(url);
            m.setCreatedBy(userId);
            mediaRepo.save(m);
        }
    }

    void recordProgress(Long requestId, RequestStatus from, RequestStatus to, String note, Long userId) {
        WorkProgressJpaEntity wp = new WorkProgressJpaEntity();
        wp.setRequestId(requestId);
        wp.setFromStatus(from != null ? from : to);
        wp.setToStatus(to);
        wp.setNote(note);
        wp.setCreatedBy(userId);
        progressRepo.save(wp);
    }

    private String generateRequestCode() {
        return "RR-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);
    }

    private Sort buildSort(String sortBy, String sortOrder) {
        String field = switch (sortBy != null ? sortBy : "createdAt") {
            case "budgetRef" -> "budgetRef";
            case "requestedTime", "preferredTime" -> "requestedTime";
            default -> "createdAt";
        };
        Sort.Direction dir = "ASC".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, field);
    }
}
