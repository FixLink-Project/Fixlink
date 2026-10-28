package com.fixlink.chatbot.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fixlink.application.port.in.MasterDataUseCase;
import com.fixlink.chatbot.adapter.out.gemini.ExtractiveFallbackSynthesizer;
import com.fixlink.chatbot.domain.service.LiveCatalog;
import com.fixlink.chatbot.domain.service.SymptomClassifier;
import com.fixlink.domain.model.Service;
import com.fixlink.domain.model.ServiceCategory;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Chatbot trả lời theo dữ liệu database mới nhất: thêm/tắt danh mục, đổi giá, mở khu vực mới. */
class LiveCatalogRefresherTest {

    private MasterDataUseCase masterData;
    private LiveCatalogRefresher refresher;
    private final ExtractiveFallbackSynthesizer synthesizer = new ExtractiveFallbackSynthesizer();

    @BeforeEach
    void setUp() {
        LiveCatalog.reset();
        masterData = mock(MasterDataUseCase.class);
        refresher = new LiveCatalogRefresher(masterData);
    }

    @AfterEach
    void tearDown() {
        LiveCatalog.reset();
    }

    private static ServiceCategory category(long id, String code, String name, String description) {
        ServiceCategory c = new ServiceCategory();
        c.setId(id);
        c.setCode(code);
        c.setName(name);
        c.setDescription(description);
        c.setIsActive(true);
        return c;
    }

    private static Service service(long categoryId, String code, String name, String description, long price, int minutes) {
        Service s = new Service();
        s.setCategoryId(categoryId);
        s.setCode(code);
        s.setName(name);
        s.setDescription(description);
        s.setBasePrice(BigDecimal.valueOf(price));
        s.setEstimatedDurationMinutes(minutes);
        s.setIsActive(true);
        return s;
    }

    private String reply(String q) {
        return synthesizer.synthesize(q, "").reply();
    }

    /** Database sau khi admin: đổi giá vệ sinh máy lạnh lên 170.000, thêm danh mục Máy tính, tắt Khóa & Cửa cuốn, mở Quận 5. */
    private void databaseChanged() {
        when(masterData.getActiveCategories()).thenReturn(List.of(
                category(1, "DIEN_LANH", "Sửa Chữa Điện Lạnh", "Chuyên sửa chữa, bảo dưỡng điều hòa, tủ lạnh, máy giặt, tủ đông tận nơi"),
                category(2, "DIEN_NUOC", "Sửa Chữa Điện Nước", "Khắc phục chập điện, rò rỉ đường ống nước, lắp thiết bị vệ sinh"),
                category(5, "MAY_TINH", "Máy Tính & Thiết Bị Số", "Laptop, PC văn phòng, Bộ nguồn, Màn hình máy tính")));
        when(masterData.getServices(any())).thenReturn(List.of(
                service(1, "VE_SINH_MAY_LANH", "Vệ sinh máy lạnh treo tường (1.0 - 2.5 HP)", "Bảo dưỡng", 170_000, 45),
                service(1, "NAP_GA_R32", "Nạp ga máy lạnh R32 / R410A trọn gói", "Bổ sung ga", 250_000, 30),
                service(2, "SUA_ONG_NUOC", "Thông nghẹt & Xử lý rò rỉ đường ống nước", "Dò rò rỉ", 180_000, 45),
                service(5, "SUA_LAPTOP", "Sửa laptop không lên nguồn", "Kiểm tra nguồn, bo mạch", 400_000, 90),
                service(4, "SUA_CUA_CUON", "Sửa chữa cửa cuốn kẹt nan / hỏng motor", "Cửa cuốn", 350_000, 90)));
        when(masterData.getActiveAreas()).thenReturn(List.of(
                new MasterDataUseCase.ServiceAreaView(1L, "Q1", "Quận 1", "TP. Hồ Chí Minh"),
                new MasterDataUseCase.ServiceAreaView(9L, "Q5", "Quận 5", "TP. Hồ Chí Minh")));
        refresher.refresh();
    }

    @Test
    @DisplayName("Danh mục, giá, khu vực mới trong database được dùng ngay trong câu trả lời")
    void shouldAnswerFromLatestDatabase() {
        databaseChanged();

        // Giá mới, gói mới, danh mục đã tắt biến mất khỏi bảng giá
        assertThat(reply("vệ sinh máy lạnh bao nhiêu tiền")).contains("170.000 đ").doesNotContain("150.000 đ");
        assertThat(reply("dịch vụ nào đắt nhất")).contains("Sửa laptop không lên nguồn").contains("400.000 đ");
        assertThat(reply("gợi ý cho tôi tất cả dịch vụ hiện đang có")).contains("Máy Tính & Thiết Bị Số")
                .doesNotContain("Cửa Cuốn");
        assertThat(reply("fixlink có bao nhiêu loại dịch vụ")).contains("3 nhóm").contains("Máy Tính & Thiết Bị Số");

        // Laptop thuộc danh mục mới: không còn "chưa hỗ trợ"
        assertThat(reply("laptop tôi bị hư, gợi ý dịch vụ")).doesNotContain("chưa hỗ trợ");
        assertThat(SymptomClassifier.classify("laptop không lên nguồn").map(SymptomClassifier.CategoryMatch::code))
                .contains("MAY_TINH");

        // Khu vực mới mở: đang phục vụ; khu vực đã tắt (Quận 3): không còn coi là đang phục vụ
        assertThat(reply("quận 5 có thợ không")).startsWith("**Có**").contains("Quận 5");
        assertThat(SymptomClassifier.detectAreaCode("ở quận 3")).isEmpty();
        assertThat(reply("fixlink hoạt động ở đâu")).contains("Quận 1").contains("Quận 5").doesNotContain("Gò Vấp");

        // AI cũng nhận dữ liệu mới
        assertThat(LiveCatalog.promptBlock()).contains("170.000 đ").contains("Máy Tính & Thiết Bị Số").contains("Quận 5");
    }

    @Test
    @DisplayName("Database lỗi hoặc rỗng: giữ dữ liệu cũ; chỉ đọc lại database khi dữ liệu đã cũ hơn 60 giây")
    void shouldKeepOldDataOnErrorAndThrottleReloads() {
        when(masterData.getActiveCategories()).thenThrow(new IllegalStateException("db down"));
        refresher.refresh();
        assertThat(reply("vệ sinh máy lạnh bao nhiêu tiền")).contains("150.000 đ");

        org.mockito.Mockito.reset(masterData);
        databaseChanged();
        refresher.refreshIfStale();
        refresher.refreshIfStale();
        // databaseChanged() gọi refresh() 1 lần + refreshIfStale() đọc thêm đúng 1 lần (lần thứ hai còn mới, bỏ qua)
        verify(masterData, times(2)).getActiveAreas();
    }
}
