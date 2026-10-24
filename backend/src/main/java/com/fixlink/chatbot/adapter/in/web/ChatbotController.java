package com.fixlink.chatbot.adapter.in.web;

import com.fixlink.adapter.in.web.dto.response.ApiResponse;
import com.fixlink.chatbot.adapter.in.web.dto.ChatFeedbackRequest;
import com.fixlink.chatbot.adapter.in.web.dto.HandoffRequest;
import com.fixlink.chatbot.adapter.in.web.dto.ChatMessageRequest;
import com.fixlink.chatbot.adapter.in.web.dto.ChatMessageResponse;
import com.fixlink.chatbot.application.port.in.ChatbotEngagementUseCase;
import com.fixlink.chatbot.application.port.in.ChatbotUseCase;
import com.fixlink.chatbot.application.port.in.ChatbotUseCase.ChatCommand;
import com.fixlink.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chatbot")
@Tag(name = "Chatbot AI CSKH", description = "Các API tương tác với Trợ lý ảo FixLink Assistant")
public class ChatbotController {

    private final ChatbotUseCase chatbotUseCase;
    private final ChatbotEngagementUseCase engagementUseCase;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    public ChatbotController(ChatbotUseCase chatbotUseCase, ChatbotEngagementUseCase engagementUseCase) {
        this.chatbotUseCase = chatbotUseCase;
        this.engagementUseCase = engagementUseCase;
    }

    @PostMapping("/messages")
    @Operation(summary = "Gửi tin nhắn tới chatbot", description = "Xử lý tin nhắn khách hàng (guest hoặc authenticated), kết hợp RAG, Tool calling và bộ nhớ hội thoại.")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @Valid @RequestBody ChatMessageRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        Long userId = principal != null ? principal.getId() : null;
        String role = principal != null ? principal.getRole().name() : "GUEST";
        String route = request.getPageContext() != null ? request.getPageContext().getRoute() : "/";
        String clientIp = extractClientIp(servletRequest);

        ChatCommand command = new ChatCommand(
                request.getSessionId(),
                request.getMessage(),
                route,
                userId,
                role,
                clientIp,
                request.getGuestContact()
        );

        var result = chatbotUseCase.handleMessage(command);

        boolean showModel = "local".equalsIgnoreCase(activeProfile) || "dev".equalsIgnoreCase(activeProfile);

        ChatMessageResponse response = ChatMessageResponse.builder()
                .reply(result.reply())
                .suggestions(result.suggestions())
                .actions(result.actions())
                .sources(result.sources())
                .needsHuman(result.needsHuman())
                .ticketId(result.ticketId())
                .sessionId(result.sessionId())
                .modelUsed(showModel ? result.modelUsed() : null)
                .cards(result.cards())
                .offerHandoff(result.offerHandoff())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Phản hồi chatbot thành công", response));
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Lấy danh sách gợi ý câu hỏi", description = "Gợi ý các câu hỏi nhanh theo ngữ cảnh route và vai trò của người dùng.")
    public ResponseEntity<ApiResponse<List<String>>> getSuggestions(
            @RequestParam(required = false, defaultValue = "/") String route,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String role = principal != null ? principal.getRole().name() : "GUEST";
        List<String> suggestions = chatbotUseCase.getSuggestions(route, role);
        return ResponseEntity.ok(ApiResponse.success("Lấy gợi ý thành công", suggestions));
    }

    @GetMapping("/welcome")
    @Operation(summary = "Lời chào theo ngữ cảnh trang",
            description = "Lời chào chủ động theo trang đang xem và vai trò (ví dụ đang xem chi tiết đơn thì hiện ngay trạng thái đơn), không tốn lượt gọi AI.")
    public ResponseEntity<ApiResponse<ChatbotEngagementUseCase.WelcomeResponse>> getWelcome(
            @RequestParam(required = false, defaultValue = "/") String route,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal != null ? principal.getId() : null;
        String role = principal != null ? principal.getRole().name() : "GUEST";
        return ResponseEntity.ok(ApiResponse.success("Lấy lời chào thành công",
                engagementUseCase.getWelcome(route, userId, role)));
    }

    @PostMapping("/handoff")
    @Operation(summary = "Gặp nhân viên CSKH",
            description = "Tạo phiếu hỗ trợ từ form trong khung chat, tự đính kèm lịch sử hội thoại và suy ra mức ưu tiên.")
    public ResponseEntity<ApiResponse<ChatbotEngagementUseCase.HandoffResult>> requestHandoff(
            @Valid @RequestBody HandoffRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal != null ? principal.getId() : null;
        String role = principal != null ? principal.getRole().name() : "GUEST";
        var result = engagementUseCase.requestHandoff(new ChatbotEngagementUseCase.HandoffCommand(
                request.getSessionId(), userId, role, request.getName(), request.getContact(), request.getIssue()));
        return ResponseEntity.ok(ApiResponse.success("Đã chuyển yêu cầu tới nhân viên CSKH", result));
    }

    @PostMapping("/feedback")
    @Operation(summary = "Gửi đánh giá phản hồi chatbot", description = "Lưu đánh giá 👍/👎 và ý kiến phản hồi của người dùng cho tin nhắn chatbot.")
    public ResponseEntity<ApiResponse<Void>> recordFeedback(
            @Valid @RequestBody ChatFeedbackRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal != null ? principal.getId() : null;
        chatbotUseCase.recordFeedback(
                request.getSessionId(),
                request.getMessageId(),
                userId,
                request.getRating(),
                request.getComment()
        );
        return ResponseEntity.ok(ApiResponse.success("Ghi nhận đánh giá thành công", null));
    }

    @DeleteMapping("/sessions/{id}")
    @Operation(summary = "Xóa phiên hội thoại", description = "Xóa lịch sử và giải phóng bộ nhớ của phiên hội thoại theo sessionId. Chỉ cho phép xóa session thuộc về user đang đăng nhập.")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @PathVariable("id") String sessionId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal != null ? principal.getId() : null;
        chatbotUseCase.deleteSession(sessionId, userId);
        return ResponseEntity.ok(ApiResponse.success("Xóa phiên hội thoại thành công", null));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
