package com.fixlink.domain;

import com.fixlink.domain.model.TechnicianProfile;
import com.fixlink.domain.model.VerificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TechnicianProfileTest {

    @Test
    @DisplayName("Khởi tạo hồ sơ thợ phải có trạng thái PENDING")
    void testInitialStatusIsPending() {
        TechnicianProfile profile = new TechnicianProfile();
        assertEquals(VerificationStatus.PENDING, profile.getVerificationStatus());
        assertNull(profile.getVerifiedBy());
    }

    @Test
    @DisplayName("Admin duyệt hồ sơ thì trạng thái chuyển sang APPROVED")
    void testAdminApproveVerification() {
        TechnicianProfile profile = new TechnicianProfile();
        profile.verify(1L);

        assertEquals(VerificationStatus.APPROVED, profile.getVerificationStatus());
        assertEquals(1L, profile.getVerifiedBy());
        assertNotNull(profile.getVerifiedAt());
        assertNull(profile.getRejectionReason());
    }

    @Test
    @DisplayName("Admin từ chối hồ sơ thì trạng thái chuyển sang REJECTED kèm lý do")
    void testAdminRejectVerification() {
        TechnicianProfile profile = new TechnicianProfile();
        profile.reject(1L, "Ảnh CCCD mờ");

        assertEquals(VerificationStatus.REJECTED, profile.getVerificationStatus());
        assertEquals(1L, profile.getVerifiedBy());
        assertEquals("Ảnh CCCD mờ", profile.getRejectionReason());
    }
}
