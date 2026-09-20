import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import Alert from '../components/Alert';
import AuthLayout from '../components/AuthLayout';
import Button from '../components/Button';
import TextField from '../components/TextField';
import { ApiError, api, toFieldErrors } from '../lib/api';

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') ?? '';

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setFormError(null);

    const clientErrors: Record<string, string> = {};
    // Chính sách mật khẩu của máy chủ: chữ hoa, chữ thường, số và ký tự đặc biệt.
    if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/.test(newPassword)) {
      clientErrors.newPassword =
        'Mật khẩu cần ít nhất 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt.';
    }
    if (confirmPassword !== newPassword) {
      clientErrors.confirmPassword = 'Hai lần nhập mật khẩu chưa giống nhau.';
    }
    if (Object.keys(clientErrors).length > 0) {
      setErrors(clientErrors);
      return;
    }

    setSubmitting(true);
    try {
      await api.post('/auth/reset-password', { token, newPassword, confirmPassword });
      navigate('/dang-nhap', { replace: true, state: { passwordReset: true } });
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

  // Mở trang mà không có token thì không thể làm gì; nói thẳng thay vì hiện form vô dụng.
  if (!token) {
    return (
      <AuthLayout title="Đặt lại mật khẩu">
        <div className="mt-8 space-y-5">
          <Alert tone="error" title="Liên kết không hợp lệ">
            Đường dẫn thiếu mã xác nhận. Hãy mở đúng liên kết trong email chúng tôi gửi, hoặc
            yêu cầu gửi lại liên kết mới.
          </Alert>
          <Link
            to="/quen-mat-khau"
            className="flex min-h-[44px] w-full items-center justify-center rounded-lg bg-brand px-5 text-sm font-medium text-white hover:bg-brand-strong"
          >
            Yêu cầu liên kết mới
          </Link>
        </div>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout
      title="Đặt mật khẩu mới"
      description="Sau khi đổi, mọi thiết bị đang đăng nhập bằng tài khoản này sẽ bị đăng xuất."
      footer={
        <p className="text-sm text-ink-soft">
          Liên kết đã hết hạn?{' '}
          <Link to="/quen-mat-khau" className="text-brand hover:text-brand-strong hover:underline">
            Yêu cầu gửi lại
          </Link>
          .
        </p>
      }
    >
      <form onSubmit={handleSubmit} noValidate className="mt-8 space-y-5">
        {formError && <Alert tone="error">{formError}</Alert>}

        <TextField
          label="Mật khẩu mới"
          name="newPassword"
          type="password"
          autoComplete="new-password"
          required
          value={newPassword}
          error={errors.newPassword}
          hint="Ít nhất 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt."
          onChange={(e) => {
            setNewPassword(e.target.value);
            setErrors(({ newPassword: _unused, ...rest }) => rest);
          }}
        />

        <TextField
          label="Nhập lại mật khẩu mới"
          name="confirmPassword"
          type="password"
          autoComplete="new-password"
          required
          value={confirmPassword}
          error={errors.confirmPassword}
          onChange={(e) => {
            setConfirmPassword(e.target.value);
            setErrors(({ confirmPassword: _unused, ...rest }) => rest);
          }}
        />

        <Button type="submit" fullWidth loading={submitting}>
          {submitting ? 'Đang đặt lại...' : 'Đặt mật khẩu mới'}
        </Button>
      </form>
    </AuthLayout>
  );
}
