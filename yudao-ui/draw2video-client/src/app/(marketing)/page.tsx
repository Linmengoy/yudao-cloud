"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowRight, ImageIcon, Layers, Sparkles } from "lucide-react";
import { useAuth } from "@/features/auth/auth-store";

export default function HomePage() {
  const router = useRouter();
  const { loggedIn, openModal } = useAuth();

  function handleStartCreate() {
    if (loggedIn) {
      router.push("/canvas");
      return;
    }
    openModal("email", "/canvas", "required");
  }

  return (
    <div className="flex flex-col overflow-hidden">
      <section className="relative px-4 pb-20 pt-20 sm:pt-24">
        <div className="pointer-events-none absolute inset-x-0 top-0 h-[420px] bg-[linear-gradient(135deg,rgba(255,208,164,0.36),rgba(247,244,237,0.2)_44%,rgba(155,190,222,0.26))] dark:bg-[linear-gradient(135deg,rgba(124,83,47,0.3),rgba(23,21,18,0.18)_44%,rgba(58,77,96,0.34))]" />
        <div className="relative mx-auto grid max-w-[1200px] items-center gap-12 lg:grid-cols-[minmax(0,0.94fr)_minmax(430px,1.06fr)]">
          <div className="max-w-2xl">
            <p className="mb-5 inline-flex rounded-full border border-border-warm bg-background/70 px-3 py-1 text-sm text-muted-gray">
              Copse AI Creative Workspace
            </p>
            <h1 className="max-w-[680px] text-[clamp(40px,7vw,60px)] font-semibold leading-[1.02] tracking-[-1.5px] text-charcoal">
              把灵感整理成可以继续生长的作品
            </h1>
            <p className="mt-5 max-w-xl text-lg leading-[1.38] text-muted-gray">
              在同一个画布里生成图片、连接参考、追踪任务和沉淀资产。创作过程保留上下文，结果也能回到项目。
            </p>
            <div className="mt-8 flex flex-wrap items-center gap-3">
              <button
                type="button"
                onClick={handleStartCreate}
                className="primary-button inline-flex items-center gap-2 px-5"
              >
                开始创作
                <ArrowRight className="size-4" />
              </button>
              <Link href="/pricing" className="secondary-button inline-flex items-center px-5">
                查看价格
              </Link>
            </div>
          </div>

          <div className="relative min-h-[420px]">
            <div className="absolute left-0 top-8 w-[64%] rounded-xl border border-border-warm bg-background p-3">
              <div className="aspect-[4/3] rounded-lg border border-border-warm bg-[linear-gradient(145deg,rgba(28,28,28,0.04),rgba(252,251,248,0.9)),linear-gradient(45deg,rgba(239,176,122,0.38),rgba(139,172,207,0.28))] dark:bg-[linear-gradient(145deg,rgba(244,239,230,0.05),rgba(23,21,18,0.9)),linear-gradient(45deg,rgba(124,83,47,0.34),rgba(58,77,96,0.32))]" />
              <div className="mt-3 flex items-center justify-between">
                <div>
                  <p className="text-sm text-charcoal">Image node</p>
                  <p className="text-xs text-muted-gray">text-to-image / edit</p>
                </div>
                <Sparkles className="size-4 text-charcoal" />
              </div>
            </div>
            <div className="absolute right-0 top-0 w-[52%] rounded-xl border border-border-warm bg-background p-4">
              <div className="mb-4 flex items-center gap-2">
                <div className="flex size-8 items-center justify-center rounded-full bg-charcoal text-off-white">
                  <Layers className="size-4" />
                </div>
                <div>
                  <p className="text-sm text-charcoal">Project canvas</p>
                  <p className="text-xs text-muted-gray">3 nodes connected</p>
                </div>
              </div>
              <div className="space-y-2">
                {["选择参考图", "写入提示词", "生成并归档"].map((item) => (
                  <div key={item} className="rounded-lg border border-border-warm px-3 py-2 text-sm text-muted-gray">
                    {item}
                  </div>
                ))}
              </div>
            </div>
            <div className="absolute bottom-0 right-12 w-[58%] rounded-xl border border-border-warm bg-background p-4">
              <div className="flex items-center gap-3">
                <div className="flex size-10 items-center justify-center rounded-lg border border-border-warm">
                  <ImageIcon className="size-5 text-charcoal" />
                </div>
                <div>
                  <p className="text-sm text-charcoal">Asset library</p>
                  <p className="text-xs text-muted-gray">生成结果自动回流到资产库</p>
                </div>
              </div>
              <div className="mt-4 grid grid-cols-3 gap-2">
                <div className="aspect-square rounded-md border border-border-warm bg-[rgba(28,28,28,0.04)]" />
                <div className="aspect-square rounded-md border border-border-warm bg-[rgba(28,28,28,0.03)]" />
                <div className="aspect-square rounded-md border border-border-warm bg-[rgba(28,28,28,0.04)]" />
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="mx-auto grid w-full max-w-[1200px] gap-6 px-4 pb-20 sm:grid-cols-3">
        {[
          ["项目画布", "每个项目都有独立草稿、节点、边和视口状态，重新打开时继续上次创作。"],
          ["多模态节点", "图片、文字、视频节点共存，参考关系直接通过连线表达。"],
          ["资产沉淀", "生成图片和视频汇总到资产库，并保留回到来源项目的路径。"],
        ].map(([title, desc]) => (
          <article key={title} className="rounded-xl border border-border-warm bg-background p-6">
            <h2 className="text-xl font-normal leading-tight text-charcoal">{title}</h2>
            <p className="mt-3 text-sm leading-relaxed text-muted-gray">{desc}</p>
          </article>
        ))}
      </section>

      <section className="border-y border-border-warm px-4 py-16">
        <div className="mx-auto grid max-w-[1200px] gap-8 md:grid-cols-3">
          {[
            ["∞", "本地项目草稿"],
            ["3", "创作节点类型"],
            ["1", "统一资产入口"],
          ].map(([value, label]) => (
            <div key={label}>
              <p className="text-5xl font-semibold leading-none tracking-[-1.2px] text-charcoal">{value}</p>
              <p className="mt-2 text-base text-muted-gray">{label}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="mx-auto grid max-w-[1200px] gap-10 px-4 py-20 lg:grid-cols-[0.86fr_1.14fr]">
        <div>
          <h2 className="text-[clamp(32px,4.6vw,48px)] font-semibold leading-none tracking-[-1.2px] text-charcoal">
            一个入口覆盖创作、任务和资产
          </h2>
          <p className="mt-4 max-w-md text-base leading-relaxed text-muted-gray">
            入口页只负责开始；登录后，侧边栏会把你带到项目、资产和任务三个核心位置。
          </p>
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          {[
            ["项目", "从项目库进入画布，保留创作上下文。"],
            ["任务", "查看生成状态、排队和完成结果。"],
            ["资产", "管理生成图片、视频和下载记录。"],
            ["钱包", "按量计费，余额与用量随时可见。"],
          ].map(([title, desc]) => (
            <article key={title} className="rounded-xl border border-border-warm bg-background p-5">
              <h3 className="text-xl font-normal leading-tight text-charcoal">{title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-muted-gray">{desc}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="px-4 pb-24">
        <div className="mx-auto flex max-w-[1200px] flex-col items-center rounded-2xl border border-border-warm bg-[linear-gradient(180deg,rgba(252,251,248,0.72),rgba(247,244,237,1))] px-6 py-14 text-center dark:bg-[linear-gradient(180deg,rgba(244,239,230,0.06),rgba(23,21,18,1))]">
          <h2 className="max-w-2xl text-[clamp(32px,5vw,48px)] font-semibold leading-none tracking-[-1.2px] text-charcoal">
            从一个空白画布开始
          </h2>
          <p className="mt-4 max-w-xl text-base leading-relaxed text-muted-gray">
            上传参考，写下想法，生成第一张图，然后把它继续连接成新的文本或视频。
          </p>
          <div className="mt-7 flex flex-wrap justify-center gap-3">
            <button type="button" onClick={handleStartCreate} className="primary-button inline-flex items-center gap-2 px-5">
              进入创作
              <ArrowRight className="size-4" />
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}
