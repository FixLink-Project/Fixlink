# Hướng dẫn deploy Trợ lý FixLink (chatbot)

Tài liệu cho nhóm trưởng: gộp code chatbot, cấu hình Render để chatbot chạy ổn định, kiểm tra sau deploy và cách xử lý khi có lỗi.
Đọc kèm `docs/render-deployment-guide.md` (deploy backend + database) và `docs/vercel-workflow-optimization-guide.md` (frontend).

---

## 1. Chatbot hoạt động thế nào (tóm tắt)

Chatbot chạy **bên trong backend `fixlink-api`**, không cần thêm service nào trên Render. Thứ tự trả lời:

1. **Dữ liệu thật trong database**: đơn, báo giá, lịch hẹn, tiền cọc của người đang đăng nhập; số liệu quản trị cho admin.
2. **Câu trả lời chuẩn** cho các câu hỏi thường gặp (giá, quy trình, cọc, thanh toán, khu vực…).
3. **AI Groq** cho câu hỏi tự do, theo chuỗi dự phòng `openai/gpt-oss-120b` → `qwen/qwen3.8-27b`, dùng key 1 rồi key 2.
4. **Bộ trả lời cục bộ** khi Groq hết hạn mức hoặc lỗi.

Nhờ vậy, khi Groq hết hạn mức, chatbot **vẫn trả lời được** các câu phổ biến và các câu về dữ liệu cá nhân.

**Danh mục, gói dịch vụ, giá, thời gian và khu vực** chatbot đọc trực tiếp từ database, làm mới tối đa mỗi 60 giây. Admin sửa giá, thêm hoặc tắt danh mục, mở khu vực mới thì khoảng 1 phút sau chatbot trả lời theo dữ liệu mới, không cần sửa code hay deploy lại.

---

## 2. Gộp code (merge)

- **Nhánh chatbot:** `chatbotAI-fixlink` trên fork `nguyenanhdo0949-create/Fixlink`.
- Nhánh này **đã gộp sẵn `main` mới nhất** của `FixLink-Project/Fixlink` (commit `7d2816d`). Các xung đột đã được giải quyết, đã chạy lại test backend (212/212) và lint, test, build frontend.

**Các bước:**

1. Tạo Pull Request: `nguyenanhdo0949-create/Fixlink:chatbotAI-fixlink` → `FixLink-Project/Fixlink:main`.
2. Nếu `main` có commit mới sau `7d2816d`, GitHub có thể báo xung đột. Giải quyết theo `hexagonal_safe_patching_blueprint.md`, chú ý **không xóa code của nhau**. Những file chatbot sửa nhiều nhất:
   - `backend/src/main/java/com/fixlink/chatbot/**` (toàn bộ phần chatbot)
   - `backend/src/main/resources/chatbot/**` (kiến thức và prompt)
   - `render.yaml` (biến môi trường chatbot)
3. Chờ CI xanh rồi mới merge.

> Chatbot **không sửa** logic nghiệp vụ của nhóm (đặt đơn, báo giá, thanh toán, ví…). Chatbot chỉ **đọc** dữ liệu qua các use case có sẵn.

---

## 3. Cấu hình Render cho service `fixlink-api`

Vào **Render Dashboard → `fixlink-api` → Environment**, thêm hoặc kiểm tra các biến sau:

| Biến | Giá trị | Bắt buộc |
|---|---|---|
| `CHATBOT_ENABLED` | `true` | Có. Đặt `false` sẽ tắt hẳn chatbot. |
| `CHATBOT_PROVIDER` | `groq` | Có |
| `GROQ_API_KEY` | key Groq thứ nhất (bắt đầu bằng `gsk_`) | Có |
| `GROQ_API_KEY_2` | key Groq của **một tài khoản Groq khác** | Không bắt buộc, nhưng nên có (gấp đôi hạn mức mỗi ngày) |

**Không đặt** `LITELLM_BASE_URL` và `LITELLM_MASTER_KEY` trên Render. Để trống thì backend tự gọi thẳng Groq với cùng chuỗi dự phòng như trên.

> **Vì sao không deploy cổng LiteLLM lên Render?** Web service gói free của Render **không nhận được kết nối nội bộ** từ service khác (xem [tài liệu Render](https://render.com/docs/private-network)). Nó còn ngủ sau 15 phút không dùng, nên câu chat đầu tiên sẽ phải chờ rất lâu. Thư mục `litellm/` chỉ dùng khi chạy local bằng Docker, hoặc khi nhóm nâng lên gói trả phí.

### Lấy key Groq

1. Đăng nhập https://console.groq.com/keys bằng một tài khoản Google, chọn **Create API Key** và sao chép key.
2. Muốn có key thứ hai thì làm lại với **một tài khoản Google khác**, rồi dán vào `GROQ_API_KEY_2`.
3. Gói free của Groq: khoảng **200.000 token mỗi ngày cho mỗi model trên mỗi key**. Hai key và hai model là đủ cho buổi chấm.

### Bảo mật key

- **Không** ghi key vào code, `render.yaml`, README hay tin nhắn nhóm công khai. Key chỉ nằm trên Render Dashboard (và file `.env` local, file này đã có trong `.gitignore`).
- Nếu lỡ để lộ key: vào console.groq.com/keys, **xóa key đó**, tạo key mới rồi dán lại trên Render.

### Nếu deploy bằng Blueprint (`render.yaml`)

`render.yaml` đã khai báo sẵn `CHATBOT_ENABLED`, `CHATBOT_PROVIDER`, `GROQ_API_KEY`, `GROQ_API_KEY_2`. Hai biến key có `sync: false`, nên Render sẽ hỏi giá trị khi tạo; bạn dán key vào là xong.

Sau khi đổi biến môi trường, chọn **Manual Deploy → Deploy latest commit**, hoặc chờ Render tự khởi động lại.

---

## 4. Frontend (Vercel)

Chatbot dùng chung API với frontend, không cần biến riêng:

- `VITE_API_URL` trên Vercel trỏ đúng địa chỉ backend Render.
- `CORS_ALLOWED_ORIGINS` trên Render có domain Vercel (hiện là `https://fixlink.vercel.app`). Nếu domain Vercel khác thì phải thêm vào biến này, nếu không chatbot và cả trang web sẽ báo lỗi mạng.

---

## 5. Kiểm tra sau khi deploy (khoảng 5 phút)

1. **Backend đã chạy:** mở `https://<backend>.onrender.com/api/v1/health`, phải trả về OK.
2. **Log khởi động:** trong Render Logs **không** có `APPLICATION FAILED`.
3. Mở web, bấm biểu tượng chatbot và hỏi lần lượt:

| Tài khoản | Câu hỏi | Kết quả đúng |
|---|---|---|
| Chưa đăng nhập | `fixlink có bao nhiêu loại dịch vụ` | Liệt kê đúng số danh mục đang bật trong database |
| Chưa đăng nhập | `vệ sinh máy lạnh bao nhiêu tiền` | Đúng giá đang lưu trong database |
| Chưa đăng nhập | `fixlink hoạt động ở đâu` | Đúng các khu vực đang bật |
| Chưa đăng nhập | `đơn của tôi thế nào` | Nhắc đăng nhập |
| Khách hàng | `tôi có bao nhiêu yêu cầu` | Đúng số đơn của tài khoản |
| Khách hàng | `máy giặt nhà tôi kêu to khi vắt, tôi nên làm gì trước khi gọi thợ` | Câu trả lời tự nhiên do AI viết; nếu AI hết hạn mức thì là câu trả lời chuẩn |
| Thợ | `số dư ví của tôi` | Đúng số dư Ví FixLink |
| Admin | `hiện có bao nhiêu thợ đang hoạt động` | Đúng số liệu trong trang quản trị |

4. **Kiểm tra dữ liệu luôn mới:** vào trang quản trị, đổi giá một gói, chờ 1 phút rồi hỏi lại giá gói đó. Chatbot phải trả lời giá mới.

---

## 6. Lưu ý gói free trước buổi chấm

- **Render ngủ sau 15 phút không có truy cập.** Lần mở đầu tiên sau đó mất khoảng 1 phút để khởi động. **Mở web 2–3 phút trước buổi chấm** và hỏi chatbot 1 câu cho chạy sẵn.
- **Hạn mức Groq tính theo ngày.** Hôm chấm hạn chế spam chatbot để test. Nếu hết hạn mức, chatbot vẫn trả lời câu phổ biến và câu về dữ liệu cá nhân; chỉ câu hỏi tự do, dài thì trả lời ngắn gọn hơn.
- **Trang chủ và database đang lệch:** trang chủ hiện 6 danh mục (thêm Điện tử, Máy tính) trong khi database có 4. Chatbot nói theo **database**. Muốn chatbot tư vấn thêm Điện tử hoặc Máy tính thì chỉ cần **thêm danh mục và gói dịch vụ vào database**; chatbot tự nhận ra trong vòng 1 phút. Nếu không thì sửa trang chủ cho khớp.

---

## 7. Xử lý sự cố

| Hiện tượng | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| Không thấy nút chatbot hoặc chatbot báo đang tắt | `CHATBOT_ENABLED` khác `true` | Đặt `true` rồi deploy lại |
| Chatbot trả lời được câu phổ biến nhưng câu tự do luôn ngắn hoặc "chưa hiểu" | Thiếu key, key sai hoặc Groq hết hạn mức | Xem Logs. Có `401` thì key sai, dán lại. Có `429` thì hết hạn mức, thêm `GROQ_API_KEY_2` hoặc chờ sang ngày |
| Logs có `litellm ... ConnectException` | Đang đặt `LITELLM_BASE_URL` | Xóa biến `LITELLM_BASE_URL` trên Render |
| Chatbot báo lỗi mạng trên web | CORS hoặc `VITE_API_URL` sai | Kiểm tra mục 4 |
| Lần chat đầu rất chậm | Render đang ngủ (gói free) | Bình thường; mở web trước khi dùng |
| Chatbot nói giá hoặc danh mục cũ | Dữ liệu vừa đổi chưa đủ 60 giây | Chờ 1 phút rồi hỏi lại |
| Logs có `chưa đọc được dữ liệu dịch vụ từ database` | Database tạm lỗi hoặc khởi động chậm | Chatbot vẫn dùng dữ liệu gần nhất; kiểm tra kết nối `fixlink-db` |

---

## 8. Chạy chatbot ở máy local (cho thành viên nhóm)

1. Tạo file `.env` ở thư mục gốc dự án (không commit), chứa `GROQ_API_KEY=...` và tùy chọn `GROQ_API_KEY_2=...`.
2. Backend **không tự đọc** file `.env`. Cách nạp: trong Git Bash ở thư mục gốc chạy `set -a; . ./.env; set +a`, rồi `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`. Nếu chạy bằng IntelliJ thì khai báo các biến trong mục Environment variables. Sau đó chạy frontend `npm run dev`.
3. Tùy chọn, có Docker: chạy cổng LiteLLM trong thư mục `litellm/` (xem `litellm/README.md`) và đặt `LITELLM_BASE_URL=http://localhost:4000` cùng `LITELLM_MASTER_KEY` trong `.env`. Không có LiteLLM thì backend vẫn gọi thẳng Groq.
