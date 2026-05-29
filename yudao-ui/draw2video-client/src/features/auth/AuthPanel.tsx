"use client";

import { useEffect, useState } from "react";
import { Eye, EyeOff } from "lucide-react";
import { useAuth } from "./auth-store";
import type { AuthMode } from "./auth-types";

const tabs: Array<{ mode: AuthMode; label: string }> = [
  { mode: "sms", label: "手机验证码" },
  { mode: "email", label: "邮箱登录" },
  { mode: "password", label: "密码登录" },
];

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
  initialMode = "sms",
  onSuccess,
}: {
  initialMode?: AuthMode;
  onSuccess?: () => void;
}) {
  const {
    loginByPassword,
    loginBySms,
    loginByEmail,
    loginByEmailCode,
    registerByEmail,
    resetPasswordByEmail,
    sendSmsCode,
    sendEmailCode,
  } = useAuth();
  const [mode, setMode] = useState<AuthMode>(initialMode);
  const [emailLoginType, setEmailLoginType] = useState<"password" | "code">("password");
  const [mobile, setMobile] = useState("");
  const [smsCode, setSmsCode] = useState("");
  const [email, setEmail] = useState("");
  const [emailCode, setEmailCode] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [inviteCode, setInviteCode] = useState("");
  const [agreeTerms, setAgreeTerms] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [smsSending, setSmsSending] = useState(false);
  const [emailSending, setEmailSending] = useState(false);
  const [smsCountdown, setSmsCountdown] = useState(0);
  const [emailCountdown, setEmailCountdown] = useState(0);
  const [error, setError] = useState("");

  useEffect(() => {
    const timer = window.setTimeout(() => setMode(initialMode), 0);
    return () => window.clearTimeout(timer);
  }, [initialMode]);

  useEffect(() => {
    if (smsCountdown <= 0) return;
    const timer = window.setTimeout(() => setSmsCountdown((value) => value - 1), 1000);
    return () => window.clearTimeout(timer);
  }, [smsCountdown]);

  useEffect(() => {
    if (emailCountdown <= 0) return;
    const timer = window.setTimeout(() => setEmailCountdown((value) => value - 1), 1000);
    return () => window.clearTimeout(timer);
  }, [emailCountdown]);

  async function handleSendCode() {
    setError("");
    try {
      if (mode === "sms") {
        if (!isMobile(mobile)) throw new Error("请输入正确的手机号");
        setSmsSending(true);
        await sendSmsCode(mobile);
        setSmsCountdown(60);
      } else {
        if (!isEmail(email)) throw new Error("请输入正确的邮箱地址");
        setEmailSending(true);
        await sendEmailCode(email, mode === "forgot" ? "RESET_PASSWORD" : mode === "email" ? "LOGIN" : "REGISTER");
        setEmailCountdown(60);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "验证码发送失败");
    } finally {
      setSmsSending(false);
      setEmailSending(false);
    }
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError("");
    try {
      setLoading(true);
      if (mode === "sms") {
        if (!isMobile(mobile)) throw new Error("请输入正确的手机号");
        if (!/^\d{6}$/.test(smsCode)) throw new Error("请输入 6 位短信验证码");
        await loginBySms(mobile, smsCode);
      }
      if (mode === "password") {
        if (!isMobile(mobile)) throw new Error("请输入正确的手机号");
        if (!password) throw new Error("请输入密码");
        await loginByPassword(mobile, password);
      }
      if (mode === "email") {
        if (!isEmail(email)) throw new Error("请输入正确的邮箱地址");
        if (emailLoginType === "password") {
          if (!password) throw new Error("请输入密码");
          await loginByEmail(email, password);
        } else {
          if (!/^\d{4}$/.test(emailCode)) throw new Error("请输入 4 位邮箱验证码");
          await loginByEmailCode(email, emailCode);
        }
      }
      if (mode === "register") {
        if (!isEmail(email)) throw new Error("请输入正确的邮箱地址");
        if (!/^\d{4}$/.test(emailCode)) throw new Error("请输入 4 位邮箱验证码");
        if (!isPassword(password)) throw new Error("密码需 8-32 位，至少包含字母和数字");
        if (password !== confirmPassword) throw new Error("两次输入的密码不一致");
        if (!agreeTerms) throw new Error("请先同意用户协议和隐私政策");
        await registerByEmail({
          email,
          code: emailCode,
          password,
          agreeTerms,
          inviteCode: inviteCode.trim() || undefined,
        });
      }
      if (mode === "forgot") {
        if (!isEmail(email)) throw new Error("请输入正确的邮箱地址");
        if (!/^\d{4}$/.test(emailCode)) throw new Error("请输入 4 位邮箱验证码");
        if (!isPassword(password)) throw new Error("密码需 8-32 位，至少包含字母和数字");
        if (password !== confirmPassword) throw new Error("两次输入的密码不一致");
        await resetPasswordByEmail(email, emailCode, password);
        setMode("email");
        setEmailLoginType("password");
        setPassword("");
        setConfirmPassword("");
        setEmailCode("");
        setError("密码已重置，请使用新密码登录");
        return;
      }
      onSuccess?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : "操作失败，请稍后重试");
    } finally {
      setLoading(false);
    }
  }

  const smsCodeDisabled = smsSending || smsCountdown > 0 || !isMobile(mobile);
  const emailCodeDisabled = emailSending || emailCountdown > 0 || !isEmail(email);

  return (
    <>
      <div className="grid grid-cols-3 gap-1 rounded-lg bg-muted p-1">
        {tabs.map((tab) => (
          <button
            key={tab.mode}
            type="button"
            onClick={() => {
              setError("");
              setMode(tab.mode);
            }}
            className={`rounded-md px-2 py-2 text-xs transition-colors ${
              mode === tab.mode
                ? "bg-background text-charcoal shadow-[rgba(0,0,0,0.1)_0px_4px_12px]"
                : "text-muted-gray hover:text-charcoal"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-4">
        {(mode === "register" || mode === "email" || mode === "forgot") ? (
          <>
            <Field label="邮箱">
              <input
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value.trim())}
                placeholder="请输入邮箱"
                className="input-base"
              />
            </Field>
            {mode === "email" && (
              <div className="grid grid-cols-2 gap-1 rounded-lg bg-muted p-1">
                <SegmentButton active={emailLoginType === "password"} onClick={() => setEmailLoginType("password")}>密码登录</SegmentButton>
                <SegmentButton active={emailLoginType === "code"} onClick={() => setEmailLoginType("code")}>验证码登录</SegmentButton>
              </div>
            )}
            {(mode === "register" || mode === "forgot" || (mode === "email" && emailLoginType === "code")) && (
              <CodeField value={emailCode} onChange={setEmailCode} onSend={handleSendCode} disabled={emailCodeDisabled} sending={emailSending} countdown={emailCountdown} />
            )}
          </>
        ) : (
          <Field label="手机号">
            <input
              type="tel"
              value={mobile}
              onChange={(event) => setMobile(event.target.value.trim())}
              placeholder="请输入手机号"
              className="input-base"
            />
          </Field>
        )}

        {mode === "sms" && (
          <CodeField value={smsCode} onChange={setSmsCode} onSend={handleSendCode} disabled={smsCodeDisabled} sending={smsSending} countdown={smsCountdown} />
        )}

        {(mode === "password" || mode === "register" || mode === "forgot" || (mode === "email" && emailLoginType === "password")) && (
          <Field label={mode === "forgot" ? "新密码" : "密码"} hint={mode === "register" || mode === "forgot" ? "8-32 位，至少包含字母和数字" : undefined}>
            <div className="relative">
              <input
                type={showPassword ? "text" : "password"}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="请输入密码"
                className="input-base pr-10"
              />
              <button
                type="button"
                onClick={() => setShowPassword((value) => !value)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-gray hover:text-charcoal"
                aria-label={showPassword ? "隐藏密码" : "显示密码"}
              >
                {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
              </button>
            </div>
          </Field>
        )}

        {(mode === "register" || mode === "forgot") && (
          <>
            <Field label="确认密码">
              <input
                type={showPassword ? "text" : "password"}
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                placeholder="请再次输入密码"
                className="input-base"
              />
            </Field>
            {mode === "register" && (
              <>
                <Field label="邀请码（选填）">
                  <input
                    value={inviteCode}
                    onChange={(event) => setInviteCode(event.target.value)}
                    placeholder="如有邀请码可填写"
                    className="input-base"
                  />
                </Field>
                <label className="flex items-start gap-2 text-xs text-muted-gray">
                  <input
                    type="checkbox"
                    checked={agreeTerms}
                    onChange={(event) => setAgreeTerms(event.target.checked)}
                    className="mt-0.5"
                  />
                  <span>我已阅读并同意用户协议和隐私政策</span>
                </label>
              </>
            )}
          </>
        )}

        {(mode === "password" || mode === "email") && (
          <button type="button" onClick={() => { setError(""); setMode("forgot"); }} className="self-end text-xs text-muted-gray hover:text-charcoal">
            忘记密码
          </button>
        )}

        {mode === "email" && (
          <p className="text-center text-xs text-muted-gray">
            还没有账号？
            <button type="button" onClick={() => { setError(""); setMode("register"); }} className="text-charcoal underline">
              立即注册
            </button>
          </p>
        )}

        {(mode === "register" || mode === "forgot") && (
          <button type="button" onClick={() => { setError(""); setMode("email"); }} className="self-center text-xs text-muted-gray hover:text-charcoal">
            返回邮箱登录
          </button>
        )}

        {error && <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">{error}</p>}

        <button type="submit" disabled={loading} className="primary-button w-full disabled:opacity-50">
          {loading ? "处理中..." : mode === "register" ? "注册并登录" : mode === "forgot" ? "重置密码" : "登录 / 注册"}
        </button>
      </form>

      <p className="mt-4 text-center text-xs text-muted-gray">
        继续即表示您同意 <span className="text-charcoal underline">使用条款</span> 和 <span className="text-charcoal underline">隐私政策</span>
      </p>
    </>
  );
}

function SegmentButton({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-md px-2 py-2 text-xs transition-colors ${
        active ? "bg-background text-charcoal shadow-[rgba(0,0,0,0.1)_0px_4px_12px]" : "text-muted-gray hover:text-charcoal"
      }`}
    >
      {children}
    </button>
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

function CodeField({
  value,
  onChange,
  onSend,
  disabled,
  sending,
  countdown,
}: {
  value: string;
  onChange: (value: string) => void;
  onSend: () => void;
  disabled: boolean;
  sending: boolean;
  countdown: number;
}) {
  return (
    <Field label="验证码">
      <div className="flex gap-2 max-sm:flex-col">
        <input
          inputMode="numeric"
          value={value}
          onChange={(event) => onChange(event.target.value.replace(/\D/g, "").slice(0, 6))}
          placeholder="请输入 6 位验证码"
          className="input-base"
        />
        <button
          type="button"
          disabled={disabled}
          onClick={onSend}
          className="shrink-0 rounded-md border border-[rgba(28,28,28,0.4)] px-3 py-2.5 text-sm text-charcoal active:opacity-80 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {sending ? "发送中" : countdown > 0 ? `${countdown}s` : "获取验证码"}
        </button>
      </div>
    </Field>
  );
}
