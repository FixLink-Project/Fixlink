import { initializeApp, type FirebaseApp } from 'firebase/app';
import {
  getDownloadURL,
  getStorage,
  ref,
  uploadBytesResumable,
  type FirebaseStorage
} from 'firebase/storage';

/** Giới hạn kích thước một ảnh tải lên. */
export const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

/** Ngưng thử lại sau khoảng này để giao diện không treo vô hạn. */
const UPLOAD_TIMEOUT_MS = 45_000;

export const ACCEPTED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'] as const;

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID
};

/**
 * Thiếu cấu hình thì tính năng tải ảnh tự tắt và giao diện chuyển sang cho dán
 * đường dẫn, thay vì vỡ giữa chừng khi người dùng đã chọn xong ảnh.
 */
export function isStorageConfigured(): boolean {
  return Boolean(firebaseConfig.apiKey && firebaseConfig.storageBucket);
}

let app: FirebaseApp | null = null;
let storage: FirebaseStorage | null = null;

function getStorageInstance(): FirebaseStorage {
  if (!isStorageConfigured()) {
    throw new Error(
      'Chưa cấu hình Firebase Storage. Thêm các biến VITE_FIREBASE_* vào tệp .env của frontend.'
    );
  }
  if (!app) app = initializeApp(firebaseConfig);
  if (!storage) {
    storage = getStorage(app);
    // Mặc định SDK thử lại tới hai phút trước khi báo lỗi. Cấu hình sai hoặc mạng
    // hỏng thì người dùng chỉ thấy thanh tiến trình đứng yên suốt thời gian đó.
    storage.maxUploadRetryTime = UPLOAD_TIMEOUT_MS;
    storage.maxOperationRetryTime = UPLOAD_TIMEOUT_MS;
  }
  return storage;
}

/** Lỗi người dùng sửa được, hiển thị thẳng lên giao diện. */
export class ImageUploadError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'ImageUploadError';
  }
}

export function validateImage(file: File): void {
  if (!ACCEPTED_IMAGE_TYPES.includes(file.type as (typeof ACCEPTED_IMAGE_TYPES)[number])) {
    throw new ImageUploadError('Chỉ nhận ảnh JPG, PNG hoặc WebP.');
  }
  if (file.size > MAX_IMAGE_BYTES) {
    const sizeMb = (file.size / (1024 * 1024)).toFixed(1);
    throw new ImageUploadError(`Ảnh nặng ${sizeMb} MB, vượt mức cho phép 5 MB. Hãy chụp lại nhỏ hơn.`);
  }
}

/** Tên tệp duy nhất, giữ phần mở rộng gốc để trình duyệt hiển thị đúng. */
function buildFileName(file: File): string {
  const extension = file.name.includes('.') ? file.name.split('.').pop() : 'jpg';
  const unique =
    typeof crypto !== 'undefined' && 'randomUUID' in crypto
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(36).slice(2)}`;
  return `${unique}.${extension}`;
}

/**
 * Tải một ảnh lên Firebase Storage và trả về đường dẫn tải xuống.
 *
 * @param folder thư mục đích, ví dụ "cccd" hoặc "avatars"
 * @param onProgress gọi lại với phần trăm 0–100 trong lúc tải
 */
export async function uploadImage(
  file: File,
  folder: string,
  onProgress?: (percent: number) => void
): Promise<string> {
  validateImage(file);

  const storageRef = ref(getStorageInstance(), `${folder}/${buildFileName(file)}`);
  const task = uploadBytesResumable(storageRef, file, { contentType: file.type });

  return new Promise<string>((resolve, reject) => {
    task.on(
      'state_changed',
      (snapshot) => {
        if (!onProgress || snapshot.totalBytes === 0) return;
        onProgress(Math.round((snapshot.bytesTransferred / snapshot.totalBytes) * 100));
      },
      (error) => {
        reject(
          new ImageUploadError(
            error.code === 'storage/unauthorized'
              ? 'Không có quyền tải ảnh lên. Kiểm tra lại quy tắc bảo mật của Firebase Storage.'
              : 'Tải ảnh thất bại. Kiểm tra mạng rồi thử lại.'
          )
        );
      },
      () => {
        getDownloadURL(task.snapshot.ref)
          .then(resolve)
          .catch(() => reject(new ImageUploadError('Tải lên xong nhưng không lấy được đường dẫn ảnh.')));
      }
    );
  });
}
