import { isStorageConfigured, uploadImage, validateImage, MAX_IMAGE_BYTES } from './storage';

export const CLOUDINARY_REPAIR_FOLDER = 'repair-requests';
export const CLOUDINARY_KYC_FOLDER = 'kyc-documents';
export const MAX_IMAGE_SIZE_MB = MAX_IMAGE_BYTES / (1024 * 1024);
export const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
export const isCloudinaryConfigured = isStorageConfigured;
export const isFirebaseConfigured = isStorageConfigured;

export function validateImageFile(file: File): string | null {
  try { validateImage(file); return null; } catch (error) { return error instanceof Error ? error.message : 'Ảnh không hợp lệ.'; }
}

export function formatFileSize(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  return `${(bytes / 1024 ** index).toFixed(index === 0 ? 0 : 1)} ${units[index]}`;
}

export interface UploadResult { url: string; provider: 'cloudinary'; path: string; sizeLabel: string; }

export async function uploadImageToCloudinary(file: File, { folder = CLOUDINARY_REPAIR_FOLDER, onProgress }: { folder?: string; onProgress?: (percent: number) => void } = {}): Promise<UploadResult> {
  const url = await uploadImage(file, folder, onProgress);
  return { url, provider: 'cloudinary', path: folder, sizeLabel: formatFileSize(file.size) };
}

export const uploadImageToFirebase = uploadImageToCloudinary;

export async function uploadImagesToCloudinary(files: File[] | FileList, { folder = CLOUDINARY_REPAIR_FOLDER, onProgress }: { folder?: string; onProgress?: (progress: { index: number; total: number; percent: number }) => void } = {}): Promise<UploadResult[]> {
  const list = Array.from(files || []);
  const results: UploadResult[] = [];
  for (let index = 0; index < list.length; index++) {
    results.push(await uploadImageToCloudinary(list[index], { folder, onProgress: percent => onProgress?.({ index, total: list.length, percent }) }));
  }
  return results;
}

export const uploadImagesToFirebase = uploadImagesToCloudinary;
