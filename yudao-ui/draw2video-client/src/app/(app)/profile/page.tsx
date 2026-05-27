"use client";

import Image from "next/image";
import { useState } from "react";
import { LogOut, ShieldCheck } from "lucide-react";
import { useAuth } from "@/features/auth/auth-store";
import { updateProfile } from "@/features/profile/profile-api";

function maskMobile(value?: string) {
  if (!value) return "--";
  return value.replace(/^(\d{3})\d{4}(\d{4})$/, "$1****$2");
}

function maskEmail(value?: string) {
  if (!value) return "未绑定";
  const [name, domain] = value.split("@");
  if (!domain) return value;
  return `${name.slice(0, 1)}***@${domain}`;
}

export default function ProfilePage() {
  const { user, fetchUser, logout } = useAuth();
  const [nickname, setNickname] = useState(user?.nickname ?? "");
  const [avatar, setAvatar] = useState(user?.avatar ?? "");
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");

  async function handleSave(event: React.FormEvent) {
    event.preventDefault();
    setMessage("");
    try {
      setSaving(true);
      await updateProfile({ nickname: nickname.trim(), avatar: avatar.trim() || undefined });
      await fetchUser();
      setMessage("资料已保存");
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "保存失败，请稍后重试");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="mx-auto max-w-[880px] px-4 py-10">
      <h1 className="text-2xl font-semibold tracking-tight text-charcoal">个人设置</h1>
      <p className="mt-2 text-sm text-muted-gray">管理你的账号资料和安全信息。</p>

      <div className="mt-6 rounded-xl border border-border-warm bg-background p-5">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
          <div className="flex size-16 shrink-0 items-center justify-center overflow-hidden rounded-full bg-charcoal text-2xl font-medium text-off-white">
            {user?.avatar ? (
              <Image src={user.avatar} alt="头像" width={64} height={64} className="size-full object-cover" />
            ) : (
              user?.nickname?.[0] ?? "U"
            )}
          </div>
          <div className="min-w-0 flex-1">
            <p className="truncate text-lg font-medium text-charcoal">{user?.nickname || "未设置昵称"}</p>
            <p className="mt-1 text-sm text-muted-gray">手机号：{maskMobile(user?.mobile)}</p>
            <p className="mt-1 text-sm text-muted-gray">邮箱：{maskEmail(user?.email)}</p>
          </div>
          <div className="inline-flex items-center gap-1 rounded-full bg-muted px-3 py-1 text-xs text-charcoal">
            <ShieldCheck className="size-3.5" />
            账号正常
          </div>
        </div>
      </div>

      <form onSubmit={handleSave} className="mt-4 rounded-xl border border-border-warm bg-background p-5">
        <h2 className="text-base font-medium text-charcoal">基础资料</h2>
        <div className="mt-4 grid gap-4 sm:grid-cols-2">
          <label>
            <span className="mb-1.5 block text-sm text-muted-gray">昵称</span>
            <input value={nickname} onChange={(event) => setNickname(event.target.value)} className="input-base" placeholder="请输入昵称" />
          </label>
          <label>
            <span className="mb-1.5 block text-sm text-muted-gray">头像地址</span>
            <input value={avatar} onChange={(event) => setAvatar(event.target.value)} className="input-base" placeholder="请输入头像 URL" />
          </label>
        </div>
        {message && <p className="mt-3 text-sm text-muted-gray">{message}</p>}
        <button type="submit" disabled={saving} className="primary-button mt-4 disabled:opacity-50">
          {saving ? "保存中..." : "保存修改"}
        </button>
      </form>

      <div className="mt-4 rounded-xl border border-border-warm bg-background p-5">
        <h2 className="text-base font-medium text-charcoal">账号安全</h2>
        <div className="mt-4 divide-y divide-border-warm text-sm">
          <div className="flex items-center justify-between py-3">
            <span className="text-muted-gray">手机号</span>
            <span className="text-charcoal">{maskMobile(user?.mobile)}</span>
          </div>
          <div className="flex items-center justify-between py-3">
            <span className="text-muted-gray">邮箱</span>
            <span className="text-charcoal">{maskEmail(user?.email)}</span>
          </div>
          <div className="flex items-center justify-between py-3">
            <span className="text-muted-gray">密码</span>
            <span className="text-muted-gray">修改密码功能待开放</span>
          </div>
        </div>
        <button
          type="button"
          onClick={() => {
            if (window.confirm("确定要退出登录吗？")) logout();
          }}
          className="mt-4 inline-flex items-center gap-2 rounded-md border border-border-warm px-4 py-2.5 text-sm text-muted-gray hover:text-charcoal"
        >
          <LogOut className="size-4" />
          退出登录
        </button>
      </div>
    </div>
  );
}
