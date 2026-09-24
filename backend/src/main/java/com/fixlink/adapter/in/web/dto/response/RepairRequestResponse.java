package com.fixlink.adapter.in.web.dto.response;

import com.fixlink.application.port.in.RepairRequestUseCase.RepairRequestDetail;
import com.fixlink.domain.model.Media;
import com.fixlink.domain.model.RepairRequest;
import com.fixlink.domain.model.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dữ liệu trả về cho một yêu cầu sửa chữa kèm danh sách tệp đính kèm (ảnh hiện trường).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepairRequestResponse {

    private Long id;
    private String requestCode;
    private Long customerId;
    private String customerName;
    private Long technicianId;
    private String technicianName;
    private Long categoryId;
    private String categoryName;
    private Long serviceId;
    private Long areaId;
    private String areaName;
    private RequestStatus status;
    private String statusLabel;

    public String getStatus() {
        return status != null ? status.name() : null;
    }
    private String title;
    private String description;
    private String deviceBrand;
    private String deviceModel;
    private String serialNumber;
    private String address;
    private String addressLine;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime preferredTime;
    private LocalDateTime requestedTime;
    private BigDecimal agreedPrice;
    private BigDecimal depositAmount;
    private BigDecimal budgetRef;
    private LocalDateTime biddingDeadline;
    private String cancelReason;
    private Long selectedQuotationId;
    private List<String> mediaUrls;
    private List<MediaResponse> media;
    private int quotationCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getAddress() {
        return address != null ? address : addressLine;
    }

    public String getAddressLine() {
        return addressLine != null ? addressLine : address;
    }

    public static RepairRequestResponse fromDetail(RepairRequestDetail detail) {
        if (detail == null || detail.request() == null) {
            return null;
        }

        RepairRequest entity = detail.request();
        List<MediaResponse> mediaList = detail.media() == null
                ? List.of()
                : detail.media().stream().map(MediaResponse::fromDomain).toList();

        List<String> urls = mediaList.stream().map(MediaResponse::getUrl).toList();

        return RepairRequestResponse.builder()
                .id(entity.getId())
                .requestCode(entity.getRequestCode())
                .customerId(entity.getCustomerId())
                .customerName(detail.customerName())
                .technicianId(entity.getTechnicianId())
                .technicianName(detail.technicianName())
                .categoryId(entity.getCategoryId())
                .categoryName(detail.categoryName())
                .serviceId(entity.getServiceId())
                .status(entity.getStatus())
                .statusLabel(resolveStatusLabel(entity.getStatus()))
                .title(entity.getTitle())
                .description(entity.getDescription())
                .deviceBrand(entity.getDeviceBrand())
                .deviceModel(entity.getDeviceModel())
                .serialNumber(entity.getSerialNumber())
                .address(entity.getAddress())
                .addressLine(entity.getAddress())
                .areaId(entity.getAreaId())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .budgetRef(entity.getBudgetRef())
                .biddingDeadline(entity.getBiddingDeadline())
                .cancelReason(entity.getCancelReason())
                .selectedQuotationId(entity.getSelectedQuotationId())
                .requestedTime(entity.getRequestedTime())
                .preferredTime(entity.getRequestedTime())
                .agreedPrice(entity.getAgreedPrice())
                .depositAmount(entity.getDepositAmount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .media(mediaList)
                .mediaUrls(urls)
                .build();
    }

    public static String resolveStatusLabel(RequestStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case DRAFT -> "Bản nháp";
            case PENDING -> "Chờ báo giá";
            case BIDDING_OPEN -> "Đang nhận báo giá";
            case MATCHED_AWAITING_DEPOSIT -> "Chờ khách cọc";
            case ASSIGNED -> "Đã nhận việc";
            case INSPECTING -> "Đang khảo sát";
            case AWAITING_COST_APPROVAL -> "Chờ duyệt phát sinh";
            case IN_PROGRESS -> "Đang sửa chữa";
            case AWAITING_ACCEPTANCE -> "Chờ nghiệm thu";
            case COMPLETED -> "Hoàn thành";
            case CANCELLED -> "Đã hủy";
        };
    }

    public static RepairRequestResponse fromDomain(RepairRequest entity, List<Media> mediaList) {
        return fromDetail(new RepairRequestDetail(entity, mediaList, null, null, null));
    }
}
