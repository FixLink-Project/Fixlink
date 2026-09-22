package com.fixlink.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelRepairRequestRequest {
    @NotBlank(message = "Lý do hủy không được để trống")
    private String reason;
}
