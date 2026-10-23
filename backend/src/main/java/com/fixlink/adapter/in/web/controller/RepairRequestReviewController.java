package com.fixlink.adapter.in.web.controller;

import com.fixlink.adapter.in.web.dto.request.CreateRepairRequestReviewRequest;
import com.fixlink.adapter.in.web.dto.response.ApiResponse;
import com.fixlink.adapter.out.persistence.entity.RepairRequestReviewJpaEntity;
import com.fixlink.adapter.out.persistence.repository.SpringDataRepairRequestRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataRepairRequestReviewRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataTechnicianProfileRepository;
import com.fixlink.domain.model.RequestStatus;
import com.fixlink.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/repair-requests/{requestId}/review")
public class RepairRequestReviewController {
    private final SpringDataRepairRequestRepository requestRepository;
    private final SpringDataRepairRequestReviewRepository reviewRepository;
    private final SpringDataTechnicianProfileRepository technicianProfileRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReview(
            @PathVariable Long requestId, @AuthenticationPrincipal UserPrincipal principal) {
        var request = requestRepository.findById(requestId)
                .filter(item -> item.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu."));
        if (!request.getCustomerId().equals(principal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem đánh giá này.");
        }
        var review = reviewRepository.findByRequestIdAndDeletedAtIsNull(requestId).orElse(null);
        return ResponseEntity.ok(ApiResponse.success(review == null ? null : toMap(review)));
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitReview(
            @PathVariable Long requestId,
            @Valid @RequestBody CreateRepairRequestReviewRequest body,
            @AuthenticationPrincipal UserPrincipal principal) {
        var request = requestRepository.findById(requestId)
                .filter(item -> item.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu."));
        if (!request.getCustomerId().equals(principal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ khách đã tạo yêu cầu mới được đánh giá.");
        }
        if (request.getStatus() != RequestStatus.AWAITING_ACCEPTANCE && request.getStatus() != RequestStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ đánh giá được khi thợ đã báo hoàn thành công việc.");
        }
        var existing = reviewRepository.findByRequestIdAndDeletedAtIsNull(requestId);
        if (existing.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(toMap(existing.get())));
        }
        if (request.getTechnicianId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Yêu cầu chưa được giao cho thợ.");
        }
        var newReview = RepairRequestReviewJpaEntity.builder()
                .requestId(requestId)
                .customerId(principal.getId())
                .technicianId(request.getTechnicianId())
                .rating(body.getRating())
                .comment(body.getComment().trim())
                .build();
        newReview.setCreatedBy(principal.getId());
        var saved = reviewRepository.save(newReview);

        technicianProfileRepository.findByUserIdAndDeletedAtIsNull(request.getTechnicianId()).ifPresent(profile -> {
            var reviews = reviewRepository.findByTechnicianIdAndDeletedAtIsNull(request.getTechnicianId());
            double average = reviews.stream().mapToInt(RepairRequestReviewJpaEntity::getRating).average().orElse(0);
            profile.setAvgRating(BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
            technicianProfileRepository.save(profile);
        });
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Đã ghi nhận đánh giá.", toMap(saved)));
    }

    private static Map<String, Object> toMap(RepairRequestReviewJpaEntity review) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", review.getId());
        result.put("requestId", review.getRequestId());
        result.put("rating", review.getRating());
        result.put("comment", review.getComment());
        result.put("createdAt", review.getCreatedAt());
        return result;
    }
}
