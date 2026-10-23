package com.fixlink.application.service;

import com.fixlink.adapter.out.persistence.entity.RepairRequestReviewJpaEntity;
import com.fixlink.adapter.out.persistence.repository.SpringDataRepairRequestRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataRepairRequestReviewRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataTechnicianProfileRepository;
import com.fixlink.application.port.in.RepairRequestReviewUseCase;
import com.fixlink.application.port.in.RepairRequestUseCase;
import com.fixlink.domain.model.RequestStatus;
import com.fixlink.domain.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RepairRequestReviewService implements RepairRequestReviewUseCase {
    private final SpringDataRepairRequestRepository requestRepository;
    private final SpringDataRepairRequestReviewRepository reviewRepository;
    private final SpringDataTechnicianProfileRepository technicianProfileRepository;
    private final RepairRequestUseCase repairRequestUseCase;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getReview(Long requestId, Long customerId) {
        var request = requestRepository.findById(requestId)
                .filter(item -> item.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repair request not found."));
        ensureOwner(request.getCustomerId(), customerId);
        return reviewRepository.findByRequestIdAndDeletedAtIsNull(requestId)
                .map(RepairRequestReviewService::toMap)
                .orElse(null);
    }

    @Override
    @Transactional
    public Map<String, Object> submitReviewAndComplete(Long requestId, Long customerId, int rating, String comment) {
        var request = requestRepository.findByIdForUpdate(requestId)
                .filter(item -> item.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repair request not found."));
        ensureOwner(request.getCustomerId(), customerId);
        if (request.getStatus() != RequestStatus.AWAITING_ACCEPTANCE && request.getStatus() != RequestStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Review is available after repair work is completed.");
        }
        if (request.getTechnicianId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Repair request has no assigned technician.");
        }

        var review = reviewRepository.findByRequestIdAndDeletedAtIsNull(requestId).orElse(null);
        if (review == null) {
            review = RepairRequestReviewJpaEntity.builder()
                    .requestId(requestId)
                    .customerId(customerId)
                    .technicianId(request.getTechnicianId())
                    .rating(rating)
                    .comment(comment.trim())
                    .build();
            review.setCreatedBy(customerId);
            review = reviewRepository.save(review);

            var profile = technicianProfileRepository.findByUserIdAndDeletedAtIsNullForUpdate(request.getTechnicianId())
                    .orElse(null);
            if (profile != null) {
                double average = reviewRepository.findByTechnicianIdAndDeletedAtIsNull(request.getTechnicianId())
                        .stream().mapToInt(RepairRequestReviewJpaEntity::getRating).average().orElse(0);
                profile.setAvgRating(BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
                technicianProfileRepository.save(profile);
            }
        }

        if (request.getStatus() == RequestStatus.AWAITING_ACCEPTANCE) {
            repairRequestUseCase.updateStatus(new RepairRequestUseCase.UpdateStatusCommand(
                    requestId, RequestStatus.COMPLETED, "Customer reviewed and accepted the completed repair.",
                    customerId, Role.CUSTOMER));
        }
        return toMap(review);
    }

    private static void ensureOwner(Long ownerId, Long customerId) {
        if (!ownerId.equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the customer who created this request can review it.");
        }
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
