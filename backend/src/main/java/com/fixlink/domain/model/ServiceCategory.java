package com.fixlink.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Thực thể danh mục ngành nghề dịch vụ sửa chữa (Jira RC-8).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCategory {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String iconUrl;
    private Integer displayOrder;
    private Boolean isActive;
    @Builder.Default
    private List<Service> services = new ArrayList<>();
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private LocalDateTime deletedAt;
    private Long deletedBy;
}
