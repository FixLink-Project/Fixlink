import { useCallback, useEffect, useState, type FormEvent } from 'react';
import Alert from '../components/Alert';
import Button from '../components/Button';
import Card from '../components/Card';
import CheckboxGroup from '../components/CheckboxGroup';
import DashboardLayout from '../components/DashboardLayout';
import StatusChip from '../components/StatusChip';
import TextArea from '../components/TextArea';
import TextField from '../components/TextField';
import { ApiError, api, formatCurrency, toFieldErrors } from '../lib/api';
import { useAuth } from '../lib/auth';
import type { ServiceArea, ServiceCategory, VerificationStatus } from '../lib/types';

interface SimpleRef {
  id: number;
  name: string;
}

interface TechnicianProfile {
  userId: number;
  fullName: string;
  phone: string;
  email: string;
  avatarUrl: string | null;
  bio: string | null;
  yearsExperience: number | null;
  verificationStatus: VerificationStatus;
  isOnline: boolean;
  avgRating: number;
  completedJobs: number;
  walletBalance: number;
  categories: SimpleRef[];
  areas: SimpleRef[];
}

interface RepairRequest {
  id: string;
  customerName: string;
  customerPhone: string;
  serviceType: string;
  address: string;
  description: string;
  status: string;
  scheduledTime: string;
}

const VERIFICATION_NOTICE: Record<VerificationStatus, { tone: 'warning' | 'success' | 'error'; title: string; body: string }> = {
  PENDING: {
    tone: 'warning',
    title: 'Hồ sơ đang chờ xác minh',
    body: 'Quản trị viên đang đối chiếu căn cước của bạn. Trong lúc chờ, bạn xem và sửa được hồ sơ nhưng chưa nhận được yêu cầu sửa chữa.'
  },
  APPROVED: {
    tone: 'success',
    title: 'Hồ sơ đã được xác minh',
    body: 'Bật chế độ nhận việc để khách hàng quanh khu vực bạn chọn nhìn thấy bạn.'
  },
  REJECTED: {
    tone: 'error',
    title: 'Hồ sơ bị từ chối',
    body: 'Ảnh căn cước chưa đạt yêu cầu đối chiếu. Cập nhật lại ảnh rõ nét hai mặt rồi gửi lại, hoặc gọi 1900 1234 để được hướng dẫn.'
  }
};

export default function TechnicianDashboardPage() {
  const { updateUser } = useAuth();

  const [profile, setProfile] = useState<TechnicianProfile | null>(null);
  const [requests, setRequests] = useState<RepairRequest[]>([]);
  const [requestsLocked, setRequestsLocked] = useState(false);
  const [categories, setCategories] = useState<ServiceCategory[]>([]);
  const [areas, setAreas] = useState<ServiceArea[]>([]);
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading');
  const [loadError, setLoadError] = useState<string | null>(null);

  const [form, setForm] = useState({
    fullName: '',
    phone: '',
    email: '',
    bio: '',
    yearsExperience: '',
    avatarUrl: ''
  });
  const [categoryIds, setCategoryIds] = useState<number[]>([]);
  const [areaIds, setAreaIds] = useState<number[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  const [togglingOnline, setTogglingOnline] = useState(false);
  const [onlineError, setOnlineError] = useState<string | null>(null);

  const applyProfile = useCallback((data: TechnicianProfile) => {
    setProfile(data);
    setForm({
      fullName: data.fullName ?? '',
      phone: data.phone ?? '',
      email: data.email ?? '',
      bio: data.bio ?? '',
      yearsExperience: data.yearsExperience != null ? String(data.yearsExperience) : '',
      avatarUrl: data.avatarUrl ?? ''
    });
    setCategoryIds(data.categories.map((c) => c.id));
    setAreaIds(data.areas.map((a) => a.id));
  }, []);

  const loadRequests = useCallback(async () => {
    try {
      const res = await api.get<RepairRequest[]>('/technicians/me/repair-requests');
      setRequests(res.data);
      setRequestsLocked(false);
    } catch (err) {
      // Thợ chưa được duyệt bị chặn 403; đó là trạng thái hợp lệ, không phải lỗi tải.
      if (err instanceof ApiError && err.statusCode === 403) {
        setRequests([]);
        setRequestsLocked(true);
      } else {
        throw err;
      }
    }
  }, []);

  useEffect(() => {
    let alive = true;

    (async () => {
      try {
        const [profileRes, categoryRes, areaRes] = await Promise.all([
          api.get<TechnicianProfile>('/technicians/me/profile'),
          api.get<ServiceCategory[]>('/categories'),
          api.get<ServiceArea[]>('/areas')
        ]);
        if (!alive) return;
        applyProfile(profileRes.data);
        setCategories(categoryRes.data.filter((c) => c.isActive));
        setAreas(areaRes.data);
        setStatus('ready');
        await loadRequests();
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
  }, [applyProfile, loadRequests]);

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

  async function handleToggleOnline() {
    if (!profile) return;
    setOnlineError(null);
    setTogglingOnline(true);
    try {
      const res = await api.patch<TechnicianProfile>('/technicians/me/status/online', {
        isOnline: !profile.isOnline
      });
      setProfile((current) => (current ? { ...current, isOnline: res.data.isOnline } : current));
    } catch (err) {
      setOnlineError(
        err instanceof ApiError ? err.message : 'Không đổi được trạng thái. Thử lại sau.'
      );
    } finally {
      setTogglingOnline(false);
    }
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
    if (form.yearsExperience.trim()) {
      const years = Number(form.yearsExperience);
      if (!Number.isInteger(years) || years < 0 || years > 60) {
        clientErrors.yearsExperience = 'Số năm kinh nghiệm là một số từ 0 đến 60.';
      }
    }
    if (Object.keys(clientErrors).length > 0) {
      setErrors(clientErrors);
      return;
    }

    setSaving(true);
    try {
      const res = await api.put<TechnicianProfile>('/technicians/me/profile', {
        fullName: form.fullName.trim(),
        phone: form.phone.trim(),
        email: form.email.trim(),
        bio: form.bio.trim() || null,
        yearsExperience: form.yearsExperience.trim() ? Number(form.yearsExperience) : null,
        avatarUrl: form.avatarUrl.trim() || null,
        categoryIds,
        areaIds
      });
      applyProfile(res.data);
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

  const approved = profile?.verificationStatus === 'APPROVED';
  const notice = profile ? VERIFICATION_NOTICE[profile.verificationStatus] : null;

  return (
    <DashboardLayout
      title="Bảng điều khiển thợ"
      description="Theo dõi trạng thái hồ sơ, bật tắt nhận việc và xem yêu cầu sửa chữa gửi tới bạn."
      actions={profile && <StatusChip status={profile.verificationStatus} />}
    >
      {status === 'loading' && (
        <div className="space-y-6">
          <div className="h-20 animate-pulse rounded-xl border border-line bg-card" />
          <div className="grid gap-6 lg:grid-cols-2">
            <div className="h-72 animate-pulse rounded-xl border border-line bg-card" />
            <div className="h-72 animate-pulse rounded-xl border border-line bg-card" />
          </div>
        </div>
      )}

      {status === 'error' && (
        <Alert tone="error" title="Chưa tải được bảng điều khiển">
          {loadError}
        </Alert>
      )}

      {status === 'ready' && profile && notice && (
        <div className="space-y-6">
          <Alert tone={notice.tone} title={notice.title}>
            {notice.body}
          </Alert>

          {/* Ba số liệu, dùng Space Grotesk cho phần số */}
          <dl className="grid gap-px overflow-hidden rounded-xl border border-line bg-line sm:grid-cols-3">
            <div className="bg-card px-5 py-4">
              <dt className="text-sm text-ink-soft">Việc đã hoàn tất</dt>
              <dd className="mt-1 font-display text-2xl font-semibold">{profile.completedJobs}</dd>
            </div>
            <div className="bg-card px-5 py-4">
              <dt className="text-sm text-ink-soft">Điểm đánh giá</dt>
              <dd className="mt-1 font-display text-2xl font-semibold">
                {profile.completedJobs > 0 ? Number(profile.avgRating).toFixed(1) : 'Chưa có'}
              </dd>
            </div>
            <div className="bg-card px-5 py-4">
              <dt className="text-sm text-ink-soft">Số dư ví</dt>
              <dd className="mt-1 font-display text-2xl font-semibold">
                {formatCurrency(profile.walletBalance)}
              </dd>
            </div>
          </dl>

          <Card
            title="Nhận yêu cầu sửa chữa"
            description={
              approved
                ? 'Bật lên khi bạn đang rảnh. Tắt đi thì khách không nhìn thấy bạn nữa.'
                : 'Mở khoá sau khi quản trị viên xác minh hồ sơ.'
            }
            actions={
              <Button
                variant={profile.isOnline ? 'secondary' : 'primary'}
                loading={togglingOnline}
                disabled={!approved}
                onClick={handleToggleOnline}
              >
                {profile.isOnline ? 'Tạm dừng nhận việc' : 'Bật nhận việc'}
              </Button>
            }
          >
            {onlineError && <Alert tone="error">{onlineError}</Alert>}
            <p className="text-ink-soft">
              Trạng thái hiện tại:{' '}
              <StatusChip
                status={profile.isOnline ? 'ACTIVE' : 'INACTIVE'}
                label={profile.isOnline ? 'Đang nhận việc' : 'Đang tạm dừng'}
              />
            </p>
          </Card>

          <div className="grid gap-6 lg:grid-cols-2">
            <Card title="Hồ sơ tay nghề" description="Khách hàng xem phần này trước khi chọn thợ.">
              <form onSubmit={handleSave} noValidate className="space-y-5">
                {saveError && <Alert tone="error">{saveError}</Alert>}
                {saved && !saveError && <Alert tone="success">Đã lưu hồ sơ của bạn.</Alert>}

                <TextField
                  label="Họ và tên"
                  name="fullName"
                  required
                  value={form.fullName}
                  error={errors.fullName}
                  onChange={(e) => update('fullName', e.target.value)}
                />
                <div className="grid gap-5 sm:grid-cols-2">
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
                    label="Số năm kinh nghiệm"
                    name="yearsExperience"
                    type="number"
                    min={0}
                    max={60}
                    value={form.yearsExperience}
                    error={errors.yearsExperience}
                    onChange={(e) => update('yearsExperience', e.target.value)}
                  />
                </div>
                <TextField
                  label="Email"
                  name="email"
                  type="email"
                  required
                  value={form.email}
                  error={errors.email}
                  onChange={(e) => update('email', e.target.value)}
                />
                <TextArea
                  label="Giới thiệu ngắn"
                  name="bio"
                  rows={3}
                  maxLength={500}
                  value={form.bio}
                  error={errors.bio}
                  hint={`${form.bio.length}/500 ký tự`}
                  onChange={(e) => update('bio', e.target.value)}
                />

                <CheckboxGroup
                  legend="Nhóm việc bạn nhận"
                  description="Khách chọn nhóm việc trước, chỉ thợ có chuyên môn tương ứng mới nhận được yêu cầu."
                  options={categories.map((c) => ({ id: c.id, label: c.name }))}
                  selected={categoryIds}
                  onChange={(next) => {
                    setCategoryIds(next);
                    setSaved(false);
                  }}
                />

                <CheckboxGroup
                  legend="Khu vực bạn tới được"
                  options={areas.map((a) => ({ id: a.id, label: a.name, hint: a.city }))}
                  selected={areaIds}
                  onChange={(next) => {
                    setAreaIds(next);
                    setSaved(false);
                  }}
                />

                <Button type="submit" loading={saving}>
                  {saving ? 'Đang lưu...' : 'Lưu hồ sơ'}
                </Button>
              </form>
            </Card>

            <Card
              title="Yêu cầu gửi tới bạn"
              description={approved ? 'Gọi cho khách để xác nhận trước khi tới.' : undefined}
            >
              {requestsLocked ? (
                <div className="py-8 text-center">
                  <p className="font-display font-semibold">Chưa mở khoá phần này</p>
                  <p className="mx-auto mt-2 max-w-sm text-sm text-ink-soft">
                    Yêu cầu sửa chữa chỉ hiện ra sau khi hồ sơ của bạn được xác minh. Trong lúc
                    chờ, hãy hoàn thiện nhóm việc và khu vực hoạt động để được ghép đúng khách.
                  </p>
                </div>
              ) : requests.length === 0 ? (
                <div className="py-8 text-center">
                  <p className="font-display font-semibold">Chưa có yêu cầu nào</p>
                  <p className="mt-2 text-sm text-ink-soft">
                    Bật chế độ nhận việc và chọn thêm khu vực để tiếp cận nhiều khách hơn.
                  </p>
                </div>
              ) : (
                <ul className="divide-y divide-line">
                  {requests.map((request) => (
                    <li key={request.id} className="py-4 first:pt-0 last:pb-0">
                      <div className="flex flex-wrap items-start justify-between gap-2">
                        <h3 className="font-display font-semibold">{request.serviceType}</h3>
                        <StatusChip status={request.status} label="Yêu cầu mới" />
                      </div>
                      <p className="mt-1.5 text-sm text-ink-soft">{request.description}</p>
                      <dl className="mt-3 space-y-1 text-sm">
                        <div className="flex gap-2">
                          <dt className="w-24 shrink-0 text-ink-soft">Khách hàng</dt>
                          <dd>
                            {request.customerName}{' '}
                            <a
                              href={`tel:${request.customerPhone}`}
                              className="text-brand hover:text-brand-strong hover:underline"
                            >
                              {request.customerPhone}
                            </a>
                          </dd>
                        </div>
                        <div className="flex gap-2">
                          <dt className="w-24 shrink-0 text-ink-soft">Địa chỉ</dt>
                          <dd>{request.address}</dd>
                        </div>
                        <div className="flex gap-2">
                          <dt className="w-24 shrink-0 text-ink-soft">Hẹn lúc</dt>
                          <dd>{request.scheduledTime}</dd>
                        </div>
                      </dl>
                    </li>
                  ))}
                </ul>
              )}
            </Card>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}
