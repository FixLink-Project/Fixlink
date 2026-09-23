package com.fixlink.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelRepairRequestRequest {

    @NotBlank(message = "Lý do hủy không được để trống")
    @Size(min = 3, max = 500, message = "Lý do hủy từ 3 đến 500 ký tự")
    private String reason;
}
