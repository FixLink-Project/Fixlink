package com.fixlink.chatbot.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Thẻ thông tin hiển thị trực quan trong khung chat (đơn sửa chữa có thanh tiến độ, lịch hẹn,
 * bảng giá, tiền cọc...). Dữ liệu thẻ lấy trực tiếp từ kết quả tool — không do LLM sinh ra — nên
 * luôn khớp với hệ thống.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCard {

    /** request | appointment | price_list | deposit | technician | job | ticket */
    private String type;
    private String title;
    private String subtitle;
    /** Nhãn trạng thái ngắn, ví dụ "Đang sửa chữa". */
    private String badge;
    /** info | success | warning | danger | neutral */
    private String badgeTone;
    private List<Field> fields;
    /** Các bước của thanh tiến độ (chỉ thẻ đơn sửa chữa). */
    private List<String> steps;
    /** Bước hiện tại trong {@link #steps}, bắt đầu từ 0. */
    private Integer currentStep;
    private String note;
    private String url;
    private String urlLabel;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Field {
        private String label;
        private String value;
    }
}
