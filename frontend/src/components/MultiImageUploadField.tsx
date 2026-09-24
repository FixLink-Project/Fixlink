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
      setUploadError(`Chỉ được tải lên tối đa ${maxFiles} ảnh. Hiện đã có ${value.length} ảnh.`);
      return;
    }

    setUploadError(null);
    for (const f of files) {
      const err = validateImageFile(f);
      if (err) {
        setUploadError(err);
        return;
      }
    }

    setUploading(true);
    setProgress(0);

    const newUrls: string[] = [];
    try {
      for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const res = await uploadImageToFirebase(file, {
          onProgress: (p) => setProgress(Math.round(((i + p / 100) / files.length) * 100))
        });
        newUrls.push(res.url);
      }
      onChange([...value, ...newUrls]);
    } catch (err: any) {
      setUploadError(err?.message || 'Tải ảnh thất bại. Vui lòng thử lại.');
    } finally {
      setUploading(false);
      setProgress(null);
    }
  }

  function handleRemove(indexToRemove: number) {
    onChange(value.filter((_, idx) => idx !== indexToRemove));
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-1.5">
        <span className="block text-sm font-semibold text-slate-800">{label}</span>
        <span className="text-xs font-medium text-slate-400">
          {value.length}/{maxFiles} tệp
        </span>
      </div>

      {/* Grid of uploaded images + Add button */}
      <div className="grid grid-cols-2 xs:grid-cols-3 sm:grid-cols-6 gap-3">
        {value.map((url, idx) => (
          <div
            key={idx}
            className="group relative aspect-square rounded-xl border border-slate-200 bg-slate-50 overflow-hidden shadow-xs"
          >
            <img
              src={url}
              alt={`Ảnh ${idx + 1}`}
              className="h-full w-full object-cover"
            />
            {!disabled && (
              <button
                type="button"
                onClick={() => handleRemove(idx)}
                className="absolute top-1 right-1 h-6 w-6 rounded-full bg-slate-900/70 text-white flex items-center justify-center text-xs opacity-0 group-hover:opacity-100 transition-opacity hover:bg-rose-600"
                title="Gỡ ảnh này"
              >
                ✕
              </button>
            )}
            <span className="absolute bottom-1 left-1 px-1.5 py-0.5 rounded bg-slate-900/60 text-[10px] text-white font-medium">
              #{idx + 1}
            </span>
          </div>
        ))}

        {/* Add photo card button */}
        {value.length < maxFiles && !disabled && (
          <button
            type="button"
            disabled={uploading}
            onClick={() => fileInputRef.current?.click()}
            className="aspect-square flex flex-col items-center justify-center gap-1.5 rounded-xl border-2 border-dashed border-slate-300 bg-slate-50 hover:bg-blue-50/50 hover:border-brand text-slate-400 hover:text-brand transition-all shadow-xs"
          >
            <span className="text-2xl">📷</span>
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
      />

      {uploading && (
        <div className="mt-3">
          <div className="flex justify-between text-xs text-slate-500 mb-1">
            <span>Đang tải ảnh lên...</span>
            <span className="font-semibold text-brand">{progress}%</span>
          </div>
          <div className="h-1.5 w-full overflow-hidden rounded-full bg-slate-100">
            <div
              className="h-full rounded-full bg-brand transition-[width] duration-200"
              style={{ width: `${progress}%` }}
            />
          </div>
        </div>
      )}

      {(uploadError || error) && (
        <p className="mt-2 text-xs font-medium text-rose-600">{uploadError ?? error}</p>
      )}
      {hint && !uploadError && !error && (
        <p className="mt-2 text-xs text-slate-500">{hint}</p>
      )}
    </div>
  );
}
