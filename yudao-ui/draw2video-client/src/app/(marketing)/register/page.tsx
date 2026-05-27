"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/auth-store";
import { AuthPanel } from "@/features/auth/AuthPanel";

export default function RegisterPage() {
  const { loggedIn } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (loggedIn) {
      router.replace("/app");
    }
  }, [loggedIn, router]);

  return (
    <div className="mx-auto flex min-h-[calc(100vh-56px)] max-w-[1120px] items-center px-4 py-10">
      <div className="grid w-full gap-8 md:grid-cols-[1fr_420px] md:items-center">
        <div>
          <p className="text-sm font-medium text-muted-gray">创建 Copse 账号</p>
          <h1 className="mt-3 text-4xl font-semibold tracking-tight text-charcoal">
            用邮箱验证码快速注册
          </h1>
          <p className="mt-4 max-w-[520px] text-sm leading-6 text-muted-gray">
            注册后即可保存创作项目、沉淀生成资产，并在任务中心追踪每一次 AIGC 生成进度。
          </p>
          <div className="mt-8 grid gap-3 text-sm text-charcoal sm:grid-cols-3">
            <div className="rounded-xl border border-border-warm bg-background p-4">邮箱验证码注册</div>
            <div className="rounded-xl border border-border-warm bg-background p-4">自动保存登录态</div>
            <div className="rounded-xl border border-border-warm bg-background p-4">进入创作工作台</div>
          </div>
        </div>
        <div className="rounded-2xl border border-border-warm bg-background p-6">
          <h2 className="text-lg font-medium text-charcoal">注册新账号</h2>
          <p className="mt-2 text-sm text-muted-gray">使用邮箱验证码注册，完成后自动进入工作台。</p>
          <div className="mt-6">
            <AuthPanel initialMode="register" />
          </div>
        </div>
      </div>
    </div>
  );
}
