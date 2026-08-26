const DEFAULT_POST_LOGIN_REDIRECT = "/plans";
const POST_LOGIN_REDIRECT_KEY = "postLoginRedirect";

type RedirectStorage = Pick<Storage, "getItem" | "removeItem" | "setItem">;

export function savePostLoginRedirect(
  redirect: string,
  getStorage: () => RedirectStorage = () => window.sessionStorage,
): boolean {
  try {
    getStorage().setItem(POST_LOGIN_REDIRECT_KEY, redirect);
    return true;
  } catch {
    return false;
  }
}

export function consumePostLoginRedirect(
  getStorage: () => RedirectStorage = () => window.sessionStorage,
  currentOrigin: string = window.location.origin,
): string {
  try {
    const storage = getStorage();
    const redirect = storage.getItem(POST_LOGIN_REDIRECT_KEY);
    storage.removeItem(POST_LOGIN_REDIRECT_KEY);

    if (!redirect) {
      return DEFAULT_POST_LOGIN_REDIRECT;
    }

    const resolved = new URL(redirect, currentOrigin);
    if (resolved.origin !== currentOrigin) {
      return DEFAULT_POST_LOGIN_REDIRECT;
    }

    return `${resolved.pathname}${resolved.search}${resolved.hash}`;
  } catch {
    return DEFAULT_POST_LOGIN_REDIRECT;
  }
}
