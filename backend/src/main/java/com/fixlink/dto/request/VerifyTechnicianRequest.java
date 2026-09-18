package com.fixlink.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyTechnicianRequest {

    @NotBlank(message = "Trạng thái xác minh không được để trống")
    @Pattern(regexp = "APPROVED|REJECTED", message = "Trạng thái phải là APPROVED hoặc REJECTED")
    private String verificationStatus;

    private String note;

    private String rejectionReason;
}
