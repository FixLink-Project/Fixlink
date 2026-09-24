package com.fixlink.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Payload gắn thêm tệp bằng chứng (ảnh/video hiện trường) vào một yêu cầu sửa chữa.
 * Tệp được upload trực tiếp từ trình duyệt lên Firebase Storage, backend chỉ lưu metadata URL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachMediaRequest {

    @NotEmpty(message = "Danh sách tệp đính kèm không được để trống")
    @Size(max = 6, message = "Tối đa 6 tệp đính kèm cho mỗi lần gửi")
    private List<@Size(max = 800000, message = "URL tệp đính kèm vượt quá dung lượng cho phép") String> mediaUrls;
}
