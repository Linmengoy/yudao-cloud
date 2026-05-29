"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/auth-store";

export default function HomePage() {
  const router = useRouter();
  const { loggedIn, openModal } = useAuth();

  function handleStartCreate() {
    if (loggedIn) {
      router.push("/create/image");
      return;
    }
    openModal("email", "/create/image", "required");
  }

  return (
    <div className="flex flex-col">
      {/* Hero */}
      <section className="flex flex-col items-center px-4 pb-20 pt-24 text-center">
        <h1 className="text-[clamp(36px,5vw,60px)] font-semibold leading-[1.1] tracking-tight text-charcoal">
          用 AI 释放你的创意
        </h1>
        <p className="mt-4 max-w-xl text-lg leading-relaxed text-muted-gray">
          输入文字描述，生成高质量图片。简单、快速、无限制。
        </p>
        <div className="mt-8 flex items-center gap-4">
          <button
            type="button"
            onClick={handleStartCreate}
            className="inline-flex items-center rounded-md bg-charcoal px-6 py-2.5 text-sm text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] active:opacity-80"
          >
            开始创作
          </button>
          <Link
            href="/pricing"
            className="inline-flex items-center rounded-md border border-[rgba(28,28,28,0.4)] px-6 py-2.5 text-sm text-charcoal active:opacity-80"
          >
            查看价格
          </Link>
        </div>
      </section>

      {/* Features */}
      <section className="mx-auto grid max-w-[1200px] gap-6 px-4 pb-24 md:grid-cols-3">
        {[
          {
            title: "文生图",
            desc: "输入文字描述，选择模型和尺寸，一键生成高质量图片。",
          },
          {
            title: "多模型支持",
            desc: "集成多种 AI 图片生成模型，满足不同风格需求。",
          },
          {
            title: "按量计费",
            desc: "灵活的充值方案，按使用量扣费，无隐藏费用。",
          },
        ].map((f) => (
          <div
            key={f.title}
            className="rounded-xl border border-border-warm bg-background p-6"
          >
            <h3 className="text-base font-medium text-charcoal">{f.title}</h3>
            <p className="mt-2 text-sm leading-relaxed text-muted-gray">
              {f.desc}
            </p>
          </div>
        ))}
      </section>
    </div>
  );
}
