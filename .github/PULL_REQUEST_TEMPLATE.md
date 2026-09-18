## Mô tả Thay đổi (Description)
<!-- Tóm tắt mục tiêu, tính năng hoặc lỗi được xử lý trong PR này -->

## Loại Thay đổi (Type of Change)
- [ ] 🐛 Bug fix (sửa lỗi không gây ảnh hưởng tương thích ngược)
- [ ] ✨ New feature (tính năng mới)
- [ ] ♻️ Refactor / Code Cleanup
- [ ] 📝 Documentation update
- [ ] ⚙️ CI/CD / DevOps configuration

## Checklist Kiểm tra trước khi yêu cầu Review
- [ ] Đã chạy `./mvnw spotless:apply` (backend) và `npm run lint` (frontend)
- [ ] Đã chạy toàn bộ test suite thành công:
  - Backend: `./mvnw test` (Pass 100%)
  - Frontend: `npm test` (Pass 100%)
- [ ] Đã kích hoạt và tuân thủ Git pre-commit hooks
- [ ] Không có credentials/secrets nào bị commit vào mã nguồn
- [ ] Tài liệu liên quan (nếu có) đã được cập nhật
