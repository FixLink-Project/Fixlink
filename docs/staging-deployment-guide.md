# FixLink - Hướng Dẫn Vận Hành & Triển Khai Staging (Staging Operations Guide)

Tài liệu này cung cấp chi tiết quy trình triển khai tự động (Continuous Deployment) lên môi trường **Staging**, cách chuẩn bị hạ tầng máy chủ và quy trình kiểm thử sau triển khai (Post-Deployment Smoke Test).

---

## 1. Tổng Quan Kiến Trúc Môi Trường Staging

```
[ Merge to 'main' ]
         │
         ▼
[ GitHub Actions: CD Staging ]
         │
         ├──► 1. Build Docker Images (Backend & Frontend)
         ├──► 2. Push tags to GitHub Container Registry (ghcr.io)
         └──► 3. SSH into Staging Host
                   │
                   ▼
       [ Staging Host / Server ]
       ├── Pull ghcr.io images
       ├── Execute docker compose -f docker-compose.staging.yml up -d
       ├── PostgreSQL DB Migration (Flyway)
       └── Automated Healthcheck & Smoke Tests
```

---

## 2. Chuẩn Bị Hạ Tầng Máy Chủ Staging

Trên máy chủ Staging (Ubuntu 22.04 LTS / Debian 12 / Docker Host):

### Bước 1: Cài đặt Docker & Docker Compose
```bash
sudo apt update
sudo apt install -y docker.io docker-compose-plugin
sudo systemctl enable --now docker
```

### Bước 2: Tạo thư mục triển khai & Phân quyền SSH
```bash
sudo mkdir -p /opt/fixlink-staging
sudo chown -R $USER:$USER /opt/fixlink-staging
```

Tạo SSH Keypair cho tài khoản deploy:
```bash
ssh-keygen -t ed25519 -C "deployer@fixlink-staging" -f ~/.ssh/staging_deploy_key
cat ~/.ssh/staging_deploy_key.pub >> ~/.ssh/authorized_keys
```
> Private key (`~/.ssh/staging_deploy_key`) sẽ được lưu vào GitHub Secret: `STAGING_SSH_KEY`.

### Bước 3: Sao chép cấu hình compose lên server
Sao chép tệp `docker-compose.staging.yml` vào thư mục `/opt/fixlink-staging/docker-compose.staging.yml`.

---

## 3. Quy Trình Triển Khai Tự Động (Continuous Deployment)

1. Khi một Pull Request được duyệt và hợp nhất (merged) vào nhánh `main`:
   - GitHub Action `.github/workflows/deploy-staging.yml` tự động khởi chạy.
   - Biên dịch và đóng gói Docker image cho backend và frontend, gắn tag commit SHA ngắn (`abc1234`) và tag `staging-latest`.
   - Đẩy image lên `ghcr.io`.
2. Action tự động SSH vào máy chủ Staging qua địa chỉ `STAGING_SERVER_HOST`:
   - Kéo (pull) bản image mới nhất.
   - Khởi động lại container dịch vụ không gây gián đoạn (rolling update).
   - Kiểm tra phản hồi HTTP từ cổng nội bộ để xác nhận ứng dụng đã sống (healthy).
3. Nếu có lỗi xảy ra, pipeline lập tức thông báo FAILED và giữ nguyên phiên bản cũ đang chạy ổn định.

---

## 4. Kiểm Thử Khói Sau Triển Khai (Post-Deployment Smoke Test)

Sau khi deploy hoàn tất, đội ngũ QA/Dev có thể kiểm tra nhanh:

```bash
# 1. Kiểm tra trạng thái container
docker compose -f /opt/fixlink-staging/docker-compose.staging.yml ps

# 2. Kiểm tra log backend
docker compose -f /opt/fixlink-staging/docker-compose.staging.yml logs -f backend

# 3. Kiểm tra API categories
curl -f -s http://staging.fixlink.vn/api/v1/categories
```
