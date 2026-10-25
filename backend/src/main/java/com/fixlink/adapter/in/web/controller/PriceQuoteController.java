package com.fixlink.adapter.in.web.controller;

import com.fixlink.adapter.in.web.dto.response.ApiResponse;
import com.fixlink.adapter.out.persistence.entity.PriceQuoteJpaEntity;
import com.fixlink.application.service.PriceQuoteService;
import com.fixlink.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/price-quotes")
@Tag(name = "Price Quotes", description = "Deal giá: thợ gửi báo giá, khách chọn")
public class PriceQuoteController {

    private final PriceQuoteService quoteService;

    @PostMapping
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitQuote(
            @Valid @RequestBody SubmitQuoteRequest body,
            @AuthenticationPrincipal UserPrincipal principal) {
        var quote = quoteService.submitQuote(
                body.getRequestId(), principal.getId(), body.getPrice(), body.getNote());
        return ResponseEntity.ok(ApiResponse.created("Đã gửi báo giá.", toMap(quote)));
    }

    @PostMapping("/{quoteId}/accept")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> acceptQuote(
            @PathVariable Long quoteId,
            @AuthenticationPrincipal UserPrincipal principal) {
        quoteService.acceptQuote(quoteId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Đã chọn báo giá và giao thợ.", null));
    }

    @GetMapping("/request/{requestId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getQuotes(
            @PathVariable Long requestId) {
        var quotes = quoteService.getQuotesByRequest(requestId);
        return ResponseEntity.ok(ApiResponse.success(quotes.stream().map(this::toMap).toList()));
    }

    private Map<String, Object> toMap(PriceQuoteJpaEntity q) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", q.getId());
        m.put("requestId", q.getRequestId());
        m.put("technicianId", q.getTechnicianId());
        m.put("quotedPrice", q.getQuotedPrice());
        m.put("note", q.getNote());
        m.put("status", q.getStatus());
        m.put("createdAt", q.getCreatedAt());
        return m;
    }

    @Data
    public static class SubmitQuoteRequest {
        @NotNull private Long requestId;
        @NotNull @Positive private BigDecimal price;
        private String note;
    }
}
