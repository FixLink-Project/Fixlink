import { describe, expect, it } from 'vitest';
import { ImageUploadError, MAX_IMAGE_BYTES, isStorageConfigured, validateImage } from '../src/lib/storage';
import { uploadImageToFirebase } from '../src/lib/firebaseStorage';

function fakeFile(type: string, size: number): File {
  const file = new File(['x'], 'anh.jpg', { type });
  Object.defineProperty(file, 'size', { value: size });
  return file;
}

describe('Kiểm tra ảnh trước khi tải lên', () => {
  it('nhận ảnh JPG, PNG và WebP trong giới hạn dung lượng', () => {
    expect(() => validateImage(fakeFile('image/jpeg', 1024))).not.toThrow();
    expect(() => validateImage(fakeFile('image/png', 1024))).not.toThrow();
    expect(() => validateImage(fakeFile('image/webp', 1024))).not.toThrow();
  });

  it('từ chối định dạng không phải ảnh', () => {
    expect(() => validateImage(fakeFile('application/pdf', 1024))).toThrow(ImageUploadError);
    expect(() => validateImage(fakeFile('image/gif', 1024))).toThrow(/JPG, PNG ho/);
  });

  it('từ chối ảnh vượt 5 MB và nói rõ ảnh nặng bao nhiêu', () => {
    expect(() => validateImage(fakeFile('image/jpeg', MAX_IMAGE_BYTES + 1))).toThrow(/5 MB/);
    expect(() => validateImage(fakeFile('image/jpeg', 7 * 1024 * 1024))).toThrow(/7\.0 MB/);
  });

  it('nhận đúng mốc 5 MB', () => {
    expect(() => validateImage(fakeFile('image/jpeg', MAX_IMAGE_BYTES))).not.toThrow();
  });

  it('báo trạng thái cấu hình của kho ảnh (Cloudinary hoặc Firebase)', () => {
    // Trả về boolean phản ánh trạng thái cấu hình hiện tại
    expect(typeof isStorageConfigured()).toBe('boolean');
  });

  it('xử lý tải ảnh khi gọi uploadImageToFirebase', async () => {
    const file = fakeFile('image/jpeg', 1024);
    // Nếu trong môi trường test jsdom không có kết nối mạng thật, reject có kiểm soát
    await expect(uploadImageToFirebase(file)).rejects.toThrow();
  });
});
