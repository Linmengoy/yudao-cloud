"use client";

import { useEffect, useState } from "react";
import { Eye, EyeOff } from "lucide-react";
import { useAuth } from "./auth-store";
import type { AuthMode } from "./auth-types";
import { VerificationCodeField } from "./VerificationCodeField";

type AuthView = "login" | "email-code" | "phone-code" | "register" | "reset";

function getInitialView(mode: AuthMode): AuthView {
  if (mode === "sms" || mode === "password") return "phone-code";
  if (mode === "register") return "register";
  if (mode === "forgot") return "reset";
  return "login";
}

function isMobile(value: string) {
  return /^1\d{10}$/.test(value);
}

function isEmail(value: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

function isPassword(value: string) {
  return /^(?=.*[A-Za-z])(?=.*\d).{8,32}$/.test(value);
}

export function AuthPanel({
  initialMode = "email",
  onSuccess,
}: {
  initialMode?: AuthMode;
  onSuccess?: () => void;
}) {
  const {
    loginBySms,
    loginByEmail,
    loginByEmailCode,
    registerByEmail,
    resetPasswordByEmail,
    sendSmsCode,
    sendEmailCode,
  } = useAuth();
  const [view, setView] = useState<AuthView>(() => getInitialView(initialMode));
  const [mobile, setMobile] = useState("");
  const [smsCode, setSmsCode] = useState("");
  const [email, setEmail] = useState("");
  const [emailCode, setEmailCode] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [agreeTerms, setAgreeTerms] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [sendingCode, setSendingCode] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const [error, setError] = useState("");

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setView(getInitialView(initialMode));
      setError("");
    }, 0);
    return () => window.clearTimeout(timer);
  }, [initialMode]);

  useEffect(() => {
    if (countdown <= 0) return;
    const timer = window.setTimeout(() => setCountdown((value) => value - 1), 1000);
    return () => window.clearTimeout(timer);
  }, [countdown]);

  function switchView(nextView: AuthView) {
    setView(nextView);
    setError("");
  }

  async function handleSendCode() {
    setError("");
    try {
      setSendingCode(true);
      if (view === "phone-code") {
        if (!isMobile(mobile)) throw new Error("请输入正确的手机号");
        await sendSmsCode(mobile);
      } else {
        if (!isEmail(email)) throw new Error("请输入正确的邮箱地址");
        const scene = view === "reset" ? "RESET_PASSWORD" : view === "email-code" ? "LOGIN" : "REGISTER";
        await sendEmailCode(email, scene);
      }
      setCountdown(60);
    } catch (err) {
      setError(err instanceof Error ? err.message : "验证码发送失败");
    } finally {
      setSendingCode(false);
    }
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError("");
    try {
      setLoading(true);
      if (view === "login") {
        if (!isEmail(email)) throw new Error("请输入正确的邮箱地址");
        if (!password) throw new Error("请输入密码");
        await loginByEmail(email, password);
      }
      if (view === "email-code") {
        if (!isEmail(email)) throw new Error("请输入正确的邮箱地址");
        if (!/^\d{4,6}$/.test(emailCode)) throw new Error("请输入邮箱验证码");
        await loginByEmailCode(email, emailCode);
      }
      if (view === "phone-code") {
        if (!isMobile(mobile)) throw new Error("请输入正确的手机号");
        if (!/^\d{6}$/.test(smsCode)) throw new Error("请输入 6 位短信验证码");
        await loginBySms(mobile, smsCode);
      }
      if (view === "register") {
        if (!isEmail(email)) throw new Error("请输入正确的邮箱地址");
        if (!/^\d{4,6}$/.test(emailCode)) throw new Error("请输入邮箱验证码");
        if (!isPassword(password)) throw new Error("密码需 8-32 位，至少包含字母和数字");
        if (password !== confirmPassword) throw new Error("两次输入的密码不一致");
        if (!agreeTerms) throw new Error("请先同意用户协议和隐私政策");
        await registerByEmail({
          email,
          code: emailCode,
          password,
          agreeTerms,
        });
      }
      if (view === "reset") {
        if (!isEmail(email)) throw new Error("请输入正确的邮箱地址");
        if (!/^\d{4,6}$/.test(emailCode)) throw new Error("请输入邮箱验证码");
        if (!isPassword(password)) throw new Error("密码需 8-32 位，至少包含字母和数字");
        if (password !== confirmPassword) throw new Error("两次输入的密码不一致");
        await resetPasswordByEmail(email, emailCode, password);
        setPassword("");
        setConfirmPassword("");
        setEmailCode("");
        setError("密码已重置，请使用新密码登录");
        setView("login");
        return;
      }
      onSuccess?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : "操作失败，请稍后重试");
    } finally {
      setLoading(false);
    }
  }

  const isCodeView = view === "email-code" || view === "phone-code" || view === "register" || view === "reset";
  const isEmailCodeView = view === "email-code" || view === "register" || view === "reset";
  const needsPassword = view === "login" || view === "register" || view === "reset";
  const needsConfirmPassword = view === "register" || view === "reset";
  const codeDisabled =
    sendingCode ||
    countdown > 0 ||
    (view === "phone-code" ? !isMobile(mobile) : !isEmail(email));

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
      {view === "phone-code" ? (
        <Field label="手机号">
          <input
            type="tel"
            value={mobile}
            onChange={(event) => setMobile(event.target.value.trim())}
            placeholder="请输入手机号"
            className="input-base"
          />
        </Field>
      ) : (
        <Field label="邮箱">
          <input
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value.trim())}
            placeholder="请输入邮箱"
            className="input-base"
          />
        </Field>
      )}

      {isCodeView && (
        <VerificationCodeField
          channel={isEmailCodeView ? "email" : "sms"}
          value={view === "phone-code" ? smsCode : emailCode}
          onChange={view === "phone-code" ? setSmsCode : setEmailCode}
          onSend={handleSendCode}
          disabled={codeDisabled}
          sending={sendingCode}
          countdown={countdown}
          recipient={view === "phone-code" ? mobile : email}
          placeholder={isEmailCodeView ? "请输入邮箱验证码" : "请输入 6 位短信验证码"}
        />
      )}

      {needsPassword && (
        <Field
          label={view === "reset" ? "新密码" : "密码"}
          hint={needsConfirmPassword ? "8-32 位，至少包含字母和数字" : undefined}
        >
          <div className="relative">
            <input
              type={showPassword ? "text" : "password"}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder={view === "reset" ? "请输入新密码" : "请输入密码"}
              className="input-base pr-10"
            />
            <button
              type="button"
              onClick={() => setShowPassword((value) => !value)}
              className="absolute right-3 top-1/2 -translate-y-1/2 rounded-md p-1 text-muted-gray hover:text-charcoal focus:outline-none focus:shadow-[rgba(0,0,0,0.1)_0px_4px_12px]"
              aria-label={showPassword ? "隐藏密码" : "显示密码"}
            >
              {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
            </button>
          </div>
        </Field>
      )}

      {needsConfirmPassword && (
        <Field label="确认密码">
          <input
            type={showPassword ? "text" : "password"}
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            placeholder="请再次输入密码"
            className="input-base"
          />
        </Field>
      )}

      {view === "register" && (
        <label className="flex items-start gap-2 text-xs leading-relaxed text-muted-gray">
          <input
            type="checkbox"
            checked={agreeTerms}
            onChange={(event) => setAgreeTerms(event.target.checked)}
            className="mt-0.5"
          />
          <span>我已阅读并同意使用条款和隐私政策</span>
        </label>
      )}

      {error && <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">{error}</p>}

      <button type="submit" disabled={loading} className="primary-button w-full disabled:opacity-50">
        {loading ? "处理中..." : getSubmitLabel(view)}
      </button>

      <AuthActions view={view} onSwitch={switchView} />

      <p className="text-center text-xs leading-relaxed text-muted-gray">
        继续即表示您同意 <span className="text-charcoal underline">使用条款</span> 和{" "}
        <span className="text-charcoal underline">隐私政策</span>
      </p>
    </form>
  );
}

function getSubmitLabel(view: AuthView) {
  if (view === "register") return "注册并登录";
  if (view === "reset") return "重置密码";
  return "登录";
}

function AuthActions({
  view,
  onSwitch,
}: {
  view: AuthView;
  onSwitch: (view: AuthView) => void;
}) {
  if (view === "login") {
    return (
      <div className="space-y-3 text-center text-xs text-muted-gray">
        <div className="flex items-center justify-between gap-3">
          <button type="button" onClick={() => onSwitch("reset")} className="auth-link">
            忘记密码？
          </button>
          <button type="button" onClick={() => onSwitch("register")} className="auth-link">
            没有账号？注册
          </button>
        </div>
        <div className="flex flex-wrap justify-center gap-x-4 gap-y-2 border-t border-border-warm pt-3">
          <button type="button" onClick={() => onSwitch("email-code")} className="auth-link">
            使用邮箱验证码登录
          </button>
          {/* <button type="button" onClick={() => onSwitch("phone-code")} className="auth-link">
            使用手机验证码登录
          </button> */}
        </div>
      </div>
    );
  }

  if (view === "email-code") {
    return (
      <div className="flex items-center justify-between gap-3 text-xs text-muted-gray">
        <button type="button" onClick={() => onSwitch("login")} className="auth-link">
          使用密码登录
        </button>
        <button type="button" onClick={() => onSwitch("register")} className="auth-link">
          注册账号
        </button>
      </div>
    );
  }

  if (view === "phone-code") {
    return (
      <div className="flex items-center justify-between gap-3 text-xs text-muted-gray">
        <button type="button" onClick={() => onSwitch("login")} className="auth-link">
          使用邮箱登录
        </button>
        <button type="button" onClick={() => onSwitch("register")} className="auth-link">
          注册账号
        </button>
      </div>
    );
  }

  return (
    <div className="text-center text-xs text-muted-gray">
      <button type="button" onClick={() => onSwitch("login")} className="auth-link">
        返回邮箱密码登录
      </button>
    </div>
  );
}

function Field({ label, hint, children }: { label: string; hint?: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm text-charcoal">{label}</span>
      {children}
      {hint && <span className="mt-1 block text-xs text-muted-gray">{hint}</span>}
    </label>
  );
}
