"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { AnimatePresence, motion } from "motion/react";
import { ChevronDown, Loader2, Search, X } from "lucide-react";
import { getAigcModelList, type AigcModel } from "@/features/generation/model-api";
import { getPromptTemplateModels, getPromptTemplatePage } from "./template-api";
import type { PromptTemplate, PromptTemplateModel } from "./template-types";

const PAGE_SIZE = 24;
const LOAD_MORE_THRESHOLD = 320;

function createRandomSeed() {
  return Math.floor(Math.random() * 1_000_000_000);
}

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

function normalizeModelText(value: string | undefined | null) {
  return (value ?? "").trim().toLowerCase();
}

function resolveTemplateAigcModel(template: PromptTemplate, aigcModels: AigcModel[]) {
  const modelCode = normalizeModelText(template.modelCode);
  const modelName = normalizeModelText(template.modelName);
  if (!modelCode && !modelName) return null;

  return aigcModels.find((model) => {
    const fields = [
      model.code,
      model.model,
      model.providerModel,
      model.name,
    ].map(normalizeModelText).filter(Boolean);

    return fields.some((field) => (
      field === modelCode ||
      field === modelName ||
      Boolean(modelCode && field.includes(modelCode)) ||
      Boolean(modelName && field.includes(modelName))
    ));
  }) ?? null;
}

export function CanvasTemplateLibraryDialog({ open, onClose, onSelect }: CanvasTemplateLibraryDialogProps) {
  const [keyword, setKeyword] = useState("");
  const [modelCode, setModelCode] = useState("");
  const [modelMenuOpen, setModelMenuOpen] = useState(false);
  const [models, setModels] = useState<PromptTemplateModel[]>([]);
  const [aigcModels, setAigcModels] = useState<AigcModel[]>([]);
  const [items, setItems] = useState<PromptTemplate[]>([]);
  const [pageNo, setPageNo] = useState(1);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");
  const randomSeedRef = useRef(createRandomSeed());

  const hasMore = items.length < total;
  const selectedModel = useMemo(
    () => models.find((model) => model.modelCode === modelCode) ?? null,
    [modelCode, models]
  );
  const selectedModelLabel = selectedModel ? getModelLabel(selectedModel) : "全部模型";

  const loadPage = useCallback(async (nextPageNo: number, replace: boolean) => {
    if (!open) return;
    if (replace) {
      randomSeedRef.current = createRandomSeed();
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
        randomSeed: randomSeedRef.current,
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
    Promise.all([
      getPromptTemplateModels().catch(() => []),
      getAigcModelList(2, "TEXT_TO_IMAGE").catch(() => []),
    ])
      .then(([templateModels, imageModels]) => {
        if (cancelled) return;
        setModels(templateModels);
        setAigcModels(imageModels);
      })
      .catch(() => {
        if (!cancelled) {
          setModels([]);
          setAigcModels([]);
        }
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
    const matchedModel = resolveTemplateAigcModel(template, aigcModels);
    onSelect({
      ...template,
      aigcModelId: matchedModel?.id,
      modelCode: matchedModel?.code ?? template.modelCode,
      modelName: matchedModel?.name ?? template.modelName,
      providerModel: matchedModel?.providerModel ?? matchedModel?.model,
    });
    setModelMenuOpen(false);
    onClose();
  }, [aigcModels, onClose, onSelect]);

  const handleScroll = useCallback((event: React.UIEvent<HTMLElement>) => {
    if (modelMenuOpen) setModelMenuOpen(false);
    const target = event.currentTarget;
    const distanceToBottom = target.scrollHeight - target.scrollTop - target.clientHeight;
    if (distanceToBottom > LOAD_MORE_THRESHOLD) return;
    if (!hasMore || loading || loadingMore || error) return;
    void loadPage(pageNo + 1, false);
  }, [error, hasMore, loadPage, loading, loadingMore, modelMenuOpen, pageNo]);

  const selectModelFilter = useCallback((nextModelCode: string) => {
    setModelCode(nextModelCode);
    setModelMenuOpen(false);
  }, []);

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
                    onChange={(event) => {
                      setKeyword(event.target.value);
                      setModelMenuOpen(false);
                    }}
                    placeholder="搜索标题或提示词"
                    className="h-10 w-[240px] rounded-full border border-border-warm bg-muted/40 pl-9 pr-3 text-sm text-charcoal outline-none transition-colors placeholder:text-muted-gray focus:border-charcoal"
                  />
                </label>
                <div className="relative">
                  <button
                    type="button"
                    onClick={() => setModelMenuOpen((value) => !value)}
                    className="flex h-10 min-w-[156px] items-center justify-between gap-3 rounded-full border border-border-warm bg-muted/40 pl-4 pr-3 text-left text-sm text-charcoal outline-none transition-colors hover:bg-muted focus:border-charcoal"
                    aria-haspopup="listbox"
                    aria-expanded={modelMenuOpen}
                    aria-label="按模型筛选"
                  >
                    <span className="max-w-[168px] truncate">{selectedModelLabel}</span>
                    <ChevronDown className={`size-4 shrink-0 text-muted-gray transition-transform ${modelMenuOpen ? "rotate-180" : ""}`} />
                  </button>
                  <AnimatePresence>
                    {modelMenuOpen ? (
                      <motion.div
                        initial={{ opacity: 0, y: -4, scale: 0.98 }}
                        animate={{ opacity: 1, y: 0, scale: 1 }}
                        exit={{ opacity: 0, y: -4, scale: 0.98 }}
                        transition={{ duration: 0.12, ease: "easeOut" }}
                        className="absolute right-0 top-12 z-[240] w-[220px] overflow-hidden rounded-2xl border border-border-warm bg-background p-1 shadow-[0_14px_36px_rgba(28,28,28,0.18)]"
                        role="listbox"
                      >
                        <button
                          type="button"
                          onClick={() => selectModelFilter("")}
                          className={`flex w-full items-center justify-between rounded-xl px-3 py-2 text-left text-sm transition-colors ${modelCode ? "text-muted-gray hover:bg-muted hover:text-charcoal" : "bg-charcoal text-off-white"}`}
                          role="option"
                          aria-selected={!modelCode}
                        >
                          <span>全部模型</span>
                          {!modelCode ? <span className="text-xs">✓</span> : null}
                        </button>
                        {models.map((model) => {
                          const isSelected = model.modelCode === modelCode;
                          return (
                            <button
                              key={model.modelCode}
                              type="button"
                              onClick={() => selectModelFilter(model.modelCode)}
                              className={`flex w-full items-center justify-between gap-3 rounded-xl px-3 py-2 text-left text-sm transition-colors ${isSelected ? "bg-charcoal text-off-white" : "text-muted-gray hover:bg-muted hover:text-charcoal"}`}
                              role="option"
                              aria-selected={isSelected}
                            >
                              <span className="truncate">{getModelLabel(model)}</span>
                              {isSelected ? <span className="text-xs">✓</span> : null}
                            </button>
                          );
                        })}
                      </motion.div>
                    ) : null}
                  </AnimatePresence>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    setModelMenuOpen(false);
                    onClose();
                  }}
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
                  onChange={(event) => {
                    setKeyword(event.target.value);
                    setModelMenuOpen(false);
                  }}
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
