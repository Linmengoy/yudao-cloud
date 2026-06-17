/* eslint-disable @next/next/no-img-element */
"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { Copy, Loader2, RefreshCw, Search, Sparkles } from "lucide-react";
import { MasonryWall } from "@/components/MasonryWall";
import { MediaPreviewDialog } from "@/features/media-preview/MediaPreviewDialog";
import type { MediaPreviewItem } from "@/features/media-preview/types";
import {
  copyPromptTemplate,
  getPromptTemplateCategories,
  getPromptTemplatePage,
} from "@/features/templates/template-api";
import type { PromptTemplate } from "@/features/templates/template-types";
import { formatFileSize } from "@/features/assets/asset-dictionaries";
import { cn } from "@/lib/utils";

const PAGE_SIZE = 30;
const MIN_TEMPLATE_COLUMN_WIDTH = 230;
const IMAGE_PRELOAD_TIMEOUT_MS = 8000;
const TEMPLATE_SKELETON_HEIGHTS = [300, 240, 360, 280, 330, 260, 380, 250, 320, 290, 350, 270];

function createRandomSeed() {
  return Math.floor(Math.random() * 1_000_000_000);
}

function parseJsonArray(value?: string) {
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed.map(String) : [];
  } catch {
    return [];
  }
}

function getTemplateKey(template: PromptTemplate) {
  return String(template.templateNo || template.id);
}

function getTemplateSpan(template: PromptTemplate, measuredRatio?: number) {
  const ratio = measuredRatio || (template.width && template.height ? template.width / template.height : 1);
  return ratio >= 1.45 ? 2 : 1;
}

function preloadImage(src: string) {
  return new Promise<void>((resolve) => {
    const image = new Image();
    const timer = window.setTimeout(resolve, IMAGE_PRELOAD_TIMEOUT_MS);
    const finish = () => {
      window.clearTimeout(timer);
      resolve();
    };
    image.onload = finish;
    image.onerror = finish;
    image.src = src;
  });
}

function preloadTemplateImages(templates: PromptTemplate[]) {
  const urls = templates.map((template) => template.imageUrl).filter(Boolean) as string[];
  return Promise.all(urls.map(preloadImage));
}

function templateToPreviewItem(template: PromptTemplate): MediaPreviewItem | null {
  if (!template.imageUrl) return null;
  return {
    kind: "image",
    url: template.imageUrl,
    title: template.title,
    prompt: template.prompt,
    fileName: template.title,
    createdAt: template.createTime,
    information: [
      { label: "分类", value: template.category || "-" },
      { label: "模型", value: template.modelName || template.modelCode || "-" },
      { label: "风格", value: parseJsonArray(template.styles).join(", ") || "-" },
      { label: "场景", value: parseJsonArray(template.scenes).join(", ") || "-" },
      { label: "来源", value: template.sourceLabel || "-" },
      { label: "尺寸", value: template.width && template.height ? `${template.width} x ${template.height}` : "-" },
      { label: "文件大小", value: formatFileSize(template.fileSize) },
      { label: "复制次数", value: String(template.copyCount ?? 0) },
      { label: "复用次数", value: String(template.useCount ?? 0) },
    ],
  };
}

function TemplateCard({
  template,
  onPreview,
  onReuse,
  onImageLoad,
  reuseLabel,
  reuseTitle,
  featuredLabel,
}: {
  template: PromptTemplate;
  onPreview: (template: PromptTemplate) => void;
  onReuse: (template: PromptTemplate) => void;
  onImageLoad: (ratio?: number) => void;
  reuseLabel: string;
  reuseTitle: string;
  featuredLabel: string;
}) {
  const styles = parseJsonArray(template.styles).slice(0, 3);
  const promptPreview = template.promptPreview || template.prompt;

  return (
    <article className="group overflow-hidden rounded-xl border border-border-warm bg-background text-left transition-colors hover:border-[rgba(28,28,28,0.4)]">
      <div className="relative bg-muted">
        <button
          type="button"
          onClick={() => onPreview(template)}
          className="block w-full text-left focus:outline-none focus:shadow-[rgba(0,0,0,0.1)_0px_4px_12px]"
          aria-label={`预览模板 ${template.title}`}
        >
          {template.imageUrl ? (
            <img
              src={template.imageUrl}
              alt={template.title}
              className="block h-auto w-full"
              loading="lazy"
              decoding="async"
              onLoad={(event) => onImageLoad(event.currentTarget.naturalWidth / event.currentTarget.naturalHeight)}
            />
          ) : (
            <div className="flex min-h-[220px] items-center justify-center text-sm text-muted-gray">暂无图片</div>
          )}
        </button>
        <button
          type="button"
          onClick={(event) => {
            event.stopPropagation();
            onReuse(template);
          }}
          className="absolute right-3 top-3 inline-flex items-center gap-1.5 rounded-full bg-background/92 px-3 py-1.5 text-xs text-charcoal opacity-0 shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] backdrop-blur transition-opacity hover:bg-background group-hover:opacity-100 focus:opacity-100 focus:outline-none focus:shadow-[rgba(0,0,0,0.1)_0px_4px_12px]"
          aria-label={`复用模板 ${template.title}`}
          title={reuseTitle}
        >
          <Sparkles className="size-3.5" />
          {reuseLabel}
        </button>
      </div>
      <button
        type="button"
        onClick={() => onPreview(template)}
        className="block w-full px-3.5 py-3 text-left focus:outline-none focus:shadow-[rgba(0,0,0,0.1)_0px_4px_12px]"
      >
        <div className="flex items-start justify-between gap-3">
          <h2 className="line-clamp-2 text-sm font-medium leading-5 text-charcoal">{template.title}</h2>
          {template.featured && (
            <span className="shrink-0 rounded-full border border-border-warm px-2 py-0.5 text-[11px] text-muted-gray">
              {featuredLabel}
            </span>
          )}
        </div>
        <p className="mt-1.5 line-clamp-3 text-xs leading-5 text-muted-gray">{promptPreview}</p>
        <div className="mt-3 flex flex-wrap gap-1.5">
          {(template.modelName || template.modelCode) && (
            <span className="rounded-full bg-charcoal px-2 py-1 text-[11px] text-off-white">
              {template.modelName || template.modelCode}
            </span>
          )}
          {template.category && (
            <span className="rounded-full bg-muted px-2 py-1 text-[11px] text-muted-gray">{template.category}</span>
          )}
          {styles.map((style) => (
            <span key={style} className="rounded-full bg-muted px-2 py-1 text-[11px] text-muted-gray">
              {style}
            </span>
          ))}
        </div>
      </button>
    </article>
  );
}

function EmptyState() {
  const t = useTranslations("templates.empty");
  return (
    <div className="mt-8 flex flex-col items-center rounded-2xl border border-dashed border-border-warm bg-background/70 px-6 py-16 text-center">
      <p className="text-sm font-medium text-charcoal">{t("title")}</p>
      <p className="mt-1 text-xs text-muted-gray">{t("hint")}</p>
    </div>
  );
}

function TemplateGridSkeleton() {
  return (
    <div className="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
      {TEMPLATE_SKELETON_HEIGHTS.map((height, index) => (
        <div key={index} className="overflow-hidden rounded-xl border border-border-warm bg-background">
          <div className="animate-pulse bg-muted" style={{ height }} />
          <div className="space-y-2 p-3.5">
            <div className="h-4 w-3/4 animate-pulse rounded bg-muted" />
            <div className="h-3 w-full animate-pulse rounded bg-muted" />
            <div className="h-3 w-2/3 animate-pulse rounded bg-muted" />
            <div className="flex gap-1.5 pt-1">
              <div className="h-5 w-16 animate-pulse rounded-full bg-muted" />
              <div className="h-5 w-20 animate-pulse rounded-full bg-muted" />
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

export default function TemplatesPage() {
  const t = useTranslations("templates");
  const commonT = useTranslations("common");
  const router = useRouter();
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [category, setCategory] = useState("");
  const [categories, setCategories] = useState<string[]>([]);
  const [templates, setTemplates] = useState<PromptTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");
  const [total, setTotal] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [pageCursor, setPageCursor] = useState(1);
  const [measuredRatios, setMeasuredRatios] = useState<Record<string, number>>({});
  const [previewTemplateId, setPreviewTemplateId] = useState<number | null>(null);
  const loadMoreElementRef = useRef<HTMLDivElement | null>(null);
  const loadingPageRef = useRef(false);
  const pageNoRef = useRef(1);
  const hasMoreRef = useRef(true);
  const templatesLengthRef = useRef(0);
  const categoryKeywordFallbackRef = useRef(false);
  const randomSeedRef = useRef(createRandomSeed());
  const resetRequestRef = useRef(0);

  const loadTemplates = useCallback(async (reset = false) => {
    if (loadingPageRef.current && !reset) return;
    if (!reset && !hasMoreRef.current) return;
    const resetRequest = reset ? resetRequestRef.current + 1 : resetRequestRef.current;
    if (reset) resetRequestRef.current = resetRequest;
    loadingPageRef.current = true;
    const nextPageNo = reset ? 1 : pageNoRef.current;
    setLoading(reset);
    setLoadingMore(!reset);
    setError("");
    if (reset) {
      randomSeedRef.current = createRandomSeed();
      pageNoRef.current = 1;
      hasMoreRef.current = true;
      templatesLengthRef.current = 0;
      categoryKeywordFallbackRef.current = false;
      setTemplates([]);
      setMeasuredRatios({});
      setTotal(0);
      setHasMore(true);
      setPageCursor(1);
    }
    try {
      const keyword = debouncedQuery.trim() || undefined;
      const data = await getPromptTemplatePage({
        pageNo: nextPageNo,
        pageSize: PAGE_SIZE,
        keyword: categoryKeywordFallbackRef.current ? category : keyword,
        category: categoryKeywordFallbackRef.current ? undefined : category || undefined,
        randomSeed: randomSeedRef.current,
      });
      let nextList = data.list ?? [];
      let nextTotal = data.total ?? 0;
      if (reset && category && !keyword && nextList.length === 0) {
        const fallbackData = await getPromptTemplatePage({
          pageNo: 1,
          pageSize: PAGE_SIZE,
          keyword: category,
          randomSeed: randomSeedRef.current,
        });
        nextList = fallbackData.list ?? [];
        nextTotal = fallbackData.total ?? 0;
        categoryKeywordFallbackRef.current = nextList.length > 0;
      }
      const nextPageCursor = nextPageNo + 1;
      const mergedLength = (reset ? 0 : templatesLengthRef.current) + nextList.length;
      const nextHasMore = nextList.length > 0 && mergedLength < nextTotal;
      if (reset) await preloadTemplateImages(nextList);
      if (reset && resetRequest !== resetRequestRef.current) return;
      setTemplates((items) => reset ? nextList : [...items, ...nextList]);
      setTotal(nextTotal);
      pageNoRef.current = nextPageCursor;
      templatesLengthRef.current = mergedLength;
      hasMoreRef.current = nextHasMore;
      setPageCursor(nextPageCursor);
      setHasMore(nextHasMore);
    } catch (err) {
      if (reset && resetRequest !== resetRequestRef.current) return;
      setError(err instanceof Error ? err.message : t("loading"));
      hasMoreRef.current = false;
      setHasMore(false);
    } finally {
      if (!reset || resetRequest === resetRequestRef.current) {
        setLoading(false);
        setLoadingMore(false);
        loadingPageRef.current = false;
      }
    }
  }, [category, debouncedQuery, t]);

  useEffect(() => {
    getPromptTemplateCategories().then(setCategories).catch(() => setCategories([]));
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedQuery(query), 350);
    return () => window.clearTimeout(timer);
  }, [query]);

  useEffect(() => {
    const timer = window.setTimeout(() => loadTemplates(true), 0);
    return () => window.clearTimeout(timer);
  }, [loadTemplates]);

  useEffect(() => {
    const element = loadMoreElementRef.current;
    if (!element || loading || loadingMore || !hasMore) return;
    const observer = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) loadTemplates(false);
    }, { rootMargin: "600px 0px" });
    observer.observe(element);
    return () => observer.disconnect();
  }, [hasMore, loadTemplates, loading, loadingMore]);

  const previewTemplate = useMemo(
    () => templates.find((template) => template.id === previewTemplateId) ?? null,
    [previewTemplateId, templates]
  );

  const previewItem = useMemo(() => previewTemplate ? templateToPreviewItem(previewTemplate) : null, [previewTemplate]);

  async function handleReuse(template: PromptTemplate) {
    router.push(`/canvas?templateId=${template.id}`);
  }

  async function handlePreview(template: PromptTemplate) {
    setPreviewTemplateId(template.id);
  }

  async function handleCopyFromPreview() {
    if (!previewTemplate?.prompt) return;
    await navigator.clipboard.writeText(previewTemplate.prompt);
    await copyPromptTemplate(previewTemplate.id);
  }

  return (
    <main className="mx-auto flex max-w-[1180px] flex-col px-6 py-10">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-charcoal">{t("title")}</h1>
          <p className="mt-1 text-sm text-muted-gray">{t("subtitle")}</p>
        </div>
        <button
          type="button"
          onClick={() => loadTemplates(true)}
          disabled={loading}
          className="inline-flex items-center gap-2 rounded-md border border-[rgba(28,28,28,0.4)] px-3 py-2 text-sm text-charcoal hover:bg-muted active:opacity-80 disabled:opacity-50"
          aria-label={commonT("refresh")}
          title={commonT("refresh")}
        >
          <RefreshCw className="size-4" />
          {commonT("refresh")}
        </button>
      </div>

      <div className="mt-8 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex w-full max-w-[420px] items-center gap-2 rounded-md border border-border-warm bg-background px-3 py-2">
          <Search className="size-4 text-muted-gray" />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder={t("searchPlaceholder")}
            className="w-full bg-transparent text-sm text-charcoal placeholder:text-muted-gray focus:outline-none"
          />
        </div>

        <div className="flex gap-2 overflow-x-auto pb-1 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden lg:max-w-[620px]">
          <button
            type="button"
            onClick={() => setCategory("")}
            className={cn(
              "shrink-0 rounded-full px-3 py-2 text-sm transition-colors",
              category === "" ? "bg-charcoal text-off-white" : "bg-muted text-muted-gray hover:text-charcoal"
            )}
          >
            {commonT("all")}
          </button>
          {categories.map((item) => (
            <button
              key={item}
              type="button"
              onClick={() => setCategory(item)}
              className={cn(
                "shrink-0 rounded-full px-3 py-2 text-sm transition-colors",
                category === item ? "bg-charcoal text-off-white" : "bg-muted text-muted-gray hover:text-charcoal"
              )}
            >
              {item}
            </button>
          ))}
        </div>
      </div>

      <div className="mt-5 flex items-center justify-between text-xs text-muted-gray">
        <span>{total > 0 ? t("count", { total }) : t("title")}</span>
        <span>{pageCursor > 1 && templates.length > 0 ? t("loaded", { count: templates.length }) : ""}</span>
      </div>

      {error && <div className="mt-4 rounded-lg border border-border-warm bg-background px-4 py-3 text-sm text-muted-gray">{error}</div>}

      {loading && !templates.length ? (
        <TemplateGridSkeleton />
      ) : templates.length === 0 ? (
        <EmptyState />
      ) : (
        <>
          <MasonryWall
            items={templates}
            getKey={getTemplateKey}
            getSpan={(template) => getTemplateSpan(template, measuredRatios[getTemplateKey(template)])}
            minColumnWidth={MIN_TEMPLATE_COLUMN_WIDTH}
            className="relative mt-6 -m-0.5"
            renderItem={(template) => (
              <TemplateCard
                template={template}
                onPreview={handlePreview}
                onReuse={handleReuse}
                onImageLoad={(ratio) => {
                  if (ratio && Number.isFinite(ratio)) {
                    const key = getTemplateKey(template);
                    setMeasuredRatios((values) => values[key] === ratio ? values : { ...values, [key]: ratio });
                  }
                }}
                reuseLabel={t("reuse")}
                reuseTitle={t("reuseToCanvas")}
                featuredLabel={t("featured")}
              />
            )}
          />
          <div ref={loadMoreElementRef} className="mt-8 flex justify-center py-4 text-xs text-muted-gray">
            {loadingMore ? (
              <span className="inline-flex items-center gap-2"><Loader2 className="size-3.5 animate-spin" />{t("loadMore")}</span>
            ) : hasMore ? (
              <span>{t("scrollMore")}</span>
            ) : total > 0 ? (
              <span>{t("allLoaded")}</span>
            ) : null}
          </div>
        </>
      )}

      <MediaPreviewDialog
        item={previewItem}
        open={previewTemplateId != null}
        onClose={() => setPreviewTemplateId(null)}
        footerActions={previewTemplate ? (
          <div className="flex gap-2">
            <button
              type="button"
              onClick={handleCopyFromPreview}
              className="flex flex-1 items-center justify-center gap-2 rounded-md border border-border-warm bg-background px-4 py-2.5 text-sm font-medium text-charcoal shadow-[rgba(0,0,0,0.05)_0px_1px_2px_0px] transition-colors hover:bg-muted active:opacity-80"
            >
              <Copy className="size-4" />
              {t("copyPrompt")}
            </button>
            <button
              type="button"
              onClick={() => handleReuse(previewTemplate)}
              className="flex flex-1 items-center justify-center gap-2 rounded-md bg-charcoal px-4 py-2.5 text-sm font-medium text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] active:opacity-80"
            >
              <Sparkles className="size-4" />
              {t("reuseTemplate")}
            </button>
          </div>
        ) : undefined}
      />
    </main>
  );
}
