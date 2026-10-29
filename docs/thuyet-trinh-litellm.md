# Thuyết trình: LiteLLM tích hợp vào Trợ lý FixLink

Tài liệu trả lời 4 câu thầy hỏi: **LiteLLM là gì, vì sao cần, có những tính năng gì, áp dụng vào FixLink thế nào**, kèm các bước demo ở máy local và các câu hỏi thầy có thể hỏi thêm.

---

## 1. LLM và LiteLLM là gì

**LLM (Large Language Model – mô hình ngôn ngữ lớn)** là mô hình AI được huấn luyện trên lượng văn bản rất lớn, có thể hiểu câu hỏi tiếng Việt và viết câu trả lời tự nhiên. Ví dụ: GPT, Gemini, Claude, Llama, Qwen. FixLink dùng LLM chạy trên **Groq** (dịch vụ chạy model rất nhanh, có gói miễn phí), cụ thể là `openai/gpt-oss-120b` và `qwen/qwen3.8-27b`.

**LiteLLM** là phần mềm mã nguồn mở đóng vai trò **cổng trung gian (AI Gateway / proxy)** giữa ứng dụng và các nhà cung cấp LLM:

- Ứng dụng chỉ gọi **một API duy nhất theo chuẩn OpenAI** (`POST /chat/completions`) tới LiteLLM.
- LiteLLM quyết định gọi nhà cung cấp nào, model nào, key nào. Gặp lỗi thì tự thử lại hoặc chuyển sang model khác.
- LiteLLM hỗ trợ hơn 100 nhà cung cấp (OpenAI, Gemini, Claude, Groq, Mistral, Ollama…) qua cùng một cách gọi.

Ví dụ dễ hiểu: LiteLLM giống **tổng đài chuyển máy**. Backend chỉ gọi một số "fixlink-chat"; tổng đài tự chuyển tới máy đang rảnh, máy này bận thì chuyển máy khác.

---

## 2. Vì sao cần LiteLLM

| Vấn đề khi gọi thẳng nhà cung cấp | LiteLLM giải quyết |
|---|---|
| Gói miễn phí giới hạn lượt dùng mỗi ngày, hết là chatbot không trả lời được | **Chia tải** giữa nhiều key và **tự chuyển model dự phòng** khi hết hạn mức (lỗi 429) |
| Mỗi nhà cung cấp có API, tham số khác nhau, đổi nhà cung cấp phải sửa code | **Một API chuẩn OpenAI** cho mọi nhà cung cấp; đổi model chỉ sửa file cấu hình |
| Key AI nằm rải rác trong backend | Key chỉ nằm ở LiteLLM; backend dùng **một master key** riêng |
| Không biết câu nào do model nào trả lời, tốn bao nhiêu | Header trả về **model thật, số lần chuyển dự phòng, thời gian, chi phí ước tính** |
| Model lỗi liên tục vẫn bị gọi, làm chậm cả hệ thống | **Cooldown**: model vừa lỗi được tạm nghỉ, các lượt sau đi thẳng sang model khác |

Tóm lại: LiteLLM giúp chatbot **ổn định hơn** (không chết khi một model hỏng), **dễ bảo trì hơn** (đổi model không sửa code) và **an toàn hơn** (gom key về một chỗ).

---

## 3. Các tính năng chính của LiteLLM

| Tính năng | Ý nghĩa | FixLink có dùng |
|---|---|---|
| API thống nhất chuẩn OpenAI | Gọi mọi nhà cung cấp bằng một định dạng | ✅ |
| Đặt tên chung cho model (model group) | Backend gọi `fixlink-chat`, không cần biết model thật | ✅ |
| Cân bằng tải (load balancing) | Nhiều key/deployment cho cùng một model, LiteLLM chia đều | ✅ 2 key Groq |
| Dự phòng (fallbacks) | Model chính lỗi thì chuyển model khác | ✅ gpt-oss-120b → qwen3.8-27b |
| Thử lại (retries), giới hạn thời gian (timeout) | Lỗi tạm thời thì thử lại; quá lâu thì dừng | ✅ `num_retries: 1`, `request_timeout: 25` |
| Tạm nghỉ model lỗi (cooldown) | Tránh gọi lại model đang lỗi | ✅ `allowed_fails: 1`, `cooldown_time: 15` |
| Bỏ tham số không hỗ trợ (`drop_params`) | Model dự phòng không hiểu tham số nào thì tự bỏ, không báo lỗi | ✅ |
| Bảo vệ bằng master key | Không có khóa thì bị chặn (401) | ✅ |
| Không ghi nội dung hội thoại vào log | Bảo vệ dữ liệu khách | ✅ `turn_off_message_logging` |
| Theo dõi chi phí qua header | Biết mỗi câu tốn bao nhiêu | ✅ có sẵn trong header |
| Virtual key, ngân sách, giới hạn theo người dùng, trang quản trị | Cấp key riêng cho từng nhóm, đặt hạn mức tiền | ❌ cần thêm database cho LiteLLM, chưa cần ở quy mô đồ án |
| Bộ nhớ đệm câu trả lời (Redis cache) | Câu giống nhau không gọi lại model | ❌ backend đã có bộ đệm câu hỏi thường gặp riêng |
| Ghi log ra Langfuse, Prometheus… | Theo dõi chất lượng AI | ❌ hướng mở rộng |

---

## 4. Áp dụng vào FixLink

### 4.1 Sơ đồ luồng một câu hỏi

```
Người dùng hỏi trên web (React)
        │
        ▼
Backend Spring Boot ─ ChatbotOrchestratorService
   1. Câu tính toán, câu chào ...............→ trả lời ngay
   2. Hỏi dữ liệu cá nhân (đơn, ví, thông báo) → đọc database, trả lời "Dữ liệu FixLink"
   3. Câu hỏi quy định đã có câu chuẩn ........→ trả lời "Câu trả lời chuẩn"
   4. Câu hỏi tự do ...........................→ gọi AI qua GroqChatAdapter
        │
        ▼  (có LITELLM_BASE_URL)
   LiteLLM :4000  model "fixlink-chat"
        ├─► Groq gpt-oss-120b  (key 1 / key 2, chia tải)
        └─► hết hạn mức / lỗi ─► Groq qwen3.8-27b (key 1 / key 2)
        │
        ▼  LiteLLM không chạy hoặc tất cả đều lỗi
   Backend gọi thẳng Groq (cùng chuỗi model, key 1 rồi key 2)
        │
        ▼  vẫn lỗi
   Bộ trả lời cục bộ từ kho tri thức ("Chế độ dự phòng") ─ chatbot không bao giờ "chết"
```

### 4.2 Những file liên quan

| File | Vai trò |
|---|---|
| `litellm/config.yaml` | Khai báo model, key (đọc từ biến môi trường), chuỗi dự phòng, retry, cooldown, master key |
| `litellm/Dockerfile`, `litellm/entrypoint.sh` | Đóng gói LiteLLM; không có key 2 thì dùng lại key 1 |
| `docker-compose.yml` (service `litellm`) | Chạy LiteLLM ở cổng 4000 |
| `backend/.../chatbot/adapter/out/groq/GroqChatAdapter.java` | Adapter gọi LLM theo chuẩn OpenAI: thử LiteLLM trước, rồi Groq key 1, key 2; ghi lại model thật từ header `x-litellm-model-name` |
| `backend/.../chatbot/infrastructure/config/ChatbotProperties.java` | Cấu hình `chatbot.litellm.base-url`, `api-key`, `models`, `timeout-seconds` |
| `backend/src/main/resources/application.yml` | `LITELLM_BASE_URL` để trống thì backend gọi thẳng Groq |
| `frontend/.../chatbot/MessageBubble.tsx` | Ở môi trường local, nhãn dưới câu trả lời ghi `LiteLLM → tên model` |

### 4.3 Vì sao hợp với kiến trúc hexagonal của nhóm

- Phần lõi chatbot (`application`) chỉ biết **cổng ra** `LlmChatPort`, không biết Groq hay LiteLLM.
- `GroqChatAdapter` là **adapter** cắm vào cổng đó. LiteLLM nói chuẩn OpenAI nên dùng chung adapter, chỉ khác địa chỉ.
- Muốn đổi sang Gemini, Claude hay model chạy nội bộ (Ollama): chỉ sửa `litellm/config.yaml`, **không sửa code Java**.

### 4.4 Vì sao bản deploy không chạy LiteLLM

Bản deploy trên Render gói free **gọi thẳng Groq**, vì:
1. Web service gói free của Render **không nhận được kết nối nội bộ** từ service khác, nên backend không gọi được LiteLLM.
2. Service free **ngủ sau 15 phút**. LiteLLM khởi động lại mất 1–2 phút, câu chat đầu sẽ phải chờ rất lâu.
3. Backend đã tự có cùng chuỗi dự phòng (2 model × 2 key → câu trả lời cục bộ), nên chatbot vẫn trả lời ổn định.

Hệ thống được thiết kế **bật/tắt LiteLLM bằng một biến môi trường** (`LITELLM_BASE_URL`), không sửa code. Lên gói trả phí thì bật lại được ngay.

---

## 5. Các bước demo ở máy local

### Chuẩn bị (trước giờ demo khoảng 10 phút)

1. Mở **Docker Desktop**, chờ trạng thái *Engine running*. Nếu Docker báo lỗi khi khởi động, **không bấm "Reset to factory defaults"** (sẽ mất hết dữ liệu Docker).
2. Mở PowerShell ở thư mục gốc dự án:
   ```
   docker compose up -d litellm
   ```
   Chờ khoảng 30 giây, trong Docker Desktop container `fixlink_litellm` chuyển sang **healthy**.
3. Chạy backend (tự nạp `.env`): mở cửa sổ khác, chạy `backend\start-local.cmd`.
4. Chạy frontend: mở cửa sổ khác, `cd frontend` rồi `npm run dev`, mở http://localhost:5173.

### Demo 1 – Tính năng LiteLLM (khoảng 3 phút)

```
powershell -ExecutionPolicy Bypass -File litellm\demo-litellm.ps1
```

Mỗi bước bấm Enter, vừa chạy vừa giải thích:

| Bước | Thầy sẽ thấy | Nói gì |
|---|---|---|
| 1 | `HTTP 200: "I'm alive!"` | LiteLLM chạy ở cổng 4000 |
| 2 | `HTTP 401` | Không có master key thì không gọi được, key Groq không bị lộ |
| 3 | `fixlink-chat`, `fixlink-groq-qwen` | Backend chỉ biết tên chung, không biết model và key thật |
| 4 | `Model thật: groq/openai/gpt-oss-120b`, `attempted-fallbacks: 0` | Gọi bình thường, kèm thời gian và chi phí ước tính |
| 5 | `Model thật: groq/qwen/qwen3.8-27b`, `attempted-fallbacks: 1` | Model chính hỏng thật, LiteLLM tự chuyển sang dự phòng, người dùng vẫn có câu trả lời |
| 6 | Cấu hình `config.yaml` | Toàn bộ chuỗi dự phòng nằm trong file cấu hình, không nằm trong code |

### Demo 2 – Chatbot trên web đi qua LiteLLM (khoảng 3 phút)

Mở chatbot ở http://localhost:5173. **Nhãn dưới mỗi câu trả lời** cho biết nguồn:

| Câu hỏi gợi ý | Nhãn mong đợi |
|---|---|
| `máy giặt nhà tôi vắt rung mạnh và kêu to, tôi nên kiểm tra gì trước khi gọi thợ?` | **Trợ lý AI · LiteLLM → gpt-oss-120b** |
| `so sánh giúp tôi nên vệ sinh máy lạnh hay nạp ga khi máy chạy mà không mát` | **Trợ lý AI · LiteLLM → …** |
| `vệ sinh máy lạnh bao nhiêu tiền` | **Câu trả lời chuẩn** (không tốn lượt AI) |
| Đăng nhập tài khoản khách rồi hỏi `tôi có bao nhiêu yêu cầu` | **Dữ liệu FixLink** (đọc database) |

Ý nói khi demo: *"Câu hỏi quy định và dữ liệu cá nhân được trả lời trực tiếp, chính xác và không tốn hạn mức. Chỉ câu hỏi tự do mới gọi AI, và câu đó đi qua LiteLLM."*

**Demo tự chuyển sang dự phòng ngay trên web (tùy chọn):** dừng LiteLLM bằng `docker compose stop litellm` rồi hỏi lại một câu tự do. Nhãn đổi thành **Groq trực tiếp → …**: backend thấy LiteLLM không phản hồi nên gọi thẳng Groq. Bật lại bằng `docker compose start litellm`.

### Lưu ý hạn mức khi demo
- Groq miễn phí khoảng **200.000 token/ngày cho mỗi model trên mỗi key**. Một câu hỏi tự do tốn khoảng 4.000–6.000 token.
- Ngoài hạn mức ngày, `gpt-oss-120b` còn giới hạn **8.000 token mỗi phút**. Hai câu AI hỏi liền nhau thì câu sau thường được **qwen3.8-27b** trả lời (nhãn ghi `LiteLLM → qwen3.8-27b`). Đây chính là cơ chế dự phòng đang chạy thật, nên khi gặp thì **chỉ cho thầy xem luôn**. Muốn câu nào cũng do gpt-oss-120b trả lời thì hỏi cách nhau khoảng 40 giây.
- Hôm demo **không test hàng loạt**. Script demo LiteLLM chỉ tốn khoảng 2 lượt gọi rất ngắn.

---

## 6. Câu hỏi thầy có thể hỏi

**Không có LiteLLM thì chatbot có chạy không?**
Có. Backend tự gọi thẳng Groq với cùng chuỗi model và key, cuối cùng là bộ trả lời cục bộ. LiteLLM là lớp giúp quản lý tập trung và đổi model không cần sửa code.

**LiteLLM khác gì so với tự viết code chuyển model trong backend?**
Code trong backend chỉ phục vụ FixLink và phải sửa, build, deploy lại mỗi lần đổi model. LiteLLM là cổng dùng chung: đổi nhà cung cấp, thêm key, đổi thứ tự dự phòng chỉ bằng file cấu hình. Nó còn có sẵn chi phí, cooldown, bảo mật bằng master key, và nhiều service khác dùng chung được.

**Làm sao biết câu trả lời do model nào viết?**
LiteLLM trả header `x-litellm-model-name`. Backend đọc header này, ghi vào kết quả (`litellm/groq/openai/gpt-oss-120b`), và giao diện local hiện thành nhãn dưới câu trả lời.

**Key Groq có bị lộ không?**
Không. Key chỉ nằm trong file `.env` (đã có trong `.gitignore`) ở máy, và trong biến môi trường trên server. Frontend không chứa key. Backend gọi LiteLLM bằng master key riêng; LiteLLM mới giữ key Groq.

**Nếu muốn dùng thêm Gemini hoặc ChatGPT thì làm sao?**
Thêm một mục trong `model_list` của `litellm/config.yaml` (key đọc bằng `os.environ/TEN_BIEN`), rồi thêm tên đó vào danh sách `fallbacks`. Không sửa code Java.

**AI có bịa thông tin không?**
Chatbot được giới hạn: dữ liệu cá nhân và câu hỏi quy định không đi qua AI mà lấy từ database và câu trả lời chuẩn. Danh mục, giá, khu vực đọc từ database và làm mới mỗi 60 giây. Prompt cấm AI bịa hotline, bảo hành, % hoàn cọc…; có thêm bộ kiểm tra để chặn câu trả lời có số liệu không có thật.

**Ghi vào CV thế nào?**
*"Tích hợp AI Gateway LiteLLM cho chatbot CSKH (Spring Boot, kiến trúc hexagonal): API thống nhất chuẩn OpenAI, cân bằng tải nhiều API key, tự chuyển model dự phòng khi hết hạn mức, cooldown, theo dõi model và chi phí theo từng câu trả lời; chatbot vẫn hoạt động khi toàn bộ nhà cung cấp AI lỗi nhờ lớp trả lời cục bộ từ database và kho tri thức."*
