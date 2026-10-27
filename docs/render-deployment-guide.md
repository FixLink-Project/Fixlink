# FixLink - Hướng Dẫn Vận Hành & Triển Khai Render.com (Render Operations & Troubleshooting Guide)

Tài liệu này cung cấp chi tiết quy trình chuẩn (SOP) để triển khai hệ thống **FixLink Backend (Spring Boot 3.3 + Java 21)** và **PostgreSQL** lên nền tảng **Render.com**, kèm giải pháp tối ưu hóa bộ nhớ cho gói 512MB RAM, xử lý xung đột Port và sổ tay khắc phục mọi lỗi thường gặp khi đưa sản phẩm lên môi trường cloud.

---

## 1. Kiến Trúc Hạ Tầng Triển Khai Trên Render

```
               [ Khách hàng / Kỹ thuật viên ]
                             │
                             ▼
             [ Frontend: Vercel (Production) ]
                             │
                    HTTPS API Request
                             │
                             ▼
    ┌─────────────────────────────────────────────────────────┐
    │  Render.com Cloud Environment                           │
    │                                                         │
    │   ┌─────────────────────────────────────────────────┐   │
    │   │  Web Service: fixlink-api (Docker Runtime)      │   │
    │   │  - Java 21 JRE Alpine / Spring Boot 3.3         │   │
    │   │  - Max RAM: 512MB (JVM tuned: Max Heap 256MB)   │   │
    │   │  - Dynamic Port: ${PORT:-10000}                 │   │
    │   │  - Healthcheck: /api/v1/health                  │   │
    │   └───────────────────────┬─────────────────────────┘   │
    │                           │                             │
    │            Internal Network (Private SSL)               │
    │                           │                             │
    │   ┌───────────────────────▼─────────────────────────┐   │
    │   │  Managed Database: fixlink-db (PostgreSQL 16)   │   │
    │   │  - Auto Flyway Migration (V1 -> V16)            │   │
    │   │  - Connection Pool: HikariCP (max-size: 5)      │   │
    │   └─────────────────────────────────────────────────┘   │
    └─────────────────────────────────────────────────────────┘
```

---

## 2. Quy Trình Triển Khai Chuẩn 4 Bước (Step-by-Step SOP)

### Bước 1: Khởi tạo PostgreSQL Database trên Render
1. Vào **Render Dashboard** $\rightarrow$ Chọn **New +** $\rightarrow$ **PostgreSQL**.
2. Điền thông tin:
   - **Name**: `fixlink-db`
   - **Database**: `fixlink_db`
   - **User**: `fixlink_user`
   - **Region**: Singapore (ưu tiên gần Việt Nam để giảm độ trễ)
   - **Plan**: Free (hoặc Starter)
3. Sau khi tạo xong, cuộn xuống mục **Connections**:
   - Sao chép **Internal Database URL** (Dạng: `postgres://fixlink_user:...@dpg-xxxx-a/fixlink_db`).
   - Sao chép các thông số riêng: `Host`, `Database`, `Username`, `Password`.

### Bước 2: Khởi tạo Web Service cho Backend
1. Trên Render Dashboard $\rightarrow$ Chọn **New +** $\rightarrow$ **Web Service**.
2. Kết nối với GitHub Repository: `FixLink-Project/Fixlink`.
3. Cấu hình dịch vụ:
   - **Name**: `fixlink-api`
   - **Region**: Singapore (cùng vùng với Database)
   - **Branch**: `main`
   - **Runtime**: `Docker`
   - **Dockerfile Path**: `./backend/Dockerfile`
   - **Docker Context**: `./backend`
   - **Plan**: Free (0.1 CPU, 512 MB RAM)

### Bước 3: Cấu hình Biến Môi Trường Bắt Buộc (Environment Variables)
Tại tab **Environment** của Web Service, cấu hình chính xác các khóa sau:

| Tên biến (Key) | Giá trị mẫu / Hướng dẫn | Mục đích |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `prod` | Kích hoạt file `application-prod.yml` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<Internal_Host>:5432/fixlink_db?sslmode=require` | Chuẩn JDBC kết nối DB Render |
| `SPRING_DATASOURCE_USERNAME`| `fixlink_user` | User của Database Render |
| `SPRING_DATASOURCE_PASSWORD`| `<Mật_khẩu_DB_Render>` | Password của Database Render |
| `JWT_SECRET` | Chuỗi bí mật 64 ký tự (HS512) | Ký và xác thực token JWT |
| `CORS_ALLOWED_ORIGINS` | `https://fixlink.vercel.app,http://localhost:5173` | Cho phép Frontend gọi API |
| `FRONTEND_URL` | `https://fixlink.vercel.app` | Link đính kèm email reset mật khẩu |
| `CHATBOT_ENABLED` | `false` *(bật `true` nếu có key)* | Tránh lỗi khi chưa cấu hình AI |
| `GROQ_API_KEY` / `GEMINI_API_KEY` | `<API_KEY_của_bạn>` | Cung cấp cho trợ lý ảo FixLink AI |

> [!WARNING]
> Không dùng biến `DATABASE_URL` dạng thô (`postgres://...`) trực tiếp nếu mật khẩu có ký tự đặc biệt (`@`, `#`, `$`), hãy ưu tiên khai báo trực tiếp 3 biến `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.

### Bước 4: Thiết lập Cơ chế Chống "Ngủ Đông" (Keep-Alive Cron)
Gói Render Free sẽ tự động tắt container (sleep) sau 15 phút không nhận request. Để ứng dụng luôn thức và sẵn sàng demo:
1. Đăng ký tài khoản miễn phí tại **[cron-job.org](https://cron-job.org)** hoặc **UptimeRobot**.
2. Tạo 1 tác vụ cron:
   - **URL**: `https://fixlink-api.onrender.com/api/v1/health`
   - **Chu kỳ**: Mỗi 10 phút một lần (Every 10 minutes).
   - **Phương thức**: `GET`.
3. Kết quả: Container backend luôn ở trạng thái nóng (warm), phản hồi ngay lập tức cho khách hàng và hội đồng chấm thi.

---

## 3. Cấu Hình Tối Ưu Hóa Hạ Tầng (Infrastructure Tuning)

### 3.1. Dockerfile Tối Ưu Cho RAM 512MB (backend/Dockerfile)

Để Spring Boot 3.3 và Java 21 chạy mượt mà trong giới hạn 512MB RAM mà không bị Render kill (Exit 137):

```dockerfile
# =============================================================================
# Stage 1: Build JAR with Maven & OpenJDK 21
# =============================================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline dependency:resolve-plugins

COPY checkstyle.xml ./
COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

# =============================================================================
# Stage 2: Runtime Image with Eclipse Temurin JRE 21 Alpine
# =============================================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S fixlink && adduser -S fixlink -G fixlink
RUN apk add --no-cache wget

COPY --from=builder /build/target/fixlink-backend-0.0.1-SNAPSHOT.jar /app/app.jar
RUN chown -R fixlink:fixlink /app

USER fixlink

# EXPOSE port linh hoạt theo biến PORT của Render
EXPOSE 8080

# Healthcheck động theo port môi trường
HEALTHCHECK --interval=30s --timeout=5s --start-period=50s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:${PORT:-8080}/api/v1/health || exit 1

# Cờ tối ưu JVM: Giới hạn Heap 256MB, Metaspace 128MB, JIT cấp độ 1
ENTRYPOINT ["java", \
  "-Xms128m", \
  "-Xmx256m", \
  "-XX:MaxMetaspaceSize=128m", \
  "-XX:CompressedClassSpaceSize=32m", \
  "-XX:ReservedCodeCacheSize=64m", \
  "-Xss256k", \
  "-XX:+TieredCompilation", \
  "-XX:TieredStopAtLevel=1", \
  "-jar", "/app/app.jar"]
```

### 3.2. File Cấu Hình Tự Động Render (render.yaml)

```yaml
services:
  - type: web
    name: fixlink-api
    runtime: docker
    dockerfilePath: ./backend/Dockerfile
    dockerContext: ./backend
    plan: free
    region: singapore
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: prod
      - key: SPRING_DATASOURCE_URL
        fromDatabase:
          name: fixlink-db
          property: connectionString
      - key: JWT_SECRET
        generateValue: true
      - key: CORS_ALLOWED_ORIGINS
        value: https://fixlink.vercel.app,http://localhost:5173
      - key: FRONTEND_URL
        value: https://fixlink.vercel.app
      - key: CHATBOT_ENABLED
        value: "false"

databases:
  - name: fixlink-db
    databaseName: fixlink_db
    user: fixlink_user
    plan: free
    region: singapore
```

---

## 4. Sổ Tay Bắt Bệnh & Xử Lý Sự Cố (Troubleshooting Master Playbook)

### Sự cố 1: `Exit Code 137 / Out of Memory (OOM)`
- **Dấu hiệu**: Log hiển thị `Killed` hoặc `Container terminated with exit code 137` ngay sau khi Spring Boot in banner.
- **Nguyên nhân**: JVM vượt quá hạn mức 512MB RAM của gói Render Free.
- **Cách xử lý**:
  1. Kiểm tra lại `ENTRYPOINT` trong Dockerfile đã có các cờ `-Xmx256m -XX:MaxMetaspaceSize=128m -XX:TieredStopAtLevel=1` chưa.
  2. Trong `application-prod.yml`, đặt `maximum-pool-size: 5` cho HikariCP để giảm số lượng kết nối tiêu tốn RAM.

---

### Sự cố 2: `Health check failed / Timed out waiting for response`
- **Dấu hiệu**: Ứng dụng khởi động xong (`Started FixLinkBackendApplication in ... seconds`) nhưng Render báo `Deployment failed: timed out`.
- **Nguyên nhân**:
  - Render cung cấp biến `$PORT` (ví dụ `10000`).
  - Spring Boot lắng nghe đúng port `${PORT}`, nhưng Dockerfile Healthcheck gửi request vào `http://localhost:8080`.
- **Cách xử lý**:
  - Sửa lệnh `CMD` trong Dockerfile thành `http://localhost:${PORT:-8080}/api/v1/health`.
  - Tăng `--start-period=50s` để cho phép Spring Boot hoàn tất quá trình nạp bean trước khi bị kiểm tra sức khỏe.

---

### Sự cố 3: `PSQLException: Connection refused / No suitable driver`
- **Dấu hiệu**: `org.postgresql.util.PSQLException: Connection to localhost:5432 refused` hoặc `Cannot load driver-class-name: no suitable driver found for postgres://...`.
- **Nguyên nhân**:
  - Thiếu biến môi trường `SPRING_PROFILES_ACTIVE=prod` khiến Spring chạy profile `local` và cố kết nối vào `localhost:5432`.
  - Hoặc dùng chuỗi kết nối `postgres://` chưa được chuyển đổi sang chuẩn `jdbc:postgresql://`.
- **Cách xử lý**:
  - Đảm bảo trong tab Environment của Render có biến `SPRING_PROFILES_ACTIVE=prod`.
  - Khai báo rõ ràng tiền tố `jdbc:postgresql://` kèm tham số `?sslmode=require`.

---

### Sự cố 4: `Could not resolve placeholder 'JWT_SECRET'`
- **Dấu hiệu**: Spring Boot crash ngay lập tức với lỗi `BeanDefinitionStoreException: Could not resolve placeholder 'JWT_SECRET' in value "${JWT_SECRET}"`.
- **Nguyên nhân**: Trong `application-prod.yml` cấu hình `jwt.secret: ${JWT_SECRET}` nhưng chưa khai báo key `JWT_SECRET` trong Render Dashboard.
- **Cách xử lý**:
  - Thêm biến `JWT_SECRET` trên Render với một chuỗi ngẫu nhiên dài từ 64 ký tự (ví dụ tạo nhanh bằng lệnh: `openssl rand -hex 32`).

---

### Sự cố 5: `CORS Policy: No 'Access-Control-Allow-Origin' header`
- **Dấu hiệu**: Mở trang web trên Vercel thì giao diện không load được dữ liệu, F12 Console báo đỏ lỗi CORS.
- **Nguyên nhân**: Biến `CORS_ALLOWED_ORIGINS` trên Render chưa chứa domain của Vercel (ví dụ: `https://fixlink.vercel.app` hoặc các domain preview `https://fixlink-*.vercel.app`).
- **Cách xử lý**:
  - Đặt `CORS_ALLOWED_ORIGINS` = `https://fixlink.vercel.app,http://localhost:5173` (hoặc tạm thời `*` để kiểm tra thông luồng).

---

## 5. Quy Trình Kiểm Thử Khói (Post-Deployment Smoke Test)

Sau khi Render báo `Deploy live`, thực hiện kiểm tra nhanh hệ sinh thái:

```bash
# 1. Kiểm tra trạng thái sức khỏe Backend
curl -i https://fixlink-api.onrender.com/api/v1/health
# Kỳ vọng: HTTP/1.1 200 OK, {"status": "UP"}

# 2. Kiểm tra Swagger UI tài liệu API
curl -i https://fixlink-api.onrender.com/swagger-ui.html
# Kỳ vọng: HTTP/1.1 200 OK (hoặc 302 Redirect về /swagger-ui/index.html)

# 3. Kiểm tra API công khai (Danh mục nghề sửa chữa)
curl -s https://fixlink-api.onrender.com/api/v1/categories | jq .
# Kỳ vọng: Danh sách các danh mục Điện lạnh, Điện tử, Sửa ống nước...

# 4. Kiểm tra kết nối từ Frontend
# Mở trình duyệt truy cập domain Vercel, thực hiện đăng nhập và xem chi tiết đơn hàng
```
