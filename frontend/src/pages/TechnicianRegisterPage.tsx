import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Alert from '../components/Alert';
import AuthLayout from '../components/AuthLayout';
import Button from '../components/Button';
import FormSection from '../components/FormSection';
import StatusChip from '../components/StatusChip';
import TextArea from '../components/TextArea';
import TextField from '../components/TextField';
import { ApiError, api, toFieldErrors } from '../lib/api';

const SPECIALIZATIONS = [
  { value: 'Điện tử', label: 'Điện tử (Tivi, Loa, Amply, Bo mạch, Thiết bị âm thanh...)' },
  { value: 'Điện lạnh', label: 'Điện lạnh (Máy lạnh, Tủ lạnh, Máy giặt, Máy sấy...)' },
  { value: 'Điện dân dụng', label: 'Điện dân dụng (Hệ thống điện nhà, Công tắc, Đèn chiếu sáng...)' },
  { value: 'Đồ gia dụng', label: 'Đồ gia dụng (Nồi cơm, Lò vi sóng, Bếp từ, Quạt điện, Máy lọc nước...)' },
  { value: 'Cấp thoát nước', label: 'Cấp thoát nước (Ống nước, Vòi sen, Bơm nước, Thiết bị vệ sinh...)' },
  { value: 'Khóa cửa & Cổng tự động', label: 'Khóa cửa & Cổng tự động (Khóa vân tay, Khóa cơ, Cổng từ...)' },
  { value: 'Thiết bị văn phòng', label: 'Thiết bị văn phòng (Máy in, Máy tính, Laptop, Mạng nội bộ...)' },
  { value: 'Khác', label: 'Chuyên môn khác' }
];

const EMPTY = {
  fullName: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
  address: '',
  specialization: '',
  citizenId: '',
  yearsExperience: '',
  bio: ''
};

function validate(form: typeof EMPTY): Record<string, string> {
  const errors: Record<string, string> = {};

  if (!form.fullName.trim()) {
    errors.fullName = 'Họ và tên không được để trống.';
  }
  if (!form.email.trim()) {
    errors.email = 'Email không được để trống.';
  } else if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(form.email.trim())) {
    errors.email = 'Email chưa đúng định dạng.';
  }
  if (!form.phone.trim()) {
    errors.phone = 'Số điện thoại không được để trống.';
  } else if (!/^0[35789][0-9]{8}$/.test(form.phone.trim())) {
    errors.phone = 'Số điện thoại cần 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09.';
  }
  if (!form.password) {
    errors.password = 'Mật khẩu không được để trống.';
  } else if (!/^(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{8,}$/.test(form.password)) {
    errors.password = 'Mật khẩu cần tối thiểu 8 ký tự, bao gồm ít nhất 1 chữ in hoa và 1 ký tự đặc biệt.';
  }
  if (form.confirmPassword !== form.password) {
    errors.confirmPassword = 'Hai lần nhập mật khẩu chưa trùng khớp.';
  }
  if (!form.address.trim()) {
    errors.address = 'Địa chỉ làm việc / khu vực hoạt động không được để trống.';
  }
  if (!form.specialization) {
    errors.specialization = 'Vui lòng chọn lĩnh vực chuyên môn chính.';
  }
  if (form.citizenId.trim() && !/^[0-9]{9,12}$/.test(form.citizenId.trim())) {
    errors.citizenId = 'Số CCCD gồm từ 9 đến 12 chữ số.';
  }
  if (form.yearsExperience.trim()) {
    const years = Number(form.yearsExperience);
    if (!Number.isInteger(years) || years < 0 || years > 60) {
      errors.yearsExperience = 'Số năm kinh nghiệm là một số từ 0 đến 60.';
    }
  }

  return errors;
}

export default function TechnicianRegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);

  function update(field: keyof typeof EMPTY, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
    setErrors((current) => {
      if (!current[field]) return current;
      const next = { ...current };
      delete next[field];
      return next;
    });
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setFormError(null);

    const clientErrors = validate(form);
    if (Object.keys(clientErrors).length > 0) {
      setErrors(clientErrors);
      return;
    }

    setSubmitting(true);
    try {
      await api.post('/auth/register/technician', {
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        phone: form.phone.trim(),
        password: form.password,
        address: form.address.trim(),
        specialization: form.specialization,
        citizenId: form.citizenId.trim() || null,
        bio: form.bio.trim() || null,
        yearsExperience: form.yearsExperience.trim() ? Number(form.yearsExperience) : null
      });

      setSuccess(true);
    } catch (err) {
      const fieldErrors = toFieldErrors(err);
      if (Object.keys(fieldErrors).length > 0) {
        setErrors(fieldErrors);
      } else if (err instanceof ApiError) {
        setFormError(err.message);
      } else {
        setFormError('Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (success) {
    return (
      <AuthLayout
        wide
        title="Đăng ký thành công!"
        description="Tài khoản thợ của bạn đã được ghi nhận vào hệ thống FixLink."
      >
        <div className="mt-6 space-y-6 text-center">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-emerald-100 text-emerald-600 shadow-xs">
            <svg className="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
            </svg>
          </div>

          <div className="rounded-2xl border border-emerald-200 bg-emerald-50/80 p-5 shadow-xs">
            <h3 className="font-display text-lg font-bold text-emerald-900">
              Đăng ký thành công! Tài khoản của bạn sẽ được admin xét duyệt.
            </h3>
            <p className="mt-2 text-sm text-emerald-800 leading-relaxed">
              Hồ sơ kỹ thuật viên của bạn đang ở trạng thái <strong className="font-semibold">Chờ duyệt (PENDING_APPROVAL)</strong>.
              Ban quản trị hệ thống sẽ kiểm tra và phê duyệt trong thời gian sớm nhất.
            </p>
          </div>

          <div className="rounded-xl border border-slate-200 bg-slate-50/80 p-4 text-left text-sm text-slate-600 space-y-2">
            <p className="font-semibold text-slate-800">Thông tin tài khoản đã đăng ký:</p>
            <div className="grid grid-cols-2 gap-2 text-xs sm:text-sm">
              <div><span className="text-slate-400">Họ và tên:</span> <span className="font-medium text-slate-900">{form.fullName}</span></div>
              <div><span className="text-slate-400">Số điện thoại:</span> <span className="font-medium text-slate-900">{form.phone}</span></div>
              <div><span className="text-slate-400">Email:</span> <span className="font-medium text-slate-900">{form.email}</span></div>
              <div><span className="text-slate-400">Chuyên môn:</span> <span className="font-medium text-slate-900">{form.specialization}</span></div>
              <div className="col-span-2"><span className="text-slate-400">Khu vực hoạt động:</span> <span className="font-medium text-slate-900">{form.address}</span></div>
            </div>
            <p className="text-xs text-slate-500 pt-2 italic">
              * Lưu ý: Trong lúc chờ duyệt, bạn chưa thể đăng nhập vào ứng dụng. Sau khi admin phê duyệt, bạn có thể đăng nhập bằng Số điện thoại và Mật khẩu vừa đặt.
            </p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row pt-2">
            <Button fullWidth onClick={() => navigate('/login')}>
              Về trang đăng nhập
            </Button>
            <Button fullWidth variant="secondary" onClick={() => navigate('/')}>
              Về trang chủ
            </Button>
          </div>
        </div>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout
      wide
      title="Đăng ký làm thợ kỹ thuật"
      description="Gia nhập mạng lưới thợ sửa chữa FixLink. Khai đúng thông tin để được admin xét duyệt nhanh nhất."
      footer={
        <div className="space-y-2 text-center text-sm text-ink-soft">
          <p>
            Đã có tài khoản?{' '}
            <Link to="/login" className="font-medium text-brand-glow hover:text-brand hover:underline">
              Đăng nhập
            </Link>
          </p>
          <p>
            Bạn là khách hàng cần sửa đồ?{' '}
            <Link to="/dang-ky" className="font-medium text-brand-glow hover:text-brand hover:underline">
              Đăng ký khách hàng
            </Link>
          </p>
        </div>
      }
    >
      <div className="mt-6 flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50/80 p-4 shadow-xs">
        <StatusChip status="PENDING_APPROVAL" />
        <p className="text-xs sm:text-sm text-amber-900 leading-relaxed">
          Tài khoản thợ tự đăng ký sẽ ở trạng thái <strong className="font-semibold">Chờ duyệt</strong>.
          Admin sẽ kiểm tra thông tin và kích hoạt tài khoản của bạn để nhận đơn sửa chữa.
        </p>
      </div>

      <form onSubmit={handleSubmit} noValidate className="mt-8 space-y-7">
        {formError && <Alert tone="error">{formError}</Alert>}

        <FormSection title="Thông tin cá nhân & Liên hệ" description="Thông tin dùng để đối chiếu hồ sơ và liên hệ làm việc">
          <TextField
            label="Họ và tên"
            name="fullName"
            autoComplete="name"
            required
            placeholder="Ví dụ: Nguyễn Văn A"
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
              autoComplete="tel"
              required
              placeholder="0912345678"
              value={form.phone}
              error={errors.phone}
              hint="Dùng để đăng nhập sau khi được duyệt."
              onChange={(e) => update('phone', e.target.value)}
            />
            <TextField
              label="Email"
              name="email"
              type="email"
              autoComplete="email"
              required
              placeholder="ban@gmail.com"
              value={form.email}
              error={errors.email}
              onChange={(e) => update('email', e.target.value)}
            />
          </div>

          <TextField
            label="Địa chỉ hoạt động"
            name="address"
            autoComplete="street-address"
            required
            placeholder="Số nhà, tên đường, phường/xã, quận/huyện, tỉnh/thành phố"
            value={form.address}
            error={errors.address}
            hint="Khu vực bạn có thể đến tận nơi sửa chữa cho khách."
            onChange={(e) => update('address', e.target.value)}
          />
        </FormSection>

        <FormSection title="Chuyên môn kỹ thuật" description="Chọn lĩnh vực dịch vụ sửa chữa chính bạn thực hiện">
          <div>
            <label htmlFor="specialization" className="mb-1.5 block text-sm font-medium text-ink">
              Lĩnh vực chuyên môn <span className="ml-0.5 text-brand-glow">*</span>
            </label>
            <select
              id="specialization"
              name="specialization"
              required
              value={form.specialization}
              onChange={(e) => update('specialization', e.target.value)}
              className={[
                'min-h-[44px] w-full rounded-xl border bg-white px-3.5 text-sm text-ink',
                'transition-all duration-200 shadow-xs focus:outline-none focus:ring-4 focus:ring-brand/10 focus:border-brand',
                errors.specialization ? 'border-danger focus:ring-danger/10 focus:border-danger' : 'border-line hover:border-line-strong'
              ].join(' ')}
            >
              <option value="">-- Chọn chuyên môn chính của bạn --</option>
              {SPECIALIZATIONS.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
            {errors.specialization && (
              <p className="mt-1.5 text-sm text-danger" role="alert">
                {errors.specialization}
              </p>
            )}
          </div>

          <div className="grid gap-5 sm:grid-cols-2">
            <TextField
              label="Số CCCD (không bắt buộc)"
              name="citizenId"
              inputMode="numeric"
              placeholder="079123456789"
              value={form.citizenId}
              error={errors.citizenId}
              hint="Giúp admin xác minh hồ sơ nhanh hơn."
              onChange={(e) => update('citizenId', e.target.value)}
            />
            <TextField
              label="Số năm kinh nghiệm"
              name="yearsExperience"
              type="number"
              inputMode="numeric"
              min={0}
              max={60}
              placeholder="3"
              value={form.yearsExperience}
              error={errors.yearsExperience}
              onChange={(e) => update('yearsExperience', e.target.value)}
            />
          </div>

          <TextArea
            label="Mô tả tay nghề & Giới thiệu thêm (không bắt buộc)"
            name="bio"
            rows={3}
            maxLength={500}
            placeholder="Ví dụ: Có 5 năm kinh nghiệm sửa bo mạch inverter máy lạnh, nhận sửa tivi sony, samsung tại nhà..."
            value={form.bio}
            error={errors.bio}
            hint={`${form.bio.length}/500 ký tự`}
            onChange={(e) => update('bio', e.target.value)}
          />
        </FormSection>

        <FormSection title="Mật khẩu bảo mật" description="Mật khẩu bảo vệ tài khoản kỹ thuật viên của bạn">
          <div className="grid gap-5 sm:grid-cols-2">
            <TextField
              label="Mật khẩu"
              name="password"
              type="password"
              autoComplete="new-password"
              required
              value={form.password}
              error={errors.password}
              hint="Ít nhất 8 ký tự, có 1 chữ in hoa và 1 ký tự đặc biệt."
              onChange={(e) => update('password', e.target.value)}
            />
            <TextField
              label="Xác nhận mật khẩu"
              name="confirmPassword"
              type="password"
              autoComplete="new-password"
              required
              value={form.confirmPassword}
              error={errors.confirmPassword}
              onChange={(e) => update('confirmPassword', e.target.value)}
            />
          </div>
        </FormSection>

        <Button type="submit" fullWidth loading={submitting}>
          {submitting ? 'Đang gửi hồ sơ đăng ký...' : 'Gửi hồ sơ đăng ký thợ'}
        </Button>
      </form>
    </AuthLayout>
  );
}
