package com.fixlink.application.service;

import com.fixlink.adapter.in.web.dto.response.AcceptQuotationResponse;
import com.fixlink.adapter.out.persistence.entity.QuotationJpaEntity;
import com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity;
import com.fixlink.adapter.out.persistence.entity.TechnicianProfileJpaEntity;
import com.fixlink.adapter.out.persistence.repository.SpringDataAppointmentRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataQuotationRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataRepairRequestRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataTechnicianProfileRepository;
import com.fixlink.application.port.in.QuotationUseCase;
import com.fixlink.domain.exception.DomainException;
import com.fixlink.domain.model.QuotationStatus;
import com.fixlink.domain.model.RequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuotationSwitchTechnicianAndBudgetTest {

    @Mock
    private SpringDataQuotationRepository quotationRepo;

    @Mock
    private SpringDataRepairRequestRepository requestRepo;

    @Mock
    private SpringDataTechnicianProfileRepository techProfileRepo;

    @Mock
    private SpringDataAppointmentRepository appointmentRepo;

    @Mock
    private RepairRequestService repairRequestService;

    @InjectMocks
    private QuotationService quotationService;

    private RepairRequestJpaEntity request;
    private QuotationJpaEntity quoteOld;
    private QuotationJpaEntity quoteNew;
    private TechnicianProfileJpaEntity techProfileNew;

    @BeforeEach
    void setUp() {
        request = new RepairRequestJpaEntity();
        request.setId(10L);
        request.setCustomerId(1L);
        request.setTechnicianId(2L);
        request.setSelectedQuotationId(101L);
        request.setStatus(RequestStatus.ASSIGNED);
        request.setBudgetRef(new BigDecimal("500000"));
        request.setAgreedPrice(new BigDecimal("450000"));
        request.setDepositAmount(new BigDecimal("135000"));

        quoteOld = new QuotationJpaEntity();
        quoteOld.setId(101L);
        quoteOld.setRequestId(10L);
        quoteOld.setTechnicianId(2L);
        quoteOld.setStatus(QuotationStatus.ACCEPTED);
        quoteOld.setPriceLaborVnd(new BigDecimal("450000"));

        quoteNew = new QuotationJpaEntity();
        quoteNew.setId(102L);
        quoteNew.setRequestId(10L);
        quoteNew.setTechnicianId(3L);
        quoteNew.setStatus(QuotationStatus.PENDING);
        quoteNew.setPriceLaborVnd(new BigDecimal("400000"));
        quoteNew.setPriceMaterialsVnd(new BigDecimal("50000"));

        techProfileNew = new TechnicianProfileJpaEntity();
        techProfileNew.setUserId(3L);
        techProfileNew.setFullName("Thợ Mới Nguyễn Văn B");
    }

    @Test
    @DisplayName("Đổi thợ thành công khi đơn ở trạng thái ASSIGNED")
    void switchTechnician_success_whenAssigned() {
        when(requestRepo.findById(10L)).thenReturn(Optional.of(request));
        when(quotationRepo.findById(102L)).thenReturn(Optional.of(quoteNew));
        when(quotationRepo.findById(101L)).thenReturn(Optional.of(quoteOld));
        when(techProfileRepo.findById(3L)).thenReturn(Optional.of(techProfileNew));
        when(requestRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AcceptQuotationResponse response = quotationService.switchTechnician(10L, 102L, 1L);

        assertNotNull(response);
        assertEquals(3L, request.getTechnicianId());
        assertEquals(102L, request.getSelectedQuotationId());
        assertEquals(RequestStatus.ASSIGNED, request.getStatus());
        assertEquals(QuotationStatus.REJECTED, quoteOld.getStatus());
        assertEquals(QuotationStatus.ACCEPTED, quoteNew.getStatus());
        assertEquals(new BigDecimal("450000"), response.getAgreedPrice());
        verify(repairRequestService).recordProgress(eq(10L), eq(RequestStatus.ASSIGNED), eq(RequestStatus.ASSIGNED),
                contains("Thợ Mới Nguyễn Văn B"), eq(1L));
    }

    @Test
    @DisplayName("Đổi thợ thành công khi đơn ở trạng thái INSPECTING -> reset về DEPOSIT_PAID")
    void switchTechnician_success_whenInspecting_resetsToDepositPaid() {
        request.setStatus(RequestStatus.INSPECTING);

        when(requestRepo.findById(10L)).thenReturn(Optional.of(request));
        when(quotationRepo.findById(102L)).thenReturn(Optional.of(quoteNew));
        when(quotationRepo.findById(101L)).thenReturn(Optional.of(quoteOld));
        when(techProfileRepo.findById(3L)).thenReturn(Optional.of(techProfileNew));
        when(requestRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AcceptQuotationResponse response = quotationService.switchTechnician(10L, 102L, 1L);

        assertNotNull(response);
        assertEquals(RequestStatus.DEPOSIT_PAID, request.getStatus());
        assertEquals(RequestStatus.DEPOSIT_PAID, response.getStatus());
        verify(repairRequestService).recordProgress(eq(10L), eq(RequestStatus.INSPECTING), eq(RequestStatus.DEPOSIT_PAID),
                contains("Thợ Mới Nguyễn Văn B"), eq(1L));
    }

    @Test
    @DisplayName("Chặn đổi thợ nếu không phải chủ yêu cầu (CUSTOMER)")
    void switchTechnician_unauthorized_throwsAccessDenied() {
        when(requestRepo.findById(10L)).thenReturn(Optional.of(request));

        DomainException ex = assertThrows(DomainException.class,
                () -> quotationService.switchTechnician(10L, 102L, 999L));
        assertEquals("ACCESS_DENIED", ex.getErrorCode());
        assertEquals(403, ex.getStatusCode());
    }

    @Test
    @DisplayName("Chặn đổi thợ khi đơn đã bắt đầu thi công IN_PROGRESS")
    void switchTechnician_inProgress_throwsInvalidOperation() {
        request.setStatus(RequestStatus.IN_PROGRESS);
        when(requestRepo.findById(10L)).thenReturn(Optional.of(request));

        DomainException ex = assertThrows(DomainException.class,
                () -> quotationService.switchTechnician(10L, 102L, 1L));
        assertEquals("INVALID_OPERATION", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("IN_PROGRESS"));
    }

    @Test
    @DisplayName("Chặn đổi sang chính thợ hiện tại đang phụ trách")
    void switchTechnician_sameTechnician_throwsInvalidOperation() {
        quoteNew.setTechnicianId(2L); // Trùng thợ cũ
        when(requestRepo.findById(10L)).thenReturn(Optional.of(request));
        when(quotationRepo.findById(102L)).thenReturn(Optional.of(quoteNew));

        DomainException ex = assertThrows(DomainException.class,
                () -> quotationService.switchTechnician(10L, 102L, 1L));
        assertEquals("INVALID_OPERATION", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("đang là thợ phụ trách"));
    }

    @Test
    @DisplayName("Chặn báo giá vượt quá trần ngân sách budgetRef của yêu cầu khi thợ tạo báo giá")
    void createQuotation_exceedsBudgetRef_throws400() {
        request.setStatus(RequestStatus.OPEN);
        request.setBudgetRef(new BigDecimal("500000"));
        when(requestRepo.findById(10L)).thenReturn(Optional.of(request));

        TechnicianProfileJpaEntity tech = new TechnicianProfileJpaEntity();
        tech.setUserId(3L);
        tech.setVerificationStatus(com.fixlink.domain.model.VerificationStatus.APPROVED);
        tech.setCategoryIds(java.util.Set.of());
        when(techProfileRepo.findById(3L)).thenReturn(Optional.of(tech));

        // category check bypass for test or set category
        request.setCategoryId(1L);
        tech.setCategoryIds(java.util.Set.of(1L));

        QuotationUseCase.CreateQuotationCommand cmd = QuotationUseCase.CreateQuotationCommand.builder()
                .priceLaborVnd(new BigDecimal("400000"))
                .priceMaterialsVnd(new BigDecimal("150000")) // Tổng 550.000 > 500.000
                .solution("Sửa bo mạch")
                .build();

        DomainException ex = assertThrows(DomainException.class,
                () -> quotationService.create(10L, cmd, 3L));
        assertEquals("PRICE_EXCEEDS_BUDGET", ex.getErrorCode());
        assertEquals(400, ex.getStatusCode());
    }
}
