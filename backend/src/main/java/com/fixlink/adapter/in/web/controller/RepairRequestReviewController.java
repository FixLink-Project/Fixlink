package com.fixlink.adapter.in.web.controller;

import com.fixlink.adapter.in.web.dto.request.CreateRepairRequestReviewRequest;
import com.fixlink.adapter.in.web.dto.response.ApiResponse;
import com.fixlink.application.port.in.RepairRequestReviewUseCase;
import com.fixlink.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/repair-requests/{requestId}/review")
public class RepairRequestReviewController {
    private final RepairRequestReviewUseCase reviewUseCase;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReview(
            @PathVariable Long requestId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(reviewUseCase.getReview(requestId, principal.getId())));
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitReview(
            @PathVariable Long requestId,
            @Valid @RequestBody CreateRepairRequestReviewRequest body,
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> review = reviewUseCase.submitReviewAndComplete(
                requestId, principal.getId(), body.getRating(), body.getComment());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Review saved.", review));
    }
}
