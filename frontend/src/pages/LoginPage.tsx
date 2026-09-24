import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import Alert from '../components/Alert';
import AuthLayout from '../components/AuthLayout';
import Button from '../components/Button';
import TextField from '../components/TextField';
import { ApiError } from '../lib/api';
import { HOME_BY_ROLE, useAuth } from '../lib/auth';

function formatCountdown(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}

interface LoginLocationState {
  username?: string;
  justRegistered?: boolean;
  passwordReset?: boolean;
  from?: string;
}

export default function LoginPage() {
  const navigate = useNavigate();
  const { signIn } = useAuth();
  const { username: prefilled, justRegistered, passwordReset, from } =
    (useLocation().state as LoginLocationState | null) ?? {};

  const [username, setUsername] = useState(prefilled ?? '');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lockedSeconds, setLockedSeconds] = useState(0);
  const timerRef = useRef<number | null>(null);

  useEffect(() => {
    if (lockedSeconds <= 0) return;
    timerRef.current = window.setInterval(() => {
      setLockedSeconds((left) => (left <= 1 ? 0 : left - 1));
    }, 1000);
    return () => {
      if (timerRef.current) window.clearInterval(timerRef.current);
    };
  }, [lockedSeconds > 0]);

  const locked = lockedSeconds > 0;

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (locked) return;

    setError(null);
    setSubmitting(true);
    try {
      const user = await signIn(username.trim(), password);
      const destination = from || (HOME_BY_ROLE[user.role] ?? '/');
      navigate(destination, { replace: true });
    } catch (err) {
      if (err instanceof ApiError && err.statusCode === 429) {
        const remaining = err.remainingSeconds;
        setLockedSeconds(typeof remaining === 'number' && remaining > 0 ? remaining : 900);
      } else if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout
      title="Đăng nhập"
      description="Dùng chung một tài khoản cho khách hàng, thợ và quản trị viên."
      footer={
        <>
          <p className="text-sm text-ink-soft">Chưa có tài khoản?</p>
          <div className="mt-3 flex flex-col gap-2 sm:flex-row">
            <Link
              to="/dang-ky"
              className="flex min-h-[44px] flex-1 items-center justify-center rounded-xl border border-slate-200 bg-white px-4 text-sm font-semibold text-slate-700 shadow-xs transition-all duration-200 hover:border-brand hover:text-brand hover:bg-blue-50/50"
            >
              👤 Đăng ký khách hàng
            </Link>
            <Link
              to="/dang-ky-tho"
              className="flex min-h-[44px] flex-1 items-center justify-center rounded-xl border border-slate-200 bg-white px-4 text-sm font-semibold text-slate-700 shadow-xs transition-all duration-200 hover:border-brand hover:text-brand hover:bg-blue-50/50"
            >
              🔧 Đăng ký làm thợ
            </Link>
          </div>
        </>
      }
    >
      <form onSubmit={handleSubmit} noValidate className="mt-8 space-y-5">
        {passwordReset && !error && !locked && (
          <Alert tone="success" title="Đã đổi mật khẩu">
            Đăng nhập lại bằng mật khẩu mới. Các thiết bị khác đã bị đăng xuất.
          </Alert>
        )}

        {justRegistered && !error && !locked && (
          <Alert tone="success" title="Đã tạo tài khoản">
            Đăng nhập bằng mật khẩu bạn vừa đặt để bắt đầu đăng yêu cầu sửa chữa.
          </Alert>
        )}

        {locked && (
          <Alert tone="warning" title="Tài khoản tạm thời bị khoá">
            Bạn đã nhập sai mật khẩu quá số lần cho phép. Thử lại sau{' '}
            <strong className="font-display text-ink">{formatCountdown(lockedSeconds)}</strong>,
            hoặc đặt lại mật khẩu nếu bạn không nhớ.
          </Alert>
        )}

        {error && !locked && <Alert tone="error">{error}</Alert>}

        <TextField
          label="Tên đăng nhập hoặc số điện thoại"
          name="username"
          autoComplete="username"
          required
          disabled={locked}
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="Ví dụ: tho_dien_lanh_01"
        />

        <TextField
          label="Mật khẩu"
          name="password"
          type="password"
          autoComplete="current-password"
          required
          disabled={locked}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="Nhập mật khẩu của bạn"
        />

        <div className="flex justify-end">
          <Link
            to="/quen-mat-khau"
            className="rounded-lg py-1 text-sm text-brand-glow transition-colors hover:text-brand hover:underline"
          >
            Quên mật khẩu?
          </Link>
        </div>

        <Button type="submit" fullWidth loading={submitting} disabled={locked}>
          {submitting ? 'Đang kiểm tra...' : 'Đăng nhập'}
        </Button>
      </form>
    </AuthLayout>
  );
}
