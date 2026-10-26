package com.fixlink.chatbot.domain.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Dựng đường dẫn mở trang Đăng yêu cầu đã điền sẵn thông tin khách mô tả trong khung chat
 * ({@code /dang-yeu-cau?category=...&title=...}). Chatbot không tạo đơn hộ: khách tự kiểm tra và bấm đăng.
 */
public final class RepairRequestDraftLink {

    public static final String ACTION_LABEL = "Đăng yêu cầu với thông tin này";

    private RepairRequestDraftLink() {
    }

    public static String build(String categoryCode, String title, String description, String areaCode, Long budget) {
        StringBuilder url = new StringBuilder("/dang-yeu-cau?category=").append(encode(categoryCode));
        if (title != null && !title.isBlank()) {
            url.append("&title=").append(encode(truncate(title.trim(), 200)));
        }
        if (description != null && !description.isBlank()) {
            url.append("&description=").append(encode(truncate(description.trim(), 1500)));
        }
        if (areaCode != null && !areaCode.isBlank()) {
            url.append("&area=").append(encode(areaCode));
        }
        if (budget != null && budget > 0) {
            url.append("&budget=").append(budget);
        }
        return url.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
