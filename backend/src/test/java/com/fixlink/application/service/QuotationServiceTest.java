package com.fixlink.application.service;

import com.fixlink.adapter.in.web.dto.response.QuotationResponse;
import com.fixlink.adapter.out.persistence.entity.QuotationJpaEntity;
import com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity;
import com.fixlink.adapter.out.persistence.entity.TechnicianProfileJpaEntity;
import com.fixlink.adapter.out.persistence.repository.SpringDataQuotationRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataRepairRequestRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataTechnicianProfileRepository;
import com.fixlink.application.port.in.QuotationUseCase;
import com.fixlink.domain.exception.DomainException;
import com.fixlink.domain.exception.ResourceNotFoundException;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests - QuotationService (RC-40, RC-41, RC-42)")
class QuotationServiceTest {

    @Mock
    private SpringDataQuotationRepository quotationRepo;

    @Mock
    private SpringDataRepairRequestRepository requestRepo;

    @Mock
    private SpringDataTechnicianProfileRepository techProfileRepo;

    @Mock
    private RepairRequestService repairRequestService;

    @InjectMocks
    private QuotationService quotationService;

    private RepairRequestJpaEntity openRequest;
    private QuotationJpaEntity pendingQuotation;
    private QuotationUseCase.CreateQuotationCommand createCmd;
    private QuotationUseCase.UpdateQuotationCommand updateCmd;

    @BeforeEach
    void setUp() {
        openRequest = new RepairRequestJpaEntity();
        openRequest.setId(100L);
        openRequest.setCustomerId(10L);
        openRequest.setStatus(RequestStatus.BIDDING_OPEN);

        pendingQuotation = QuotationJpaEntity.builder()
                .id(1L)
                .requestId(100L)
                .technicianId(20L)
                .solution("Thay thế linh kiện hỏng")
                .priceLaborVnd(new BigDecimal("150000"))
                .priceMaterialsVnd(new BigDecimal("200000"))
                .status(QuotationStatus.PENDING)
                .build();
        pendingQuotation.setCreatedAt(LocalDateTime.now());

        createCmd = QuotationUseCase.CreateQuotationCommand.builder()
                .solution("Thay tụ quạt điều hòa")
                .priceLaborVnd(new BigDecimal("150000"))
                .priceMaterialsVnd(new BigDecimal("200000"))
                .inspectionTime(LocalDateTime.now().plusDays(1))
                .estimatedFinish(LocalDateTime.now().plusDays(2))
                .note("Bảo hành 6 tháng")
                .build();

        updateCmd = QuotationUseCase.UpdateQuotationCommand.builder()
                .solution("Thay tụ và vệ sinh quạt")
                .priceLaborVnd(new BigDecimal("180000"))
                .priceMaterialsVnd(new BigDecimal("220000"))
                .inspectionTime(LocalDateTime.now().plusDays(1))
                .estimatedFinish(LocalDateTime.now().plusDays(2))
                .note("Bảo hành 12 tháng")
                .build();
    }

    // ── RC-40: Gửi báo giá ──

    @Test
    @DisplayName("RC-40: Kỹ thuật viên gửi báo giá thành công khi yêu cầu đang BIDDING_OPEN")
    void create_success() {
        openRequest.setCategoryId(1L);
        TechnicianProfileJpaEntity tech = TechnicianProfileJpaEntity.builder()
                .userId(20L)
                .verificationStatus(com.fixlink.domain.model.VerificationStatus.APPROVED)
                .categoryIds(java.util.Set.of(1L))
                .areaIds(java.util.Set.of())
                .build();
        when(techProfileRepo.findById(20L)).thenReturn(Optional.of(tech));
        when(requestRepo.findById(100L)).thenReturn(Optional.of(openRequest));
        when(quotationRepo.existsByRequestIdAndTechnicianId(100L, 20L)).thenReturn(false);
        when(quotationRepo.save(any(QuotationJpaEntity.class))).thenAnswer(invocation -> {
            QuotationJpaEntity q = invocation.getArgument(0);
            q.setId(1L);
            return q;
        });

        QuotationResponse res = quotationService.create(100L, createCmd, 20L);

        assertThat(res).isNotNull();
        assertThat(res.getRequestId()).isEqualTo(100L);
        assertThat(res.getTechnicianId()).isEqualTo(20L);
        assertThat(res.getStatus()).isEqualTo(QuotationStatus.PENDING);
        assertThat(res.getTotalPrice()).isEqualByComparingTo(new BigDecimal("350000"));
        verify(quotationRepo).save(any(QuotationJpaEntity.class));
    }

    @Test
    @DisplayName("RC-40: Từ chối gửi báo giá khi KTV đã báo giá cho đơn này trước đó")
    void create_duplicateQuotation_throwsException() {
        openRequest.setCategoryId(1L);
        TechnicianProfileJpaEntity tech = TechnicianProfileJpaEntity.builder()
                .userId(20L)
                .verificationStatus(com.fixlink.domain.model.VerificationStatus.APPROVED)
                .categoryIds(java.util.Set.of(1L))
                .areaIds(java.util.Set.of())
                .build();
        when(techProfileRepo.findById(20L)).thenReturn(Optional.of(tech));
        when(requestRepo.findById(100L)).thenReturn(Optional.of(openRequest));
        when(quotationRepo.existsByRequestIdAndTechnicianId(100L, 20L)).thenReturn(true);

        assertThatThrownBy(() -> quotationService.create(100L, createCmd, 20L))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Bạn đã gửi báo giá cho yêu cầu này rồi");
    }

    @Test
    @DisplayName("RC-40: Từ chối gửi báo giá khi yêu cầu không tồn tại")
    void create_requestNotFound_throwsException() {
        when(requestRepo.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quotationService.create(999L, createCmd, 20L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("RC-40: Từ chối gửi báo giá khi đơn không ở trạng thái BIDDING_OPEN")
    void create_requestNotBiddingOpen_throwsException() {
        openRequest.setStatus(RequestStatus.IN_PROGRESS);
        when(requestRepo.findById(100L)).thenReturn(Optional.of(openRequest));

        assertThatThrownBy(() -> quotationService.create(100L, createCmd, 20L))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Yêu cầu không ở trạng thái nhận báo giá");
    }

    // ── RC-41: Sửa & Rút báo giá ──

    @Test
    @DisplayName("RC-41: Kỹ thuật viên cập nhật báo giá thành công khi đang PENDING")
    void update_success() {
        when(quotationRepo.findById(1L)).thenReturn(Optional.of(pendingQuotation));
        when(requestRepo.findById(100L)).thenReturn(Optional.of(openRequest));
        when(quotationRepo.save(any(QuotationJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuotationResponse res = quotationService.update(100L, 1L, updateCmd, 20L);

        assertThat(res).isNotNull();
        assertThat(res.getSolution()).isEqualTo("Thay tụ và vệ sinh quạt");
        assertThat(res.getTotalPrice()).isEqualByComparingTo(new BigDecimal("400000"));
        verify(quotationRepo).save(pendingQuotation);
    }

    @Test
    @DisplayName("RC-41 (Anti-IDOR): Không được sửa báo giá của kỹ thuật viên khác")
    void update_notOwner_throwsForbidden() {
        when(quotationRepo.findById(1L)).thenReturn(Optional.of(pendingQuotation));

        assertThatThrownBy(() -> quotationService.update(100L, 1L, updateCmd, 999L))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Bạn không phải chủ báo giá này");
    }

    @Test
    @DisplayName("RC-41: Không được sửa báo giá khi trạng thái không phải PENDING")
    void update_notPending_throwsException() {
        pendingQuotation.setStatus(QuotationStatus.ACCEPTED);
        when(quotationRepo.findById(1L)).thenReturn(Optional.of(pendingQuotation));

        assertThatThrownBy(() -> quotationService.update(100L, 1L, updateCmd, 20L))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Chỉ sửa được báo giá đang chờ duyệt");
    }

    @Test
    @DisplayName("RC-41: Kỹ thuật viên rút lại báo giá thành công khi đang PENDING")
    void withdraw_success() {
        when(quotationRepo.findById(1L)).thenReturn(Optional.of(pendingQuotation));

        quotationService.withdraw(100L, 1L, 20L);

        assertThat(pendingQuotation.getStatus()).isEqualTo(QuotationStatus.WITHDRAWN);
        verify(quotationRepo).save(pendingQuotation);
    }

    @Test
    @DisplayName("RC-41 (Anti-IDOR): Không được rút báo giá của kỹ thuật viên khác")
    void withdraw_notOwner_throwsForbidden() {
        when(quotationRepo.findById(1L)).thenReturn(Optional.of(pendingQuotation));

        assertThatThrownBy(() -> quotationService.withdraw(100L, 1L, 999L))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Bạn không phải chủ báo giá này");
    }

    @Test
    @DisplayName("RC-41: Không được rút báo giá khi đã được chấp nhận (ACCEPTED)")
    void withdraw_alreadyAccepted_throwsException() {
        pendingQuotation.setStatus(QuotationStatus.ACCEPTED);
        when(quotationRepo.findById(1L)).thenReturn(Optional.of(pendingQuotation));

        assertThatThrownBy(() -> quotationService.withdraw(100L, 1L, 20L))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Chỉ rút được báo giá đang chờ duyệt");
    }

    // ── RC-42: KTV xem danh sách báo giá ──

    @Test
    @DisplayName("RC-42: KTV xem toàn bộ danh sách báo giá đã gửi")
    void getMyQuotations_all() {
        when(quotationRepo.findByTechnicianIdOrderByCreatedAtDesc(20L)).thenReturn(List.of(pendingQuotation));

        List<QuotationResponse> list = quotationService.getMyQuotations(20L, null);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo(1L);
        assertThat(list.get(0).getTechnicianId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("RC-42: KTV lọc danh sách báo giá theo trạng thái PENDING")
    void getMyQuotations_filterByStatus() {
        when(quotationRepo.findByTechnicianIdAndStatusOrderByCreatedAtDesc(20L, QuotationStatus.PENDING))
                .thenReturn(List.of(pendingQuotation));

        List<QuotationResponse> list = quotationService.getMyQuotations(20L, QuotationStatus.PENDING);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getStatus()).isEqualTo(QuotationStatus.PENDING);
    }

    @Test
    @DisplayName("RC-42: KTV xem chi tiết báo giá đơn lẻ của mình")
    void getQuotationByIdForTechnician_success() {
        when(quotationRepo.findById(1L)).thenReturn(Optional.of(pendingQuotation));

        QuotationResponse res = quotationService.getQuotationByIdForTechnician(1L, 20L);

        assertThat(res).isNotNull();
        assertThat(res.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("RC-42 (Anti-IDOR): KTV không được xem chi tiết báo giá của KTV khác")
    void getQuotationByIdForTechnician_notOwner_throwsForbidden() {
        when(quotationRepo.findById(1L)).thenReturn(Optional.of(pendingQuotation));

        assertThatThrownBy(() -> quotationService.getQuotationByIdForTechnician(1L, 999L))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Bạn không phải chủ báo giá này");
    }
}
