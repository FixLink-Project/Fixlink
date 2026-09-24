import {
  FIREBASE_CONFIG,
  FIREBASE_STORAGE_FOLDER,
  FIREBASE_KYC_FOLDER
} from './firebaseConfig';

export { FIREBASE_STORAGE_FOLDER, FIREBASE_KYC_FOLDER };

export const FIREBASE_SDK_VERSION = '10.12.5';
export const MAX_IMAGE_SIZE_MB = 5;
export const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];

const FIREBASE_APP_URL = `https://www.gstatic.com/firebasejs/${FIREBASE_SDK_VERSION}/firebase-app.js`;
const FIREBASE_STORAGE_URL = `https://www.gstatic.com/firebasejs/${FIREBASE_SDK_VERSION}/firebase-storage.js`;

let firebaseStoragePromise: Promise<any> | null = null;

export function isFirebaseConfigured(): boolean {
  return Boolean(
    FIREBASE_CONFIG &&
    FIREBASE_CONFIG.apiKey &&
    FIREBASE_CONFIG.projectId &&
    FIREBASE_CONFIG.storageBucket &&
    FIREBASE_CONFIG.appId
  );
}

export function formatFileSize(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  const value = bytes / Math.pow(1024, index);
  return `${value.toFixed(index === 0 ? 0 : 1)} ${units[index]}`;
}

export function validateImageFile(file: File, { maxSizeMb = MAX_IMAGE_SIZE_MB }: { maxSizeMb?: number } = {}): string | null {
  if (!file) return 'Vui lòng chọn một tệp ảnh';
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    return 'Chỉ chấp nhận ảnh định dạng JPG, PNG, WEBP hoặc GIF';
  }
  if (file.size > maxSizeMb * 1024 * 1024) {
    return `Ảnh "${file.name}" vượt quá ${maxSizeMb}MB`;
  }
  return null;
}

function sanitizeFileName(fileName: string): string {
  return (fileName || 'anh-hien-truong.jpg')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-zA-Z0-9.-]/g, '-')
    .toLowerCase();
}

export function buildStoragePath(file: File, folder = FIREBASE_STORAGE_FOLDER): string {
  const today = new Date().toISOString().slice(0, 10);
  const uniqueName = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}-${sanitizeFileName(file.name)}`;
  return `${folder}/${today}/${uniqueName}`;
}

async function loadFirebaseStorage(): Promise<any> {
  if (!isFirebaseConfigured()) return null;

  if (!firebaseStoragePromise) {
    firebaseStoragePromise = (async () => {
      const [appModule, storageModule] = await Promise.all([
        import(/* @vite-ignore */ FIREBASE_APP_URL),
        import(/* @vite-ignore */ FIREBASE_STORAGE_URL)
      ]);
      const app = appModule.initializeApp(FIREBASE_CONFIG);
      return { appModule, storageModule, storage: storageModule.getStorage(app) };
    })();
  }
  return firebaseStoragePromise;
}

export function compressImageToDataUrl(file: File, { maxWidth = 1280, quality = 0.82 }: { maxWidth?: number; quality?: number } = {}): Promise<string> {
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
        const ctx = canvas.getContext('2d');
        if (!ctx) {
          resolve(reader.result as string);
          return;
        }
        ctx.drawImage(image, 0, 0, canvas.width, canvas.height);
        resolve(canvas.toDataURL('image/jpeg', quality));
      };
      image.src = reader.result as string;
    };
    reader.readAsDataURL(file);
  });
}

export interface UploadResult {
  url: string;
  provider: 'firebase-storage' | 'local-preview';
  path: string | null;
  sizeLabel: string;
}

export async function uploadImageToFirebase(
  file: File,
  { folder = FIREBASE_STORAGE_FOLDER, onProgress }: { folder?: string; onProgress?: (percent: number) => void } = {}
): Promise<UploadResult> {
  const validationError = validateImageFile(file);
  if (validationError) throw new Error(validationError);

  try {
    const firebase = await loadFirebaseStorage();

    if (!firebase) {
      const dataUrl = await compressImageToDataUrl(file);
      if (typeof onProgress === 'function') onProgress(100);
      return { url: dataUrl, provider: 'local-preview', path: null, sizeLabel: formatFileSize(file.size) };
    }

    const { storageModule, storage } = firebase;
    const path = buildStoragePath(file, folder);
    const uploadTask = storageModule.uploadBytesResumable(storageModule.ref(storage, path), file, {
      contentType: file.type,
      cacheControl: 'public,max-age=31536000'
    });

    const url = await new Promise<string>((resolve, reject) => {
      uploadTask.on(
        'state_changed',
        (snapshot: any) => {
          if (typeof onProgress === 'function') {
            onProgress(Math.round((snapshot.bytesTransferred / snapshot.totalBytes) * 100));
          }
        },
        (error: any) => reject(new Error(`Upload thất bại: ${error.code || error.message}`)),
        async () => {
          try {
            resolve(await storageModule.getDownloadURL(uploadTask.snapshot.ref));
          } catch (err) {
            reject(new Error('Không lấy được URL tải ảnh sau khi upload'));
          }
        }
      );
    });

    return { url, provider: 'firebase-storage', path, sizeLabel: formatFileSize(file.size) };
  } catch (err) {
    // Fallback to local Data URL on network / config issues
    console.warn('Firebase upload fallback to local Data URL:', err);
    const dataUrl = await compressImageToDataUrl(file);
    if (typeof onProgress === 'function') onProgress(100);
    return { url: dataUrl, provider: 'local-preview', path: null, sizeLabel: formatFileSize(file.size) };
  }
}

export async function uploadImagesToFirebase(
  files: File[] | FileList,
  {
    folder = FIREBASE_STORAGE_FOLDER,
    onProgress
  }: {
    folder?: string;
    onProgress?: (progress: { index: number; total: number; percent: number }) => void;
  } = {}
): Promise<UploadResult[]> {
  const fileList = Array.from(files || []);
  const results: UploadResult[] = [];

  for (let index = 0; index < fileList.length; index++) {
    results.push(
      await uploadImageToFirebase(fileList[index], {
        folder,
        onProgress: percent => {
          if (typeof onProgress === 'function') {
            onProgress({ index, total: fileList.length, percent });
          }
        }
      })
    );
  }

  return results;
}
