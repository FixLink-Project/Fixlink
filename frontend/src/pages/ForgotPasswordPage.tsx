import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import Alert from '../components/Alert';
import AuthLayout from '../components/AuthLayout';
import Button from '../components/Button';
import TextField from '../components/TextField';
import { ApiError, api } from '../lib/api';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [sentMessage, setSentMessage] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email.trim())) {
      setError('Email chưa đúng định dạng.');
      return;
    }

    setSubmitting(true);
    try {
      const res = await api.post<null>('/auth/forgot-password', { email: email.trim() });
      // Máy chủ luôn trả cùng một thông báo, dù email có tồn tại hay không.
      setSentMessage(res.message);
    } catch (err) {
      setError(
        err instanceof ApiError
          ? err.message
          : 'Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.'
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout
      title="Quên mật khẩu"
      description="Nhập email bạn đã dùng khi đăng ký. Chúng tôi gửi liên kết để bạn đặt mật khẩu mới."
      footer={
        <p className="text-sm text-ink-soft">
          Nhớ ra mật khẩu rồi?{' '}
          <Link to="/dang-nhap" className="text-brand hover:text-brand-strong hover:underline">
            Quay lại đăng nhập
          </Link>
          .
        </p>
      }
    >
      {sentMessage ? (
        <div className="mt-8 space-y-5">
          <Alert tone="success" title="Đã gửi yêu cầu">
            {sentMessage}
          </Alert>
          <p className="text-sm text-ink-soft">
            Không thấy thư? Kiểm tra thư mục spam, hoặc thử lại sau một lát. Vì lý do an toàn,
            chúng tôi không cho biết email này đã đăng ký hay chưa.
          </p>
          <Button variant="secondary" onClick={() => setSentMessage(null)}>
            Gửi lại cho email khác
          </Button>
        </div>
      ) : (
        <form onSubmit={handleSubmit} noValidate className="mt-8 space-y-5">
          {error && <Alert tone="error">{error}</Alert>}

          <TextField
            label="Email đã đăng ký"
            name="email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="ban@gmail.com"
          />

          <Button type="submit" fullWidth loading={submitting}>
            {submitting ? 'Đang gửi...' : 'Gửi liên kết đặt lại'}
          </Button>
        </form>
      )}
    </AuthLayout>
  );
}
