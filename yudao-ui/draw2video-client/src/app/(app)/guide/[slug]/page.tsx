"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useParams } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { getPublishedGuide } from "@/features/guide/guide-api";
import type { GuideContent } from "@/features/guide/guide-types";

function renderContent(content?: string) {
  if (!content) return <p className="text-sm text-muted-gray">该指南暂无正文。</p>;
  return content.split(/\n{2,}/).map((paragraph, index) => {
    const text = paragraph.trim();
    if (!text) return null;
    if (text.startsWith("#")) {
      return <h2 key={index} className="mt-6 text-lg font-semibold text-charcoal">{text.replace(/^#+\s*/, "")}</h2>;
    }
    if (/^!\[.*?\]\(.+?\)$/.test(text)) {
      const match = text.match(/^!\[(.*?)\]\((.+?)\)$/);
      return match ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img key={index} src={match[2]} alt={match[1]} className="mt-4 max-h-[520px] rounded-lg border border-border-warm object-contain" />
      ) : null;
    }
    return <p key={index} className="text-sm leading-7 text-charcoal">{text}</p>;
  });
}

export default function GuideDetailPage() {
  const params = useParams<{ slug: string }>();
  const slug = decodeURIComponent(params.slug);
  const [guide, setGuide] = useState<GuideContent | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    getPublishedGuide(slug)
      .then((data) => {
        if (!cancelled) setGuide(data);
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err.message : "指南不存在或尚未发布");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [slug]);

  const content = useMemo(() => renderContent(guide?.content), [guide?.content]);

  if (loading) return <div className="flex min-h-full items-center justify-center text-sm text-muted-gray">加载中...</div>;

  if (error || !guide) {
    return (
      <main className="mx-auto max-w-3xl px-4 py-10">
        <Link href="/guide" className="mb-6 inline-flex items-center gap-2 text-sm text-muted-gray hover:text-charcoal">
          <ArrowLeft className="size-4" />
          返回指南
        </Link>
        <div className="rounded-lg border border-dashed border-border-warm py-20 text-center text-sm text-muted-gray">
          {error || "指南不存在或尚未发布"}
        </div>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-3xl px-4 py-10">
      <Link href="/guide" className="mb-6 inline-flex items-center gap-2 text-sm text-muted-gray hover:text-charcoal">
        <ArrowLeft className="size-4" />
        返回指南
      </Link>
      <article>
        <p className="text-xs text-muted-gray">{guide.category}</p>
        <h1 className="mt-2 text-3xl font-semibold text-charcoal">{guide.title}</h1>
        {guide.summary && <p className="mt-3 text-sm leading-6 text-muted-gray">{guide.summary}</p>}
        {guide.coverUrl && (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={guide.coverUrl} alt={guide.title} className="mt-6 max-h-[420px] w-full rounded-lg border border-border-warm object-cover" />
        )}
        <div className="mt-8 space-y-4">{content}</div>
      </article>
    </main>
  );
}

