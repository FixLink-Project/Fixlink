package com.fixlink.application.port.in;

import com.fixlink.adapter.in.web.dto.response.RepairRequestDetailResponse;
import com.fixlink.adapter.in.web.dto.response.RepairRequestResponse;
import com.fixlink.domain.model.Media;
import com.fixlink.domain.model.RepairRequest;
import com.fixlink.domain.model.RequestStatus;
import com.fixlink.domain.model.Role;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Use case quản lý Yêu cầu Sửa chữa:
 * - List có chia tab + phân trang đánh số (RC-30)
 * - Tạo mới kèm ảnh Firebase Storage / Lưu bản nháp (RC-8)
 * - Cập nhật trạng thái và gắn tệp đính kèm
 * - Tương thích ngược với hệ thống Báo giá (Quotation/Bidding) của Sprint 2
 */
public interface RepairRequestUseCase {

    // ── RC-30 & RC-8 New Methods ──
    RepairRequestPageResult listRequests(RepairRequestQuery query);

    RepairRequestDetail getRequestDetail(Long requestId, Long viewerId, Role viewerRole);

    RepairRequestDetail createRequest(CreateRepairRequestCommand command);

    RepairRequestDetail updateStatus(UpdateStatusCommand command);

    RepairRequestDetail attachMedia(AttachMediaCommand command);

    // ── Sprint 2 Existing Methods ──
    RepairRequestResponse create(CreateCommand command, Long customerId);

    RepairRequestResponse update(Long requestId, UpdateCommand command, Long customerId);

    RepairRequestResponse cancel(Long requestId, String reason, Long customerId);

    Page<RepairRequestResponse> getMyRequests(Long customerId, String status, int page, int limit,
                                               String sortBy, String sortOrder);

    RepairRequestDetailResponse getDetail(Long requestId, Long userId, String role);

    Page<RepairRequestResponse> getMatchingForTechnician(Long technicianId, Long areaId, int page, int limit,
                                                          String search, String sortBy, String sortOrder);

    default Page<RepairRequestResponse> getMatchingForTechnician(Long technicianId, int page, int limit,
                                                                 String search, String sortBy, String sortOrder) {
        return getMatchingForTechnician(technicianId, null, page, limit, search, sortBy, sortOrder);
    }

    /**
     * Tham số truy vấn danh sách theo chuẩn query params của FixLink API Specification.
     */
    record RepairRequestQuery(
            int page,
            int limit,
            String tab,
            String search,
            String sortBy,
            String sortOrder,
            Long viewerId,
            Role viewerRole
    ) {}

    /**
     * Một dòng dữ liệu trên bảng danh sách: yêu cầu + tệp đính kèm + tên hiển thị đã resolve.
     */
    record RepairRequestDetail(
            RepairRequest request,
            List<Media> media,
            String customerName,
            String technicianName,
            String categoryName
    ) {}

    record RepairRequestPageResult(
            List<RepairRequestDetail> items,
            int currentPage,
            int limit,
            long totalItems,
            int totalPages
    ) {}

    record CreateRepairRequestCommand(
            Long customerId,
            String title,
            String description,
            String address,
            Long categoryId,
            Long serviceId,
            Long areaId,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal budgetRef,
            Integer biddingDeadlineDays,
            LocalDateTime requestedTime,
            boolean saveAsDraft,
            List<String> mediaUrls
    ) {
        public CreateRepairRequestCommand(
                Long customerId,
                String title,
                String description,
                String address,
                Long categoryId,
                Long serviceId,
                LocalDateTime requestedTime,
                boolean saveAsDraft,
                List<String> mediaUrls
        ) {
            this(customerId, title, description, address, categoryId, serviceId,
                    null, null, null, null, null, requestedTime, saveAsDraft, mediaUrls);
        }
    }

    record UpdateStatusCommand(
            Long requestId,
            RequestStatus newStatus,
            String note,
            Long actorId,
            Role actorRole
    ) {}

    record AttachMediaCommand(
            Long requestId,
            List<String> mediaUrls,
            Long actorId,
            Role actorRole
    ) {}

    @Data
    @Builder
    class CreateCommand {
        private String title;
        private String description;
        private Long categoryId;
        private Long areaId;
        private String addressLine;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private LocalDateTime preferredTime;
        private BigDecimal budgetRef;
        private Integer biddingDeadlineDays;
        private List<String> mediaUrls;
    }

    @Data
    @Builder
    class UpdateCommand {
        private String title;
        private String description;
        private Long categoryId;
        private Long areaId;
        private String addressLine;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private LocalDateTime preferredTime;
        private BigDecimal budgetRef;
        private Integer biddingDeadlineDays;
        private List<String> mediaUrls;
    }
}
