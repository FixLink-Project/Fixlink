package com.fixlink.chatbot.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Form "Gặp nhân viên" ngay trong khung chat. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoffRequest {

    @NotBlank(message = "sessionId không được để trống")
    private String sessionId;

    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String name;

    /** Số điện thoại hoặc email — bắt buộc với khách chưa đăng nhập. */
    @Size(max = 120, message = "Thông tin liên hệ tối đa 120 ký tự")
    private String contact;

    @NotBlank(message = "Vui lòng mô tả vấn đề cần hỗ trợ")
    @Size(max = 1000, message = "Nội dung tối đa 1000 ký tự")
    private String issue;
}
