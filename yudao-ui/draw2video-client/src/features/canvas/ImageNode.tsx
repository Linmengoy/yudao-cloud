/* eslint-disable @next/next/no-img-element */
"use client";

import { useCallback, useEffect, useMemo, useRef, useState, type MouseEvent } from "react";
import { createPortal } from "react-dom";
import type { NodeProps, Node } from "@xyflow/react";
import { useReactFlow, useStore, useUpdateNodeInternals } from "@xyflow/react";
import { AnimatePresence, motion } from "motion/react";
import {
  ArrowUp,
  Check,
  Gem,
  ImageIcon,
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
  NodeDataPatchEventDetail,
  PromptNodeData,
  ReferencePickerEventDetail,
  ResultNodeData,
  SketchNodeData,
} from "./types";
import { deleteImage, saveImage } from "./image-store";
import { DEFAULT_PROMPT_DATA } from "./types";
import { NodeCreateHandle } from "./NodeCreateHandle";
import { createGenerationTask, resolveInputImages } from "./use-generation";
import { DEFAULT_MODEL_ID, getModelById, IMAGE_MODELS } from "@/features/image-generation/models";
import type { ImageModeration, ImageOutputFormat, ImageQuality } from "@/features/image-generation/types";
import { calculateImageSize, normalizeImageSize, type SizeTier } from "@/features/image-generation/size";
import { DynamicParamForm } from "@/features/generation/DynamicParamForm";
import type { AigcModelParamTemplate } from "@/features/generation/model-api";
import { useAigcModels } from "@/features/generation/use-aigc-models";
import { canvasNodeRunApi, isServerCanvasProjectId, waitCanvasNodeRunResult } from "@/features/canvas/canvas-node-run-api";
import { getSafetyCopy } from "@/features/safety/safety-copy";
import { SafetyInlineNotice } from "@/features/safety/safety-ui";
import { normalizeSafetyStatus, normalizeSafetyStatusFromError } from "@/features/safety/safety-status";
import { MediaPreviewDialog } from "@/features/media-preview/MediaPreviewDialog";
import { SelectedMediaToolbar } from "@/features/media-preview/SelectedMediaToolbar";
import { downloadMedia, imageNodeToMediaPreview } from "@/features/media-preview/media-preview-utils";
import { cn } from "@/lib/utils";
import { clampToViewport } from "./floating-position";

type ImageNodeProps = NodeProps<Node<ImageNodeData, "image">>;

const PLACEHOLDER_WIDTH = 360;
const PLACEHOLDER_HEIGHT = 260;
const IMAGE_MAX_WIDTH = 420;
const IMAGE_MAX_HEIGHT = 420;
const PREVIEW_SLOT_WIDTH = IMAGE_MAX_WIDTH;
const PREVIEW_SLOT_HEIGHT = IMAGE_MAX_HEIGHT;
const COMPOSER_WIDTH = 620;

type SizeMode = "auto" | "ratio";

const TIERS: SizeTier[] = ["1K", "2K", "4K"];
const RATIOS = ["1:1", "3:2", "2:3", "16:9", "9:16", "4:3", "3:4", "21:9"];

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

const BUILT_IN_IMAGE_PARAM_KEYS = new Set([
  "size",
  "quality",
  "output_format",
  "output_compression",
  "moderation",
  "n",
]);

function formatSizeSummary(size: string) {
  if (size === "auto") return "1:1 · auto";
  const [w, h] = size.split("x").map(Number);
  if (!w || !h) return size;
  return `${w === h ? "1:1" : `${w}:${h}`} · ${Math.max(w, h) >= 2048 ? "2K" : "1K"}`;
}

function dimensionsFromSize(size: string) {
  if (!size || size === "auto") return null;
  const [w, h] = size.split("x").map(Number);
  if (!w || !h) return null;
  return { width: w, height: h };
}

function getSizeSelection(size: string): { mode: SizeMode; tier: SizeTier; ratio: string } {
  const normalized = normalizeImageSize(size);
  if (!normalized || normalized === "auto") {
    return { mode: "auto", tier: "1K", ratio: "1:1" };
  }

  for (const tier of TIERS) {
    for (const ratio of RATIOS) {
      if (calculateImageSize(tier, ratio) === normalized) {
        return {
          mode: "ratio",
          tier,
          ratio,
        };
      }
    }
  }

  return {
    mode: "ratio",
    tier: "1K",
    ratio: "1:1",
  };
}

function formatElapsed(ms: number | null | undefined) {
  if (!ms || ms < 0) return "0s";
  const totalSeconds = Math.floor(ms / 1000);
  if (totalSeconds < 60) return `${totalSeconds}s`;
  return `${Math.floor(totalSeconds / 60)}m ${totalSeconds % 60}s`;
}

function formatCost(value: number | null | undefined) {
  if (value == null || !Number.isFinite(value)) return "—";
  return Number.isInteger(value) ? String(value) : value.toFixed(2).replace(/\.?0+$/, "");
}

function normalizeTemplateOption(option: string) {
  return option.trim().replace(/^"+|"+$/g, "");
}

function getSizeCapabilityBadge(templates: AigcModelParamTemplate[]) {
  const sizeTemplate = templates.find((item) => item.paramKey === "size");
  const maxEdge = Math.max(
    0,
    ...(sizeTemplate?.options ?? [])
      .map(normalizeTemplateOption)
      .map((option) => {
        const match = option.match(/^(\d+)x(\d+)$/);
        if (!match) return 0;
        return Math.max(Number(match[1]), Number(match[2]));
      })
  );

  if (maxEdge >= 3200) return "4K";
  if (maxEdge >= 1500) return "2K";
  if (maxEdge >= 1080) return "1080P";
  return null;
}

function scaleToPreview(width: number, height: number) {
  const scale = Math.min(1, IMAGE_MAX_WIDTH / width, IMAGE_MAX_HEIGHT / height);
  return {
    width: Math.max(120, Math.round(width * scale)),
    height: Math.max(120, Math.round(height * scale)),
  };
}

function getDisplaySize(data: ImageNodeData, measuredSize: { width: number; height: number } | null, sizeParam: string) {
  const width = measuredSize?.width ?? data.width;
  const height = measuredSize?.height ?? data.height;

  if (!data.dataUrl) {
    const targetSize = dimensionsFromSize(sizeParam);
    if (targetSize) return scaleToPreview(targetSize.width, targetSize.height);
    return { width: PLACEHOLDER_WIDTH, height: PLACEHOLDER_HEIGHT };
  }

  if (!width || !height) return { width: PLACEHOLDER_WIDTH, height: PLACEHOLDER_HEIGHT };
  return scaleToPreview(width, height);
}

function ParamSegmented<T extends string>({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: T;
  options: { label: string; value: T }[];
  onChange: (value: T) => void;
}) {
  return (
    <div className="grid grid-cols-[52px_1fr] items-center gap-3">
      <span className="text-xs font-medium text-muted-gray">{label}</span>
      <div className="grid gap-1 rounded-xl bg-muted p-1" style={{ gridTemplateColumns: `repeat(${options.length}, minmax(0, 1fr))` }}>
        {options.map((option) => (
          <button
            key={option.value}
            type="button"
            onClick={() => onChange(option.value)}
            className={cn(
              "rounded-lg px-2 py-1.5 text-xs font-medium text-muted-gray transition-colors",
              value === option.value && "bg-background text-charcoal shadow-sm"
            )}
          >
            {option.label}
          </button>
        ))}
      </div>
    </div>
  );
}

export function ImageNodeComponent({ id, data, selected, dragging }: ImageNodeProps) {
  const { setNodes, setEdges, getNode, getNodes, getEdges } = useReactFlow();
  const updateNodeInternals = useUpdateNodeInternals();
  const edges = useStore((s) => s.edges) as AppEdge[];
  const nodes = useStore((s) => s.nodes) as AppNode[];
  const zoom = useStore((s) => s.transform[2] || 1);
  const [referencePickerPromptId, setReferencePickerPromptId] = useState<string | null>(null);
  const [modelPopoverOpen, setModelPopoverOpen] = useState(false);
  const [paramsPopoverOpen, setParamsPopoverOpen] = useState(false);
  const modelBtnRef = useRef<HTMLButtonElement>(null);
  const paramsBtnRef = useRef<HTMLButtonElement>(null);
  const modelPopoverRef = useRef<HTMLDivElement>(null);
  const paramsPopoverRef = useRef<HTMLDivElement>(null);
  const nodeMenuRef = useRef<HTMLDivElement>(null);
  const [measuredSize, setMeasuredSize] = useState<{ width: number; height: number } | null>(null);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [nodeMenu, setNodeMenu] = useState<{ x: number; y: number; visible: boolean }>({
    x: 0,
    y: 0,
    visible: false,
  });
  const [nodeMenuPos, setNodeMenuPos] = useState<{ x: number; y: number } | null>(null);
  const createdAtMs = useMemo(() => new Date(data.generationStartedAt ?? data.createdAt).getTime(), [data.createdAt, data.generationStartedAt]);
  const [now, setNow] = useState(createdAtMs);

  const status = data.status ?? "idle";
  const isGenerating = status === "pending";
  const kind = data.kind ?? "draft";
  const canCompose = kind !== "uploaded";
  const modelId = data.modelId ?? DEFAULT_MODEL_ID;
  const selectedAigcModelId = data.aigcModelId;
  const params = data.params ?? DEFAULT_PROMPT_DATA.params;
  const prompt = data.prompt ?? "";
  const currentModel = getModelById(modelId) ?? getModelById(DEFAULT_MODEL_ID)!;
  const aigcModels = useAigcModels({ type: 2, capability: "TEXT_TO_IMAGE", params });
  const activeModelName = aigcModels.selectedModel?.name ?? data.modelName ?? currentModel.name;
  const activeProviderModel = aigcModels.selectedModel?.model ?? data.providerModel ?? modelId;
  const activeAigcModelId = selectedAigcModelId ?? aigcModels.selectedModel?.id;
  const extraParamTemplates = useMemo(
    () => aigcModels.templates.filter((item) => !BUILT_IN_IMAGE_PARAM_KEYS.has(item.paramKey)),
    [aigcModels.templates]
  );
  const selectedModelCapabilityBadge = useMemo(() => getSizeCapabilityBadge(aigcModels.templates), [aigcModels.templates]);
  const costLabel = aigcModels.priceLoading ? "…" : formatCost(aigcModels.price?.salePrice);
  const imageSrc = data.previewUrl || data.dataUrl;
  const sizeSelection = useMemo(() => getSizeSelection(params.size), [params.size]);

  const connectedImages = edges
    .filter((edge) => edge.target === id)
    .map((edge) => {
      const node = nodes.find((n) => n.id === edge.source);
      return node?.type === "image" || node?.type === "sketch"
        ? { edgeId: edge.id, data: node.data as ImageNodeData | SketchNodeData }
        : null;
    })
    .filter((item): item is { edgeId: string; data: ImageNodeData | SketchNodeData } => item !== null);
  const selectedNodeCount = nodes.filter((node) => node.selected).length;
  const isOnlySelectedNode = selected && selectedNodeCount === 1;
  const showNodeActions = selectedNodeCount <= 1;
  const fixedUiScale = 1 / zoom;

  const isReferencedByActivePrompt = Boolean(
    referencePickerPromptId &&
      referencePickerPromptId !== id &&
      edges.some((edge) => edge.source === id && edge.target === referencePickerPromptId)
  );

  useEffect(() => {
    function handleReferencePicker(e: Event) {
      const detail = (e as CustomEvent<ReferencePickerEventDetail>).detail;
      setReferencePickerPromptId(detail?.promptId ?? null);
    }

    window.addEventListener("copse:reference-picker", handleReferencePicker);
    return () => window.removeEventListener("copse:reference-picker", handleReferencePicker);
  }, []);

  useEffect(() => {
    if (!isGenerating) return;
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [isGenerating]);

  useEffect(() => {
    if (!selected && referencePickerPromptId === id) {
      window.dispatchEvent(new CustomEvent<ReferencePickerEventDetail>("copse:reference-picker", {
        detail: { promptId: null },
      }));
    }
  }, [id, referencePickerPromptId, selected]);

  const updateData = useCallback(
    (patch: Partial<ImageNodeData>) => {
      setNodes((nds) =>
        nds.map((n) => (n.id === id ? { ...n, data: { ...n.data, ...patch } } : n))
      );
      window.dispatchEvent(new CustomEvent<NodeDataPatchEventDetail>("copse:node-data-patch", {
        detail: { nodeId: id, patch },
      }));
    },
    [id, setNodes]
  );

  const deleteNode = useCallback(() => {
    setNodeMenu((prev) => ({ ...prev, visible: false }));
    setNodes((nds) => nds.filter((n) => n.id !== id));
    setEdges((eds) => eds.filter((e) => e.source !== id && e.target !== id));
    deleteImage(data.imageId).catch(() => {});
  }, [data.imageId, id, setEdges, setNodes]);

  const duplicateNode = useCallback(() => {
    const source = getNode(id);
    if (!source) return;
    const nextImageId = `img_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
    const duplicateData: ImageNodeData = {
      ...data,
      imageId: nextImageId,
      fileName: data.fileName,
      createdAt: new Date().toISOString(),
    };
    saveImage(duplicateData).catch(() => {});
    setNodeMenu((prev) => ({ ...prev, visible: false }));
    setNodes((nds) => [
      ...nds.map((node) => ({ ...node, selected: false })),
      {
        id: nextImageId,
        type: "image",
        position: { x: source.position.x + 40, y: source.position.y + 40 },
        selected: true,
        data: duplicateData,
      },
    ]);
  }, [data, getNode, id, setNodes]);

  const copyImageToClipboard = useCallback(async () => {
    setNodeMenu((prev) => ({ ...prev, visible: false }));
    if (!imageSrc) return;

    try {
      if (imageSrc.startsWith("data:") && "ClipboardItem" in window) {
        const response = await fetch(imageSrc);
        const blob = await response.blob();
        await navigator.clipboard.write([
          new ClipboardItem({ [blob.type || data.mimeType || "image/png"]: blob }),
        ]);
        return;
      }
      await navigator.clipboard.writeText(imageSrc);
    } catch {
      try {
        await navigator.clipboard.writeText(imageSrc);
      } catch {
        // Clipboard permissions can be denied by the browser; keep the menu action silent.
      }
    }
  }, [imageSrc, data.mimeType]);

  const updateParams = useCallback(
    (patch: Record<string, unknown>) => {
      setNodes((nds) =>
        nds.map((n) => {
          if (n.id !== id) return n;
          const d = n.data as ImageNodeData;
          return { ...n, data: { ...d, params: { ...(d.params ?? DEFAULT_PROMPT_DATA.params), ...patch } } };
        })
      );
      window.dispatchEvent(new CustomEvent<NodeDataPatchEventDetail>("copse:node-data-patch", {
        detail: { nodeId: id, patch: { params: { ...(data.params ?? DEFAULT_PROMPT_DATA.params), ...patch } } },
      }));
    },
    [data.params, id, setNodes]
  );

  const handleNodeClick = useCallback(
    (e: MouseEvent<HTMLDivElement>) => {
      if (!referencePickerPromptId || referencePickerPromptId === id) return;
      e.stopPropagation();
      setEdges((eds) => {
        const exists = eds.some((edge) => edge.source === id && edge.target === referencePickerPromptId);
        if (exists) return eds;
        return [
          ...eds,
          {
            id: `e-${id}-${referencePickerPromptId}`,
            source: id,
            target: referencePickerPromptId,
            type: "signal",
          },
        ];
      });
    },
    [id, referencePickerPromptId, setEdges]
  );

  const removeImage = useCallback(
    (edgeId: string) => setEdges((eds) => eds.filter((e) => e.id !== edgeId)),
    [setEdges]
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

  const handleSizeModeChange = useCallback(
    (mode: SizeMode) => {
      if (mode === "auto") {
        updateParams({ size: "auto" });
        return;
      }
      if (mode === "ratio") {
        updateParams({ size: calculateImageSize(sizeSelection.tier, sizeSelection.ratio) ?? "auto" });
      }
    },
    [sizeSelection.ratio, sizeSelection.tier, updateParams]
  );

  const handleTierChange = useCallback(
    (tier: SizeTier) => {
      updateParams({ size: calculateImageSize(tier, sizeSelection.ratio) ?? "auto" });
    },
    [sizeSelection.ratio, updateParams]
  );

  const handleRatioChange = useCallback(
    (ratio: string) => {
      updateParams({ size: calculateImageSize(sizeSelection.tier, ratio) ?? "auto" });
    },
    [sizeSelection.tier, updateParams]
  );

  const handleModelSelect = useCallback(
    (nextModelId: string) => {
      const model = getModelById(nextModelId);
      aigcModels.setSelectedModelId(null);
      updateData({
        modelId: nextModelId,
        providerModel: nextModelId,
        modelName: model?.name,
        aigcModelId: undefined,
        params: model?.quality ? { ...params, quality: model.quality } : params,
      });
      setModelPopoverOpen(false);
    },
    [aigcModels, params, updateData]
  );

  const handleAigcModelSelect = useCallback(
    (nextModelId: number) => {
      const model = aigcModels.models.find((item) => item.id === nextModelId);
      if (!model) return;
      aigcModels.setSelectedModelId(nextModelId);
      updateData({
        modelId: String(nextModelId),
        providerModel: model.model,
        modelName: model.name,
        aigcModelId: model.id,
      });
      setModelPopoverOpen(false);
    },
    [aigcModels, updateData]
  );

  const openReferencePicker = useCallback(() => {
    if (isGenerating) return;
    window.dispatchEvent(new CustomEvent<ReferencePickerEventDetail>("copse:reference-picker", {
      detail: { promptId: id },
    }));
  }, [id, isGenerating]);

  const handleGenerate = useCallback(async () => {
    const cleanPrompt = prompt.trim();
    if (!cleanPrompt || isGenerating) return;

    const startedAt = new Date().toISOString();
    const ctx = { nodes: getNodes() as AppNode[], edges: getEdges() as AppEdge[] };
    const { snapshots, ids, mode } = resolveInputImages(id, ctx);
    const resultDraft: ResultNodeData = {
      taskId: null,
      promptNodeId: id,
      prompt: cleanPrompt,
      params,
      modelId,
      providerModel: activeProviderModel,
      aigcModelId: activeAigcModelId,
      modelName: activeModelName,
      mode,
      inputImages: snapshots,
      inputImageIds: ids,
      status: "pending",
      imageUrls: [],
      errorMessage: null,
      createdAt: startedAt,
      completedAt: null,
      elapsedMs: null,
    };
    const promptData: PromptNodeData = {
      prompt: cleanPrompt,
      modelId,
      providerModel: activeProviderModel,
      aigcModelId: activeAigcModelId,
      modelName: activeModelName,
      params,
      isGenerating: true,
    };

    updateData({
      status: "pending",
      errorMessage: null,
      safetyStatus: null,
      safetyReason: null,
      generationStartedAt: startedAt,
      generationCompletedAt: null,
      elapsedMs: null,
    });

    const projectId = new URLSearchParams(window.location.search).get("projectId");
    if (isServerCanvasProjectId(projectId) && activeAigcModelId) {
      const clientId = `node_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
      try {
        const run = await canvasNodeRunApi.runNode(projectId, id, {
          clientId,
          baseVersion: 0,
          runId: clientId,
          nodeType: "image",
          generateType: "IMAGE",
          generateMode: mode === "edit" ? "IMAGE_TO_IMAGE" : "TEXT_TO_IMAGE",
          modelId: activeAigcModelId,
          prompt: cleanPrompt,
          inputParams: JSON.stringify({ ...params, inputImageIds: ids, inputImages: snapshots }),
          sync: false,
        });
        await waitCanvasNodeRunResult(projectId, id, {
          taskId: run.taskId,
          baseVersion: 0,
          nodeType: "image",
        });
        return;
      } catch {
      }
    }

    let updates: Partial<ResultNodeData>;
    try {
      updates = await createGenerationTask(promptData, resultDraft);
    } catch (err) {
      updates = {
        status: "failed",
        taskId: null,
        imageUrls: [],
        errorMessage: err instanceof Error ? err.message : "生成失败，请稍后重试。",
        safetyStatus: null,
        safetyReason: null,
        elapsedMs: Date.now() - new Date(startedAt).getTime(),
        completedAt: new Date().toISOString(),
      };
    }
    const completedAt = typeof updates.completedAt === "string" ? updates.completedAt : new Date().toISOString();
    const nextData: Partial<ImageNodeData> = {
      status: updates.status === "complete" ? "idle" : "failed",
      taskId: typeof updates.taskId === "string" ? updates.taskId : null,
      errorMessage: typeof updates.errorMessage === "string" ? updates.errorMessage : null,
      safetyStatus: typeof updates.safetyStatus === "string" ? updates.safetyStatus : null,
      safetyReason: typeof updates.safetyReason === "string" ? updates.safetyReason : null,
      generationCompletedAt: completedAt,
      elapsedMs: typeof updates.elapsedMs === "number" ? updates.elapsedMs : Date.now() - new Date(startedAt).getTime(),
    };

    if (updates.status === "complete" && updates.imageUrls?.[0]) {
      nextData.kind = "generated";
      nextData.dataUrl = updates.imageUrls[0];
      nextData.fileName = "generated-image.png";
      nextData.mimeType = params.output_format === "jpeg" ? "image/jpeg" : `image/${params.output_format}`;
    }

    setNodes((nds) =>
      nds.map((n) => {
        if (n.id !== id) return n;
        const merged = { ...(n.data as ImageNodeData), ...nextData };
        saveImage(merged).catch(() => {});
        return { ...n, data: merged };
      })
    );
  }, [activeAigcModelId, activeModelName, activeProviderModel, getEdges, getNodes, id, isGenerating, modelId, params, prompt, setNodes, updateData]);

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
    window.addEventListener("pointerdown", handlePointerDown, true);
    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.removeEventListener("pointerdown", handlePointerDown, true);
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [modelPopoverOpen, paramsPopoverOpen]);

  useEffect(() => {
    if (!nodeMenu.visible) return;

    function handlePointerDown(e: PointerEvent) {
      if (nodeMenuRef.current?.contains(e.target as HTMLElement)) return;
      setNodeMenu((prev) => ({ ...prev, visible: false }));
    }
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") setNodeMenu((prev) => ({ ...prev, visible: false }));
    }

    window.addEventListener("pointerdown", handlePointerDown, true);
    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.removeEventListener("pointerdown", handlePointerDown, true);
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [nodeMenu.visible]);

  useEffect(() => {
    if (!nodeMenu.visible || !nodeMenuRef.current) {
      setNodeMenuPos(null);
      return;
    }
    const { width, height } = nodeMenuRef.current.getBoundingClientRect();
    setNodeMenuPos(clampToViewport({ x: nodeMenu.x, y: nodeMenu.y, width, height }));
  }, [nodeMenu.visible, nodeMenu.x, nodeMenu.y]);

  const elapsedMs = data.elapsedMs ?? (isGenerating ? now - createdAtMs : null);
  const safetyStatus = normalizeSafetyStatus(data.safetyStatus) !== "idle" ? normalizeSafetyStatus(data.safetyStatus) : normalizeSafetyStatusFromError(data.safetyReason ?? data.errorMessage);
  const safety = getSafetyCopy(safetyStatus, "generation");
  const previewItem = useMemo(() => imageNodeToMediaPreview({ ...data, elapsedMs }), [data, elapsedMs]);
  const displaySize = getDisplaySize(data, measuredSize, params.size);
  const displayLeft = (PREVIEW_SLOT_WIDTH - displaySize.width) / 2;
  const displayTop = 28 + PREVIEW_SLOT_HEIGHT - displaySize.height;
  const pickerActiveForThisNode = referencePickerPromptId === id;
  useEffect(() => {
    const frame = window.requestAnimationFrame(() => updateNodeInternals(id));
    const timeout = window.setTimeout(() => updateNodeInternals(id), 260);

    return () => {
      window.cancelAnimationFrame(frame);
      window.clearTimeout(timeout);
    };
  }, [displayLeft, displayTop, displaySize.width, displaySize.height, id, updateNodeInternals]);

  const handlePreviewDownload = useCallback(() => {
    if (!previewItem) return;
    downloadMedia(previewItem);
  }, [previewItem]);

  return (
    <>
      <div
        onContextMenuCapture={(e) => {
          e.preventDefault();
          e.stopPropagation();
          setNodeMenu({ x: e.clientX, y: e.clientY, visible: true });
          window.dispatchEvent(new CustomEvent("copse:canvas-interaction"));
        }}
        onContextMenu={(e) => {
          e.preventDefault();
          e.stopPropagation();
          setNodeMenu({ x: e.clientX, y: e.clientY, visible: true });
          window.dispatchEvent(new CustomEvent("copse:canvas-interaction"));
        }}
        onPointerDown={(e) => {
          if (referencePickerPromptId && referencePickerPromptId !== id) {
            e.stopPropagation();
          }
        }}
        onClick={handleNodeClick}
        className={cn(
          "relative",
          referencePickerPromptId && referencePickerPromptId !== id ? "cursor-pointer" : ""
        )}
        style={{ width: PREVIEW_SLOT_WIDTH }}
      >
        <div className="relative" style={{ width: PREVIEW_SLOT_WIDTH, height: PREVIEW_SLOT_HEIGHT + 28 }}>
          <AnimatePresence>
            {isOnlySelectedNode && !dragging && previewItem && (
              <SelectedMediaToolbar
                canDownload={Boolean(previewItem.url)}
                onDownload={handlePreviewDownload}
                onOpenPreview={() => setPreviewOpen(true)}
                uiScale={fixedUiScale}
                style={{
                  left: displayLeft + displaySize.width / 2,
                  top: displayTop - 54 * fixedUiScale,
                  pointerEvents: "auto",
                }}
              />
            )}
          </AnimatePresence>
          <motion.div
            className="absolute flex items-center gap-1.5 bg-transparent px-1 text-sm font-medium text-muted-gray"
            initial={false}
            animate={{
              left: displayLeft,
              top: Math.max(0, displayTop - 28 * fixedUiScale),
              width: displaySize.width,
              scale: fixedUiScale,
            }}
            transition={{ type: "spring", stiffness: 360, damping: 34 }}
            style={{ transformOrigin: "bottom left" }}
          >
            <ImageIcon className="size-4" />
            <span className="line-clamp-1" title={data.fileName}>
              Image
            </span>
          </motion.div>

          <motion.div
            className="absolute"
            initial={false}
            animate={{
              left: displayLeft,
              top: displayTop,
              width: displaySize.width,
              height: displaySize.height,
            }}
            transition={{ type: "spring", stiffness: 360, damping: 34 }}
          >
            {isReferencedByActivePrompt && (
              <div className="pointer-events-none absolute -right-2 -top-2 z-10 flex size-6 items-center justify-center rounded-full bg-charcoal text-off-white shadow">
                <Check className="size-3.5" />
              </div>
            )}

            <motion.div
              data-node-preview-card
              data-node-id={id}
              initial={{ opacity: 0, scale: 0.97 }}
              animate={{ opacity: 1, scale: 1, width: displaySize.width, height: displaySize.height }}
              transition={{
                opacity: { duration: 0.18, ease: "easeOut" },
                scale: { duration: 0.18, ease: "easeOut" },
                width: { type: "spring", stiffness: 360, damping: 34 },
                height: { type: "spring", stiffness: 360, damping: 34 },
              }}
              className={cn(
                "canvas-node-drag-handle group relative rounded-xl border bg-background shadow-[0_1px_3px_rgba(0,0,0,0.06)]",
                selected ? "border-charcoal/60 ring-2 ring-charcoal/35" : "border-border-warm",
                isReferencedByActivePrompt && "border-charcoal ring-2 ring-charcoal/30"
              )}
              style={{ width: displaySize.width, height: displaySize.height, pointerEvents: "auto" }}
            >
              <div className="absolute inset-0 flex items-center justify-center overflow-hidden rounded-[inherit]">
                {imageSrc ? (
                  <img
                    src={imageSrc}
                    alt={data.fileName}
                    className="size-full object-contain"
                    draggable={false}
                    onLoad={(event) => {
                      const image = event.currentTarget;
                      if (image.naturalWidth > 0 && image.naturalHeight > 0) {
                        setMeasuredSize({ width: image.naturalWidth, height: image.naturalHeight });
                      }
                      if (
                        image.naturalWidth > 0 &&
                        image.naturalHeight > 0 &&
                        (data.width !== image.naturalWidth || data.height !== image.naturalHeight)
                      ) {
                        updateData({ width: image.naturalWidth, height: image.naturalHeight });
                      }
                    }}
                  />
                ) : (
                  <ImageIcon className="size-12 text-muted-gray/40" />
                )}
                <AnimatePresence>
                  {isGenerating && (
                    <motion.div
                      key="generating"
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      exit={{ opacity: 0 }}
                      transition={{ duration: 0.18 }}
                      className="absolute inset-0 flex flex-col items-center justify-center bg-charcoal/45 text-off-white"
                    >
                      <motion.div
                        animate={{ opacity: [0.55, 1, 0.55] }}
                        transition={{ duration: 1.4, repeat: Infinity, ease: "easeInOut" }}
                      >
                        <Loader2 className="mb-2 size-7 animate-spin" />
                      </motion.div>
                      <span className="text-xs">生成中 {formatElapsed(elapsedMs)}</span>
                    </motion.div>
                  )}
                  {data.status === "failed" && !isGenerating && (
                    <motion.div
                      key="failed"
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      exit={{ opacity: 0 }}
                      transition={{ duration: 0.16 }}
                      className="absolute inset-0 flex flex-col items-center justify-center bg-background/90 px-4 text-center text-destructive"
                    >
                      {safety.status !== "idle" ? <SafetyInlineNotice state={{ ...safety, description: data.safetyReason || safety.description }} /> : <span className="text-xs">{data.errorMessage ?? "生成失败"}</span>}
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
              <NodeCreateHandle nodeId={id} direction="incoming" selected={selected} showButton={showNodeActions} />
              <NodeCreateHandle nodeId={id} direction="outgoing" selected={selected} showButton={showNodeActions} />
            </motion.div>
          </motion.div>
        </div>

        <AnimatePresence>
          {isOnlySelectedNode && !dragging && canCompose && (
            <motion.div
              initial={{ opacity: 0, y: -8 * fixedUiScale, scale: 0.99 * fixedUiScale }}
              animate={{ opacity: 1, y: 0, scale: fixedUiScale }}
              exit={{ opacity: 0, y: -8 * fixedUiScale, scale: 0.99 * fixedUiScale }}
              transition={{ duration: 0.18, ease: "easeOut" }}
              className="nodrag nowheel rounded-xl border border-border-warm bg-background p-4 shadow-[0_8px_24px_rgba(28,28,28,0.08)]"
              style={{
                width: COMPOSER_WIDTH,
                marginLeft: (PREVIEW_SLOT_WIDTH - COMPOSER_WIDTH) / 2,
                marginTop: 12 * fixedUiScale,
                transformOrigin: "top center",
                pointerEvents: "auto",
              }}
            >
            <div className="mb-3 flex items-start justify-between gap-3">
              <div className="flex min-w-0 flex-wrap items-center gap-2">
                <button
                  type="button"
                  disabled={isGenerating}
                  className="flex size-9 items-center justify-center rounded-lg border border-border-warm bg-muted text-muted-gray"
                  aria-label="Prompt 工具"
                >
                  <Sparkles className="size-4" />
                </button>
                {connectedImages.map((img) => (
                  <div key={img.edgeId} className="group relative shrink-0">
                    <div className="size-9 overflow-hidden rounded-lg border border-border-warm bg-muted">
                      {(img.data.previewUrl || img.data.dataUrl) ? (
                        <img src={img.data.previewUrl || img.data.dataUrl} alt={img.data.fileName} className="size-full object-cover" draggable={false} />
                      ) : (
                        <ImageIcon className="m-2 size-5 text-muted-gray/40" />
                      )}
                    </div>
                    <button
                      type="button"
                      onClick={() => removeImage(img.edgeId)}
                      disabled={isGenerating}
                      className="absolute -right-1.5 -top-1.5 flex size-4 items-center justify-center rounded-full bg-charcoal text-off-white opacity-0 shadow transition-opacity group-hover:opacity-100 disabled:cursor-not-allowed"
                      aria-label="移除参考图"
                    >
                      <X className="size-2.5" />
                    </button>
                  </div>
                ))}
                <button
                  type="button"
                  onClick={openReferencePicker}
                  disabled={isGenerating}
                  className={cn(
                    "flex size-9 items-center justify-center rounded-lg text-muted-gray transition-colors disabled:cursor-not-allowed disabled:opacity-40",
                    pickerActiveForThisNode ? "bg-charcoal text-off-white" : "bg-muted hover:text-charcoal"
                  )}
                  aria-label="选择参考图"
                >
                  <Plus className="size-4" />
                </button>
              </div>
              {isGenerating && <span className="text-xs text-muted-gray">生成中 {formatElapsed(elapsedMs)}</span>}
            </div>

            <div className="relative min-h-[130px]">
              <textarea
                value={prompt}
                onChange={(e) => updateData({ prompt: e.target.value })}
                disabled={isGenerating}
                placeholder="Describe anything you want to generate"
                className="h-full min-h-[130px] w-full resize-none bg-transparent text-base leading-7 text-charcoal placeholder:text-muted-gray focus:outline-none disabled:cursor-not-allowed disabled:text-muted-gray"
              />
              {pickerActiveForThisNode && (
                <div className="pointer-events-none absolute inset-0 flex items-center justify-center rounded-lg bg-background/70 text-xs text-muted-gray">
                  ESC to exit
                </div>
              )}
            </div>

            <div className="mt-4 flex items-center justify-between gap-3 border-t border-border-warm pt-3">
              <div className="flex min-w-0 items-center gap-2">
                <div className="relative">
                  <button
                    ref={modelBtnRef}
                    type="button"
                    disabled={isGenerating}
                    onClick={() => {
                      setParamsPopoverOpen(false);
                      setModelPopoverOpen((v) => !v);
                    }}
                    className="flex items-center gap-2 rounded-md border border-transparent px-2 py-1.5 text-sm font-medium text-charcoal transition-colors hover:border-charcoal/40 hover:bg-background disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    <Sparkles className="size-4 text-muted-gray" />
                    <span>{activeModelName}</span>
                  </button>
                  <AnimatePresence>
                    {modelPopoverOpen && (
                    <motion.div
                      ref={modelPopoverRef}
                      initial={{ opacity: 0, y: 4, scale: 0.98 }}
                      animate={{ opacity: 1, y: 0, scale: 1 }}
                      exit={{ opacity: 0, y: 4, scale: 0.98 }}
                      transition={{ duration: 0.14, ease: "easeOut" }}
                      className="absolute bottom-full left-0 z-[260] mb-2 w-[280px] overflow-hidden rounded-xl border border-border-warm bg-background p-2 shadow-[0_4px_12px_rgba(0,0,0,0.1)]"
                    >
                      <div className="max-h-[320px] overflow-y-auto">
                        {aigcModels.models.length > 0 ? aigcModels.models.map((model) => {
                          const isSelected = aigcModels.selectedModelId === model.id || selectedAigcModelId === model.id;
                          const badge = isSelected ? selectedModelCapabilityBadge ?? "Image" : "Image";
                          return (
                            <button
                              key={model.id}
                              type="button"
                              onClick={() => handleAigcModelSelect(model.id)}
                              className={cn("mb-1 flex w-full flex-col items-stretch gap-2 rounded-xl px-3 py-3 text-left transition-colors last:mb-0", isSelected ? "bg-muted" : "hover:bg-muted/70")}
                            >
                              <span className="flex min-w-0 items-center gap-2">
                                <ImageIcon className="size-4 shrink-0 text-muted-gray" />
                                <span className="truncate text-sm font-medium text-charcoal">{model.name}</span>
                                {isSelected && <Check className="ml-auto size-4 shrink-0 text-charcoal" />}
                              </span>
                              <span className="ml-6 flex w-fit items-center gap-1 rounded-full bg-muted px-2 py-0.5 text-[11px] font-medium text-muted-gray">
                                <Gem className="size-3" />
                                {badge}
                              </span>
                            </button>
                          );
                        }) : IMAGE_MODELS.filter((m) => m.enabled).map((model) => {
                          const isSelected = modelId === model.id;
                          return (
                            <button key={model.id} type="button" onClick={() => handleModelSelect(model.id)} className={cn("mb-1 flex w-full flex-col items-stretch gap-2 rounded-xl px-3 py-3 text-left transition-colors last:mb-0", isSelected ? "bg-muted" : "hover:bg-muted/70")}>
                              <span className="flex min-w-0 items-center gap-2">
                                <ImageIcon className="size-4 shrink-0 text-muted-gray" />
                                <span className="truncate text-sm font-medium text-charcoal">{model.name}</span>
                                {isSelected && <Check className="ml-auto size-4 shrink-0 text-charcoal" />}
                              </span>
                              <span className="ml-6 flex w-fit items-center gap-1 rounded-full bg-muted px-2 py-0.5 text-[11px] font-medium text-muted-gray">
                                <Gem className="size-3" />
                                Image
                              </span>
                            </button>
                          );
                        })}
                      </div>
                    </motion.div>
                    )}
                  </AnimatePresence>
                </div>
                <span className="h-5 w-px bg-border-warm" />
                <div className="relative">
                  <button
                    ref={paramsBtnRef}
                    type="button"
                    disabled={isGenerating}
                    onClick={() => {
                      setModelPopoverOpen(false);
                      setParamsPopoverOpen((v) => !v);
                    }}
                    className="flex items-center gap-2 rounded-md border border-transparent px-2 py-1.5 text-sm text-muted-gray transition-colors hover:border-charcoal/40 hover:bg-background hover:text-charcoal disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    <SlidersHorizontal className="size-4" />
                    <span>{formatSizeSummary(params.size)}</span>
                  </button>
                  <AnimatePresence>
                    {paramsPopoverOpen && (
                    <motion.div
                      ref={paramsPopoverRef}
                      initial={{ opacity: 0, y: 4, scale: 0.98 }}
                      animate={{ opacity: 1, y: 0, scale: 1 }}
                      exit={{ opacity: 0, y: 4, scale: 0.98 }}
                      transition={{ duration: 0.14, ease: "easeOut" }}
                      className="absolute bottom-full left-0 z-[260] mb-2 w-[440px] rounded-xl border border-border-warm bg-background p-4 shadow-[0_4px_12px_rgba(0,0,0,0.1)]"
                    >
                      <div className="mb-4 flex items-center justify-between">
                        <div>
                          <p className="text-sm font-medium text-charcoal">生成参数</p>
                          <p className="mt-0.5 text-[11px] text-muted-gray">当前：{params.size}</p>
                        </div>
                        <button type="button" onClick={() => setParamsPopoverOpen(false)} className="flex size-7 items-center justify-center rounded-full text-muted-gray transition-colors hover:bg-muted hover:text-charcoal" aria-label="关闭参数">
                          <X className="size-4" />
                        </button>
                      </div>

                      <section className="space-y-3">
                        <p className="text-xs font-medium text-muted-gray">Size</p>
                        <div className="grid grid-cols-2 gap-1 rounded-xl bg-muted p-1">
                          {(["auto", "ratio"] as const).map((mode) => (
                            <button
                              key={mode}
                              type="button"
                              onClick={() => handleSizeModeChange(mode)}
                              className={cn(
                                "rounded-lg px-3 py-2 text-xs font-medium text-muted-gray transition-colors",
                                sizeSelection.mode === mode && "bg-background text-charcoal shadow-sm"
                              )}
                            >
                              {mode === "auto" ? "Auto" : "Ratio"}
                            </button>
                          ))}
                        </div>

                        {sizeSelection.mode === "ratio" && (
                          <div className="space-y-3">
                            <div className="grid grid-cols-3 gap-2">
                              {TIERS.map((tier) => (
                                <button
                                  key={tier}
                                  type="button"
                                  onClick={() => handleTierChange(tier)}
                                  className={cn(
                                    "rounded-lg bg-muted px-3 py-2 text-xs font-medium text-muted-gray transition-colors hover:text-charcoal",
                                    sizeSelection.tier === tier && "bg-background text-charcoal shadow-sm"
                                  )}
                                >
                                  {tier}
                                </button>
                              ))}
                            </div>
                            <div className="grid grid-cols-4 gap-1.5">
                              {RATIOS.map((ratio) => (
                                <button
                                  key={ratio}
                                  type="button"
                                  onClick={() => handleRatioChange(ratio)}
                                  className={cn(
                                    "rounded-lg bg-muted px-2 py-1.5 text-xs font-medium text-muted-gray transition-colors hover:text-charcoal",
                                    sizeSelection.ratio === ratio && "bg-background text-charcoal shadow-sm"
                                  )}
                                >
                                  {ratio}
                                </button>
                              ))}
                            </div>
                          </div>
                        )}
                      </section>

                      <section className="mt-5 space-y-3">
                        <p className="text-xs font-medium text-muted-gray">Output</p>

                        <div className="space-y-2">
                          <ParamSegmented label="质量" value={params.quality} options={QUALITY_OPTIONS} onChange={(value) => updateParams({ quality: value })} />
                          <ParamSegmented label="格式" value={params.output_format} options={FORMAT_OPTIONS} onChange={handleFormatChange} />
                          <ParamSegmented label="审核" value={params.moderation} options={MODERATION_OPTIONS} onChange={(value) => updateParams({ moderation: value })} />
                        </div>

                      </section>

                      {(aigcModels.templateLoading || extraParamTemplates.length > 0) && (
                        <section className="mt-5 space-y-3">
                          <div className="flex items-center justify-between gap-3">
                            <p className="text-xs font-medium text-muted-gray">模型额外参数</p>
                            {aigcModels.templateLoading && <span className="text-[11px] text-muted-gray">加载中...</span>}
                          </div>
                          {extraParamTemplates.length > 0 && (
                            <DynamicParamForm templates={extraParamTemplates} values={params} disabled={isGenerating} onChange={updateParams} />
                          )}
                        </section>
                      )}
                    </motion.div>
                    )}
                  </AnimatePresence>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <button
                  type="button"
                  onClick={handleGenerate}
                  disabled={!prompt.trim() || isGenerating}
                  className="flex h-11 items-center gap-3 rounded-full bg-charcoal/90 py-1 pl-4 pr-1 text-off-white shadow-[0_4px_12px_rgba(0,0,0,0.18)] transition-opacity active:opacity-85 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-black/80"
                  aria-label={isGenerating ? "生成中" : "开始生成"}
                >
                  <span
                    className={cn(
                      "flex items-center gap-2 text-sm font-semibold tabular-nums text-off-white transition-colors duration-200 ease-in-out",
                      aigcModels.priceLoading && "text-off-white/45"
                    )}
                  >
                    <Sparkles className="size-4 text-current" />
                    <AnimatePresence mode="wait" initial={false}>
                      <motion.span
                        key={costLabel}
                        initial={{ opacity: 0, y: 4 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: -4 }}
                        transition={{ duration: 0.18, ease: "easeInOut" }}
                      >
                        {costLabel}
                      </motion.span>
                    </AnimatePresence>
                  </span>
                  <span className="flex size-9 items-center justify-center rounded-full bg-off-white text-charcoal shadow-sm">
                    {isGenerating ? <Loader2 className="size-5 animate-spin" /> : <ArrowUp className="size-5" />}
                  </span>
                </button>
              </div>
            </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {nodeMenu.visible && createPortal(
        <motion.div
          ref={nodeMenuRef}
          initial={{ opacity: 0, scale: 0.98, y: -2 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          transition={{ duration: 0.14, ease: "easeOut" }}
          className="fixed z-[220] min-w-[240px] rounded-xl border border-border-warm bg-background py-1 shadow-[0_4px_16px_rgba(0,0,0,0.08)]"
          style={nodeMenuPos ? { left: nodeMenuPos.x, top: nodeMenuPos.y } : { visibility: "hidden" }}
        >
          <button
            type="button"
            onClick={copyImageToClipboard}
            disabled={!data.dataUrl}
            className="flex w-full items-center justify-between px-4 py-2 text-left text-[13px] text-charcoal transition-colors hover:bg-muted disabled:text-muted-gray disabled:opacity-45"
          >
            <span>Copy</span>
            <span className="ml-6 text-[11px] text-muted-gray">⌘C</span>
          </button>
          <button
            type="button"
            disabled
            className="flex w-full items-center justify-between px-4 py-2 text-left text-[13px] text-muted-gray opacity-45"
          >
            <span>Paste</span>
            <span className="ml-6 text-[11px]">⌘V</span>
          </button>
          <button
            type="button"
            onClick={duplicateNode}
            className="flex w-full items-center justify-between px-4 py-2 text-left text-[13px] text-charcoal transition-colors hover:bg-muted"
          >
            <span>Duplicate</span>
          </button>
          <div className="my-1 h-px bg-border-warm" />
          <button
            type="button"
            onClick={deleteNode}
            className="flex w-full items-center justify-between px-4 py-2 text-left text-[13px] text-charcoal transition-colors hover:bg-muted"
          >
            <span>Delete</span>
            <span className="ml-6 text-[11px] text-muted-gray">⌫</span>
          </button>
          <div className="my-1 h-px bg-border-warm" />
          <button
            type="button"
            onClick={copyImageToClipboard}
            disabled={!data.dataUrl}
            className="flex w-full items-center justify-between px-4 py-2 text-left text-[13px] text-charcoal transition-colors hover:bg-muted disabled:text-muted-gray disabled:opacity-45"
          >
            <span>Copy to clipboard</span>
          </button>
        </motion.div>,
        document.body
      )}
      <MediaPreviewDialog item={previewItem} open={previewOpen} onClose={() => setPreviewOpen(false)} />
    </>
  );
}
