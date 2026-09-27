package com.fixlink.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bản ghi thợ nhận việc của một yêu cầu sửa chữa.
 *
 * <p><b>Lưu ý khi dùng lại lớp này.</b> Sau khi pivot sang mô hình "ai nhận
 * trước được trước", bảng {@code quotations} không còn là "nhiều báo giá cạnh
 * tranh" nữa: mỗi yêu cầu có <b>tối đa một</b> bản ghi, tạo ra tại thời điểm thợ
 * bấm "Nhận việc", với {@code status = ACCEPTED} và giá lấy nguyên từ
 * {@code budgetRef} của yêu cầu. Hai field {@code priceLaborVnd} /
 * {@code priceMaterialsVnd} chỉ còn để đọc dữ liệu cũ — luồng mới đặt toàn bộ
 * giá vào {@code priceLaborVnd} và để vật tư bằng 0.
 *
 * <p>Lớp này hiện <b>chưa được service nào sử dụng</b> (production làm việc trực
 * tiếp trên {@code QuotationJpaEntity}). Nếu sau này wire vào, đừng thêm lại các
 * luật của mô hình đấu giá cũ (so sánh nhiều báo giá, auto-reject các báo giá
 * khác, thợ sửa/rút báo giá) — chúng không còn đúng nghiệp vụ.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Quotation {
    private Long id;
    private Long requestId;
    private Long technicianId;
    private String solution;
    private BigDecimal priceLaborVnd;
    private BigDecimal priceMaterialsVnd;
    private LocalDateTime inspectionTime;
    private LocalDateTime estimatedFinish;
    private String note;
    private QuotationStatus status;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;

    public BigDecimal totalPrice() {
        BigDecimal labor = priceLaborVnd != null ? priceLaborVnd : BigDecimal.ZERO;
        BigDecimal materials = priceMaterialsVnd != null ? priceMaterialsVnd : BigDecimal.ZERO;
        return labor.add(materials);
    }
}
