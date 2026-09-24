# FIXLINK - TÀI LIỆU ĐẶC TẢ API (API SPECIFICATION)
> **Phiên bản:** v1.0.0 (Chuẩn Sprint 0 & Master Data)  
> **Dự án:** FixLink – Nền tảng kết nối khách hàng và kỹ thuật viên sửa chữa  
> **Mục đích:**  
> - **Backend (BE):** Dùng làm khuôn mẫu triển khai API chuẩn RESTful, cấu trúc dữ liệu chặt chẽ, kiểm soát validation.  
> - **Frontend (FE):** Dùng làm Mock Data xây dựng giao diện UI (Web/Mobile) độc lập mà không cần đợi BE hoàn thiện.  
> - **Tiêu chuẩn thiết kế:** Đáp ứng 100% yêu cầu của Giảng viên hướng dẫn về **Phân trang (Pagination), Bộ lọc (Filter), Tìm kiếm (Search), Sắp xếp (Sort)** và xử lý mã lỗi thống nhất.

---

## MỤC LỤC
1. [Quy Chuẩn Thiết Kế API Chung](#1-quy-chuẩn-thiết-kế-api-chung)
   - [1.1. Base URL & Versioning](#11-base-url--versioning)
   - [1.2. Chuẩn định dạng Response Body](#12-chuẩn-định-dạng-response-body)
   - [1.3. Chuẩn Query Parameters cho API Danh sách (List API)](#13-chuẩn-query-parameters-cho-api-danh-sách-list-api)
   - [1.4. Bảng mã lỗi hệ thống (Standard Error Codes)](#14-bảng-mã-lỗi-hệ-thống-standard-error-codes)
2. [Module 1: Xác Thực & Tài Khoản (Authentication & Profile)](#2-module-1-xác-thực--tài-khoản-authentication--profile)
   - [POST /api/v1/auth/login](#21-post-apiv1authlogin)
   - [POST /api/v1/auth/register/customer](#22-post-apiv1authregistercustomer)
   - [POST /api/v1/auth/register/technician](#23-post-apiv1authregistertechnician)
   - [GET /api/v1/auth/me](#24-get-apiv1authme)
3. [Module 2: Quản Trị Người Dùng & Duyệt KYC (Admin Management)](#3-module-2-quản-trị-người-dùng--duyệt-kyc-admin-management)
   - [GET /api/v1/admin/users](#31-get-apiv1adminusers)
   - [GET /api/v1/admin/users/{id}](#32-get-apiv1adminusersid)
   - [PATCH /api/v1/admin/users/{id}/status](#33-patch-apiv1adminusersidstatus)
   - [PATCH /api/v1/admin/technicians/{userId}/verify](#34-patch-apiv1admintechniciansuseridverify)
4. [Module 3: Danh Mục Dịch Vụ & Địa Bàn (Master Data)](#4-module-3-danh-mục-dịch-vụ--địa-bàn-master-data)
   - [GET /api/v1/categories](#41-get-apiv1categories)
   - [POST /api/v1/admin/categories](#42-post-apiv1admincategories)
   - [GET /api/v1/areas](#43-get-apiv1areas)
5. [Hướng Dẫn Dành Cho Frontend (Mock UI Guide)](#5-hướng-dẫn-dành-cho-frontend-mock-ui-guide)
6. [Module 6: Hồ Sơ Khách Hàng (Customer Profile Self-Service)](#6-module-6-hồ-sơ-khách-hàng-customer-profile-self-service)
   - [GET /api/v1/customers/{userId}/profile](#61-get-apiv1customersuseridprofile)
   - [PUT /api/v1/customers/{userId}/profile](#62-put-apiv1customersuseridprofile)
   - [GET /api/v1/customers/{userId}/audit-trail](#63-get-apiv1customersuseridaudit-trail)
7. [Module 7: Quản Lý Hồ Sơ Kỹ Thuật Viên (Technician Profile Self-Service)](#7-module-7-quản-lý-hồ-sơ-kỹ-thuật-viên--thợ-technician-profile-self-service)
   - [GET /api/v1/technicians/me/profile](#71-get-apiv1techniciansmeprofile)
   - [PUT /api/v1/technicians/me/profile](#72-put-apiv1techniciansmeprofile)
   - [PATCH /api/v1/technicians/me/status/online](#73-patch-apiv1techniciansmestatusonline)

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
| **401** | `INVALID_CREDENTIALS` | Sai username hoặc password khi đăng nhập. |
| **401** | `UNAUTHORIZED` | Token JWT thiếu, hết hạn hoặc không hợp lệ. |
| **403** | `ACCOUNT_BLOCKED` | Tài khoản đã bị Admin khóa vi phạm chính sách. |
| **403** | `ACCESS_DENIED` | Không có quyền hạn truy cập tài nguyên (ví dụ: Khách gọi API Admin). |
| **404** | `RESOURCE_NOT_FOUND` | Không tìm thấy bản ghi theo ID cung cấp. |
| **500** | `INTERNAL_SERVER_ERROR` | Lỗi phát sinh từ phía máy chủ / cơ sở dữ liệu. |

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
| `password` | String | Có | Tối thiểu 6 ký tự |
| `fullName` | String | Có | Không để trống |
| `phone` | String | Có | Regex định dạng số điện thoại Việt Nam |
| `email` | String | Có | Định dạng email hợp lệ |

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
| `password` | String | Có | Tối thiểu 8 ký tự, ít nhất 1 chữ cái in hoa và 1 ký tự đặc biệt. Băm một chiều bằng BCrypt trước khi lưu Database |
| `fullName` | String | Có | Tên đầy đủ của Thợ |
| `phone` | String | Có | Số điện thoại liên hệ |
| `email` | String | Có | Email liên hệ |
| `citizenId` | String | Có | Số CCCD từ 9 - 12 chữ số |
| `idCardFrontUrl` | String | Không | Link ảnh mặt trước CCCD |
| `idCardBackUrl` | String | Không | Link ảnh mặt sau CCCD |
| `bio` | String | Không | Giới thiệu năng lực nghề nghiệp |
| `yearsExperience`| Integer| Không | Số năm kinh nghiệm (Mặc định: 0) |

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

#### Response 201 Created:
```json
{
  "statusCode": 201,
  "message": "Tạo danh mục thành công",
  "data": {
    "id": 4,
    "name": "Khóa & Cửa cuốn",
    "iconUrl": "https://s3.fixlink.vn/icons/khoa-cua.png",
    "isActive": true
  }
}
```

---

### 4.3. GET `/api/v1/areas`
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

## 5. HƯỚNG DẪN DÀNH CHO FRONTEND (MOCK UI GUIDE)

1. **Cách gắn Access Token:**  
   Sau khi gọi `POST /api/v1/auth/login`, lấy `data.accessToken` và lưu vào `localStorage` hoặc `cookie`. Mọi request sau đó gửi qua Header:
   ```http
   Authorization: Bearer <accessToken>
   ```
2. **Xử lý phân trang:**  
   Frontend căn cứ vào `meta.totalPages`, `meta.currentPage` để hiển thị thanh phân trang `< 1 2 3 ... >`. Khi click sang trang mới, truyền `?page=X&limit=10`.
3. **Mock UI:**  
   Frontend có thể copy trực tiếp các đoạn JSON trong phần **Response 200 OK** ở trên để làm Mock Service (sử dụng MSW - Mock Service Worker hoặc Axios Interceptor) để thiết kế giao diện ngay mà không cần đợi Backend.

---

## 6. MODULE KHÁCH HÀNG (CUSTOMER PROFILE - RC-17)

### 6.1. GET /api/v1/customers/{userId}/profile
- **Mô tả:** Lấy thông tin chi tiết hồ sơ cá nhân của khách hàng. Hỗ trợ cả `userId` dạng chuỗi (`usr_1`) hoặc số (`1`).
- **Headers:** `Authorization: Bearer <token>` (hoặc header `X-User-Id: <userId>`)
- **Response 200 OK:**
```json
{
  "statusCode": 200,
  "message": "Lấy thông tin hồ sơ thành công",
  "data": {
    "userId": 1,
    "fullName": "Nguyễn Văn A",
    "phone": "0901234567",
    "email": "nguyenvana@gmail.com",
    "avatarUrl": "https://s3.fixlink.vn/avatars/usr_customer01.jpg",
    "membershipTier": "VIP",
    "createdAt": "2026-09-17T08:00:00",
    "updatedAt": "2026-09-17T08:00:00"
  }
}
```

### 6.2. PUT /api/v1/customers/{userId}/profile
- **Mô tả:** Cập nhật hồ sơ cá nhân của khách hàng (RC-17).
- **Tiêu chí nghiệm thu (Acceptance Criteria):**
  - **AC 1:** Cập nhật hồ sơ chính mình ➔ Thành công, chỉ bản ghi của user đó thay đổi.
  - **AC 2 (Chống IDOR):** Cố tình sửa hồ sơ user khác ➔ Bị từ chối với mã `403 Forbidden` (`ACCESS_DENIED`).
  - **AC 3 (Validation):** Gửi dữ liệu không hợp lệ ➔ Báo lỗi per-field validation `400 Bad Request` (`VALIDATION_FAILED`) và không lưu vào CSDL.
  - **AC 4 (Audit Trail):** Khi lưu thành công, tự động ghi nhận thay đổi vào bảng `audit_logs` (gồm giá trị cũ, giá trị mới, thời gian).
- **Request Headers:**
  - `Authorization: Bearer <token>`
  - `Content-Type: application/json`
- **Request Body:**
```json
{
  "fullName": "Nguyễn Văn A (Đã Cập Nhật)",
  "phone": "0909998888",
  "email": "nguyenvana.new@gmail.com",
  "avatarUrl": "https://images.unsplash.com/photo-avatar.jpg"
}
```
- **Response 200 OK (AC 1 Thành công):**
```json
{
  "statusCode": 200,
  "message": "Cập nhật thông tin hồ sơ thành công",
  "data": {
    "userId": 1,
    "fullName": "Nguyễn Văn A (Đã Cập Nhật)",
    "phone": "0909998888",
    "email": "nguyenvana.new@gmail.com",
    "avatarUrl": "https://images.unsplash.com/photo-avatar.jpg",
    "membershipTier": "VIP",
    "updatedAt": "2026-09-17T09:10:00"
  }
}
```
- **Response 403 Forbidden (AC 2 Chặn IDOR):**
```json
{
  "statusCode": 403,
  "errorCode": "ACCESS_DENIED",
  "message": "Bạn không có quyền chỉnh sửa hồ sơ của người dùng khác (Chặn theo tiêu chí RC-17 AC 2)"
}
```
- **Response 400 Bad Request (AC 3 Lỗi Validation):**
```json
{
  "statusCode": 400,
  "errorCode": "VALIDATION_FAILED",
  "message": "Dữ liệu đầu vào không hợp lệ",
  "errors": {
    "fullName": "Họ và tên không được để trống",
    "phone": "Số điện thoại không đúng định dạng Việt Nam (VD: 0901234567)"
  }
}
```

### 6.3. GET /api/v1/customers/{userId}/audit-trail
- **Mô tả:** Tra cứu lịch sử thay đổi hồ sơ khách hàng (Audit Trail - AC 4).
- **Headers:** `Authorization: Bearer <token>`
- **Response 200 OK:**
```json
{
  "statusCode": 200,
  "message": "Lấy lịch sử kiểm toán thành công",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "action": "UPDATE_CUSTOMER_PROFILE",
      "entityName": "customer_profiles",
      "entityId": "1",
      "oldValues": "{\"fullName\":\"Nguyễn Văn A\",\"phone\":\"0901234567\",\"email\":\"nguyenvana@gmail.com\"}",
      "newValues": "{\"fullName\":\"Nguyễn Văn A (Đã Cập Nhật)\",\"phone\":\"0909998888\",\"email\":\"nguyenvana.new@gmail.com\"}",
      "createdAt": "2026-09-17T09:10:00"
    }
  ]
}
```

---

## 7. MODULE 7: QUẢN LÝ HỒ SƠ KỸ THUẬT VIÊN / THỢ (TECHNICIAN PROFILE SELF-SERVICE)

### 7.1. GET `/api/v1/technicians/me/profile`
- **Mô tả:** Kỹ thuật viên xem thông tin hồ sơ cá nhân, tay nghề, số năm kinh nghiệm, tiểu sử và trạng thái duyệt KYC (`PENDING`, `APPROVED`, `REJECTED`).
- **Headers:** `Authorization: Bearer <token>`
- **Response 200 OK:**
```json
{
  "statusCode": 200,
  "message": "Lấy hồ sơ kỹ thuật viên thành công",
  "data": {
    "userId": 3,
    "fullName": "Trần Văn Thợ Điện Lạnh",
    "phone": "0988776655",
    "email": "thodienlanh@fixlink.vn",
    "citizenId": "079201009999",
    "idCardFrontUrl": "https://images.fixlink.vn/cccd/079201009999_front.jpg",
    "idCardBackUrl": "https://images.fixlink.vn/cccd/079201009999_back.jpg",
    "avatarUrl": "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=400",
    "bio": "Thợ sửa chữa điều hòa, tủ lạnh, máy giặt hơn 8 năm kinh nghiệm tại TP.HCM. Tay nghề cao, trung thực, tận tâm.",
    "yearsExperience": 8,
    "avgRating": 4.95,
    "completedJobs": 142,
    "walletBalance": 3850000.0,
    "verificationStatus": "APPROVED",
    "verifiedAt": "2026-09-15T08:30:00",
    "verifiedBy": "admin_system",
    "rejectionReason": null,
    "isOnline": true
  }
}
```

### 7.2. PUT `/api/v1/technicians/me/profile`
- **Mô tả:** Kỹ thuật viên tự cập nhật thông tin cá nhân (Họ tên, SĐT, Email, Tiểu sử bio, Số năm kinh nghiệm, Avatar).
- **Headers:** `Authorization: Bearer <token>`, `Content-Type: application/json`
- **Request Body:**
```json
{
  "fullName": "Trần Văn Thợ Điện Lạnh (Chuyên Nghiệp)",
  "phone": "0988776655",
  "email": "thodienlanh@fixlink.vn",
  "bio": "Chuyên gia điện lạnh 10 năm kinh nghiệm xử lý inverter và VRV dân dụng.",
  "yearsExperience": 10,
  "avatarUrl": "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=400"
}
```
- **Response 200 OK:**
```json
{
  "statusCode": 200,
  "message": "Cập nhật hồ sơ kỹ thuật viên thành công",
  "data": {
    "userId": 3,
    "fullName": "Trần Văn Thợ Điện Lạnh (Chuyên Nghiệp)",
    "phone": "0988776655",
    "email": "thodienlanh@fixlink.vn",
    "yearsExperience": 10,
    "bio": "Chuyên gia điện lạnh 10 năm kinh nghiệm xử lý inverter và VRV dân dụng.",
    "verificationStatus": "APPROVED",
    "isOnline": true
  }
}
```

### 7.3. PATCH `/api/v1/technicians/me/status/online`
- **Mô tả:** Kỹ thuật viên bật/tắt trạng thái sẵn sàng nhận việc. Chặn (403 FORBIDDEN) nếu tài khoản chưa được duyệt KYC (`verificationStatus != APPROVED`).
- **Headers:** `Authorization: Bearer <token>`
- **Request Body:**
```json
{
  "isOnline": true
}
```
- **Response 200 OK:**
```json
{
  "statusCode": 200,
  "message": "Đã bật chế độ sẵn sàng nhận việc",
  "data": {
    "userId": 3,
    "isOnline": true
  }
}
```
- **Response 403 Forbidden (Khi hồ sơ chưa được duyệt):**
```json
{
  "statusCode": 403,
  "errorCode": "TECHNICIAN_NOT_VERIFIED",
  "message": "Hồ sơ của bạn chưa được Admin phê duyệt (trạng thái hiện tại: PENDING). Vui lòng đợi xét duyệt trước khi nhận việc."
}
```


