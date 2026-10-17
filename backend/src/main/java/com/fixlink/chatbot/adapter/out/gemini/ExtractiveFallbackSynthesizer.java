package com.fixlink.chatbot.adapter.out.gemini;

import com.fixlink.chatbot.application.port.out.GeminiChatPort.ChatOrchestrationResult;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ExtractiveFallbackSynthesizer {

    public ChatOrchestrationResult synthesize(String userMessage, String ragContext) {
        String query = userMessage != null ? userMessage.trim().toLowerCase(Locale.ROOT) : "";

        // 1. Chào hỏi
        if (query.startsWith("chào") || query.startsWith("xin chào") || query.startsWith("hi") || query.startsWith("hello")) {
            return new ChatOrchestrationResult(
                    "Xin chào bạn! Mình là FixLink Assistant, trợ lý CSKH của FixLink. Mình có thể hỗ trợ bạn tìm thợ sửa điện lạnh, điện nước, đồ gia dụng, sửa khóa hoặc hướng dẫn quy trình sử dụng nền tảng.",
                    List.of("Cách đăng yêu cầu sửa chữa", "Các loại dịch vụ hỗ trợ", "Quy trình thợ nhận việc"),
                    List.of(Map.of("type", "link", "label", "Đăng yêu cầu ngay", "url", "/dang-yeu-cau")),
                    false,
                    null,
                    "local-fallback"
            );
        }

        // 2. Tra cứu từ kho tri thức
        if (ragContext != null && !ragContext.isBlank()) {
            StringBuilder reply = new StringBuilder();
            reply.append("Dựa trên tài liệu quy định chính thức của FixLink, mình xin cung cấp thông tin như sau:\n\n");

            String[] lines = ragContext.split("\n");
            int count = 0;
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("---")) {
                    continue;
                }
                if (!trimmed.startsWith("-") && !trimmed.startsWith("•") && !trimmed.startsWith("*") && !trimmed.startsWith("|")) {
                    reply.append("• ");
                }
                reply.append(trimmed).append("\n");
                count++;
                if (count >= 6) {
                    break;
                }
            }

            reply.append("\nNếu bạn cần thêm sự trợ giúp chuyên sâu, bạn có thể tạo phiếu hỗ trợ để gặp nhân viên CSKH.");

            return new ChatOrchestrationResult(
                    reply.toString().trim(),
                    List.of("Gặp nhân viên hỗ trợ", "Xem yêu cầu của tôi", "Quay lại trang chủ"),
                    List.of(
                            Map.of("type", "link", "label", "Đăng yêu cầu", "url", "/dang-yeu-cau"),
                            Map.of("type", "link", "label", "Yêu cầu của tôi", "url", "/yeu-cau-cua-toi")
                    ),
                    false,
                    null,
                    "local-synthesizer"
            );
        }

        // 3. Fallback mặc định khi không có tri thức phù hợp
        return new ChatOrchestrationResult(
                "Hiện tại hệ thống đang kết nối đường truyền AI để xử lý. Về câu hỏi của bạn, mình chưa có thông tin chính thức. Bạn có muốn mình tạo một phiếu hỗ trợ (Ticket) để nhân viên CSKH liên hệ giải đáp không?",
                List.of("Tạo phiếu hỗ trợ", "Xem danh mục dịch vụ", "Trang chủ FixLink"),
                List.of(
                        Map.of("type", "link", "label", "Trang chủ", "url", "/"),
                        Map.of("type", "link", "label", "Đăng yêu cầu", "url", "/dang-yeu-cau")
                ),
                true,
                null,
                "local-fallback"
        );
    }
}
