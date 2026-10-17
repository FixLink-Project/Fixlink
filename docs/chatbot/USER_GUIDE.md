# Hướng Dẫn Sử Dụng & Vận Hành Trợ Lý FixLink AI (User & Developer Guide)

---

## 1. Giới Thiệu Tổng Quan

**Trợ lý FixLink AI** là hệ thống trợ lý ảo thông minh được tích hợp trực tiếp trên nền tảng FixLink (website React 18 frontend và Spring Boot backend theo kiến trúc Hexagonal).

### Tính Năng Nổi Bật:
- **Tư vấn & Chẩn đoán triệu chứng:** Hướng dẫn sơ bộ nguyên nhân hư hỏng đồ điện tử, điện lạnh, đường ống nước, khóa cửa và tự động gợi ý danh mục dịch vụ phù hợp.
- **Tuân thủ quy tắc First-Come-First-Served (FCFS):** Giải thích rõ quy trình nhận việc nhanh nhất cho thợ đầu tiên bấm nhận việc, tuyệt đối không áp dụng đấu giá.
- **RAG Đa tầng (Hybrid RAG):** Kết hợp tìm kiếm từ khóa BM25 và tìm kiếm ngữ nghĩa Vector (Reciprocal Rank Fusion - RRF) với cửa sổ mở rộng ngữ cảnh (Window Retrieval $i \pm 1$).
- **Tool Calling Thông Minh (Native & MCP):** Cho phép tra cứu danh mục dịch vụ, khu vực hoạt động, tình trạng đơn sửa chữa, lịch hẹn thợ và tạo phiếu hỗ trợ sự cố (`TCK-XXXX`).
- **Khả Năng Chống Chịu Cao (Resilience):** Chuỗi failover tự động (`gemini-3.8-flash` $\to$ `gemini-3.6-flash` $\to$ `gemini-3.5-flash-lite` $\to$ `gemini-3-flash-preview` $\to$ `gemini-3.1-flash-lite` $\to$ Extractive Local Synthesizer).
- **Bảo Vệ Đa Lớp:** Chống prompt injection, kiểm tra IDOR phân quyền nghiêm ngặt, lọc liên kết URL theo whitelist nội bộ, rate limiting chống spam.

---

## 2. Hướng Dẫn Dành Cho Khách Hàng (Customer)

### 2.1. Mở Cửa Sổ Hỗ Trợ
- Bấm vào nút tròn xanh **"Hỗ trợ AI"** ở góc dưới cùng bên phải màn hình.
- Phím tắt: Nhấn phím **Escape (Esc)** trên bàn phím để nhanh chóng đóng/mở cửa sổ.

### 2.2. Hỏi Triệu Chứng & Gợi Ý Dịch Vụ
- **Ví dụ câu hỏi:**
  - *"Máy lạnh nhà tôi bật chỉ có gió thổi, không mát thì bị làm sao?"*
  - *"Ống bồn rửa bát bị rò rỉ nước, cần đặt thợ ngành nào?"*
- Trợ lý AI sẽ:
  1. Phân tích nguyên nhân tiềm ẩn (hết gas, hỏng tụ, nghẹt lưới lọc...).
  2. Đưa ra cảnh báo an toàn (ngắt điện, khóa van nước nếu có rò rỉ).
  3. Gợi ý đúng danh mục dịch vụ kèm nút bấm điều hướng nhanh tới trang **[Đăng yêu cầu ngay](/dang-yeu-cau)**.

### 2.3. Tra Cứu Đơn Hàng Của Tôi
- Khách hàng đã đăng nhập có thể hỏi:
  - *"Kiểm tra danh sách các đơn sửa chữa của tôi."*
  - *"Đơn sửa máy giặt của tôi đã có thợ nhận chưa?"*
- Chatbot sẽ gọi an toàn công cụ `get_my_repair_requests` và hiển thị trạng thái chính xác từ cơ sở dữ liệu.

### 2.4. Báo Cáo Sự Cố & Khiếu Nại
- Nếu phát sinh mâu thuẫn hoặc thợ không đến:
  - *"Thợ đòi thêm 500k ngoài giá đã chốt, tôi muốn khiếu nại."*
  - *"Tôi muốn tạo phiếu hỗ trợ khẩn cấp."*
- Chatbot sẽ tiếp nhận thông tin, tạo mã phiếu hỗ trợ dạng **`TCK-XXXX`** và chuyển tới ban quản trị FixLink để giải quyết.

---

## 3. Hướng Dẫn Dành Cho Thợ Kỹ Thuật (Technician)

### 3.1. Tra Cứu Trạng Thái Hồ Sơ & Đánh Giá
- Thợ kỹ thuật có thể hỏi:
  - *"Kiểm tra trạng thái xác minh hồ sơ và số sao đánh giá của tôi."*
  - *"Hôm nay tôi có bao nhiêu lịch hẹn sửa chữa?"*
- Chatbot kiểm tra quyền `TECHNICIAN` từ JWT và hiển thị thông tin hồ sơ, số việc đã làm, điểm đánh giá trung bình.

### 3.2. Báo Cáo Trở Ngại Khi Nhận Việc
- Nếu khách hàng vắng nhà hoặc không liên lạc được:
  - *"Tôi đã đến nhà khách nhưng gọi điện không ai nghe máy, phải làm sao?"*
- Trợ lý AI sẽ hướng dẫn thợ chụp ảnh bằng chứng vị trí, thời gian và tạo phiếu hỗ trợ để ghi nhận không bị trừ điểm uy tín.

---

## 4. Hướng Dẫn Dành Cho Quản Trị Viên (Admin)

### 4.1. Tái Lập Chỉ Mục Tri Thức RAG (Re-indexing)
Khi cập nhật nội dung các file tài liệu Markdown trong `backend/src/main/resources/chatbot/knowledge/`:
- Quản trị viên có thể kích hoạt nạp lại chỉ mục bộ nhớ đệm mà không cần khởi động lại server:
```http
POST /api/v1/admin/chatbot/reindex
Authorization: Bearer <ADMIN_JWT_TOKEN>
```
Phản hồi:
```json
{
  "statusCode": 200,
  "message": "Tái lập chỉ mục RAG thành công",
  "data": {
    "totalIndexedChunks": 42
  }
}
```

### 4.2. Quản Lý Phiếu Hỗ Trợ Khách Hàng
Bảng `support_tickets` lưu trữ toàn bộ các phiếu yêu cầu được chatbot tạo ra:
- `ticket_code`: Mã phiếu định danh dạng `TCK-1001`.
- `user_id`: ID người dùng (nếu đã đăng nhập).
- `guest_name` / `guest_contact`: Tên & SĐT/Email nếu là khách vãng lai.
- `title`, `description`, `priority` (`LOW`, `MEDIUM`, `HIGH`, `URGENT`), `status` (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`).

---

## 5. Cấu Hình Kỹ Thuật & Môi Trường

### 5.1. File Môi Trường (`.env`)
```bash
# Khóa bí mật Gemini API (Bắt buộc)
GEMINI_API_KEY=your_gemini_api_key_here

# Cấu hình chuỗi Failover Model
CHATBOT_GEMINI_MODELS=gemini-3.8-flash,gemini-3.6-flash,gemini-3.5-flash-lite,gemini-3-flash-preview,gemini-3.1-flash-lite

# Chế độ công cụ: native (mặc định) hoặc mcp
CHATBOT_TOOLS_MODE=native

# Giới hạn tần suất gửi tin
CHATBOT_LIMITS_GUEST_RATE_PER_10MIN=20
CHATBOT_LIMITS_MAX_INPUT_CHARS=1000
```

### 5.2. Chạy Cục Bộ (Local Development)
1. **Khởi động Backend:**
   ```bash
   cd chatbot-fixlink/backend
   ./mvnw spring-boot:run
   ```
2. **Khởi động Frontend:**
   ```bash
   cd chatbot-fixlink/frontend
   pnpm dev
   ```

---

## 6. Chính Sách Bảo Mật & An Toàn

- **Ngăn chặn rò rỉ API Key:** Toàn bộ liên lạc với Google Generative Language API diễn ra ở Backend. API Key không bao giờ được gửi xuống trình duyệt khách hàng.
- **Phân quyền IDOR:** Các công cụ tra cứu dữ liệu cá nhân (`get_my_repair_requests`, `get_repair_request_status`, `read_support_ticket`) sử dụng danh tính được giải mã từ JWT `SecurityContext`, tuyệt đối không tin tưởng tham số do người dùng tự nhập trong câu hỏi.
- **Lọc liên kết độc hại (Output Whitelist Guard):** Mọi URL trả về trong câu trả lời của AI đều được đối soát với danh sách trắng các route nội bộ của FixLink, ngăn chặn tấn công Phishing và SSRF.
