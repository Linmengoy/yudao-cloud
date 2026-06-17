"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { BookOpen, Search } from "lucide-react";
import { getPublishedGuides } from "@/features/guide/guide-api";
import type { GuideContent } from "@/features/guide/guide-types";

function groupByCategory(guides: GuideContent[]) {
  return guides.reduce<Record<string, GuideContent[]>>((groups, guide) => {
    const category = guide.category || "基础功能";
    groups[category] = [...(groups[category] ?? []), guide];
    return groups;
  }, {});
}

export default function GuidePage() {
  const [guides, setGuides] = useState<GuideContent[]>([]);
  const [activeCategory, setActiveCategory] = useState("");
  const [keyword, setKeyword] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    getPublishedGuides()
      .then((data) => {
        if (cancelled) return;
        setGuides(data);
      })
      .catch((err) => {
        if (cancelled) return;
        setError(err instanceof Error ? err.message : "使用指南加载失败");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const categories = useMemo(() => Array.from(new Set(guides.map((guide) => guide.category || "基础功能"))), [guides]);

  const filteredGuides = useMemo(() => {
    const text = keyword.trim().toLowerCase();
    return guides.filter((guide) => {
      const matchesCategory = !activeCategory || (guide.category || "基础功能") === activeCategory;
      const matchesKeyword =
        !text ||
        `${guide.title} ${guide.summary ?? ""} ${guide.category ?? ""} ${guide.content ?? ""}`.toLowerCase().includes(text);
      return matchesCategory && matchesKeyword;
    });
  }, [activeCategory, guides, keyword]);

  const groupedGuides = useMemo(() => groupByCategory(filteredGuides), [filteredGuides]);

  return (
    <main className="mx-auto flex max-w-6xl flex-col px-4 py-8 md:px-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-charcoal">使用指南</h1>
          <p className="mt-1 text-sm text-muted-gray">按业务模块浏览已发布的操作说明。</p>
        </div>
        <div className="relative w-full md:w-80">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-gray" />
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="搜索指南标题、模块或正文"
            className="h-10 w-full rounded-lg border border-border-warm bg-background pl-9 pr-3 text-sm outline-none focus:border-[rgba(28,28,28,0.45)]"
          />
        </div>
      </div>

      <div className="mt-6 flex gap-2 overflow-x-auto pb-1">
        <button
          type="button"
          onClick={() => setActiveCategory("")}
          className={`shrink-0 rounded-full px-3 py-2 text-sm ${activeCategory === "" ? "bg-charcoal text-off-white" : "bg-muted text-muted-gray hover:text-charcoal"}`}
        >
          全部模块
        </button>
        {categories.map((category) => (
          <button
            key={category}
            type="button"
            onClick={() => setActiveCategory(category)}
            className={`shrink-0 rounded-full px-3 py-2 text-sm ${activeCategory === category ? "bg-charcoal text-off-white" : "bg-muted text-muted-gray hover:text-charcoal"}`}
          >
            {category}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="py-20 text-center text-sm text-muted-gray">加载中...</div>
      ) : error ? (
        <div className="mt-8 rounded-lg border border-dashed border-border-warm py-20 text-center text-sm text-muted-gray">{error}</div>
      ) : filteredGuides.length === 0 ? (
        <div className="mt-8 rounded-lg border border-dashed border-border-warm py-20 text-center text-sm text-muted-gray">
          当前模块暂无已发布指南
        </div>
      ) : (
        <div className="mt-8 space-y-8">
          {Object.entries(groupedGuides).map(([category, items]) => (
            <section key={category}>
              <h2 className="mb-3 text-base font-medium text-charcoal">{category}</h2>
              <div className="grid gap-3 md:grid-cols-2">
                {items.map((guide) => (
                  <Link
                    key={guide.slug}
                    href={`/guide/${guide.slug}`}
                    className="rounded-lg border border-border-warm bg-background p-4 transition-colors hover:border-[rgba(28,28,28,0.35)]"
                  >
                    <div className="mb-3 flex size-9 items-center justify-center rounded-lg bg-muted text-charcoal">
                      <BookOpen className="size-4" />
                    </div>
                    <h3 className="line-clamp-1 text-sm font-medium text-charcoal">{guide.title}</h3>
                    <p className="mt-1 line-clamp-2 min-h-10 text-xs leading-5 text-muted-gray">
                      {guide.summary || "查看该模块的操作步骤、图片和注意事项。"}
                    </p>
                    <p className="mt-3 text-xs text-muted-gray">{guide.updateTime || guide.publishTime || ""}</p>
                  </Link>
                ))}
              </div>
            </section>
          ))}
        </div>
      )}
    </main>
  );
}

