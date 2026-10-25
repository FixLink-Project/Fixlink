/**
 * FixLink - Module tích hợp Cloudinary Upload
 * Tải ảnh trực tiếp lên Cloudinary sử dụng Unsigned Upload Preset.
 */

export const CLOUDINARY_CLOUD_NAME = import.meta.env.VITE_CLOUDINARY_CLOUD_NAME || '';
export const CLOUDINARY_UPLOAD_PRESET = import.meta.env.VITE_CLOUDINARY_UPLOAD_PRESET || '';

export function isCloudinaryConfigured(): boolean {
  return Boolean(
    CLOUDINARY_CLOUD_NAME &&
    CLOUDINARY_CLOUD_NAME.trim() !== '' &&
    CLOUDINARY_UPLOAD_PRESET &&
    CLOUDINARY_UPLOAD_PRESET.trim() !== ''
  );
}

export interface CloudinaryUploadResponse {
  url: string;
  publicId: string;
  format: string;
  bytes: number;
}

/**
 * Tải một tệp ảnh lên Cloudinary qua Unsigned Upload API
 * @param file Tệp ảnh cần tải
 * @param folder Thư mục trên Cloudinary (mặc định: 'fixlink')
 * @param onProgress Callback thông báo tiến trình phần trăm (0 - 100)
 */
export async function uploadToCloudinary(
  file: File,
  folder = 'fixlink',
  onProgress?: (percent: number) => void
): Promise<string> {
  if (!isCloudinaryConfigured()) {
    throw new Error(
      'Cloudinary chưa được cấu hình. Vui lòng thêm VITE_CLOUDINARY_CLOUD_NAME và VITE_CLOUDINARY_UPLOAD_PRESET vào file frontend/.env'
    );
  }

  const formData = new FormData();
  formData.append('file', file);
  formData.append('upload_preset', CLOUDINARY_UPLOAD_PRESET);
  if (folder) {
    formData.append('folder', folder);
  }

  return new Promise<string>((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('POST', `https://api.cloudinary.com/v1_1/${CLOUDINARY_CLOUD_NAME}/image/upload`);

    if (xhr.upload && typeof onProgress === 'function') {
      xhr.upload.onprogress = (event) => {
        if (event.lengthComputable && event.total > 0) {
          const percent = Math.round((event.loaded / event.total) * 100);
          onProgress(percent);
        }
      };
    }

    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          const res = JSON.parse(xhr.responseText);
          resolve(res.secure_url || res.url);
        } catch {
          reject(new Error('Phản hồi từ Cloudinary không đúng định dạng JSON.'));
        }
      } else {
        try {
          const errRes = JSON.parse(xhr.responseText);
          reject(new Error(errRes.error?.message || `Upload ảnh thất bại (HTTP ${xhr.status})`));
        } catch {
          reject(new Error(`Upload ảnh lên Cloudinary thất bại với HTTP status ${xhr.status}`));
        }
      }
    };

    xhr.onerror = () => {
      reject(new Error('Lỗi kết nối mạng khi tải ảnh lên Cloudinary. Kiểm tra kết nối mạng rồi thử lại.'));
    };

    xhr.send(formData);
  });
}
