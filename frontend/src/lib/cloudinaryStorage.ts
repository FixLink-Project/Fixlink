/**
 * FixLink - Module tích hợp Cloudinary Upload
 * Tải ảnh trực tiếp lên Cloudinary sử dụng Unsigned Upload Preset.
 */

export const CLOUDINARY_CLOUD_NAME = import.meta.env.VITE_CLOUDINARY_CLOUD_NAME || '';
export const CLOUDINARY_UPLOAD_PRESET = import.meta.env.VITE_CLOUDINARY_UPLOAD_PRESET || '';

export function getCloudinaryConfig(): { cloudName: string; uploadPreset: string } {
  const cloudName = (import.meta.env.VITE_CLOUDINARY_CLOUD_NAME || CLOUDINARY_CLOUD_NAME || '').trim();
  const uploadPreset = (import.meta.env.VITE_CLOUDINARY_UPLOAD_PRESET || CLOUDINARY_UPLOAD_PRESET || '').trim();
  return { cloudName, uploadPreset };
}

export function isCloudinaryConfigured(): boolean {
  const { cloudName, uploadPreset } = getCloudinaryConfig();
  return Boolean(cloudName && uploadPreset);
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
  const { cloudName, uploadPreset } = getCloudinaryConfig();
  if (!cloudName || !uploadPreset) {
    throw new Error(
      'Cloudinary chưa được cấu hình. Vui lòng thêm VITE_CLOUDINARY_CLOUD_NAME và VITE_CLOUDINARY_UPLOAD_PRESET vào file frontend/.env'
    );
  }

  const formData = new FormData();
  formData.append('file', file);
  formData.append('upload_preset', uploadPreset);
  if (folder) {
    formData.append('folder', folder);
  }

  return new Promise<string>((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    // Timeout 30s để tránh giao diện bị đứng/treo vô hạn khi mạng yếu
    xhr.timeout = 30000;
    xhr.open('POST', `https://api.cloudinary.com/v1_1/${cloudName}/image/upload`);

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

    xhr.ontimeout = () => {
      reject(new Error('Quá thời gian tải ảnh lên Cloudinary (timeout 30s). Vui lòng thử lại hoặc giảm dung lượng ảnh.'));
    };

    xhr.onabort = () => {
      reject(new Error('Quá trình tải ảnh bị hủy.'));
    };

    xhr.send(formData);
  });
}
