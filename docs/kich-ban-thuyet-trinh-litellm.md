# Kịch bản thuyết trình và demo: LiteLLM trong Trợ lý FixLink

Đi kèm slide `docs/LiteLLM-FixLink.pptx` (14 slide). Tổng thời gian khoảng **15 phút**: thuyết trình khoảng 9 phút, demo khoảng 5 phút, còn lại để hỏi đáp.

Ký hiệu:
- 🗣️ là lời nói. Không cần đọc thuộc, nói theo ý là được.
- 👉 là thao tác: bấm, chỉ tay vào slide, mở cửa sổ.

---

# PHẦN 1 – KỊCH BẢN THUYẾT TRÌNH THEO SLIDE

## Slide 1 – Tiêu đề (30 giây)

🗣️ *"Em xin trình bày phần LiteLLM trong Trợ lý FixLink. Trợ lý FixLink là chatbot chăm sóc khách hàng của hệ thống, dùng AI để trả lời. Phần của em là làm sao cho chatbot **luôn trả lời được**, kể cả khi model AI hết lượt dùng hoặc gặp lỗi. Công cụ em dùng là LiteLLM."*

👉 Chỉ vào hình bên phải: *"Ý tưởng chính: backend không gọi thẳng model AI mà gọi qua LiteLLM ở giữa. LiteLLM tự chọn model phù hợp."*

## Slide 2 – 4 vấn đề khi gọi thẳng AI (1 phút)

🗣️ *"Trước khi có LiteLLM, nếu backend gọi thẳng nhà cung cấp AI thì gặp 4 vấn đề."*

👉 Lần lượt chỉ 4 thẻ:
1. *"Thứ nhất, quan trọng nhất: **hết hạn mức thì chatbot ngừng trả lời**. Nhóm dùng gói miễn phí của Groq, mỗi model chỉ được khoảng 200.000 token một ngày, riêng gpt-oss-120b chỉ 8.000 token một phút. Hỏi dồn vài câu là chạm giới hạn."*
2. *"Thứ hai, mỗi nhà cung cấp có kiểu API khác nhau, **đổi model là phải sửa code**, build và deploy lại."*
3. *"Thứ ba, muốn dùng nhiều key để tăng hạn mức thì phải tự viết code chia tải."*
4. *"Thứ tư, **không biết câu trả lời do model nào viết**, mất bao lâu, tốn bao nhiêu."*

## Slide 3 – LiteLLM là gì (1 phút)

🗣️ *"LiteLLM là phần mềm mã nguồn mở, đóng vai trò **cổng trung gian**, còn gọi là AI Gateway, giữa ứng dụng và các model AI."*

👉 Chỉ sơ đồ bên phải từ trên xuống:
*"Ứng dụng chỉ gọi **một API duy nhất theo chuẩn OpenAI**, là `POST /chat/completions` với tên model `fixlink-chat`. LiteLLM nhận yêu cầu rồi tự định tuyến sang nhà cung cấp thật. Nó hỗ trợ hơn 100 nhà cung cấp: Groq, OpenAI, Gemini, Claude, Ollama… FixLink hiện dùng Groq."*

👉 Chỉ ô ví dụ màu xám: *"Có thể hình dung như **tổng đài chuyển máy**: backend chỉ gọi một số, tổng đài tự nối tới máy đang rảnh, máy này bận thì chuyển máy khác."*

## Slide 4 – Công dụng: vì sao FixLink cần (1 phút)

🗣️ *"Giá trị của LiteLLM với FixLink gói trong 3 con số."*

👉 Chỉ lần lượt:
- *"**1**: một API chuẩn OpenAI cho mọi model. Backend Java chỉ có một cách gọi AI."*
- *"**2 × 2**: 2 model nhân 2 key Groq. LiteLLM chia tải giữa 2 key; model chính hết hạn mức thì tự chuyển sang model dự phòng."*
- *"**0**: không phải sửa dòng Java nào khi đổi model. Muốn thêm Gemini hay đổi thứ tự dự phòng, chỉ sửa file cấu hình."*

🗣️ Chỉ ô tối bên dưới: *"Tóm lại: chatbot **ổn định hơn, dễ bảo trì hơn và minh bạch hơn**."*

## Slide 5 – Các tính năng (45 giây)

🗣️ *"Đây là các tính năng chính của LiteLLM. Cột cuối là những gì nhóm đã bật: API thống nhất, tên chung cho nhóm model, cân bằng tải, dự phòng, thử lại, tạm nghỉ model lỗi, bảo vệ bằng master key, không ghi nội dung hội thoại của khách vào log, và theo dõi model, chi phí qua header."*

🗣️ *"Dòng cuối là virtual key, ngân sách, trang quản trị. Những tính năng này cần thêm database riêng cho LiteLLM, ở quy mô đồ án chưa cần nên nhóm chưa bật."*

## Slide 6 – LiteLLM trong luồng trả lời của FixLink ⭐ (1 phút 30 giây, slide quan trọng nhất)

🗣️ *"Đây là slide quan trọng nhất: LiteLLM nằm ở đâu trong hệ thống."*

👉 Đi theo mũi tên, từ trái sang phải rồi xuống dưới:
1. *"Người dùng hỏi trên giao diện React. Câu hỏi vào backend Spring Boot, tới `ChatbotOrchestratorService`."*
2. 👉 Chỉ ô viền xanh lá: *"Câu hỏi về **dữ liệu cá nhân** (đơn của tôi, ví, thông báo) và câu hỏi **quy định** (giá, cọc) được trả lời ngay từ database và câu trả lời chuẩn, **không tốn lượt AI**, nên luôn chính xác."*
3. *"Chỉ **câu hỏi tự do** mới gọi AI, qua `GroqChatAdapter`. Đây là adapter của cổng `LlmChatPort` theo kiến trúc hexagonal của nhóm."*
4. 👉 Chỉ ô LiteLLM màu teal: *"**Lớp 1**: có biến `LITELLM_BASE_URL` thì đi qua LiteLLM ở cổng 4000. LiteLLM gọi gpt-oss-120b, chia tải giữa key 1 và key 2. Gặp lỗi 429 hay lỗi khác thì tự chuyển sang qwen3.8-27b."*
5. 👉 Chỉ đường nét đứt: *"**Lớp 2**: nếu chính LiteLLM không phản hồi, backend tự gọi thẳng Groq với cùng chuỗi model."*
6. *"**Lớp 3**: nếu mọi AI đều lỗi, bộ trả lời cục bộ từ kho tri thức vẫn trả lời. Chatbot **không bao giờ ngừng trả lời**."*

👉 Chỉ ô tối dưới cùng: *"Trên giao diện local, dưới mỗi câu trả lời có nhãn cho biết đường đi, ví dụ 'LiteLLM → gpt-oss-120b'. Lát nữa em sẽ demo."*

## Slide 7 – Code 1: khai báo model (1 phút)

🗣️ *"Em đi vào code. Toàn bộ cấu hình LiteLLM nằm trong `litellm/config.yaml`."*

👉 Chỉ số trên code, nói theo số:
- *"**(1)** `model_name: fixlink-chat` là tên chung, backend chỉ biết tên này."*
- *"**(2)** `groq/openai/gpt-oss-120b` là model thật. Tiền tố `groq/` cho LiteLLM biết phải gọi Groq."*
- *"**(3)** Key đọc từ biến môi trường bằng `os.environ/…`, **không ghi thẳng vào file**, nên không lộ key lên GitHub."*
- *"**(4)** Cùng tên `fixlink-chat` khai báo 2 lần với 2 key khác nhau, LiteLLM hiểu là 2 bản của cùng một model và **chia tải**, gấp đôi hạn mức."*
- *"**(5)** `fixlink-groq-qwen` là nhóm dự phòng, model khác và có hạn mức riêng."*

## Slide 8 – Code 2: định tuyến, dự phòng, bảo mật (1 phút)

🗣️ *"Phần thứ hai của file cấu hình quyết định LiteLLM xử lý lỗi thế nào."*
- *"**(1)** `fallbacks`: `fixlink-chat` lỗi thì tự gọi `fixlink-groq-qwen`."*
- *"**(2)** `allowed_fails: 1` và `cooldown_time: 15`: model lỗi 1 lần thì tạm nghỉ 15 giây, các lượt sau đi thẳng model khác cho nhanh."*
- *"**(3)** `drop_params`: model dự phòng không hiểu tham số nào thì tự bỏ đi, không báo lỗi."*
- *"**(4)** `num_retries`, `request_timeout`: thử lại 1 lần, quá 25 giây thì dừng."*
- *"**(5)** Không ghi nội dung hội thoại của khách vào log, để bảo vệ dữ liệu."*
- *"**(6)** `master_key`: backend phải gửi đúng khóa này, thiếu là bị chặn."*

🗣️ *"Điểm hay là **toàn bộ chiến lược dự phòng nằm trong cấu hình**, không nằm trong code."*

## Slide 9 – Code 3: chạy LiteLLM bằng Docker (45 giây)

🗣️ *"LiteLLM chạy bằng Docker với image chính thức, nhóm không phải tự viết proxy."*
- *"**(2)** File cấu hình được gắn vào container, sửa xong chỉ cần khởi động lại container, không build lại."*
- *"**(3)** Key lấy từ file `.env`; file này nằm trong `.gitignore`."*
- *"**(4)** Healthcheck: Docker hiện trạng thái 'healthy' khi LiteLLM sẵn sàng."*
- *"**(5)** Script `entrypoint.sh`: không có key thứ hai thì dùng lại key thứ nhất, nên cấu hình không bị lỗi."*

## Slide 10 – Code 4: backend chọn đường gọi AI (1 phút)

🗣️ *"Phía backend Java, `application.yml` có cấu hình `litellm.base-url`."*

👉 Chỉ **(1)**: *"**Bật hay tắt LiteLLM chỉ bằng một biến môi trường.** Có giá trị thì đi qua LiteLLM, để trống thì gọi thẳng Groq. Không phải sửa code."*

👉 Chỉ hàm `endpoints()`: *"Hàm này dựng danh sách các đường gọi AI theo thứ tự ưu tiên: **(2)** LiteLLM trước, **(3)** rồi Groq bằng key 1, **(4)** rồi Groq bằng key 2. Thứ tự trong danh sách chính là thứ tự thử. Vì LiteLLM nói chuẩn OpenAI nên dùng chung một adapter với Groq, chỉ khác địa chỉ và khóa."*

## Slide 11 – Code 5: thử lần lượt, tạm nghỉ, ghi model thật (1 phút)

🗣️ *"Đây là vòng lặp gọi AI."*
- *"**(1)(2)** Duyệt từng đường gọi, trong mỗi đường duyệt từng model, bỏ qua model đang tạm nghỉ."*
- *"**(3)** Gọi lỗi thì `applyCooldown`, thời gian nghỉ tùy loại lỗi: 429 thì chờ theo thời gian Groq báo, 404 nghỉ 1 giờ, lỗi máy chủ nghỉ 60 giây."*
- *"**(4)** Hết mọi đường thì trả lời từ kho tri thức, chatbot không bao giờ 'chết'."*
- *"**(5)** Khi gọi qua LiteLLM, backend đọc header `x-litellm-model-name` để biết **model thật** đã trả lời, kể cả khi LiteLLM đã chuyển dự phòng. Giá trị này hiện thành nhãn trên giao diện."*

## Slide 12 – Kiểm chứng (45 giây, có thể chuyển sang demo luôn)

🗣️ *"Để kiểm chứng, nhóm viết script `demo-litellm.ps1` gồm 6 bước. Bên phải là kết quả chạy thật. Gọi bình thường thì model thật là gpt-oss-120b, `attempted-fallbacks` bằng 0. Khi model chính hỏng thật, LiteLLM tự chuyển sang qwen, `attempted-fallbacks` bằng 1, người dùng vẫn nhận câu trả lời. Em xin demo trực tiếp."*

👉 **Chuyển sang PHẦN 2 – DEMO.** Demo xong quay lại slide 13.

## Slide 13 – Local và bản deploy (1 phút)

🗣️ *"Thầy có thể hỏi: vì sao bản deploy không chạy LiteLLM?"*

👉 Chỉ cột phải: *"Bản deploy dùng Render gói miễn phí. Web service miễn phí của Render **không nhận được kết nối nội bộ** từ service khác, nên backend không gọi được LiteLLM. Service miễn phí còn **ngủ sau 15 phút**, LiteLLM khởi động lại mất 1–2 phút, câu chat đầu sẽ phải chờ rất lâu."*

🗣️ *"Vì backend đã có sẵn cùng chuỗi dự phòng, bản deploy gọi thẳng Groq mà vẫn ổn định."*

👉 Chỉ ô tối: *"Và nhờ thiết kế bật/tắt bằng biến `LITELLM_BASE_URL`, khi lên gói trả phí chỉ cần đặt biến này là chạy LiteLLM, không sửa code."*

## Slide 14 – Kết luận (30 giây)

🗣️ *"Tóm lại, LiteLLM giúp Trợ lý FixLink **ổn định** (2 model nhân 2 key, dự phòng nhiều lớp), **dễ mở rộng** (thêm Gemini hay Claude chỉ bằng file cấu hình) và **minh bạch** (biết model thật, thời gian, chi phí từng câu). Em xin cảm ơn thầy, em sẵn sàng trả lời câu hỏi."*

---

# PHẦN 2 – KỊCH BẢN DEMO

## A. Chuẩn bị (làm trước giờ demo 15 phút)

| # | Việc | Cách làm | Dấu hiệu đúng |
|---|---|---|---|
| 1 | Mở Docker Desktop | Mở app, chờ khoảng 1 phút | Góc dưới hiện *Engine running* |
| 2 | Chạy LiteLLM | PowerShell ở thư mục gốc dự án: `docker compose up -d litellm` | Sau khoảng 30–40 giây, container `fixlink_litellm` hiện **healthy** |
| 3 | Chạy backend | Cửa sổ PowerShell khác: `backend\start-local.cmd` (tự nạp `.env`) | Log có dòng `Started FixlinkApplication` (khoảng 1–2 phút) |
| 4 | Chạy frontend | Cửa sổ khác: `cd frontend` rồi `npm run dev` | Hiện `Local: http://localhost:5173` |
| 5 | Mở sẵn | Trình duyệt mở http://localhost:5173; mở sẵn một PowerShell ở thư mục gốc để chạy script; mở sẵn `litellm/config.yaml` trong VS Code | |
| 6 | Kiểm tra nhanh | Hỏi chatbot 1 câu bất kỳ | Có câu trả lời |

**Lưu ý:**
- Docker báo lỗi khi khởi động thì **không bấm "Reset to factory defaults"** (mất hết dữ liệu Docker). Tắt hẳn Docker Desktop rồi mở lại.
- Tài khoản mẫu: `customer01` (khách), `tho_dien_lanh_01` (thợ), `admin`. Mật khẩu xem trong `backend/.../infrastructure/config/DataInitializer.java`.
- Trước demo **không hỏi chatbot hàng loạt**, để giữ hạn mức Groq.

## B. Demo 1 – Script tính năng LiteLLM (khoảng 2 phút 30 giây)

👉 Ở cửa sổ PowerShell đã mở sẵn, gõ:

```
powershell -ExecutionPolicy Bypass -File litellm\demo-litellm.ps1
```

Mỗi bước script dừng lại, nói xong thì bấm **Enter**:

| Bước | Màn hình hiện | 🗣️ Nói |
|---|---|---|
| 1 | `HTTP 200: "I'm alive!"` | *"LiteLLM đang chạy ở cổng 4000."* |
| 2 | `Bị chặn đúng như mong đợi: HTTP 401` | *"Gọi mà không có master key thì bị chặn. Người ngoài không dùng ké được key Groq của nhóm."* |
| 3 | `fixlink-chat`, `fixlink-groq-qwen`, `fixlink-demo-loi` | *"Đây là các nhóm model. Bên ngoài chỉ thấy tên chung, không thấy model thật hay key."* |
| 4 | `Model thật đã trả lời: groq/openai/gpt-oss-120b`, `attempted-fallbacks: 0`, thời gian, chi phí | *"Gọi bình thường: backend chỉ gửi tên 'fixlink-chat', LiteLLM chọn gpt-oss-120b. Header còn cho biết thời gian và chi phí ước tính của câu này."* |
| 5 | `Model thật: groq/qwen/qwen3.8-27b`, `attempted-fallbacks: 1` | *"Nhóm 'fixlink-demo-loi' cố tình trỏ tới một model không tồn tại, tức model chính **hỏng thật**. LiteLLM nhận lỗi rồi **tự chuyển** sang qwen, `attempted-fallbacks` bằng 1, người dùng vẫn có câu trả lời. Backend không phải sửa dòng code nào."* |
| 6 | Phần cấu hình định tuyến | 👉 Chuyển sang VS Code chỉ `config.yaml`: *"Toàn bộ hành vi vừa rồi đến từ mấy dòng cấu hình này."* |

> Nhóm `fixlink-demo-loi` **chỉ dùng cho demo**, backend không bao giờ gọi nhóm này. Nếu thầy hỏi thì nói rõ như vậy.

## C. Demo 2 – Chatbot trên web đi qua LiteLLM (khoảng 2 phút)

👉 Mở http://localhost:5173, bấm nút **"Hỗ trợ AI"** góc phải dưới.

**Câu 1: câu hỏi tự do (đi qua AI)**
- Gõ: `máy giặt nhà tôi vắt rung mạnh và kêu to, tôi nên kiểm tra gì trước khi gọi thợ?`
- Mong đợi: câu trả lời hướng dẫn kiểm tra (tắt nguồn, cân bằng máy, đồ giặt bị vón…), nhãn dưới câu trả lời ghi **"Trợ lý AI · LiteLLM → gpt-oss-120b"** hoặc **"Trợ lý AI · LiteLLM → qwen3.8-27b"** (khi gpt-oss-120b vừa chạm giới hạn token mỗi phút).
- 🗣️ *"Câu hỏi tự do nên đi qua AI. Nhãn cho thấy câu này đi qua LiteLLM và tên model thật đã trả lời."*
- Muốn câu nào cũng đi qua AI thì hỏi kiểu **xin tư vấn, giải thích**: *"…nên kiểm tra gì"*, *"tại sao…"*, *"nên A hay B"*. Câu hỏi **chọn dịch vụ, giá, quy định, dữ liệu của tôi** (ví dụ *"máy lạnh rỉ nước nên chọn dịch vụ nào"*) được trả lời bằng **câu trả lời chuẩn** từ database. Đây là thiết kế có chủ đích để giá và gói luôn đúng, không phải lỗi.

**Câu 2: câu hỏi quy định (không tốn AI)**
- Gõ: `vệ sinh máy lạnh bao nhiêu tiền`
- Mong đợi: giá lấy từ database, nhãn **"Câu trả lời chuẩn"**.
- 🗣️ *"Câu hỏi giá được trả lời ngay từ database, chính xác và không tốn lượt AI. Admin đổi giá thì khoảng 1 phút sau chatbot nói giá mới."*

**Câu 3 (tùy chọn): dữ liệu cá nhân**
- Đăng nhập `customer01`, mở chatbot, gõ: `tôi có bao nhiêu yêu cầu` hoặc `tôi có thông báo gì mới không`
- Mong đợi: số liệu thật của tài khoản, nhãn **"Dữ liệu FixLink"**.

**Câu 4 (tùy chọn, ấn tượng nhất): tắt LiteLLM ngay trước mặt thầy**
1. Ở PowerShell gõ: `docker compose stop litellm`
2. Hỏi chatbot: `tủ lạnh nhà tôi chạy nhưng ngăn mát không lạnh, tại sao vậy?`
3. Mong đợi: vẫn có câu trả lời, nhãn đổi thành **"Trợ lý AI · Groq trực tiếp → …"**.
4. 🗣️ *"LiteLLM vừa bị tắt nhưng chatbot vẫn trả lời: backend thấy LiteLLM không phản hồi nên tự gọi thẳng Groq. Đây là lớp dự phòng thứ hai."*
5. **Nhớ bật lại**: `docker compose start litellm`

## D. Tình huống bất ngờ và cách xử lý

| Tình huống | Nói gì / làm gì |
|---|---|
| Hỏi mà nhãn ghi **"Câu trả lời chuẩn"** thay vì "Trợ lý AI" | Câu đó là câu hỏi chọn dịch vụ, giá hoặc quy định nên trả lời từ database (đúng thiết kế). 🗣️ *"Câu hỏi về gói và giá luôn trả lời từ database để không sai giá; chỉ câu xin tư vấn mới gọi AI."* Rồi hỏi một câu tư vấn trong mục Câu 1. |
| Nhãn ghi **"LiteLLM → qwen3.8-27b"** thay vì gpt-oss-120b | 🗣️ *"gpt-oss-120b vừa chạm giới hạn 8.000 token mỗi phút, LiteLLM tự chuyển sang qwen. Đây chính là cơ chế dự phòng đang chạy thật."* (Tình huống này là điểm cộng, cứ chỉ cho thầy xem.) |
| Nhãn ghi **"Chế độ dự phòng"** | Groq hết hạn mức trong ngày. 🗣️ *"Mọi model AI đều hết lượt nên chatbot dùng lớp 3, trả lời từ kho tri thức, vẫn không bị treo."* Rồi dùng kết quả chạy thật trên **slide 12** để minh họa. |
| Script báo lỗi ở bước 1 (không kết nối được) | LiteLLM chưa chạy: gõ `docker compose up -d litellm`, chờ 30 giây rồi chạy lại script. |
| Câu trả lời chậm (hơn 10 giây) | Bình thường với lần gọi đầu hoặc khi đang chuyển dự phòng. Nói về luồng dự phòng trong lúc chờ. |
| Backend không khởi động vì cổng 8080 đang bị chiếm | Đóng cửa sổ backend cũ, hoặc khởi động lại máy trước buổi demo. |
| Docker không lên | Bỏ Demo 1, chỉ demo Demo 2. Backend tự gọi thẳng Groq (nhãn "Groq trực tiếp"); giải thích LiteLLM bằng slide 6–12. |

## E. Câu hỏi thầy có thể hỏi (trả lời ngắn)

- **Không có LiteLLM thì chatbot có chạy không?** Có. Backend tự gọi thẳng Groq với cùng chuỗi model và key, cuối cùng là bộ trả lời cục bộ. LiteLLM giúp quản lý tập trung và đổi model không cần sửa code.
- **Sao không tự viết code chuyển model trong backend luôn?** Nhóm có viết, đó là lớp dự phòng 2. Nhưng code trong backend phải sửa, build, deploy lại mỗi lần đổi model. LiteLLM đổi bằng cấu hình, có sẵn chia tải, cooldown, đo chi phí, và nhiều service khác dùng chung được.
- **Key có bị lộ không?** Không. Key nằm trong `.env` (có trong `.gitignore`) và biến môi trường trên server. Frontend không chứa key. Backend gọi LiteLLM bằng master key riêng.
- **Muốn thêm Gemini làm dự phòng thì sao?** Thêm một mục vào `model_list` (key đọc bằng `os.environ/GEMINI_API_KEY`), thêm tên đó vào `fallbacks`, rồi khởi động lại container.
- **Chatbot có bịa thông tin không?** Dữ liệu cá nhân và câu hỏi quy định không đi qua AI mà lấy từ database và câu trả lời chuẩn. Prompt cấm AI bịa hotline, bảo hành, tỷ lệ hoàn cọc; có thêm bộ kiểm tra chặn số liệu không có thật.
- **Theo dõi chi phí thế nào?** Header `x-litellm-response-cost` cho từng câu (script demo bước 4 có hiện). Gói Groq miễn phí nên chi phí thực tế bằng 0, đây là chi phí ước tính nếu trả phí.
