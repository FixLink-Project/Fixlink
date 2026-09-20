package com.fixlink.adapter.in.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryImpactAssessmentResponse {
    private Long categoryId;
    private String categoryName;
    private long activeTechniciansCount;
    private long pendingRequestsCount;
    private boolean canDeleteDirectly;
}
