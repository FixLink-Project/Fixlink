import type { AuthUser } from './types';

/**
 * Phiên đăng nhập được giữ RIÊNG cho từng tab (sessionStorage), để đăng nhập
 * khách hàng ở tab này và thợ ở tab khác không ghi đè lên nhau.
 *
 * Tab chỉ chọn phiên khởi đầu MỘT lần, lúc vừa mở:
 *  1. Tab được mở từ một liên kết trong tab khác (Ctrl/giữa chuột/"Mở trong tab
 *     mới") nhận đúng phiên của tab đã mở nó.
 *  2. Ngược lại nhận "phiên gần nhất" trong localStorage (mở tab mới, mở lại
 *     trình duyệt) để không phải đăng nhập lại.
 * Sau đó tab sống độc lập: đăng nhập/đăng xuất ở tab khác không ảnh hưởng.
 */
export interface StoredSession {
  accessToken: string | null;
  refreshToken: string | null;
  user: AuthUser | null;
}

const SESSION_KEY = 'fixlink.session';
/** Phiên do tab nguồn gửi sang tab con sắp mở, chỉ dùng một lần trong thời gian ngắn. */
const HANDOFF_KEY = 'fixlink.session.handoff';
const HANDOFF_TTL_MS = 10_000;
/** Phát ra khi phiên của tab bị xoá (đăng xuất, hết hạn) để giao diện cập nhật. */
export const SESSION_CLEARED_EVENT = 'fixlink:session-cleared';

/** Các key cũ dùng chung mọi tab — chuyển sang định dạng mới rồi xoá. */
const LEGACY_TOKEN_KEY = 'fixlink.accessToken';
const LEGACY_REFRESH_KEY = 'fixlink.refreshToken';
const LEGACY_USER_KEY = 'fixlink.user';

const EMPTY: StoredSession = { accessToken: null, refreshToken: null, user: null };

/** Bản trong bộ nhớ: dự phòng khi trình duyệt chặn Web Storage. */
let memory: StoredSession | null = null;

function parse(raw: string | null): StoredSession | null {
  if (!raw) return null;
  try {
    const value = JSON.parse(raw) as Partial<StoredSession> | null;
    if (!value || typeof value !== 'object') return null;
    return {
      accessToken: value.accessToken ?? null,
      refreshToken: value.refreshToken ?? null,
      user: value.user ?? null
    };
  } catch {
    return null;
  }
}

function isEmpty(session: StoredSession): boolean {
  return !session.accessToken && !session.refreshToken && !session.user;
}

function readLegacy(): StoredSession | null {
  try {
    const accessToken = localStorage.getItem(LEGACY_TOKEN_KEY);
    const refreshToken = localStorage.getItem(LEGACY_REFRESH_KEY);
    const rawUser = localStorage.getItem(LEGACY_USER_KEY);
    localStorage.removeItem(LEGACY_TOKEN_KEY);
    localStorage.removeItem(LEGACY_REFRESH_KEY);
    localStorage.removeItem(LEGACY_USER_KEY);
    let user: AuthUser | null = null;
    try {
      user = rawUser ? (JSON.parse(rawUser) as AuthUser) : null;
    } catch {
      user = null;
    }
    const session = { accessToken, refreshToken, user };
    return isEmpty(session) ? null : session;
  } catch {
    return null;
  }
}

/** Lấy (và xoá) phiên mà tab nguồn vừa gửi sang, nếu còn hạn. */
function takeHandoff(): StoredSession | null {
  try {
    const raw = localStorage.getItem(HANDOFF_KEY);
    if (!raw) return null;
    localStorage.removeItem(HANDOFF_KEY);
    const { at, session } = JSON.parse(raw) as { at: number; session: unknown };
    if (typeof at !== 'number' || Date.now() - at > HANDOFF_TTL_MS) return null;
    return parse(JSON.stringify(session));
  } catch {
    return null;
  }
}

function readShared(): StoredSession | null {
  try {
    return parse(localStorage.getItem(SESSION_KEY)) ?? readLegacy();
  } catch {
    return null;
  }
}

function readOwn(): StoredSession | null {
  try {
    return parse(sessionStorage.getItem(SESSION_KEY));
  } catch {
    return null;
  }
}

function writeOwn(session: StoredSession): void {
  memory = session;
  try {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  } catch {
    // Bị chặn storage: vẫn còn bản trong bộ nhớ.
  }
}

export function getSession(): StoredSession {
  const own = readOwn();
  if (own) {
    memory = own;
    return own;
  }
  if (memory) return memory;
  // Lần đầu trong tab: chọn phiên khởi đầu rồi ghi lại ngay, để tab không bao
  // giờ tự nhận phiên của tab khác ở những lần đọc sau.
  const initial = takeHandoff() ?? readShared() ?? EMPTY;
  writeOwn(initial);
  return initial;
}

export function saveSession(patch: Partial<StoredSession>): StoredSession {
  const next = { ...getSession(), ...patch };
  if (isEmpty(next)) {
    clearSession();
    return EMPTY;
  }
  writeOwn(next);
  try {
    localStorage.setItem(SESSION_KEY, JSON.stringify(next));
  } catch {
    // Không lưu được bản chia sẻ thì tab mới phải đăng nhập lại.
  }
  return next;
}

/** Đăng xuất tab hiện tại. Không đụng tới phiên của các tab khác. */
export function clearSession(): void {
  const current = getSession();
  // Lưu phiên rỗng (thay vì xoá) để tab này không tự nhận lại phiên chia sẻ.
  writeOwn(EMPTY);
  try {
    // Chỉ xoá bản chia sẻ nếu nó đúng là phiên của tab này; nếu tab khác đã
    // đăng nhập sau đó thì giữ nguyên phiên của tab kia.
    const shared = parse(localStorage.getItem(SESSION_KEY));
    if (shared && shared.accessToken === current.accessToken) {
      localStorage.removeItem(SESSION_KEY);
    }
  } catch {
    // private browsing
  }
  if (typeof window !== 'undefined') window.dispatchEvent(new Event(SESSION_CLEARED_EVENT));
}

/** Gửi phiên của tab này cho tab con sắp được mở từ một liên kết. */
function handOffTo(target: EventTarget | null): void {
  const link = target instanceof Element ? target.closest('a[href]') : null;
  if (!(link instanceof HTMLAnchorElement) || link.origin !== window.location.origin) return;
  try {
    localStorage.setItem(HANDOFF_KEY, JSON.stringify({ at: Date.now(), session: getSession() }));
  } catch {
    // Không gửi được thì tab con nhận phiên gần nhất như bình thường.
  }
}

/**
 * Theo dõi các cách mở liên kết sang tab mới để tab con nhận đúng phiên của tab
 * này. Trả về hàm gỡ bỏ.
 */
export function installNewTabHandoff(): () => void {
  const onClick = (e: MouseEvent) => {
    const opensNewTab =
      e.ctrlKey || e.metaKey || e.shiftKey ||
      (e.target instanceof Element && e.target.closest('a[target="_blank"]') !== null);
    if (opensNewTab) handOffTo(e.target);
  };
  const onAuxClick = (e: MouseEvent) => {
    if (e.button === 1) handOffTo(e.target);
  };
  // Menu chuột phải → "Mở liên kết trong tab mới".
  const onContextMenu = (e: MouseEvent) => handOffTo(e.target);

  document.addEventListener('click', onClick, true);
  document.addEventListener('auxclick', onAuxClick, true);
  document.addEventListener('contextmenu', onContextMenu, true);
  return () => {
    document.removeEventListener('click', onClick, true);
    document.removeEventListener('auxclick', onAuxClick, true);
    document.removeEventListener('contextmenu', onContextMenu, true);
  };
}
