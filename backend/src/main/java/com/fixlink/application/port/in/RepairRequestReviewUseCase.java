package com.fixlink.application.port.in;

import java.util.Map;

public interface RepairRequestReviewUseCase {
    Map<String, Object> getReview(Long requestId, Long customerId);

    Map<String, Object> submitReviewAndComplete(Long requestId, Long customerId, int rating, String comment);
}
