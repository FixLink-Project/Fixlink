package com.fixlink.application.port.in;

import com.fixlink.adapter.in.web.dto.response.RepairRequestDetailResponse;
import com.fixlink.adapter.in.web.dto.response.RepairRequestResponse;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface RepairRequestUseCase {

    RepairRequestResponse create(CreateCommand command, Long customerId);

    RepairRequestResponse update(Long requestId, UpdateCommand command, Long customerId);

    RepairRequestResponse cancel(Long requestId, String reason, Long customerId);

    Page<RepairRequestResponse> getMyRequests(Long customerId, String status, int page, int limit,
                                               String sortBy, String sortOrder);

    RepairRequestDetailResponse getDetail(Long requestId, Long userId, String role);

    Page<RepairRequestResponse> getMatchingForTechnician(Long technicianId, int page, int limit,
                                                          String search, String sortBy, String sortOrder);

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
