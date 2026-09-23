package com.fixlink.application.service;

import com.fixlink.adapter.in.web.dto.response.RepairRequestDetailResponse;
import com.fixlink.adapter.in.web.dto.response.RepairRequestResponse;
import com.fixlink.adapter.out.persistence.entity.*;
import com.fixlink.adapter.out.persistence.repository.*;
import com.fixlink.application.port.in.RepairRequestUseCase;
import com.fixlink.domain.exception.DomainException;
import com.fixlink.domain.exception.ResourceNotFoundException;
import com.fixlink.domain.model.QuotationStatus;
import com.fixlink.domain.model.RequestStatus;
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
                .status(RequestStatus.BIDDING_OPEN)
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
                .biddingDeadlineDays(5)
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
        assertEquals(RequestStatus.BIDDING_OPEN, response.getStatus());
        assertTrue(response.getRequestCode().startsWith("REQ-"));
        verify(mediaRepo, times(1)).save(any(MediaJpaEntity.class));
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
        assertEquals(RequestStatus.CANCELLED, response.getStatus());
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
}
