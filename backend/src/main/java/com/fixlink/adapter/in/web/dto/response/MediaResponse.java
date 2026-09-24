package com.fixlink.adapter.in.web.dto.response;

import com.fixlink.domain.model.Media;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Metadata tệp đính kèm (ảnh/video) đã upload lên Firebase Storage.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponse {

    private Long id;
    private String url;
    private String mediaType;
    private Long uploadedBy;
    private LocalDateTime createdAt;

    public static MediaResponse fromDomain(Media media) {
        if (media == null) {
            return null;
        }
        return MediaResponse.builder()
                .id(media.getId())
                .url(media.getUrl())
                .mediaType(media.getMediaType() != null ? media.getMediaType().name() : null)
                .uploadedBy(media.getUploadedBy())
                .createdAt(media.getCreatedAt())
                .build();
    }
}
