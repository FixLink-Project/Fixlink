/**
 * FixLink - Dịch vụ upload ảnh lên Firebase Storage (Firebase Web SDK v10 Modular).
 *
 * Module ESM thuần, dùng được cho các trang HTML tĩnh của Spring Boot (không cần bundler).
 * SDK được nạp động (dynamic import) từ Google CDN đúng 1 lần và cache lại.
 *
 * Chế độ dự phòng (Demo Mode): nếu chưa cấu hình Firebase (js/firebase-config.js còn placeholder),
 * module sẽ nén ảnh ngay trên trình duyệt thành Data URL để luồng nghiệp vụ vẫn chạy được
 * mà không cần tài khoản Firebase. Khi đã cấu hình, ảnh được upload thật lên Firebase Storage.
 */
import {
  FIREBASE_CONFIG,
  FIREBASE_STORAGE_FOLDER,
  FIREBASE_KYC_FOLDER
} from './firebase-config.js';

export { FIREBASE_STORAGE_FOLDER, FIREBASE_KYC_FOLDER };

export const FIREBASE_SDK_VERSION = '10.12.5';
export const MAX_IMAGE_SIZE_MB = 5;
export const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];

const FIREBASE_APP_URL = `https://www.gstatic.com/firebasejs/${FIREBASE_SDK_VERSION}/firebase-app.js`;
const FIREBASE_STORAGE_URL = `https://www.gstatic.com/firebasejs/${FIREBASE_SDK_VERSION}/firebase-storage.js`;

let firebaseStoragePromise = null;

/** Kiểm tra xem nhóm đã cấu hình Firebase Storage thật hay chưa. */
export function isFirebaseConfigured() {
  return Boolean(
    FIREBASE_CONFIG
      && FIREBASE_CONFIG.apiKey
      && FIREBASE_CONFIG.projectId
      && FIREBASE_CONFIG.storageBucket
      && FIREBASE_CONFIG.appId
  );
}

/** Định dạng dung lượng tệp hiển thị trên giao diện. */
export function formatFileSize(bytes) {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  const value = bytes / Math.pow(1024, index);
  return `${value.toFixed(index === 0 ? 0 : 1)} ${units[index]}`;
}

/** Kiểm tra tệp ảnh hợp lệ trước khi upload. Trả về thông báo lỗi hoặc null nếu hợp lệ. */
export function validateImageFile(file, { maxSizeMb = MAX_IMAGE_SIZE_MB } = {}) {
  if (!file) {
    return 'Vui lòng chọn một tệp ảnh';
  }
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    return 'Chỉ chấp nhận ảnh định dạng JPG, PNG, WEBP hoặc GIF';
  }
  if (file.size > maxSizeMb * 1024 * 1024) {
    return `Ảnh "${file.name}" vượt quá ${maxSizeMb}MB`;
  }
  return null;
}

/** Nạp Firebase App + Storage từ CDN (chỉ 1 lần cho mỗi phiên trình duyệt). */
async function loadFirebaseStorage() {
  if (!isFirebaseConfigured()) {
    return null;
  }
  if (!firebaseStoragePromise) {
    firebaseStoragePromise = (async () => {
      const [appModule, storageModule] = await Promise.all([
        import(/* webpackIgnore: true */ /* @vite-ignore */ FIREBASE_APP_URL),
        import(/* webpackIgnore: true */ /* @vite-ignore */ FIREBASE_STORAGE_URL)
      ]);
      const app = appModule.initializeApp(FIREBASE_CONFIG);
      return { appModule, storageModule, storage: storageModule.getStorage(app) };
    })();
  }
  return firebaseStoragePromise;
}

function sanitizeFileName(fileName) {
  return (fileName || 'anh-hien-truong.jpg')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-zA-Z0-9.-]/g, '-')
    .toLowerCase();
}

/** Tên đường dẫn duy nhất trên Firebase Storage. */
export function buildStoragePath(file, folder = FIREBASE_STORAGE_FOLDER) {
  const today = new Date().toISOString().slice(0, 10);
  const uniqueName = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}-${sanitizeFileName(file.name)}`;
  return `${folder}/${today}/${uniqueName}`;
}

/**
 * Nén ảnh trên trình duyệt (canvas) rồi trả về Data URL.
 * Dùng cho chế độ Demo khi chưa cấu hình Firebase để payload gửi lên backend không quá lớn.
 */
export function compressImageToDataUrl(file, { maxWidth = 1280, quality = 0.82 } = {}) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onerror = () => reject(new Error('Không thể đọc tệp ảnh đã chọn'));
    reader.onload = () => {
      const image = new Image();
      image.onerror = () => reject(new Error('Tệp đã chọn không phải là ảnh hợp lệ'));
      image.onload = () => {
        const scale = Math.min(1, maxWidth / (image.width || maxWidth));
        const canvas = document.createElement('canvas');
        canvas.width = Math.max(1, Math.round((image.width || maxWidth) * scale));
        canvas.height = Math.max(1, Math.round((image.height || maxWidth) * scale));

        const context = canvas.getContext('2d');
        context.drawImage(image, 0, 0, canvas.width, canvas.height);
        resolve(canvas.toDataURL('image/jpeg', quality));
      };
      image.src = reader.result;
    };
    reader.readAsDataURL(file);
  });
}

/**
 * Upload 1 ảnh lên Firebase Storage.
 * @returns {Promise<{url: string, provider: 'firebase-storage'|'local-preview', path: string|null, sizeLabel: string}>}
 */
export async function uploadImageToFirebase(file, { folder = FIREBASE_STORAGE_FOLDER, onProgress } = {}) {
  const validationError = validateImageFile(file);
  if (validationError) {
    throw new Error(validationError);
  }

  const firebase = await loadFirebaseStorage();

  // Chế độ Demo: chưa cấu hình Firebase -> nén ảnh thành Data URL để minh hoạ luồng nghiệp vụ.
  if (!firebase) {
    console.warn('[FixLink] Firebase Storage chưa được cấu hình -> dùng Data URL (chế độ demo).');
    const dataUrl = await compressImageToDataUrl(file);
    if (typeof onProgress === 'function') onProgress(100);
    return {
      url: dataUrl,
      provider: 'local-preview',
      path: null,
      sizeLabel: formatFileSize(file.size)
    };
  }

  const { storageModule, storage } = firebase;
  const path = buildStoragePath(file, folder);
  const storageRef = storageModule.ref(storage, path);

  const uploadTask = storageModule.uploadBytesResumable(storageRef, file, {
    contentType: file.type,
    cacheControl: 'public,max-age=31536000'
  });

  const downloadUrl = await new Promise((resolve, reject) => {
    uploadTask.on(
      'state_changed',
      snapshot => {
        if (typeof onProgress === 'function') {
          onProgress(Math.round((snapshot.bytesTransferred / snapshot.totalBytes) * 100));
        }
      },
      error => reject(new Error(`Upload thất bại: ${error.code || error.message}`)),
      async () => {
        try {
          resolve(await storageModule.getDownloadURL(uploadTask.snapshot.ref));
        } catch (error) {
          reject(new Error('Không lấy được URL tải ảnh sau khi upload'));
        }
      }
    );
  });

  return {
    url: downloadUrl,
    provider: 'firebase-storage',
    path,
    sizeLabel: formatFileSize(file.size)
  };
}

/** Upload tuần tự nhiều ảnh, trả về danh sách kết quả đã upload. */
export async function uploadImagesToFirebase(files, { folder = FIREBASE_STORAGE_FOLDER, onProgress } = {}) {
  const fileList = Array.from(files || []);
  const results = [];

  for (let index = 0; index < fileList.length; index++) {
    const result = await uploadImageToFirebase(fileList[index], {
      folder,
      onProgress: percent => {
        if (typeof onProgress === 'function') {
          onProgress({ index, total: fileList.length, percent });
        }
      }
    });
    results.push(result);
  }

  return results;
}

/** Xoá 1 tệp trên Firebase Storage theo download URL (dùng khi người dùng bỏ ảnh trước khi lưu). */
export async function deleteImageFromFirebase(url) {
  const firebase = await loadFirebaseStorage();
  if (!firebase || !url || !url.startsWith('http')) {
    return false;
  }
  try {
    const { storageModule, storage } = firebase;
    await storageModule.deleteObject(storageModule.ref(storage, url));
    return true;
  } catch (error) {
    console.warn('[FixLink] Không thể xoá ảnh trên Firebase Storage:', error);
    return false;
  }
}
