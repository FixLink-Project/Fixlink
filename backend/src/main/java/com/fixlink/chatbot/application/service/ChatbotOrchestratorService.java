package com.fixlink.chatbot.application.service;

import com.fixlink.chatbot.adapter.out.gemini.ExtractiveFallbackSynthesizer;
import com.fixlink.chatbot.adapter.out.persistence.ChatbotFeedbackEntity;
import com.fixlink.chatbot.adapter.out.persistence.ChatbotFeedbackJpaRepository;
import com.fixlink.chatbot.adapter.out.rag.InMemoryRagIndex;
import com.fixlink.chatbot.application.port.in.ChatbotUseCase;
import com.fixlink.chatbot.application.port.out.ChatbotToolPort;
import com.fixlink.chatbot.application.port.out.LlmChatPort;
import com.fixlink.chatbot.domain.model.ChatAction;
import com.fixlink.chatbot.domain.model.ChatCard;
import com.fixlink.chatbot.domain.model.ChatMessage;
import com.fixlink.chatbot.domain.model.ChatSession;
import com.fixlink.chatbot.domain.model.SourceReference;
import com.fixlink.chatbot.domain.service.RepairRequestDraftLink;
import com.fixlink.chatbot.domain.service.SymptomClassifier;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

@Service
public class ChatbotOrchestratorService implements ChatbotUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ChatbotOrchestratorService.class);

    private final LlmChatPort geminiChatPort;
    private final ChatbotToolPort toolPort;
    private final InMemoryRagIndex ragIndex;
    private final ConversationMemoryService memoryService;
    private final RateLimiterService rateLimiterService;
    private final ChatbotFeedbackJpaRepository feedbackRepository;
    private final ChatbotProperties properties;
    private final ExtractiveFallbackSynthesizer verifiedAnswers;
    private final ScenarioPlaybook playbook;
    private final ReplyGroundingGuard groundingGuard;
    private final ToolCallRecorder recorder;
    private final ChatCardFactory cardFactory;
    private final FaqAnswerCache faqCache;

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter PROMPT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm, EEEE dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));

    private String baseSystemPrompt = "";
    /** Bản rút gọn cho nhà cung cấp giới hạn token/phút (Groq miễn phí). */
    private String compactSystemPrompt = "";

    public ChatbotOrchestratorService(
            LlmChatPort geminiChatPort,
            ChatbotToolPort toolPort,
            InMemoryRagIndex ragIndex,
            ConversationMemoryService memoryService,
            RateLimiterService rateLimiterService,
            ChatbotFeedbackJpaRepository feedbackRepository,
            ChatbotProperties properties,
            ExtractiveFallbackSynthesizer verifiedAnswers,
            ScenarioPlaybook playbook,
            ReplyGroundingGuard groundingGuard,
            ToolCallRecorder recorder,
            ChatCardFactory cardFactory,
            FaqAnswerCache faqCache
    ) {
        this.geminiChatPort = geminiChatPort;
        this.toolPort = toolPort;
        this.ragIndex = ragIndex;
        this.memoryService = memoryService;
        this.rateLimiterService = rateLimiterService;
        this.feedbackRepository = feedbackRepository;
        this.properties = properties;
        this.verifiedAnswers = verifiedAnswers;
        this.playbook = playbook;
        this.groundingGuard = groundingGuard;
        this.recorder = recorder;
        this.cardFactory = cardFactory;
        this.faqCache = faqCache;
    }

    @PostConstruct
    public void loadSystemPrompt() {
        baseSystemPrompt = readPrompt("classpath:chatbot/prompts/system.vi.txt");
        compactSystemPrompt = readPrompt("classpath:chatbot/prompts/system.compact.vi.txt");
        if (compactSystemPrompt.isEmpty()) {
            compactSystemPrompt = baseSystemPrompt;
        }
    }

    private String readPrompt(String location) {
        try {
            Resource resource = new PathMatchingResourcePatternResolver().getResource(location);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    String prompt = sb.toString().trim();
                    LOG.info("Loaded system prompt {} (length: {} chars)", location, prompt.length());
                    return prompt;
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to load system prompt from {}", location, e);
        }
        return "";
    }

    /** Tool dành riêng cho người đã đăng nhập; gửi ít tool hơn giúp giảm token mỗi request. */
    private static final Set<String> CUSTOMER_TOOLS = Set.of(
            "get_my_repair_requests", "get_repair_request_status", "get_my_appointments", "get_request_quotations");
    private static final Set<String> TECHNICIAN_TOOLS = Set.of(
            "get_my_repair_requests", "get_repair_request_status", "get_my_appointments",
            "get_my_technician_status", "find_matching_jobs", "get_request_quotations");

    private List<Map<String, Object>> toolsForRole(String role) {
        String r = role != null ? role.toUpperCase(Locale.ROOT) : "GUEST";
        return toolPort.getToolDeclarations().stream()
                .filter(t -> {
                    String name = String.valueOf(t.get("name"));
                    boolean personal = TECHNICIAN_TOOLS.contains(name);
                    if (!personal) {
                        return true;
                    }
                    return switch (r) {
                        case "CUSTOMER" -> CUSTOMER_TOOLS.contains(name);
                        case "TECHNICIAN" -> true;
                        default -> false;
                    };
                })
                .toList();
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
                    "disabled",
                    List.of(),
                    false
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

        // 2. Session & Memory — không cho một danh tính dùng lại lịch sử hội thoại của tài khoản khác
        String scopedSessionId = scopeSessionId(command.sessionId(), command.userId());
        ChatSession session = memoryService.getOrCreateSession(scopedSessionId, command.userId(), command.role());
        List<ChatMessage> history = memoryService.getHistory(session.getSessionId());

        // 3. RAG Retrieval
        var ragResult = ragIndex.retrieve(cleanMessage, command.role());
        String ragContext = ragResult.mergedContent();
        List<SourceReference> sources = ragResult.sources();

        // 4. Build Dynamic System Prompt with runtime variables
        String userConversation = userConversationText(history, cleanMessage);
        List<String> scenarioGuidance = playbook.guidanceFor(cleanMessage, userConversation,
                lastAssistantAskedQuestion(history));
        String dynamicPrompt = buildDynamicSystemPrompt(command, sources, ragContext, scenarioGuidance);

        // 5. Call LLM & Tools Orchestration
        var tools = toolsForRole(command.role());
        LlmChatPort.ChatOrchestrationResult orchestrationResult;
        List<ToolCallRecorder.ToolTrace> toolTraces;
        FaqAnswerCache.Entry cached = faqCache.lookup(cleanMessage, command.role(), history);
        if (cached != null) {
            orchestrationResult = cached.result();
            toolTraces = cached.traces();
        } else {
            recorder.start();
            try {
                orchestrationResult = geminiChatPort.generateChatResponse(
                        dynamicPrompt,
                        history,
                        cleanMessage,
                        tools,
                        command.userId(),
                        command.role(),
                        command.route(),
                        command.guestContact(),
                        ragContext
                );
            } finally {
                toolTraces = recorder.stop();
            }
            faqCache.store(cleanMessage, command.role(), history, orchestrationResult, toolTraces);
        }

        // 6. Persist to Conversation Memory
        Instant now = Instant.now();
        ChatMessage userMsg = ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .role("user")
                .content(cleanMessage)
                .timestamp(now)
                .build();
        memoryService.addMessage(session.getSessionId(), userMsg);

        String reply = groundingGuard.enforce(localizeRefusal(orchestrationResult.reply(), cleanMessage),
                userConversation);

        List<ChatAction> actions = new ArrayList<>();
        if (orchestrationResult.actions() != null) {
            for (var a : orchestrationResult.actions()) {
                String label = a.get("label") != null ? a.get("label") : "";
                // Không có trang riêng cho phiếu hỗ trợ: nút "Tạo phiếu..." do model tự chế sẽ dẫn sai chỗ
                if (label.toLowerCase(Locale.ROOT).contains("phiếu")) {
                    continue;
                }
                actions.add(new ChatAction(a.get("type"), label, a.get("url")));
            }
        }
        String draftSummary = attachDraftRequestAction(actions, cleanMessage, userConversation, history);
        String modelUsed = orchestrationResult.modelUsed() != null ? orchestrationResult.modelUsed() : "";
        if (draftSummary != null && (modelUsed.equals("local-fallback") || modelUsed.equals("local-synthesizer"))) {
            // AI tạm quá tải nhưng đã đủ thông tin đặt thợ: trả bản tóm tắt cụ thể thay vì câu xin lỗi chung chung
            reply = draftSummary;
        } else if (draftSummary != null && modelUsed.startsWith("local-")) {
            // Câu trả lời FAQ cục bộ vẫn giữ, kèm bản tóm tắt yêu cầu đã điền sẵn lên đầu
            reply = draftSummary + NL + NL + "---" + NL + NL + reply;
        }

        List<ChatCard> cards = cardFactory.fromToolTraces(toolTraces);
        boolean offerHandoff = shouldOfferHandoff(cleanMessage, history, orchestrationResult, modelUsed);

        ChatMessage modelMsg = ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .role("model")
                .content(reply)
                .actions(actions)
                .suggestions(orchestrationResult.suggestions())
                .sources(sources)
                .needsHuman(orchestrationResult.needsHuman())
                .ticketId(orchestrationResult.ticketId())
                .modelUsed(orchestrationResult.modelUsed())
                .timestamp(now)
                .build();
        memoryService.addMessage(session.getSessionId(), modelMsg);

        List<String> suggestions = new ArrayList<>(
                orchestrationResult.suggestions() != null && !orchestrationResult.suggestions().isEmpty()
                        ? orchestrationResult.suggestions()
                        : getSuggestions(command.route(), command.role()));
        if (offerHandoff && suggestions.stream().noneMatch(x -> x.toLowerCase(Locale.ROOT).contains("nhân viên"))) {
            suggestions.add(0, "Gặp nhân viên hỗ trợ");
        }

        return new ChatResponse(
                reply,
                suggestions,
                actions,
                sources,
                orchestrationResult.needsHuman(),
                orchestrationResult.ticketId(),
                command.sessionId() != null && !command.sessionId().isBlank() ? command.sessionId() : session.getSessionId(),
                orchestrationResult.modelUsed(),
                cards,
                offerHandoff
        );
    }

    /**
     * Chủ động mời gặp nhân viên như CSKH của các sàn lớn: khi khách bức xúc/khiếu nại, khi trợ lý không trả lời
     * được hai lượt liên tiếp, khi khách hỏi lại gần như nguyên văn câu trước, hoặc khi model đánh dấu cần người.
     */
    private boolean shouldOfferHandoff(String message, List<ChatMessage> history,
                                       LlmChatPort.ChatOrchestrationResult result, String modelUsed) {
        if (result.ticketId() != null) {
            return false; // đã chuyển nhân viên bằng phiếu hỗ trợ
        }
        if (result.needsHuman()) {
            return true;
        }
        List<String> scenarios = playbook.matchedScenarioIds(message);
        if (scenarios.contains("abusive") || scenarios.contains("quality_complaint")
                || scenarios.contains("human_handoff") || scenarios.contains("refund_payment")) {
            return true;
        }
        boolean unresolvedNow = modelUsed.equals("local-fallback") || modelUsed.equals("local-synthesizer");
        ChatMessage lastBot = null;
        ChatMessage lastUser = null;
        if (history != null) {
            for (int i = history.size() - 1; i >= 0 && (lastBot == null || lastUser == null); i--) {
                ChatMessage m = history.get(i);
                if ("user".equalsIgnoreCase(m.getRole())) {
                    if (lastUser == null) {
                        lastUser = m;
                    }
                } else if (lastBot == null) {
                    lastBot = m;
                }
            }
        }
        if (unresolvedNow && lastBot != null && lastBot.getModelUsed() != null
                && (lastBot.getModelUsed().equals("local-fallback") || lastBot.getModelUsed().equals("local-synthesizer"))) {
            return true;
        }
        return lastUser != null && similarity(message, lastUser.getContent()) >= 0.8;
    }

    /** Độ trùng từ (Jaccard) giữa hai câu sau khi chuẩn hóa — phát hiện khách phải hỏi lại cùng một câu. */
    static double similarity(String a, String b) {
        if (a == null || b == null) {
            return 0;
        }
        Set<String> wa = new HashSet<>(List.of(SymptomClassifier.normalize(a).split(" ")));
        Set<String> wb = new HashSet<>(List.of(SymptomClassifier.normalize(b).split(" ")));
        if (wa.size() < 4 || wb.size() < 4) {
            return 0;
        }
        Set<String> inter = new HashSet<>(wa);
        inter.retainAll(wb);
        Set<String> union = new HashSet<>(wa);
        union.addAll(wb);
        return (double) inter.size() / union.size();
    }

    private static final Pattern BOOKING_INTENT = Pattern.compile(
            "\\b(dat tho|goi tho|can tho|tim tho|dang yeu cau|dat lich|muon sua|can sua|sua giup|sua dum|cho tho qua|tho qua|tho den)\\b");

    /**
     * Gắn nút "Đăng yêu cầu với thông tin này" (form điền sẵn) khi khách đã mô tả thiết bị và có ý định
     * đặt thợ, hoặc model đã gợi ý đăng yêu cầu. Làm bằng code để không phụ thuộc việc model nhớ gọi tool
     * hay chép đúng đường dẫn dài.
     */
    private String attachDraftRequestAction(List<ChatAction> actions, String message, String userConversation,
                                            List<ChatMessage> history) {
        boolean alreadyPrefilled = actions.stream()
                .anyMatch(a -> a.getUrl() != null && a.getUrl().startsWith("/dang-yeu-cau?"));
        if (alreadyPrefilled) {
            return null;
        }
        var category = SymptomClassifier.classify(userConversation);
        if (category.isEmpty() || SymptomClassifier.isDanger(message)) {
            return null;
        }
        String normalizedMessage = SymptomClassifier.normalize(message);
        var area = SymptomClassifier.detectAreaCode(userConversation);
        var budget = SymptomClassifier.detectBudget(userConversation);
        boolean modelSuggestedPosting = actions.stream().anyMatch(a -> "/dang-yeu-cau".equals(a.getUrl()));
        boolean wantsBooking = BOOKING_INTENT.matcher(normalizedMessage).find()
                || area.isPresent() || budget.isPresent();
        if (!modelSuggestedPosting && !wantsBooking) {
            return null;
        }

        List<String> userMessages = new ArrayList<>();
        if (history != null) {
            for (ChatMessage msg : history) {
                if ("user".equalsIgnoreCase(msg.getRole()) && msg.getContent() != null) {
                    userMessages.add(msg.getContent().trim());
                }
            }
        }
        userMessages.add(message.trim());
        // Tiêu đề: tin nhắn gần nhất có nhắc thiết bị; mô tả: các tin nhắn khách đã kể trong phiên.
        String title = userMessages.stream()
                .filter(m -> SymptomClassifier.classify(m).isPresent())
                .reduce((first, second) -> second)
                .map(ChatbotOrchestratorService::symptomOnly)
                .filter(t -> t.length() >= 4)
                .orElse(category.get().name());
        String description = String.join(". ", userMessages.subList(Math.max(0, userMessages.size() - 4), userMessages.size()));

        String url = RepairRequestDraftLink.build(category.get().code(), capitalize(title), description,
                area.orElse(null), budget.orElse(null));
        actions.removeIf(a -> a.getUrl() != null && a.getUrl().startsWith("/dang-yeu-cau"));
        actions.add(0, new ChatAction("link", RepairRequestDraftLink.ACTION_LABEL, url));
        while (actions.size() > 3) {
            actions.remove(actions.size() - 1);
        }

        StringBuilder summary = new StringBuilder("Mình đã tổng hợp yêu cầu của bạn:")
                .append(NL).append("- **Danh mục:** ").append(category.get().name());
        area.ifPresent(code -> summary.append(NL).append("- **Khu vực:** ").append(SymptomClassifier.areaLabel(code)));
        budget.ifPresent(b -> summary.append(NL).append("- **Ngân sách:** ").append(vnd(b))
                .append(" (cọc 30% = ").append(vnd((long) Math.ceil(b * 0.3)))
                .append(" khi bạn chọn thợ, phần còn lại khi bạn đánh giá và nghiệm thu)"));
        summary.append(NL).append(NL).append("Bấm **").append(RepairRequestDraftLink.ACTION_LABEL)
                .append("** để mở form đã điền sẵn, kiểm tra lại, bổ sung địa chỉ cụ thể");
        if (area.isEmpty()) {
            summary.append(", khu vực");
        }
        if (budget.isEmpty()) {
            summary.append(", ngân sách");
        }
        summary.append(" và ảnh hiện trường rồi bấm **Đăng yêu cầu** (cần đăng nhập tài khoản khách hàng).");
        return summary.toString();
    }

    /** Xuống dòng trong Markdown của câu trả lời (luôn là \n, không phụ thuộc hệ điều hành). */
    private static final String NL = "\n";

    private static String vnd(long amount) {
        return String.format(Locale.ROOT, "%,d", amount).replace(',', '.') + " đ";
    }

    /** Giữ phần mô tả sự cố trong câu, bỏ các vế nói về khu vực, ngân sách hay ý định đặt thợ. */
    private static String symptomOnly(String message) {
        List<String> kept = new ArrayList<>();
        for (String part : message.split("[,.;!?\\n]+")) {
            String p = part.trim();
            if (p.isEmpty()
                    || SymptomClassifier.detectAreaCode(p).isPresent()
                    || SymptomClassifier.detectBudget(p).isPresent()
                    || BOOKING_INTENT.matcher(SymptomClassifier.normalize(p)).find()) {
                continue;
            }
            kept.add(p);
        }
        return String.join(", ", kept);
    }

    private static String capitalize(String text) {
        String t = text.length() > 120 ? text.substring(0, 120) : text;
        return t.isEmpty() ? t : Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    private static boolean lastAssistantAskedQuestion(List<ChatMessage> history) {
        if (history == null) {
            return false;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage msg = history.get(i);
            if (!"user".equalsIgnoreCase(msg.getRole())) {
                String content = msg.getContent() != null ? msg.getContent().trim() : "";
                int tailStart = Math.max(0, content.length() - 160);
                return content.substring(tailStart).contains("?");
            }
        }
        return false;
    }

    private static String userConversationText(List<ChatMessage> history, String currentMessage) {
        StringBuilder sb = new StringBuilder(currentMessage != null ? currentMessage : "");
        if (history != null) {
            for (ChatMessage msg : history) {
                if ("user".equalsIgnoreCase(msg.getRole()) && msg.getContent() != null) {
                    sb.append('\n').append(msg.getContent());
                }
            }
        }
        return sb.toString();
    }

    private static final Pattern ENGLISH_REFUSAL = Pattern.compile(
            "^(i['’]m sorry|i am sorry|sorry,? i can|i can['’]?t (help|comply|assist))", Pattern.CASE_INSENSITIVE);
    private static final Pattern VIETNAMESE_CHARS = Pattern.compile(
            "[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]",
            Pattern.CASE_INSENSITIVE);

    /**
     * Một số model nhỏ từ chối bằng một câu tiếng Anh cụt dù người dùng hỏi tiếng Việt; thay bằng lời
     * từ chối tiếng Việt lịch sự, có hướng người dùng về phạm vi hỗ trợ của FixLink.
     */
    private String localizeRefusal(String reply, String userMessage) {
        if (reply == null || reply.length() > 200 || !ENGLISH_REFUSAL.matcher(reply.trim()).find()
                || !VIETNAMESE_CHARS.matcher(userMessage).find()) {
            return reply;
        }
        return "Xin lỗi bạn, yêu cầu này nằm ngoài phạm vi hỗ trợ của trợ lý FixLink. Mình có thể giúp bạn "
                + "chọn dịch vụ sửa chữa phù hợp, hướng dẫn đăng yêu cầu, giải thích chính sách cọc – nghiệm thu, "
                + "tra cứu đơn và lịch hẹn, hoặc tạo phiếu hỗ trợ gặp nhân viên CSKH.";
    }

    /**
     * Nếu phiên đã gắn với một tài khoản khác (ví dụ đăng xuất rồi đăng nhập tài khoản khác trên
     * cùng trình duyệt), tách sang khóa phiên riêng để không lộ lịch sử hội thoại của người trước.
     */
    private String scopeSessionId(String sessionId, Long userId) {
        if (sessionId == null || sessionId.isBlank()) {
            return sessionId;
        }
        ChatSession existing = memoryService.getSession(sessionId);
        if (existing != null && existing.getUserId() != null && !existing.getUserId().equals(userId)) {
            return sessionId + "#" + (userId != null ? "u" + userId : "guest");
        }
        return sessionId;
    }

    private String buildDynamicSystemPrompt(ChatCommand command, List<SourceReference> relevantSources,
                                            String ragContext, List<String> scenarioGuidance) {
        StringBuilder sb = new StringBuilder();
        sb.append(properties.usesFullKnowledgeInPrompt() ? baseSystemPrompt : compactSystemPrompt).append("\n\n");

        ZonedDateTime nowVn = ZonedDateTime.now(VN_ZONE);
        String role = command.role() != null ? command.role().toUpperCase(Locale.ROOT) : "GUEST";

        sb.append("=== NGỮ CẢNH PHIÊN HIỆN TẠI ===\n");
        sb.append("- Thời gian hiện tại (giờ Việt Nam): ").append(nowVn.format(PROMPT_TIME_FORMAT)).append("\n");
        sb.append("- Vai trò người dùng: ").append(describeRole(role)).append("\n");
        sb.append("- Trạng thái đăng nhập: ")
                .append(command.userId() != null ? "ĐÃ ĐĂNG NHẬP" : "CHƯA ĐĂNG NHẬP (khách vãng lai)").append("\n");
        sb.append("- Trang đang xem: ").append(command.route() != null ? command.route() : "/")
                .append(" (").append(describeRoute(command.route())).append(")\n");
        sb.append("- ").append(roleGuidance(role)).append("\n\n");

        if (properties.usesFullKnowledgeInPrompt()) {
            String knowledge = ragIndex.getFullKnowledgeText();
            if (knowledge != null && !knowledge.isBlank()) {
                sb.append("=== KHO TRI THỨC CHÍNH THỨC CỦA FIXLINK ===\n");
                sb.append("Đây là nguồn sự thật về chính sách và quy trình. Trả lời dựa trên đây, không suy diễn ra ngoài.\n");
                sb.append("<kho-tri-thuc>\n").append(knowledge).append("\n</kho-tri-thuc>\n\n");
            }
        } else {
            // Nhà cung cấp giới hạn token/phút (Groq miễn phí): chỉ nạp các đoạn tri thức liên quan nhất.
            sb.append("=== TRÍCH ĐOẠN KHO TRI THỨC FIXLINK LIÊN QUAN TỚI CÂU HỎI ===\n");
            sb.append("Đây là nguồn sự thật về chính sách và quy trình. Nếu trích đoạn chưa đủ để trả lời chính xác, ")
                    .append("hãy gọi tool search_knowledge_base với từ khóa cụ thể trước khi trả lời; không suy diễn ra ngoài.\n");
            String excerpt = ragContext != null ? ragContext : "";
            int maxChars = properties.getGroq().getMaxKnowledgeChars();
            if (verifiedAnswers.verifiedAnswerFor(command.message()).isPresent()) {
                maxChars = Math.min(maxChars, 600); // đã có câu trả lời chuẩn bên dưới: chỉ cần trích đoạn ngắn
            }
            if (maxChars > 0 && excerpt.length() > maxChars) {
                excerpt = excerpt.substring(0, maxChars) + "\n...(đã rút gọn)";
            }
            sb.append("<kho-tri-thuc>\n").append(excerpt.isBlank() ? "(Không có trích đoạn phù hợp)" : excerpt)
                    .append("\n</kho-tri-thuc>\n\n");
        }

        if (scenarioGuidance != null && !scenarioGuidance.isEmpty()) {
            sb.append("=== HƯỚNG DẪN XỬ LÝ TÌNH HUỐNG CHO TIN NHẮN NÀY ===\n");
            for (String guidance : scenarioGuidance) {
                sb.append("- ").append(guidance).append("\n");
            }
            sb.append("\n");
        }

        verifiedAnswers.verifiedAnswerFor(command.message()).ifPresent(answer -> {
            sb.append("=== CÂU TRẢ LỜI CHUẨN ĐÃ KIỂM DUYỆT CHO CHỦ ĐỀ NÀY ===\n");
            sb.append("Các dữ kiện dưới đây là chính xác theo quy định FixLink. Dùng đúng các dữ kiện này, ")
                    .append("không nói trái ngược; điều chỉnh câu chữ để trả lời đúng câu hỏi cụ thể của người dùng.\n");
            sb.append("<cau-tra-loi-chuan>\n").append(answer).append("\n</cau-tra-loi-chuan>\n\n");
        });

        if (relevantSources != null && !relevantSources.isEmpty()) {
            sb.append("Mục tri thức có vẻ liên quan nhất tới câu hỏi vừa rồi (gợi ý tra nhanh): ");
            sb.append(String.join("; ", relevantSources.stream()
                    .limit(5)
                    .map(src -> src.getSection() + " [" + src.getTitle() + "]")
                    .toList()));
            sb.append("\n");
        }

        return sb.toString();
    }

    private String describeRole(String role) {
        return switch (role) {
            case "CUSTOMER" -> "CUSTOMER (Khách hàng)";
            case "TECHNICIAN" -> "TECHNICIAN (Kỹ thuật viên / Thợ)";
            case "ADMIN" -> "ADMIN (Quản trị viên)";
            default -> "GUEST (Khách vãng lai, chưa đăng nhập)";
        };
    }

    private String roleGuidance(String role) {
        return switch (role) {
            case "CUSTOMER" -> "Người dùng là KHÁCH HÀNG đã đăng nhập: được dùng tool tra cứu yêu cầu, lịch hẹn, phiếu hỗ trợ của chính họ.";
            case "TECHNICIAN" -> "Người dùng là THỢ đã đăng nhập: được dùng tool kiểm tra hồ sơ KYC, việc đang nhận, việc phù hợp đang mở, lịch hẹn của chính họ.";
            case "ADMIN" -> "Người dùng là QUẢN TRỊ VIÊN: giải đáp quy trình vận hành; chatbot không có tool quản trị, hướng dẫn họ dùng trang /quan-tri.";
            default -> "Người dùng CHƯA ĐĂNG NHẬP: không tra cứu được dữ liệu cá nhân; khi họ hỏi về đơn/lịch hẹn/hồ sơ của mình, hướng dẫn đăng nhập tại /dang-nhap (khách hàng) hoặc /dang-nhap-tho (thợ).";
        };
    }

    private String describeRoute(String route) {
        String r = route != null ? route.toLowerCase(Locale.ROOT) : "/";
        if (r.startsWith("/dang-yeu-cau")) {
            return "trang tạo yêu cầu sửa chữa";
        } else if (r.startsWith("/yeu-cau-cua-toi/")) {
            return "trang chi tiết một yêu cầu của khách, số ở cuối đường dẫn là ID yêu cầu";
        } else if (r.startsWith("/yeu-cau-cua-toi")) {
            return "trang danh sách yêu cầu của khách";
        } else if (r.startsWith("/tho/yeu-cau/")) {
            return "trang chi tiết công việc của thợ, số ở cuối đường dẫn là ID yêu cầu";
        } else if (r.startsWith("/tho/ho-so")) {
            return "trang hồ sơ thợ";
        } else if (r.startsWith("/tho")) {
            return "bảng việc của thợ";
        } else if (r.startsWith("/dang-ky-tho")) {
            return "trang đăng ký làm thợ";
        } else if (r.startsWith("/dang-ky") || r.startsWith("/register")) {
            return "trang đăng ký tài khoản khách hàng";
        } else if (r.startsWith("/dang-nhap") || r.startsWith("/login")) {
            return "trang đăng nhập";
        } else if (r.startsWith("/quen-mat-khau") || r.startsWith("/dat-lai-mat-khau")) {
            return "trang khôi phục mật khẩu";
        } else if (r.startsWith("/quan-tri")) {
            return "trang quản trị";
        } else if (r.startsWith("/khach-hang")) {
            return "trang tổng quan của khách hàng";
        }
        return "trang chủ";
    }

    @Override
    public List<String> getSuggestions(String route, String role) {
        String r = route != null ? route.trim().toLowerCase() : "/";
        String uRole = role != null ? role.toUpperCase() : "GUEST";

        if (r.contains("dang-yeu-cau")) {
            return List.of(
                    "Máy lạnh chảy nước chọn mục nào?",
                    "Cần chuẩn bị ảnh chụp thế nào?",
                    "Bao lâu thì có thợ gửi báo giá?"
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
                        "Cách gửi báo giá cạnh tranh?",
                        "Làm sao để nhận nhiều việc hơn?"
                );
            }
            return List.of(
                    "Đăng ký làm thợ cần giấy tờ gì?",
                    "Quy trình duyệt KYC thợ thế nào?",
                    "Thợ nhận việc qua báo giá thế nào?"
            );
        } else if (r.contains("login") || r.contains("dang-nhap")) {
            return List.of(
                    "Quên mật khẩu phải làm sao?",
                    "Tài khoản bị tạm khóa 15 phút là sao?",
                    "Đăng ký tài khoản mới thế nào?"
            );
        }

        // Default home suggestions
        // Menu lối vào chính, giống chatbot CSKH của các sàn lớn: tư vấn – giá – đặt dịch vụ – tra cứu – gặp nhân viên
        if ("CUSTOMER".equals(uRole)) {
            return List.of(
                    "Tư vấn sửa thiết bị đang hỏng",
                    "Đơn của tôi đang ở trạng thái nào?",
                    "Lịch hẹn sắp tới của tôi",
                    "Gặp nhân viên hỗ trợ"
            );
        }
        return List.of(
                "Tư vấn sửa thiết bị đang hỏng",
                "Bảng giá tham khảo dịch vụ",
                "Tiền cọc và thanh toán thế nào?",
                "FixLink là gì, có uy tín không?"
        );
    }

    @Override
    public void recordFeedback(String sessionId, String messageId, Long userId, String rating, String comment) {
        if (sessionId == null || sessionId.isBlank() || rating == null || rating.isBlank()) {
            return;
        }
        // UP/DOWN: đánh giá từng câu trả lời; CSAT_1..CSAT_5: khảo sát hài lòng cuối cuộc trò chuyện
        if (!rating.toUpperCase(Locale.ROOT).matches("UP|DOWN|CSAT_[1-5]")) {
            throw new DomainException("INVALID_RATING", "Giá trị đánh giá không hợp lệ (UP, DOWN hoặc CSAT_1..CSAT_5)", 400);
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
