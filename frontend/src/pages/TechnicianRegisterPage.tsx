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

const EMPTY = {
  username: '',
  password: '',
  confirmPassword: '',
  fullName: '',
  phone: '',
  email: '',
  citizenId: '',
  idCardFrontUrl: '',
  idCardBackUrl: '',
  yearsExperience: '',
  bio: ''
};

function validate(form: typeof EMPTY): Record<string, string> {
  const errors: Record<string, string> = {};

  if (form.username.trim().length < 3 || form.username.trim().length > 50) {
    errors.username = 'Tên đăng nhập cần từ 3 đến 50 ký tự.';
  }
  if (!/^(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{8,}$/.test(form.password)) {
    errors.password = 'Mật khẩu cần ít nhất 8 ký tự, có 1 chữ hoa và 1 ký tự đặc biệt.';
  }
  if (form.confirmPassword !== form.password) {
    errors.confirmPassword = 'Hai lần nhập mật khẩu chưa giống nhau.';
  }
  if (!form.fullName.trim()) {
    errors.fullName = 'Hãy nhập họ và tên đúng như trên căn cước.';
  }
  if (!/^0[35789][0-9]{8}$/.test(form.phone.trim())) {
    errors.phone = 'Số điện thoại cần 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09.';
  }
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(form.email.trim())) {
    errors.email = 'Email chưa đúng định dạng.';
  }
  if (!/^[0-9]{9,12}$/.test(form.citizenId.trim())) {
    errors.citizenId = 'Số căn cước gồm 9 đến 12 chữ số.';
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
        username: form.username.trim(),
        password: form.password,
        fullName: form.fullName.trim(),
        phone: form.phone.trim(),
        email: form.email.trim(),
        citizenId: form.citizenId.trim(),
        idCardFrontUrl: form.idCardFrontUrl.trim() || null,
        idCardBackUrl: form.idCardBackUrl.trim() || null,
        bio: form.bio.trim() || null,
        yearsExperience: form.yearsExperience.trim() ? Number(form.yearsExperience) : null
      });
      navigate('/dang-nhap', {
        replace: true,
        state: { username: form.username.trim(), justRegistered: true }
      });
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

  return (
    <AuthLayout
      wide
      title="Đăng ký làm thợ"
      description="Khai đúng thông tin trên căn cước. Quản trị viên sẽ đối chiếu trước khi bạn nhận việc đầu tiên."
      footer={
        <p className="text-sm text-ink-soft">
          Đã có tài khoản?{' '}
          <Link to="/dang-nhap" className="text-brand hover:text-brand-strong hover:underline">
            Đăng nhập
          </Link>
          . Bạn là khách cần sửa đồ?{' '}
          <Link to="/dang-ky" className="text-brand hover:text-brand-strong hover:underline">
            Đăng ký khách hàng
          </Link>
          .
        </p>
      }
    >
      <div className="mt-6 flex items-start gap-3 rounded-xl border border-amber/30 bg-amber-soft px-4 py-3">
        <StatusChip status="PENDING" />
        <p className="text-sm text-ink-soft">
          Hồ sơ mới luôn ở trạng thái chờ duyệt. Bạn xem được bảng điều khiển ngay, nhưng chỉ
          nhận được yêu cầu sửa chữa sau khi quản trị viên xác minh danh tính.
        </p>
      </div>

      <form onSubmit={handleSubmit} noValidate className="mt-8 space-y-8">
        {formError && <Alert tone="error">{formError}</Alert>}

        <FormSection title="Tài khoản đăng nhập">
          <TextField
            label="Tên đăng nhập"
            name="username"
            autoComplete="username"
            required
            value={form.username}
            error={errors.username}
            hint="Từ 3 đến 50 ký tự."
            onChange={(e) => update('username', e.target.value)}
          />
          <div className="grid gap-5 sm:grid-cols-2">
            <TextField
              label="Mật khẩu"
              name="password"
              type="password"
              autoComplete="new-password"
              required
              value={form.password}
              error={errors.password}
              hint="Ít nhất 8 ký tự, có 1 chữ hoa và 1 ký tự đặc biệt."
              onChange={(e) => update('password', e.target.value)}
            />
            <TextField
              label="Nhập lại mật khẩu"
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

        <FormSection
          title="Thông tin liên hệ"
          description="Khách hàng thấy tên và gọi vào số này khi hẹn giờ sửa."
        >
          <TextField
            label="Họ và tên"
            name="fullName"
            autoComplete="name"
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
              autoComplete="tel"
              required
              placeholder="0912345678"
              value={form.phone}
              error={errors.phone}
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
        </FormSection>

        <FormSection
          title="Xác minh danh tính"
          description="Ảnh căn cước chỉ dùng để đối chiếu, không hiển thị cho khách hàng."
        >
          <TextField
            label="Số căn cước công dân"
            name="citizenId"
            inputMode="numeric"
            required
            placeholder="079123456789"
            value={form.citizenId}
            error={errors.citizenId}
            onChange={(e) => update('citizenId', e.target.value)}
          />
          <div className="grid gap-5 sm:grid-cols-2">
            <TextField
              label="Ảnh mặt trước căn cước"
              name="idCardFrontUrl"
              type="url"
              placeholder="https://..."
              value={form.idCardFrontUrl}
              error={errors.idCardFrontUrl}
              hint="Dán đường dẫn ảnh. Tính năng tải ảnh trực tiếp sẽ bổ sung sau."
              onChange={(e) => update('idCardFrontUrl', e.target.value)}
            />
            <TextField
              label="Ảnh mặt sau căn cước"
              name="idCardBackUrl"
              type="url"
              placeholder="https://..."
              value={form.idCardBackUrl}
              error={errors.idCardBackUrl}
              onChange={(e) => update('idCardBackUrl', e.target.value)}
            />
          </div>
        </FormSection>

        <FormSection
          title="Tay nghề"
          description="Phần này giúp khách chọn đúng thợ. Có thể bỏ trống và bổ sung sau."
        >
          <TextField
            label="Số năm kinh nghiệm"
            name="yearsExperience"
            type="number"
            inputMode="numeric"
            min={0}
            max={60}
            placeholder="5"
            value={form.yearsExperience}
            error={errors.yearsExperience}
            onChange={(e) => update('yearsExperience', e.target.value)}
          />
          <TextArea
            label="Giới thiệu ngắn"
            name="bio"
            rows={4}
            maxLength={500}
            placeholder="Ví dụ: Chuyên sửa điều hòa, tủ lạnh, máy giặt tại nhà khu vực Cầu Giấy."
            value={form.bio}
            error={errors.bio}
            hint={`${form.bio.length}/500 ký tự`}
            onChange={(e) => update('bio', e.target.value)}
          />
        </FormSection>

        <Button type="submit" fullWidth loading={submitting}>
          {submitting ? 'Đang gửi hồ sơ...' : 'Gửi hồ sơ đăng ký'}
        </Button>
      </form>
    </AuthLayout>
  );
}
