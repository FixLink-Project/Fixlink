/**
 * FixLink - Dịch vụ upload ảnh lên Cloudinary (Unsigned Upload qua REST API).
 *
 * Module ESM thuần, dùng được cho các trang HTML tĩnh của Spring Boot (không cần bundler hay SDK ngoài).
 *
 * Chế độ dự phòng (Demo Mode): nếu chưa cấu hình Cloudinary (CLOUDINARY_CLOUD_NAME và CLOUDINARY_UPLOAD_PRESET còn rỗng),
 * module sẽ nén ảnh ngay trên trình duyệt thành Data URL để luồng nghiệp vụ vẫn hoạt động mượt mà.
 */
import {
  CLOUDINARY_CLOUD_NAME,
  CLOUDINARY_UPLOAD_PRESET,
  CLOUDINARY_REPAIR_FOLDER,
  CLOUDINARY_KYC_FOLDER
} from './firebase-config.js';

export { CLOUDINARY_REPAIR_FOLDER, CLOUDINARY_KYC_FOLDER };
// Alias tương thích ngược nếu cần
export const FIREBASE_STORAGE_FOLDER = CLOUDINARY_REPAIR_FOLDER;
export const FIREBASE_KYC_FOLDER = CLOUDINARY_KYC_FOLDER;

export const MAX_IMAGE_SIZE_MB = 5;
export const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];

/** Kiểm tra xem đã cấu hình Cloudinary thật hay chưa. */
export function isCloudinaryConfigured() {
  return Boolean(CLOUDINARY_CLOUD_NAME && CLOUDINARY_UPLOAD_PRESET);
}

export const isFirebaseConfigured = isCloudinaryConfigured;

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
    const sizeMb = (file.size / (1024 * 1024)).toFixed(1);
    return `Ảnh "${file.name}" vượt quá ${maxSizeMb} MB (${sizeMb} MB)`;
  }
  return null;
}

/**
 * Nén ảnh trên trình duyệt (canvas) rồi trả về Data URL.
 * Dùng cho chế độ Demo khi chưa cấu hình Cloudinary để luồng kiểm thử/demo vẫn chạy trơn tru.
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
 * Upload 1 ảnh lên Cloudinary qua REST API.
 * @returns {Promise<{url: string, provider: 'cloudinary'|'local-preview', path: string|null, sizeLabel: string}>}
 */
export async function uploadImageToCloudinary(file, { folder = CLOUDINARY_REPAIR_FOLDER, onProgress } = {}) {
  const validationError = validateImageFile(file);
  if (validationError) {
    throw new Error(validationError);
  }

  // Chế độ Demo: chưa cấu hình Cloudinary -> nén ảnh thành Data URL
  if (!isCloudinaryConfigured()) {
    console.warn('[FixLink] Cloudinary chưa được cấu hình -> dùng Data URL (chế độ demo).');
    const dataUrl = await compressImageToDataUrl(file);
    if (typeof onProgress === 'function') onProgress(100);
    return {
      url: dataUrl,
      provider: 'local-preview',
      path: null,
      sizeLabel: formatFileSize(file.size)
    };
  }

  const formData = new FormData();
  formData.append('file', file);
  formData.append('upload_preset', CLOUDINARY_UPLOAD_PRESET);
  if (folder) {
    formData.append('folder', folder);
  }

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('POST', `https://api.cloudinary.com/v1_1/${encodeURIComponent(CLOUDINARY_CLOUD_NAME)}/image/upload`);

    if (xhr.upload && typeof onProgress === 'function') {
      xhr.upload.onprogress = event => {
        if (event.lengthComputable) {
          onProgress(Math.round((event.loaded / event.total) * 100));
        }
      };
    }

    xhr.onerror = () => reject(new Error('Tải ảnh lên Cloudinary thất bại. Kiểm tra kết nối mạng.'));

    xhr.onload = () => {
      try {
        const response = JSON.parse(xhr.responseText);
        if (xhr.status < 200 || xhr.status >= 300) {
          throw new Error(response?.error?.message || 'Cloudinary từ chối tải ảnh.');
        }
        resolve({
          url: response.secure_url || response.url,
          provider: 'cloudinary',
          path: response.public_id || folder,
          sizeLabel: formatFileSize(file.size)
        });
      } catch (err) {
        reject(new Error(err instanceof Error ? err.message : 'Phản hồi từ Cloudinary không hợp lệ.'));
      }
    };

    xhr.send(formData);
  });
}

// Giữ lại tên cũ để tương thích ngược 100% với các trang gọi cũ
export const uploadImageToFirebase = uploadImageToCloudinary;

/** Upload tuần tự nhiều ảnh, trả về danh sách kết quả đã upload. */
export async function uploadImagesToCloudinary(files, { folder = CLOUDINARY_REPAIR_FOLDER, onProgress } = {}) {
  const fileList = Array.from(files || []);
  const results = [];

  for (let index = 0; index < fileList.length; index++) {
    const result = await uploadImageToCloudinary(fileList[index], {
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

export const uploadImagesToFirebase = uploadImagesToCloudinary;

/** Hỗ trợ xoá tệp (nếu client gọi) */
export async function deleteImage(url) {
  console.info('[FixLink] Cloudinary client unsigned upload không hỗ trợ xoá trực tiếp từ trình duyệt vì lý do bảo mật.');
  return false;
}

export const deleteImageFromFirebase = deleteImage;
