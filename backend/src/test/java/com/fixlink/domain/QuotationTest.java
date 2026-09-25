package com.fixlink.domain;

import com.fixlink.domain.exception.DomainException;
import com.fixlink.domain.model.Quotation;
import com.fixlink.domain.model.QuotationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Tests - Thực thể miền Quotation")
class QuotationTest {

    @Test
    @DisplayName("Tính tổng giá trị báo giá chính xác (Công + Vật tư)")
    void testCalculateTotalPrice() {
        Quotation quotation = Quotation.builder()
                .repairRequestId(1L)
                .technicianId(10L)
                .solution("Thay tụ quạt điều hòa")
                .priceLabor(new BigDecimal("150000"))
                .priceMaterials(new BigDecimal("200000"))
                .build();

        BigDecimal total = quotation.calculateTotalPrice();

        assertEquals(new BigDecimal("350000"), total);
        assertEquals(new BigDecimal("350000"), quotation.getTotalPrice());
    }

    @Test
    @DisplayName("Xác thực thành công với báo giá đầy đủ và hợp lệ")
    void testValidateSuccess() {
        Quotation quotation = Quotation.builder()
                .repairRequestId(1L)
                .technicianId(10L)
                .solution("Sửa vòi nước rò rỉ")
                .priceLabor(new BigDecimal("100000"))
                .priceMaterials(new BigDecimal("50000"))
                .build();

        assertDoesNotThrow(quotation::validate);
        assertEquals(new BigDecimal("150000"), quotation.getTotalPrice());
    }

    @Test
    @DisplayName("Từ chối báo giá khi phương án sửa chữa rỗng")
    void testValidateBlankSolutionThrowsException() {
        Quotation quotation = Quotation.builder()
                .repairRequestId(1L)
                .technicianId(10L)
                .solution("   ")
                .priceLabor(new BigDecimal("100000"))
                .priceMaterials(BigDecimal.ZERO)
                .build();

        DomainException ex = assertThrows(DomainException.class, quotation::validate);
        assertEquals("Phương án sửa chữa không được để trống", ex.getMessage());
    }

    @Test
    @DisplayName("Từ chối báo giá khi chi phí nhân công âm")
    void testValidateNegativeLaborPriceThrowsException() {
        Quotation quotation = Quotation.builder()
                .repairRequestId(1L)
                .technicianId(10L)
                .solution("Vệ sinh máy lạnh")
                .priceLabor(new BigDecimal("-10000"))
                .priceMaterials(BigDecimal.ZERO)
                .build();

        DomainException ex = assertThrows(DomainException.class, quotation::validate);
        assertEquals("Chi phí nhân công không được âm", ex.getMessage());
    }

    @Test
    @DisplayName("Từ chối báo giá khi chi phí vật tư âm")
    void testValidateNegativeMaterialsPriceThrowsException() {
        Quotation quotation = Quotation.builder()
                .repairRequestId(1L)
                .technicianId(10L)
                .solution("Vệ sinh máy lạnh")
                .priceLabor(new BigDecimal("100000"))
                .priceMaterials(new BigDecimal("-50000"))
                .build();

        DomainException ex = assertThrows(DomainException.class, quotation::validate);
        assertEquals("Chi phí vật tư không được âm", ex.getMessage());
    }

    @Test
    @DisplayName("Từ chối báo giá khi tổng tiền bằng 0")
    void testValidateZeroTotalPriceThrowsException() {
        Quotation quotation = Quotation.builder()
                .repairRequestId(1L)
                .technicianId(10L)
                .solution("Khảo sát không tính phí")
                .priceLabor(BigDecimal.ZERO)
                .priceMaterials(BigDecimal.ZERO)
                .build();

        DomainException ex = assertThrows(DomainException.class, quotation::validate);
        assertEquals("Tổng giá trị báo giá phải lớn hơn 0", ex.getMessage());
    }

    @Test
    @DisplayName("Rút lại báo giá thành công khi trạng thái đang PENDING")
    void testWithdrawSuccessWhenPending() {
        Quotation quotation = Quotation.builder()
                .status(QuotationStatus.PENDING)
                .build();

        quotation.withdraw();

        assertEquals(QuotationStatus.WITHDRAWN, quotation.getStatus());
        assertNotNull(quotation.getUpdatedAt());
    }

    @Test
    @DisplayName("Không được rút lại báo giá khi đã được chấp nhận")
    void testWithdrawFailWhenAccepted() {
        Quotation quotation = Quotation.builder()
                .status(QuotationStatus.ACCEPTED)
                .build();

        DomainException ex = assertThrows(DomainException.class, quotation::withdraw);
        assertEquals("Chỉ có thể rút lại báo giá khi đang chờ phản hồi (PENDING)", ex.getMessage());
    }

    @Test
    @DisplayName("Chấp nhận và từ chối báo giá cập nhật trạng thái chính xác")
    void testAcceptAndRejectTransitions() {
        Quotation q1 = Quotation.builder().status(QuotationStatus.PENDING).build();
        q1.accept();
        assertEquals(QuotationStatus.ACCEPTED, q1.getStatus());

        Quotation q2 = Quotation.builder().status(QuotationStatus.PENDING).build();
        q2.reject();
        assertEquals(QuotationStatus.REJECTED, q2.getStatus());
    }
}
