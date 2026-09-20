package com.fixlink.adapter.in.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaSimpleResponse {
    private Long id;
    private String code;
    private String name;
    private String city;
}
