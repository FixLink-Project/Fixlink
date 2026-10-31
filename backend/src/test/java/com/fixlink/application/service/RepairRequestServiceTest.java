package com.fixlink.application.service;

import com.fixlink.adapter.in.web.dto.response.RepairRequestDetailResponse;
import com.fixlink.adapter.in.web.dto.response.RepairRequestResponse;
import com.fixlink.adapter.out.persistence.entity.*;
import com.fixlink.adapter.out.persistence.mapper.RepairRequestMapper;
import com.fixlink.adapter.out.persistence.repository.*;
import com.fixlink.application.port.in.RepairRequestUseCase;
import com.fixlink.domain.exception.DomainException;
import com.fixlink.domain.exception.ResourceNotFoundException;
import com.fixlink.domain.model.QuotationStatus;
import com.fixlink.domain.model.RepairRequest;
import com.fixlink.domain.model.RequestStatus;
import com.fixlink.domain.model.Role;
import com.fixlink.domain.model.UserStatus;
import com.fixlink.domain.model.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepairRequestServiceTest {

    @Mock
    private SpringDataRepairRequestRepository requestRepo;

    @Mock
    private SpringDataRepairRequestReviewRepository reviewRepo;

    @Mock
    private SpringDataMediaRepository mediaRepo;

    @Mock
    private SpringDataWorkProgressRepository progressRepo;

    @Mock
    private SpringDataQuotationRepository quotationRepo;

    @Mock
    private SpringDataServiceCategoryRepository categoryRepo;

    @Mock
    private SpringDataServiceAreaRepository areaRepo;

    @Mock
    private SpringDataTechnicianProfileRepository techProfileRepo;

    @Mock
    private SpringDataUserRepository userRepo;

    // Hai phu thuoc do Sprint 2 (main) them vao RepairRequestService;
    // test cua PR #1 viet truoc do nen chua mock.
    @Mock
    private SpringDataCustomerProfileRepository customerProfileRepo;

    @Mock
    private SpringDataPaymentRepository paymentRepo;

    @Mock
    private PaymentService paymentService;

    @Mock
    private RepairRequestMapper repairRequestMapper;

    @Mock
    private com.fixlink.application.port.in.NotificationUseCase notificationUseCase;

    @InjectMocks
    private RepairRequestService service;

    private UserJpaEntity activeCustomer;
    private ServiceCategoryJpaEntity activeCategory;
    private ServiceAreaJpaEntity activeArea;
    private RepairRequestJpaEntity sampleRequest;

    @BeforeEach
    void setUp() {
        activeCustomer = UserJpaEntity.builder()
                .id(1L)
                .username("customer01")
                .status(UserStatus.ACTIVE)
                .build();

        activeCategory = ServiceCategoryJpaEntity.builder()
                .id(10L)
                .name("Điện lạnh")
                .isActive(true)
                .build();

        activeArea = ServiceAreaJpaEntity.builder()
                .id(20L)
                .name("Quận 1")
                .isActive(true)
                .build();

        sampleRequest = RepairRequestJpaEntity.builder()
                .id(100L)
                .requestCode("REQ-20260923-ABCD")
                .customerId(1L)
                .categoryId(10L)
                .areaId(20L)
                .title("Sửa máy giặt LG")
                .description("Máy giặt kêu to khi vắt")
                .address("123 Lê Lợi, Quận 1")
                .status(RequestStatus.OPEN)
                .requestedTime(LocalDateTime.now().plusDays(1))
                .budgetRef(new BigDecimal("500000"))
                .build();
    }

    // =========================================================================
    // RC-32: Create Repair Request
    // =========================================================================

    @Test
    @DisplayName("RC-32: Khách hàng tạo yêu cầu sửa chữa thành công")
    void testCreate_Success() {
        var cmd = RepairRequestUseCase.CreateCommand.builder()
                .title("Sửa điều hòa Panasonic")
                .description("Điều hòa không mát, có mùi ẩm mốc")
                .categoryId(10L)
                .areaId(20L)
                .addressLine("456 Hai Bà Trưng")
                .budgetRef(new BigDecimal("600000"))
                                .mediaUrls(List.of("https://s3.example.com/img1.jpg"))
                .build();

        when(userRepo.findById(1L)).thenReturn(Optional.of(activeCustomer));
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(activeCategory));
        when(areaRepo.findById(20L)).thenReturn(Optional.of(activeArea));
        when(requestRepo.save(any(RepairRequestJpaEntity.class))).thenAnswer(invocation -> {
            RepairRequestJpaEntity saved = invocation.getArgument(0);
            saved.setId(101L);
            return saved;
        });

        RepairRequestResponse response = service.create(cmd, 1L);

        assertNotNull(response);
        assertEquals("Sửa điều hòa Panasonic", response.getTitle());
        assertEquals(RequestStatus.OPEN.name(), response.getStatus());
        assertTrue(response.getRequestCode().startsWith("REQ-"));
        verify(mediaRepo, times(1)).saveAll(anyList());
        verify(progressRepo, times(1)).save(any(WorkProgressJpaEntity.class));
    }

    @Test
    @DisplayName("RC-32: Báo lỗi khi tài khoản khách hàng không tồn tại")
    void testCreate_CustomerNotFound_ThrowsException() {
        var cmd = RepairRequestUseCase.CreateCommand.builder()
                .title("Sửa máy nước nóng")
                .description("Nước không nóng")
                .categoryId(10L)
                .addressLine("123 Test")
                .build();

        when(userRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(cmd, 999L));
    }

    @Test
    @DisplayName("RC-32: Báo lỗi khi tài khoản khách hàng bị khóa (BLOCKED)")
    void testCreate_CustomerInactive_ThrowsException() {
        UserJpaEntity blockedUser = UserJpaEntity.builder()
                .id(2L)
                .status(UserStatus.BLOCKED)
                .build();

        var cmd = RepairRequestUseCase.CreateCommand.builder()
                .title("Sửa máy giặt")
                .description("Hỏng board mạch")
                .categoryId(10L)
                .addressLine("123 Test")
                .build();

        when(userRepo.findById(2L)).thenReturn(Optional.of(blockedUser));

        DomainException ex = assertThrows(DomainException.class, () -> service.create(cmd, 2L));
        assertEquals("ACCOUNT_INACTIVE", ex.getErrorCode());
    }

    @Test
    @DisplayName("RC-32: Báo lỗi khi danh mục dịch vụ không tồn tại")
    void testCreate_CategoryNotFound_ThrowsException() {
        var cmd = RepairRequestUseCase.CreateCommand.builder()
                .title("Sửa máy lọc nước")
                .description("Nước chảy yếu")
                .categoryId(999L)
                .addressLine("123 Test")
                .build();

        when(userRepo.findById(1L)).thenReturn(Optional.of(activeCustomer));
        when(categoryRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(cmd, 1L));
    }

    @Test
    @DisplayName("RC-32: Báo lỗi khi danh mục dịch vụ đang tạm ngưng hoạt động")
    void testCreate_CategoryInactive_ThrowsException() {
        ServiceCategoryJpaEntity inactiveCategory = ServiceCategoryJpaEntity.builder()
                .id(15L)
                .name("Danh mục ngưng")
                .isActive(false)
                .build();

        var cmd = RepairRequestUseCase.CreateCommand.builder()
                .title("Sửa quạt trần")
                .description("Quạt rung lắc mạnh")
                .categoryId(15L)
                .addressLine("123 Test")
                .build();

        when(userRepo.findById(1L)).thenReturn(Optional.of(activeCustomer));
        when(categoryRepo.findById(15L)).thenReturn(Optional.of(inactiveCategory));

        DomainException ex = assertThrows(DomainException.class, () -> service.create(cmd, 1L));
        assertEquals("INVALID_OPERATION", ex.getErrorCode());
    }

    @Test
    @DisplayName("RC-32: Báo lỗi khi khu vực dịch vụ đang tạm ngưng hoạt động")
    void testCreate_AreaInactive_ThrowsException() {
        ServiceAreaJpaEntity inactiveArea = ServiceAreaJpaEntity.builder()
                .id(25L)
                .name("Khu vực bảo trì")
                .isActive(false)
                .build();

        var cmd = RepairRequestUseCase.CreateCommand.builder()
                .title("Sửa máy bơm nước")
                .description("Máy bơm không lên nước")
                .categoryId(10L)
                .areaId(25L)
                .addressLine("123 Test")
                .build();

        when(userRepo.findById(1L)).thenReturn(Optional.of(activeCustomer));
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(activeCategory));
        when(areaRepo.findById(25L)).thenReturn(Optional.of(inactiveArea));

        DomainException ex = assertThrows(DomainException.class, () -> service.create(cmd, 1L));
        assertEquals("INVALID_OPERATION", ex.getErrorCode());
    }

    // =========================================================================
    // RC-33: Update Repair Request
    // =========================================================================

    @Test
    @DisplayName("RC-33: Khách hàng cập nhật yêu cầu thành công khi chưa có báo giá")
    void testUpdate_Success() {
        var cmd = RepairRequestUseCase.UpdateCommand.builder()
                .title("Tiêu đề đã sửa")
                .description("Mô tả chi tiết bổ sung")
                .categoryId(10L)
                .areaId(20L)
                .addressLine("456 Lê Lợi, Quận 1")
                .budgetRef(new BigDecimal("700000"))
                .build();

        when(requestRepo.findById(100L)).thenReturn(Optional.of(sampleRequest));
        when(quotationRepo.countByRequestId(100L)).thenReturn(0L);
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(activeCategory));
        when(areaRepo.findById(20L)).thenReturn(Optional.of(activeArea));
        when(requestRepo.save(any(RepairRequestJpaEntity.class))).thenAnswer(i -> i.getArgument(0));

        RepairRequestResponse response = service.update(100L, cmd, 1L);

        assertNotNull(response);
        assertEquals("Tiêu đề đã sửa", response.getTitle());
        assertEquals("Mô tả chi tiết bổ sung", response.getDescription());
        verify(progressRepo, times(1)).save(any(WorkProgressJpaEntity.class));
    }

    @Test
    @DisplayName("RC-33: Từ chối cập nhật khi người dùng không phải chủ sở hữu yêu cầu")
    void testUpdate_NotOwner_ThrowsAccessDenied() {
        var cmd = RepairRequestUseCase.UpdateCommand.builder()
                .title("Hack title")
                .description("Hack description")
                .categoryId(10L)
                .addressLine("Hack address")
                .build();

        when(requestRepo.findById(100L)).thenReturn(Optional.of(sampleRequest));

        DomainException ex = assertThrows(DomainException.class, () -> service.update(100L, cmd, 999L));
        assertEquals("ACCESS_DENIED", ex.getErrorCode());
    }

    @Test
    @DisplayName("RC-33: Từ chối cập nhật khi đơn đã có ít nhất một thợ gửi báo giá")
    void testUpdate_WhenQuotationsExist_ThrowsException() {
        var cmd = RepairRequestUseCase.UpdateCommand.builder()
                .title("Sửa tiêu đề")
                .description("Mô tả")
                .categoryId(10L)
                .addressLine("123 Test")
                .build();

        when(requestRepo.findById(100L)).thenReturn(Optional.of(sampleRequest));
        when(quotationRepo.countByRequestId(100L)).thenReturn(2L); // Đã có 2 báo giá

        DomainException ex = assertThrows(DomainException.class, () -> service.update(100L, cmd, 1L));
        assertEquals("INVALID_OPERATION", ex.getErrorCode());
    }

    @Test
    @DisplayName("RC-33: Từ chối cập nhật khi đơn đã ở trạng thái IN_PROGRESS hoặc COMPLETED")
    void testUpdate_InvalidStatus_ThrowsException() {
        sampleRequest.setStatus(RequestStatus.IN_PROGRESS);

        var cmd = RepairRequestUseCase.UpdateCommand.builder()
                .title("Sửa tiêu đề")
                .description("Mô tả")
                .categoryId(10L)
                .addressLine("123 Test")
                .build();

        when(requestRepo.findById(100L)).thenReturn(Optional.of(sampleRequest));

        DomainException ex = assertThrows(DomainException.class, () -> service.update(100L, cmd, 1L));
        assertEquals("INVALID_OPERATION", ex.getErrorCode());
    }

    // =========================================================================
    // RC-34: Cancel Repair Request
    // =========================================================================

    @Test
    @DisplayName("RC-34: Khách hàng hủy yêu cầu thành công và tự động REJECT các báo giá PENDING")
    void testCancel_Success() {
        QuotationJpaEntity pendingQuote = QuotationJpaEntity.builder()
                .id(201L)
                .requestId(100L)
                .technicianId(5L)
                .status(QuotationStatus.PENDING)
                .build();

        when(requestRepo.findById(100L)).thenReturn(Optional.of(sampleRequest));
        when(requestRepo.save(any(RepairRequestJpaEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(quotationRepo.findByRequestIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(pendingQuote));

        RepairRequestResponse response = service.cancel(100L, "Tôi tìm được thợ gần nhà", 1L);

        assertNotNull(response);
        assertEquals(RequestStatus.CANCELLED.name(), response.getStatus());
        assertEquals("Tôi tìm được thợ gần nhà", response.getCancelReason());
        assertEquals(QuotationStatus.REJECTED, pendingQuote.getStatus());
        verify(quotationRepo, times(1)).save(pendingQuote);
        verify(progressRepo, times(1)).save(any(WorkProgressJpaEntity.class));
    }

    @Test
    @DisplayName("RC-34: Không thể hủy đơn khi người thao tác không phải chủ sở hữu")
    void testCancel_NotOwner_ThrowsAccessDenied() {
        when(requestRepo.findById(100L)).thenReturn(Optional.of(sampleRequest));

        DomainException ex = assertThrows(DomainException.class, () -> service.cancel(100L, "Lý do", 999L));
        assertEquals("ACCESS_DENIED", ex.getErrorCode());
    }

    @Test
    @DisplayName("RC-34: Không thể hủy đơn khi đơn đã hoàn thành hoặc đã hủy trước đó")
    void testCancel_AlreadyCompletedOrCancelled_ThrowsException() {
        sampleRequest.setStatus(RequestStatus.COMPLETED);
        when(requestRepo.findById(100L)).thenReturn(Optional.of(sampleRequest));

        DomainException ex = assertThrows(DomainException.class, () -> service.cancel(100L, "Muốn hủy", 1L));
        assertEquals("INVALID_OPERATION", ex.getErrorCode());
    }

    // =========================================================================
    // RC-37 & Details: Discovery and Matching
    // =========================================================================

    @Test
    @DisplayName("RC-37: Thợ chưa KYC không được xem danh sách yêu cầu phù hợp (403)")
    void testGetMatchingForTechnician_UnverifiedTech_ThrowsException() {
        TechnicianProfileJpaEntity unverifiedTech = TechnicianProfileJpaEntity.builder()
                .userId(50L)
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        when(techProfileRepo.findById(50L)).thenReturn(Optional.of(unverifiedTech));

        DomainException ex = assertThrows(DomainException.class,
                () -> service.getMatchingForTechnician(50L, 1, 10, null, "createdAt", "DESC"));
        assertEquals("UNVERIFIED_TECHNICIAN", ex.getErrorCode());
    }

    @Test
    @DisplayName("RC-37: Thợ đã duyệt nhưng chưa cấu hình danh mục/khu vực trả về trang rỗng")
    void testGetMatchingForTechnician_EmptyConfig_ReturnsEmptyPage() {
        TechnicianProfileJpaEntity emptyConfigTech = TechnicianProfileJpaEntity.builder()
                .userId(50L)
                .verificationStatus(VerificationStatus.APPROVED)
                .categoryIds(Collections.emptySet())
                .areaIds(Collections.emptySet())
                .build();

        when(techProfileRepo.findById(50L)).thenReturn(Optional.of(emptyConfigTech));

        Page<RepairRequestResponse> result = service.getMatchingForTechnician(50L, 1, 10, null, "createdAt", "DESC");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Xem chi tiết yêu cầu sửa chữa kèm theo báo giá và tiến độ")
    void testGetDetail_Success_AsOwner() {
        when(requestRepo.findById(100L)).thenReturn(Optional.of(sampleRequest));
        when(categoryRepo.findById(10L)).thenReturn(Optional.of(activeCategory));
        when(areaRepo.findById(20L)).thenReturn(Optional.of(activeArea));
        when(quotationRepo.findByRequestIdOrderByCreatedAtDesc(100L)).thenReturn(Collections.emptyList());
        when(progressRepo.findByRequestIdOrderByCreatedAtAsc(100L)).thenReturn(Collections.emptyList());

        RepairRequestDetailResponse detail = service.getDetail(100L, 1L, "CUSTOMER");

        assertNotNull(detail);
        assertEquals(100L, detail.getRequest().getId());
        assertEquals("Sửa máy giặt LG", detail.getRequest().getTitle());
    }

    @Test
    @DisplayName("Thợ có thể chuyển trạng thái từ DEPOSIT_PAID sang INSPECTING")
    void testUpdateStatus_TechnicianFromDepositPaidToInspecting() {
        sampleRequest.setStatus(RequestStatus.DEPOSIT_PAID);
        sampleRequest.setTechnicianId(2L);
        when(requestRepo.findById(100L)).thenReturn(Optional.of(sampleRequest));
        when(requestRepo.save(any(RepairRequestJpaEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(repairRequestMapper.toDomain(any(RepairRequestJpaEntity.class))).thenReturn(
                RepairRequest.builder().id(100L).status(RequestStatus.INSPECTING).build()
        );

        var cmd = new RepairRequestUseCase.UpdateStatusCommand(
                100L, RequestStatus.INSPECTING, "Bắt đầu khảo sát", 2L, Role.TECHNICIAN
        );

        var result = service.updateStatus(cmd);

        assertEquals(RequestStatus.INSPECTING, result.request().getStatus());
        verify(progressRepo, times(1)).save(any(WorkProgressJpaEntity.class));
    }

    @Test
    @DisplayName("Nghiệm thu đơn thanh toán TIỀN MẶT: Cọc 600k, giá 2tr -> cộng 400k vào ví thợ")
    void testCompleteStatus_SettlementWithCash_WithDeposit() {
        sampleRequest.setStatus(RequestStatus.AWAITING_ACCEPTANCE);
        sampleRequest.setTechnicianId(2L);
        sampleRequest.setAgreedPrice(new BigDecimal("2000000"));
        sampleRequest.setDepositAmount(new BigDecimal("600000"));

        TechnicianProfileJpaEntity tech = new TechnicianProfileJpaEntity();
        tech.setUserId(2L);
        tech.setWalletBalance(new BigDecimal("1000000"));
        tech.setCompletedJobs(5);

        PaymentJpaEntity cashFinalPayment = PaymentJpaEntity.builder()
                .paymentMethod("CASH")
                .status(com.fixlink.domain.model.PaymentStatus.CONFIRMED)
                .amount(new BigDecimal("1400000"))
                .build();

        when(requestRepo.findById(100L)).thenReturn(Optional.of(sampleRequest));
        when(requestRepo.save(any(RepairRequestJpaEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(techProfileRepo.findById(2L)).thenReturn(Optional.of(tech));
        when(paymentRepo.findByRequestIdAndPaymentTypeAndDeletedAtIsNull(eq(100L), eq(com.fixlink.domain.model.PaymentType.FINAL)))
                .thenReturn(Optional.of(cashFinalPayment));
        when(reviewRepo.existsByRequestIdAndDeletedAtIsNull(100L)).thenReturn(true);
        when(repairRequestMapper.toDomain(any(RepairRequestJpaEntity.class))).thenReturn(
                RepairRequest.builder().id(100L).status(RequestStatus.COMPLETED).build()
        );

        var cmd = new RepairRequestUseCase.UpdateStatusCommand(
                100L, RequestStatus.COMPLETED, "Khách đã nghiệm thu", 1L, Role.CUSTOMER
        );

        var result = service.updateStatus(cmd);

        assertEquals(RequestStatus.COMPLETED, result.request().getStatus());
        // 1.000.000 - 200.000 = 800.000 (khấu trừ 10% hoa hồng sàn khi khách trả tiền mặt)
        assertEquals(new BigDecimal("800000"), tech.getWalletBalance());
        assertEquals(6, tech.getCompletedJobs());
        verify(techProfileRepo).save(tech);
    }

    @Test
    @DisplayName("Nghiệm thu đơn thanh toán TIỀN MẶT: Không cọc, giá 2tr -> trừ 200k hoa hồng trong ví thợ")
    void testCompleteStatus_SettlementWithCash_NoDeposit() {
        sampleRequest.setStatus(RequestStatus.AWAITING_ACCEPTANCE);
        sampleRequest.setTechnicianId(2L);
        sampleRequest.setAgreedPrice(new BigDecimal("2000000"));
        sampleRequest.setDepositAmount(BigDecimal.ZERO);

        TechnicianProfileJpaEntity tech = new TechnicianProfileJpaEntity();
        tech.setUserId(2L);
        tech.setWalletBalance(new BigDecimal("1000000"));
        tech.setCompletedJobs(2);

        PaymentJpaEntity cashFinalPayment = PaymentJpaEntity.builder()
                .paymentMethod("CASH")
                .status(com.fixlink.domain.model.PaymentStatus.CONFIRMED)
                .amount(new BigDecimal("2000000"))
                .build();

        when(requestRepo.findById(100L)).thenReturn(Optional.of(sampleRequest));
        when(requestRepo.save(any(RepairRequestJpaEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(techProfileRepo.findById(2L)).thenReturn(Optional.of(tech));
        when(paymentRepo.findByRequestIdAndPaymentTypeAndDeletedAtIsNull(eq(100L), eq(com.fixlink.domain.model.PaymentType.FINAL)))
                .thenReturn(Optional.of(cashFinalPayment));
        when(reviewRepo.existsByRequestIdAndDeletedAtIsNull(100L)).thenReturn(true);
        when(repairRequestMapper.toDomain(any(RepairRequestJpaEntity.class))).thenReturn(
                RepairRequest.builder().id(100L).status(RequestStatus.COMPLETED).build()
        );

        var cmd = new RepairRequestUseCase.UpdateStatusCommand(
                100L, RequestStatus.COMPLETED, "Khách đã nghiệm thu", 1L, Role.CUSTOMER
        );

        var result = service.updateStatus(cmd);

        assertEquals(RequestStatus.COMPLETED, result.request().getStatus());
        // 1.000.000 + (0 - 200.000) = 800.000
        assertEquals(new BigDecimal("800000"), tech.getWalletBalance());
        assertEquals(3, tech.getCompletedJobs());
        verify(techProfileRepo).save(tech);
    }

    @Test
    @DisplayName("Nghiệm thu đơn thanh toán VIETQR: Đơn 2tr -> giải ngân 1.800.000đ vào ví thợ")
    void testCompleteStatus_SettlementWithVietQR() {
        sampleRequest.setStatus(RequestStatus.AWAITING_ACCEPTANCE);
        sampleRequest.setTechnicianId(2L);
        sampleRequest.setAgreedPrice(new BigDecimal("2000000"));
        sampleRequest.setDepositAmount(new BigDecimal("600000"));

        TechnicianProfileJpaEntity tech = new TechnicianProfileJpaEntity();
        tech.setUserId(2L);
        tech.setWalletBalance(new BigDecimal("500000"));
        tech.setCompletedJobs(0);

        PaymentJpaEntity qrFinalPayment = PaymentJpaEntity.builder()
                .paymentMethod("VIETQR")
                .status(com.fixlink.domain.model.PaymentStatus.CONFIRMED)
                .amount(new BigDecimal("1400000"))
                .build();

        when(requestRepo.findById(100L)).thenReturn(Optional.of(sampleRequest));
        when(requestRepo.save(any(RepairRequestJpaEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(techProfileRepo.findById(2L)).thenReturn(Optional.of(tech));
        when(paymentRepo.findByRequestIdAndPaymentTypeAndDeletedAtIsNull(eq(100L), eq(com.fixlink.domain.model.PaymentType.FINAL)))
                .thenReturn(Optional.of(qrFinalPayment));
        when(reviewRepo.existsByRequestIdAndDeletedAtIsNull(100L)).thenReturn(true);
        when(repairRequestMapper.toDomain(any(RepairRequestJpaEntity.class))).thenReturn(
                RepairRequest.builder().id(100L).status(RequestStatus.COMPLETED).build()
        );

        var cmd = new RepairRequestUseCase.UpdateStatusCommand(
                100L, RequestStatus.COMPLETED, "Khách đã nghiệm thu", 1L, Role.CUSTOMER
        );

        var result = service.updateStatus(cmd);

        assertEquals(RequestStatus.COMPLETED, result.request().getStatus());
        // 500.000 + (2.000.000 - 200.000) = 2.300.000
        assertEquals(new BigDecimal("2300000"), tech.getWalletBalance());
        assertEquals(1, tech.getCompletedJobs());
        verify(techProfileRepo).save(tech);
    }

    @Test
    @DisplayName("Khi chuyển sang IN_PROGRESS: Tự động đóng các báo giá dự phòng (PENDING) thành REJECTED")
    void testUpdateStatus_ToInProgress_AutoClosesStandbyQuotations() {
        sampleRequest.setStatus(RequestStatus.INSPECTING);
        sampleRequest.setTechnicianId(2L);

        QuotationJpaEntity standbyQuote1 = QuotationJpaEntity.builder()
                .id(201L)
                .requestId(100L)
                .technicianId(3L)
                .status(QuotationStatus.PENDING)
                .build();
        QuotationJpaEntity standbyQuote2 = QuotationJpaEntity.builder()
                .id(202L)
                .requestId(100L)
                .technicianId(4L)
                .status(QuotationStatus.PENDING)
                .build();

        when(requestRepo.findById(100L)).thenReturn(Optional.of(sampleRequest));
        when(requestRepo.save(any(RepairRequestJpaEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(quotationRepo.findByRequestIdAndStatus(100L, QuotationStatus.PENDING))
                .thenReturn(List.of(standbyQuote1, standbyQuote2));
        when(repairRequestMapper.toDomain(any(RepairRequestJpaEntity.class))).thenReturn(
                RepairRequest.builder().id(100L).status(RequestStatus.IN_PROGRESS).build()
        );

        var cmd = new RepairRequestUseCase.UpdateStatusCommand(
                100L, RequestStatus.IN_PROGRESS, "Khách hàng xác nhận tiếp tục làm việc không phát sinh chi phí", 1L, Role.CUSTOMER
        );

        var result = service.updateStatus(cmd);

        assertEquals(RequestStatus.IN_PROGRESS, result.request().getStatus());
        assertEquals(QuotationStatus.REJECTED, standbyQuote1.getStatus());
        assertEquals(QuotationStatus.REJECTED, standbyQuote2.getStatus());
        verify(quotationRepo).save(standbyQuote1);
        verify(quotationRepo).save(standbyQuote2);
    }
}
