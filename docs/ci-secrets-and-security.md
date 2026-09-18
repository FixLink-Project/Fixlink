# FixLink - CI/CD Secrets Management & Security Architecture

Tài liệu hướng dẫn quản lý bí mật (Secrets Management), cách thức lưu trữ trên kho bí mật của hệ thống CI/CD (GitHub Secrets & Environments) và các nguyên tắc bảo mật tối thượng: **"Secrets stored in the CI secret store, never in the repository"**.

---

## 1. Nguyên Tắc Cốt Lõi (Zero-Leak Policy)

1. **Tuyệt đối không lưu secret trong mã nguồn**: Không bao giờ commit mật khẩu, khóa riêng tư (private keys), access tokens, hoặc chuỗi kết nối trực tiếp vào git repository (kể cả file config hay script).
2. **Kho bí mật tập trung (CI Secret Store)**: Toàn bộ bí mật phục vụ quy trình build và deploy phải được định nghĩa trong mục **Settings > Secrets and variables > Actions** của GitHub.
3. **Phân tách theo Môi trường (Environment Isolation)**: Các bí mật của môi trường Staging được cấu hình trong GitHub Environment `staging`. Môi trường Production (sau này) sẽ có kho secret hoàn toàn độc lập.

---

## 2. Danh Mục Các Bí Mật Cần Cấu Hình (Required CI Secrets)

Truy cập: **Repository Settings > Environments > staging > Environment secrets** và bổ sung:

| Tên Bí Mật (Secret Name) | Mô Tả & Mục Đích | Ví Dụ Giá Trị |
| :--- | :--- | :--- |
| `STAGING_SERVER_HOST` | Địa chỉ IP hoặc tên miền của máy chủ Staging | `103.154.xxx.yyy` hoặc `staging.fixlink.vn` |
| `STAGING_SSH_USER` | Tên người dùng SSH để kết nối máy chủ deploy | `ubuntu` hoặc `deployer` |
| `STAGING_SSH_KEY` | Private Key (OpenSSH format) của tài khoản deploy | `-----BEGIN OPENSSH PRIVATE KEY----- ...` |
| `STAGING_DB_PASSWORD` | Mật khẩu database PostgreSQL trên Staging | Mật khẩu ngẫu nhiên phức tạp (tối thiểu 24 ký tự) |
| `STAGING_JWT_SECRET` | Khóa bí mật ký JWT Token (chuẩn HS512, >= 512 bits) | Chuỗi ngẫu nhiên dài bảo đảm tính bảo mật |

> [!NOTE]
> Token truy cập GitHub Container Registry (`GITHUB_TOKEN`) được GitHub Actions tự động cung cấp trong runtime với quyền `packages: write`, không cần cấu hình thủ công.

---

## 3. Cách Thức Hoạt Động Trong Pipeline Deploy

Khi workflow `deploy-staging.yml` chạy:
1. Runner sử dụng `${{ secrets.GITHUB_TOKEN }}` để build và push image lên registry bảo mật `ghcr.io`.
2. Action `appleboy/ssh-action` sử dụng `STAGING_SSH_KEY` để mở phiên SSH được mã hóa tới máy chủ Staging.
3. Các secret `STAGING_DB_PASSWORD`, `STAGING_JWT_SECRET` được truyền trực tiếp vào môi trường chạy của docker-compose trên server mà không bao giờ ghi ra file log hay tệp văn bản tĩnh.
4. Quá trình triển khai hoàn tất và phiên làm việc được đóng ngay lập tức.
