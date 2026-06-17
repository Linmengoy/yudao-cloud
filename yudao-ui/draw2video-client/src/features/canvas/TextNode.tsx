"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { Node, NodeProps } from "@xyflow/react";
import { useReactFlow, useStore } from "@xyflow/react";
import { AnimatePresence, motion } from "motion/react";
import { ArrowUp, Check, ChevronDown, Loader2, RotateCcw, Sparkles, Text, X } from "lucide-react";
import type { AppEdge, AppNode, ImageNodeData, NodeDataPatchEventDetail, NodeEditingPresenceEventDetail, SketchNodeData, TextNodeData } from "./types";
import { NodeCreateHandle } from "./NodeCreateHandle";
import { generationApi } from "@/features/generation/generation-api";
import { waitGenerationResult } from "@/features/generation/generation-poll";
import { canvasNodeRunApi, isServerCanvasProjectId } from "@/features/canvas/canvas-node-run-api";
import { useAigcModels } from "@/features/generation/use-aigc-models";
import { cn } from "@/lib/utils";
import { EditableNodeTitle } from "./EditableNodeTitle";
import { CanvasNodeTitle } from "./CanvasNodeTitle";
import { parseMarkdownPreview, type MarkdownPreviewBlock } from "./markdown-preview";
import { createPromptMentionToken, getComposerUiScale, PromptMentionInput, promptValueToSubmitPrompt, useComposerWheelPan, type PromptMentionOption } from "./PromptMentionInput";

type TextNodeProps = NodeProps<Node<TextNodeData, "text">>;

const MIN_WIDTH = 220;
const MIN_HEIGHT = 160;
const DEFAULT_MODEL = "Gemini 3.1 Flash Lite";
const COMPOSER_WIDTH = 620;
const TEXT_MODEL_TYPE = 1;
const TEXT_CAPABILITY = "TEXT_GENERATE";

function formatCost(value: number | null | undefined, currency = "积分") {
  if (value == null || !Number.isFinite(value)) return "--";
  return `${Number(value).toLocaleString("zh-CN", { maximumFractionDigits: 2 })} ${currency}`;
}

function renderMarkdownBlock(block: MarkdownPreviewBlock, index: number) {
  if (block.type === "heading") {
    const className = block.level === 1 ? "text-base font-semibold" : block.level === 2 ? "text-sm font-semibold" : "text-[13px] font-semibold";
    const HeadingTag = `h${block.level}` as "h1" | "h2" | "h3";
    return <HeadingTag key={index} className={className}>{block.text}</HeadingTag>;
  }
  if (block.type === "quote") {
    return <blockquote key={index} className="border-l-2 border-border-warm pl-3 text-muted-gray">{block.text}</blockquote>;
  }
  if (block.type === "list") {
    const ListTag = block.ordered ? "ol" : "ul";
    return (
      <ListTag key={index} className={cn("space-y-1 pl-5", block.ordered ? "list-decimal" : "list-disc")}>
        {block.items.map((item, itemIndex) => <li key={itemIndex}>{item}</li>)}
      </ListTag>
    );
  }
  if (block.type === "code") {
    return <pre key={index} className="overflow-auto rounded-lg bg-charcoal/10 p-3 text-[12px] leading-5"><code>{block.text}</code></pre>;
  }
  return <p key={index} className="whitespace-pre-wrap">{block.text}</p>;
}

function MarkdownPreview({ value, className }: { value: string; className?: string }) {
  const blocks = useMemo(() => parseMarkdownPreview(value), [value]);
  if (!value.trim()) return <p className={cn("text-muted-gray", className)}>暂无内容</p>;
  return (
    <div className={cn("space-y-3 text-sm leading-6 text-charcoal", className)}>
      {blocks.map(renderMarkdownBlock)}
    </div>
  );
}

export function TextNodeComponent({ id, data, selected, dragging }: TextNodeProps) {
  const { setNodes, getNodes, getEdges, getViewport, setViewport } = useReactFlow();
  const composerWheelRef = useComposerWheelPan<HTMLDivElement>(getViewport, setViewport);
  const zoom = useStore((s) => s.transform[2] || 1);
  const selectedNodeCount = useStore((s) => s.nodes.filter((node) => node.selected).length);
  const referenceImagesSignature = useStore((s) => (s.edges as AppEdge[])
    .filter((edge) => edge.target === id)
    .map((edge) => {
      const node = (s.nodes as AppNode[]).find((item) => item.id === edge.source);
      if (node?.type !== "image" && node?.type !== "sketch") return null;
      const nodeData = node.data as ImageNodeData | SketchNodeData;
      return [edge.id, edge.source, nodeData.previewUrl ?? "", nodeData.dataUrl ? nodeData.dataUrl.length : 0, nodeData.fileName].join(":");
    })
    .filter(Boolean)
    .join("|"));
  const [editing, setEditing] = useState(false);
  const [draftContent, setDraftContent] = useState(data.content);
  const [candidateContent, setCandidateContent] = useState<string | null>(null);
  const [candidateDraft, setCandidateDraft] = useState("");
  const [candidateMode, setCandidateMode] = useState<"preview" | "markdown">("preview");
  const [modelPopoverOpen, setModelPopoverOpen] = useState(false);
  const [resizing, setResizing] = useState(false);
  const resizeStartRef = useRef<{
    x: number;
    y: number;
    width: number;
    height: number;
  } | null>(null);
  const modelBtnRef = useRef<HTMLButtonElement | null>(null);
  const modelPopoverRef = useRef<HTMLDivElement | null>(null);

  const isGenerating = data.status === "pending";
  const isOnlySelectedNode = selected && selectedNodeCount === 1;
  const showNodeActions = selectedNodeCount <= 1;
  const fixedUiScale = getComposerUiScale(zoom);
  const selectedAigcModelId = typeof data.aigcModelId === "number" ? data.aigcModelId : null;
  const textModelParams = useMemo(() => ({}), []);
  const aigcModels = useAigcModels({
    type: TEXT_MODEL_TYPE,
    capability: TEXT_CAPABILITY,
    preferredModelId: selectedAigcModelId,
    params: textModelParams,
  });
  const activeAigcModel = aigcModels.models.find((item) => item.id === selectedAigcModelId) ?? aigcModels.selectedModel;
  const activeAigcModelId = activeAigcModel?.id ?? selectedAigcModelId;
  const activeModelName = activeAigcModel?.name ?? data.modelName ?? data.modelId ?? DEFAULT_MODEL;
  const costLabel = aigcModels.priceLoading ? "..." : formatCost(aigcModels.price?.salePrice, aigcModels.price?.currencyType || "积分");
  const canGenerate = Boolean(data.prompt.trim()) && !isGenerating && !aigcModels.loading && !aigcModels.templateLoading && Boolean(activeAigcModelId);
  const mentionOptions = useMemo<PromptMentionOption[]>(() => {
    void referenceImagesSignature;
    const currentNodes = getNodes() as AppNode[];
    const currentEdges = getEdges() as AppEdge[];
    const images: PromptMentionOption[] = [];
    for (const edge of currentEdges) {
      if (edge.target !== id) continue;
      const node = currentNodes.find((item) => item.id === edge.source);
      if (node?.type !== "image" && node?.type !== "sketch") continue;
      const nodeData = node.data as ImageNodeData | SketchNodeData;
      images.push({
        id: node.id,
        label: `图片 ${images.length + 1}`,
        token: createPromptMentionToken(node.id),
        thumbnailUrl: nodeData.previewUrl || nodeData.dataUrl,
      });
    }
    return images;
  }, [getEdges, getNodes, id, referenceImagesSignature]);

  const sendEditingPresence = useCallback((nodeId: string | null) => {
    window.dispatchEvent(new CustomEvent<NodeEditingPresenceEventDetail>("copse:node-editing-presence", {
      detail: { nodeId },
    }));
  }, []);

  const updateData = useCallback(
    (patch: Partial<TextNodeData>, options?: { flush?: boolean }) => {
      setNodes((nds) =>
        nds.map((node) =>
          node.id === id ? { ...node, data: { ...node.data, ...patch } } : node
        )
      );
      window.dispatchEvent(new CustomEvent<NodeDataPatchEventDetail>("copse:node-data-patch", {
        detail: { nodeId: id, patch, flush: options?.flush },
      }));
    },
    [id, setNodes]
  );

  useEffect(() => {
    if (aigcModels.loading || aigcModels.models.length === 0) return;
    if (selectedAigcModelId) return;
    const nextModel = aigcModels.selectedModel ?? aigcModels.models[0];
    if (!nextModel) return;
    updateData({
      modelId: String(nextModel.id),
      providerModel: nextModel.providerModel ?? nextModel.model,
      modelName: nextModel.name,
      aigcModelId: nextModel.id,
    }, { flush: true });
  }, [aigcModels.loading, aigcModels.models, aigcModels.selectedModel, selectedAigcModelId, updateData]);

  useEffect(() => {
    if (!modelPopoverOpen) return;
    function handlePointerDown(event: PointerEvent) {
      const target = event.target as HTMLElement;
      if (modelPopoverRef.current?.contains(target) || modelBtnRef.current?.contains(target)) return;
      setModelPopoverOpen(false);
    }
    window.addEventListener("pointerdown", handlePointerDown);
    return () => window.removeEventListener("pointerdown", handlePointerDown);
  }, [modelPopoverOpen]);

  const commitContent = useCallback(() => {
    setEditing(false);
    sendEditingPresence(null);
    updateData({ content: draftContent, updatedAt: new Date().toISOString() }, { flush: true });
  }, [draftContent, sendEditingPresence, updateData]);

  const handleAigcModelSelect = useCallback((nextModelId: number) => {
    const model = aigcModels.models.find((item) => item.id === nextModelId);
    if (!model) return;
    aigcModels.setSelectedModelId(nextModelId);
    updateData({
      modelId: String(nextModelId),
      providerModel: model.providerModel ?? model.model,
      modelName: model.name,
      aigcModelId: model.id,
    }, { flush: true });
    setModelPopoverOpen(false);
  }, [aigcModels, updateData]);

  const acceptCandidate = useCallback(() => {
    const content = candidateDraft.trim();
    updateData({
      content,
      status: "idle",
      errorMessage: null,
      updatedAt: new Date().toISOString(),
    }, { flush: true });
    setCandidateContent(null);
    setCandidateDraft("");
    setCandidateMode("preview");
  }, [candidateDraft, updateData]);

  const cancelCandidate = useCallback(() => {
    setCandidateContent(null);
    setCandidateDraft("");
    setCandidateMode("preview");
  }, []);

  const handleGenerate = useCallback(async () => {
    const prompt = promptValueToSubmitPrompt(data.prompt, mentionOptions).trim();
    if (!prompt || isGenerating || aigcModels.loading || aigcModels.templateLoading) return;

    if (!activeAigcModelId) {
      updateData({ status: "failed", errorMessage: "请选择 AIGC 文本模型后再生成。" });
      return;
    }

    const startedAt = new Date().toISOString();
    setCandidateContent(null);
    setCandidateDraft("");
    setCandidateMode("preview");
    updateData({ status: "pending", taskId: null, errorMessage: null, generationStartedAt: startedAt, generationCompletedAt: null, elapsedMs: null });

    const params = new URLSearchParams(window.location.search);
    const projectId = params.get("projectId");
    const clientId = `node_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
    if (isServerCanvasProjectId(projectId)) {
      try {
        const run = await canvasNodeRunApi.runNode(projectId, id, {
          clientId,
          baseVersion: 0,
          runId: clientId,
          nodeType: "text",
          generateType: "TEXT",
          generateMode: "TEXT_GENERATE",
          modelId: activeAigcModelId,
          prompt,
          inputParams: JSON.stringify({ previousContent: data.content }),
          sync: false,
        });
        updateData({ taskId: String(run.taskId), taskStatus: run.status }, { flush: true });
        return;
      } catch (error) {
        updateData({
          status: "failed",
          taskId: null,
          errorMessage: error instanceof Error ? error.message : "文本任务提交失败",
          generationCompletedAt: new Date().toISOString(),
          elapsedMs: Date.now() - new Date(startedAt).getTime(),
        });
        return;
      }
    }

    try {
      const submit = await generationApi.generateText({
        modelId: activeAigcModelId,
        prompt,
        inputParams: JSON.stringify({ previousContent: data.content }),
        sync: false,
      });
      updateData({ taskId: String(submit.taskId), taskStatus: submit.status }, { flush: true });
      const result = await waitGenerationResult(submit.taskId);
      const completedAt = result.finishTime ?? new Date().toISOString();

      if (result.status === "SUCCESS") {
        const outputText = result.outputText ?? String(result.outputDataValue ?? "");
        setCandidateContent(outputText);
        setCandidateDraft(outputText);
        updateData({
          status: "idle",
          taskId: String(submit.taskId),
          errorMessage: null,
          generationCompletedAt: completedAt,
          elapsedMs: Date.now() - new Date(startedAt).getTime(),
        });
        return;
      }

      updateData({
        status: "failed",
        taskId: String(submit.taskId),
        errorMessage: result.failMessage ?? "文本生成失败，请稍后重试。",
        generationCompletedAt: completedAt,
        elapsedMs: Date.now() - new Date(startedAt).getTime(),
      });
    } catch (error) {
      updateData({
        status: "failed",
        taskId: null,
        errorMessage: error instanceof Error ? error.message : "文本生成失败，请稍后重试。",
        generationCompletedAt: new Date().toISOString(),
        elapsedMs: Date.now() - new Date(startedAt).getTime(),
      });
    }
  }, [activeAigcModelId, aigcModels.loading, aigcModels.templateLoading, data.content, data.prompt, id, isGenerating, mentionOptions, updateData]);

  useEffect(() => {
    if (!resizing) return;

    function handlePointerMove(event: PointerEvent) {
      const start = resizeStartRef.current;
      if (!start) return;
      updateData({
        width: Math.max(MIN_WIDTH, Math.round(start.width + (event.clientX - start.x) / zoom)),
        height: Math.max(MIN_HEIGHT, Math.round(start.height + (event.clientY - start.y) / zoom)),
      });
    }

    function handlePointerUp() {
      setResizing(false);
      resizeStartRef.current = null;
    }

    window.addEventListener("pointermove", handlePointerMove);
    window.addEventListener("pointerup", handlePointerUp);
    return () => {
      window.removeEventListener("pointermove", handlePointerMove);
      window.removeEventListener("pointerup", handlePointerUp);
    };
  }, [resizing, updateData, zoom]);

  return (
    <div className="relative" style={{ width: data.width, height: data.height }}>
      <CanvasNodeTitle maxWidth={data.width}>
        <Text className="size-4" />
        <EditableNodeTitle
          value={data.fileName}
          fallback="Text"
          onCommit={(fileName) => updateData({ fileName, updatedAt: new Date().toISOString() }, { flush: true })}
        />
      </CanvasNodeTitle>

      <motion.div
        data-node-preview-card
        data-node-id={id}
        initial={{ opacity: 0, scale: 0.97 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.18, ease: "easeOut" }}
        className={cn(
          "canvas-node-drag-handle group relative rounded-xl border bg-background shadow-[0_1px_3px_rgba(0,0,0,0.06)]",
          selected ? "border-charcoal/60 ring-2 ring-charcoal/35" : "border-border-warm"
        )}
        style={{ width: data.width, height: data.height, pointerEvents: "auto" }}
        onDoubleClick={(event) => {
          event.stopPropagation();
          setDraftContent(data.content);
          setEditing(true);
          sendEditingPresence(id);
        }}
      >
        {editing ? (
          <textarea
            value={draftContent}
            autoFocus
            onChange={(event) => setDraftContent(event.target.value)}
            onBlur={commitContent}
            onKeyDown={(event) => {
              if ((event.metaKey || event.ctrlKey) && event.key === "Enter") {
                commitContent();
              }
              if (event.key === "Escape") {
                setEditing(false);
                sendEditingPresence(null);
                setDraftContent(data.content);
              }
            }}
            className="nodrag nowheel size-full resize-none rounded-xl bg-transparent p-4 text-sm leading-6 text-charcoal outline-none"
          />
        ) : data.content ? (
          <div className="h-full overflow-hidden p-4">
            <MarkdownPreview value={data.content} />
          </div>
        ) : (
          <div className="p-4">
            {isGenerating ? (
              <div className="space-y-3">
                <div className="h-2.5 w-[92%] animate-pulse rounded-full bg-muted-gray/45" />
                <div className="h-2.5 w-[70%] animate-pulse rounded-full bg-muted-gray/45" />
                <div className="h-2.5 w-[32%] animate-pulse rounded-full bg-muted-gray/45" />
              </div>
            ) : (
              <div className="space-y-4">
                <div className="mx-auto mt-5 space-y-2 text-muted-gray/60">
                  <div className="mx-auto h-2 w-16 rounded-full bg-current" />
                  <div className="mx-auto h-2 w-16 rounded-full bg-current" />
                  <div className="mx-auto h-2 w-16 rounded-full bg-current" />
                  <div className="mx-auto h-2 w-10 rounded-full bg-current" />
                </div>
                <div className="pt-5 text-sm text-muted-gray">
                  <p className="mb-3">尝试：</p>
                  <div className="space-y-3 text-charcoal">
                    <button
                      type="button"
                      className="nodrag flex items-center gap-2 text-left transition-colors hover:text-muted-gray"
                      onClick={(event) => {
                        event.stopPropagation();
                        setDraftContent(data.content);
                        setEditing(true);
                        sendEditingPresence(id);
                      }}
                    >
                      <Text className="size-4" />
                      自己编写内容
                    </button>
                    <div className="flex items-center gap-2">
                      <Sparkles className="size-4" />
                      使用下方 Composer 生成
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        <AnimatePresence>
          {isGenerating && data.content && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.16 }}
              className="absolute inset-0 flex items-center justify-center rounded-xl bg-background/65"
            >
              <Loader2 className="size-6 animate-spin text-muted-gray" />
            </motion.div>
          )}
        </AnimatePresence>

        <button
          type="button"
          aria-label="调整文本卡片大小"
          className="nodrag nowheel absolute bottom-1 right-1 size-5 cursor-nwse-resize rounded bg-charcoal/10 hover:bg-charcoal/20"
          onPointerDown={(event) => {
            event.preventDefault();
            event.stopPropagation();
            resizeStartRef.current = {
              x: event.clientX,
              y: event.clientY,
              width: data.width,
              height: data.height,
            };
            setResizing(true);
          }}
        />

        <NodeCreateHandle nodeId={id} direction="incoming" selected={selected} showButton={showNodeActions} />
        <NodeCreateHandle nodeId={id} direction="outgoing" selected={selected} showButton={showNodeActions} />
      </motion.div>

      <AnimatePresence>
        {isOnlySelectedNode && !dragging && (
          <motion.div
            initial={{ opacity: 0, y: -8 * fixedUiScale, scale: 0.99 * fixedUiScale }}
            animate={{ opacity: 1, y: 0, scale: fixedUiScale }}
            exit={{ opacity: 0, y: -8 * fixedUiScale, scale: 0.99 * fixedUiScale }}
            transition={{ duration: 0.18, ease: "easeOut" }}
            ref={composerWheelRef}
            className="nodrag nowheel absolute rounded-xl border border-border-warm bg-background p-4 shadow-[0_8px_24px_rgba(28,28,28,0.08)]"
            style={{
              width: COMPOSER_WIDTH,
              left: (data.width - COMPOSER_WIDTH) / 2,
              top: data.height + 12 * fixedUiScale,
              transformOrigin: "top center",
              pointerEvents: "auto",
            }}
          >
            {candidateContent == null ? (
              <PromptMentionInput
                value={data.prompt}
                onChange={(nextPrompt) => updateData({ prompt: nextPrompt })}
                mentions={mentionOptions}
                disabled={isGenerating}
                placeholder="写下你想生成或优化的文本内容"
                minHeightClassName="min-h-[110px]"
                onSubmit={() => {
                  if (canGenerate) void handleGenerate();
                }}
              />
            ) : (
              <div className="rounded-xl border border-border-warm bg-muted/35 p-3">
                <div className="mb-3 flex items-center justify-between gap-3">
                  <div>
                    <p className="text-sm font-semibold text-charcoal">生成结果预览</p>
                    <p className="text-[11px] text-muted-gray">采用后会覆盖当前文本内容</p>
                  </div>
                  <div className="flex rounded-lg bg-background p-1 text-xs font-medium text-muted-gray">
                    <button
                      type="button"
                      onClick={() => setCandidateMode("preview")}
                      className={cn("rounded-md px-2.5 py-1 transition-colors", candidateMode === "preview" && "bg-charcoal text-off-white")}
                    >
                      预览
                    </button>
                    <button
                      type="button"
                      onClick={() => setCandidateMode("markdown")}
                      className={cn("rounded-md px-2.5 py-1 transition-colors", candidateMode === "markdown" && "bg-charcoal text-off-white")}
                    >
                      Markdown
                    </button>
                  </div>
                </div>
                {candidateMode === "preview" ? (
                  <div className="max-h-[220px] overflow-auto rounded-lg bg-background p-3">
                    <MarkdownPreview value={candidateDraft} />
                  </div>
                ) : (
                  <textarea
                    value={candidateDraft}
                    onChange={(event) => setCandidateDraft(event.target.value)}
                    className="nodrag nowheel min-h-[220px] w-full resize-y rounded-lg bg-background p-3 text-sm leading-6 text-charcoal outline-none"
                  />
                )}
              </div>
            )}
            <div className="mt-4 flex items-center justify-between gap-3 border-t border-border-warm pt-3">
              <div className="relative min-w-0">
                <button
                  ref={modelBtnRef}
                  type="button"
                  disabled={isGenerating || aigcModels.loading || candidateContent != null}
                  onClick={() => setModelPopoverOpen((value) => !value)}
                  className="flex max-w-[280px] items-center gap-2 rounded-xl bg-muted px-3 py-2 text-left text-sm font-medium text-charcoal transition-colors hover:bg-muted/80 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <Sparkles className="size-4 shrink-0 text-muted-gray" />
                  <span className="truncate">{aigcModels.loading ? "模型加载中" : activeModelName}</span>
                  <ChevronDown className={cn("size-4 shrink-0 text-muted-gray transition-transform", modelPopoverOpen && "rotate-180")} />
                </button>
                <AnimatePresence>
                  {modelPopoverOpen && (
                    <motion.div
                      ref={modelPopoverRef}
                      data-composer-local-wheel="true"
                      initial={{ opacity: 0, y: 4, scale: 0.98 }}
                      animate={{ opacity: 1, y: 0, scale: 1 }}
                      exit={{ opacity: 0, y: 4, scale: 0.98 }}
                      transition={{ duration: 0.14, ease: "easeOut" }}
                      className="absolute bottom-full left-0 z-[260] mb-2 max-h-64 w-[320px] overflow-y-auto rounded-xl border border-border-warm bg-background p-1.5 shadow-[0_4px_16px_rgba(0,0,0,0.12)]"
                    >
                      {aigcModels.models.length === 0 ? (
                        <div className="px-3 py-2 text-xs text-muted-gray">暂无可用文本模型</div>
                      ) : aigcModels.models.map((model) => (
                        <button
                          key={model.id}
                          type="button"
                          onClick={() => handleAigcModelSelect(model.id)}
                          className={cn(
                            "mb-1 flex w-full items-start justify-between gap-3 rounded-lg px-3 py-2 text-left transition-colors last:mb-0 hover:bg-muted",
                            activeAigcModelId === model.id && "bg-muted"
                          )}
                        >
                          <span className="min-w-0">
                            <span className="block truncate text-sm font-medium text-charcoal">{model.name}</span>
                            <span className="block truncate text-[11px] text-muted-gray">{model.remark || model.providerName || model.providerModel || model.model}</span>
                          </span>
                          {activeAigcModelId === model.id && <Check className="mt-0.5 size-4 shrink-0 text-charcoal" />}
                        </button>
                      ))}
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
              <div className="flex items-center gap-2">
                {candidateContent != null ? (
                  <>
                    <button
                      type="button"
                      onClick={cancelCandidate}
                      className="flex h-10 items-center gap-2 rounded-full bg-muted px-4 text-sm font-medium text-charcoal transition-colors hover:bg-muted/80"
                    >
                      取消
                    </button>
                    <button
                      type="button"
                      onClick={handleGenerate}
                      disabled={!canGenerate}
                      className="flex h-10 items-center gap-2 rounded-full bg-muted px-4 text-sm font-medium text-charcoal transition-colors hover:bg-muted/80 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      <RotateCcw className="size-4" />
                      重新生成
                    </button>
                    <button
                      type="button"
                      onClick={acceptCandidate}
                      disabled={!candidateDraft.trim()}
                      className="flex h-10 items-center gap-2 rounded-full bg-charcoal px-4 text-sm font-semibold text-off-white transition-opacity active:opacity-85 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      <Check className="size-4" />
                      覆盖内容
                    </button>
                  </>
                ) : (
                  <>
                    {aigcModels.error && <span className="max-w-[160px] truncate text-xs text-destructive">{aigcModels.error}</span>}
                    <button
                      type="button"
                      onClick={handleGenerate}
                      disabled={!canGenerate}
                      className="flex h-11 items-center gap-3 rounded-full bg-charcoal/90 py-1 pl-4 pr-1 text-off-white shadow-[0_4px_12px_rgba(0,0,0,0.18)] transition-opacity active:opacity-85 disabled:cursor-not-allowed disabled:opacity-50"
                      aria-label={isGenerating ? "生成中" : "生成文本"}
                    >
                      <span className="flex items-center gap-2 text-sm font-semibold tabular-nums">
                        <Sparkles className="size-4" />
                        {isGenerating ? "生成中" : costLabel}
                      </span>
                      <span className="flex size-9 items-center justify-center rounded-full bg-off-white text-charcoal shadow-sm">
                        {isGenerating ? <Loader2 className="size-5 animate-spin" /> : <ArrowUp className="size-5" />}
                      </span>
                    </button>
                  </>
                )}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {data.status === "failed" && (
          <motion.div
            initial={{ opacity: 0, y: -4 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -4 }}
            transition={{ duration: 0.14 }}
            className="absolute left-0 flex items-center gap-1 text-xs text-destructive"
            style={{ top: data.height + 8 }}
          >
            <X className="size-3" />
            {data.errorMessage ?? "生成失败"}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
