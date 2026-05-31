"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { usePathname, useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  onAuthExpired,
  refreshAccessToken,
  setTokens,
} from "@/lib/api-client";
import * as AuthApi from "./auth-api";
import type {
  AuthMode,
  EmailRegisterReq,
  LoginToken,
  MemberUser,
} from "./auth-types";
import { getProfile } from "@/features/profile/profile-api";
import { getAigcWallet } from "@/features/wallet/wallet-api";
import type { AigcWallet } from "@/features/wallet/wallet-types";

type AuthReason = "required" | "expired" | "manual-logout" | null;

interface AuthState {
  user: MemberUser | null;
  wallet: AigcWallet | null;
  loading: boolean;
  loggedIn: boolean;
  authReason: AuthReason;
}

interface AuthContextValue extends AuthState {
  authMode: AuthMode;
  modalOpen: boolean;
  redirectTo: string | null;
  loginByPassword: (mobile: string, password: string) => Promise<void>;
  loginBySms: (mobile: string, code: string) => Promise<void>;
  loginByEmail: (email: string, password: string) => Promise<void>;
  loginByEmailCode: (email: string, code: string) => Promise<void>;
  registerByEmail: (payload: EmailRegisterReq) => Promise<void>;
  sendSmsCode: (mobile: string) => Promise<void>;
  sendEmailCode: (email: string, scene?: string) => Promise<void>;
  validateEmailCode: (email: string, code: string, scene?: string) => Promise<void>;
  resetPasswordByEmail: (email: string, code: string, password: string) => Promise<void>;
  fetchUser: () => Promise<MemberUser | null>;
  refreshWallet: () => Promise<AigcWallet | null>;
  logout: () => Promise<void>;
  openModal: (mode?: AuthMode, redirectTo?: string | null, reason?: AuthReason) => void;
  closeModal: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function applyLoginToken(token: LoginToken) {
  setTokens(token.accessToken, token.refreshToken);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const queryClient = useQueryClient();
  const [state, setState] = useState<AuthState>({
    user: null,
    wallet: null,
    loading: true,
    loggedIn: false,
    authReason: null,
  });
  const [modalOpen, setModalOpen] = useState(false);
  const [authMode, setAuthMode] = useState<AuthMode>("email");
  const [redirectTo, setRedirectTo] = useState<string | null>(null);

  const fetchUser = useCallback(async () => {
    const [user, wallet] = await Promise.all([getProfile(), getAigcWallet().catch(() => null)]);
    setState((current) => ({
      ...current,
      user,
      wallet,
      loading: false,
      loggedIn: true,
      authReason: null,
    }));
    return user;
  }, []);

  const clearAuthState = useCallback((reason: AuthReason = null) => {
    clearTokens();
    queryClient.clear();
    setState({ user: null, wallet: null, loading: false, loggedIn: false, authReason: reason });
  }, [queryClient]);

  const completeLogin = useCallback(async () => {
    await fetchUser();
    setModalOpen(false);
    const target = redirectTo ?? "/app";
    setRedirectTo(null);
    router.push(target);
  }, [fetchUser, redirectTo, router]);

  useEffect(() => {
    let ignore = false;

    async function init() {
      if (!getAccessToken() && !getRefreshToken()) {
        if (!ignore) {
          setState({ user: null, wallet: null, loading: false, loggedIn: false, authReason: null });
        }
        return;
      }

      try {
        if (!getAccessToken()) {
          await refreshAccessToken();
        }
        const [user, wallet] = await Promise.all([getProfile(), getAigcWallet().catch(() => null)]);
        if (!ignore) {
          setState({
            user,
            wallet,
            loading: false,
            loggedIn: true,
            authReason: null,
          });
        }
      } catch {
        if (!ignore) {
          clearAuthState();
        }
      }
    }

    init();
    return () => {
      ignore = true;
    };
  }, [clearAuthState]);

  useEffect(() => {
    return onAuthExpired(() => {
      clearAuthState("expired");
      setAuthMode("email");
      setRedirectTo(pathname);
      setModalOpen(true);
    });
  }, [clearAuthState, pathname]);

  const loginByPassword = useCallback(
    async (mobile: string, password: string) => {
      const token = await AuthApi.loginByPassword({ mobile, password });
      applyLoginToken(token);
      await completeLogin();
    },
    [completeLogin]
  );

  const loginBySms = useCallback(
    async (mobile: string, code: string) => {
      const token = await AuthApi.loginBySms({ mobile, code });
      applyLoginToken(token);
      await completeLogin();
    },
    [completeLogin]
  );

  const loginByEmail = useCallback(
    async (email: string, password: string) => {
      const token = await AuthApi.loginByEmail({ email, password });
      applyLoginToken(token);
      await completeLogin();
    },
    [completeLogin]
  );

  const loginByEmailCode = useCallback(
    async (email: string, code: string) => {
      const token = await AuthApi.loginByEmailCode({ email, code });
      applyLoginToken(token);
      await completeLogin();
    },
    [completeLogin]
  );

  const registerByEmail = useCallback(
    async (payload: EmailRegisterReq) => {
      const token = await AuthApi.registerByEmail(payload);
      applyLoginToken(token);
      await completeLogin();
    },
    [completeLogin]
  );

  const logout = useCallback(async () => {
    try {
      if (getAccessToken()) {
        await AuthApi.logout();
      }
    } finally {
      setModalOpen(false);
      setRedirectTo(null);
      clearAuthState("manual-logout");
      router.push("/");
    }
  }, [clearAuthState, router]);

  const resetPasswordByEmail = useCallback(async (email: string, code: string, password: string) => {
    await AuthApi.resetPasswordByEmail({ email, code, password });
  }, []);

  const refreshWallet = useCallback(async () => {
    if (!getAccessToken()) return null;
    const wallet = await getAigcWallet();
    setState((current) => (current.loggedIn ? { ...current, wallet } : current));
    return wallet;
  }, []);

  const openModal = useCallback((mode: AuthMode = "email", nextRedirectTo: string | null = null, reason: AuthReason = "required") => {
    setAuthMode(mode);
    setRedirectTo(nextRedirectTo);
    setState((current) => ({ ...current, authReason: reason }));
    setModalOpen(true);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        ...state,
        authMode,
        modalOpen,
        redirectTo,
        loginByPassword,
        loginBySms,
        loginByEmail,
        loginByEmailCode,
        registerByEmail,
        sendSmsCode: (mobile) => AuthApi.sendSmsCode({ mobile, scene: "LOGIN" }).then(() => undefined),
        sendEmailCode: (email, scene = "REGISTER") => AuthApi.sendEmailCode({ email, scene }).then(() => undefined),
        validateEmailCode: (email, code, scene = "REGISTER") =>
          AuthApi.validateEmailCode({ email, code, scene }).then(() => undefined),
        resetPasswordByEmail,
        fetchUser,
        refreshWallet,
        logout,
        openModal,
        closeModal: () => setModalOpen(false),
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
