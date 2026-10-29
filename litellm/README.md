# Cổng LiteLLM cho Trợ lý FixLink

LiteLLM là một proxy tương thích OpenAI đứng giữa backend và các nhà cung cấp LLM.
Backend chỉ gọi một model tên `fixlink-chat`. LiteLLM tự chuyển sang model dự phòng khi model
chính hết hạn mức (HTTP 429), quá tải hoặc lỗi.

```
Backend (Spring Boot)
   │  POST /chat/completions  model=fixlink-chat
   ▼
LiteLLM :4000 ──► Groq gpt-oss-120b          (chính)
              └─► Groq qwen3.8-27b           (dự phòng, hạn mức riêng)

LiteLLM không chạy / tất cả đều lỗi ──► backend gọi thẳng Groq ──► bộ trả lời cục bộ (kho tri thức)
```

Câu trả lời ghi lại model đã dùng (`modelUsed`), ví dụ `litellm/groq/qwen/qwen3.8-27b`, để biết
lượt đó do nhà cung cấp nào trả lời.

## Chạy ở máy (localhost)

1. Điền vào file `.env` ở thư mục gốc (file này **không** được commit):
   ```
   GROQ_API_KEY=gsk_...            # https://console.groq.com/keys
   LITELLM_MASTER_KEY=sk-fixlink-<chuỗi ngẫu nhiên>
   LITELLM_BASE_URL=http://localhost:4000
   GROQ_API_KEY_2=gsk_...          # (tùy chọn) key của một tài khoản Groq khác
   ```
2. Bật Docker Desktop, rồi chạy `litellm\start-litellm.cmd` (hoặc `docker compose up -d litellm`).
3. Chạy backend như bình thường (`run-project.bat` hoặc `backend\start-local.cmd`); backend đọc `.env`.

Kiểm tra nhanh LiteLLM:

```bash
curl http://localhost:4000/health/liveliness
```

## Demo các tính năng LiteLLM

Khi LiteLLM đang chạy, mở PowerShell ở thư mục gốc dự án:

```
powershell -ExecutionPolicy Bypass -File litellm\demo-litellm.ps1
```

Script đi qua 6 bước, mỗi bước chờ bấm Enter:
1. Kiểm tra LiteLLM đang chạy (`/health/liveliness`).
2. Gọi không có master key thì bị chặn (HTTP 401).
3. Liệt kê các nhóm model (`/v1/models`): backend chỉ thấy tên chung, không thấy key Groq.
4. Gọi bình thường: header `x-litellm-model-name` cho biết model thật, kèm thời gian và chi phí ước tính.
5. Gọi nhóm `fixlink-demo-loi` (model chính cố tình trỏ tới model không tồn tại): LiteLLM tự chuyển sang
   `fixlink-groq-qwen`, header `x-litellm-attempted-fallbacks: 1`.
6. In phần cấu hình định tuyến trong `config.yaml`.

Script tốn khoảng 2 lượt gọi Groq rất ngắn. Trên giao diện chatbot (môi trường local), nhãn dưới mỗi câu
trả lời AI ghi `Trợ lý AI · LiteLLM → tên model`.

## Deploy (Render)

Bản deploy **không chạy LiteLLM**: web service gói free của Render không nhận được kết nối nội bộ và ngủ
sau 15 phút không dùng, nên câu chat đầu tiên sẽ phải chờ rất lâu. Backend gọi thẳng Groq với cùng chuỗi
dự phòng (gpt-oss-120b → qwen3.8-27b, key 1 rồi key 2, cuối cùng là câu trả lời cục bộ). Trên Render chỉ
đặt `GROQ_API_KEY` (và `GROQ_API_KEY_2`), **không** đặt `LITELLM_BASE_URL`. Chi tiết xem
`docs/huong-dan-deploy-chatbot.md`.

Khi lên gói trả phí, có thể chạy LiteLLM thành service riêng rồi đặt `LITELLM_BASE_URL` cho backend,
không phải sửa code.

Frontend trên Vercel **không** cần và **không được** chứa các key này.

## Hạn mức miễn phí của Groq (quan trọng khi demo)

Mỗi model Groq miễn phí giới hạn khoảng **200.000 token/ngày** cho mỗi tài khoản. Một câu hỏi tốn
khoảng 4.000–5.000 token (gọi thêm tool thì nhiều hơn), nên 2 model trả lời được khoảng **80 câu AI/ngày**
cho mỗi key. Câu hỏi về quy định có sẵn câu trả lời chuẩn và câu hỏi về đơn/lịch hẹn của người đã đăng nhập
được trả lời trực tiếp từ hệ thống, **không tốn hạn mức AI**.

- Thêm `GROQ_API_KEY_2` (tạo từ một tài khoản Google khác tại console.groq.com): LiteLLM chia tải
  giữa 2 key, **gấp đôi** hạn mức. Bỏ trống thì key 2 dùng lại key 1.
- Server deploy nên dùng key riêng, không dùng chung key với máy cá nhân đang test.
- Tránh test hàng loạt ngay trước buổi demo để không tiêu hết hạn mức trong ngày.
- Hết hạn mức thì chatbot tự chuyển sang trả lời cục bộ từ kho tri thức (vẫn đúng quy định, tra
  được đơn/lịch hẹn thật cho người đã đăng nhập), không bị treo.

## Thêm hoặc đổi nhà cung cấp

Sửa `litellm/config.yaml`: thêm một mục trong `model_list` rồi thêm tên đó vào danh sách
`fallbacks`. Key luôn đọc từ biến môi trường bằng cú pháp `os.environ/TEN_BIEN`.
