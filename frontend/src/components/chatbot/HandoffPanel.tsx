import { useState, type FormEvent } from 'react';

interface HandoffPanelProps {
  isGuest: boolean;
  defaultContact: string;
  defaultIssue: string;
  onSubmit: (form: { name?: string; contact?: string; issue: string }) => Promise<string | null>;
  onClose: () => void;
}

/** Form "Gặp nhân viên" một chạm ngay trong khung chat; phiếu hỗ trợ tự kèm lịch sử hội thoại. */
export default function HandoffPanel({ isGuest, defaultContact, defaultIssue, onSubmit, onClose }: HandoffPanelProps) {
  const [name, setName] = useState('');
  const [contact, setContact] = useState(defaultContact);
  const [issue, setIssue] = useState(defaultIssue);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (issue.trim().length < 5) {
      setError('Vui lòng mô tả ngắn gọn vấn đề cần hỗ trợ (ít nhất 5 ký tự).');
      return;
    }
    if (isGuest && !contact.trim()) {
      setError('Vui lòng để lại số điện thoại hoặc email để nhân viên liên hệ lại.');
      return;
    }
    setSubmitting(true);
    setError(null);
    const err = await onSubmit({ name, contact, issue });
    setSubmitting(false);
    if (err) {
      setError(err);
    } else {
      onClose();
    }
  };

  return (
    <div className="absolute inset-x-0 bottom-0 z-10 bg-white border-t border-slate-200 shadow-[0_-8px_24px_rgba(15,23,42,0.08)] rounded-t-2xl p-4 animate-in slide-in-from-bottom-4 duration-200">
      <div className="flex items-center justify-between mb-2">
        <div>
          <div className="text-sm font-bold text-slate-900">👤 Gặp nhân viên CSKH</div>
          <div className="text-[11px] text-slate-500">Nhân viên sẽ nhận kèm toàn bộ nội dung cuộc trò chuyện.</div>
        </div>
        <button type="button" onClick={onClose} className="p-1 text-slate-400 hover:text-slate-700" aria-label="Đóng form">
          ✕
        </button>
      </div>

      <form onSubmit={handleSubmit} className="space-y-2 text-xs">
        {isGuest && (
          <div className="grid grid-cols-2 gap-2">
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Họ tên (không bắt buộc)"
              maxLength={100}
              className="px-2.5 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
            <input
              value={contact}
              onChange={(e) => setContact(e.target.value)}
              placeholder="SĐT hoặc email *"
              maxLength={120}
              className="px-2.5 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>
        )}
        <textarea
          value={issue}
          onChange={(e) => setIssue(e.target.value)}
          rows={3}
          maxLength={1000}
          placeholder="Bạn cần hỗ trợ vấn đề gì? (kèm mã đơn nếu có)"
          className="w-full px-2.5 py-2 border border-slate-300 rounded-lg resize-none focus:ring-2 focus:ring-indigo-500 focus:outline-none"
        />
        {error && <div className="text-red-600 text-[11px]">{error}</div>}
        <button
          type="submit"
          disabled={submitting}
          className="w-full py-2 rounded-lg bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white font-semibold transition-colors"
        >
          {submitting ? 'Đang gửi...' : 'Gửi yêu cầu hỗ trợ'}
        </button>
      </form>
    </div>
  );
}
