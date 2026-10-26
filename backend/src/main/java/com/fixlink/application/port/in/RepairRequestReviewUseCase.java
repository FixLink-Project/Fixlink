package com.fixlink.application.port.in;

import java.util.List;
import java.util.Map;

public interface RepairRequestReviewUseCase {
    Map<String, Object> getReview(Long requestId, Long customerId);

    Map<String, Object> submitReviewAndComplete(Long requestId, Long customerId, int rating, String comment);

    List<Map<String, Object>> getReviewsForTechnician(Long technicianId);
}
