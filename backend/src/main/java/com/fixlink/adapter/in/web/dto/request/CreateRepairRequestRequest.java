package com.fixlink.adapter.in.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;

    @NotNull(message = "Danh mục dịch vụ là bắt buộc")
    private Long categoryId;

    private Long areaId;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String addressLine;

    private BigDecimal latitude;
    private BigDecimal longitude;

    private LocalDateTime preferredTime;

    @DecimalMin(value = "0.0", inclusive = true, message = "Ngân sách tham khảo không được âm")
    private BigDecimal budgetRef;

    @Min(value = 1, message = "Hạn nhận báo giá tối thiểu 1 ngày")
    @Max(value = 30, message = "Hạn nhận báo giá tối đa 30 ngày")
    private Integer biddingDeadlineDays;

    @Size(max = 10, message = "Tối đa 10 hình ảnh hoặc video đính kèm")
    private List<String> mediaUrls;
}
