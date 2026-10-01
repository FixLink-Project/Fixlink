import { useCallback, useEffect, useState, type FormEvent } from 'react';
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

interface AuditEntry {
  id: number;
  userId: number;
  action: string;
  entityName: string;
  entityId: string;
  oldValues: string | null;
  newValues: string | null;
  createdAt: string;
}

const FIELD_LABELS: Record<string, string> = {
  fullName: 'Họ và tên',
  phone: 'Số điện thoại',
  email: 'Email',
  avatarUrl: 'Ảnh đại diện'
};

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

/** So hai khối JSON cũ/mới để chỉ hiện những trường thực sự thay đổi. */
function diffFields(oldRaw: string | null, newRaw: string | null) {
  let before: Record<string, unknown> = {};
  let after: Record<string, unknown> = {};
  try {
    before = oldRaw ? JSON.parse(oldRaw) : {};
    after = newRaw ? JSON.parse(newRaw) : {};
  } catch {
    return [];
  }

  return Object.keys({ ...before, ...after })
    .filter((key) => String(before[key] ?? '') !== String(after[key] ?? ''))
    .map((key) => ({
      field: FIELD_LABELS[key] ?? key,
      before: String(before[key] ?? '(trống)'),
      after: String(after[key] ?? '(trống)')
    }));
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
  const [audit, setAudit] = useState<AuditEntry[]>([]);
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading');
  const [loadError, setLoadError] = useState<string | null>(null);

  const [form, setForm] = useState({ fullName: '', phone: '', email: '', avatarUrl: '' });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  const loadAudit = useCallback(async () => {
    const res = await api.get<AuditEntry[]>(`/customers/${userId}/audit-trail`);
    setAudit(res.data);
  }, [userId]);

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
        await loadAudit();
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
  }, [userId, loadAudit]);

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
      await loadAudit();
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
          className="flex items-center gap-3.5 p-4 rounded-2xl bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-100 shadow-xs hover:shadow-card hover:border-brand/40 transition-all"
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
            <p className="text-sm font-bold text-emerald-700 mt-0.5">Bảo hiểm Escrow 100%</p>
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

          <Card
            title="Lịch sử thay đổi tài khoản"
            description="Mọi chỉnh sửa hồ sơ đều được ghi lại an toàn trên hệ thống."
          >
            {audit.length === 0 ? (
              <div className="py-8 text-center">
                <p className="text-3xl">📋</p>
                <p className="mt-2 font-bold text-slate-800">Chưa có thay đổi nào được ghi nhận</p>
                <p className="mt-1 text-xs text-slate-500 max-w-xs mx-auto">
                  Lần đầu bạn sửa đổi thông tin cá nhân, bản ghi đối chiếu sẽ xuất hiện ở đây.
                </p>
              </div>
            ) : (
              <ol className="divide-y divide-slate-100">
                {audit.map((entry) => {
                  const changes = diffFields(entry.oldValues, entry.newValues);
                  return (
                    <li key={entry.id} className="py-3.5 first:pt-0 last:pb-0">
                      <div className="flex flex-wrap items-baseline justify-between gap-2">
                        <p className="font-bold text-slate-800 text-sm">Cập nhật hồ sơ</p>
                        <time className="text-xs text-slate-400" dateTime={entry.createdAt}>
                          {formatDateTime(entry.createdAt)}
                        </time>
                      </div>
                      {changes.length > 0 ? (
                        <ul className="mt-2 space-y-1 text-xs">
                          {changes.map((change) => (
                            <li key={change.field} className="text-slate-600">
                              <span className="font-semibold text-slate-700">{change.field}:</span> {change.before}{' '}
                              → <span className="font-bold text-brand">{change.after}</span>
                            </li>
                          ))}
                        </ul>
                      ) : (
                        <p className="mt-1 text-xs text-slate-400">Không có trường nào đổi giá trị.</p>
                      )}
                    </li>
                  );
                })}
              </ol>
            )}
          </Card>
        </div>
      )}
    </DashboardLayout>
  );
}
