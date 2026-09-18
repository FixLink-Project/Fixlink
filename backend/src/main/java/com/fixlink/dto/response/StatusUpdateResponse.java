package com.fixlink.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Response DTO cho RC-21 (Block/Unblock) và RC-22 (Verify Technician).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatusUpdateResponse {

    private String userId;
    private String status;
    private String verificationStatus;
    private LocalDateTime verifiedAt;
    private String verifiedBy;
}
