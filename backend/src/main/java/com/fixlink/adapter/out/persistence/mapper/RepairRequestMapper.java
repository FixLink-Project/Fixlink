package com.fixlink.adapter.out.persistence.mapper;

import com.fixlink.adapter.out.persistence.entity.MediaJpaEntity;
import com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity;
import com.fixlink.domain.model.Media;
import com.fixlink.domain.model.RepairRequest;
import org.springframework.stereotype.Component;

/**
 * Chuyển đổi qua lại giữa thực thể JPA và mô hình nghiệp vụ của Yêu cầu Sửa chữa / Tệp đính kèm.
 */
@Component
public class RepairRequestMapper {

    public RepairRequest toDomain(RepairRequestJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return RepairRequest.builder()
                .id(entity.getId())
                .requestCode(entity.getRequestCode())
                .customerId(entity.getCustomerId())
                .technicianId(entity.getTechnicianId())
                .categoryId(entity.getCategoryId())
                .serviceId(entity.getServiceId())
                .status(entity.getStatus())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .deviceBrand(entity.getDeviceBrand())
                .deviceModel(entity.getDeviceModel())
                .serialNumber(entity.getSerialNumber())
                .address(entity.getAddress())
                .requestedTime(entity.getRequestedTime())
                .agreedPrice(entity.getAgreedPrice())
                .depositAmount(entity.getDepositAmount())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .deletedAt(entity.getDeletedAt())
                .deletedBy(entity.getDeletedBy())
                .build();
    }

    public Media toDomain(MediaJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return Media.builder()
                .id(entity.getId())
                .ownerType(entity.getOwnerType())
                .ownerId(entity.getOwnerId())
                .mediaType(entity.getMediaType())
                .url(entity.getUrl())
                .uploadedBy(entity.getUploadedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
