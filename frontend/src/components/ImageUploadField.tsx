import { useId, useRef, useState, type ChangeEvent } from 'react';
import Button from './Button';
import { ImageUploadError, isStorageConfigured, uploadImage, validateImage } from '../lib/storage';

interface ImageUploadFieldProps {
  label: string;
  /** Đường dẫn ảnh hiện tại; rỗng nghĩa là chưa có. */
  value: string;
  onChange: (url: string) => void;
  /** Thư mục trên Storage, ví dụ "cccd" hoặc "avatars". */
  folder: string;
  hint?: string;
  error?: string;
  disabled?: boolean;
}

export default function ImageUploadField({
  label,
  value,
  onChange,
  folder,
  hint,
  error,
  disabled = false
}: ImageUploadFieldProps) {
  const inputId = useId();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [preview, setPreview] = useState<string | null>(null);
  const [progress, setProgress] = useState<number | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);

  const configured = isStorageConfigured();
  const uploading = progress !== null;
  const shownImage = preview ?? (value || null);

  async function handleFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    // Xoá giá trị để chọn lại đúng tệp vừa rồi vẫn kích hoạt sự kiện.
    event.target.value = '';
    if (!file) return;

    setUploadError(null);
    try {
      validateImage(file);
    } catch (err) {
      setUploadError(err instanceof ImageUploadError ? err.message : 'Tệp không hợp lệ.');
      return;
    }

    // Hiện ngay ảnh vừa chọn để người dùng biết mình chọn đúng tệp.
    const localPreview = URL.createObjectURL(file);
    setPreview(localPreview);
    setProgress(0);

    try {
      const url = await uploadImage(file, folder, setProgress);
      onChange(url);
    } catch (err) {
      setPreview(null);
      setUploadError(
        err instanceof ImageUploadError ? err.message : 'Tải ảnh thất bại. Thử lại sau.'
      );
    } finally {
      URL.revokeObjectURL(localPreview);
      setProgress(null);
    }
  }

  function clearImage() {
    setPreview(null);
    setUploadError(null);
    onChange('');
  }

  return (
    <div>
      <span className="mb-1.5 block text-sm font-medium text-ink">{label}</span>

      {shownImage && (
        <img
          src={shownImage}
          alt={`Ảnh đã chọn cho ${label}`}
          className="mb-3 h-36 w-full rounded-xl border border-line bg-surface object-contain"
        />
      )}

      {configured ? (
        <>
          <input
            ref={fileInputRef}
            id={inputId}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            className="sr-only"
            disabled={disabled || uploading}
            onChange={handleFile}
          />
          <div className="flex flex-wrap items-center gap-2">
            <Button
              type="button"
              variant="secondary"
              loading={uploading}
              disabled={disabled}
              onClick={() => fileInputRef.current?.click()}
            >
              {uploading ? `Đang tải ${progress}%` : shownImage ? 'Chọn ảnh khác' : 'Chọn ảnh'}
            </Button>
            {shownImage && !uploading && (
              <Button type="button" variant="quiet" disabled={disabled} onClick={clearImage}>
                Gỡ ảnh
              </Button>
            )}
          </div>

          {uploading && (
            <div
              role="progressbar"
              aria-valuenow={progress ?? 0}
              aria-valuemin={0}
              aria-valuemax={100}
              aria-label={`Tiến trình tải ${label}`}
              className="mt-3 h-1.5 w-full overflow-hidden rounded-full bg-line"
            >
              <div
                className="h-full bg-brand transition-[width] duration-200"
                style={{ width: `${progress}%` }}
              />
            </div>
          )}
        </>
      ) : (
        <>
          <input
            id={inputId}
            type="url"
            value={value}
            disabled={disabled}
            placeholder="https://..."
            onChange={(e) => onChange(e.target.value)}
            className="min-h-[44px] w-full rounded-lg border border-line bg-card px-3.5 text-ink placeholder:text-ink-soft/60"
          />
          <p className="mt-1.5 text-sm text-ink-soft">
            Chưa bật kho ảnh nên tạm dán đường dẫn. Thêm các biến VITE_FIREBASE_* vào tệp .env
            của frontend để chọn ảnh trực tiếp từ máy.
          </p>
        </>
      )}

      {(uploadError || error) && <p className="mt-1.5 text-sm text-rose">{uploadError ?? error}</p>}
      {hint && !uploadError && !error && <p className="mt-1.5 text-sm text-ink-soft">{hint}</p>}
    </div>
  );
}
