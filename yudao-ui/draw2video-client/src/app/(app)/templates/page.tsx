/* eslint-disable @next/next/no-img-element */
"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
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
}: {
  template: PromptTemplate;
  onPreview: (template: PromptTemplate) => void;
  onReuse: (template: PromptTemplate) => void;
  onImageLoad: (ratio?: number) => void;
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
          title="复用到画布"
        >
          <Sparkles className="size-3.5" />
          复用
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
              精选
            </span>
          )}
        </div>
        <p className="mt-1.5 line-clamp-3 text-xs leading-5 text-muted-gray">{promptPreview}</p>
        <div className="mt-3 flex flex-wrap gap-1.5">
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
  return (
    <div className="mt-8 flex flex-col items-center rounded-2xl border border-dashed border-border-warm bg-background/70 px-6 py-16 text-center">
      <p className="text-sm font-medium text-charcoal">没有找到匹配模板</p>
      <p className="mt-1 text-xs text-muted-gray">换一个关键词或分类试试。</p>
    </div>
  );
}

export default function TemplatesPage() {
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

  const loadTemplates = useCallback(async (reset = false) => {
    if (loadingPageRef.current) return;
    if (!reset && !hasMoreRef.current) return;
    loadingPageRef.current = true;
    const nextPageNo = reset ? 1 : pageNoRef.current;
    setLoading(reset);
    setLoadingMore(!reset);
    setError("");
    if (reset) {
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
      });
      let nextList = data.list ?? [];
      let nextTotal = data.total ?? 0;
      if (reset && category && !keyword && nextList.length === 0) {
        const fallbackData = await getPromptTemplatePage({
          pageNo: 1,
          pageSize: PAGE_SIZE,
          keyword: category,
        });
        nextList = fallbackData.list ?? [];
        nextTotal = fallbackData.total ?? 0;
        categoryKeywordFallbackRef.current = nextList.length > 0;
      }
      const nextPageCursor = nextPageNo + 1;
      const mergedLength = (reset ? 0 : templatesLengthRef.current) + nextList.length;
      const nextHasMore = nextList.length > 0 && mergedLength < nextTotal;
      setTemplates((items) => reset ? nextList : [...items, ...nextList]);
      setTotal(nextTotal);
      pageNoRef.current = nextPageCursor;
      templatesLengthRef.current = mergedLength;
      hasMoreRef.current = nextHasMore;
      setPageCursor(nextPageCursor);
      setHasMore(nextHasMore);
    } catch (err) {
      setError(err instanceof Error ? err.message : "模板库加载失败");
    } finally {
      setLoading(false);
      setLoadingMore(false);
      loadingPageRef.current = false;
    }
  }, [category, debouncedQuery]);

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
          <h1 className="text-2xl font-semibold tracking-tight text-charcoal">模板库</h1>
          <p className="mt-1 text-sm text-muted-gray">搜索可复用的提示词和生成结果，快速带入画布继续创作。</p>
        </div>
        <button
          type="button"
          onClick={() => loadTemplates(true)}
          disabled={loading}
          className="inline-flex items-center gap-2 rounded-md border border-[rgba(28,28,28,0.4)] px-3 py-2 text-sm text-charcoal hover:bg-muted active:opacity-80 disabled:opacity-50"
          aria-label="刷新模板列表"
          title="刷新模板列表"
        >
          <RefreshCw className="size-4" />
          刷新
        </button>
      </div>

      <div className="mt-8 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex w-full max-w-[420px] items-center gap-2 rounded-md border border-border-warm bg-background px-3 py-2">
          <Search className="size-4 text-muted-gray" />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="搜索提示词、标题或标签"
            className="w-full bg-transparent text-sm text-charcoal placeholder:text-muted-gray focus:outline-none"
          />
        </div>

        <div className="flex gap-2 overflow-x-auto pb-1 lg:max-w-[620px]">
          <button
            type="button"
            onClick={() => setCategory("")}
            className={cn(
              "shrink-0 rounded-full px-3 py-2 text-sm transition-colors",
              category === "" ? "bg-charcoal text-off-white" : "bg-muted text-muted-gray hover:text-charcoal"
            )}
          >
            全部
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
        <span>{total > 0 ? `共 ${total} 个模板` : "模板库"}</span>
        <span>{pageCursor > 1 && templates.length > 0 ? `已加载 ${templates.length}` : ""}</span>
      </div>

      {error && <div className="mt-4 rounded-lg border border-border-warm bg-background px-4 py-3 text-sm text-muted-gray">{error}</div>}

      {loading && !templates.length ? (
        <div className="mt-16 flex items-center justify-center text-sm text-muted-gray">
          <Loader2 className="mr-2 size-4 animate-spin" />
          加载模板库...
        </div>
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
              />
            )}
          />
          <div ref={loadMoreElementRef} className="mt-8 flex justify-center py-4 text-xs text-muted-gray">
            {loadingMore ? (
              <span className="inline-flex items-center gap-2"><Loader2 className="size-3.5 animate-spin" />加载更多...</span>
            ) : hasMore ? (
              <span>继续下滑加载更多</span>
            ) : total > 0 ? (
              <span>已加载全部</span>
            ) : null}
          </div>
        </>
      )}

      <MediaPreviewDialog
        item={previewItem}
        open={previewTemplateId != null}
        onClose={() => setPreviewTemplateId(null)}
      />
      {previewTemplate && previewTemplateId != null && (
        <div className="pointer-events-none fixed bottom-6 right-6 z-[410] hidden gap-2 lg:flex">
          <button
            type="button"
            onClick={handleCopyFromPreview}
            className="pointer-events-auto inline-flex items-center gap-2 rounded-md border border-border-warm bg-background px-4 py-2 text-sm text-charcoal shadow-[rgba(0,0,0,0.05)_0px_1px_2px_0px] hover:bg-muted active:opacity-80"
          >
            <Copy className="size-4" />
            复制提示词
          </button>
          <button
            type="button"
            onClick={() => handleReuse(previewTemplate)}
            className="pointer-events-auto inline-flex items-center gap-2 rounded-md bg-charcoal px-4 py-2 text-sm text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] active:opacity-80"
          >
            <Sparkles className="size-4" />
            复用到画布
          </button>
        </div>
      )}
    </main>
  );
}
