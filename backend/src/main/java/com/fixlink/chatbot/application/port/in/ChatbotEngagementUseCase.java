package com.fixlink.chatbot.application.port.in;

import com.fixlink.chatbot.domain.model.ChatAction;
import com.fixlink.chatbot.domain.model.ChatCard;
import java.util.List;
import java.util.Map;

/**
 * Các tính năng chăm sóc khách hàng quanh hội thoại chatbot: lời chào theo ngữ cảnh trang đang xem,
 * chuyển nhân viên một chạm kèm lịch sử hội thoại, và số liệu đo lường chất lượng (CSAT).
 */
public interface ChatbotEngagementUseCase {

    /** Lời chào chủ động theo trang và vai trò, không tốn lượt gọi LLM. */
    WelcomeResponse getWelcome(String route, Long userId, String role);

    /** Tạo phiếu hỗ trợ từ form trong khung chat, tự đính kèm lịch sử hội thoại cho nhân viên. */
    HandoffResult requestHandoff(HandoffCommand command);

    /** Thống kê phản hồi 👍/👎 và điểm hài lòng CSAT cho trang quản trị. */
    Map<String, Object> getQualityStats();

    record WelcomeResponse(
            String message,
            List<String> suggestions,
            List<ChatCard> cards,
            List<ChatAction> actions
    ) { }

    record HandoffCommand(
            String sessionId,
            Long userId,
            String role,
            String name,
            String contact,
            String issue
    ) { }

    record HandoffResult(String ticketCode, String priority, String message) { }
}
