/**
 * FixLink - Cấu hình Firebase (Firebase Console > Project settings > Your apps > Web app).
 *
 * HƯỚNG DẪN: Thay các giá trị placeholder bên dưới bằng thông tin dự án Firebase thật của nhóm.
 * Xem tài liệu chi tiết: docs/firebase-storage-setup.md
 *
 * LƯU Ý: Các khóa dưới đây là cấu hình công khai của Firebase Web SDK (không phải bí mật server).
 * Việc bảo vệ dữ liệu do Firebase Storage Security Rules đảm nhiệm, KHÔNG commit file chứa
 * khóa Admin SDK / service account JSON vào repository.
 */
export const FIREBASE_CONFIG = {
  apiKey: '',              // ví dụ: 'AIzaSy...'
  authDomain: '',          // ví dụ: 'fixlink-platform.firebaseapp.com'
  projectId: '',           // ví dụ: 'fixlink-platform'
  storageBucket: '',       // ví dụ: 'fixlink-platform.appspot.com'
  messagingSenderId: '',   // ví dụ: '123456789012'
  appId: ''                // ví dụ: '1:123456789012:web:abcdef123456'
};

/** Thư mục gốc trên Firebase Storage để chứa ảnh hiện trường của các yêu cầu sửa chữa. */
export const FIREBASE_STORAGE_FOLDER = 'fixlink/repair-requests';

/** Thư mục gốc chứa ảnh CCCD 2 mặt phục vụ eKYC của thợ. */
export const FIREBASE_KYC_FOLDER = 'fixlink/kyc-documents';
