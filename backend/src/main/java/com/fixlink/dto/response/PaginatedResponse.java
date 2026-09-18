package com.fixlink.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginatedResponse<T> {

    private int statusCode;
    private String message;
    private Meta meta;
    private List<T> data;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Meta {
        private int currentPage;
        private int limit;
        private long totalItems;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }

    public static <T> PaginatedResponse<T> of(String message, List<T> data,
                                                int currentPage, int limit,
                                                long totalItems, int totalPages) {
        Meta meta = Meta.builder()
                .currentPage(currentPage)
                .limit(limit)
                .totalItems(totalItems)
                .totalPages(totalPages)
                .hasNext(currentPage < totalPages)
                .hasPrevious(currentPage > 1)
                .build();

        return PaginatedResponse.<T>builder()
                .statusCode(200)
                .message(message)
                .meta(meta)
                .data(data)
                .build();
    }
}
