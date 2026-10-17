# DANH SÁCH QUYẾT ĐỊNH KỸ THUẬT (ARCHITECTURAL DECISIONS)

Tài liệu này ghi lại các quyết định thiết kế và kỹ thuật quan trọng trong quá trình phát triển Chatbot CSKH & Tư vấn FixLink.

---

### ADR-001: Quản Lý Khóa API và Xác Thực Gemini
- **Bối cảnh:** Cần gọi Gemini API an toàn, tuân thủ nguyên tắc không lộ API key.
- **Quyết định:** Sử dụng header `x-goog-api-key` gọi endpoint `https://generativelanguage.googleapis.com/v1beta`. Đọc khóa qua biến môi trường `GEMINI_API_KEY` (hoặc `.env` đã được ignore bởi Git). Tuyệt đối không log, không in ra màn hình hoặc trả về client.
- **Hệ quả:** An toàn tuyệt đối, tương thích cả môi trường phát triển local, Docker và CI/CD.

---

### ADR-002: Chuỗi Mô Hình Failover và Khả Năng Tự Phục Hồi (Model Resiliency)
- **Bối cảnh:** Gemini Free Tier có giới hạn tần suất (5 req/phút) và một số model có thể bị quá tải (503 UNAVAILABLE như `gemini-3.7-flash`) hoặc bị Google khai tử (404 NOT_FOUND như dòng `gemini-2.5`).
- **Quyết định:**
  - Thiết lập chuỗi failover ưu tiên: `gemini-3.8-flash` (chính) → `gemini-3.6-flash` → `gemini-3.5-flash-lite` (nhanh nhất, p50 ~ 1.3s) → `gemini-3-flash-preview` → `gemini-3.1-flash-lite`.
  - Cơ chế cooldown: khi model gặp 429 hoặc 503, đánh dấu khóa tạm thời (60s - 300s) và chuyển ngay sang model kế tiếp trong chuỗi.
  - Tích hợp Extractive Fallback: nếu toàn bộ model đám mây thất bại, hệ thống tự trích xuất thông tin từ tài liệu RAG trong bộ nhớ thay vì trả lỗi 500.
- **Hệ quả:** Trải nghiệm người dùng không bị gián đoạn, hệ thống có tính chịu lỗi cao.

---

### ADR-003: Chiến Lược Embedding Kép (Dual-Tier Embedding)
- **Bối cảnh:** Cần RAG index chạy nhanh, độc lập khi chạy test CI không có Internet, nhưng đạt độ chính xác ngữ nghĩa cao khi chạy live.
- **Quyết định:** Cung cấp 2 cơ chế embedding:
  1. `GeminiEmbeddingClient` (`gemini-embedding-2` / `gemini-embedding-001`) khi có API key.
  2. `DeterministicFastEmbeddingModel` (in-memory) khi chạy unit test / integration test CI hoặc offline.
- **Hệ quả:** CI tests luôn xanh 100% không cần key hay mạng ngoài; môi trường live tận dụng tối đa sức mạnh mô hình Gemini 3072 chiều.

---

### ADR-004: Kiến Trúc Đa Chế Độ Cho Tools (Native Mode & MCP Mode)
- **Bối cảnh:** Yêu cầu đồ án cần chứng minh khả năng tích hợp giao thức MCP (Model Context Protocol), nhưng production cần chạy gọn nhẹ không phụ thuộc Node.js.
- **Quyết định:** Thiết kế interface `ChatbotToolPort` (Hexagonal Architecture):
  - `native` (**Mặc định**): Gọi trực tiếp các UseCase nội bộ trong Spring Boot.
  - `mcp`: Sử dụng Java Stdio MCP Client giao tiếp với `MCPSERVER/server.js`.
  - Tự động fallback về `native` nếu tiến trình MCP gặp sự cố.
- **Hệ quả:** Đạt trọn vẹn điểm yêu cầu môn học về MCP, đồng thời đảm bảo tính sẵn sàng cao cho môi trường triển khai thực tế.

---

### ADR-005: Lưu Trữ Phiếu Hỗ Trợ (Support Tickets) & Đánh Giá Phản Hồi (Feedback)
- **Bối cảnh:** Production cần lưu trữ ticket và feedback an toàn, có khả năng đồng thời và truy vấn theo người dùng.
- **Quyết định:**
  - Tạo bảng `support_tickets` qua Flyway `V12__create_support_tickets.sql` với mã định danh tự tăng dạng `TCK-XXXX`, kèm chỉ mục chống race condition.
  - Tạo bảng `chatbot_feedback` qua Flyway `V13__create_chatbot_feedback.sql`.
- **Hệ quả:** Dữ liệu nhất quán trong cơ sở dữ liệu PostgreSQL/H2 của FixLink, độc lập với file JSON tạm của MCP server demo.

---

### ADR-006: Bảo Mật Dữ Liệu & Danh Tính Người Dùng
- **Bối cảnh:** Phòng chống các lỗ hổng IDOR, Prompt Injection, rò rỉ dữ liệu tài chính/mật khẩu.
- **Quyết định:**
  - Định danh người dùng (`userId`, `role`) **chỉ** được lấy từ `SecurityContext` do server kiểm tra từ JWT, tuyệt đối không nhận tham số từ prompt hay LLM sinh ra.
  - Toàn bộ dữ liệu RAG và kết quả từ tool được bọc trong thẻ XML `<ngu-canh-tri-thuc>` và `<ket-qua-cong-cu>` để cách ly prompt injection.
  - Chatbot chỉ được phép tạo ticket hỗ trợ và tư vấn link nội bộ (whitelist); cấm thực hiện thao tác tài chính, đặt cọc hay nghiệm thu thay người dùng.
