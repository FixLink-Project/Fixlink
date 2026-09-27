package com.fixlink.infrastructure.scheduler;

import com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity;
import com.fixlink.adapter.out.persistence.repository.SpringDataRepairRequestRepository;
import com.fixlink.application.service.RepairRequestService;
import com.fixlink.domain.model.RequestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tự hủy các yêu cầu mở quá 3 ngày mà không có thợ nào nhận.
 *
 * <p>Thuộc mô hình "ai nhận trước được trước": yêu cầu chỉ sống trong cửa sổ
 * {@code apply_deadline = createdAt + 3 ngày}. Hết hạn thì chuyển sang
 * {@link RequestStatus#CANCELLED} kèm lý do tự động.
 *
 * <p>Lưu ý: endpoint {@code /repair-requests/matching} đã lọc kép theo
 * {@code apply_deadline} nên kể cả khi job này chưa kịp chạy, thợ cũng không
 * bao giờ thấy yêu cầu hết hạn. Job chỉ làm nốt việc dọn trạng thái.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredRequestScheduler {

    private static final String CANCEL_REASON = "Hết hạn 3 ngày không có thợ nhận";

    private final SpringDataRepairRequestRepository requestRepo;
    private final RepairRequestService repairRequestService;

    /** Chạy mỗi 15 phút. */
    @Scheduled(fixedDelayString = "${fixlink.scheduler.expire-requests-delay-ms:900000}")
    @Transactional
    public void cancelExpiredOpenRequests() {
        List<RepairRequestJpaEntity> expired = requestRepo.findExpiredOpenRequests(LocalDateTime.now());
        if (expired.isEmpty()) {
            return;
        }

        for (RepairRequestJpaEntity request : expired) {
            RequestStatus oldStatus = request.getStatus();
            request.setStatus(RequestStatus.CANCELLED);
            request.setCancelReason(CANCEL_REASON);
            requestRepo.save(request);

            repairRequestService.recordProgress(
                    request.getId(), oldStatus, RequestStatus.CANCELLED, CANCEL_REASON, null);
        }

        log.info("Đã tự hủy {} yêu cầu hết hạn 3 ngày không có thợ nhận", expired.size());
    }
}
