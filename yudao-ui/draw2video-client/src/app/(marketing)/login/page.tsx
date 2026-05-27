"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/auth-store";
import { AuthPanel } from "@/features/auth/AuthPanel";

export default function LoginPage() {
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
          <p className="text-sm font-medium text-muted-gray">Copse 账号</p>
          <h1 className="mt-3 text-4xl font-semibold tracking-tight text-charcoal">
            登录后继续创作
          </h1>
          <p className="mt-4 max-w-[520px] text-sm leading-6 text-muted-gray">
            登录后可保存作品、查看任务进度、管理资产，并在钱包中追踪每一次生成消耗。
          </p>
          <div className="mt-8 grid gap-3 text-sm text-charcoal sm:grid-cols-3">
            <div className="rounded-xl border border-border-warm bg-background p-4">保存作品草稿</div>
            <div className="rounded-xl border border-border-warm bg-background p-4">追踪生成任务</div>
            <div className="rounded-xl border border-border-warm bg-background p-4">管理资产钱包</div>
          </div>
        </div>
        <div className="rounded-2xl border border-border-warm bg-background p-6">
          <h2 className="text-lg font-medium text-charcoal">账号登录 / 注册</h2>
          <p className="mt-2 text-sm text-muted-gray">选择一种方式继续进入工作台。</p>
          <div className="mt-6">
            <AuthPanel initialMode="sms" />
          </div>
        </div>
      </div>
    </div>
  );
}
