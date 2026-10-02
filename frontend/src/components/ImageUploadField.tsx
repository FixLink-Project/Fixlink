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
        <div className="mb-3 overflow-hidden rounded-2xl border border-slate-200 bg-slate-50 p-2 shadow-xs">
          <img
            src={shownImage}
            alt={`Ảnh đã chọn cho ${label}`}
            className="h-40 w-full object-contain rounded-xl"
          />
        </div>
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

          {!shownImage && !uploading && (
            <button
              type="button"
              disabled={disabled}
              onClick={() => fileInputRef.current?.click()}
              className="flex w-full flex-col items-center gap-2 rounded-2xl border-2 border-dashed border-slate-300 bg-slate-50/60 px-4 py-8 text-slate-500 shadow-xs transition-all duration-200 hover:border-brand hover:bg-blue-50/40 hover:text-brand"
            >
              <span className="flex h-12 w-12 items-center justify-center rounded-xl bg-white text-2xl shadow-xs border border-slate-200">
                📷
              </span>
              <span className="text-sm font-semibold">Nhấn để tải ảnh sự cố / linh kiện</span>
              <span className="text-xs text-slate-400">JPG, PNG, WebP tối đa 5 MB</span>
            </button>
          )}

          {(shownImage || uploading) && (
            <div className="flex flex-wrap items-center gap-2">
              <Button
                type="button"
                variant="secondary"
                loading={uploading}
                disabled={disabled}
                onClick={() => fileInputRef.current?.click()}
              >
                {uploading ? `Đang tải ${progress}%` : 'Chọn ảnh khác'}
              </Button>
              {shownImage && !uploading && (
                <Button type="button" variant="quiet" disabled={disabled} onClick={clearImage}>
                  Gỡ ảnh
                </Button>
              )}
            </div>
          )}

          {uploading && (
            <div
              role="progressbar"
              aria-valuenow={progress ?? 0}
              aria-valuemin={0}
              aria-valuemax={100}
              aria-label={`Tiến trình tải ${label}`}
              className="mt-3 h-2 w-full overflow-hidden rounded-full bg-slate-100"
            >
              <div
                className="h-full rounded-full bg-brand transition-[width] duration-200"
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
            className="min-h-[44px] w-full rounded-xl border border-slate-200 bg-white px-3.5 text-sm text-slate-900 placeholder:text-slate-400 shadow-xs transition-all duration-200 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/20"
          />
          <p className="mt-1.5 text-xs text-slate-500">
            Dán đường link ảnh hiện trạng hoặc linh kiện thay thế.
          </p>
        </>
      )}

      {(uploadError || error) && <p className="mt-1.5 text-sm text-danger">{uploadError ?? error}</p>}
      {hint && !uploadError && !error && <p className="mt-1.5 text-sm text-ink-muted">{hint}</p>}
    </div>
  );
}
