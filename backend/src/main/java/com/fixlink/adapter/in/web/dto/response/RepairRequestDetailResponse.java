package com.fixlink.adapter.in.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepairRequestDetailResponse {
    private RepairRequestResponse request;
    private List<QuotationResponse> quotations;
    private List<WorkProgressResponse> workProgress;
}
