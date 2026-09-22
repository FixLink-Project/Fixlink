package com.fixlink.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateRepairRequestRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
    private String title;

    @NotBlank(message = "Mô tả không được để trống")
    private String description;

    @NotNull(message = "Danh mục dịch vụ là bắt buộc")
    private Long categoryId;

    private Long areaId;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String addressLine;

    private BigDecimal latitude;
    private BigDecimal longitude;

    private LocalDateTime preferredTime;

    private BigDecimal budgetRef;

    private Integer biddingDeadlineDays;

    private List<String> mediaUrls;
}
