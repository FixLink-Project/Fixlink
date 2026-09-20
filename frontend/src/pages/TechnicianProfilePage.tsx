import { useCallback, useEffect, useState, type FormEvent } from 'react';
import Alert from '../components/Alert';
import Button from '../components/Button';
import Card from '../components/Card';
import CheckboxGroup from '../components/CheckboxGroup';
import DashboardLayout from '../components/DashboardLayout';
import ImageUploadField from '../components/ImageUploadField';
import StatusChip from '../components/StatusChip';
import Tabs from '../components/Tabs';
import TextArea from '../components/TextArea';
import TextField from '../components/TextField';
import { ApiError, api, toFieldErrors } from '../lib/api';
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
  citizenId: string;
  categories: SimpleRef[];
  areas: SimpleRef[];
}

type TabId = 'CA_NHAN' | 'CHUYEN_MON' | 'BAO_MAT';

const TABS = [
  { id: 'CA_NHAN' as const, label: 'Thông tin cá nhân' },
  { id: 'CHUYEN_MON' as const, label: 'Chuyên môn & Khu vực' },
  { id: 'BAO_MAT' as const, label: 'Bảo mật' }
];

const TECHNICIAN_NAV = [
  { to: '/tho', label: 'Bảng điều khiển' },
  { to: '/tho/ho-so', label: 'Hồ sơ của tôi' }
];

export default function TechnicianProfilePage() {
  const { updateUser, signOut } = useAuth();
  const [tab, setTab] = useState<TabId>('CA_NHAN');

  const [profile, setProfile] = useState<TechnicianProfile | null>(null);
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
  const [savedTab, setSavedTab] = useState<TabId | null>(null);

  const [passwordForm, setPasswordForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  const [passwordErrors, setPasswordErrors] = useState<Record<string, string>>({});
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [changingPassword, setChangingPassword] = useState(false);
  const [passwordChanged, setPasswordChanged] = useState(false);

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
  }, [applyProfile]);

  function update(field: keyof typeof form, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
    setSavedTab(null);
    setErrors((current) => {
      if (!current[field]) return current;
      const next = { ...current };
      delete next[field];
      return next;
    });
  }

  async function saveProfile(event: FormEvent, fromTab: TabId) {
    event.preventDefault();
    setSaveError(null);
    setSavedTab(null);

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
      setSavedTab(fromTab);
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

  async function handleChangePassword(event: FormEvent) {
    event.preventDefault();
    setPasswordError(null);
    setPasswordChanged(false);

    const clientErrors: Record<string, string> = {};
    if (!passwordForm.currentPassword) {
      clientErrors.currentPassword = 'Nhập mật khẩu hiện tại để xác nhận là bạn.';
    }
    if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/.test(passwordForm.newPassword)) {
      clientErrors.newPassword =
        'Mật khẩu cần ít nhất 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt.';
    }
    if (passwordForm.confirmPassword !== passwordForm.newPassword) {
      clientErrors.confirmPassword = 'Hai lần nhập mật khẩu chưa giống nhau.';
    }
    if (Object.keys(clientErrors).length > 0) {
      setPasswordErrors(clientErrors);
      return;
    }

    setChangingPassword(true);
    try {
      await api.put('/auth/change-password', passwordForm);
      setPasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
      setPasswordErrors({});
      setPasswordChanged(true);
    } catch (err) {
      const fieldErrors = toFieldErrors(err);
      if (Object.keys(fieldErrors).length > 0) {
        setPasswordErrors(fieldErrors);
      } else if (err instanceof ApiError) {
        setPasswordError(err.message);
      } else {
        setPasswordError('Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.');
      }
    } finally {
      setChangingPassword(false);
    }
  }

  function updatePassword(field: keyof typeof passwordForm, value: string) {
    setPasswordForm((current) => ({ ...current, [field]: value }));
    setPasswordChanged(false);
    setPasswordErrors((current) => {
      if (!current[field]) return current;
      const next = { ...current };
      delete next[field];
      return next;
    });
  }

  return (
    <DashboardLayout
      nav={TECHNICIAN_NAV}
      title="Hồ sơ của tôi"
      description="Thông tin bạn khai ở đây quyết định khách nào nhìn thấy bạn."
      actions={profile && <StatusChip status={profile.verificationStatus} />}
    >
      {status === 'loading' && (
        <div className="h-80 animate-pulse rounded-xl border border-line bg-card" />
      )}

      {status === 'error' && (
        <Alert tone="error" title="Chưa tải được hồ sơ">
          {loadError}
        </Alert>
      )}

      {status === 'ready' && profile && (
        <Tabs label="Các phần của hồ sơ" items={TABS} active={tab} onChange={setTab}>
          {saveError && (
            <div className="mb-5">
              <Alert tone="error">{saveError}</Alert>
            </div>
          )}

          {tab === 'CA_NHAN' && (
            <Card title="Thông tin cá nhân" description="Khách gọi vào số này khi hẹn giờ sửa.">
              <form onSubmit={(e) => saveProfile(e, 'CA_NHAN')} noValidate className="space-y-5">
                {savedTab === 'CA_NHAN' && <Alert tone="success">Đã lưu thông tin cá nhân.</Alert>}

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
                    label="Email"
                    name="email"
                    type="email"
                    required
                    value={form.email}
                    error={errors.email}
                    onChange={(e) => update('email', e.target.value)}
                  />
                </div>

                <TextField
                  label="Số căn cước"
                  name="citizenId"
                  value={profile.citizenId}
                  disabled
                  hint="Đã dùng để xác minh danh tính nên không tự sửa được. Cần đổi thì gọi 1900 1234."
                  onChange={() => undefined}
                />

                <ImageUploadField
                  label="Ảnh đại diện"
                  folder="avatars"
                  value={form.avatarUrl}
                  error={errors.avatarUrl}
                  hint="Khách nhìn ảnh này khi chọn thợ. Tối đa 5 MB."
                  onChange={(url) => update('avatarUrl', url)}
                />

                <Button type="submit" loading={saving}>
                  {saving ? 'Đang lưu...' : 'Lưu thông tin'}
                </Button>
              </form>
            </Card>
          )}

          {tab === 'CHUYEN_MON' && (
            <Card
              title="Chuyên môn và khu vực"
              description="Hệ thống chỉ gửi yêu cầu khớp cả nhóm việc lẫn địa bàn bạn chọn."
            >
              <form onSubmit={(e) => saveProfile(e, 'CHUYEN_MON')} noValidate className="space-y-6">
                {savedTab === 'CHUYEN_MON' && (
                  <Alert tone="success">Đã lưu chuyên môn và khu vực hoạt động.</Alert>
                )}

                <div className="grid gap-5 sm:grid-cols-2">
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

                <TextArea
                  label="Giới thiệu ngắn"
                  name="bio"
                  rows={4}
                  maxLength={500}
                  value={form.bio}
                  error={errors.bio}
                  hint={`${form.bio.length}/500 ký tự`}
                  onChange={(e) => update('bio', e.target.value)}
                />

                <CheckboxGroup
                  legend="Nhóm việc bạn nhận"
                  options={categories.map((c) => ({ id: c.id, label: c.name }))}
                  selected={categoryIds}
                  onChange={(next) => {
                    setCategoryIds(next);
                    setSavedTab(null);
                  }}
                />

                <CheckboxGroup
                  legend="Khu vực bạn tới được"
                  options={areas.map((a) => ({ id: a.id, label: a.name, hint: a.city }))}
                  selected={areaIds}
                  onChange={(next) => {
                    setAreaIds(next);
                    setSavedTab(null);
                  }}
                />

                <Button type="submit" loading={saving}>
                  {saving ? 'Đang lưu...' : 'Lưu chuyên môn'}
                </Button>
              </form>
            </Card>
          )}

          {tab === 'BAO_MAT' && (
            <Card
              title="Đổi mật khẩu"
              description="Đổi xong, mọi thiết bị khác đang đăng nhập sẽ bị đăng xuất."
            >
              <form onSubmit={handleChangePassword} noValidate className="max-w-md space-y-5">
                {passwordError && <Alert tone="error">{passwordError}</Alert>}
                {passwordChanged && (
                  <Alert tone="success" title="Đã đổi mật khẩu">
                    Lần đăng nhập sau hãy dùng mật khẩu mới.{' '}
                    <button
                      type="button"
                      onClick={signOut}
                      className="underline hover:text-brand-strong"
                    >
                      Đăng xuất khỏi thiết bị này
                    </button>
                  </Alert>
                )}

                <TextField
                  label="Mật khẩu hiện tại"
                  name="currentPassword"
                  type="password"
                  autoComplete="current-password"
                  required
                  value={passwordForm.currentPassword}
                  error={passwordErrors.currentPassword}
                  onChange={(e) => updatePassword('currentPassword', e.target.value)}
                />
                <TextField
                  label="Mật khẩu mới"
                  name="newPassword"
                  type="password"
                  autoComplete="new-password"
                  required
                  value={passwordForm.newPassword}
                  error={passwordErrors.newPassword}
                  hint="Ít nhất 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt."
                  onChange={(e) => updatePassword('newPassword', e.target.value)}
                />
                <TextField
                  label="Nhập lại mật khẩu mới"
                  name="confirmPassword"
                  type="password"
                  autoComplete="new-password"
                  required
                  value={passwordForm.confirmPassword}
                  error={passwordErrors.confirmPassword}
                  onChange={(e) => updatePassword('confirmPassword', e.target.value)}
                />

                <Button type="submit" loading={changingPassword}>
                  {changingPassword ? 'Đang đổi...' : 'Đổi mật khẩu'}
                </Button>
              </form>
            </Card>
          )}
        </Tabs>
      )}
    </DashboardLayout>
  );
}
