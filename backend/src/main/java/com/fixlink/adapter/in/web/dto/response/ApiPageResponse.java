package com.fixlink.adapter.in.web.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ApiPageResponse<T> {
    private int statusCode;
    private String message;
    private PageMeta meta;
    private List<T> data;

    public ApiPageResponse() {}

    public ApiPageResponse(int statusCode, String message, PageMeta meta, List<T> data) {
        this.statusCode = statusCode;
        this.message = message;
        this.meta = meta;
        this.data = data;
    }

    public static <T> ApiPageResponse<T> of(String message, int currentPage, int limit, long totalItems, int totalPages, List<T> data) {
        PageMeta meta = new PageMeta(currentPage, limit, totalItems, totalPages);
        return new ApiPageResponse<>(200, message, meta, data);
    }

    @Getter
    @Setter
    public static class PageMeta {
        private int currentPage;
        private int limit;
        private long totalItems;
        private int totalPages;
        /** Suy ra từ currentPage và totalPages để client không phải tự tính lại. */
        private boolean hasNext;
        private boolean hasPrevious;

        public PageMeta() {}

        public PageMeta(int currentPage, int limit, long totalItems, int totalPages) {
            this.currentPage = currentPage;
            this.limit = limit;
            this.totalItems = totalItems;
            this.totalPages = totalPages;
            this.hasNext = currentPage < totalPages;
            this.hasPrevious = currentPage > 1;
        }
    }
}
