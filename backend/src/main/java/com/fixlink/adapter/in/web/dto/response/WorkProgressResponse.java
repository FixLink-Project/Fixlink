package com.fixlink.adapter.in.web.dto.response;

import com.fixlink.domain.model.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkProgressResponse {
    private Long id;
    private RequestStatus fromStatus;
    private RequestStatus toStatus;
    private String note;
    private LocalDateTime createdAt;
}
