package com.fixlink.chatbot.application.service;

import com.fixlink.chatbot.adapter.out.persistence.ChatbotFeedbackEntity;
import com.fixlink.chatbot.adapter.out.persistence.ChatbotFeedbackJpaRepository;
import com.fixlink.chatbot.adapter.out.rag.InMemoryRagIndex;
import com.fixlink.chatbot.application.port.in.ChatbotUseCase;
import com.fixlink.chatbot.application.port.out.ChatbotToolPort;
import com.fixlink.chatbot.application.port.out.GeminiChatPort;
import com.fixlink.chatbot.domain.model.ChatAction;
import com.fixlink.chatbot.domain.model.ChatMessage;
import com.fixlink.chatbot.domain.model.ChatSession;
import com.fixlink.chatbot.domain.model.SourceReference;
import com.fixlink.chatbot.infrastructure.config.ChatbotProperties;
import com.fixlink.domain.exception.DomainException;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

@Service
public class ChatbotOrchestratorService implements ChatbotUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ChatbotOrchestratorService.class);

    private final GeminiChatPort geminiChatPort;
    private final ChatbotToolPort toolPort;
    private final InMemoryRagIndex ragIndex;
    private final ConversationMemoryService memoryService;
    private final RateLimiterService rateLimiterService;
    private final ChatbotFeedbackJpaRepository feedbackRepository;
    private final ChatbotProperties properties;

    private String baseSystemPrompt = "";

    public ChatbotOrchestratorService(
            GeminiChatPort geminiChatPort,
            ChatbotToolPort toolPort,
            InMemoryRagIndex ragIndex,
            ConversationMemoryService memoryService,
            RateLimiterService rateLimiterService,
            ChatbotFeedbackJpaRepository feedbackRepository,
            ChatbotProperties properties
    ) {
        this.geminiChatPort = geminiChatPort;
        this.toolPort = toolPort;
        this.ragIndex = ragIndex;
        this.memoryService = memoryService;
        this.rateLimiterService = rateLimiterService;
        this.feedbackRepository = feedbackRepository;
        this.properties = properties;
    }

    @PostConstruct
    public void loadSystemPrompt() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource("classpath:chatbot/prompts/system.vi.txt");
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    baseSystemPrompt = sb.toString().trim();
                    LOG.info("Loaded system prompt (length: {} chars)", baseSystemPrompt.length());
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to load system prompt from classpath:chatbot/prompts/system.vi.txt", e);
        }
    }

    @Override
    public ChatResponse handleMessage(ChatCommand command) {
        if (!properties.isEnabled()) {
            return new ChatResponse(
                    "Tính năng trợ lý CSKH hiện đang tạm bảo trì. Vui lòng thử lại sau.",
                    List.of(),
                    List.of(),
                    List.of(),
                    false,
                    null,
                    command.sessionId(),
                    "disabled"
            );
        }

        // 1. Validate input & Guardrails
        String rawMessage = command.message();
        if (rawMessage == null || rawMessage.isBlank()) {
            throw new DomainException("INVALID_INPUT", "Tin nhắn không được để trống", 400);
        }

        int maxChars = properties.getLimits().getMaxInputChars();
        if (rawMessage.length() > maxChars) {
            throw new DomainException("INPUT_TOO_LONG", "Tin nhắn quá dài (tối đa " + maxChars + " ký tự)", 400);
        }

        // Clean control characters
        String cleanMessage = rawMessage.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "").trim();

        // Rate limiting check
        rateLimiterService.checkRateLimit(command.clientIp(), command.userId());

        // 2. Session & Memory
        ChatSession session = memoryService.getOrCreateSession(command.sessionId(), command.userId(), command.role());
        List<ChatMessage> history = memoryService.getHistory(session.getSessionId());

        // 3. RAG Retrieval
        var ragResult = ragIndex.retrieve(cleanMessage, command.role());
        String ragContext = ragResult.mergedContent();
        List<SourceReference> sources = ragResult.sources();

        // 4. Build Dynamic System Prompt with runtime variables
        String dynamicPrompt = buildDynamicSystemPrompt(command, ragContext);

        // 5. Call LLM & Tools Orchestration
        var tools = toolPort.getToolDeclarations();
        var orchestrationResult = geminiChatPort.generateChatResponse(
                dynamicPrompt,
                history,
                cleanMessage,
                tools,
                command.userId(),
                command.role(),
                command.route(),
                command.guestContact()
        );

        // 6. Persist to Conversation Memory
        Instant now = Instant.now();
        ChatMessage userMsg = ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .role("user")
                .content(cleanMessage)
                .timestamp(now)
                .build();
        memoryService.addMessage(session.getSessionId(), userMsg);

        List<ChatAction> actions = new ArrayList<>();
        if (orchestrationResult.actions() != null) {
            for (var a : orchestrationResult.actions()) {
                actions.add(new ChatAction(a.get("type"), a.get("label"), a.get("url")));
            }
        }

        ChatMessage modelMsg = ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .role("model")
                .content(orchestrationResult.reply())
                .actions(actions)
                .suggestions(orchestrationResult.suggestions())
                .sources(sources)
                .needsHuman(orchestrationResult.needsHuman())
                .ticketId(orchestrationResult.ticketId())
                .modelUsed(orchestrationResult.modelUsed())
                .timestamp(now)
                .build();
        memoryService.addMessage(session.getSessionId(), modelMsg);

        return new ChatResponse(
                orchestrationResult.reply(),
                orchestrationResult.suggestions(),
                actions,
                sources,
                orchestrationResult.needsHuman(),
                orchestrationResult.ticketId(),
                session.getSessionId(),
                orchestrationResult.modelUsed()
        );
    }

    private String buildDynamicSystemPrompt(ChatCommand command, String ragContext) {
        StringBuilder sb = new StringBuilder();
        sb.append(baseSystemPrompt).append("\n\n");

        ZonedDateTime nowVn = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        String timeStr = nowVn.format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy (z)"));

        sb.append("=== THÔNG TIN NGỮ CẢNH HỆ THỐNG HIỆN TẠI ===\n");
        sb.append("- Thời gian hiện tại: ").append(timeStr).append("\n");
        sb.append("- Vai trò người dùng hiện tại: ").append(command.role() != null ? command.role() : "GUEST (Khách vãng lai)").append("\n");
        sb.append("- Đường dẫn trang đang xem (Route): ").append(command.route() != null ? command.route() : "/").append("\n");
        sb.append("- Trạng thái đăng nhập: ").append(command.userId() != null ? "ĐÃ ĐĂNG NHẬP (UserId: " + command.userId() + ")" : "CHƯA ĐĂNG NHẬP (Guest)").append("\n\n");

        if (ragContext != null && !ragContext.isBlank()) {
            sb.append("<ngu-canh-tri-thuc>\n");
            sb.append(ragContext).append("\n");
            sb.append("</ngu-canh-tri-thuc>\n\n");
            sb.append("LƯU Ý: Nội dung trong <ngu-canh-tri-thuc> là dữ liệu tra cứu chính thức. Hãy trích dẫn chính xác và không bịa đặt ngoài các tài liệu này.\n");
        }

        return sb.toString();
    }

    @Override
    public List<String> getSuggestions(String route, String role) {
        String r = route != null ? route.trim().toLowerCase() : "/";
        String uRole = role != null ? role.toUpperCase() : "GUEST";

        if (r.contains("dang-yeu-cau")) {
            return List.of(
                    "Máy lạnh chảy nước chọn mục nào?",
                    "Cần chuẩn bị ảnh chụp thế nào?",
                    "Thời gian thợ nhận việc là bao lâu?"
            );
        } else if (r.contains("yeu-cau-cua-toi")) {
            return List.of(
                    "Yêu cầu của tôi đang ở trạng thái nào?",
                    "Khi nào thì cần bấm nghiệm thu?",
                    "Nếu thợ đến muộn thì làm sao?"
            );
        } else if (r.contains("tho")) {
            if ("TECHNICIAN".equals(uRole)) {
                return List.of(
                        "Hồ sơ thợ của tôi đã được duyệt chưa?",
                        "Lỗi JOB_ALREADY_TAKEN là gì?",
                        "Làm sao để nhận nhiều việc hơn?"
                );
            }
            return List.of(
                    "Đăng ký làm thợ cần giấy tờ gì?",
                    "Quy trình duyệt KYC thợ thế nào?",
                    "Thợ nhận việc theo nguyên tắc nào?"
            );
        } else if (r.contains("login") || r.contains("dang-nhap")) {
            return List.of(
                    "Quên mật khẩu phải làm sao?",
                    "Tài khoản bị tạm khóa 15 phút là sao?",
                    "Đăng ký tài khoản mới thế nào?"
            );
        }

        // Default home suggestions
        return List.of(
                "FixLink là gì?",
                "Tủ lạnh không lạnh nên chọn dịch vụ nào?",
                "Tiền cọc được giữ ở đâu?",
                "Làm sao để đăng yêu cầu sửa chữa?"
        );
    }

    @Override
    public void recordFeedback(String sessionId, String messageId, Long userId, String rating, String comment) {
        if (sessionId == null || sessionId.isBlank() || rating == null || rating.isBlank()) {
            return;
        }
        ChatbotFeedbackEntity entity = ChatbotFeedbackEntity.builder()
                .sessionId(sessionId)
                .messageId(messageId)
                .userId(userId)
                .rating(rating.toUpperCase())
                .comment(comment != null ? comment.trim() : null)
                .createdAt(Instant.now())
                .build();
        feedbackRepository.save(entity);
        LOG.info("Recorded feedback {} for session {}", rating, sessionId);
    }

    @Override
    public void deleteSession(String sessionId, Long userId) {
        ChatSession session = memoryService.getSession(sessionId);
        if (session != null && session.getUserId() != null) {
            if (userId == null || !session.getUserId().equals(userId)) {
                throw new DomainException(
                        "CHATBOT_SESSION_FORBIDDEN",
                        "Bạn không có quyền xóa phiên hội thoại này.",
                        403
                );
            }
        }
        memoryService.clearSession(sessionId);
    }
}
