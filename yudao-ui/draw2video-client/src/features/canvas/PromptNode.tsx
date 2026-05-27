/* eslint-disable @next/next/no-img-element */
"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import type { NodeProps, Node } from "@xyflow/react";
import { Handle, Position, useReactFlow, useStore } from "@xyflow/react";
import {
  ArrowUp,
  Check,
  Clock,
  GripVertical,
  Loader2,
  Plus,
  SlidersHorizontal,
  Sparkles,
  X,
} from "lucide-react";
import type {
  AppEdge,
  AppNode,
  ImageNodeData,
  PromptNodeData,
  ReferencePickerEventDetail,
} from "./types";
import { DEFAULT_PROMPT_DATA } from "./types";
import { createGenerationTask, createResultDraft } from "./use-generation";
import { DEFAULT_MODEL_ID, getModelById, IMAGE_MODELS } from "@/features/image-generation/models";
import type { ImageModeration, ImageOutputFormat, ImageQuality } from "@/features/image-generation/types";
import { ParamSelect } from "@/features/image-generation/ParamSelect";
import { SizePickerModal } from "@/features/image-generation/SizePickerModal";
import { cn } from "@/lib/utils";
import { clampToViewport } from "./floating-position";

type PromptNodeProps = NodeProps<Node<PromptNodeData, "prompt">>;

const QUALITY_OPTIONS = [
  { label: "Auto", value: "auto" as ImageQuality },
  { label: "Low", value: "low" as ImageQuality },
  { label: "Medium", value: "medium" as ImageQuality },
  { label: "High", value: "high" as ImageQuality },
];

const FORMAT_OPTIONS = [
  { label: "PNG", value: "png" as ImageOutputFormat },
  { label: "JPEG", value: "jpeg" as ImageOutputFormat },
  { label: "WebP", value: "webp" as ImageOutputFormat },
];

const MODERATION_OPTIONS = [
  { label: "Auto", value: "auto" as ImageModeration },
  { label: "Low", value: "low" as ImageModeration },
];

function formatSizeSummary(size: string) {
  if (size === "auto") return "auto";
  const [w, h] = size.split("x").map(Number);
  if (!w || !h) return size;
  if (w === h) return "1:1";
  return `${w}:${h}`;
}

export function PromptNodeComponent({ id, data, selected }: PromptNodeProps) {
  const params = data.params ?? DEFAULT_PROMPT_DATA.params;
  const modelId = data.modelId ?? DEFAULT_MODEL_ID;
  const isLocked = Boolean(data.isGenerating);
  const isActive = Boolean(selected);
  const { setNodes, setEdges, getNode, getNodes, getEdges } = useReactFlow();

  const [modelPopoverOpen, setModelPopoverOpen] = useState(false);
  const [paramsPopoverOpen, setParamsPopoverOpen] = useState(false);
  const [sizeModalOpen, setSizeModalOpen] = useState(false);
  const [referencePickerPromptId, setReferencePickerPromptId] = useState<string | null>(null);
  const modelBtnRef = useRef<HTMLButtonElement>(null);
  const paramsBtnRef = useRef<HTMLButtonElement>(null);
  const modelPopoverRef = useRef<HTMLDivElement>(null);
  const paramsPopoverRef = useRef<HTMLDivElement>(null);
  const [modelPopoverPos, setModelPopoverPos] = useState<{ x: number; y: number } | null>(null);
  const [paramsPopoverPos, setParamsPopoverPos] = useState<{ x: number; y: number } | null>(null);

  const storeNodes = useStore((s) => s.nodes) as AppNode[];
  const storeEdges = useStore((s) => s.edges) as AppEdge[];
  const currentModel = getModelById(modelId) ?? getModelById(DEFAULT_MODEL_ID)!;
  const pickerActiveForThisNode = referencePickerPromptId === id;

  const connectedImages = storeEdges
    .filter((edge) => edge.target === id)
    .map((edge) => {
      const node = storeNodes.find((n) => n.id === edge.source);
      return node?.type === "image"
        ? { edgeId: edge.id, data: node.data as ImageNodeData }
        : null;
    })
    .filter((item): item is { edgeId: string; data: ImageNodeData } => item !== null);

  useEffect(() => {
    function handleReferencePicker(e: Event) {
      const detail = (e as CustomEvent<ReferencePickerEventDetail>).detail;
      setReferencePickerPromptId(detail?.promptId ?? null);
    }

    window.addEventListener("copse:reference-picker", handleReferencePicker);
    return () => window.removeEventListener("copse:reference-picker", handleReferencePicker);
  }, []);

  useEffect(() => {
    if (!isActive && pickerActiveForThisNode) {
      window.dispatchEvent(new CustomEvent<ReferencePickerEventDetail>("copse:reference-picker", {
        detail: { promptId: null },
      }));
    }
  }, [isActive, pickerActiveForThisNode]);

  const updateData = useCallback(
    (patch: Partial<PromptNodeData>) => {
      setNodes((nds) =>
        nds.map((n) => (n.id === id ? { ...n, data: { ...n.data, ...patch } } : n))
      );
    },
    [id, setNodes]
  );

  const updateParams = useCallback(
    (patch: Record<string, unknown>) => {
      setNodes((nds) =>
        nds.map((n) => {
          if (n.id !== id) return n;
          const d = n.data as PromptNodeData;
          return { ...n, data: { ...d, params: { ...d.params, ...patch } } };
        })
      );
    },
    [id, setNodes]
  );

  const removeImage = useCallback(
    (edgeId: string) => setEdges((eds) => eds.filter((e) => e.id !== edgeId)),
    [setEdges]
  );

  const handleGenerate = useCallback(async () => {
    if (!data.prompt.trim() || data.isGenerating) return;

    const node = getNode(id);
    if (!node) return;

    const ctx = { nodes: getNodes() as AppNode[], edges: getEdges() as AppEdge[] };
    const { newNodes, newEdges } = createResultDraft(id, data, node.position, ctx);
    const resultNode = newNodes[0];
    if (!resultNode || resultNode.type !== "result") return;

    updateData({ isGenerating: true });
    setNodes((nds) => [...nds, ...newNodes]);
    setEdges((eds) => [...eds, ...newEdges]);

    const applyResultPatch = (updates: Partial<typeof resultNode.data>) => {
      setNodes((nds) =>
        nds.map((n) => (n.id === resultNode.id ? { ...n, data: { ...n.data, ...updates } } : n))
      );
    };

    try {
      const updates = await createGenerationTask(data, resultNode.data);
      applyResultPatch(updates);
    } catch (error) {
      applyResultPatch({
        status: "failed",
        imageUrls: [],
        errorMessage: error instanceof Error ? error.message : "生成失败",
        elapsedMs: Date.now() - new Date(resultNode.data.createdAt).getTime(),
        completedAt: new Date().toISOString(),
      });
    }

    updateData({ isGenerating: false });
  }, [id, data, getNode, getNodes, getEdges, setNodes, setEdges, updateData]);

  const handleDelete = useCallback(() => {
    window.dispatchEvent(new CustomEvent<ReferencePickerEventDetail>("copse:reference-picker", {
      detail: { promptId: null },
    }));
    setNodes((nds) => nds.filter((n) => n.id !== id));
    setEdges((eds) => eds.filter((e) => e.source !== id && e.target !== id));
  }, [id, setNodes, setEdges]);

  const handleModelSelect = useCallback(
    (selectedModelId: string) => {
      if (isLocked) return;
      const model = getModelById(selectedModelId);
      if (model?.quality) {
        setNodes((nds) =>
          nds.map((n) => {
            if (n.id !== id) return n;
            const d = n.data as PromptNodeData;
            return {
              ...n,
              data: {
                ...d,
                modelId: selectedModelId,
                params: { ...d.params, quality: model.quality! },
              },
            };
          })
        );
      } else {
        updateData({ modelId: selectedModelId });
      }
      setModelPopoverOpen(false);
    },
    [id, isLocked, setNodes, updateData]
  );

  const handleFormatChange = useCallback(
    (format: ImageOutputFormat) => {
      const patch: Record<string, unknown> = { output_format: format };
      if (format === "png") patch.output_compression = null;
      else if (params.output_compression === null) patch.output_compression = 80;
      updateParams(patch);
    },
    [params.output_compression, updateParams]
  );

  const openReferencePicker = useCallback(() => {
    if (isLocked) return;
    window.dispatchEvent(new CustomEvent<ReferencePickerEventDetail>("copse:reference-picker", {
      detail: { promptId: id },
    }));
  }, [id, isLocked]);

  useEffect(() => {
    if (!modelPopoverOpen && !paramsPopoverOpen) return;

    function handlePointerDown(e: PointerEvent) {
      const target = e.target as HTMLElement;
      if (modelPopoverRef.current?.contains(target) || modelBtnRef.current?.contains(target)) return;
      if (paramsPopoverRef.current?.contains(target) || paramsBtnRef.current?.contains(target)) return;
      setModelPopoverOpen(false);
      setParamsPopoverOpen(false);
    }
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") {
        setModelPopoverOpen(false);
        setParamsPopoverOpen(false);
      }
    }
    function handleCanvasInteraction() {
      setModelPopoverOpen(false);
      setParamsPopoverOpen(false);
    }

    window.addEventListener("pointerdown", handlePointerDown, true);
    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("copse:canvas-interaction", handleCanvasInteraction);
    return () => {
      window.removeEventListener("pointerdown", handlePointerDown, true);
      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("copse:canvas-interaction", handleCanvasInteraction);
    };
  }, [modelPopoverOpen, paramsPopoverOpen]);

  useEffect(() => {
    if (!modelPopoverOpen || !modelBtnRef.current || !modelPopoverRef.current) {
      setModelPopoverPos(null);
      return;
    }
    const btnRect = modelBtnRef.current.getBoundingClientRect();
    const { width, height } = modelPopoverRef.current.getBoundingClientRect();
    setModelPopoverPos(clampToViewport({
      x: btnRect.left,
      y: btnRect.top - height - 10,
      width,
      height,
    }));
  }, [modelPopoverOpen]);

  useEffect(() => {
    if (!paramsPopoverOpen || !paramsBtnRef.current || !paramsPopoverRef.current) {
      setParamsPopoverPos(null);
      return;
    }
    const btnRect = paramsBtnRef.current.getBoundingClientRect();
    const { width, height } = paramsPopoverRef.current.getBoundingClientRect();
    setParamsPopoverPos(clampToViewport({
      x: btnRect.left,
      y: btnRect.top - height - 10,
      width,
      height,
    }));
  }, [paramsPopoverOpen]);

  const summary = `${formatSizeSummary(params.size)} · ${params.quality === "auto" ? "auto" : params.quality}`;
  const compressionDisabled = params.output_format === "png";

  if (!isActive) {
    return (
      <div className="relative w-[320px] rounded-xl border border-border-warm bg-background shadow-[0_1px_3px_rgba(0,0,0,0.06)]">
        <Handle type="target" position={Position.Left} isConnectable={!isLocked} className="!size-3 !border-2 !border-border-warm !bg-background" />
        <div className="flex items-center justify-between border-b border-border-warm px-3 py-2">
          <div className="flex items-center gap-2 text-muted-gray">
            <GripVertical className="size-4" />
            <span className="text-xs font-medium text-charcoal">Prompt</span>
          </div>
        </div>
        <div className="flex min-h-[132px] flex-col justify-between px-4 py-4">
          {isLocked ? (
            <div className="flex flex-1 items-center justify-center rounded-lg bg-muted">
              <Loader2 className="size-6 animate-spin text-muted-gray" />
            </div>
          ) : (
            <p className={cn("line-clamp-3 text-sm leading-6", data.prompt ? "text-charcoal" : "text-muted-gray")}>
              {data.prompt || "描述你想生成的图片..."}
            </p>
          )}
          <div className="mt-4 flex items-center gap-2 text-xs text-muted-gray">
            <Sparkles className="size-3.5" />
            <span className="font-medium text-charcoal">{currentModel.name}</span>
            <span>|</span>
            <span>{summary}</span>
            <span>|</span>
            <span>{params.n}x</span>
          </div>
        </div>
        <Handle type="source" position={Position.Right} isConnectable={!isLocked} className="!size-3 !border-2 !border-border-warm !bg-background" />
      </div>
    );
  }

  return (
    <>
      <div className="relative w-[620px] rounded-xl border border-border-warm bg-background shadow-[0_8px_24px_rgba(28,28,28,0.08)]">
        <Handle type="target" position={Position.Left} isConnectable={!isLocked} className="!size-3 !border-2 !border-border-warm !bg-background" />

        <div className="flex min-h-[320px] flex-col p-4">
          <div className="mb-3 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <button
                type="button"
                className="nodrag nowheel flex size-9 items-center justify-center rounded-lg border border-border-warm bg-muted text-muted-gray"
                aria-label="Prompt 工具"
              >
                <Sparkles className="size-4" />
              </button>
              <button
                type="button"
                onClick={openReferencePicker}
                disabled={isLocked}
                className={cn(
                  "nodrag nowheel flex size-9 items-center justify-center rounded-lg text-muted-gray transition-colors disabled:cursor-not-allowed disabled:opacity-40",
                  pickerActiveForThisNode ? "bg-charcoal text-off-white" : "bg-muted hover:text-charcoal"
                )}
                aria-label="选择参考图"
              >
                <Plus className="size-4" />
              </button>
            </div>
            <button
              type="button"
              onClick={handleDelete}
              disabled={isLocked}
              className="nodrag nowheel flex size-8 items-center justify-center rounded-lg text-muted-gray hover:bg-muted hover:text-charcoal disabled:cursor-not-allowed disabled:opacity-40"
              aria-label="删除 Prompt"
            >
              <X className="size-4" />
            </button>
          </div>

          {connectedImages.length > 0 && (
            <div className="mb-3 flex gap-2 overflow-x-auto pb-1">
              {connectedImages.map((img) => (
                <div key={img.edgeId} className="group relative shrink-0">
                  <div className="size-12 overflow-hidden rounded-lg border border-border-warm bg-muted">
                    <img src={img.data.dataUrl} alt={img.data.fileName} className="size-full object-cover" draggable={false} />
                  </div>
                  <button
                    type="button"
                    onClick={() => removeImage(img.edgeId)}
                    disabled={isLocked}
                    className="nodrag nowheel absolute -right-1.5 -top-1.5 flex size-5 items-center justify-center rounded-full bg-charcoal text-off-white opacity-0 shadow transition-opacity group-hover:opacity-100 disabled:cursor-not-allowed"
                    aria-label="移除参考图"
                  >
                    <X className="size-3" />
                  </button>
                </div>
              ))}
            </div>
          )}

          <div className="relative flex-1">
            <textarea
              value={data.prompt}
              onChange={(e) => updateData({ prompt: e.target.value })}
              disabled={isLocked}
              placeholder="描述你想生成的图片..."
              className="nodrag nowheel h-full min-h-[140px] w-full resize-none bg-transparent text-base leading-7 text-charcoal placeholder:text-muted-gray focus:outline-none disabled:cursor-not-allowed disabled:text-muted-gray"
            />
            {pickerActiveForThisNode && (
              <div className="pointer-events-none absolute inset-0 flex items-center justify-center rounded-lg bg-background/70 text-xs text-muted-gray">
                ESC to exit
              </div>
            )}
          </div>

          <div className="mt-4 flex items-center justify-between gap-3 border-t border-border-warm pt-3">
            <div className="flex min-w-0 items-center gap-2">
              <button
                ref={modelBtnRef}
                type="button"
                disabled={isLocked}
                onClick={() => {
                  setParamsPopoverOpen(false);
                  setModelPopoverOpen((v) => !v);
                }}
                className="nodrag nowheel flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm font-medium text-charcoal hover:bg-muted disabled:cursor-not-allowed disabled:opacity-40"
              >
                <Sparkles className="size-4 text-muted-gray" />
                <span>{currentModel.name}</span>
              </button>
              <span className="h-5 w-px bg-border-warm" />
              <button
                ref={paramsBtnRef}
                type="button"
                disabled={isLocked}
                onClick={() => {
                  setModelPopoverOpen(false);
                  setParamsPopoverOpen((v) => !v);
                }}
                className="nodrag nowheel flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm text-muted-gray hover:bg-muted hover:text-charcoal disabled:cursor-not-allowed disabled:opacity-40"
              >
                <SlidersHorizontal className="size-4" />
                <span>{summary}</span>
              </button>
            </div>

            <div className="flex items-center gap-2">
              <span className="rounded-lg px-2 py-1 text-sm text-muted-gray">{params.n}x</span>
              <button
                type="button"
                onClick={handleGenerate}
                disabled={!data.prompt.trim() || data.isGenerating}
                className="nodrag nowheel flex size-10 items-center justify-center rounded-full bg-charcoal text-off-white shadow-sm transition-opacity active:opacity-80 disabled:cursor-not-allowed disabled:opacity-50"
                aria-label={data.isGenerating ? "生成中" : "开始生成"}
              >
                {data.isGenerating ? <Loader2 className="size-5 animate-spin" /> : <ArrowUp className="size-5" />}
              </button>
            </div>
          </div>
        </div>

        <Handle type="source" position={Position.Right} isConnectable={!isLocked} className="!size-3 !border-2 !border-border-warm !bg-background" />
      </div>

      {sizeModalOpen && (
        <SizePickerModal
          currentSize={params.size}
          onSelect={(size) => updateParams({ size })}
          onClose={() => setSizeModalOpen(false)}
        />
      )}

      {modelPopoverOpen && (
        <div
          ref={modelPopoverRef}
          className="fixed z-[200] w-[340px] rounded-2xl border border-border-warm bg-background shadow-lg"
          style={modelPopoverPos ? { left: modelPopoverPos.x, top: modelPopoverPos.y } : { visibility: "hidden" }}
        >
          <div className="border-b border-border-warm px-4 py-3">
            <p className="text-sm font-medium text-charcoal">模型偏好</p>
          </div>
          <div className="max-h-[280px] overflow-y-auto py-1">
            {IMAGE_MODELS.filter((m) => m.enabled).map((model) => {
              const isSelected = modelId === model.id;
              return (
                <button
                  key={model.id}
                  type="button"
                  onClick={() => handleModelSelect(model.id)}
                  className={cn("nodrag nowheel flex w-full items-start gap-3 px-4 py-3 text-left transition-colors", isSelected ? "bg-muted" : "hover:bg-muted")}
                >
                  <Sparkles className="mt-0.5 size-4 shrink-0 text-muted-gray" />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-medium text-charcoal">{model.name}</span>
                      {model.estimatedSeconds && (
                        <span className="flex items-center gap-0.5 rounded-full border border-border-warm px-1.5 py-0.5 text-[10px] text-muted-gray">
                          <Clock className="size-2.5" />
                          {model.estimatedSeconds}s
                        </span>
                      )}
                    </div>
                    <p className="mt-0.5 text-[11px] text-muted-gray">{model.description}</p>
                  </div>
                  {isSelected && <Check className="mt-1 size-4 shrink-0 text-charcoal" />}
                </button>
              );
            })}
          </div>
        </div>
      )}

      {paramsPopoverOpen && (
        <div
          ref={paramsPopoverRef}
          className="fixed z-[200] w-[360px] rounded-2xl border border-border-warm bg-background p-4 shadow-lg"
          style={paramsPopoverPos ? { left: paramsPopoverPos.x, top: paramsPopoverPos.y } : { visibility: "hidden" }}
        >
          <div className="mb-3 flex items-center justify-between">
            <p className="text-sm font-medium text-charcoal">生成参数</p>
            <button type="button" onClick={() => setParamsPopoverOpen(false)} className="nodrag nowheel flex size-7 items-center justify-center rounded-md text-muted-gray hover:bg-muted" aria-label="关闭参数">
              <X className="size-4" />
            </button>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <button
              type="button"
              onClick={() => setSizeModalOpen(true)}
              className="nodrag nowheel rounded-md border border-border-warm px-2 py-1.5 text-left text-xs text-charcoal hover:border-charcoal/40"
            >
              尺寸：{params.size}
            </button>
            <label className="nodrag nowheel text-xs text-muted-gray">
              数量
              <input
                type="number"
                min={1}
                max={10}
                value={params.n}
                onChange={(e) => updateParams({ n: Math.min(10, Math.max(1, Number(e.target.value) || 1)) })}
                className="mt-1 w-full rounded-md border border-border-warm bg-background px-2 py-1.5 text-charcoal focus:outline-none"
              />
            </label>
            <ParamSelect label="质量" value={params.quality} options={QUALITY_OPTIONS} onChange={(v) => updateParams({ quality: v })} />
            <ParamSelect label="格式" value={params.output_format} options={FORMAT_OPTIONS} onChange={handleFormatChange} />
            <ParamSelect label="审核" value={params.moderation} options={MODERATION_OPTIONS} onChange={(v) => updateParams({ moderation: v })} />
            {!compressionDisabled ? (
              <label className="nodrag nowheel text-xs text-muted-gray">
                压缩率
                <input
                  type="number"
                  min={0}
                  max={100}
                  value={params.output_compression ?? 80}
                  onChange={(e) => updateParams({ output_compression: Number(e.target.value) || null })}
                  className="mt-1 w-full rounded-md border border-border-warm bg-background px-2 py-1.5 text-charcoal focus:outline-none"
                />
              </label>
            ) : (
              <div className="text-xs text-muted-gray">
                压缩率
                <div className="mt-1 rounded-md border border-border-warm bg-muted px-2 py-1.5">—</div>
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
}
