package com.fixlink.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Thực thể tệp đính kèm (media) - metadata của tệp đã upload lên Firebase Storage.
 * Dùng để lưu ảnh hiện trường trước/sau sửa chữa của một yêu cầu sửa chữa.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Media {

    /** Giá trị mặc định cho cột owner_type khi tệp thuộc về một yêu cầu sửa chữa. */
    public static final String OWNER_REPAIR_REQUEST = "REPAIR_REQUEST";

    private Long id;
    private String ownerType;
    private Long ownerId;
    private MediaType mediaType;
    private String url;
    private Long uploadedBy;
    private Long createdBy;
    private LocalDateTime createdAt;

    public Long getUploadedBy() {
        return uploadedBy != null ? uploadedBy : createdBy;
    }

    public Long getCreatedBy() {
        return createdBy != null ? createdBy : uploadedBy;
    }
}
