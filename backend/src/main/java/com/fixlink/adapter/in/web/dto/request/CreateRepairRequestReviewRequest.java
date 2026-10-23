package com.fixlink.adapter.in.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateRepairRequestReviewRequest {
    @NotNull @Min(1) @Max(5)
    private Integer rating;

    @NotBlank
    @Size(max = 2000)
    private String comment;
}
