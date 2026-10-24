---
title: "Tài khoản, bảo mật và chính sách mật khẩu FixLink"
audience: [guest, customer, technician, admin]
updated: "2026-10-23"
confidence: verified
---

# Tài khoản, bảo mật và chính sách mật khẩu

## 1. Đăng ký tài khoản
- **Khách hàng cá nhân:** tự đăng ký miễn phí tại `/dang-ky` với Tên đăng nhập (3–50 ký tự), Mật khẩu (tối thiểu 6 ký tự), Họ tên, Số điện thoại Việt Nam (10 số, đầu 03/05/07/08/09) và Email. Tên đăng nhập, số điện thoại, email không được trùng tài khoản khác. Đăng ký xong có thể dùng ngay.
- **Kỹ thuật viên (thợ):** đăng ký tại `/dang-ky-tho`, mật khẩu tối thiểu 8 ký tự gồm ít nhất 1 chữ in hoa và 1 ký tự đặc biệt; khai thêm số CCCD, ảnh 2 mặt CCCD, chuyên môn và khu vực. Tài khoản thợ **phải chờ Admin duyệt mới đăng nhập được**.
- Một người có thể có tài khoản khách hàng và tài khoản thợ riêng biệt.

## 2. Đăng nhập
- Khách hàng: `/dang-nhap`. Thợ: `/dang-nhap-tho`. Chọn đúng cổng theo loại tài khoản; tài khoản không đúng vai trò sẽ báo không tồn tại.
- Phiên đăng nhập được duy trì bằng token; bấm Đăng xuất để kết thúc phiên trên thiết bị dùng chung.

## 3. Chính sách mật khẩu khi đổi hoặc đặt lại mật khẩu
Mật khẩu mới bắt buộc:
1. Tối thiểu **8 ký tự**.
2. Có ít nhất **1 chữ in hoa** (`A-Z`) và **1 chữ thường** (`a-z`).
3. Có ít nhất **1 chữ số** (`0-9`).
4. Có ít nhất **1 ký tự đặc biệt**, ví dụ `!@#$%^&*()_+-=`.
5. Ví dụ hợp lệ: `Password@123`, `FixLink#2026`. Không hợp lệ: `12345678`, `password`.

## 4. Khóa tài khoản và an ninh đăng nhập
- **Khóa tạm thời:** nhập sai mật khẩu **5 lần liên tiếp** → tài khoản bị khóa **15 phút** (mã lỗi `ACCOUNT_TEMPORARILY_LOCKED`, HTTP 429, kèm số giây cần chờ).
- **Tài khoản chờ duyệt / bị từ chối duyệt** (thợ): `ACCOUNT_PENDING_APPROVAL` / `ACCOUNT_REJECTED` (HTTP 403).
- **Bị khóa / ngừng hoạt động** do vi phạm hoặc nghi vấn bảo mật: `ACCOUNT_BLOCKED` / `ACCOUNT_INACTIVE` (HTTP 403). Cần tạo phiếu hỗ trợ để CSKH thẩm định mở lại.

## 5. Quên và đặt lại mật khẩu
1. Vào **Quên mật khẩu** (`/quen-mat-khau`), nhập **email** đã đăng ký.
2. Hệ thống gửi email chứa liên kết/mã đặt lại; mã có hiệu lực **30 phút** và chỉ dùng được một lần. Vì bảo mật, thông báo trả về luôn giống nhau dù email có tồn tại hay không.
3. Mỗi email được yêu cầu tối đa **3 lần trong 1 giờ**.
4. Vào **Đặt lại mật khẩu** (`/dat-lai-mat-khau`), nhập mã và mật khẩu mới theo chính sách mục 3. Sau khi đặt lại, các phiên đăng nhập cũ bị thu hồi.
- Không nhận được email: kiểm tra hộp thư rác/spam, kiểm tra đúng email đăng ký, chờ vài phút rồi thử lại.

## 6. Cam kết bảo mật
- Trợ lý AI **không bao giờ hỏi mật khẩu hoặc mã OTP**, không đổi mật khẩu hộ, chỉ hướng dẫn liên kết chính thức.
- Mật khẩu được lưu dạng băm (hash); mã đặt lại mật khẩu chỉ lưu bản băm.
- Không chia sẻ mật khẩu cho bất kỳ ai, kể cả người tự xưng là nhân viên FixLink.
