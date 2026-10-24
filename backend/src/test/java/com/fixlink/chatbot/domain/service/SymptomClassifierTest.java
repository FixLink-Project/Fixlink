package com.fixlink.chatbot.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SymptomClassifierTest {

    @Test
    @DisplayName("Hiểu viết tắt: 'tl' là tủ lạnh (Điện lạnh), không phải tivi")
    void shouldClassifyAbbreviatedDevices() {
        assertThat(SymptomClassifier.classify("tl nhà mình hư rồi")).get()
                .extracting(SymptomClassifier.CategoryMatch::code).isEqualTo("DIEN_LANH");
        assertThat(SymptomClassifier.classify("ml ko lanh")).get()
                .extracting(SymptomClassifier.CategoryMatch::code).isEqualTo("DIEN_LANH");
        assertThat(SymptomClassifier.explainAbbreviations("tl hư r, sửa bn")).contains("tl = tủ lạnh", "bn = bao nhiêu");
    }

    @Test
    @DisplayName("Nhận diện khu vực phục vụ, phân biệt quận 1 với quận 12")
    void shouldDetectServiceArea() {
        assertThat(SymptomClassifier.detectAreaCode("nhà ở quận 7 nha")).contains("Q7");
        assertThat(SymptomClassifier.detectAreaCode("mình ở Gò Vấp")).contains("GO_VAP");
        assertThat(SymptomClassifier.detectAreaCode("mình ở quận 12")).isEmpty();
    }

    @Test
    @DisplayName("Đọc ngân sách theo nhiều cách viết, không nhầm số điện thoại hay chữ 'trên'")
    void shouldDetectBudget() {
        assertThat(SymptomClassifier.detectBudget("ngân sách tầm 300k")).contains(300_000L);
        assertThat(SymptomClassifier.detectBudget("khoảng 1tr2 thôi")).contains(1_200_000L);
        assertThat(SymptomClassifier.detectBudget("1,5 triệu được không")).contains(1_500_000L);
        assertThat(SymptomClassifier.detectBudget("giá 500.000đ")).contains(500_000L);
        assertThat(SymptomClassifier.detectBudget("sdt 0912345678, giá bao nhiêu")).isEmpty();
        assertThat(SymptomClassifier.detectBudget("máy 2 trên 3 năm rồi")).isEmpty();
    }

    @Test
    @DisplayName("Link điền sẵn form được mã hóa an toàn")
    void shouldBuildEncodedDraftLink() {
        String url = RepairRequestDraftLink.build("DIEN_LANH", "Tủ lạnh không lạnh", "Ngăn mát ấm & kêu to", "Q7", 300_000L);
        assertThat(url).startsWith("/dang-yeu-cau?category=DIEN_LANH")
                .contains("&title=T%E1%BB%A7%20l%E1%BA%A1nh")
                .contains("%26")
                .endsWith("&area=Q7&budget=300000");
    }
}
