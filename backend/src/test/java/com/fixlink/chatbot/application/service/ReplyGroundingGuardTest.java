package com.fixlink.chatbot.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReplyGroundingGuardTest {

    private final ReplyGroundingGuard guard = new ReplyGroundingGuard();

    @Test
    @DisplayName("Bảng markdown do AI viết được đổi thành danh sách để không vỡ khung chat")
    void shouldTurnTablesIntoBullets() {
        String out = guard.enforce("""
                Các việc phù hợp:
                | Tiêu đề | Khu vực | Ngân sách |
                |---|---|---|
                | Sửa máy lạnh | Quận 1 | 450.000 đ |
                | Thông tắc ống | Quận 3 | 300.000 đ |
                Bạn muốn xem việc nào?""", "");
        assertThat(out).doesNotContain("|").contains("- Sửa máy lạnh – Quận 1 · 450.000 đ")
                .contains("- Thông tắc ống – Quận 3 · 300.000 đ").contains("Bạn muốn xem việc nào?")
                .doesNotContain("Tiêu đề");
    }

    @Test
    @DisplayName("Không lộ mã nội bộ: mã trạng thái/danh mục đổi sang tên tiếng Việt, mã đơn giữ nguyên")
    void shouldHumanizeInternalCodes() {
        String out = guard.enforce("Bạn có 3 yêu cầu đang sửa chữa (IN_PROGRESS). Đơn REQ-TEST-ASSIGNED ở trạng thái "
                + "`ASSIGNED`, danh mục DIEN_LANH. Mình đã gọi get_my_repair_requests.", "");
        assertThat(out).doesNotContain("IN_PROGRESS").doesNotContain("`ASSIGNED`").doesNotContain("DIEN_LANH")
                .doesNotContain("get_my_repair_requests")
                .contains("đang sửa chữa.").contains("REQ-TEST-ASSIGNED").contains("trạng thái Đã chọn thợ")
                .contains("Sửa Chữa Điện Lạnh");
    }

    @Test
    @DisplayName("Xóa hotline, email, website do model tự bịa")
    void shouldRemoveInventedContacts() {
        String reply = "Gọi hotline 1900 8899 hoặc 0901234567, email cskh@fixlink.vn, xem thêm tại www.fixlink.vn";
        String guarded = guard.enforce(reply, "Cho tôi số hotline");

        assertThat(guarded).doesNotContain("1900 8899").doesNotContain("0901234567")
                .doesNotContain("cskh@fixlink.vn").doesNotContain("www.fixlink.vn")
                .contains("chưa công bố số điện thoại");
    }

    @Test
    @DisplayName("Giữ nguyên SĐT do chính khách cung cấp và số khẩn cấp 114/115")
    void shouldKeepCustomerProvidedPhoneAndEmergencyNumbers() {
        String reply = "Mình đã ghi nhận SĐT 0912 345 678 của bạn. Khẩn cấp gọi 114 hoặc 115.";
        String guarded = guard.enforce(reply, "sdt của tôi là 0912345678");

        assertThat(guarded).isEqualTo(reply);
    }

    @Test
    @DisplayName("Chỉ giữ tỷ lệ có thật (cọc 30%, tất toán 70%), xóa tỷ lệ tự bịa")
    void shouldKeepOnlyKnownPercentages() {
        String guarded = guard.enforce("Cọc 30%, còn lại 70%. Phí nền tảng 15% và hoàn cọc 50%.", "phí bao nhiêu");

        assertThat(guarded).contains("30%").contains("70%").doesNotContain("15%").doesNotContain("50%");
    }

    @Test
    @DisplayName("Không đụng tới số tiền, mã đơn và link nội bộ")
    void shouldNotTouchMoneyCodesAndInternalLinks() {
        String reply = "Đơn REQ-20261023-A1B2C3: giá 1.000.000 đ, cọc 300.000 đ. [Xem đơn](/yeu-cau-cua-toi/26)";
        assertThat(guard.enforce(reply, "đơn của tôi")).isEqualTo(reply);
    }
}
