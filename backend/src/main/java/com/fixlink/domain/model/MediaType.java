package com.fixlink.domain.model;

/**
 * Loại tệp đính kèm được lưu trên Firebase Storage (Jira RC-8 - bảng media).
 */
public enum MediaType {
    IMAGE,      // Ảnh hiện trường (jpg, png, webp)
    VIDEO,      // Video hiện trường (mp4, mov)
    DOCUMENT    // Tài liệu (pdf, hoá đơn, biên bản)
}
