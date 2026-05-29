"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
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

interface AuthState {
  user: MemberUser | null;
  wallet: AigcWallet | null;
  loading: boolean;
  loggedIn: boolean;
}

interface AuthContextValue extends AuthState {
  authMode: AuthMode;
  modalOpen: boolean;
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
  openModal: (mode?: AuthMode) => void;
  closeModal: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function applyLoginToken(token: LoginToken) {
  setTokens(token.accessToken, token.refreshToken);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({
    user: null,
    wallet: null,
    loading: true,
    loggedIn: false,
  });
  const [modalOpen, setModalOpen] = useState(false);
  const [authMode, setAuthMode] = useState<AuthMode>("sms");

  const fetchUser = useCallback(async () => {
    const [user, wallet] = await Promise.all([getProfile(), getAigcWallet().catch(() => null)]);
    setState((current) => ({
      ...current,
      user,
      wallet,
      loading: false,
      loggedIn: true,
    }));
    return user;
  }, []);

  const clearAuthState = useCallback(() => {
    clearTokens();
    setState({ user: null, wallet: null, loading: false, loggedIn: false });
  }, []);

  useEffect(() => {
    let ignore = false;

    async function init() {
      if (!getAccessToken() && !getRefreshToken()) {
        if (!ignore) {
          setState({ user: null, wallet: null, loading: false, loggedIn: false });
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
      clearAuthState();
      setAuthMode("sms");
      setModalOpen(true);
    });
  }, [clearAuthState]);

  const loginByPassword = useCallback(
    async (mobile: string, password: string) => {
      const token = await AuthApi.loginByPassword({ mobile, password });
      applyLoginToken(token);
      await fetchUser();
      setModalOpen(false);
    },
    [fetchUser]
  );

  const loginBySms = useCallback(
    async (mobile: string, code: string) => {
      const token = await AuthApi.loginBySms({ mobile, code });
      applyLoginToken(token);
      await fetchUser();
      setModalOpen(false);
    },
    [fetchUser]
  );

  const loginByEmail = useCallback(
    async (email: string, password: string) => {
      const token = await AuthApi.loginByEmail({ email, password });
      applyLoginToken(token);
      await fetchUser();
      setModalOpen(false);
    },
    [fetchUser]
  );

  const loginByEmailCode = useCallback(
    async (email: string, code: string) => {
      const token = await AuthApi.loginByEmailCode({ email, code });
      applyLoginToken(token);
      await fetchUser();
      setModalOpen(false);
    },
    [fetchUser]
  );

  const registerByEmail = useCallback(
    async (payload: EmailRegisterReq) => {
      const token = await AuthApi.registerByEmail(payload);
      applyLoginToken(token);
      await fetchUser();
      setModalOpen(false);
    },
    [fetchUser]
  );

  const logout = useCallback(async () => {
    try {
      if (getAccessToken()) {
        await AuthApi.logout();
      }
    } finally {
      clearAuthState();
    }
  }, [clearAuthState]);

  const resetPasswordByEmail = useCallback(async (email: string, code: string, password: string) => {
    await AuthApi.resetPasswordByEmail({ email, code, password });
  }, []);

  const refreshWallet = useCallback(async () => {
    if (!getAccessToken()) return null;
    const wallet = await getAigcWallet();
    setState((current) => (current.loggedIn ? { ...current, wallet } : current));
    return wallet;
  }, []);

  const openModal = useCallback((mode: AuthMode = "sms") => {
    setAuthMode(mode);
    setModalOpen(true);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        ...state,
        authMode,
        modalOpen,
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
