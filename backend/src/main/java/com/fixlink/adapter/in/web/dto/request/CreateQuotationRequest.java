package com.fixlink.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateQuotationRequest {

    @NotBlank(message = "Giải pháp sửa chữa không được để trống")
    private String solution;

    @NotNull(message = "Giá công không được để trống")
    private BigDecimal priceLaborVnd;

    @NotNull(message = "Giá vật tư không được để trống")
    private BigDecimal priceMaterialsVnd;

    private LocalDateTime inspectionTime;
    private LocalDateTime estimatedFinish;
    private String note;
}
