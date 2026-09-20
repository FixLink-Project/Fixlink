package com.fixlink.adapter.in.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryImpactResponse {
    private Long categoryId;
    private String categoryName;
    private long affectedTechnicians;
    private long affectedPendingRequests;
    private String message;
}
