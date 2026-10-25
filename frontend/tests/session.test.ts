import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { AuthUser } from '../src/lib/types';

const customer: AuthUser = {
  id: 'u-1',
  username: 'khach',
  role: 'CUSTOMER',
  status: 'ACTIVE',
  fullName: 'Khách',
  avatarUrl: null,
  isVerified: true,
  verificationStatus: null
} as AuthUser;

const technician: AuthUser = { ...customer, id: 'u-2', username: 'tho', role: 'TECHNICIAN' } as AuthUser;

/**
 * Mô phỏng một tab trình duyệt: sessionStorage riêng, localStorage dùng chung,
 * và bản module mới (giống mỗi tab tự nạp lại JS).
 */
function createStorage(): Storage {
  const data = new Map<string, string>();
  return {
    get length() {
      return data.size;
    },
    clear: () => data.clear(),
    getItem: (k) => (data.has(k) ? data.get(k)! : null),
    key: (i) => Array.from(data.keys())[i] ?? null,
    removeItem: (k) => void data.delete(k),
    setItem: (k, v) => void data.set(k, String(v))
  };
}

const sharedLocal = createStorage();

async function openTab() {
  const tabSession = createStorage();
  vi.resetModules();
  vi.stubGlobal('localStorage', sharedLocal);
  vi.stubGlobal('sessionStorage', tabSession);
  const mod = await import('../src/lib/session');
  // Mỗi lần thao tác trên tab phải dùng đúng storage của tab đó.
  const inTab =
    <A extends unknown[], R>(fn: (...args: A) => R) =>
    (...args: A): R => {
      vi.stubGlobal('localStorage', sharedLocal);
      vi.stubGlobal('sessionStorage', tabSession);
      return fn(...args);
    };
  return {
    getSession: inTab(mod.getSession),
    saveSession: inTab(mod.saveSession),
    clearSession: inTab(mod.clearSession),
    installNewTabHandoff: inTab(mod.installNewTabHandoff),
    /** Chạy đoạn mã (vd bấm chuột) trong ngữ cảnh storage của tab này. */
    run: inTab(<R,>(fn: () => R) => fn()),
    storage: tabSession
  };
}

describe('phiên đăng nhập theo từng tab', () => {
  beforeEach(() => sharedLocal.clear());
  afterEach(() => vi.unstubAllGlobals());

  it('đăng nhập thợ ở tab khác không ghi đè phiên khách hàng của tab hiện tại', async () => {
    const tabUser = await openTab();
    tabUser.saveSession({ accessToken: 'token-khach', refreshToken: 'r1', user: customer });

    const tabTho = await openTab();
    // Tab mới nhận phiên gần nhất làm điểm khởi đầu…
    expect(tabTho.getSession().user?.role).toBe('CUSTOMER');
    // …rồi đăng nhập thợ.
    tabTho.saveSession({ accessToken: 'token-tho', refreshToken: 'r2', user: technician });

    expect(tabUser.getSession()).toMatchObject({ accessToken: 'token-khach', user: { role: 'CUSTOMER' } });
    expect(tabTho.getSession()).toMatchObject({ accessToken: 'token-tho', user: { role: 'TECHNICIAN' } });
  });

  it('tab mở sau cùng nhận phiên đăng nhập gần nhất', async () => {
    const tabA = await openTab();
    tabA.saveSession({ accessToken: 'token-khach', refreshToken: 'r1', user: customer });
    const tabB = await openTab();
    tabB.saveSession({ accessToken: 'token-tho', refreshToken: 'r2', user: technician });

    const tabC = await openTab();
    expect(tabC.getSession().accessToken).toBe('token-tho');
  });

  it('đăng xuất ở một tab không làm mất phiên của tab khác', async () => {
    const tabUser = await openTab();
    tabUser.saveSession({ accessToken: 'token-khach', refreshToken: 'r1', user: customer });
    const tabTho = await openTab();
    tabTho.saveSession({ accessToken: 'token-tho', refreshToken: 'r2', user: technician });

    tabUser.clearSession();

    expect(tabUser.getSession().user).toBeNull();
    expect(tabTho.getSession().accessToken).toBe('token-tho');
    // Bản chia sẻ vẫn là phiên thợ, tab mới mở vẫn đăng nhập được.
    expect((await openTab()).getSession().accessToken).toBe('token-tho');
  });

  it('đăng xuất ở tab giữ phiên gần nhất thì tab mới phải đăng nhập lại', async () => {
    const tab = await openTab();
    tab.saveSession({ accessToken: 'token-khach', refreshToken: 'r1', user: customer });
    tab.clearSession();

    expect(tab.getSession().accessToken).toBeNull();
    expect((await openTab()).getSession().accessToken).toBeNull();
  });

  it('chuyển phiên lưu theo định dạng cũ sang định dạng mới', async () => {
    sharedLocal.setItem('fixlink.accessToken', 'token-cu');
    sharedLocal.setItem('fixlink.refreshToken', 'r-cu');
    sharedLocal.setItem('fixlink.user', JSON.stringify(customer));

    const tab = await openTab();
    expect(tab.getSession()).toMatchObject({ accessToken: 'token-cu', refreshToken: 'r-cu', user: { id: 'u-1' } });
    expect(sharedLocal.getItem('fixlink.accessToken')).toBeNull();
  });
});

describe('api gửi đúng token của tab', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    sharedLocal.clear();
  });

  it('request ở tab khách hàng vẫn dùng token khách sau khi tab khác đăng nhập thợ', async () => {
    const tabUser = await openTab();
    tabUser.saveSession({ accessToken: 'token-khach', refreshToken: 'r1', user: customer });
    const userSession = sessionStorage;

    const tabTho = await openTab();
    tabTho.saveSession({ accessToken: 'token-tho', refreshToken: 'r2', user: technician });

    vi.stubGlobal('sessionStorage', userSession);
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ success: true, data: null }), { status: 200 })
    );
    vi.stubGlobal('fetch', fetchMock);
    const { api } = await import('../src/lib/api');
    await api.get('/me');

    const headers = fetchMock.mock.calls[0][1].headers as Headers;
    expect(headers.get('Authorization')).toBe('Bearer token-khach');
  });
});

describe('tab mở sẵn và tab con', () => {
  beforeEach(() => sharedLocal.clear());
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.useRealTimers();
    document.body.innerHTML = '';
  });

  it('tab đang mở ở trạng thái chưa đăng nhập không tự nhận phiên do tab khác đăng nhập sau', async () => {
    const guestTab = await openTab();
    expect(guestTab.getSession().user).toBeNull();

    const tabUser = await openTab();
    tabUser.saveSession({ accessToken: 'token-khach', refreshToken: 'r1', user: customer });

    expect(guestTab.getSession().accessToken).toBeNull();
  });

  function clickLink(href: string, init: MouseEventInit, type = 'click') {
    const link = document.createElement('a');
    link.href = href;
    link.textContent = 'link';
    document.body.appendChild(link);
    link.addEventListener('click', (e) => e.preventDefault());
    link.dispatchEvent(new MouseEvent(type, { bubbles: true, cancelable: true, ...init }));
  }

  it.each([
    ['Ctrl+click', { ctrlKey: true }, 'click'],
    ['Cmd+click (macOS)', { metaKey: true }, 'click'],
    ['bấm chuột giữa', { button: 1 }, 'auxclick'],
    ['chuột phải → mở tab mới', { button: 2 }, 'contextmenu']
  ])('tab con mở bằng %s nhận phiên của tab nguồn, không phải phiên gần nhất', async (_name, init, type) => {
    const tabUser = await openTab();
    tabUser.saveSession({ accessToken: 'token-khach', refreshToken: 'r1', user: customer });
    const tabTho = await openTab();
    tabTho.saveSession({ accessToken: 'token-tho', refreshToken: 'r2', user: technician });

    const uninstall = tabUser.installNewTabHandoff();
    tabUser.run(() => clickLink('/yeu-cau-cua-toi', init, type));
    uninstall();

    const child = await openTab();
    expect(child.getSession()).toMatchObject({ accessToken: 'token-khach', user: { role: 'CUSTOMER' } });
    // Phiên gửi sang chỉ dùng một lần: tab mở sau đó lại nhận phiên gần nhất.
    expect((await openTab()).getSession().accessToken).toBe('token-tho');
  });

  it('bấm liên kết bình thường (cùng tab) không gửi phiên cho tab khác', async () => {
    const tabUser = await openTab();
    tabUser.saveSession({ accessToken: 'token-khach', refreshToken: 'r1', user: customer });
    const tabTho = await openTab();
    tabTho.saveSession({ accessToken: 'token-tho', refreshToken: 'r2', user: technician });

    const uninstall = tabUser.installNewTabHandoff();
    tabUser.run(() => clickLink('/yeu-cau-cua-toi', {}));
    uninstall();

    expect((await openTab()).getSession().accessToken).toBe('token-tho');
  });

  it('phiên gửi sang quá hạn thì tab mới nhận phiên gần nhất', async () => {
    vi.useFakeTimers({ toFake: ['Date'] });
    const tabUser = await openTab();
    tabUser.saveSession({ accessToken: 'token-khach', refreshToken: 'r1', user: customer });
    const tabTho = await openTab();
    tabTho.saveSession({ accessToken: 'token-tho', refreshToken: 'r2', user: technician });

    const uninstall = tabUser.installNewTabHandoff();
    tabUser.run(() => clickLink('/yeu-cau-cua-toi', { ctrlKey: true }));
    uninstall();
    vi.setSystemTime(Date.now() + 11_000);

    expect((await openTab()).getSession().accessToken).toBe('token-tho');
  });

  it('liên kết sang trang web khác không mang theo phiên', async () => {
    const tabUser = await openTab();
    tabUser.saveSession({ accessToken: 'token-khach', refreshToken: 'r1', user: customer });

    const uninstall = tabUser.installNewTabHandoff();
    tabUser.run(() => clickLink('https://example.com/', { ctrlKey: true }));
    uninstall();

    expect(sharedLocal.getItem('fixlink.session.handoff')).toBeNull();
  });
});

describe('phiên hết hạn (401)', () => {
  beforeEach(() => sharedLocal.clear());
  afterEach(() => vi.unstubAllGlobals());

  function unauthorized() {
    return new Response(
      JSON.stringify({ statusCode: 401, errorCode: 'UNAUTHORIZED', message: 'Hết phiên' }),
      { status: 401 }
    );
  }

  it('xoá phiên của tab và báo cho giao diện', async () => {
    const tab = await openTab();
    tab.saveSession({ accessToken: 'token-khach', refreshToken: 'r1', user: customer });
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(unauthorized()));
    const onCleared = vi.fn();
    window.addEventListener('fixlink:session-cleared', onCleared);

    const { api } = await import('../src/lib/api');
    await expect(tab.run(() => api.get('/khach-hang/yeu-cau'))).rejects.toThrow();
    window.removeEventListener('fixlink:session-cleared', onCleared);

    expect(onCleared).toHaveBeenCalledTimes(1);
    expect(tab.getSession().accessToken).toBeNull();
  });

  it('401 của request cũ trả về muộn không xoá phiên mới đăng nhập', async () => {
    const tab = await openTab();
    tab.saveSession({ accessToken: 'token-cu', refreshToken: 'r1', user: customer });
    let respond!: (r: Response) => void;
    vi.stubGlobal('fetch', vi.fn().mockReturnValue(new Promise<Response>((r) => (respond = r))));

    const { api } = await import('../src/lib/api');
    const pending = tab.run(() => api.get('/khach-hang/yeu-cau'));
    // Trong lúc chờ, người dùng đăng nhập tài khoản thợ ngay trong tab này.
    tab.saveSession({ accessToken: 'token-tho', refreshToken: 'r2', user: technician });
    respond(unauthorized());
    await expect(tab.run(() => pending)).rejects.toThrow();

    expect(tab.getSession().accessToken).toBe('token-tho');
  });
});
