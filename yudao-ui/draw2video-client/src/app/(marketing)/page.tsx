"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowRight, Clapperboard, ImageIcon, Layers3, Play, Sparkles, Wand2 } from "lucide-react";
import { useAuth } from "@/features/auth/auth-store";
import { getCommunityPosts } from "@/features/community/community-api";
import { CommunityPostCard } from "@/features/community/CommunityPostCard";
import type { CommunityPost } from "@/features/community/community-types";

const inspirationWorks = [
  {
    title: "棚拍人像重塑",
    label: "风格",
    image: "/www-home/assets/images/inspiration-portrait.webp",
    className: "md:col-span-2",
  },
  {
    title: "竹林动作短片",
    label: "运镜",
    image: "/www-home/assets/images/inspiration-bamboo.webp",
    className: "",
  },
  {
    title: "能量轨迹模拟",
    label: "特效",
    image: "/www-home/assets/images/inspiration-robots.webp",
    className: "",
  },
  {
    title: "音频响应光流",
    label: "视觉",
    image: "/www-home/assets/images/inspiration-wave.webp",
    className: "md:col-span-2",
  },
];

const modelCards = [
  ["Image", "高质图像生成", "适合海报、人像、产品视觉和风格探索。"],
  ["Video", "图生视频", "把关键画面扩展为有运动、有节奏的短片。"],
  ["Effect", "创意特效", "快速生成转场、光流、粒子和视觉包装。"],
];

function HomePage() {
  const router = useRouter();
  const { loggedIn, openModal } = useAuth();
  const [communityPosts, setCommunityPosts] = useState<CommunityPost[]>([]);

  useEffect(() => {
    let ignore = false;
    getCommunityPosts({ pageNo: 1, pageSize: 6, sort: "hot" })
      .then((data) => {
        if (!ignore) setCommunityPosts(data.list ?? []);
      })
      .catch(() => {
        if (!ignore) setCommunityPosts([]);
      });
    return () => {
      ignore = true;
    };
  }, []);

  function handleStartCreate() {
    if (loggedIn) {
      router.push("/canvas");
      return;
    }
    openModal("email", "/canvas", "required");
  }

  return (
    <main className="overflow-hidden bg-[#070809] text-[#f6f3ed]">
      <section className="relative min-h-[680px] px-5 pb-20 pt-24 sm:px-8 lg:min-h-[760px] lg:pt-32">
        <div className="absolute inset-0 -z-0">
          <img
            src="/www-home/assets/images/hero-cinema.webp"
            alt=""
            className="h-full w-full object-cover object-center opacity-95"
          />
          <div className="absolute inset-0 bg-[linear-gradient(90deg,rgba(7,8,9,0.96),rgba(7,8,9,0.65)_34%,rgba(7,8,9,0.16)_62%,rgba(7,8,9,0.86)),linear-gradient(180deg,rgba(7,8,9,0.72),rgba(7,8,9,0.1)_45%,#070809_100%)]" />
        </div>

        <div className="relative z-10 mx-auto flex min-h-[520px] max-w-[1420px] flex-col justify-center">
          <p className="mb-5 text-xs font-bold uppercase tracking-[0.12em] text-[#e7b65d]">AI Image & Video Studio</p>
          <h1 className="max-w-[720px] text-[clamp(44px,8vw,96px)] font-semibold leading-[0.98] text-[#f6f3ed]">
            把灵感，变成会发光的影像。
          </h1>
          <p className="mt-6 max-w-[620px] text-[17px] leading-8 text-[#d7d2c8]/80 sm:text-xl">
            Copse AI 为创作者、品牌与短片团队提供图像生成、视频成片、风格迁移和画布协作，一站完成从概念到发布的关键步骤。
          </p>
          <div className="mt-9 flex flex-wrap gap-4">
            <button
              type="button"
              onClick={handleStartCreate}
              className="inline-flex min-h-14 items-center justify-center gap-2 rounded-full bg-[linear-gradient(135deg,#61d7d5,#f5da89_48%,#ff6d77)] px-7 text-base font-bold text-[#071011] shadow-[0_18px_54px_rgba(97,215,213,0.2)] transition-transform hover:-translate-y-0.5"
            >
              开始创作
              <ArrowRight className="size-4" />
            </button>
            <Link
              href="#community"
              className="inline-flex min-h-14 items-center justify-center gap-2 rounded-full border border-white/30 bg-white/[0.06] px-7 text-base font-bold text-[#f6f3ed] transition-transform hover:-translate-y-0.5"
            >
              浏览公开作品
            </Link>
          </div>
        </div>

        <div className="relative z-10 mx-auto mt-10 grid max-w-[680px] overflow-hidden rounded-lg border border-white/15 bg-white/10 shadow-[0_28px_90px_rgba(0,0,0,0.44)] backdrop-blur md:ml-auto md:mr-0 md:grid-cols-[1.2fr_1fr_1fr]">
          {[
            ["Seedance 2.0", "电影级运镜"],
            ["4K", "高质资产输出"],
            ["Canvas", "项目化创作"],
          ].map(([value, label]) => (
            <div key={value} className="border-b border-white/10 bg-[#0c0e10]/80 p-5 last:border-b-0 md:border-b-0 md:border-r md:last:border-r-0">
              <strong className="block text-2xl">{value}</strong>
              <span className="mt-1 block text-sm text-[#a6a8ad]">{label}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="relative z-10 mx-auto -mt-10 grid w-[min(900px,calc(100%-40px))] gap-4 pb-20 sm:grid-cols-2">
        <button
          type="button"
          onClick={handleStartCreate}
          className="flex min-h-[76px] items-center justify-center gap-3 rounded-lg border border-white/20 bg-[#141619]/85 px-6 text-xl font-bold shadow-[0_28px_90px_rgba(0,0,0,0.44)] backdrop-blur transition-transform hover:-translate-y-0.5"
        >
          <Wand2 className="size-6 text-[#61d7d5]" />
          开始创作
        </button>
        <button
          type="button"
          onClick={handleStartCreate}
          className="flex min-h-[76px] items-center justify-center gap-3 rounded-lg border border-[#61d7d5]/40 bg-[#141619]/85 px-6 text-xl font-bold shadow-[0_28px_90px_rgba(0,0,0,0.44)] backdrop-blur transition-transform hover:-translate-y-0.5"
        >
          <Play className="size-6 text-[#61d7d5]" />
          快速体验 Seedance 2.0
        </button>
      </section>

      <section className="mx-auto w-[min(1420px,calc(100%-40px))] pb-24" id="gallery">
        <div className="mb-7 flex flex-col justify-between gap-4 md:flex-row md:items-end">
          <div>
            <p className="mb-3 text-xs font-bold uppercase tracking-[0.12em] text-[#e7b65d]">Inspiration</p>
            <h2 className="text-[clamp(32px,4vw,54px)] font-semibold leading-tight">灵感创作</h2>
          </div>
          <div className="inline-flex rounded-full border border-white/15 bg-white/[0.06] p-1 text-sm font-bold">
            <span className="rounded-full bg-[#61d7d5] px-4 py-2 text-[#071011]">全部</span>
            <span className="px-4 py-2 text-[#f6f3ed]/70">特效</span>
            <span className="px-4 py-2 text-[#f6f3ed]/70">风格</span>
          </div>
        </div>

        <div className="grid gap-5 md:grid-cols-4">
          {inspirationWorks.map((work) => (
            <article
              key={work.title}
              className={`group relative min-h-[280px] overflow-hidden rounded-lg border border-white/15 bg-[#111315] shadow-[0_22px_60px_rgba(0,0,0,0.25)] ${work.className}`}
            >
              <img src={work.image} alt={work.title} className="h-full min-h-[280px] w-full object-cover transition-transform duration-300 group-hover:scale-[1.04]" />
              <div className="absolute inset-x-0 bottom-0 flex items-end justify-between gap-4 bg-[linear-gradient(180deg,transparent,rgba(0,0,0,0.86))] px-5 pb-5 pt-28">
                <span className="rounded-full bg-white/20 px-3 py-1 text-xs font-bold backdrop-blur">{work.label}</span>
                <strong className="text-right text-xl">{work.title}</strong>
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className="mx-auto mb-24 grid w-[min(1420px,calc(100%-40px))] gap-8 rounded-lg border border-white/15 bg-[#111315]/90 p-7 md:p-12 lg:grid-cols-[0.85fr_1.15fr]">
        <div>
          <p className="mb-3 text-xs font-bold uppercase tracking-[0.12em] text-[#e7b65d]">Workflow</p>
          <h2 className="text-[clamp(32px,4vw,54px)] font-semibold leading-tight">从提示词到分镜，从素材到成片。</h2>
          <p className="mt-5 text-[17px] leading-8 text-[#a6a8ad]">
            用统一工作台管理参考图、模型参数、镜头版本和资产输出。团队可以围绕同一个创作任务快速迭代。
          </p>
        </div>
        <div className="grid gap-4 md:grid-cols-3">
          {[
            [Sparkles, "01", "灵感输入", "提示词 / 参考图 / 风格"],
            [Clapperboard, "02", "模型生成", "图像 / 视频 / 特效"],
            [Layers3, "03", "资产沉淀", "版本 / 审核 / 发布"],
          ].map(([Icon, step, title, desc]) => (
            <div key={String(step)} className="rounded-lg border border-white/15 bg-white/[0.06] p-5">
              <Icon className="mb-5 size-6 text-[#61d7d5]" />
              <span className="text-xs font-black text-[#e7b65d]">{String(step)}</span>
              <strong className="mt-4 block text-2xl">{String(title)}</strong>
              <small className="mt-2 block text-sm text-[#a6a8ad]">{String(desc)}</small>
            </div>
          ))}
        </div>
      </section>

      <section className="mx-auto w-[min(1420px,calc(100%-40px))] pb-24">
        <div className="mb-7">
          <p className="mb-3 text-xs font-bold uppercase tracking-[0.12em] text-[#e7b65d]">Models</p>
          <h2 className="text-[clamp(32px,4vw,54px)] font-semibold leading-tight">为真实创作场景准备的模型矩阵</h2>
        </div>
        <div className="grid gap-5 md:grid-cols-3">
          {modelCards.map(([label, title, desc]) => (
            <article key={title} className="min-h-[210px] rounded-lg border border-white/15 bg-white/[0.06] p-7">
              <span className="text-xs font-black text-[#e7b65d]">{label}</span>
              <h3 className="mt-5 text-2xl font-semibold">{title}</h3>
              <p className="mt-3 leading-7 text-[#a6a8ad]">{desc}</p>
            </article>
          ))}
        </div>
      </section>

      <section id="community" className="mx-auto w-[min(1420px,calc(100%-40px))] pb-24">
        <div className="mb-7 flex flex-col justify-between gap-4 md:flex-row md:items-end">
          <div>
            <p className="mb-3 text-xs font-bold uppercase tracking-[0.12em] text-[#e7b65d]">Community</p>
            <h2 className="text-[clamp(32px,4vw,54px)] font-semibold leading-tight">公开作品</h2>
          </div>
          <Link href="/community" className="inline-flex items-center gap-2 text-sm font-bold text-[#61d7d5]">
            查看更多
            <ArrowRight className="size-4" />
          </Link>
        </div>

        {communityPosts.length > 0 ? (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {communityPosts.map((post) => (
              <CommunityPostCard key={post.id} post={post} />
            ))}
          </div>
        ) : (
          <div className="grid min-h-[220px] place-items-center rounded-lg border border-dashed border-white/20 bg-white/[0.04] text-sm text-[#a6a8ad]">
            公开作品加载中
          </div>
        )}
      </section>

      <section className="mx-auto mb-24 flex w-[min(1420px,calc(100%-40px))] flex-col justify-between gap-8 rounded-lg border border-[#61d7d5]/30 bg-[linear-gradient(90deg,rgba(97,215,213,0.12),rgba(255,109,119,0.1)),#17191c] p-8 md:flex-row md:items-center md:p-12">
        <div>
          <p className="mb-3 text-xs font-bold uppercase tracking-[0.12em] text-[#e7b65d]">Creator Plan</p>
          <h2 className="max-w-[780px] text-[clamp(30px,4vw,50px)] font-semibold leading-tight">
            让 Copse AI 成为你的日常创作入口。
          </h2>
          <p className="mt-4 max-w-[720px] leading-7 text-[#a6a8ad]">会员权益、创作者挑战赛、作品广场和模型体验都可以继续接入到现有应用。</p>
        </div>
        <button
          type="button"
          onClick={handleStartCreate}
          className="inline-flex min-h-14 shrink-0 items-center justify-center gap-2 rounded-full bg-[linear-gradient(135deg,#61d7d5,#f5da89_48%,#ff6d77)] px-7 text-base font-bold text-[#071011]"
        >
          进入 Copse AI
          <ImageIcon className="size-4" />
        </button>
      </section>
    </main>
  );
}

export default HomePage;
