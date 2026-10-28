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
import { ApiError, api, fetchTechnicianReviews, toFieldErrors, type TechnicianReviewItem } from '../lib/api';
import { useAuth } from '../lib/auth';
import type { ServiceArea, VerificationStatus } from '../lib/types';

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

type TabId = 'CA_NHAN' | 'CHUYEN_MON' | 'DANH_GIA' | 'BAO_MAT';

const TABS = [
  { id: 'CA_NHAN' as const, label: 'Thông tin cá nhân' },
  { id: 'CHUYEN_MON' as const, label: 'Chuyên môn & Khu vực' },
  { id: 'DANH_GIA' as const, label: '⭐ Đánh giá từ khách hàng' },
  { id: 'BAO_MAT' as const, label: 'Bảo mật' }
];

const TECHNICIAN_NAV = [
  { to: '/tho', label: 'Bảng điều khiển' },
  { to: '/tho/lich-su', label: 'Việc đã hoàn thành' },
  { to: '/tho/ho-so', label: 'Hồ sơ của tôi' }
];

export default function TechnicianProfilePage() {
  const { updateUser, signOut } = useAuth();
  const [tab, setTab] = useState<TabId>('CA_NHAN');

  const [profile, setProfile] = useState<TechnicianProfile | null>(null);
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

  // Danh sách đánh giá từ khách hàng
  const [reviews, setReviews] = useState<TechnicianReviewItem[]>([]);
  const [loadingReviews, setLoadingReviews] = useState(false);

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
        const [profileRes, areaRes] = await Promise.all([
          api.get<TechnicianProfile>('/technicians/me/profile'),
          api.get<ServiceArea[]>('/areas')
        ]);
        if (!alive) return;
        applyProfile(profileRes.data);
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

  // Tải danh sách đánh giá từ khách hàng khi chuyển sang tab DANH_GIA
  useEffect(() => {
    if (tab === 'DANH_GIA' && profile?.userId) {
      setLoadingReviews(true);
      fetchTechnicianReviews(profile.userId)
        .then((res) => setReviews(res.data || []))
        .catch(() => setReviews([]))
        .finally(() => setLoadingReviews(false));
    }
  }, [tab, profile?.userId]);

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
        <div className="h-80 animate-pulse rounded-2xl border border-white/10 bg-surface-card" />
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

                {/* Chuyên môn được cấp phép hành nghề - Chỉ đọc, do Admin thẩm định offline */}
                <div className="space-y-3 rounded-2xl border border-slate-200/90 bg-slate-50/70 p-4">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <span className="text-sm font-bold text-slate-900">
                      Chuyên môn sửa chữa được cấp phép
                    </span>
                    <span className="inline-flex items-center gap-1 rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-0.5 text-xs font-semibold text-emerald-700">
                      <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                      </svg>
                      Đã qua phỏng vấn & thẩm định bởi Admin
                    </span>
                  </div>

                  {profile.categories.length > 0 ? (
                    <div className="flex flex-wrap gap-2 pt-1">
                      {profile.categories.map((c) => (
                        <span
                          key={c.id}
                          className="inline-flex items-center gap-1.5 rounded-xl border border-blue-200 bg-white px-3.5 py-2 text-xs font-bold text-brand shadow-2xs"
                        >
                          🛠️ {c.name}
                        </span>
                      ))}
                    </div>
                  ) : (
                    <div className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-xs text-amber-800">
                      ⚠️ Chưa có chuyên môn nào được phê duyệt. Vui lòng liên hệ Admin để phỏng vấn sát hạch.
                    </div>
                  )}

                  <p className="text-xs text-slate-500 leading-relaxed pt-1">
                    * Nhằm đảm bảo an toàn kỹ thuật cho khách hàng, danh mục chuyên môn được Ban quản trị FixLink trực tiếp phỏng vấn và cấp phép thực tế. Nếu bạn muốn mở rộng thêm chuyên môn sửa chữa mới, vui lòng liên hệ hotline <strong className="text-slate-700">1900 1234</strong> để đăng ký lịch sát hạch.
                  </p>
                </div>

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

          {tab === 'DANH_GIA' && (
            <Card
              title="Đánh giá từ khách hàng"
              description="Tổng hợp các nhận xét và chấm điểm sao từ các khách hàng mà bạn đã hoàn tất sửa chữa."
            >
              {loadingReviews ? (
                <div className="py-8 text-center text-sm text-slate-500 animate-pulse">
                  Đang tải danh sách đánh giá...
                </div>
              ) : reviews.length === 0 ? (
                <div className="py-10 text-center text-slate-500 text-sm bg-slate-50 rounded-xl border border-dashed border-slate-200">
                  <span className="text-3xl block mb-2">⭐</span>
                  Chưa có đánh giá nào từ khách hàng.<br />
                  Sau khi hoàn thành và nghiệm thu các đơn hàng được giao, đánh giá của khách hàng sẽ xuất hiện tại đây.
                </div>
              ) : (
                <div className="space-y-4">
                  {/* Tóm tắt điểm trung bình */}
                  <div className="flex items-center justify-between p-4 bg-amber-50/80 border border-amber-200 rounded-xl">
                    <div>
                      <p className="text-xs font-bold text-amber-900 uppercase tracking-wide">Điểm đánh giá trung bình</p>
                      <p className="text-2xl font-black text-amber-950 mt-0.5">
                        {(reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length).toFixed(1)} / 5.0
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="text-xs text-amber-800 font-semibold">{reviews.length} lượt đánh giá thực tế</p>
                      <div className="text-amber-500 text-lg">
                        {'★'.repeat(Math.round(reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length))}
                      </div>
                    </div>
                  </div>

                  {/* Danh sách từng đánh giá */}
                  <div className="divide-y divide-slate-100 border border-slate-200 rounded-xl overflow-hidden">
                    {reviews.map((rev) => (
                      <div key={rev.id} className="p-4 bg-white hover:bg-slate-50/60 transition-colors space-y-1.5">
                        <div className="flex items-center justify-between text-xs">
                          <div className="flex items-center gap-2">
                            <span className="font-bold text-slate-900">{rev.customerName || 'Khách hàng'}</span>
                            <span className="text-slate-400">• Đơn #{rev.requestId}</span>
                          </div>
                          <span className="text-amber-500 font-bold">
                            {'★'.repeat(rev.rating)}{'☆'.repeat(5 - rev.rating)}
                          </span>
                        </div>
                        <p className="text-xs text-slate-700 leading-relaxed font-medium">
                          "{rev.comment || 'Khách hàng không để lại nhận xét chi tiết.'}"
                        </p>
                        {rev.createdAt && (
                          <p className="text-[11px] text-slate-400">
                            {new Date(rev.createdAt).toLocaleDateString('vi-VN', {
                              year: 'numeric',
                              month: 'long',
                              day: 'numeric'
                            })}
                          </p>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}
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
                      className="underline hover:text-brand-glow"
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
