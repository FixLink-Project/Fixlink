package com.fixlink.application.service;

import com.fixlink.adapter.in.web.dto.response.*;
import com.fixlink.adapter.out.persistence.entity.*;
import com.fixlink.adapter.out.persistence.mapper.RepairRequestMapper;
import com.fixlink.adapter.out.persistence.repository.*;
import com.fixlink.application.port.in.RepairRequestUseCase;
import com.fixlink.domain.exception.DomainException;
import com.fixlink.domain.exception.InvalidStateTransitionException;
import com.fixlink.domain.exception.ResourceNotFoundException;
import com.fixlink.domain.exception.ValidationFailedException;
import com.fixlink.domain.model.Media;
import com.fixlink.domain.model.QuotationStatus;
import com.fixlink.domain.model.MediaType;
import com.fixlink.domain.model.RequestStatus;
import com.fixlink.domain.model.RequestTab;
import com.fixlink.domain.model.Role;
import com.fixlink.domain.model.UserStatus;
import com.fixlink.domain.model.VerificationStatus;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Nghiệp vụ Yêu cầu Sửa chữa:
 * - Danh sách chia tab + phân trang đánh số (RC-30)
 * - Tạo mới kèm ảnh Firebase Storage / Lưu bản nháp (RC-8)
 * - Gắn ảnh bằng chứng và cập nhật trạng thái
 * - Quản lý quy trình đấu thầu & báo giá (Sprint 2)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RepairRequestService implements RepairRequestUseCase {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("createdAt", "requestedTime", "status", "title", "agreedPrice", "budgetRef");
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_MEDIA_PER_REQUEST = 6;
    private static final DateTimeFormatter CODE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SpringDataRepairRequestRepository requestRepo;
    private final SpringDataMediaRepository mediaRepo;
    private final SpringDataWorkProgressRepository progressRepo;
    private final SpringDataQuotationRepository quotationRepo;
    private final SpringDataServiceCategoryRepository categoryRepo;
    private final SpringDataServiceAreaRepository areaRepo;
    private final SpringDataTechnicianProfileRepository techProfileRepo;
    private final SpringDataCustomerProfileRepository customerProfileRepo;
    private final SpringDataUserRepository userRepo;
    private final RepairRequestMapper repairRequestMapper;

    // ==================================================================================
    // 1. DANH SÁCH YÊU CẦU (chia tab + phân trang đánh số + tìm kiếm + sắp xếp)
    // ==================================================================================

    @Override
    @Transactional(readOnly = true)
    public RepairRequestPageResult listRequests(RepairRequestQuery query) {
        int currentPage = query.page() > 0 ? query.page() : 1;
        int pageSize = query.limit() > 0 ? Math.min(query.limit(), MAX_PAGE_SIZE) : DEFAULT_PAGE_SIZE;
        RequestTab tab = RequestTab.fromCode(query.tab());

        Sort.Direction direction = "ASC".equalsIgnoreCase(query.sortOrder()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(currentPage - 1, pageSize, Sort.by(direction, resolveSortProperty(query.sortBy())));

        Page<RepairRequestJpaEntity> pageResult =
                requestRepo.findAll(buildSpecification(query, tab), pageable);

        return new RepairRequestPageResult(
                toDetails(pageResult.getContent()),
                currentPage,
                pageSize,
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public RepairRequestDetail getRequestDetail(Long requestId, Long viewerId, Role viewerRole) {
        RepairRequestJpaEntity entity = findRequestOrThrow(requestId);
        assertPermission(entity, viewerId, viewerRole, Permission.VIEW);
        return toDetails(List.of(entity)).get(0);
    }

    /**
     * Bộ lọc động: soft-delete + phạm vi dữ liệu theo vai trò (data scoping) + tab trạng thái + từ khóa.
     */
    private Specification<RepairRequestJpaEntity> buildSpecification(RepairRequestQuery query, RequestTab tab) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));

            // Data scoping: khách hàng chỉ thấy yêu cầu của mình, thợ chỉ thấy việc được giao, Admin thấy tất cả.
            if (query.viewerRole() == Role.CUSTOMER && query.viewerId() != null) {
                predicates.add(cb.equal(root.get("customerId"), query.viewerId()));
            } else if (query.viewerRole() == Role.TECHNICIAN && query.viewerId() != null) {
                predicates.add(cb.equal(root.get("technicianId"), query.viewerId()));
            }

            // Tab bar: nhóm nhiều trạng thái chi tiết vào một tab nghiệp vụ.
            if (!tab.isAll()) {
                predicates.add(root.get("status").in(tab.getStatuses()));
            }

            if (query.search() != null && !query.search().isBlank()) {
                String pattern = "%" + query.search().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("requestCode")), pattern),
                        cb.like(cb.lower(root.get("address")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String resolveSortProperty(String sortBy) {
        if (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy)) {
            return sortBy;
        }
        return "createdAt";
    }

    /**
     * Nạp tệp đính kèm và tên hiển thị (khách hàng / thợ / danh mục) theo lô để tránh truy vấn N+1.
     */
    private List<RepairRequestDetail> toDetails(List<RepairRequestJpaEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }

        List<Long> requestIds = entities.stream().map(RepairRequestJpaEntity::getId).toList();
        Map<Long, List<Media>> mediaByRequest = mediaRepo
                .findByOwnerTypeAndOwnerIdInOrderByCreatedAtAsc(Media.OWNER_REPAIR_REQUEST, requestIds)
                .stream()
                .map(repairRequestMapper::toDomain)
                .collect(Collectors.groupingBy(Media::getOwnerId));

        Map<Long, String> customerNames = resolveNames(
                uniqueIds(entities, RepairRequestJpaEntity::getCustomerId),
                id -> customerProfileRepo.findByUserId(id).map(CustomerProfileJpaEntity::getFullName).orElse(null));

        Map<Long, String> technicianNames = resolveNames(
                uniqueIds(entities, RepairRequestJpaEntity::getTechnicianId),
                id -> techProfileRepo.findByUserId(id).map(TechnicianProfileJpaEntity::getFullName).orElse(null));

        Map<Long, String> categoryNames = categoryRepo
                .findAllById(uniqueIds(entities, RepairRequestJpaEntity::getCategoryId))
                .stream()
                .collect(Collectors.toMap(ServiceCategoryJpaEntity::getId, ServiceCategoryJpaEntity::getName, (a, b) -> a));

        return entities.stream()
                .map(entity -> new RepairRequestDetail(
                        repairRequestMapper.toDomain(entity),
                        mediaByRequest.getOrDefault(entity.getId(), List.of()),
                        lookupName(customerNames, entity.getCustomerId()),
                        lookupName(technicianNames, entity.getTechnicianId()),
                        lookupName(categoryNames, entity.getCategoryId())
                ))
                .toList();
    }

    private String lookupName(Map<Long, String> names, Long id) {
        return id == null ? null : names.get(id);
    }

    private List<Long> uniqueIds(List<RepairRequestJpaEntity> entities, Function<RepairRequestJpaEntity, Long> extractor) {
        return entities.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Map<Long, String> resolveNames(List<Long> ids, Function<Long, String> loader) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> result = new HashMap<>();
        for (Long id : ids) {
            String name = loader.apply(id);
            if (name != null) {
                result.put(id, name);
            }
        }
        return result;
    }

    // ==================================================================================
    // 2. TẠO MỚI YÊU CẦU SỬA CHỮA (kèm ảnh đã upload lên Firebase Storage)
    // ==================================================================================

    @Override
    @Transactional
    public RepairRequestDetail createRequest(CreateRepairRequestCommand command) {
        validateCustomerAccount(command.customerId());

        ServiceCategoryJpaEntity category = categoryRepo
                .findByIdAndDeletedAtIsNull(command.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy danh mục dịch vụ với ID = " + command.categoryId()));

        if (Boolean.FALSE.equals(category.getIsActive())) {
            throw new ValidationFailedException("Danh mục dịch vụ đã ngừng hoạt động",
                    Map.of("categoryId", "Danh mục dịch vụ đã ngừng hoạt động"));
        }

        List<String> mediaUrls = normalizeMediaUrls(command.mediaUrls());

        RequestStatus initialStatus;
        if (command.saveAsDraft()) {
            initialStatus = RequestStatus.DRAFT;
        } else if (command.biddingDeadlineDays() != null || command.budgetRef() != null) {
            initialStatus = RequestStatus.BIDDING_OPEN;
        } else {
            initialStatus = RequestStatus.PENDING;
        }

        RepairRequestJpaEntity entity = new RepairRequestJpaEntity();
        entity.setRequestCode(generateRequestCode());
        entity.setCustomerId(command.customerId());
        entity.setCategoryId(category.getId());
        entity.setServiceId(command.serviceId());
        entity.setAreaId(command.areaId());
        entity.setLatitude(command.latitude());
        entity.setLongitude(command.longitude());
        entity.setBudgetRef(command.budgetRef() != null ? command.budgetRef() : BigDecimal.ZERO);
        if (command.biddingDeadlineDays() != null) {
            entity.setBiddingDeadline(LocalDateTime.now().plusDays(command.biddingDeadlineDays()));
        }
        entity.setStatus(initialStatus);
        entity.setTitle(command.title().trim());
        entity.setDescription(command.description().trim());
        entity.setAddress(command.address().trim());
        entity.setRequestedTime(command.requestedTime() != null
                ? command.requestedTime()
                : LocalDateTime.now().plusDays(1));
        entity.setAgreedPrice(BigDecimal.ZERO);
        entity.setDepositAmount(BigDecimal.ZERO);
        entity.setCreatedBy(command.customerId());
        entity.setUpdatedBy(command.customerId());

        RepairRequestJpaEntity saved = requestRepo.save(entity);
        saveMedia(saved.getId(), mediaUrls, command.customerId());

        recordProgress(saved.getId(), null, initialStatus,
                command.saveAsDraft() ? "Khách lưu bản nháp" : "Khách tạo yêu cầu mới (chờ báo giá)",
                command.customerId());

        log.info(">>> Khách hàng {} đã tạo yêu cầu sửa chữa {} với {} tệp đính kèm (trạng thái {})",
                command.customerId(), saved.getRequestCode(), mediaUrls.size(), saved.getStatus());

        return toDetails(List.of(saved)).get(0);
    }

    // ==================================================================================
    // 3. CẬP NHẬT TRẠNG THÁI (Tab trạng thái) & GẮN THÊM ẢNH BẰNG CHỨNG
    // ==================================================================================

    @Override
    @Transactional
    public RepairRequestDetail updateStatus(UpdateStatusCommand command) {
        RepairRequestJpaEntity entity = findRequestOrThrow(command.requestId());
        assertPermission(entity, command.actorId(), command.actorRole(), Permission.UPDATE_STATUS);

        RequestStatus currentStatus = entity.getStatus();
        RequestStatus targetStatus = command.newStatus();

        if (targetStatus == null) {
            throw new ValidationFailedException("Trạng thái mới không hợp lệ",
                    Map.of("status", "Trạng thái mới không hợp lệ"));
        }
        if (targetStatus == currentStatus) {
            throw new InvalidStateTransitionException(currentStatus, targetStatus);
        }

        if (command.actorRole() == Role.CUSTOMER) {
            boolean cancelOwnRequest = targetStatus == RequestStatus.CANCELLED
                    && currentStatus != RequestStatus.COMPLETED
                    && currentStatus != RequestStatus.CANCELLED;
            boolean publishDraft = targetStatus == RequestStatus.PENDING && currentStatus == RequestStatus.DRAFT;

            if (!cancelOwnRequest && !publishDraft) {
                throw new InvalidStateTransitionException(currentStatus, targetStatus);
            }
        }

        entity.setStatus(targetStatus);
        entity.setUpdatedBy(command.actorId());
        RepairRequestJpaEntity saved = requestRepo.save(entity);

        recordProgress(saved.getId(), currentStatus, targetStatus,
                command.note() != null ? command.note() : "Cập nhật trạng thái", command.actorId());

        log.info(">>> Yêu cầu {} chuyển trạng thái {} -> {} bởi user {} ({}). Ghi chú: {}",
                saved.getRequestCode(), currentStatus, targetStatus, command.actorId(), command.actorRole(),
                command.note() != null ? command.note() : "không có");

        return toDetails(List.of(saved)).get(0);
    }

    @Override
    @Transactional
    public RepairRequestDetail attachMedia(AttachMediaCommand command) {
        RepairRequestJpaEntity entity = findRequestOrThrow(command.requestId());
        assertPermission(entity, command.actorId(), command.actorRole(), Permission.ATTACH_MEDIA);

        List<String> mediaUrls = normalizeMediaUrls(command.mediaUrls());
        long storedMedia = mediaRepo.countByOwnerTypeAndOwnerId(Media.OWNER_REPAIR_REQUEST, entity.getId());

        if (storedMedia + mediaUrls.size() > MAX_MEDIA_PER_REQUEST) {
            throw new ValidationFailedException(
                    "Mỗi yêu cầu chỉ được đính kèm tối đa " + MAX_MEDIA_PER_REQUEST + " tệp",
                    Map.of("mediaUrls", "Đã có " + storedMedia + " tệp, chỉ có thể thêm tối đa "
                            + Math.max(MAX_MEDIA_PER_REQUEST - storedMedia, 0) + " tệp"));
        }

        saveMedia(entity.getId(), mediaUrls, command.actorId());
        log.info(">>> User {} ({}) đã gắn thêm {} tệp bằng chứng vào yêu cầu {}",
                command.actorId(), command.actorRole(), mediaUrls.size(), entity.getRequestCode());

        return toDetails(List.of(entity)).get(0);
    }

    // ==================================================================================
    // 4. SPRINT 2 METHODS (Bidding / Quotation / Matching)
    // ==================================================================================

    /** Danh muc phai ton tai va dang hoat dong (lay tu PR #1). */
    private void assertCategoryActive(Long categoryId) {
        ServiceCategoryJpaEntity category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục dịch vụ"));
        if (category.getIsActive() != null && !category.getIsActive()) {
            throw new DomainException("INVALID_OPERATION",
                    "Danh mục dịch vụ hiện đang tạm dừng hoạt động");
        }
    }

    /** Khu vuc (neu co) phai ton tai va dang hoat dong (lay tu PR #1). */
    private void assertAreaActive(Long areaId) {
        if (areaId == null) {
            return;
        }
        ServiceAreaJpaEntity area = areaRepo.findById(areaId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khu vực"));
        if (area.getIsActive() != null && !area.getIsActive()) {
            throw new DomainException("INVALID_OPERATION",
                    "Khu vực dịch vụ hiện đang tạm dừng hoạt động");
        }
    }

    private String trimOrNull(String value) {
        return value != null ? value.trim() : null;
    }

    @Override
    @Transactional
    public RepairRequestResponse create(CreateCommand cmd, Long customerId) {
        UserJpaEntity customer = userRepo.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin khách hàng"));
        if (customer.getStatus() != UserStatus.ACTIVE) {
            throw new DomainException("ACCOUNT_INACTIVE",
                    "Tài khoản khách hàng không hoạt động hoặc đã bị khóa", 403);
        }

        assertCategoryActive(cmd.getCategoryId());
        assertAreaActive(cmd.getAreaId());

        RepairRequestJpaEntity entity = new RepairRequestJpaEntity();
        entity.setRequestCode(generateRequestCode());
        entity.setCustomerId(customerId);
        entity.setCategoryId(cmd.getCategoryId());
        entity.setAreaId(cmd.getAreaId());
        entity.setTitle(trimOrNull(cmd.getTitle()));
        entity.setDescription(trimOrNull(cmd.getDescription()));
        entity.setAddress(cmd.getAddressLine() != null ? cmd.getAddressLine().trim() : "");
        entity.setLatitude(cmd.getLatitude());
        entity.setLongitude(cmd.getLongitude());
        entity.setRequestedTime(cmd.getPreferredTime() != null ? cmd.getPreferredTime() : LocalDateTime.now().plusDays(1));
        entity.setBudgetRef(cmd.getBudgetRef() != null ? cmd.getBudgetRef() : BigDecimal.ZERO);
        entity.setStatus(RequestStatus.BIDDING_OPEN);

        int deadlineDays = cmd.getBiddingDeadlineDays() != null ? cmd.getBiddingDeadlineDays() : 3;
        entity.setBiddingDeadline(LocalDateTime.now().plusDays(deadlineDays));

        entity.setCreatedBy(customerId);
        entity.setUpdatedBy(customerId);
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

        assertCategoryActive(cmd.getCategoryId());
        assertAreaActive(cmd.getAreaId());

        entity.setTitle(trimOrNull(cmd.getTitle()));
        entity.setDescription(trimOrNull(cmd.getDescription()));
        entity.setCategoryId(cmd.getCategoryId());
        entity.setAreaId(cmd.getAreaId());
        entity.setAddress(trimOrNull(cmd.getAddressLine()));
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
            mediaRepo.deleteByOwnerTypeAndOwnerId(Media.OWNER_REPAIR_REQUEST, requestId);
            saveMedia(requestId, cmd.getMediaUrls(), customerId);
        }

        recordProgress(requestId, entity.getStatus(), entity.getStatus(),
                "Khách cập nhật thông tin yêu cầu", customerId);

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

        // RC-34 (lay tu PR #1): chuyen toan bo bao gia dang PENDING sang REJECTED
        // vi yeu cau da bi huy.
        for (QuotationJpaEntity quote : quotationRepo.findByRequestIdOrderByCreatedAtDesc(requestId)) {
            if (quote.getStatus() == QuotationStatus.PENDING) {
                quote.setStatus(QuotationStatus.REJECTED);
                quote.setNote(quote.getNote() != null
                        ? quote.getNote() + " [Hủy do khách hủy yêu cầu]"
                        : "[Hủy do khách hủy yêu cầu]");
                quote.setUpdatedBy(customerId);
                quotationRepo.save(quote);
            }
        }

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
            RequestStatus rs = RequestStatus.valueOf(status.toUpperCase(Locale.ROOT));
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
                .filter(r -> r.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu sửa chữa"));

        boolean isOwner = entity.getCustomerId().equals(userId);
        boolean isAdmin = "ADMIN".equals(role) || "STAFF".equals(role);
        boolean isTechnician = "TECHNICIAN".equals(role);

        if (isTechnician) {
            // RC-39: Kiểm tra tài khoản thợ phải được duyệt eKYC (APPROVED)
            TechnicianProfileJpaEntity profile = techProfileRepo.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ thợ"));
            if (profile.getVerificationStatus() != VerificationStatus.APPROVED) {
                throw new DomainException("UNVERIFIED_TECHNICIAN",
                        "Tài khoản chưa được xác minh. Hãy hoàn tất KYC trước.", 403);
            }

            boolean isAssignedTech = entity.getTechnicianId() != null && entity.getTechnicianId().equals(userId);
            boolean hasQuotation = quotationRepo.existsByRequestIdAndTechnicianId(requestId, userId);
            boolean isOpenForBidding = entity.getStatus() == RequestStatus.BIDDING_OPEN;

            // Thợ được xem chi tiết khi: đơn đang mở thầu, hoặc thợ đã được giao việc, hoặc thợ đã gửi báo giá
            if (!isOpenForBidding && !isAssignedTech && !hasQuotation) {
                throw new DomainException("ACCESS_DENIED", "Bạn không có quyền xem yêu cầu này", 403);
            }
        } else if (!isOwner && !isAdmin) {
            throw new DomainException("ACCESS_DENIED", "Bạn không có quyền xem yêu cầu này", 403);
        }

        List<QuotationResponse> quotations = Collections.emptyList();
        if (isOwner || isAdmin) {
            quotations = quotationRepo.findByRequestIdOrderByCreatedAtDesc(requestId).stream()
                    .map(this::toQuotationResponse)
                    .toList();
        } else if (isTechnician) {
            // RC-39: Thợ chỉ nhìn thấy báo giá của chính mình (bảo mật giá cạnh tranh)
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
    public Page<RepairRequestResponse> getMatchingForTechnician(Long technicianId, Long areaId, int page, int limit,
                                                                 String search, String sortBy, String sortOrder) {
        TechnicianProfileJpaEntity profile = techProfileRepo.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ thợ"));

        if (profile.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new DomainException("UNVERIFIED_TECHNICIAN",
                    "Tài khoản chưa được xác minh. Hãy hoàn tất KYC trước.", 403);
        }

        Set<Long> categoryIds = profile.getCategoryIds();
        Set<Long> areaIds = profile.getAreaIds();

        if (categoryIds.isEmpty()) {
            return Page.empty();
        }

        // Nếu lọc theo khu vực cụ thể (RC-38)
        if (areaId != null) {
            areaRepo.findByIdAndIsActiveTrue(areaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Khu vực hoạt động với ID = " + areaId + " không tồn tại hoặc đã bị vô hiệu hóa"));
        } else if (areaIds.isEmpty()) {
            return Page.empty();
        }

        String searchKeyword = (search != null && !search.isBlank()) ? search.trim() : null;

        Sort sort = buildSort(sortBy, sortOrder);
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), limit, sort);

        if (areaId != null) {
            return requestRepo.findMatchingForTechnicianByArea(categoryIds, areaId, searchKeyword, technicianId, pageable)
                    .map(this::toResponse);
        }

        return requestRepo.findMatchingForTechnician(categoryIds, areaIds, searchKeyword, technicianId, pageable)
                .map(this::toResponse);
    }

    // ==================================================================================
    // 5. HÀM HỖ TRỢ (kiểm tra dữ liệu, lưu tệp đính kèm, sinh mã, phân quyền)
    // ==================================================================================

    private RepairRequestJpaEntity findRequestOrThrow(Long requestId) {
        return requestRepo.findById(requestId)
                .filter(request -> request.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy yêu cầu sửa chữa với ID = " + requestId));
    }

    private RepairRequestJpaEntity findOwnedRequest(Long requestId, Long customerId) {
        RepairRequestJpaEntity entity = findRequestOrThrow(requestId);
        if (!entity.getCustomerId().equals(customerId)) {
            throw new DomainException("ACCESS_DENIED", "Bạn không phải chủ yêu cầu này", 403);
        }
        return entity;
    }

    private void validateCustomerAccount(Long customerId) {
        var userEntity = userRepo.findByIdAndDeletedAtIsNull(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID = " + customerId));

        if (userEntity.getRole() != Role.CUSTOMER) {
            throw new AccessDeniedException("Chỉ tài khoản Khách hàng mới được đăng yêu cầu sửa chữa");
        }
        if (userEntity.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("Tài khoản không ở trạng thái hoạt động, không thể đăng yêu cầu");
        }
    }

    private List<String> normalizeMediaUrls(List<String> mediaUrls) {
        if (mediaUrls == null || mediaUrls.isEmpty()) {
            return List.of();
        }

        Set<String> distinctUrls = new LinkedHashSet<>();
        for (String url : mediaUrls) {
            if (url != null && !url.isBlank()) {
                distinctUrls.add(url.trim());
            }
        }

        if (distinctUrls.size() > MAX_MEDIA_PER_REQUEST) {
            throw new ValidationFailedException(
                    "Tối đa " + MAX_MEDIA_PER_REQUEST + " tệp đính kèm cho mỗi yêu cầu",
                    Map.of("mediaUrls", "Số lượng tệp vượt quá giới hạn cho phép"));
        }

        return List.copyOf(distinctUrls);
    }

    private List<Media> saveMedia(Long requestId, List<String> mediaUrls, Long uploaderId) {
        if (mediaUrls == null || mediaUrls.isEmpty()) {
            return List.of();
        }

        List<MediaJpaEntity> entities = mediaUrls.stream().map(url -> {
            MediaJpaEntity media = new MediaJpaEntity();
            media.setOwnerType(Media.OWNER_REPAIR_REQUEST);
            media.setOwnerId(requestId);
            media.setMediaType(resolveMediaType(url));
            media.setUrl(url);
            media.setUploadedBy(uploaderId);
            media.setCreatedBy(uploaderId);
            return media;
        }).toList();

        return mediaRepo.saveAll(entities).stream()
                .map(repairRequestMapper::toDomain)
                .toList();
    }

    private MediaType resolveMediaType(String url) {
        String path = url.split("[?#]")[0].toLowerCase(Locale.ROOT);

        if (path.startsWith("data:video") || path.endsWith(".mp4") || path.endsWith(".mov")) {
            return MediaType.VIDEO;
        }
        if (path.startsWith("data:application/pdf") || path.endsWith(".pdf")) {
            return MediaType.DOCUMENT;
        }
        return MediaType.IMAGE;
    }

    private String generateRequestCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
            String code = "REQ-" + LocalDate.now().format(CODE_DATE_FORMAT) + "-" + suffix;
            if (requestRepo.findByRequestCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Không thể sinh mã yêu cầu sửa chữa duy nhất, vui lòng thử lại");
    }

    private void assertPermission(RepairRequestJpaEntity entity, Long actorId, Role actorRole, Permission permission) {
        if (actorRole == Role.ADMIN) {
            return;
        }

        boolean isOwnerCustomer = actorRole == Role.CUSTOMER && Objects.equals(entity.getCustomerId(), actorId);
        boolean isAssignedTechnician = actorRole == Role.TECHNICIAN && Objects.equals(entity.getTechnicianId(), actorId);

        boolean allowed = switch (permission) {
            case VIEW -> isOwnerCustomer || isAssignedTechnician;
            case UPDATE_STATUS -> isOwnerCustomer;
            case ATTACH_MEDIA -> isOwnerCustomer || isAssignedTechnician;
        };

        if (!allowed) {
            throw new AccessDeniedException("Bạn không có quyền thao tác trên yêu cầu sửa chữa này");
        }
    }

    private RepairRequestResponse toResponse(RepairRequestJpaEntity e) {
        List<MediaResponse> mediaResponses = mediaRepo.findByOwnerTypeAndOwnerIdOrderByCreatedAtAsc(
                Media.OWNER_REPAIR_REQUEST, e.getId())
                .stream()
                .map(repairRequestMapper::toDomain)
                .map(MediaResponse::fromDomain)
                .toList();

        List<String> urls = mediaResponses.stream().map(MediaResponse::getUrl).toList();

        String categoryName = categoryRepo.findById(e.getCategoryId())
                .map(ServiceCategoryJpaEntity::getName).orElse(null);
        String areaName = e.getAreaId() != null
                ? areaRepo.findById(e.getAreaId()).map(ServiceAreaJpaEntity::getName).orElse(null)
                : null;

        String customerName = customerProfileRepo.findByUserId(e.getCustomerId())
                .map(CustomerProfileJpaEntity::getFullName).orElse(null);
        String techName = e.getTechnicianId() != null
                ? techProfileRepo.findByUserId(e.getTechnicianId()).map(TechnicianProfileJpaEntity::getFullName).orElse(null)
                : null;

        int quoteCount = (int) quotationRepo.countByRequestId(e.getId());

        return RepairRequestResponse.builder()
                .id(e.getId())
                .requestCode(e.getRequestCode())
                .customerId(e.getCustomerId())
                .customerName(customerName)
                .technicianId(e.getTechnicianId())
                .technicianName(techName)
                .categoryId(e.getCategoryId())
                .categoryName(categoryName)
                .areaId(e.getAreaId())
                .areaName(areaName)
                .serviceId(e.getServiceId())
                .status(e.getStatus())
                .statusLabel(RepairRequestResponse.resolveStatusLabel(e.getStatus()))
                .title(e.getTitle())
                .description(e.getDescription())
                .address(e.getAddress())
                .addressLine(e.getAddress())
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())
                .preferredTime(e.getRequestedTime())
                .requestedTime(e.getRequestedTime())
                .agreedPrice(e.getAgreedPrice())
                .depositAmount(e.getDepositAmount())
                .budgetRef(e.getBudgetRef())
                .biddingDeadline(e.getBiddingDeadline())
                .cancelReason(e.getCancelReason())
                .selectedQuotationId(e.getSelectedQuotationId())
                .mediaUrls(urls)
                .media(mediaResponses)
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

    public void recordProgress(Long requestId, RequestStatus from, RequestStatus to, String note, Long userId) {
        WorkProgressJpaEntity wp = new WorkProgressJpaEntity();
        wp.setRequestId(requestId);
        wp.setFromStatus(from != null ? from : to);
        wp.setToStatus(to);
        wp.setNote(note);
        wp.setCreatedBy(userId);
        progressRepo.save(wp);
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

    private enum Permission {
        VIEW,
        UPDATE_STATUS,
        ATTACH_MEDIA
    }
}
