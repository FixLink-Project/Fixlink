package com.fixlink.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateServiceRequest {

    @Size(max = 150, message = "Tên dịch vụ không được vượt quá 150 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;

    @Positive(message = "Giá ước tính phải là số dương")
    private BigDecimal estimatedPrice;

    @Size(max = 50, message = "Đơn vị không được vượt quá 50 ký tự")
    private String unit;

    private String iconUrl;

    private Integer sortOrder;

    private Boolean isActive;

    private Long categoryId;
}
