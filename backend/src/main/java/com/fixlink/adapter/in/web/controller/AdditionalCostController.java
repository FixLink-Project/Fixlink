package com.fixlink.adapter.in.web.controller;

import com.fixlink.adapter.in.web.dto.response.ApiResponse;
import com.fixlink.adapter.out.persistence.entity.AdditionalCostJpaEntity;
import com.fixlink.application.service.AdditionalCostService;
import com.fixlink.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
@RequestMapping("/api/v1/additional-costs")
@Tag(name = "Additional Costs", description = "Chi phí phát sinh: thợ đăng, khách duyệt")
public class AdditionalCostController {

    private final AdditionalCostService costService;

    @PostMapping
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addCost(
            @Valid @RequestBody AddCostRequest body,
            @AuthenticationPrincipal UserPrincipal principal) {
        var cost = costService.addCost(body.getRequestId(), principal.getId(),
                body.getDescription(), body.getAmount());
        return ResponseEntity.ok(ApiResponse.created("Đã thêm chi phí phát sinh.", toMap(cost)));
    }

    @PostMapping("/{costId}/approve")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveCost(
            @PathVariable Long costId,
            @AuthenticationPrincipal UserPrincipal principal) {
        var cost = costService.approveCost(costId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Đã duyệt chi phí phát sinh.", toMap(cost)));
    }

    @PostMapping("/{costId}/reject")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rejectCost(
            @PathVariable Long costId,
            @AuthenticationPrincipal UserPrincipal principal) {
        var cost = costService.rejectCost(costId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Đã từ chối chi phí phát sinh.", toMap(cost)));
    }

    @GetMapping("/request/{requestId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCosts(
            @PathVariable Long requestId) {
        var costs = costService.getCostsByRequest(requestId);
        return ResponseEntity.ok(ApiResponse.success(costs.stream().map(this::toMap).toList()));
    }

    private Map<String, Object> toMap(AdditionalCostJpaEntity c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("requestId", c.getRequestId());
        m.put("description", c.getDescription());
        m.put("amount", c.getAmount());
        m.put("status", c.getStatus());
        m.put("createdAt", c.getCreatedAt());
        m.put("approvedAt", c.getApprovedAt());
        return m;
    }

    @Data
    public static class AddCostRequest {
        @NotNull private Long requestId;
        @NotBlank private String description;
        @NotNull @Positive private BigDecimal amount;
    }
}
