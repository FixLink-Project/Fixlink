package com.fixlink.adapter.in.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu cập nhật hồ sơ khách hàng (RC-17)")
public class UpdateCustomerProfileRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên không được vượt quá 100 ký tự")
    @Schema(description = "Họ và tên khách hàng", example = "Trần Thị Mai Anh")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại không đúng định dạng Việt Nam (VD: 0901234567)")
    @Schema(description = "Số điện thoại liên hệ", example = "0901234567")
    private String phone;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng hợp lệ")
    @Schema(description = "Địa chỉ email liên hệ", example = "maianh.tran@gmail.com")
    private String email;

    @Schema(description = "URL ảnh đại diện (tùy chọn)", example = "https://images.unsplash.com/photo-1494790108377-be9c29b29330")
    private String avatarUrl;
}
