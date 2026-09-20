# FIXLINK - TÀI LIỆU ĐẶC TẢ API (API SPECIFICATION)
> **Phiên bản:** v2.0.0 (Sprint 0 – Auth Lifecycle, User Profile & Service Catalog)  
> **Dự án:** FixLink – Nền tảng kết nối khách hàng và kỹ thuật viên sửa chữa  
> **Mục đích:**  
> - **Backend (BE):** Dùng làm khuôn mẫu triển khai API chuẩn RESTful, cấu trúc dữ liệu chặt chẽ, kiểm soát validation.  
> - **Frontend (FE):** Dùng làm Mock Data xây dựng giao diện UI (Web/Mobile) độc lập mà không cần đợi BE hoàn thiện.  
> - **Tiêu chuẩn thiết kế:** Đáp ứng 100% yêu cầu của Giảng viên hướng dẫn về **Phân trang (Pagination), Bộ lọc (Filter), Tìm kiếm (Search), Sắp xếp (Sort)** và xử lý mã lỗi thống nhất.

> **Quy ước bảo mật mật khẩu:**  
> Toàn bộ mật khẩu trong hệ thống FixLink **BẮT BUỘC** mã hóa bằng **BCrypt** (cost factor ≥ 12).  
> Không bao giờ lưu hoặc trả về mật khẩu dạng plain text trong response.  
> Login dùng `BCrypt.checkpw()`, Register/Change/Reset dùng `BCrypt.hashpw(password, BCrypt.gensalt(12))`.

---

## MỤC LỤC
1. [Quy Chuẩn Thiết Kế API Chung](#1-quy-chuẩn-thiết-kế-api-chung)
   - [1.1. Base URL & Versioning](#11-base-url--versioning)
   - [1.2. Chuẩn định dạng Response Body](#12-chuẩn-định-dạng-response-body)
   - [1.3. Chuẩn Query Parameters cho API Danh sách (List API)](#13-chuẩn-query-parameters-cho-api-danh-sách-list-api)
   - [1.4. Bảng mã lỗi hệ thống (Standard Error Codes)](#14-bảng-mã-lỗi-hệ-thống-standard-error-codes)
   - [1.5. Chính sách mật khẩu (Password Policy)](#15-chính-sách-mật-khẩu-password-policy)
2. [Module 1: Xác Thực & Tài Khoản (Authentication & Profile)](#2-module-1-xác-thực--tài-khoản-authentication--profile)
   - [POST /api/v1/auth/login](#21-post-apiv1authlogin)
   - [POST /api/v1/auth/register/customer](#22-post-apiv1authregistercustomer)
   - [POST /api/v1/auth/register/technician](#23-post-apiv1authregistertechnician)
   - [GET /api/v1/auth/me](#24-get-apiv1authme)
   - [PUT /api/v1/auth/change-password](#25-put-apiv1authchange-password)
   - [POST /api/v1/auth/forgot-password](#26-post-apiv1authforgot-password)
   - [POST /api/v1/auth/reset-password](#27-post-apiv1authreset-password)
   - [POST /api/v1/auth/logout](#28-post-apiv1authlogout)
   - [POST /api/v1/auth/refresh-token](#29-post-apiv1authrefresh-token)
3. [Module 2: Quản Trị Người Dùng & Duyệt KYC (Admin Management)](#3-module-2-quản-trị-người-dùng--duyệt-kyc-admin-management)
   - [GET /api/v1/admin/users](#31-get-apiv1adminusers)
   - [GET /api/v1/admin/users/{id}](#32-get-apiv1adminusersid)
   - [PATCH /api/v1/admin/users/{id}/status](#33-patch-apiv1adminusersidstatus)
   - [PATCH /api/v1/admin/technicians/{userId}/verify](#34-patch-apiv1admintechniciansuseridverify)
4. [Module 3: Danh Mục Dịch Vụ & Địa Bàn (Master Data)](#4-module-3-danh-mục-dịch-vụ--địa-bàn-master-data)
   - [GET /api/v1/categories](#41-get-apiv1categories)
   - [POST /api/v1/admin/categories](#42-post-apiv1admincategories)
   - [GET /api/v1/admin/categories/{id}](#43-get-apiv1admincategoriesid)
   - [PUT /api/v1/admin/categories/{id}](#44-put-apiv1admincategoriesid)
   - [DELETE /api/v1/admin/categories/{id}](#45-delete-apiv1admincategoriesid)
   - [GET /api/v1/areas](#46-get-apiv1areas)
5. [Module 4: Quản Lý Hồ Sơ Kỹ Thuật Viên (Technician Profile Management)](#5-module-4-quản-lý-hồ-sơ-kỹ-thuật-viên-technician-profile-management)
   - [PUT /api/v1/technicians/me/profile](#51-put-apiv1techniciansmeprofile)
6. [Hướng Dẫn Dành Cho Frontend (Mock UI Guide)](#6-hướng-dẫn-dành-cho-frontend-mock-ui-guide)

---

## 1. QUY CHUẨN THIẾT KẾ API CHUNG

### 1.1. Base URL & Versioning
- **Local Development:** `http://localhost:8080`
- **Mọi Endpoint đều bắt đầu bằng:** `/api/v1`
- **Content-Type:** `application/json; charset=UTF-8`

### 1.2. Chuẩn định dạng Response Body

#### A. Phản hồi thành công cho đối tượng đơn lẻ (Single Object Response)
Áp dụng cho các thao tác Tạo mới (`201 Created`), Chi tiết hoặc Cập nhật (`200 OK`):
```json
{
  "statusCode": 200,
  "message": "Mô tả kết quả thực hiện thành công",
  "data": {
    "id": "usr_11223344",
    "name": "..."
  }
}
```

#### B. Phản hồi thành công cho danh sách có phân trang (Paginated List Response)
> **BẮT BUỘC THEO YÊU CẦU CỦA THẦY:** Tất cả các API trả về danh sách đều phải chứa block `meta` phục vụ render thanh phân trang trên giao diện.
```json
{
  "statusCode": 200,
  "message": "Lấy danh sách thành công",
  "meta": {
    "currentPage": 1,
    "limit": 10,
    "totalItems": 45,
    "totalPages": 5,
    "hasNext": true,
    "hasPrevious": false
  },
  "data": [
    { ... },
    { ... }
  ]
}
```

#### C. Phản hồi lỗi chuẩn (Standard Error Response)
```json
{
  "statusCode": 400,
  "errorCode": "USER_ALREADY_EXISTS",
  "message": "Số điện thoại hoặc Email đã được sử dụng",
  "errors": {
    "email": "Email đã tồn tại trong hệ thống"
  }
}
```

---

### 1.3. Chuẩn Query Parameters cho API Danh sách (List API)

Mọi API List đều dùng chung bộ query params chuẩn hóa:
| Tham số | Kiểu dữ liệu | Bắt buộc | Mặc định | Ý nghĩa & Ví dụ |
| :--- | :---: | :---: | :---: | :--- |
| `page` | `Integer` | Không | `1` | Trang hiện tại (bắt đầu từ 1). |
| `limit` | `Integer` | Không | `10` | Số lượng bản ghi trên một trang (tối đa 100). |
| `search` | `String` | Không | `null` | Từ khóa tìm kiếm đa trường (Tên, SĐT, Email, CCCD,...). |
| `filter` | `String` | Không | `ALL` | Bộ lọc theo trạng thái, vai trò,... (tùy API). |
| `sortBy` | `String` | Không | `createdAt` | Tên trường cần sắp xếp (vd: `createdAt`, `fullName`, `rating`). |
| `sortOrder` | `String` | Không | `DESC` | Thứ tự sắp xếp: `ASC` (Tăng dần) hoặc `DESC` (Giảm dần). |

---

### 1.4. Bảng mã lỗi hệ thống (Standard Error Codes)

| HTTP Status | Error Code | Ý nghĩa |
| :---: | :--- | :--- |
| **400** | `VALIDATION_FAILED` | Dữ liệu đầu vào sai định dạng (thiếu trường, sai regex email/phone). |
| **400** | `USER_ALREADY_EXISTS` | Tên đăng nhập, Email, Số điện thoại hoặc CCCD đã bị trùng lặp. |
| **400** | `INVALID_OPERATION` | Thao tác không hợp lệ theo quy tắc nghiệp vụ. |
| **400** | `INVALID_CURRENT_PASSWORD` | Mật khẩu hiện tại không chính xác (dùng trong đổi mật khẩu). |
| **400** | `PASSWORD_SAME_AS_OLD` | Mật khẩu mới không được trùng với mật khẩu hiện tại. |
| **400** | `INVALID_RESET_TOKEN` | Token đặt lại mật khẩu không hợp lệ, đã hết hạn hoặc đã được sử dụng. |
| **400** | `INVALID_REFRESH_TOKEN` | Refresh token không hợp lệ, đã hết hạn hoặc đã bị thu hồi. |
| **400** | `DUPLICATE_CATEGORY_NAME` | Tên danh mục dịch vụ đã tồn tại trong hệ thống. |
| **401** | `INVALID_CREDENTIALS` | Sai username hoặc password khi đăng nhập. |
| **401** | `UNAUTHORIZED` | Token JWT thiếu, hết hạn hoặc không hợp lệ. |
| **401** | `TOKEN_BLACKLISTED` | Token JWT đã bị thu hồi do đăng xuất hoặc đổi mật khẩu. |
| **403** | `ACCOUNT_BLOCKED` | Tài khoản đã bị Admin khóa vi phạm chính sách. |
| **403** | `ACCESS_DENIED` | Không có quyền hạn truy cập tài nguyên (ví dụ: Khách gọi API Admin). |
| **404** | `RESOURCE_NOT_FOUND` | Không tìm thấy bản ghi theo ID cung cấp. |
| **429** | `RATE_LIMIT_EXCEEDED` | Gửi quá nhiều yêu cầu trong thời gian ngắn (áp dụng cho forgot-password). |
| **500** | `INTERNAL_SERVER_ERROR` | Lỗi phát sinh từ phía máy chủ / cơ sở dữ liệu. |

---

### 1.5. Chính sách mật khẩu (Password Policy)

> **Áp dụng cho:** Register, Change Password, Reset Password.

| Tiêu chí | Quy tắc |
| :--- | :--- |
| Độ dài tối thiểu | 8 ký tự |
| Chữ hoa | Ít nhất 1 ký tự viết hoa (A-Z) |
| Chữ thường | Ít nhất 1 ký tự viết thường (a-z) |
| Chữ số | Ít nhất 1 chữ số (0-9) |
| Ký tự đặc biệt | Ít nhất 1 trong: `!@#$%^&*()_+-=` |
| Regex kiểm tra | `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=]).{8,}$` |
| Mã hóa lưu trữ | `BCrypt.hashpw(password, BCrypt.gensalt(12))` |
| So sánh khi login | `BCrypt.checkpw(inputPassword, storedHash)` |

**Ví dụ mật khẩu hợp lệ:** `Password@123`, `FixLink#2026`, `Abc!5678`  
**Ví dụ mật khẩu KHÔNG hợp lệ:** `12345678` (thiếu chữ hoa, chữ thường, ký tự đặc biệt), `password` (thiếu chữ hoa, số, ký tự đặc biệt)

---

## 2. MODULE 1: XÁC THỰC & TÀI KHOẢN (AUTHENTICATION & PROFILE)

### 2.1. POST `/api/v1/auth/login`
- **Mô tả:** Đăng nhập hệ thống dành cho mọi Actor (Khách hàng, Kỹ thuật viên, Admin, Staff).
- **Yêu cầu Auth:** Public.

#### Request Headers:
```http
Content-Type: application/json
```

#### Request Body:
```json
{
  "username": "customer01",
  "password": "Password@123",
  "deviceToken": "fcm_device_token_optional"
}
```
| Trường | Kiểu | Bắt buộc | Ràng buộc |
| :--- | :---: | :---: | :--- |
| `username` | String | Có | Không được để trống |
| `password` | String | Có | Không được để trống |
| `deviceToken` | String | Không | Token thông báo đẩy Firebase FCM |

#### Response 200 OK (Thành công):
```json
{
  "statusCode": 200,
  "message": "Đăng nhập thành công",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjdXN0b21lcjAxIiwidXNlcklkIjoyLCJyb2xlIjoiQ1VTVE9NRVIiLCJpYXQiOjE3ODk...",
    "refreshToken": "d9b2d63d-ce7a-4286-9a29-bc8c4146a482",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": "usr_2",
      "username": "customer01",
      "role": "CUSTOMER",
      "status": "ACTIVE",
      "fullName": "Nguyễn Văn A",
      "avatarUrl": "https://s3.fixlink.vn/avatars/usr_customer01.jpg",
      "isVerified": true
    }
  }
}
```

#### Response 401 Unauthorized (Sai tài khoản / mật khẩu):
```json
{
  "statusCode": 401,
  "errorCode": "INVALID_CREDENTIALS",
  "message": "Tên đăng nhập hoặc mật khẩu không chính xác"
}
```

#### Response 403 Forbidden (Tài khoản bị khóa):
```json
{
  "statusCode": 403,
  "errorCode": "ACCOUNT_BLOCKED",
  "message": "Tài khoản của bạn đã bị khóa do vi phạm chính sách"
}
```

---

### 2.2. POST `/api/v1/auth/register/customer`
- **Mô tả:** Đăng ký tài khoản Khách hàng (Customer) mới.
- **Yêu cầu Auth:** Public.

#### Request Body:
```json
{
  "username": "lethikhach",
  "password": "Password@123",
  "fullName": "Lê Thị Khách",
  "phone": "0912345678",
  "email": "lethikhach@gmail.com"
}
```
| Trường | Kiểu | Bắt buộc | Ràng buộc |
| :--- | :---: | :---: | :--- |
| `username` | String | Có | 3 - 50 ký tự |
| `password` | String | Có | Tuân thủ Password Policy (Mục 1.5) |
| `fullName` | String | Có | Không để trống |
| `phone` | String | Có | Regex định dạng số điện thoại Việt Nam |
| `email` | String | Có | Định dạng email hợp lệ |

> **Xử lý mật khẩu:** `password` được mã hóa `BCrypt.hashpw(password, BCrypt.gensalt(12))` trước khi lưu vào cột `users.password_hash`.

#### Response 201 Created (Thành công):
```json
{
  "statusCode": 201,
  "message": "Đăng ký tài khoản thành công",
  "data": {
    "userId": "usr_4",
    "username": "lethikhach",
    "role": "CUSTOMER",
    "fullName": "Lê Thị Khách",
    "phone": "0912345678",
    "email": "lethikhach@gmail.com",
    "createdAt": "2026-09-14T19:50:00"
  }
}
```

#### Response 400 Bad Request (Trùng lặp dữ liệu):
```json
{
  "statusCode": 400,
  "errorCode": "USER_ALREADY_EXISTS",
  "message": "Số điện thoại hoặc Email đã được sử dụng"
}
```

#### Response 400 Bad Request (Mật khẩu không đạt chính sách):
```json
{
  "statusCode": 400,
  "errorCode": "VALIDATION_FAILED",
  "message": "Dữ liệu đầu vào không hợp lệ",
  "errors": {
    "password": "Mật khẩu phải có tối thiểu 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt"
  }
}
```

---

### 2.3. POST `/api/v1/auth/register/technician`
- **Mô tả:** Đăng ký tài khoản Thợ / Kỹ thuật viên kèm thông tin xác minh hồ sơ KYC.
- **Yêu cầu Auth:** Public.

#### Request Body:
```json
{
  "username": "tho_dien_lanh_02",
  "password": "Password@123",
  "fullName": "Trần Văn B",
  "phone": "0987654321",
  "email": "tranvanb@gmail.com",
  "citizenId": "012345678901",
  "idCardFrontUrl": "https://s3.fixlink.vn/temp/id_front.jpg",
  "idCardBackUrl": "https://s3.fixlink.vn/temp/id_back.jpg",
  "bio": "Chuyên sửa chữa điều hòa, tủ lạnh công nghiệp trên 5 năm kinh nghiệm",
  "yearsExperience": 5
}
```
| Trường | Kiểu | Bắt buộc | Ràng buộc |
| :--- | :---: | :---: | :--- |
| `username` | String | Có | 3 - 50 ký tự |
| `password` | String | Có | Tuân thủ Password Policy (Mục 1.5) |
| `fullName` | String | Có | Tên đầy đủ của Thợ |
| `phone` | String | Có | Số điện thoại liên hệ |
| `email` | String | Có | Email liên hệ |
| `citizenId` | String | Có | Số CCCD từ 9 - 12 chữ số |
| `idCardFrontUrl` | String | Không | Link ảnh mặt trước CCCD |
| `idCardBackUrl` | String | Không | Link ảnh mặt sau CCCD |
| `bio` | String | Không | Giới thiệu năng lực nghề nghiệp |
| `yearsExperience`| Integer| Không | Số năm kinh nghiệm (Mặc định: 0) |

> **Xử lý mật khẩu:** `password` được mã hóa `BCrypt.hashpw(password, BCrypt.gensalt(12))` trước khi lưu vào cột `users.password_hash`.

#### Response 201 Created (Thành công):
```json
{
  "statusCode": 201,
  "message": "Đăng ký thành công. Hồ sơ đang chờ Admin xác minh",
  "data": {
    "userId": "usr_5",
    "username": "tho_dien_lanh_02",
    "role": "TECHNICIAN",
    "verificationStatus": "PENDING",
    "createdAt": "2026-09-14T19:55:00"
  }
}
```

---

### 2.4. GET `/api/v1/auth/me`
- **Mô tả:** Lấy thông tin tài khoản và profile của người dùng đang đăng nhập dựa trên JWT Token.
- **Yêu cầu Auth:** Bắt buộc (`Authorization: Bearer <JWT_TOKEN>`).

#### Response 200 OK:
```json
{
  "statusCode": 200,
  "message": "Lấy thông tin người dùng thành công",
  "data": {
    "id": "usr_5",
    "username": "tho_dien_lanh_02",
    "role": "TECHNICIAN",
    "status": "PENDING",
    "createdAt": "2026-09-14T19:55:00",
    "technicianProfile": {
      "fullName": "Trần Văn B",
      "phone": "0987654321",
      "email": "tranvanb@gmail.com",
      "citizenId": "012345678901",
      "avatarUrl": null,
      "verificationStatus": "PENDING",
      "yearsExperience": 5,
      "avgRating": 0.0,
      "completedJobs": 0,
      "walletBalance": 0,
      "isOnline": false
    }
  }
}
```

---

### 2.5. PUT `/api/v1/auth/change-password`
> **User Story:** *As an authenticated user, I want to change my own password, so that I can keep my account secure.*

- **Mô tả:** Đổi mật khẩu tài khoản đang đăng nhập. Yêu cầu nhập đúng mật khẩu hiện tại, mật khẩu mới phải khớp xác nhận và tuân thủ Password Policy.
- **Yêu cầu Auth:** Bắt buộc (`Authorization: Bearer <JWT_TOKEN>`).
- **Áp dụng cho:** Mọi Actor (Customer, Technician, Admin, Staff).

#### Request Headers:
```http
Content-Type: application/json
Authorization: Bearer <accessToken>
```

#### Request Body:
```json
{
  "currentPassword": "Password@123",
  "newPassword": "NewSecure@456",
  "confirmPassword": "NewSecure@456"
}
```
| Trường | Kiểu | Bắt buộc | Ràng buộc |
| :--- | :---: | :---: | :--- |
| `currentPassword` | String | Có | Không được để trống. Backend dùng `BCrypt.checkpw(currentPassword, storedHash)` để xác minh. |
| `newPassword` | String | Có | Tuân thủ Password Policy (Mục 1.5). Không được trùng `currentPassword`. |
| `confirmPassword` | String | Có | Phải giống hệt `newPassword`. |

#### Luồng xử lý Backend:
1. Trích xuất `userId` từ JWT Token.
2. Truy vấn `users` lấy `password_hash` theo `userId`.
3. So sánh `currentPassword` với `password_hash` bằng `BCrypt.checkpw()`.
   - Sai → trả `400 INVALID_CURRENT_PASSWORD`.
4. Kiểm tra `newPassword == confirmPassword`.
   - Không khớp → trả `400 VALIDATION_FAILED` với lỗi per-field.
5. Kiểm tra `newPassword` tuân thủ Password Policy (Mục 1.5).
   - Không đạt → trả `400 VALIDATION_FAILED` với lỗi per-field.
6. Kiểm tra `newPassword != currentPassword` (so sánh plain text trước khi hash).
   - Trùng → trả `400 PASSWORD_SAME_AS_OLD`.
7. Hash mật khẩu mới: `BCrypt.hashpw(newPassword, BCrypt.gensalt(12))`.
8. Cập nhật `users.password_hash` và `users.updated_at`.
9. **Hủy toàn bộ refresh token khác** của user (chỉ giữ lại session hiện tại) → buộc các thiết bị khác phải đăng nhập lại.
10. Trả response thành công.

#### Response 200 OK (Thành công):
```json
{
  "statusCode": 200,
  "message": "Đổi mật khẩu thành công. Các phiên đăng nhập khác đã bị đăng xuất",
  "data": {
    "userId": "usr_2",
    "passwordChangedAt": "2026-09-16T14:30:00"
  }
}
```

#### Response 400 Bad Request (Sai mật khẩu hiện tại):
```json
{
  "statusCode": 400,
  "errorCode": "INVALID_CURRENT_PASSWORD",
  "message": "Mật khẩu hiện tại không chính xác"
}
```

#### Response 400 Bad Request (Mật khẩu mới không khớp xác nhận):
```json
{
  "statusCode": 400,
  "errorCode": "VALIDATION_FAILED",
  "message": "Dữ liệu đầu vào không hợp lệ",
  "errors": {
    "confirmPassword": "Mật khẩu xác nhận không khớp với mật khẩu mới"
  }
}
```

#### Response 400 Bad Request (Mật khẩu mới không đạt chính sách):
```json
{
  "statusCode": 400,
  "errorCode": "VALIDATION_FAILED",
  "message": "Dữ liệu đầu vào không hợp lệ",
  "errors": {
    "newPassword": "Mật khẩu phải có tối thiểu 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt"
  }
}
```

#### Response 400 Bad Request (Mật khẩu mới trùng mật khẩu cũ):
```json
{
  "statusCode": 400,
  "errorCode": "PASSWORD_SAME_AS_OLD",
  "message": "Mật khẩu mới không được trùng với mật khẩu hiện tại"
}
```

#### Response 401 Unauthorized (Token hết hạn / không hợp lệ):
```json
{
  "statusCode": 401,
  "errorCode": "UNAUTHORIZED",
  "message": "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại"
}
```

---

### 2.6. POST `/api/v1/auth/forgot-password`
> **User Story:** *As a user who forgot my password, I want to request a password reset link, so that I can regain access to my account.*

- **Mô tả:** Gửi yêu cầu đặt lại mật khẩu. Hệ thống sẽ gửi email chứa link reset kèm token một lần (single-use, time-limited).
- **Yêu cầu Auth:** Public.
- **Bảo mật chống User Enumeration:** Response **luôn trả 200** bất kể email có tồn tại trong hệ thống hay không.
- **Rate Limit:** Tối đa **3 request / email / giờ** để chống spam/brute-force.

#### Request Headers:
```http
Content-Type: application/json
```

#### Request Body:
```json
{
  "email": "nguyenvana@gmail.com"
}
```
| Trường | Kiểu | Bắt buộc | Ràng buộc |
| :--- | :---: | :---: | :--- |
| `email` | String | Có | Định dạng email hợp lệ (RFC 5322). |

#### Luồng xử lý Backend:
1. Validate định dạng email → Sai format → trả `400 VALIDATION_FAILED`.
2. Tìm user theo email trong bảng `customer_profiles` hoặc `technician_profiles`.
   - **Email không tồn tại** → **KHÔNG** trả lỗi. Vẫn trả response 200 giống hệt (chống user enumeration). Không gửi email.
   - **Email tồn tại** → Tiếp tục bước 3.
3. Kiểm tra rate limit: nếu đã gửi ≥ 3 lần trong 1 giờ → trả `429 RATE_LIMIT_EXCEEDED`.
4. Tạo `resetToken` = UUID v4 ngẫu nhiên.
5. Lưu vào bảng `password_reset_tokens`:
   ```
   | token (PK)    | user_id (FK) | expires_at              | is_used | created_at              |
   | :------------ | :----------- | :---------------------- | :-----: | :---------------------- |
   | uuid-v4-token | usr_2        | NOW() + 15 MINUTES      | false   | NOW()                   |
   ```
6. Hủy tất cả token cũ chưa dùng của user đó (`is_used = false AND user_id = :userId`) → chỉ giữ token mới nhất.
7. Gửi email chứa link: `https://fixlink.vn/reset-password?token=<resetToken>`.
8. Trả response 200.

#### Response 200 OK (Luôn trả — bất kể email có tồn tại):
```json
{
  "statusCode": 200,
  "message": "Nếu email đã được đăng ký trong hệ thống, một liên kết đặt lại mật khẩu đã được gửi đến hộp thư của bạn. Vui lòng kiểm tra email (bao gồm thư mục Spam) trong vòng 15 phút."
}
```

#### Response 400 Bad Request (Sai định dạng email):
```json
{
  "statusCode": 400,
  "errorCode": "VALIDATION_FAILED",
  "message": "Dữ liệu đầu vào không hợp lệ",
  "errors": {
    "email": "Địa chỉ email không đúng định dạng"
  }
}
```

#### Response 429 Too Many Requests (Rate limit):
```json
{
  "statusCode": 429,
  "errorCode": "RATE_LIMIT_EXCEEDED",
  "message": "Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau 1 giờ"
}
```

---

### 2.7. POST `/api/v1/auth/reset-password`
> **User Story:** *As a user with a valid reset link, I want to set a new password, so that I can access my account again.*

- **Mô tả:** Đặt lại mật khẩu mới bằng token nhận được qua email. Token chỉ dùng được **một lần** và có hiệu lực **15 phút** kể từ lúc tạo.
- **Yêu cầu Auth:** Public (xác thực qua `token` trong request body thay vì JWT).

#### Request Headers:
```http
Content-Type: application/json
```

#### Request Body:
```json
{
  "token": "d9b2d63d-ce7a-4286-9a29-bc8c4146a482",
  "newPassword": "NewSecure@789",
  "confirmPassword": "NewSecure@789"
}
```
| Trường | Kiểu | Bắt buộc | Ràng buộc |
| :--- | :---: | :---: | :--- |
| `token` | String | Có | UUID v4 nhận từ email. Không được để trống. |
| `newPassword` | String | Có | Tuân thủ Password Policy (Mục 1.5). |
| `confirmPassword` | String | Có | Phải giống hệt `newPassword`. |

#### Luồng xử lý Backend:
1. Validate input: `token`, `newPassword`, `confirmPassword` không được trống.
2. Kiểm tra `newPassword == confirmPassword` → Không khớp → `400 VALIDATION_FAILED`.
3. Kiểm tra `newPassword` tuân thủ Password Policy → Không đạt → `400 VALIDATION_FAILED`.
4. Tìm token trong bảng `password_reset_tokens`:
   - **Không tồn tại** → `400 INVALID_RESET_TOKEN`.
   - **`is_used = true`** → `400 INVALID_RESET_TOKEN` (message: "Token đã được sử dụng").
   - **`expires_at < NOW()`** → `400 INVALID_RESET_TOKEN` (message: "Token đã hết hạn").
5. Lấy `user_id` từ token record → truy vấn `users` lấy `password_hash` hiện tại.
6. Kiểm tra `newPassword` không trùng mật khẩu cũ: `BCrypt.checkpw(newPassword, oldHash)`.
   - Trùng → `400 PASSWORD_SAME_AS_OLD`.
7. Hash mật khẩu mới: `BCrypt.hashpw(newPassword, BCrypt.gensalt(12))`.
8. **Bắt đầu transaction:**
   a. Cập nhật `users.password_hash` và `users.updated_at`.
   b. Đánh dấu token: `is_used = true`.
   c. **Xóa toàn bộ refresh tokens** của user → buộc đăng nhập lại trên mọi thiết bị.
   d. **Thêm tất cả access token đang active vào blacklist** (nếu dùng cơ chế token blacklist).
9. **Commit transaction.**
10. Trả response thành công.

#### Response 200 OK (Thành công):
```json
{
  "statusCode": 200,
  "message": "Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới",
  "data": {
    "userId": "usr_2",
    "passwordResetAt": "2026-09-16T14:45:00"
  }
}
```

#### Response 400 Bad Request (Token không hợp lệ / hết hạn / đã dùng):
```json
{
  "statusCode": 400,
  "errorCode": "INVALID_RESET_TOKEN",
  "message": "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu gửi lại"
}
```

#### Response 400 Bad Request (Mật khẩu mới trùng mật khẩu cũ):
```json
{
  "statusCode": 400,
  "errorCode": "PASSWORD_SAME_AS_OLD",
  "message": "Mật khẩu mới không được trùng với mật khẩu hiện tại"
}
```

#### Response 400 Bad Request (Mật khẩu mới không khớp xác nhận):
```json
{
  "statusCode": 400,
  "errorCode": "VALIDATION_FAILED",
  "message": "Dữ liệu đầu vào không hợp lệ",
  "errors": {
    "confirmPassword": "Mật khẩu xác nhận không khớp với mật khẩu mới"
  }
}
```

#### Response 400 Bad Request (Mật khẩu mới không đạt chính sách):
```json
{
  "statusCode": 400,
  "errorCode": "VALIDATION_FAILED",
  "message": "Dữ liệu đầu vào không hợp lệ",
  "errors": {
    "newPassword": "Mật khẩu phải có tối thiểu 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt"
  }
}
```

---

### 2.8. POST `/api/v1/auth/logout`
> **User Story:** *As a logged-in user, I want to log out, so that my session is invalidated and no one can reuse my token.*

- **Mô tả:** Đăng xuất phiên hiện tại. Access token bị thêm vào blacklist phía server, refresh token bị xóa. Token sau khi logout không thể tái sử dụng.
- **Yêu cầu Auth:** Bắt buộc (`Authorization: Bearer <JWT_TOKEN>`).
- **Áp dụng cho:** Mọi Actor.

#### Request Headers:
```http
Content-Type: application/json
Authorization: Bearer <accessToken>
```

#### Request Body:
```json
{
  "refreshToken": "d9b2d63d-ce7a-4286-9a29-bc8c4146a482"
}
```
| Trường | Kiểu | Bắt buộc | Ràng buộc |
| :--- | :---: | :---: | :--- |
| `refreshToken` | String | Có | Refresh token của phiên đăng nhập hiện tại. Không được để trống. |

#### Luồng xử lý Backend:
1. Trích xuất `userId` và `jti` (JWT ID) từ access token trong header.
2. Validate `refreshToken` trong request body: tìm trong bảng `refresh_tokens` theo `token = :refreshToken AND user_id = :userId`.
   - Không tìm thấy → vẫn tiếp tục blacklist access token (fail-safe).
3. **Thêm access token vào blacklist:**
   - Lưu vào bảng `token_blacklist` hoặc Redis SET:
     ```
     | jti (PK)      | user_id | expires_at (= JWT exp) | blacklisted_at |
     | :------------ | :------ | :--------------------- | :------------- |
     | jwt-id-string | usr_2   | 2026-09-17T14:30:00    | NOW()          |
     ```
   - TTL trong Redis/DB = thời gian còn lại của access token (tránh tích tụ vĩnh viễn).
4. **Xóa refresh token** khỏi bảng `refresh_tokens`: `DELETE WHERE token = :refreshToken`.
5. **Xóa device token** (nếu có): `UPDATE users SET device_token = NULL WHERE id = :userId` (ngừng gửi push notification).
6. Trả response thành công.

#### Response 200 OK (Thành công):
```json
{
  "statusCode": 200,
  "message": "Đăng xuất thành công"
}
```

#### Response 401 Unauthorized (Token đã hết hạn hoặc bị blacklist):
```json
{
  "statusCode": 401,
  "errorCode": "UNAUTHORIZED",
  "message": "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại"
}
```

> **Lưu ý cho Frontend:** Sau khi nhận response 200, FE phải:
> 1. Xóa `accessToken` và `refreshToken` khỏi `localStorage` / `cookie`.
> 2. Redirect về trang Login.
> 3. Clear mọi state/cache liên quan đến user.

---

### 2.9. POST `/api/v1/auth/refresh-token`
> **User Story:** *As a logged-in user whose access token is about to expire, I want to refresh it, so that I can continue using the app without re-entering credentials.*

- **Mô tả:** Làm mới (refresh) access token đã hết hạn bằng refresh token còn hiệu lực. Áp dụng cơ chế **Token Rotation** — mỗi lần refresh tạo cặp token mới hoàn toàn, hủy cặp cũ.
- **Yêu cầu Auth:** Public (xác thực qua `refreshToken` trong request body thay vì JWT header).

#### Cấu hình Token Lifecycle:
| Loại token | Thời hạn (TTL) | Ghi chú |
| :--- | :---: | :--- |
| `accessToken` (JWT) | **24 giờ** (86400s) | Chứa `userId`, `role`, `iat`, `exp`, `jti`. Ký bằng HS512. |
| `refreshToken` (UUID) | **7 ngày** (604800s) | Lưu phía server trong bảng `refresh_tokens`. |

#### Request Headers:
```http
Content-Type: application/json
```

#### Request Body:
```json
{
  "refreshToken": "d9b2d63d-ce7a-4286-9a29-bc8c4146a482"
}
```
| Trường | Kiểu | Bắt buộc | Ràng buộc |
| :--- | :---: | :---: | :--- |
| `refreshToken` | String | Có | Refresh token nhận từ response Login hoặc lần Refresh trước đó. |

#### Luồng xử lý Backend (Token Rotation):
1. Tìm `refreshToken` trong bảng `refresh_tokens`:
   - **Không tồn tại** → `400 INVALID_REFRESH_TOKEN`. Có thể đã bị thu hồi (compromised token replay).
   - **`expires_at < NOW()`** → `400 INVALID_REFRESH_TOKEN` (message: "Refresh token đã hết hạn").
2. Lấy thông tin user: `userId`, `role`, `status` từ bảng `users`.
3. Kiểm tra trạng thái tài khoản:
   - `status = BLOCKED` → `403 ACCOUNT_BLOCKED` (tài khoản bị khóa không cho refresh).
4. **Xóa refresh token cũ:** `DELETE FROM refresh_tokens WHERE token = :oldRefreshToken`.
5. **Tạo cặp token mới:**
   - `newAccessToken` = JWT HS512 mới với `jti` mới, `exp = NOW() + 24h`.
   - `newRefreshToken` = UUID v4 mới, `expires_at = NOW() + 7d`.
6. **Lưu refresh token mới** vào bảng `refresh_tokens`:
   ```
   | token (PK)          | user_id | expires_at          | created_at |
   | :------------------ | :------ | :------------------ | :--------- |
   | new-uuid-v4-token   | usr_2   | NOW() + 7 DAYS      | NOW()      |
   ```
7. Trả cặp token mới cho client.

#### Response 200 OK (Thành công):
```json
{
  "statusCode": 200,
  "message": "Làm mới token thành công",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjdXN0b21lcjAxIiwidXNlcklkIjoyLCJyb2xlIjoiQ1VTVE9NRVIiLCJqdGkiOiJuZXctand0LWlkIiwiaWF0IjoxNzg5...",
    "refreshToken": "f8a3e72b-1c4d-4e5f-a6b7-8c9d0e1f2a3b",
    "tokenType": "Bearer",
    "expiresIn": 86400
  }
}
```

#### Response 400 Bad Request (Refresh token không hợp lệ / hết hạn / đã bị thu hồi):
```json
{
  "statusCode": 400,
  "errorCode": "INVALID_REFRESH_TOKEN",
  "message": "Phiên đăng nhập đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại"
}
```

#### Response 403 Forbidden (Tài khoản bị khóa):
```json
{
  "statusCode": 403,
  "errorCode": "ACCOUNT_BLOCKED",
  "message": "Tài khoản của bạn đã bị khóa do vi phạm chính sách"
}
```

> **Lưu ý cho Frontend:**
> 1. Khi `accessToken` hết hạn (nhận `401 UNAUTHORIZED` từ bất kỳ API nào), FE gọi `POST /api/v1/auth/refresh-token` kèm `refreshToken` đã lưu.
> 2. Nếu refresh thành công → cập nhật cả `accessToken` và `refreshToken` mới vào `localStorage` / `cookie`.
> 3. Retry request gốc với `accessToken` mới.
> 4. Nếu refresh thất bại (`400` hoặc `403`) → xóa token, redirect về Login.

---

## 3. MODULE 2: QUẢN TRỊ NGƯỜI DÙNG & DUYỆT KYC (ADMIN MANAGEMENT)

### 3.1. GET `/api/v1/admin/users`
- **Mô tả:** Admin lấy danh sách người dùng toàn hệ thống có đầy đủ **Phân trang, Bộ lọc vai trò, Bộ lọc trạng thái, Tìm kiếm từ khóa và Sắp xếp**.
- **Yêu cầu Auth:** Bắt buộc quyền `ADMIN` (`Authorization: Bearer <ADMIN_TOKEN>`).

#### Request Query Parameters:
```
GET /api/v1/admin/users?page=1&limit=10&search=Trần&role=TECHNICIAN&status=PENDING&sortBy=createdAt&sortOrder=DESC
```
| Parameter | Type | Required | Default | Example | Mô tả chi tiết |
| :--- | :---: | :---: | :---: | :---: | :--- |
| `page` | Integer | Không | `1` | `1` | Trang hiện tại (1-based index). |
| `limit` | Integer | Không | `10` | `10` | Số bản ghi hiển thị trên mỗi trang. |
| `search` | String | Không | `null` | `Trần Văn` | Tìm kiếm theo Tên, Username, Email, Số điện thoại. |
| `role` | String | Không | `ALL` | `TECHNICIAN` | Lọc vai trò: `ALL`, `CUSTOMER`, `TECHNICIAN`, `ADMIN`, `STAFF`. |
| `status` | String | Không | `ALL` | `PENDING` | Lọc trạng thái: `ALL`, `ACTIVE`, `BLOCKED`, `PENDING`. |
| `sortBy` | String | Không | `createdAt` | `createdAt` | Trường sắp xếp: `createdAt`, `username`. |
| `sortOrder` | String | Không | `DESC` | `DESC` | Hướng sắp xếp: `ASC` (Tăng dần), `DESC` (Giảm dần). |

#### Response 200 OK (Thành công):
```json
{
  "statusCode": 200,
  "message": "Lấy danh sách người dùng thành công",
  "meta": {
    "currentPage": 1,
    "limit": 10,
    "totalItems": 15,
    "totalPages": 2,
    "hasNext": true,
    "hasPrevious": false
  },
  "data": [
    {
      "id": "usr_5",
      "username": "tho_dien_lanh_02",
      "role": "TECHNICIAN",
      "status": "PENDING",
      "createdAt": "2026-09-14T19:55:00",
      "technicianProfile": {
        "fullName": "Trần Văn B",
        "phone": "0987654321",
        "email": "tranvanb@gmail.com",
        "citizenId": "012345678901",
        "avatarUrl": null,
        "verificationStatus": "PENDING",
        "yearsExperience": 5,
        "avgRating": 0.0,
        "completedJobs": 0,
        "walletBalance": 0,
        "isOnline": false
      }
    },
    {
      "id": "usr_2",
      "username": "customer01",
      "role": "CUSTOMER",
      "status": "ACTIVE",
      "createdAt": "2026-09-14T19:40:00",
      "customerProfile": {
        "fullName": "Nguyễn Văn A",
        "phone": "0901234567",
        "email": "nguyenvana@gmail.com",
        "avatarUrl": "https://s3.fixlink.vn/avatars/usr_customer01.jpg",
        "membershipTier": "VIP"
      }
    }
  ]
}
```

---

### 3.2. GET `/api/v1/admin/users/{id}`
- **Mô tả:** Xem thông tin hồ sơ chi tiết của một người dùng theo ID.
- **Yêu cầu Auth:** Bắt buộc quyền `ADMIN`.
- **Path Parameter:** `id` (Long hoặc chuỗi `usr_11223344`).

#### Response 200 OK:
```json
{
  "statusCode": 200,
  "message": "Thành công",
  "data": {
    "id": "usr_5",
    "username": "tho_dien_lanh_02",
    "role": "TECHNICIAN",
    "status": "PENDING",
    "createdAt": "2026-09-14T19:55:00",
    "technicianProfile": {
      "fullName": "Trần Văn B",
      "phone": "0987654321",
      "email": "tranvanb@gmail.com",
      "citizenId": "012345678901",
      "verificationStatus": "PENDING",
      "yearsExperience": 5,
      "avgRating": 0.0,
      "completedJobs": 0,
      "walletBalance": 0
    }
  }
}
```

---

### 3.3. PATCH `/api/v1/admin/users/{id}/status`
- **Mô tả:** Admin khóa hoặc mở khóa tài khoản người dùng vi phạm quy định.
- **Yêu cầu Auth:** Bắt buộc quyền `ADMIN`.

#### Request Body:
```json
{
  "status": "BLOCKED",
  "reason": "Phát hiện hành vi giao dịch ngoài sàn bỏ qua phí Escrow"
}
```

#### Response 200 OK:
```json
{
  "statusCode": 200,
  "message": "Cập nhật trạng thái tài khoản thành công",
  "data": {
    "userId": "usr_5",
    "status": "BLOCKED"
  }
}
```

---

### 3.4. PATCH `/api/v1/admin/technicians/{userId}/verify`
- **Mô tả:** Admin phê duyệt (`APPROVED`) hoặc từ chối (`REJECTED`) xác minh hồ sơ KYC Thợ.
- **Yêu cầu Auth:** Bắt buộc quyền `ADMIN`.
- **Path Parameter:** `userId` (Ví dụ: `usr_5` hoặc `5`).

#### Request Body - Trường hợp Phê Duyệt (`APPROVED`):
```json
{
  "verificationStatus": "APPROVED",
  "note": "CCCD và bằng nghề điện lạnh hợp lệ"
}
```

#### Request Body - Trường hợp Từ Chối (`REJECTED`):
```json
{
  "verificationStatus": "REJECTED",
  "rejectionReason": "Ảnh CCCD mặt sau bị mờ, không rõ số nhận diện"
}
```

#### Response 200 OK:
```json
{
  "statusCode": 200,
  "message": "Cập nhật trạng thái xác minh thợ thành công",
  "data": {
    "userId": "usr_5",
    "verificationStatus": "APPROVED",
    "verifiedAt": "2026-09-14T20:10:00",
    "verifiedBy": "admin_usr_1"
  }
}
```

---

## 4. MODULE 3: DANH MỤC DỊCH VỤ & ĐỊA BÀN (MASTER DATA)

### 4.1. GET `/api/v1/categories`
- **Mô tả:** Danh sách các danh mục ngành nghề sửa chữa (Điện gia dụng, Điện lạnh, Nước dân dụng,...) dành cho Khách hàng chọn lúc đăng việc hoặc Thợ đăng ký kỹ năng.
- **Yêu cầu Auth:** Public.

#### Request Query Parameters:
```
GET /api/v1/categories?page=1&limit=10&search=Điện&isActive=true&sortBy=name&sortOrder=ASC
```
| Parameter | Type | Required | Default | Example |
| :--- | :---: | :---: | :---: | :--- |
| `page` | Integer | Không | `1` | `1` |
| `limit` | Integer | Không | `10` | `10` |
| `search` | String | Không | `null` | `Lạnh` |
| `isActive` | Boolean | Không | `true` | `true` |
| `sortBy` | String | Không | `name` | `name` |
| `sortOrder` | String | Không | `ASC` | `ASC` |

#### Response 200 OK:
```json
{
  "statusCode": 200,
  "message": "Lấy danh mục dịch vụ thành công",
  "meta": {
    "currentPage": 1,
    "limit": 10,
    "totalItems": 4,
    "totalPages": 1,
    "hasNext": false,
    "hasPrevious": false
  },
  "data": [
    {
      "id": 1,
      "name": "Sửa chữa Điện lạnh",
      "iconUrl": "https://s3.fixlink.vn/icons/dien-lanh.png",
      "description": "Máy lạnh, tủ lạnh, máy giặt",
      "isActive": true
    },
    {
      "id": 2,
      "name": "Sửa chữa Điện dân dụng",
      "iconUrl": "https://s3.fixlink.vn/icons/dien-dan-dung.png",
      "description": "Hệ thống điện nhà, chập cháy, aptomat",
      "isActive": true
    },
    {
      "id": 3,
      "name": "Sửa chữa Ống nước & Máy bơm",
      "iconUrl": "https://s3.fixlink.vn/icons/ong-nuoc.png",
      "description": "Rò rỉ đường ống, thay van, máy bơm nước",
      "isActive": true
    }
  ]
}
```

---

### 4.2. POST `/api/v1/admin/categories`
- **Mô tả:** Admin tạo mới một danh mục dịch vụ.
- **Yêu cầu Auth:** Bắt buộc quyền `ADMIN`.

#### Request Body:
```json
{
  "name": "Khóa & Cửa cuốn",
  "iconUrl": "https://s3.fixlink.vn/icons/khoa-cua.png",
  "description": "Sửa khóa cửa nhà, khóa vân tay, cửa cuốn tự động"
}
```
| Trường | Kiểu | Bắt buộc | Ràng buộc |
| :--- | :---: | :---: | :--- |
| `name` | String | Có | 2 - 100 ký tự. Không được trùng với danh mục đã tồn tại (case-insensitive). |
| `iconUrl` | String | Không | URL hợp lệ (http:// hoặc https://). |
| `description` | String | Không | Tối đa 500 ký tự. |

#### Response 201 Created:
```json
{
  "statusCode": 201,
  "message": "Tạo danh mục thành công",
  "data": {
    "id": 4,
    "name": "Khóa & Cửa cuốn",
    "iconUrl": "https://s3.fixlink.vn/icons/khoa-cua.png",
    "description": "Sửa khóa cửa nhà, khóa vân tay, cửa cuốn tự động",
    "isActive": true,
    "createdAt": "2026-09-16T15:00:00",
    "createdBy": "admin_usr_1"
  }
}
```

#### Response 400 Bad Request (Tên trùng lặp):
```json
{
  "statusCode": 400,
  "errorCode": "DUPLICATE_CATEGORY_NAME",
  "message": "Tên danh mục đã tồn tại trong hệ thống",
  "errors": {
    "name": "Danh mục 'Khóa & Cửa cuốn' đã tồn tại"
  }
}
```

---

### 4.3. GET `/api/v1/admin/categories/{id}`
> **User Story:** *As an admin, I want to view, update, and delete service categories, so that the service catalog stays accurate and up-to-date.*

- **Mô tả:** Xem chi tiết một danh mục dịch vụ theo ID, bao gồm thông tin audit trail và số lượng thợ/yêu cầu đang sử dụng.
- **Yêu cầu Auth:** Bắt buộc quyền `ADMIN`.
- **Path Parameter:** `id` (Integer) — ID của danh mục dịch vụ.

#### Request:
```
GET /api/v1/admin/categories/1
```

#### Response 200 OK:
```json
{
  "statusCode": 200,
  "message": "Lấy chi tiết danh mục thành công",
  "data": {
    "id": 1,
    "name": "Sửa chữa Điện lạnh",
    "iconUrl": "https://s3.fixlink.vn/icons/dien-lanh.png",
    "description": "Máy lạnh, tủ lạnh, máy giặt",
    "isActive": true,
    "createdAt": "2026-09-14T10:00:00",
    "createdBy": "admin_usr_1",
    "updatedAt": "2026-09-15T08:30:00",
    "updatedBy": "admin_usr_1",
    "statistics": {
      "totalTechnicians": 12,
      "totalRepairRequests": 45,
      "activeRepairRequests": 3
    }
  }
}
```

#### Response 404 Not Found:
```json
{
  "statusCode": 404,
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "Không tìm thấy danh mục dịch vụ với ID = 999"
}
```

---

### 4.4. PUT `/api/v1/admin/categories/{id}`

- **Mô tả:** Admin cập nhật thông tin danh mục dịch vụ (tên, icon, mô tả, trạng thái kích hoạt).
- **Yêu cầu Auth:** Bắt buộc quyền `ADMIN`.
- **Path Parameter:** `id` (Integer) — ID của danh mục dịch vụ cần cập nhật.

#### Request Headers:
```http
Content-Type: application/json
Authorization: Bearer <ADMIN_TOKEN>
```

#### Request Body:
```json
{
  "name": "Sửa chữa Điện lạnh & Điều hòa",
  "iconUrl": "https://s3.fixlink.vn/icons/dien-lanh-v2.png",
  "description": "Máy lạnh, tủ lạnh, máy giặt, điều hòa trung tâm",
  "isActive": true
}
```
| Trường | Kiểu | Bắt buộc | Ràng buộc |
| :--- | :---: | :---: | :--- |
| `name` | String | Có | 2 - 100 ký tự. Không trùng danh mục khác (trừ chính nó, case-insensitive). |
| `iconUrl` | String | Không | URL hợp lệ (http:// hoặc https://). Để `null` giữ nguyên icon cũ. |
| `description` | String | Không | Tối đa 500 ký tự. Để `null` giữ nguyên mô tả cũ. |
| `isActive` | Boolean | Không | `true` hoặc `false`. Mặc định giữ nguyên nếu không gửi. |

#### Luồng xử lý Backend:
1. Tìm danh mục theo `id` → Không tồn tại → `404 RESOURCE_NOT_FOUND`.
2. Kiểm tra danh mục đã bị soft-delete (`deleted_at IS NOT NULL`) → `404 RESOURCE_NOT_FOUND`.
3. Kiểm tra tên trùng lặp: `SELECT COUNT(*) FROM service_categories WHERE LOWER(name) = LOWER(:newName) AND id != :currentId AND deleted_at IS NULL`.
   - Trùng → `400 DUPLICATE_CATEGORY_NAME`.
4. Cập nhật các trường thay đổi.
5. Ghi audit trail: `updated_at = NOW()`, `updated_by = :adminUserId`.
6. Trả response.

#### Response 200 OK (Thành công):
```json
{
  "statusCode": 200,
  "message": "Cập nhật danh mục thành công",
  "data": {
    "id": 1,
    "name": "Sửa chữa Điện lạnh & Điều hòa",
    "iconUrl": "https://s3.fixlink.vn/icons/dien-lanh-v2.png",
    "description": "Máy lạnh, tủ lạnh, máy giặt, điều hòa trung tâm",
    "isActive": true,
    "updatedAt": "2026-09-16T15:10:00",
    "updatedBy": "admin_usr_1"
  }
}
```

#### Response 400 Bad Request (Tên trùng lặp):
```json
{
  "statusCode": 400,
  "errorCode": "DUPLICATE_CATEGORY_NAME",
  "message": "Tên danh mục đã tồn tại trong hệ thống",
  "errors": {
    "name": "Danh mục 'Sửa chữa Điện lạnh & Điều hòa' đã tồn tại"
  }
}
```

#### Response 400 Bad Request (Validation lỗi):
```json
{
  "statusCode": 400,
  "errorCode": "VALIDATION_FAILED",
  "message": "Dữ liệu đầu vào không hợp lệ",
  "errors": {
    "name": "Tên danh mục không được để trống và phải từ 2 đến 100 ký tự"
  }
}
```

#### Response 404 Not Found:
```json
{
  "statusCode": 404,
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "Không tìm thấy danh mục dịch vụ với ID = 999"
}
```

---

### 4.5. DELETE `/api/v1/admin/categories/{id}`

- **Mô tả:** Admin xóa một danh mục dịch vụ. Áp dụng cơ chế **Soft Delete** — danh mục không bị xóa vật lý mà được đánh dấu `deleted_at` và tự động `isActive = false`.
- **Yêu cầu Auth:** Bắt buộc quyền `ADMIN`.
- **Path Parameter:** `id` (Integer) — ID của danh mục dịch vụ cần xóa.

#### Request:
```
DELETE /api/v1/admin/categories/4
```

#### Request Headers:
```http
Authorization: Bearer <ADMIN_TOKEN>
```

#### Luồng xử lý Backend:
1. Tìm danh mục theo `id` → Không tồn tại → `404 RESOURCE_NOT_FOUND`.
2. Kiểm tra danh mục đã bị soft-delete (`deleted_at IS NOT NULL`) → `404 RESOURCE_NOT_FOUND` (coi như không tồn tại).
3. Kiểm tra ràng buộc dữ liệu liên quan (thông tin cảnh báo — **KHÔNG chặn xóa**):
   - Đếm số thợ đang liên kết: `SELECT COUNT(*) FROM technician_categories WHERE category_id = :id`.
   - Đếm số yêu cầu sửa chữa đang mở: `SELECT COUNT(*) FROM repair_requests WHERE category_id = :id AND status NOT IN ('COMPLETED', 'CANCELLED')`.
4. **Thực hiện Soft Delete:**
   a. `UPDATE service_categories SET deleted_at = NOW(), deleted_by = :adminUserId, is_active = false, updated_at = NOW(), updated_by = :adminUserId WHERE id = :id`.
5. Trả response kèm thông tin impact.

#### Response 200 OK (Thành công):
```json
{
  "statusCode": 200,
  "message": "Xóa danh mục thành công",
  "data": {
    "id": 4,
    "name": "Khóa & Cửa cuốn",
    "deletedAt": "2026-09-16T15:20:00",
    "deletedBy": "admin_usr_1",
    "impact": {
      "affectedTechnicians": 3,
      "affectedActiveRequests": 0,
      "note": "3 kỹ thuật viên đã được gỡ liên kết với danh mục này"
    }
  }
}
```

#### Response 404 Not Found:
```json
{
  "statusCode": 404,
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "Không tìm thấy danh mục dịch vụ với ID = 999"
}
```

---

### 4.6. GET `/api/v1/areas`
- **Mô tả:** Danh sách các quận/huyện và tỉnh/thành phố để ghép việc thợ theo khoảng cách/địa bàn.
- **Yêu cầu Auth:** Public.

#### Request Query Parameters:
```
GET /api/v1/areas?page=1&limit=10&city=TP. Hồ Chí Minh&search=Quận 1&sortBy=district&sortOrder=ASC
```
| Parameter | Type | Required | Default | Example |
| :--- | :---: | :---: | :---: | :--- |
| `page` | Integer | Không | `1` | `1` |
| `limit` | Integer | Không | `10` | `10` |
| `city` | String | Không | `ALL` | `TP. Hồ Chí Minh` |
| `search` | String | Không | `null` | `Bình Thạnh` |
| `sortBy` | String | Không | `district` | `district` |
| `sortOrder` | String | Không | `ASC` | `ASC` |

#### Response 200 OK:
```json
{
  "statusCode": 200,
  "message": "Lấy danh sách khu vực thành công",
  "meta": {
    "currentPage": 1,
    "limit": 10,
    "totalItems": 24,
    "totalPages": 3,
    "hasNext": true,
    "hasPrevious": false
  },
  "data": [
    {
      "id": 1,
      "district": "Quận 1",
      "city": "TP. Hồ Chí Minh",
      "isActive": true
    },
    {
      "id": 2,
      "district": "Quận Bình Thạnh",
      "city": "TP. Hồ Chí Minh",
      "isActive": true
    }
  ]
}
```

---

## 5. MODULE 4: QUẢN LÝ HỒ SƠ KỸ THUẬT VIÊN (TECHNICIAN PROFILE MANAGEMENT)

### 5.1. PUT `/api/v1/technicians/me/profile`
> **User Story:** *As a technician, I want to update my own profile (trade, service area, bio), so that customers can find me for the right jobs.*

- **Mô tả:** Kỹ thuật viên tự cập nhật hồ sơ nghề nghiệp của mình, bao gồm thông tin cá nhân, danh mục kỹ năng và khu vực hoạt động. Chỉ owner mới được sửa record của chính mình.
- **Yêu cầu Auth:** Bắt buộc (`Authorization: Bearer <JWT_TOKEN>`). Chỉ role `TECHNICIAN`.
- **Ownership Check:** Backend trích xuất `userId` từ JWT → chỉ cho phép sửa profile tương ứng. Không dùng Path Parameter `/{id}` để tránh IDOR.

#### Request Headers:
```http
Content-Type: application/json
Authorization: Bearer <TECHNICIAN_TOKEN>
```

#### Request Body:
```json
{
  "fullName": "Trần Văn B",
  "phone": "0987654321",
  "email": "tranvanb_updated@gmail.com",
  "bio": "Chuyên gia sửa chữa điều hòa, tủ lạnh công nghiệp, máy giặt – trên 7 năm kinh nghiệm. Cam kết bảo hành 6 tháng.",
  "yearsExperience": 7,
  "avatarUrl": "https://s3.fixlink.vn/avatars/usr_tho_dien_lanh_02_v2.jpg",
  "categoryIds": [1, 2],
  "areaIds": [1, 2, 5]
}
```
| Trường | Kiểu | Bắt buộc | Ràng buộc |
| :--- | :---: | :---: | :--- |
| `fullName` | String | Có | 2 - 100 ký tự. Không để trống. |
| `phone` | String | Có | Regex số điện thoại VN: `^(0[3\|5\|7\|8\|9])[0-9]{8}$`. Không trùng phone user khác. |
| `email` | String | Có | Định dạng email hợp lệ (RFC 5322). Không trùng email user khác. |
| `bio` | String | Không | Tối đa 1000 ký tự. Giới thiệu năng lực nghề nghiệp. |
| `yearsExperience` | Integer | Không | Số nguyên ≥ 0, tối đa 50. Mặc định giữ nguyên nếu không gửi. |
| `avatarUrl` | String | Không | URL hợp lệ (http:// hoặc https://). Để `null` giữ nguyên avatar cũ. |
| `categoryIds` | Integer[] | Không | Mảng ID danh mục dịch vụ. Mỗi ID phải tồn tại trong `service_categories` và `is_active = true`. Để `[]` (mảng rỗng) sẽ xóa tất cả liên kết cũ. Không gửi trường này → giữ nguyên. |
| `areaIds` | Integer[] | Không | Mảng ID khu vực hoạt động. Mỗi ID phải tồn tại trong `service_areas` và `is_active = true`. Để `[]` (mảng rỗng) sẽ xóa tất cả liên kết cũ. Không gửi trường này → giữ nguyên. |

> **Trường KHÔNG được phép sửa (Backend PHẢI ignore nếu client gửi):**
> - `verificationStatus` — Chỉ Admin thay đổi qua API duyệt KYC (Mục 3.4).
> - `avgRating`, `completedJobs` — Hệ thống tự tính toán.
> - `walletBalance` — Chỉ thay đổi qua module tài chính.
> - `citizenId`, `idCardFrontUrl`, `idCardBackUrl` — Sau khi đã APPROVED, không cho phép sửa CCCD. Nếu cần sửa phải liên hệ Admin re-verify.

#### Luồng xử lý Backend:
1. Trích xuất `userId` từ JWT Token.
2. Kiểm tra role = `TECHNICIAN` → Không phải → `403 ACCESS_DENIED`.
3. Truy vấn `technician_profiles WHERE user_id = :userId` → Không tìm thấy → `404 RESOURCE_NOT_FOUND`.
4. Validate từng trường:
   a. `phone` regex → sai → thêm vào `errors.phone`.
   b. `email` format → sai → thêm vào `errors.email`.
   c. Kiểm tra `phone` trùng: `SELECT COUNT(*) FROM technician_profiles WHERE phone = :phone AND user_id != :userId UNION SELECT COUNT(*) FROM customer_profiles WHERE phone = :phone` → trùng → thêm vào `errors.phone`.
   d. Kiểm tra `email` trùng tương tự → trùng → thêm vào `errors.email`.
   e. `yearsExperience < 0 OR > 50` → thêm vào `errors.yearsExperience`.
   f. `categoryIds` — với mỗi ID: `SELECT COUNT(*) FROM service_categories WHERE id = :catId AND is_active = true AND deleted_at IS NULL` → không tồn tại → thêm vào `errors.categoryIds`.
   g. `areaIds` — tương tự check trong `service_areas`.
5. Nếu có bất kỳ lỗi nào → trả `400 VALIDATION_FAILED` kèm tất cả `errors`.
6. **Bắt đầu transaction:**
   a. Cập nhật `technician_profiles`: `fullName`, `phone`, `email`, `bio`, `years_experience`, `avatar_url`, `updated_at = NOW()`, `updated_by = :userId`.
   b. Nếu `categoryIds` được gửi:
      - `DELETE FROM technician_categories WHERE technician_id = :techId`.
      - `INSERT INTO technician_categories (technician_id, category_id)` cho mỗi `categoryIds[i]`.
   c. Nếu `areaIds` được gửi:
      - `DELETE FROM technician_areas WHERE technician_id = :techId`.
      - `INSERT INTO technician_areas (technician_id, area_id)` cho mỗi `areaIds[i]`.
7. **Commit transaction.**
8. Truy vấn lại profile đầy đủ kèm categories và areas để trả response.

#### Response 200 OK (Thành công):
```json
{
  "statusCode": 200,
  "message": "Cập nhật hồ sơ kỹ thuật viên thành công",
  "data": {
    "userId": "usr_5",
    "fullName": "Trần Văn B",
    "phone": "0987654321",
    "email": "tranvanb_updated@gmail.com",
    "bio": "Chuyên gia sửa chữa điều hòa, tủ lạnh công nghiệp, máy giặt – trên 7 năm kinh nghiệm. Cam kết bảo hành 6 tháng.",
    "yearsExperience": 7,
    "avatarUrl": "https://s3.fixlink.vn/avatars/usr_tho_dien_lanh_02_v2.jpg",
    "verificationStatus": "APPROVED",
    "avgRating": 4.8,
    "completedJobs": 23,
    "walletBalance": 1500000,
    "isOnline": true,
    "categories": [
      {
        "id": 1,
        "name": "Sửa chữa Điện lạnh"
      },
      {
        "id": 2,
        "name": "Sửa chữa Điện dân dụng"
      }
    ],
    "areas": [
      {
        "id": 1,
        "district": "Quận 1",
        "city": "TP. Hồ Chí Minh"
      },
      {
        "id": 2,
        "district": "Quận Bình Thạnh",
        "city": "TP. Hồ Chí Minh"
      },
      {
        "id": 5,
        "district": "Quận Phú Nhuận",
        "city": "TP. Hồ Chí Minh"
      }
    ],
    "updatedAt": "2026-09-16T15:30:00"
  }
}
```

#### Response 400 Bad Request (Validation lỗi đa trường):
```json
{
  "statusCode": 400,
  "errorCode": "VALIDATION_FAILED",
  "message": "Dữ liệu đầu vào không hợp lệ",
  "errors": {
    "phone": "Số điện thoại không đúng định dạng (VD: 0912345678)",
    "email": "Email đã được sử dụng bởi tài khoản khác",
    "yearsExperience": "Số năm kinh nghiệm phải từ 0 đến 50",
    "categoryIds": "Danh mục ID = 99 không tồn tại hoặc đã bị vô hiệu hóa"
  }
}
```

#### Response 401 Unauthorized:
```json
{
  "statusCode": 401,
  "errorCode": "UNAUTHORIZED",
  "message": "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại"
}
```

#### Response 403 Forbidden (Không phải Technician):
```json
{
  "statusCode": 403,
  "errorCode": "ACCESS_DENIED",
  "message": "Chỉ kỹ thuật viên mới có quyền cập nhật hồ sơ nghề nghiệp"
}
```

#### Response 404 Not Found (Profile chưa tồn tại):
```json
{
  "statusCode": 404,
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "Không tìm thấy hồ sơ kỹ thuật viên. Vui lòng liên hệ quản trị viên"
}
```

---

## 6. HƯỚNG DẪN DÀNH CHO FRONTEND (MOCK UI GUIDE)

### 6.1. Cách gắn Access Token
Sau khi gọi `POST /api/v1/auth/login`, lấy `data.accessToken` và lưu vào `localStorage` hoặc `cookie`. Mọi request sau đó gửi qua Header:
```http
Authorization: Bearer <accessToken>
```

### 6.2. Xử lý phân trang
Frontend căn cứ vào `meta.totalPages`, `meta.currentPage` để hiển thị thanh phân trang `< 1 2 3 ... >`. Khi click sang trang mới, truyền `?page=X&limit=10`.

### 6.3. Mock UI
Frontend có thể copy trực tiếp các đoạn JSON trong phần **Response 200 OK** ở trên để làm Mock Service (sử dụng MSW - Mock Service Worker hoặc Axios Interceptor) để thiết kế giao diện ngay mà không cần đợi Backend.

### 6.4. Xử lý vòng đời Token (Token Lifecycle)

#### A. Khi Access Token hết hạn (nhận 401 từ API bất kỳ):
```
┌──────────────┐    401 UNAUTHORIZED     ┌──────────────────┐
│  Gọi API     │ ──────────────────────► │  Interceptor     │
│  bất kỳ      │                         │  (Axios/Fetch)   │
└──────────────┘                         └───────┬──────────┘
                                                 │
                                    ┌────────────▼────────────┐
                                    │ POST /auth/refresh-token │
                                    │ body: { refreshToken }   │
                                    └────────────┬────────────┘
                                                 │
                                  ┌──────────────┴──────────────┐
                                  │                             │
                            200 OK │                       400/403│
                                  │                             │
                    ┌─────────────▼──────┐          ┌──────────▼─────────┐
                    │ Lưu token mới      │          │ Xóa token          │
                    │ Retry request gốc  │          │ Redirect → Login   │
                    └────────────────────┘          └────────────────────┘
```

#### B. Khi người dùng bấm Đăng xuất:
1. Gọi `POST /api/v1/auth/logout` kèm `refreshToken` trong body.
2. Nhận `200 OK` → Xóa `accessToken` + `refreshToken` khỏi `localStorage`/`cookie`.
3. Clear mọi state/cache ứng dụng.
4. Redirect về trang Login.

#### C. Khi đổi/reset mật khẩu thành công:
- Trên thiết bị hiện tại: giữ nguyên session (nếu change password) hoặc redirect login (nếu reset password).
- Trên các thiết bị khác: tự động bị đăng xuất do refresh token đã bị hủy.

### 6.5. Xử lý lỗi validation per-field (Multi-field errors)
Khi nhận `400 VALIDATION_FAILED`, object `errors` chứa key = tên trường, value = thông báo lỗi. Frontend hiển thị inline error bên dưới mỗi input field tương ứng:
```javascript
// Ví dụ xử lý trong React
if (response.errorCode === 'VALIDATION_FAILED' && response.errors) {
  Object.entries(response.errors).forEach(([field, message]) => {
    setFieldError(field, message); // Gắn lỗi vào form field
  });
}
```

### 6.6. Bảng tổng hợp Token Configuration (cho FE lưu trữ)
| Thông số | Giá trị | Ghi chú |
| :--- | :---: | :--- |
| Access Token TTL | 24 giờ | JWT HS512, chứa `userId`, `role`, `jti` |
| Refresh Token TTL | 7 ngày | UUID v4, lưu server-side |
| Reset Token TTL | 15 phút | UUID v4, single-use |
| Password BCrypt Cost | 12 | Cost factor cho mã hóa mật khẩu |
| Rate Limit (Forgot Password) | 3 lần/giờ/email | Chống spam/brute-force |
