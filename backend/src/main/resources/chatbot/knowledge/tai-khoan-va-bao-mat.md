---
title: "Tài khoản, bảo mật và chính sách mật khẩu FixLink"
audience: [guest, customer, technician, admin]
updated: "2026-10-17"
confidence: verified
---

# Tài khoản, bảo mật và chính sách mật khẩu

## 1. Đăng ký tài khoản
- **Khách hàng cá nhân:** Có thể tự đăng ký trực tuyến miễn phí tại `/dang-ky` (hoặc `/register`) bằng cách điền Tên đăng nhập, Mật khẩu, Họ tên, Số điện thoại và Email.
- **Kỹ thuật viên (Thợ):** Đăng ký tại `/dang-ky-tho`, yêu cầu cung cấp thêm Số Căn cước công dân (CCCD/CMND), ảnh chụp 2 mặt CCCD, danh mục chuyên môn và khu vực phục vụ để xét duyệt KYC.

## 2. Tiêu chuẩn chính sách mật khẩu (Password Policy)
Để đảm bảo an toàn tối đa cho tài khoản, mật khẩu tại FixLink bắt buộc phải tuân thủ nghiêm ngặt các quy tắc sau (theo đặc tả API §1.5):
1. **Độ dài:** Tối thiểu **8 ký tự**.
2. **Ký tự viết hoa:** Chứa ít nhất **1 chữ cái viết hoa** (`A-Z`).
3. **Ký tự viết thường:** Chứa ít nhất **1 chữ cái viết thường** (`a-z`).
4. **Chữ số:** Chứa ít nhất **1 chữ số** (`0-9`).
5. **Ký tự đặc biệt:** Chứa ít nhất **1 ký tự đặc biệt** trong tập hợp: `!@#$%^&*()_+-=`
6. **Ví dụ mật khẩu hợp lệ:** `Password@123`, `FixLink#2026`, `Abc!5678`.
7. **Ví dụ mật khẩu KHÔNG hợp lệ:** `12345678` (thiếu chữ hoa, chữ thường, ký tự đặc biệt), `password` (thiếu số và ký tự đặc biệt).

## 3. Quy định khóa tài khoản và an ninh đăng nhập
- **Khóa tạm thời do nhập sai (Brute-force protection):**
  - Nếu người dùng nhập sai mật khẩu liên tiếp **5 lần**, hệ thống sẽ tự động kích hoạt cơ chế phòng vệ và tạm khóa tài khoản trong vòng **15 phút**.
  - Mã lỗi trả về là `ACCOUNT_TEMPORARILY_LOCKED` (HTTP 429) kèm số giây còn lại cần chờ trước khi được thử lại.
- **Khóa tài khoản vĩnh viễn/ngừng hoạt động:**
  - Nếu tài khoản có dấu hiệu vi phạm chính sách cộng đồng, gian lận hoặc chưa xác minh đầy đủ, Quản trị viên (Admin) có quyền khóa tài khoản (`ACCOUNT_BLOCKED`, HTTP 403) hoặc đưa về trạng thái ngừng hoạt động (`ACCOUNT_INACTIVE`, HTTP 403).
  - Khi gặp lỗi này, người dùng cần tạo phiếu hỗ trợ để được đội ngũ CSKH thẩm định mở lại tài khoản.

## 4. Quên và đặt lại mật khẩu
- Khi quên mật khẩu, người dùng truy cập trang **Quên mật khẩu** (`/quen-mat-khau`).
- Nhập email hoặc tên tài khoản đã đăng ký để nhận mã xác thực (Reset Token).
- Sau khi có mã xác thực, người dùng truy cập trang **Đặt lại mật khẩu** (`/dat-lai-mat-khau`), nhập mã và thiết lập mật khẩu mới tuân thủ đúng chính sách mật khẩu ở trên.
- **Lưu ý bảo mật:** Trợ lý AI không bao giờ hỏi mật khẩu của bạn, không bao giờ tự ý đổi mật khẩu hộ người dùng, và chỉ hướng dẫn truy cập các liên kết chính thức nêu trên.
