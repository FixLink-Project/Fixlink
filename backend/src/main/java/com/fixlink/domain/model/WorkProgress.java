package com.fixlink.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkProgress {
    private Long id;
    private Long requestId;
    private RequestStatus fromStatus;
    private RequestStatus toStatus;
    private String note;
    private LocalDateTime createdAt;
    private Long createdBy;
}
