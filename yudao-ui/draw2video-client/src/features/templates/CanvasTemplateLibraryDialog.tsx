"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { AnimatePresence, motion } from "motion/react";
import { ChevronDown, Loader2, Search, X } from "lucide-react";
import { getPromptTemplateModels, getPromptTemplatePage } from "./template-api";
import type { PromptTemplate, PromptTemplateModel } from "./template-types";

const PAGE_SIZE = 24;
const LOAD_MORE_THRESHOLD = 320;

type CanvasTemplateLibraryDialogProps = {
  open: boolean;
  onClose: () => void;
  onSelect: (template: PromptTemplate) => void;
};

function getModelLabel(model: PromptTemplateModel) {
  return model.modelName || model.modelCode;
}

function getTemplateModelLabel(template: PromptTemplate, models: PromptTemplateModel[], selectedModel: PromptTemplateModel | null) {
  if (template.modelName) return template.modelName;
  if (template.modelCode) {
    return models.find((model) => model.modelCode === template.modelCode)?.modelName || template.modelCode;
  }
  return selectedModel?.modelName || selectedModel?.modelCode || "";
}

export function CanvasTemplateLibraryDialog({ open, onClose, onSelect }: CanvasTemplateLibraryDialogProps) {
  const [keyword, setKeyword] = useState("");
  const [modelCode, setModelCode] = useState("");
  const [models, setModels] = useState<PromptTemplateModel[]>([]);
  const [items, setItems] = useState<PromptTemplate[]>([]);
  const [pageNo, setPageNo] = useState(1);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");

  const hasMore = items.length < total;
  const selectedModel = useMemo(
    () => models.find((model) => model.modelCode === modelCode) ?? null,
    [modelCode, models]
  );

  const loadPage = useCallback(async (nextPageNo: number, replace: boolean) => {
    if (!open) return;
    if (replace) {
      setLoading(true);
    } else {
      setLoadingMore(true);
    }
    setError("");
    try {
      const page = await getPromptTemplatePage({
        pageNo: nextPageNo,
        pageSize: PAGE_SIZE,
        keyword: keyword.trim() || undefined,
        modelCode: modelCode || undefined,
      });
      setItems((current) => replace ? page.list : [...current, ...page.list]);
      setTotal(page.total);
      setPageNo(nextPageNo);
    } catch (err) {
      setError(err instanceof Error ? err.message : "素材库加载失败");
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  }, [keyword, modelCode, open]);

  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    getPromptTemplateModels()
      .then((data) => {
        if (!cancelled) setModels(data);
      })
      .catch(() => {
        if (!cancelled) setModels([]);
      });
    return () => {
      cancelled = true;
    };
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const timer = window.setTimeout(() => {
      void loadPage(1, true);
    }, 180);
    return () => window.clearTimeout(timer);
  }, [loadPage, open]);

  useEffect(() => {
    if (!open) return;
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") onClose();
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onClose, open]);

  const handleSelect = useCallback((template: PromptTemplate) => {
    onSelect(template);
    onClose();
  }, [onClose, onSelect]);

  const handleScroll = useCallback((event: React.UIEvent<HTMLElement>) => {
    const target = event.currentTarget;
    const distanceToBottom = target.scrollHeight - target.scrollTop - target.clientHeight;
    if (distanceToBottom > LOAD_MORE_THRESHOLD) return;
    if (!hasMore || loading || loadingMore || error) return;
    void loadPage(pageNo + 1, false);
  }, [error, hasMore, loadPage, loading, loadingMore, pageNo]);

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 z-[220] flex items-center justify-center bg-charcoal/30 px-4 py-5"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <motion.div
            role="dialog"
            aria-modal="true"
            aria-label="素材库"
            initial={{ opacity: 0, y: 12, scale: 0.985 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 12, scale: 0.985 }}
            transition={{ duration: 0.16, ease: "easeOut" }}
            className="flex h-[min(82vh,760px)] w-[min(1080px,96vw)] flex-col overflow-hidden rounded-[28px] border border-border-warm bg-background shadow-[0_24px_80px_rgba(28,28,28,0.22)]"
          >
            <header className="flex shrink-0 items-center justify-between gap-4 border-b border-border-warm px-6 py-4">
              <div>
                <h2 className="text-lg font-semibold text-charcoal">素材库</h2>
                <p className="text-sm text-muted-gray">选择一个素材，在画布上创建图片节点</p>
              </div>
              <div className="flex items-center gap-2">
                <label className="relative hidden sm:block">
                  <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-gray" />
                  <input
                    value={keyword}
                    onChange={(event) => setKeyword(event.target.value)}
                    placeholder="搜索标题或提示词"
                    className="h-10 w-[240px] rounded-full border border-border-warm bg-muted/40 pl-9 pr-3 text-sm text-charcoal outline-none transition-colors placeholder:text-muted-gray focus:border-charcoal"
                  />
                </label>
                <label className="relative">
                  <select
                    value={modelCode}
                    onChange={(event) => setModelCode(event.target.value)}
                    className="h-10 min-w-[156px] appearance-none rounded-full border border-border-warm bg-muted/40 pl-4 pr-9 text-sm text-charcoal outline-none transition-colors focus:border-charcoal"
                    aria-label="按模型筛选"
                  >
                    <option value="">全部模型</option>
                    {models.map((model) => (
                      <option key={model.modelCode} value={model.modelCode}>
                        {getModelLabel(model)}
                      </option>
                    ))}
                  </select>
                  <ChevronDown className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted-gray" />
                </label>
                <button
                  type="button"
                  onClick={onClose}
                  className="flex size-10 items-center justify-center rounded-full text-charcoal transition-colors hover:bg-muted"
                  aria-label="关闭素材库"
                  title="关闭"
                >
                  <X className="size-5" />
                </button>
              </div>
            </header>

            <div className="border-b border-border-warm px-6 py-3 sm:hidden">
              <label className="relative block">
                <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-gray" />
                <input
                  value={keyword}
                  onChange={(event) => setKeyword(event.target.value)}
                  placeholder="搜索标题或提示词"
                  className="h-10 w-full rounded-full border border-border-warm bg-muted/40 pl-9 pr-3 text-sm text-charcoal outline-none transition-colors placeholder:text-muted-gray focus:border-charcoal"
                />
              </label>
            </div>

            <main className="min-h-0 flex-1 overflow-y-auto px-6 py-5" onScroll={handleScroll}>
              {error ? (
                <div className="flex h-full items-center justify-center text-sm text-red-600">{error}</div>
              ) : loading ? (
                <div className="flex h-full items-center justify-center gap-2 text-sm text-muted-gray">
                  <Loader2 className="size-4 animate-spin" />
                  加载素材中
                </div>
              ) : items.length === 0 ? (
                <div className="flex h-full items-center justify-center text-sm text-muted-gray">
                  未找到匹配的素材
                </div>
              ) : (
                <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
                  {items.map((template) => {
                    const modelLabel = getTemplateModelLabel(template, models, selectedModel);
                    return (
                    <button
                      key={template.id}
                      type="button"
                      onClick={() => handleSelect(template)}
                      className="group overflow-hidden rounded-2xl border border-border-warm bg-background text-left transition-all hover:-translate-y-0.5 hover:border-charcoal/35 hover:shadow-[0_12px_28px_rgba(28,28,28,0.12)] focus:outline-none focus:ring-2 focus:ring-charcoal/25"
                    >
                      <div className="aspect-[4/3] overflow-hidden bg-muted">
                        {template.imageUrl ? (
                          <img
                            src={template.imageUrl}
                            alt={template.title}
                            className="size-full object-cover transition-transform duration-200 group-hover:scale-[1.03]"
                            draggable={false}
                          />
                        ) : (
                          <div className="flex size-full items-center justify-center text-xs text-muted-gray">无预览</div>
                        )}
                      </div>
                      <div className="space-y-2 p-3">
                        <div className="flex items-center gap-2">
                          <span className="min-w-0 flex-1 truncate text-sm font-medium text-charcoal">{template.title}</span>
                          {template.featured ? (
                            <span className="shrink-0 rounded-full bg-charcoal px-2 py-0.5 text-[10px] text-off-white">推荐</span>
                          ) : null}
                        </div>
                        <p className="line-clamp-2 min-h-[32px] text-xs leading-4 text-muted-gray">
                          {template.promptPreview || template.prompt}
                        </p>
                        <div className="flex items-center justify-between gap-2 text-[11px] text-muted-gray">
                          {modelLabel ? <span className="truncate">{modelLabel}</span> : <span />}
                          {template.category ? <span className="shrink-0 truncate">{template.category}</span> : null}
                        </div>
                      </div>
                    </button>
                    );
                  })}
                  {loadingMore ? (
                    <div className="col-span-full flex items-center justify-center gap-2 py-4 text-sm text-muted-gray">
                      <Loader2 className="size-4 animate-spin" />
                      加载更多素材
                    </div>
                  ) : null}
                </div>
              )}
            </main>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
