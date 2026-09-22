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
public class Media {
    private Long id;
    private String ownerType;
    private Long ownerId;
    private String url;
    private LocalDateTime createdAt;
    private Long createdBy;
}
