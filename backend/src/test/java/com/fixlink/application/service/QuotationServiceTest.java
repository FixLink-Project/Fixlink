package com.fixlink.application.service;

import com.fixlink.adapter.out.persistence.entity.QuotationJpaEntity;
import com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity;
import com.fixlink.adapter.out.persistence.entity.TechnicianProfileJpaEntity;
import com.fixlink.adapter.out.persistence.repository.SpringDataQuotationRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataRepairRequestRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataTechnicianProfileRepository;
import com.fixlink.domain.exception.DomainException;
import com.fixlink.domain.model.QuotationStatus;
import com.fixlink.domain.model.RequestStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuotationServiceTest {

    @Mock private SpringDataQuotationRepository quotationRepo;
    @Mock private SpringDataRepairRequestRepository requestRepo;
    @Mock private SpringDataTechnicianProfileRepository techProfileRepo;
    @Mock private RepairRequestService repairRequestService;
    @InjectMocks private QuotationService quotationService;

    @Test
    void accept_selectsOneQuotationRejectsOthersAndCalculatesDeposit() {
        RepairRequestJpaEntity request = requestInBidding();
        QuotationJpaEntity chosen = quotation(501L, 91L, "400000", "100001");
        QuotationJpaEntity other = quotation(502L, 92L, "200000", "0");
        when(requestRepo.findByIdForUpdate(40L)).thenReturn(Optional.of(request));
        when(quotationRepo.findById(501L)).thenReturn(Optional.of(chosen));
        when(quotationRepo.findByRequestIdAndStatus(40L, QuotationStatus.PENDING)).thenReturn(List.of(chosen, other));
        when(techProfileRepo.findById(91L)).thenReturn(Optional.of(TechnicianProfileJpaEntity.builder()
                .fullName("Technician A").build()));

        var result = quotationService.accept(40L, 501L, 7L);

        assertEquals(501L, result.getSelectedQuotationId());
        assertEquals(new BigDecimal("500001"), result.getAgreedPrice());
        assertEquals(new BigDecimal("150001"), result.getDepositAmount());
        assertEquals(RequestStatus.MATCHED_AWAITING_DEPOSIT, result.getStatus());
        assertEquals(QuotationStatus.ACCEPTED, chosen.getStatus());
        assertEquals(QuotationStatus.REJECTED, other.getStatus());
        assertEquals(501L, request.getSelectedQuotationId());
        assertEquals(91L, request.getTechnicianId());
        assertEquals(RequestStatus.MATCHED_AWAITING_DEPOSIT, request.getStatus());
        verify(requestRepo).findByIdForUpdate(40L);
        verify(repairRequestService).recordProgress(eq(40L), eq(RequestStatus.BIDDING_OPEN),
                eq(RequestStatus.MATCHED_AWAITING_DEPOSIT), anyString(), eq(7L));
    }

    @Test
    void accept_rejectsRequestOwnedByAnotherCustomer() {
        RepairRequestJpaEntity request = requestInBidding();
        request.setCustomerId(8L);
        when(requestRepo.findByIdForUpdate(40L)).thenReturn(Optional.of(request));

        assertThrows(DomainException.class, () -> quotationService.accept(40L, 501L, 7L));
        verifyNoInteractions(quotationRepo);
    }

    @Test
    void accept_rejectsWhenRequestIsNoLongerOpenForBidding() {
        RepairRequestJpaEntity request = requestInBidding();
        request.setStatus(RequestStatus.MATCHED_AWAITING_DEPOSIT);
        when(requestRepo.findByIdForUpdate(40L)).thenReturn(Optional.of(request));

        assertThrows(DomainException.class, () -> quotationService.accept(40L, 501L, 7L));
        verifyNoInteractions(quotationRepo);
    }

    private RepairRequestJpaEntity requestInBidding() {
        return RepairRequestJpaEntity.builder()
                .id(40L).customerId(7L).status(RequestStatus.BIDDING_OPEN).build();
    }

    private QuotationJpaEntity quotation(Long id, Long technicianId, String labor, String materials) {
        return QuotationJpaEntity.builder()
                .id(id).requestId(40L).technicianId(technicianId)
                .priceLaborVnd(new BigDecimal(labor)).priceMaterialsVnd(new BigDecimal(materials))
                .status(QuotationStatus.PENDING).build();
    }
}
