package com.fixlink.application.port.in;

import com.fixlink.adapter.in.web.dto.response.AcceptQuotationResponse;
import com.fixlink.adapter.in.web.dto.response.QuotationResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface QuotationUseCase {

    QuotationResponse create(Long requestId, CreateQuotationCommand command, Long technicianId);

    QuotationResponse update(Long requestId, Long quotationId, UpdateQuotationCommand command, Long technicianId);

    void withdraw(Long requestId, Long quotationId, Long technicianId);

    List<QuotationResponse> getQuotationsForRequest(Long requestId, Long customerId);

    AcceptQuotationResponse accept(Long requestId, Long quotationId, Long customerId);

    List<QuotationResponse> getMyQuotations(Long technicianId, com.fixlink.domain.model.QuotationStatus status);

    QuotationResponse getQuotationByIdForTechnician(Long quotationId, Long technicianId);

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
