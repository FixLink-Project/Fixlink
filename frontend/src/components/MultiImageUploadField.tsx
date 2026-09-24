import { useRef, useState, type ChangeEvent } from 'react';
import { uploadImageToFirebase, validateImageFile } from '../lib/firebaseStorage';

interface MultiImageUploadFieldProps {
  label: string;
  value: string[];
  onChange: (urls: string[]) => void;
  maxFiles?: number;
  hint?: string;
  error?: string;
  disabled?: boolean;
}

export default function MultiImageUploadField({
  label,
  value = [],
  onChange,
  maxFiles = 6,
  hint,
  error,
  disabled = false
}: MultiImageUploadFieldProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState<number | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);

  async function handleFiles(event: ChangeEvent<HTMLInputElement>) {
    const files = Array.from(event.target.files || []);
    event.target.value = '';
    if (!files.length) return;

    if (value.length + files.length > maxFiles) {
      setUploadError(`Có thể đính kèm tối đa ${maxFiles} ảnh. Hiện đã có ${value.length} ảnh.`);
      return;
    }

    const validationError = files.map((file) => validateImageFile(file)).find(Boolean);
    if (validationError) {
      setUploadError(validationError);
      return;
    }

    setUploadError(null);
    setUploading(true);
    setProgress(0);

    const newUrls: string[] = [];
    try {
      for (let index = 0; index < files.length; index++) {
        const result = await uploadImageToFirebase(files[index], {
          onProgress: (percent) => setProgress(Math.round(((index + percent / 100) / files.length) * 100))
        });
        newUrls.push(result.url);
      }
      onChange([...value, ...newUrls]);
    } catch (uploadFailure) {
      setUploadError(uploadFailure instanceof Error ? uploadFailure.message : 'Tải ảnh thất bại. Vui lòng thử lại.');
    } finally {
      setUploading(false);
      setProgress(null);
    }
  }

  return (
    <div>
      <div className="mb-1.5 flex items-center justify-between">
        <span className="block text-sm font-semibold text-slate-800">{label}</span>
        <span className="text-xs font-medium text-slate-500">{value.length}/{maxFiles} ảnh</span>
      </div>

      <div className="grid grid-cols-2 gap-3 xs:grid-cols-3 sm:grid-cols-6">
        {value.map((url, index) => (
          <div key={`${url}-${index}`} className="group relative aspect-square overflow-hidden rounded-xl border border-slate-200 bg-slate-50 shadow-xs">
            <img src={url} alt={`Ảnh thiết bị ${index + 1}`} className="h-full w-full object-cover" />
            {!disabled && (
              <button
                type="button"
                onClick={() => onChange(value.filter((_, itemIndex) => itemIndex !== index))}
                aria-label={`Xóa ảnh ${index + 1}`}
                className="absolute right-1 top-1 flex h-7 w-7 items-center justify-center rounded-full bg-slate-900/75 text-sm text-white transition-colors hover:bg-rose-600"
              >
                ×
              </button>
            )}
            <span className="absolute bottom-1 left-1 rounded bg-slate-900/60 px-1.5 py-0.5 text-[10px] font-medium text-white">Ảnh {index + 1}</span>
          </div>
        ))}

        {value.length < maxFiles && !disabled && (
          <button
            type="button"
            disabled={uploading}
            onClick={() => fileInputRef.current?.click()}
            className="flex aspect-square flex-col items-center justify-center gap-1.5 rounded-xl border-2 border-dashed border-slate-300 bg-slate-50 text-slate-500 shadow-xs transition-all hover:border-brand hover:bg-blue-50/50 hover:text-brand disabled:cursor-wait disabled:opacity-60"
          >
            <span aria-hidden="true" className="text-2xl">＋</span>
            <span className="text-xs font-semibold">Thêm ảnh</span>
          </button>
        )}
      </div>

      <input
        ref={fileInputRef}
        type="file"
        multiple
        accept="image/jpeg,image/png,image/webp,image/gif"
        className="sr-only"
        disabled={disabled || uploading}
        onChange={handleFiles}
        aria-label={label}
      />

      {uploading && (
        <div className="mt-3" role="status" aria-live="polite">
          <div className="mb-1 flex justify-between text-xs text-slate-600">
            <span>Đang tải ảnh lên…</span>
            <span className="font-semibold text-brand">{progress}%</span>
          </div>
          <div className="h-1.5 w-full overflow-hidden rounded-full bg-slate-100">
            <div className="h-full rounded-full bg-brand transition-[width] duration-200" style={{ width: `${progress}%` }} />
          </div>
        </div>
      )}

      {(uploadError || error) && <p className="mt-2 text-xs font-medium text-rose-600" role="alert">{uploadError ?? error}</p>}
      {hint && !uploadError && !error && <p className="mt-2 text-xs text-slate-500">{hint}</p>}
    </div>
  );
}
