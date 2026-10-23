package com.fixlink.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UpdateQuotationRequest {

    private String solution;

    @NotNull(message = "Giá báo không được để trống")
    private BigDecimal priceLaborVnd;

    private BigDecimal priceMaterialsVnd;

    private LocalDateTime inspectionTime;
    private LocalDateTime estimatedFinish;
    private String note;
}
