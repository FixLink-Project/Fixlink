package com.fixlink.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload cập nhật trạng thái một yêu cầu sửa chữa.
 * Admin có thể chuyển sang mọi trạng thái; Khách hàng chỉ được HỦY yêu cầu của chính mình
 * hoặc ĐĂNG bản nháp (DRAFT -> PENDING).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRepairRequestStatusRequest {

    @NotBlank(message = "Trạng thái mới không được để trống")
    private String status;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String note;
}
