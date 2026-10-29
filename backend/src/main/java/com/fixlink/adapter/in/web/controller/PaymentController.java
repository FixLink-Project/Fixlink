package com.fixlink.adapter.in.web.controller;

import com.fixlink.adapter.in.web.dto.response.ApiResponse;
import com.fixlink.adapter.out.persistence.entity.BillJpaEntity;
import com.fixlink.adapter.out.persistence.entity.PaymentJpaEntity;
import com.fixlink.application.service.BillPdfService;
import com.fixlink.application.service.PaymentService;
import com.fixlink.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Thanh toán QR: đặt cọc 30% + thanh toán tổng")
public class PaymentController {

    private final PaymentService paymentService;
    private final BillPdfService billPdfService;

    @PostMapping("/deposit/{requestId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createDeposit(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal principal) {
        var result = paymentService.createDepositPayment(requestId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Đã tạo QR đặt cọc.", toPaymentResultMap(result)));
    }

    @PostMapping("/final/{requestId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createFinalPayment(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal principal) {
        var result = paymentService.createFinalPayment(requestId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Đã tạo QR thanh toán tổng.", toPaymentResultMap(result)));
    }

    @PostMapping("/{paymentId}/confirm")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmPayment(
            @PathVariable Long paymentId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        String method = (body != null && body.containsKey("paymentMethod")) ? body.get("paymentMethod") : "VIETQR";
        var bill = paymentService.confirmPayment(paymentId, principal.getId(), method);
        return ResponseEntity.ok(ApiResponse.success("Đã xác nhận thanh toán.", toBillMap(bill)));
    }

    @GetMapping("/request/{requestId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPayments(
            @PathVariable Long requestId) {
        var payments = paymentService.getPaymentsByRequest(requestId);
        return ResponseEntity.ok(ApiResponse.success(payments.stream().map(this::toPaymentMap).toList()));
    }

    @GetMapping("/bills/request/{requestId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBills(
            @PathVariable Long requestId) {
        var bills = paymentService.getBillsByRequest(requestId);
        return ResponseEntity.ok(ApiResponse.success(bills.stream().map(this::toBillMap).toList()));
    }

    @GetMapping("/bills/{billId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBill(@PathVariable Long billId) {
        var bill = paymentService.getBillById(billId);
        return ResponseEntity.ok(ApiResponse.success(toBillMap(bill)));
    }

    @GetMapping(value = "/bills/{billId}/html", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> getBillHtml(@PathVariable Long billId) {
        var bill = paymentService.getBillById(billId);
        return ResponseEntity.ok(billPdfService.generateBillHtml(bill));
    }

    private Map<String, Object> toPaymentResultMap(PaymentService.PaymentResult r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("paymentId", r.paymentId());
        m.put("qrUrl", r.qrUrl());
        m.put("amount", r.amount());
        m.put("transferContent", r.transferContent());
        m.put("bankInfo", Map.of(
                "bankId", r.bankInfo().bankId(),
                "accountNo", r.bankInfo().accountNo(),
                "accountName", r.bankInfo().accountName(),
                "bankName", r.bankInfo().bankName()
        ));
        return m;
    }

    private Map<String, Object> toPaymentMap(PaymentJpaEntity p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("requestId", p.getRequestId());
        m.put("paymentType", p.getPaymentType());
        m.put("amount", p.getAmount());
        m.put("status", p.getStatus());
        m.put("transferContent", p.getTransferContent());
        m.put("qrUrl", p.getQrUrl());
        m.put("paymentMethod", p.getPaymentMethod() != null ? p.getPaymentMethod() : "VIETQR");
        m.put("createdAt", p.getCreatedAt());
        m.put("confirmedAt", p.getConfirmedAt());
        return m;
    }

    private Map<String, Object> toBillMap(BillJpaEntity b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("billNumber", b.getBillNumber());
        m.put("paymentId", b.getPaymentId());
        m.put("billType", b.getBillType());
        m.put("customerName", b.getCustomerName());
        m.put("customerPhone", b.getCustomerPhone());
        m.put("customerAddress", b.getCustomerAddress());
        m.put("technicianName", b.getTechnicianName());
        m.put("technicianPhone", b.getTechnicianPhone());
        m.put("serviceCategory", b.getServiceCategory());
        m.put("serviceDescription", b.getServiceDescription());
        m.put("requestId", b.getRequestId());
        m.put("quotedPrice", b.getQuotedPrice());
        m.put("depositAmount", b.getDepositAmount());
        m.put("additionalCost", b.getAdditionalCost());
        m.put("totalAmount", b.getTotalAmount());
        m.put("paymentMethod", b.getPaymentMethod() != null ? b.getPaymentMethod() : "VIETQR");
        m.put("createdAt", b.getCreatedAt());
        return m;
    }
}
