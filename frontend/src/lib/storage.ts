/** Cloudinary unsigned image uploads. Configure a restricted unsigned preset. */
export const MAX_IMAGE_BYTES = 5 * 1024 * 1024;
export const ACCEPTED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'] as const;
const cloudName = import.meta.env.VITE_CLOUDINARY_CLOUD_NAME || '';
const uploadPreset = import.meta.env.VITE_CLOUDINARY_UPLOAD_PRESET || '';

export function isStorageConfigured(): boolean {
  return Boolean(cloudName && uploadPreset);
}

export class ImageUploadError extends Error {
  constructor(message: string) { super(message); this.name = 'ImageUploadError'; }
}

export function validateImage(file: File): void {
  if (!ACCEPTED_IMAGE_TYPES.includes(file.type as (typeof ACCEPTED_IMAGE_TYPES)[number])) {
    throw new ImageUploadError('Chỉ chấp nhận ảnh JPG, PNG hoặc WebP.');
  }
  if (file.size > MAX_IMAGE_BYTES) {
    const sizeMb = (file.size / (1024 * 1024)).toFixed(1);
    throw new ImageUploadError(`Ảnh vượt quá giới hạn 5 MB (${sizeMb} MB).`);
  }
}

export function uploadImage(file: File, folder: string, onProgress?: (percent: number) => void): Promise<string> {
  validateImage(file);
  if (!isStorageConfigured()) throw new ImageUploadError('Chưa cấu hình Cloudinary. Thêm VITE_CLOUDINARY_CLOUD_NAME và VITE_CLOUDINARY_UPLOAD_PRESET vào frontend/.env.');
  const data = new FormData();
  data.append('file', file);
  data.append('upload_preset', uploadPreset);
  if (folder) data.append('folder', `fixlink/${folder}`);

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('POST', `https://api.cloudinary.com/v1_1/${encodeURIComponent(cloudName)}/image/upload`);
    xhr.upload.onprogress = event => {
      if (event.lengthComputable) onProgress?.(Math.round(event.loaded / event.total * 100));
    };
    xhr.onerror = () => reject(new ImageUploadError('Tải ảnh thất bại. Kiểm tra mạng rồi thử lại.'));
    xhr.onload = () => {
      try {
        const result = JSON.parse(xhr.responseText);
        if (xhr.status < 200 || xhr.status >= 300) throw new Error(result?.error?.message || 'Cloudinary từ chối tải ảnh.');
        resolve(result.secure_url);
      } catch (error) {
        reject(new ImageUploadError(error instanceof Error ? error.message : 'Phản hồi Cloudinary không hợp lệ.'));
      }
    };
    xhr.send(data);
  });
}
