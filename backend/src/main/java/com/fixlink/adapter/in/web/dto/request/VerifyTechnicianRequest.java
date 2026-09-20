package com.fixlink.adapter.in.web.dto.request;

import com.fixlink.domain.model.VerificationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyTechnicianRequest {

    @NotNull(message = "Trạng thái xác minh không được để trống (APPROVED hoặc REJECTED)")
    private VerificationStatus verificationStatus;

    private String note;
    private String rejectionReason;
}
