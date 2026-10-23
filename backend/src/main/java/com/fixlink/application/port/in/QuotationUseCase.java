package com.fixlink.application.port.in;

import com.fixlink.adapter.in.web.dto.response.AcceptQuotationResponse;
import com.fixlink.adapter.in.web.dto.response.QuotationResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface QuotationUseCase {

    /**
     * Thợ nhận việc theo mô hình "ai nhận trước được trước" (RC-pivot).
     *
     * <p>Thay thế hoàn toàn luồng cũ {@code create (gửi báo giá) → accept
     * (khách duyệt)}. Giá lấy từ {@code budgetRef} của yêu cầu, thợ không nhập
     * giá. Chỉ thợ đầu tiên thành công; các thợ sau nhận lỗi
     * {@code JOB_ALREADY_TAKEN} (HTTP 409).
     */
    AcceptQuotationResponse apply(Long requestId, Long technicianId);

    QuotationResponse create(Long requestId, CreateQuotationCommand command, Long technicianId);

    QuotationResponse update(Long requestId, Long quotationId, UpdateQuotationCommand command, Long technicianId);

    void withdraw(Long requestId, Long quotationId, Long technicianId);

    List<QuotationResponse> getQuotationsForRequest(Long requestId, Long customerId);

    List<QuotationResponse> getQuotationsForRequest(Long requestId, Long userId, String role);

    AcceptQuotationResponse accept(Long requestId, Long quotationId, Long customerId);

    @Data
    @Builder
    class CreateQuotationCommand {
        private String solution;
        private BigDecimal priceLaborVnd;
        private BigDecimal priceMaterialsVnd;
        private LocalDateTime inspectionTime;
        private LocalDateTime estimatedFinish;
        private String note;
    }

    @Data
    @Builder
    class UpdateQuotationCommand {
        private String solution;
        private BigDecimal priceLaborVnd;
        private BigDecimal priceMaterialsVnd;
        private LocalDateTime inspectionTime;
        private LocalDateTime estimatedFinish;
        private String note;
    }
}
