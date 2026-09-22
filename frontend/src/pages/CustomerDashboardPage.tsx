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
      title={`Chào ${profile?.fullName?.trim() || user?.username || 'bạn'}`}
      description="Giữ thông tin liên hệ chính xác để thợ gọi đúng số khi tới sửa."
    >
      {status === 'loading' && (
        <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
          {[0, 1].map((i) => (
            <div key={i} className="animate-pulse rounded-xl border border-line bg-card p-5">
              <div className="h-5 w-40 rounded bg-line" />
              <div className="mt-4 space-y-3">
                <div className="h-11 rounded bg-line/70" />
                <div className="h-11 rounded bg-line/70" />
                <div className="h-11 rounded bg-line/70" />
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
          <Card title="Hồ sơ của tôi" description="Thông tin này hiển thị cho thợ nhận việc.">
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
                  <p className="text-sm text-ink-soft">
                    Cập nhật lần cuối {formatDateTime(profile.updatedAt)}
                  </p>
                )}
              </div>
            </form>
          </Card>

          <Card
            title="Lịch sử thay đổi"
            description="Mọi chỉnh sửa hồ sơ đều được ghi lại để đối chiếu khi có tranh chấp."
          >
            {audit.length === 0 ? (
              <div className="py-6 text-center">
                <p className="font-medium">Chưa có thay đổi nào được ghi nhận.</p>
                <p className="mt-1 text-sm text-ink-soft">
                  Lần đầu bạn sửa hồ sơ, bản ghi sẽ xuất hiện ở đây.
                </p>
              </div>
            ) : (
              <ol className="divide-y divide-line">
                {audit.map((entry) => {
                  const changes = diffFields(entry.oldValues, entry.newValues);
                  return (
                    <li key={entry.id} className="py-4 first:pt-0 last:pb-0">
                      <div className="flex flex-wrap items-baseline justify-between gap-2">
                        <p className="font-display font-semibold">Cập nhật hồ sơ</p>
                        <time className="text-sm text-ink-soft" dateTime={entry.createdAt}>
                          {formatDateTime(entry.createdAt)}
                        </time>
                      </div>
                      {changes.length > 0 ? (
                        <ul className="mt-2 space-y-1 text-sm">
                          {changes.map((change) => (
                            <li key={change.field} className="text-ink-soft">
                              <span className="text-ink">{change.field}:</span> {change.before}{' '}
                              thành <span className="text-ink">{change.after}</span>
                            </li>
                          ))}
                        </ul>
                      ) : (
                        <p className="mt-1 text-sm text-ink-soft">Không có trường nào đổi giá trị.</p>
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
