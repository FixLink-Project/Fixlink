# KẾ HOẠCH TRIỂN KHAI CHI TIẾT (IMPLEMENTATION PLAN)

**Dự án:** Chatbot CSKH & Tư vấn thông minh cho FixLink  
**Kiến trúc:** Hexagonal Architecture + RAG (BM25 + Vector + RRF) + MCP / Tool Calling + Gemini Resilient LLM  
**Nhánh:** `feature/chatbot-cskh`

---

## 1. Sơ Đồ Kiến Trúc Hệ Thống (Hexagonal Architecture)

```
[ Frontend: React Widget (Desktop/Mobile) / Vanilla JS Widget (Static HTML) ]
                                 │
                                 ▼ (REST API: /api/v1/chatbot/...)
┌────────────────────────────────────────────────────────────────────────┐
│                  Adapter In (Web Controllers)                          │
│  - ChatbotController (/messages, /suggestions, /feedback, /sessions)    │
│  - AdminChatbotController (/reindex)                                   │
└────────────────────────────────┬───────────────────────────────────────┘
                                 │
                                 ▼
┌────────────────────────────────────────────────────────────────────────┐
│                  Application Layer (Use Cases)                         │
│  - ChatbotUseCase / ChatbotOrchestratorService                         │
│    + Rate Limiting (IP/User)                                           │
│    + Guardrails & Prompt Injection Defense                             │
│    + Conversation Memory Management (Session / Multi-turn)             │
│    + Function Calling Coordinator (≤ 4 rounds)                         │
│    + Final Structured Output Formatter                                 │
│  - SupportTicketUseCase & FeedbackUseCase                              │
└──────────────────┬───────────────────────────────┬─────────────────────┘
                   │                               │
                   ▼ (Ports Out)                   ▼ (Ports Out)
┌─────────────────────────────────────┐   ┌──────────────────────────────┐
│        RAG & Knowledge Engine       │   │    Tool & MCP Integration    │
│  - KnowledgeBasePort                │   │  - ChatbotToolPort           │
│  - MarkdownChunker (Heading+Overlap)│   │  - NativeToolAdapter (Prod)  │
│  - HybridRetriever (BM25 + Vector)  │   │  - McpClientToolAdapter (Dev)│
│  - RRF Ranker & Window Expander     │   │  - SupportTicketRepository   │
└──────────────────┬──────────────────┘   └──────────────────────────────┘
                   │
                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│           Adapter Out (External AI Service: Gemini Resilient)          │
│  - ResilientGeminiChatModel (Failover: 3.8 → 3.6 → 3.5-lite → 3-prev)  │
│  - Fallback Local Extractive Synthesizer                               │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Kế Hoạch Triển Khai Theo Từng Pha

### Pha 1: Kho Tri Thức & RAG Engine
1. **Tạo tài liệu tri thức chuẩn (`resources/chatbot/knowledge/*.md`):**
   - `gioi-thieu-fixlink.md`: Sứ mệnh, giá trị, mô hình FCFS.
   - `quy-trinh-khach-hang.md`: Đăng việc, chọn giá, thợ nhận, hẹn lịch, nghiệm thu, đánh giá.
   - `quy-trinh-tho.md`: Đăng ký, nộp CCCD KYC, nhận việc, lỗi `JOB_ALREADY_TAKEN`.
   - `tien-coc-va-thanh-toan.md`: Tài khoản trung gian giữ cọc, giải ngân sau nghiệm thu.
   - `trang-thai-yeu-cau.md`: Toàn bộ `RequestStatus` hiện hành (ghi chú deprecated).
   - `tai-khoan-va-bao-mat.md`: Quy định mật khẩu, đổi/quên mật khẩu, khóa tài khoản tạm thời.
   - `anh-va-chung-tu.md`: Tải ảnh hiện trường, định dạng.
   - `xu-ly-su-co-thuong-gap.md`: Các câu hỏi xử lý sự cố thường gặp.
   - `lien-he-va-ho-tro.md`: Hướng dẫn tạo phiếu hỗ trợ ticket (`TCK-xxxx`).
   - Bảng ánh xạ **Triệu chứng → Danh mục DB thật** (Điện lạnh, Điện dân dụng, Ống nước, Gia dụng, Khóa cửa).
2. **Xây dựng RAG Engine:**
   - Markdown Chunking có cấu trúc, lưu metadata (`file`, `section`, `audience`).
   - Hybrid Retrieval (Vector + BM25 kết hợp RRF), mở rộng cửa sổ i±1 (`RerankingContentRetriever`).
   - Lọc theo `audience` của người dùng (guest/customer/technician/admin).
   - Ngưỡng điểm tối thiểu (`min-score` = 0.35); dưới ngưỡng thì bot không dùng tài liệu mà thông báo chưa có thông tin chính thức.

### Pha 2: Tool Calling & Tích Hợp MCP
1. **Cơ sở dữ liệu:**
   - Tạo migration `V12__create_support_tickets.sql` cho bảng `support_tickets`.
   - Tạo migration `V13__create_chatbot_feedback.sql` cho bảng `chatbot_feedback`.
2. **Bộ Tool Nghiệp Vụ (`ChatbotToolPort`):**
   - `list_service_categories`, `list_service_areas`.
   - `suggest_category`: Gợi ý danh mục từ triệu chứng mô tả.
   - `get_my_repair_requests`, `get_repair_request_status`: Truy vấn yêu cầu của chính user từ SecurityContext.
   - `get_my_technician_status`: Kiểm tra hồ sơ KYC và công việc của thợ.
   - `get_my_appointments`: Kiểm tra lịch hẹn của user.
   - `create_support_ticket`: Tạo phiếu hỗ trợ sau khi khách hàng xác nhận.
   - `read_support_ticket`: Tra cứu phiếu hỗ trợ.
   - `search_knowledge_base`: Bot chủ động truy vấn RAG khi cần.
3. **Hai chế độ vận hành:**
   - `native`: Gọi trực tiếp Spring Services (Mặc định).
   - `mcp`: Kết nối stdio tới `MCPSERVER/server.js`, tự động fallback sang `native` nếu lỗi.

### Pha 3: Backend API, Hội Thoại & Bảo Mật
1. **System Prompt (`resources/chatbot/prompts/system.vi.txt`):**
   - Đảm bảo tuân thủ nguyên tắc: Giới thiệu FCFS, cấm bịa đặt, cấm nhắc đấu giá, xưng hô lịch sự, từ chối ngoài phạm vi, kiểm tra an toàn.
2. **Chatbot Orchestrator & Controller:**
   - `POST /api/v1/chatbot/messages` (Hỗ trợ cả guest và authenticated user).
   - `GET /api/v1/chatbot/suggestions?route=...` (Gợi ý chip bấm nhanh theo ngữ cảnh trang).
   - `POST /api/v1/chatbot/feedback` (Ghi nhận đánh giá 👍/👎).
   - `DELETE /api/v1/chatbot/sessions/{id}`.
   - `POST /api/v1/admin/chatbot/reindex`.
3. **Bảo mật & Phòng thủ:**
   - Rate limit theo IP / User.
   - Chống Prompt Injection bằng phân định dữ liệu.
   - Whitelist đường dẫn điều hướng nội bộ (`actions[].url`).
   - Không thực hiện thao tác tài chính hay đổi mật khẩu tự động.

### Pha 4: Frontend Chat Widget
1. **React Component (`frontend/src/components/chatbot/`):**
   - `ChatWidget.tsx`: Nút tròn cố định góc dưới bên phải + Modal chat popup.
   - `ChatWindow.tsx`, `MessageBubble.tsx`, `SuggestionChips.tsx`, `TypingIndicator.tsx`.
   - Render Markdown an toàn, trích dẫn nguồn thu gọn, nút đánh giá và tạo ticket.
   - Lưu session trong `sessionStorage`, tự động dọn dẹp khi đăng xuất.
2. **Vanilla JS Widget cho các trang tĩnh HTML:**
   - `backend/src/main/resources/static/js/chatbot-widget.js`.
   - Tự động mount vào tất cả các trang HTML tĩnh hiện có.

### Pha 5: Kiểm Thử Toàn Diện & Golden QA
1. **Kiểm thử tự động JUnit 5 (CI-safe):**
   - Mockito tests cho Failover, Cooldown, Prompt Guard, Whitelist URL, IDOR check, RAG retriever, Ticket concurrency.
2. **Bộ câu hỏi kiểm thử thực tế (`golden-qa.yaml`):**
   - Tối thiểu 60 câu hỏi phủ khắp 9 nhóm nghiệp vụ (FAQ, gợi ý, cá nhân hóa, ticket, FCFS, tấn công an toàn, đa lượt...).
   - Script chạy kiểm thử tự động `scripts/chatbot/run-qa-suite.js`.
3. **Kiểm thử giao diện:**
   - Chụp ảnh màn hình Desktop (1366x768) và Mobile (390x844).
   - Kiểm tra console, CORS, responsive.

---

## 3. Quản Lý Rủi Ro

| Rủi ro | Mức độ | Biện pháp giảm thiểu |
|---|---|---|
| Hạn mức Gemini Free Tier (5 req/phút) | Cao | Chuỗi 5 model failover, tăng độ trễ nghỉ giữa các lượt test, caching câu hỏi thường gặp, local extractive synthesizer |
| Prompt Injection / Vượt rào bảo mật | Trung bình | Đóng gói dữ liệu trong thẻ phân định `<data>`, chỉ nhận `userId` từ SecurityContext, whitelist URLs |
| Mô hình nhắc tới đấu giá cũ | Thấp | Ràng buộc chặt trong System Prompt và tài liệu tri thức (gắn nhãn cấm) |
| Lỗi Node MCP khi triển khai production | Trung bình | Chế độ `native` là mặc định, tự động chuyển về `native` nếu MCP server chết |
