package com.fixlink.application.service;

import com.fixlink.adapter.out.persistence.entity.PriceQuoteJpaEntity;
import com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity;
import com.fixlink.adapter.out.persistence.repository.SpringDataPriceQuoteRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataRepairRequestRepository;
import com.fixlink.domain.exception.DomainException;
import com.fixlink.domain.model.PriceQuoteStatus;
import com.fixlink.domain.model.RequestStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceQuoteService {

    private final SpringDataPriceQuoteRepository quoteRepo;
    private final SpringDataRepairRequestRepository requestRepo;

    @Transactional
    public PriceQuoteJpaEntity submitQuote(Long requestId, Long technicianId,
                                           BigDecimal price, String note) {
        var request = requestRepo.findById(requestId)
                .filter(r -> r.getDeletedAt() == null)
                .orElseThrow(() -> new DomainException("QUOTE_REQUEST_NOT_FOUND",
                        "Không tìm thấy yêu cầu sửa chữa.", 404));

        if (request.getStatus() != RequestStatus.OPEN) {
            throw new DomainException("QUOTE_REQUEST_NOT_OPEN",
                    "Yêu cầu không ở trạng thái mở để nhận báo giá.", 409);
        }

        if (request.getBudgetRef() != null && request.getBudgetRef().compareTo(BigDecimal.ZERO) > 0
                && price.compareTo(request.getBudgetRef()) > 0) {
            throw new DomainException("PRICE_EXCEEDS_BUDGET",
                    "Báo giá (" + price + " VNĐ) không được vượt quá trần ngân sách (" + request.getBudgetRef() + " VNĐ)", 400);
        }

        quoteRepo.findByRequestIdAndTechnicianIdAndDeletedAtIsNull(requestId, technicianId)
                .ifPresent(q -> {
                    throw new DomainException("QUOTE_ALREADY_EXISTS",
                            "Bạn đã gửi báo giá cho yêu cầu này rồi.", 409);
                });

        var quote = PriceQuoteJpaEntity.builder()
                .requestId(requestId)
                .technicianId(technicianId)
                .quotedPrice(price)
                .note(note)
                .build();
        quote.setCreatedBy(technicianId);
        return quoteRepo.save(quote);
    }

    @Transactional
    public void acceptQuote(Long quoteId, Long customerId) {
        PriceQuoteJpaEntity quote = quoteRepo.findById(quoteId)
                .filter(q -> q.getDeletedAt() == null)
                .orElseThrow(() -> new DomainException("QUOTE_NOT_FOUND",
                        "Không tìm thấy báo giá.", 404));

        RepairRequestJpaEntity request = requestRepo.findById(quote.getRequestId())
                .filter(r -> r.getDeletedAt() == null)
                .orElseThrow(() -> new DomainException("QUOTE_REQUEST_NOT_FOUND",
                        "Không tìm thấy yêu cầu sửa chữa.", 404));

        if (!request.getCustomerId().equals(customerId)) {
            throw new DomainException("QUOTE_UNAUTHORIZED",
                    "Bạn không có quyền chọn báo giá cho yêu cầu này.", 403);
        }

        if (request.getStatus() != RequestStatus.OPEN) {
            throw new DomainException("QUOTE_REQUEST_NOT_OPEN",
                    "Yêu cầu không ở trạng thái để chọn báo giá.", 409);
        }

        quote.setStatus(PriceQuoteStatus.ACCEPTED);
        quoteRepo.save(quote);

        quoteRepo.findByRequestIdAndDeletedAtIsNull(quote.getRequestId()).stream()
                .filter(q -> !q.getId().equals(quoteId))
                .forEach(q -> {
                    q.setStatus(PriceQuoteStatus.REJECTED);
                    quoteRepo.save(q);
                });

        request.setTechnicianId(quote.getTechnicianId());
        request.setAgreedPrice(quote.getQuotedPrice());
        request.setStatus(RequestStatus.ASSIGNED);
        request.setUpdatedBy(customerId);
        requestRepo.save(request);
    }

    @Transactional(readOnly = true)
    public List<PriceQuoteJpaEntity> getQuotesByRequest(Long requestId) {
        return quoteRepo.findByRequestIdAndDeletedAtIsNull(requestId);
    }
}
