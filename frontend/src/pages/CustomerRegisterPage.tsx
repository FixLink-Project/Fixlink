import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Alert from '../components/Alert';
import AuthLayout from '../components/AuthLayout';
import Button from '../components/Button';
import TextField from '../components/TextField';
import { ApiError, api, toFieldErrors } from '../lib/api';

interface RegisterResult {
  userId: string;
  username: string;
  fullName: string;
}

const EMPTY = {
  username: '',
  password: '',
  confirmPassword: '',
  fullName: '',
  phone: '',
  email: ''
};

/** Kiểm tra tại chỗ để người dùng biết lỗi trước khi gửi; máy chủ vẫn kiểm lại. */
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
    errors.fullName = 'Hãy nhập họ và tên để thợ biết cách xưng hô.';
  }
  if (!/^0[35789][0-9]{8}$/.test(form.phone.trim())) {
    errors.phone = 'Số điện thoại cần 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09.';
  }
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(form.email.trim())) {
    errors.email = 'Email chưa đúng định dạng.';
  }

  return errors;
}

export default function CustomerRegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function update(field: keyof typeof EMPTY, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
    // Xoá lỗi của riêng trường đang sửa để người dùng thấy phản hồi ngay.
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
      await api.post<RegisterResult>('/auth/register/customer', {
        username: form.username.trim(),
        password: form.password,
        fullName: form.fullName.trim(),
        phone: form.phone.trim(),
        email: form.email.trim()
      });
      // Chuyển sang trang đăng nhập kèm tên tài khoản vừa tạo.
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
      title="Đăng ký khách hàng"
      description="Tạo tài khoản để đăng yêu cầu sửa chữa và theo dõi tiến độ."
      footer={
        <p className="text-sm text-ink-soft">
          Đã có tài khoản?{' '}
          <Link to="/dang-nhap" className="text-brand hover:text-brand-strong hover:underline">
            Đăng nhập
          </Link>
          . Bạn là thợ muốn nhận việc?{' '}
          <Link to="/dang-ky-tho" className="text-brand hover:text-brand-strong hover:underline">
            Đăng ký làm thợ
          </Link>
          .
        </p>
      }
    >
      <form onSubmit={handleSubmit} noValidate className="mt-8 space-y-5">
        {formError && <Alert tone="error">{formError}</Alert>}

        <TextField
          label="Tên đăng nhập"
          name="username"
          autoComplete="username"
          required
          value={form.username}
          error={errors.username}
          hint="Từ 3 đến 50 ký tự, dùng để đăng nhập."
          onChange={(e) => update('username', e.target.value)}
        />

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

        <Button type="submit" fullWidth loading={submitting}>
          {submitting ? 'Đang tạo tài khoản...' : 'Tạo tài khoản'}
        </Button>
      </form>
    </AuthLayout>
  );
}
