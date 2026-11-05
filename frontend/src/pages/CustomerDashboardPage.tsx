import { useEffect, useState, type FormEvent } from 'react';
import Alert from '../components/Alert';
import Button from '../components/Button';
import Card from '../components/Card';
import DashboardLayout from '../components/DashboardLayout';
import ImageUploadField from '../components/ImageUploadField';
import TextField from '../components/TextField';
import { ApiError, api, toFieldErrors } from '../lib/api';
import { useAuth } from '../lib/auth';

interface CustomerProfile {
  userId: number;
  fullName: string;
  phone: string;
  email: string;
  avatarUrl: string | null;
  membershipTier: string | null;
  createdAt: string;
  updatedAt: string;
}



function formatDateTime(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date);
}



const CUSTOMER_NAV = [
  { to: '/khach-hang', label: 'Bảng điều khiển' },
  { to: '/yeu-cau-cua-toi', label: 'Yêu cầu của tôi' },
  { to: '/dang-yeu-cau', label: 'Đăng yêu cầu' }
];

export default function CustomerDashboardPage() {
  const { user, updateUser } = useAuth();
  const userId = user ? user.id.replace('usr_', '') : '';

  const [profile, setProfile] = useState<CustomerProfile | null>(null);
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading');
  const [loadError, setLoadError] = useState<string | null>(null);

  const [form, setForm] = useState({ fullName: '', phone: '', email: '', avatarUrl: '' });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    if (!userId) return;
    let alive = true;

    (async () => {
      try {
        const res = await api.get<CustomerProfile>(`/customers/${userId}/profile`);
        if (!alive) return;
        setProfile(res.data);
        setForm({
          fullName: res.data.fullName ?? '',
          phone: res.data.phone ?? '',
          email: res.data.email ?? '',
          avatarUrl: res.data.avatarUrl ?? ''
        });
        setStatus('ready');
      } catch (err) {
        if (!alive) return;
        setLoadError(
          err instanceof ApiError ? err.message : 'Không kết nối được máy chủ. Thử tải lại trang.'
        );
        setStatus('error');
      }
    })();

    return () => {
      alive = false;
    };
  }, [userId]);

  function update(field: keyof typeof form, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
    setSaved(false);
    setErrors((current) => {
      if (!current[field]) return current;
      const next = { ...current };
      delete next[field];
      return next;
    });
  }

  async function handleSave(event: FormEvent) {
    event.preventDefault();
    setSaveError(null);
    setSaved(false);

    const clientErrors: Record<string, string> = {};
    if (!form.fullName.trim()) clientErrors.fullName = 'Họ và tên không được để trống.';
    if (!/^0[35789][0-9]{8}$/.test(form.phone.trim())) {
      clientErrors.phone = 'Số điện thoại cần 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09.';
    }
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(form.email.trim())) {
      clientErrors.email = 'Email chưa đúng định dạng.';
    }
    if (Object.keys(clientErrors).length > 0) {
      setErrors(clientErrors);
      return;
    }

    setSaving(true);
    try {
      const res = await api.put<CustomerProfile>(`/customers/${userId}/profile`, {
        fullName: form.fullName.trim(),
        phone: form.phone.trim(),
        email: form.email.trim(),
        avatarUrl: form.avatarUrl.trim() || null
      });
      setProfile(res.data);
      updateUser({ fullName: res.data.fullName, avatarUrl: res.data.avatarUrl });
      setSaved(true);
    } catch (err) {
      const fieldErrors = toFieldErrors(err);
      if (Object.keys(fieldErrors).length > 0) {
        setErrors(fieldErrors);
      } else if (err instanceof ApiError) {
        setSaveError(err.message);
      } else {
        setSaveError('Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.');
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <DashboardLayout
      nav={CUSTOMER_NAV}
      title={`Chào ${profile?.fullName?.trim() || user?.username || 'bạn'} 👋`}
      description="Quản lý thông tin tài khoản và đối chiếu lịch sử sửa chữa thiết bị."
    >
      {/* Quick customer shortcuts */}
      <div className="mb-6 grid grid-cols-1 sm:grid-cols-3 gap-4">
        <a
          href="/dang-yeu-cau"
          className="flex items-center gap-3.5 p-4 rounded-xl bg-blue-50 border border-blue-100 shadow-xs hover:border-brand/40 transition-colors"
        >
          <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-brand text-white text-xl shadow-xs">
            ⚡
          </span>
          <div>
            <p className="text-xs font-bold uppercase tracking-wider text-brand">Cần sửa gấp?</p>
            <p className="text-sm font-bold text-slate-900 mt-0.5">Đăng yêu cầu mới →</p>
          </div>
        </a>

        <a
          href="/yeu-cau-cua-toi"
          className="flex items-center gap-3.5 p-4 rounded-2xl bg-white border border-slate-200 shadow-xs hover:shadow-card hover:border-slate-300 transition-all"
        >
          <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-amber-50 text-amber-600 text-xl border border-amber-200">
            📋
          </span>
          <div>
            <p className="text-xs font-semibold text-slate-500">Đơn sửa chữa</p>
            <p className="text-sm font-bold text-slate-900 mt-0.5">Xem tiến độ & báo giá</p>
          </div>
        </a>

        <div className="flex items-center gap-3.5 p-4 rounded-2xl bg-white border border-slate-200 shadow-xs">
          <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600 text-xl border border-emerald-200">
            🛡️
          </span>
          <div>
            <p className="text-xs font-semibold text-slate-500">Bảo vệ khách hàng</p>
            <p className="text-sm font-bold text-emerald-700 mt-0.5">Bảo đảm an toàn 100%</p>
          </div>
        </div>
      </div>

      {status === 'loading' && (
        <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
          {[0, 1].map((i) => (
            <div key={i} className="animate-pulse rounded-2xl border border-slate-200 bg-white p-6 shadow-xs">
              <div className="h-5 w-40 rounded bg-slate-200" />
              <div className="mt-4 space-y-3">
                <div className="h-11 rounded bg-slate-100" />
                <div className="h-11 rounded bg-slate-100" />
                <div className="h-11 rounded bg-slate-100" />
              </div>
            </div>
          ))}
        </div>
      )}

      {status === 'error' && (
        <Alert tone="error" title="Chưa tải được hồ sơ">
          {loadError}
        </Alert>
      )}

      {status === 'ready' && (
        <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
          <Card title="Hồ sơ của tôi" description="Thông tin này hiển thị cho thợ nhận việc khi họ liên hệ với bạn.">
            <form onSubmit={handleSave} noValidate className="space-y-5">
              {saveError && <Alert tone="error">{saveError}</Alert>}
              {saved && !saveError && (
                <Alert tone="success">Đã lưu thay đổi vào hồ sơ của bạn.</Alert>
              )}

              <TextField
                label="Họ và tên"
                name="fullName"
                required
                value={form.fullName}
                error={errors.fullName}
                onChange={(e) => update('fullName', e.target.value)}
              />
              <TextField
                label="Số điện thoại"
                name="phone"
                type="tel"
                inputMode="numeric"
                required
                value={form.phone}
                error={errors.phone}
                onChange={(e) => update('phone', e.target.value)}
              />
              <TextField
                label="Email"
                name="email"
                type="email"
                required
                value={form.email}
                error={errors.email}
                onChange={(e) => update('email', e.target.value)}
              />
              <ImageUploadField
                label="Ảnh đại diện"
                folder="avatars"
                value={form.avatarUrl}
                error={errors.avatarUrl}
                hint="Ảnh vuông nhìn rõ mặt, tối đa 5 MB."
                onChange={(url) => update('avatarUrl', url)}
              />

              <div className="flex flex-col items-start gap-3 sm:flex-row sm:items-center">
                <Button type="submit" loading={saving}>
                  {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
                </Button>
                {profile && (
                  <p className="text-xs text-slate-400">
                    Cập nhật lần cuối: {formatDateTime(profile.updatedAt)}
                  </p>
                )}
              </div>
            </form>
          </Card>
        </div>
      )}
    </DashboardLayout>
  );
}
