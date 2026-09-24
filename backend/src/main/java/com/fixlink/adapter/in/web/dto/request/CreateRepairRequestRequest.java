package com.fixlink.adapter.in.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Payload tạo mới một yêu cầu sửa chữa (Khách hàng đăng yêu cầu trên FixLink).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRepairRequestRequest {

    @NotBlank(message = "Tiêu đề yêu cầu không được để trống")
    @Size(min = 4, max = 200, message = "Tiêu đề yêu cầu từ 4 đến 200 ký tự")
    private String title;

    @NotBlank(message = "Mô tả sự cố không được để trống")
    @Size(min = 5, max = 5000, message = "Mô tả sự cố từ 5 đến 5000 ký tự")
    private String description;

    @NotBlank(message = "Địa chỉ thi công không được để trống")
    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String addressLine;

    @NotNull(message = "Danh mục dịch vụ không được để trống")
    private Long categoryId;

    private Long serviceId;

    private Long areaId;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @DecimalMin(value = "0.0", inclusive = true, message = "Ngân sách tham khảo không được âm")
    private BigDecimal budgetRef;

    private Integer biddingDeadlineDays;

    private LocalDateTime preferredTime;

    /** Thời điểm mong muốn thợ đến khảo sát/sửa chữa (mặc định: sau 1 ngày). */
    private LocalDateTime requestedTime;

    /** true = lưu bản nháp (DRAFT), false/null = đăng ngay để nhận báo giá. */
    private Boolean saveAsDraft;

    /**
     * Danh sách URL tệp đã upload lên Firebase Storage (tối đa 6 tệp).
     */
    @Size(max = 6, message = "Tối đa 6 tệp đính kèm cho mỗi yêu cầu")
    private List<@Size(max = 800000, message = "URL tệp đính kèm vượt quá dung lượng cho phép") String> mediaUrls;

    public void setAddress(String address) {
        this.address = address;
        if (this.addressLine == null) {
            this.addressLine = address;
        }
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
        if (this.address == null) {
            this.address = addressLine;
        }
    }

    public String getAddress() {
        return address != null ? address : addressLine;
    }

    public String getAddressLine() {
        return addressLine != null ? addressLine : address;
    }

    public static class CreateRepairRequestRequestBuilder {
        public CreateRepairRequestRequestBuilder address(String address) {
            this.address = address;
            if (this.addressLine == null) {
                this.addressLine = address;
            }
            return this;
        }

        public CreateRepairRequestRequestBuilder addressLine(String addressLine) {
            this.addressLine = addressLine;
            if (this.address == null) {
                this.address = addressLine;
            }
            return this;
        }
    }
}
