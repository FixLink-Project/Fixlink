# FixLink — Hướng dẫn cấu hình Firebase Storage (Tải ảnh lên Firebase)

> **Áp dụng cho:** ảnh hiện trường của Yêu cầu sửa chữa (`fixlink/repair-requests`) và ảnh CCCD eKYC của thợ (`fixlink/kyc-documents`).
> **Mã Jira liên quan:** `RC-30` (Danh sách yêu cầu sửa chữa: tab trạng thái + phân trang đánh số + upload ảnh), `RC-11`/`RC-29` (eKYC CCCD 2 mặt).

---

## 1. Kiến trúc upload (Direct-to-Cloud)

```
[ Trình duyệt (Web) ]──1. chọn/kéo thả ảnh──►[ Nén ảnh + kiểm tra 5MB ]──2. upload──►[ Firebase Storage Bucket ]
             │                                                                              │
             └──3. gửi URL tải về (mediaUrls) ──►[ Backend Spring Boot ]──4. lưu metadata ►[ bảng media (Flyway V3) ]
```

- Backend **không nhận file nhị phân** → tiết kiệm băng thông/dung lượng server, chỉ lưu URL + thông tin tệp trong bảng `media`.
- Việc phân quyền tệp do **Firebase Storage Security Rules** đảm nhiệm.

---

## 2. Các tệp liên quan trong mã nguồn

| Tệp | Vai trò |
| :--- | :--- |
| `backend/src/main/resources/static/js/firebase-config.js` | Khai báo cấu hình Firebase (placeholder cần điền) |
| `backend/src/main/resources/static/js/firebase-storage.js` | Hàm upload ảnh dùng cho các trang HTML tĩnh (`customer-repair-requests.html`, `admin-dashboard.html`, `technician-register.html`) |
| `backend/src/main/resources/static/js/pagination.js` | Thành phần dùng chung: tab bar + phân trang đánh số |
| `frontend/src/services/firebaseConfig.js` | Cấu hình Firebase cho SPA Vite |
| `frontend/src/services/firebaseStorage.js` | Hàm upload ảnh cho SPA Vite |
| `frontend/src/pages/repairRequestsPage.js` | Trang Yêu cầu sửa chữa của SPA (tab + phân trang đánh số + upload) |

---

## 3. Các bước cấu hình Firebase

1. Truy cập [Firebase Console](https://console.firebase.google.com/) → **Add project** → đặt tên ví dụ `fixlink-platform`.
2. Trong project vừa tạo: **Build → Storage → Get started** → chọn location (ví dụ `asia-southeast1`).
3. **Project settings (⚙️) → Your apps → Web app (</>)** → đăng ký app để nhận `firebaseConfig`.
4. Dán cấu hình vào **cả hai** tệp:
   - `backend/src/main/resources/static/js/firebase-config.js`
   - `frontend/src/services/firebaseConfig.js`

```javascript
export const FIREBASE_CONFIG = {
  apiKey: 'AIzaSy...',
  authDomain: 'fixlink-platform.firebaseapp.com',
  projectId: 'fixlink-platform',
  storageBucket: 'fixlink-platform.appspot.com',
  messagingSenderId: '123456789012',
  appId: '1:123456789012:web:abcdef123456'
};
```

---

## 4. Storage Security Rules đề xuất

Mở **Storage → Rules** và dán cấu hình sau (chỉ cho phép ghi ảnh ≤ 5MB, đọc công khai theo URL):

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /fixlink/repair-requests/{allPaths=**} {
      allow read: if true;
      allow write: if request.resource.size < 5 * 1024 * 1024
                   && request.resource.contentType.matches('image/.*');
    }
    match /fixlink/kyc-documents/{allPaths=**} {
      allow read: if true;
      allow write: if request.resource.size < 5 * 1024 * 1024
                   && request.resource.contentType.matches('image/.*');
    }
  }
}
```

> Khuyến nghị môi trường production: siết `allow read/write` theo **Firebase Authentication** (custom token do backend cấp) thay vì mở công khai.

---

## 5. Chạy không cần tài khoản Firebase (Demo Mode)

Nếu `js/firebase-config.js` vẫn còn placeholder, module `firebase-storage.js` tự động chuyển sang **Demo Mode**:

- Ảnh được nén ngay trên trình duyệt (canvas, tối đa rộng 1280px, JPEG quality 0.82) → trả về **Data URL**.
- Luồng nghiệp vụ (tạo yêu cầu, gắn ảnh bằng chứng, hiển thị gallery) vẫn hoạt động bình thường.
- Trên giao diện hiển thị cảnh báo màu vàng: *"Firebase Storage chưa được cấu hình ... chế độ demo"*.

---

## 6. Kiểm thử nhanh sau khi cấu hình

1. Chạy backend: double-click `run-project.bat` (hoặc `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`).
2. Mở [http://localhost:8080/customer-repair-requests.html](http://localhost:8080/customer-repair-requests.html) → **Đăng yêu cầu mới** → chọn ảnh.
3. Mở **Firebase Console → Storage** và kiểm tra tệp mới trong `fixlink/repair-requests/<ngày>/`.
4. Kiểm tra API: ảnh phải xuất hiện trong `media` của response `GET /api/v1/repair-requests`.

---

## 7. Xử lý sự cố thường gặp

| Hiện tượng | Nguyên nhân & cách xử lý |
| :--- | :--- |
| `Upload thất bại: storage/unauthorized` | Storage Rules chặn ghi — kiểm tra lại mục 4 |
| `storage/unknown` / CORS | Kiểm tra bucket đã bật Storage và `storageBucket` đúng định dạng |
| Ảnh upload xong nhưng không hiển thị | URL Firebase cần quyền đọc công khai (`allow read: if true`) hoặc dùng token download |
| Console báo module CDN không tải được | Máy không có Internet — hệ thống tự chuyển sang Demo Mode |
