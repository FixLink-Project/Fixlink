# BÁO CÁO XÁC MINH VÀ ĐÁNH GIÁ MÔ HÌNH GEMINI (MODEL_REPORT)

**Thời gian kiểm chứng:** 2026-10-17  
**Phương pháp:** Thử nghiệm trực tiếp bằng API thật (`generativelanguage.googleapis.com/v1beta`) qua script `scripts/chatbot/probe-models.js`.  
**Xác thực:** Header `x-goog-api-key` qua biến môi trường `GEMINI_API_KEY` (tuyệt đối không in/ghi key vào mã nguồn hoặc báo cáo).

---

## 1. Bảng Tổng Hợp Khảo Sát Mô Hình

Tổng cộng có **63 models** được trả về từ endpoint `/v1beta/models`. Dưới đây là kết quả kiểm thử chức năng (Tiếng Việt, Tool Calling, Structured JSON Schema, Multi-turn, Đo độ trễ) cho các model ứng viên:

| Model ID | Khả dụng | Function Calling | JSON Schema | Tool + JSON | Độ trễ p50 | HTTP / Quota / Ghi chú | Đánh giá |
|---|---|---|---|---|---|---|---|
| **`gemini-3.8-flash`** | **Có (200)** | **Đạt** (`get_weather`) | **Đạt** (JSON enum) | **Có** | ~11.6s | Model thế hệ mới nhất, suy luận sâu, rất chi tiết. Giới hạn Free Tier: 5 req/min. | **ĐẠT (Ưu tiên 1)** |
| **`gemini-3.7-flash`** | Không (503) | Chưa kiểm tra | Chưa kiểm tra | - | - | Status 503 `UNAVAILABLE`: "This model is currently experiencing high demand. Spikes in demand are usually temporary." | **TẠM THỜI LỖI (Dự phòng)** |
| **`gemini-3.6-flash`** | **Có (200)** | **Đạt** | **Đạt** | **Có** | ~4.5s | Hoạt động rất tốt, phản hồi chuẩn xác. Giới hạn Free Tier: 5 req/min. | **ĐẠT (Ưu tiên 2)** |
| **`gemini-3.5-flash`** | Không (429) | Chưa kiểm tra | Chưa kiểm tra | - | - | Status 429 `RESOURCE_EXHAUSTED` khi gọi dồn dập (chia sẻ quota 5 req/phút của project). | **CHẠM QUOTA** |
| **`gemini-3.5-flash-lite`** | **Có (200)** | **Đạt** | **Đạt** | **Có** | **~1.3s** | **Cực nhanh, ổn định nhất**, không bị nghẽn, trả lời tiếng Việt trôi chảy. | **ĐẠT (Ưu tiên 3 - Lite dự phòng)** |
| **`gemini-3-flash-preview`** | **Có (200)** | **Đạt** | **Đạt** | **Có** | ~3.1s | Model đang dùng trong dự án RAG mẫu, chạy ổn định. | **ĐẠT (Ưu tiên 4)** |
| **`gemini-3.1-flash-lite`** | **Có (200)** | **Đạt** | **Đạt** | **Có** | ~4.9s | Hoạt động tốt, tool calling chuẩn. | **ĐẠT (Ưu tiên 5)** |
| **`gemini-2.5-flash`** | Không (404) | - | - | - | - | Status 404: Bị Google đánh dấu deprecated ("no longer available to new users, please update to gemini-3.8-flash"). | **LOẠI (Đã bị khai tử)** |
| **`gemini-2.5-flash-lite`** | Không (404) | - | - | - | - | Status 404: Bị Google đánh dấu deprecated ("no longer available to new users, please update to gemini-3.5-flash-lite"). | **LOẠI (Đã bị khai tử)** |

---

## 2. Kiểm Tra Mô Hình Embedding

| Model Embedding | HTTP Status | Kích thước Vector | Độ trễ | Trạng thái |
|---|---|---|---|---|
| **`gemini-embedding-001`** | **200 OK** | 3072 | 512ms | ĐẠT |
| **`gemini-embedding-2-preview`**| **200 OK** | 3072 | 514ms | ĐẠT |
| **`gemini-embedding-2`** | **200 OK** | 3072 | 549ms | **ĐẠT (Khuyên dùng)** |

---

## 3. Thiết Kế Chuỗi Failover (Resilience Chain)

Dựa trên kết quả đo đạc thực tế, chuỗi mô hình xử lý hội thoại chatbot được xếp hạng theo tiêu chí: **Mới nhất & thông minh nhất trước → Tốc độ nhanh & ổn định làm dự phòng cuối**:

1. **Model chính (Primary):** `gemini-3.8-flash`  
   - Ưu điểm: Hiểu ngữ cảnh tiếng Việt sâu sắc, xử lý phân loại triệu chứng và gọi tool chính xác cao nhất.
2. **Model kế tiếp (Fallback 1):** `gemini-3.6-flash`  
   - Kích hoạt khi: Model 3.8 gặp lỗi 429/503 hoặc timeout (>20s).
3. **Model tốc độ cao (Fallback 2):** `gemini-3.5-flash-lite`  
   - Ưu điểm: Độ trễ cực thấp (p50 ~ 1.3s), tỉ lệ thành công 100%, cứu vãn trải nghiệm người dùng khi hệ thống đang chịu tải.
4. **Model dự phòng khẩn cấp (Fallback 3):** `gemini-3-flash-preview`  
   - Model ổn định đã được kiểm chứng từ dự án mẫu RAG.
5. **Local Synthesizer (Extractive Fallback):**  
   - Nếu toàn bộ các model trên đám mây đều bị nghẽn (hết quota / mất mạng), engine tự động trích xuất trực tiếp câu trả lời từ tài liệu RAG đã nạp trong bộ nhớ, đảm bảo website không bao giờ bị lỗi 500.

### Cấu hình chuỗi Failover trong `application.yml`
```yaml
chatbot:
  enabled: ${CHATBOT_ENABLED:true}
  gemini:
    api-key: ${GEMINI_API_KEY:}
    base-url: https://generativelanguage.googleapis.com/v1beta
    models:
      - gemini-3.8-flash
      - gemini-3.6-flash
      - gemini-3.5-flash-lite
      - gemini-3-flash-preview
      - gemini-3.1-flash-lite
    timeout-seconds: 20
    max-retries: 1
    cooldown-seconds: 60
    temperature: 0.3
    max-output-tokens: 700
```
*(Cấu hình hoàn toàn qua yaml và biến môi trường, không hard-code trong mã Java).*

---

## 4. Xác Nhận Khả Năng Kết Hợp Tool Calling & Structured Output
- Tất cả các model Flash thế hệ 3 (`3.8-flash`, `3.6-flash`, `3.5-flash-lite`, `3-flash-preview`) **đều hỗ trợ gửi cả Tools và GenerationConfig (JSON Schema)** trong cùng 1 request API.
- Tuy nhiên, để đảm bảo kiến trúc đa lượt an toàn và tương thích với mọi LLM provider:
  - **Vòng lặp Tool Calling (Tối đa 4 vòng):** LLM quyết định gọi tool nào và truyền tham số dạng JSON.
  - **Vòng Chốt Kết Quả (Final Response Synthesis):** Khi LLM không gọi thêm tool nào nữa, kết quả tổng hợp cuối cùng được định dạng theo đúng JSON schema `{reply, suggestions, actions, needsHuman}` để frontend hiển thị mượt mà.
