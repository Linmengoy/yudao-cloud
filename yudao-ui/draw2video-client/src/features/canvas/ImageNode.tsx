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
import { useAuth } from "@/features/auth/auth-store";
import { NodeCreateHandle } from "./NodeCreateHandle";
import { createGenerationTask, resolveInputImages } from "./use-generation";
import type { ImageModeration, ImageOutputFormat, ImageQuality, ImageTaskParams } from "@/features/image-generation/types";
import { getGenerationStatusLabel } from "@/features/generation/generation-status";
import type { AigcModelParamTemplate } from "@/features/generation/model-api";
import { useAigcModels } from "@/features/generation/use-aigc-models";
import { canvasNodeRunApi, getCanvasNodeRunPatch, isServerCanvasProjectId, waitCanvasNodeRunResult } from "@/features/canvas/canvas-node-run-api";
import { getMyAsset } from "@/features/assets/asset-api";
import { getAssetPreviewExpireTime, getAssetPreviewUrl } from "@/features/assets/asset-dictionaries";
import { getSafetyCopy } from "@/features/safety/safety-copy";
import { SafetyInlineNotice } from "@/features/safety/safety-ui";
import { normalizeSafetyStatus, normalizeSafetyStatusFromError } from "@/features/safety/safety-status";
import { MediaPreviewDialog } from "@/features/media-preview/MediaPreviewDialog";
import { SelectedMediaToolbar } from "@/features/media-preview/SelectedMediaToolbar";
import { downloadMedia, imageNodeToMediaPreview } from "@/features/media-preview/media-preview-utils";
import { cn } from "@/lib/utils";
import { clampToViewport } from "./floating-position";
import { EditableNodeTitle } from "./EditableNodeTitle";
import { CanvasNodeTitle } from "./CanvasNodeTitle";
import { createPromptMentionToken, PromptMentionInput, promptValueToSubmitPrompt, useComposerWheelPan, type PromptMentionOption } from "./PromptMentionInput";

type ImageNodeProps = NodeProps<Node<ImageNodeData, "image">>;
type ConnectedImage = { edgeId: string; nodeId: string; data: Pick<ImageNodeData | SketchNodeData, "previewUrl" | "dataUrl" | "fileName"> };

const PLACEHOLDER_WIDTH = 360;
const PLACEHOLDER_HEIGHT = 260;
const IMAGE_MAX_WIDTH = 420;
const IMAGE_MAX_HEIGHT = 420;
const COMPOSER_WIDTH = 620;

type SizeSelection = { tier: string; ratio: string };

const TIERS = ["1K", "2K", "4K"];
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
  "ratio",
  "aspectRatio",
  "imageSize",
  "resolution",
]);

function dimensionsFromSize(size: string) {
  if (!size || size === "auto") return null;
  const [w, h] = size.split("x").map(Number);
  if (!w || !h) return null;
  return { width: w, height: h };
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
  let value = String(option).trim();
  for (let i = 0; i < 3; i += 1) {
    try {
      const parsed = JSON.parse(value);
      if (typeof parsed !== "string") break;
      value = parsed.trim();
    } catch {
      break;
    }
  }
  return value.replace(/\\/g, "").replace(/^"+|"+$/g, "").trim();
}

function findTemplate(templates: AigcModelParamTemplate[], keys: string[]) {
  return templates.find((item) => keys.includes(item.paramKey));
}

function templateOptions(template: AigcModelParamTemplate | undefined, fallback: string[]) {
  const options = (template?.options ?? []).map(normalizeTemplateOption).filter(Boolean);
  return options.length > 0 ? options : fallback;
}

function templateDefault(template: AigcModelParamTemplate | undefined, fallback: string) {
  const normalized = template?.defaultValue ? normalizeTemplateOption(template.defaultValue) : "";
  return normalized || templateOptions(template, [fallback])[0] || fallback;
}

function segmentedOptions<T extends string>(template: AigcModelParamTemplate | undefined, fallback: { label: string; value: T }[]) {
  const fallbackByValue = new Map(fallback.map((option) => [option.value, option.label]));
  const options = templateOptions(template, fallback.map((option) => option.value));
  return options.map((value) => ({
    label: fallbackByValue.get(value as T) ?? value,
    value: value as T,
  }));
}

function hasParamValue(params: Record<string, unknown>, key: string | undefined) {
  if (!key) return false;
  const value = params[key];
  return value !== undefined && value !== null && String(value) !== "";
}

function getModelSizeSelection(params: Record<string, unknown>, templates: AigcModelParamTemplate[]): SizeSelection {
  const ratioTemplate = findTemplate(templates, ["ratio", "aspectRatio"]);
  const imageSizeTemplate = findTemplate(templates, ["imageSize", "resolution"]);
  return {
    tier: normalizeTemplateOption(String(params[imageSizeTemplate?.paramKey ?? "imageSize"] ?? templateDefault(imageSizeTemplate, ""))),
    ratio: normalizeTemplateOption(String(params[ratioTemplate?.paramKey ?? "ratio"] ?? templateDefault(ratioTemplate, ""))),
  };
}

function formatModelSizeSummary(params: Record<string, unknown>, templates: AigcModelParamTemplate[]) {
  const ratioTemplate = findTemplate(templates, ["ratio", "aspectRatio"]);
  const imageSizeTemplate = findTemplate(templates, ["imageSize", "resolution"]);
  if (ratioTemplate || imageSizeTemplate) {
    const ratio = normalizeTemplateOption(String(params[ratioTemplate?.paramKey ?? "ratio"] ?? templateDefault(ratioTemplate, "1:1")));
    const imageSize = normalizeTemplateOption(String(params[imageSizeTemplate?.paramKey ?? "imageSize"] ?? templateDefault(imageSizeTemplate, "1K")));
    return `${ratio} · ${imageSize}`;
  }
  const sizeTemplate = findTemplate(templates, ["size"]);
  if (sizeTemplate) {
    return normalizeTemplateOption(String(params.size ?? templateDefault(sizeTemplate, "")));
  }
  return "参数";
}

function modelParamKeys(templates: AigcModelParamTemplate[]) {
  return new Set(templates.map((template) => template.paramKey));
}

function filterModelParams(params: ImageTaskParams, templates: AigcModelParamTemplate[]): ImageTaskParams {
  const keys = modelParamKeys(templates);
  return Object.fromEntries(
    Object.entries(params).filter(([key, value]) => value !== undefined && value !== null && value !== "" && keys.has(key))
  ) as ImageTaskParams;
}

function getSizeCapabilityBadge(templates: AigcModelParamTemplate[]) {
  const sizeTemplate = findTemplate(templates, ["size"]);
  const imageSizeTemplate = findTemplate(templates, ["imageSize", "resolution"]);
  if (imageSizeTemplate) {
    const options = templateOptions(imageSizeTemplate, ["1K"]);
    if (options.includes("4K")) return "4K";
    if (options.includes("2K")) return "2K";
    return options[0] ?? "Image";
  }
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

function buildServerInputParams(params: Record<string, unknown>, ids: string[], snapshots: ResultNodeData["inputImages"]) {
  return JSON.stringify({
    ...params,
    inputImageIds: ids,
    inputImageUrls: snapshots
      .map((image) => image.dataUrl)
      .filter((url) => /^https?:\/\//.test(url)),
    inputImages: snapshots.map(({ imageId, fileName, dataUrl, width, height, mimeType }) => ({
      imageId,
      fileName,
      dataUrl,
      width,
      height,
      mimeType,
    })),
  });
}

function getConnectedImagesSignature(nodeId: string, nodes: AppNode[], edges: AppEdge[]) {
  return edges
    .filter((edge) => edge.target === nodeId)
    .map((edge) => {
      const node = nodes.find((n) => n.id === edge.source);
      if (node?.type !== "image" && node?.type !== "sketch") return null;
      const nodeData = node.data as ImageNodeData | SketchNodeData;
      return [
        edge.id,
        edge.source,
        nodeData.previewUrl ?? "",
        nodeData.dataUrl ? nodeData.dataUrl.length : 0,
        nodeData.fileName,
      ].join(":");
    })
    .filter(Boolean)
    .join("|");
}

function getConnectedImages(nodeId: string, nodes: AppNode[], edges: AppEdge[]): ConnectedImage[] {
  const connectedImages: ConnectedImage[] = [];
  for (const edge of edges) {
    if (edge.target !== nodeId) continue;
    const node = nodes.find((n) => n.id === edge.source);
    if (node?.type !== "image" && node?.type !== "sketch") continue;
    const nodeData = node.data as ImageNodeData | SketchNodeData;
    connectedImages.push({
      edgeId: edge.id,
      nodeId: node.id,
      data: {
        previewUrl: nodeData.previewUrl ?? null,
        dataUrl: nodeData.dataUrl ?? "",
        fileName: nodeData.fileName,
      },
    });
  }
  return connectedImages;
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
  const hasImage = Boolean(data.previewUrl || data.dataUrl);

  if (!hasImage) {
    const targetSize = dimensionsFromSize(sizeParam);
    if (targetSize) return scaleToPreview(targetSize.width, targetSize.height);
    return { width: PLACEHOLDER_WIDTH, height: PLACEHOLDER_HEIGHT };
  }

  if (!width || !height) return { width: PLACEHOLDER_WIDTH, height: PLACEHOLDER_HEIGHT };
  return scaleToPreview(width, height);
}

function getVisibleImageLeftInset(image: HTMLImageElement) {
  if (!image.naturalWidth || !image.naturalHeight) return 0;
  const sampleScale = Math.min(1, 240 / Math.max(image.naturalWidth, image.naturalHeight));
  const width = Math.max(1, Math.round(image.naturalWidth * sampleScale));
  const height = Math.max(1, Math.round(image.naturalHeight * sampleScale));
  const canvas = document.createElement("canvas");
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext("2d", { willReadFrequently: true });
  if (!ctx) return 0;
  try {
    ctx.drawImage(image, 0, 0, width, height);
    const data = ctx.getImageData(0, 0, width, height).data;
    for (let x = 0; x < width; x += 1) {
      for (let y = 0; y < height; y += 1) {
        if (data[(y * width + x) * 4 + 3] > 8) {
          return x / sampleScale;
        }
      }
    }
  } catch {
    return 0;
  }
  return 0;
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
  const { user } = useAuth();
  const { setNodes, setEdges, getNode, getNodes, getEdges, getViewport, setViewport } = useReactFlow();
  const composerWheelRef = useComposerWheelPan<HTMLDivElement>(getViewport, setViewport);
  const toolbarWheelRef = useComposerWheelPan<HTMLDivElement>(getViewport, setViewport);
  const updateNodeInternals = useUpdateNodeInternals();
  const [referencePickerPromptId, setReferencePickerPromptId] = useState<string | null>(null);
  const zoom = useStore((s) => s.transform[2] || 1);
  const selectedNodeCount = useStore((s) => s.nodes.reduce((count, node) => count + (node.selected ? 1 : 0), 0));
  const connectedImagesSignature = useStore((s) => getConnectedImagesSignature(id, s.nodes as AppNode[], s.edges as AppEdge[]));
  const isReferencedByActivePrompt = useStore((s) => Boolean(
    referencePickerPromptId &&
      referencePickerPromptId !== id &&
      (s.edges as AppEdge[]).some((edge) => edge.source === id && edge.target === referencePickerPromptId)
  ));
  const [modelPopoverOpen, setModelPopoverOpen] = useState(false);
  const [paramsPopoverOpen, setParamsPopoverOpen] = useState(false);
  const modelBtnRef = useRef<HTMLButtonElement>(null);
  const paramsBtnRef = useRef<HTMLButtonElement>(null);
  const modelPopoverRef = useRef<HTMLDivElement>(null);
  const paramsPopoverRef = useRef<HTMLDivElement>(null);
  const nodeMenuRef = useRef<HTMLDivElement>(null);
  const activeRunPollRef = useRef<string | null>(null);
  const [measuredSize, setMeasuredSize] = useState<{ width: number; height: number } | null>(null);
  const [visibleImageInset, setVisibleImageInset] = useState<{ left: number } | null>(null);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [nodeMenu, setNodeMenu] = useState<{ x: number; y: number; visible: boolean }>({
    x: 0,
    y: 0,
    visible: false,
  });
  const [nodeMenuPos, setNodeMenuPos] = useState<{ x: number; y: number } | null>(null);
  const createdAtMs = useMemo(() => new Date(data.generationStartedAt ?? data.createdAt).getTime(), [data.createdAt, data.generationStartedAt]);
  const [now, setNow] = useState(createdAtMs);
  const [startedAtMs, setStartedAtMs] = useState(() => Number.isFinite(createdAtMs) ? createdAtMs : Date.now());
  const effectiveStartedAtMs = startedAtMs > 0 ? startedAtMs : createdAtMs;

  const status = data.status ?? "idle";
  const isGenerating = status === "pending";
  const kind = data.kind ?? "draft";
  const canCompose = kind !== "uploaded";
  const modelId = data.modelId ?? "";
  const selectedAigcModelId = data.aigcModelId;
  const params = data.params ?? DEFAULT_PROMPT_DATA.params;
  const prompt = data.prompt ?? "";
  const connectedImages = useMemo(() => {
    void connectedImagesSignature;
    const currentNodes = getNodes() as AppNode[];
    const currentEdges = getEdges() as AppEdge[];
    return getConnectedImages(id, currentNodes, currentEdges);
  }, [connectedImagesSignature, getEdges, getNodes, id]);
  const mentionOptions = useMemo<PromptMentionOption[]>(
    () =>
      connectedImages.map((image, index) => ({
        id: image.nodeId,
        label: `图片 ${index + 1}`,
        token: createPromptMentionToken(image.nodeId),
        thumbnailUrl: image.data.previewUrl || image.data.dataUrl,
      })),
    [connectedImages]
  );
  const generationCapability = connectedImages.length > 0 ? "IMAGE_TO_IMAGE" : "TEXT_TO_IMAGE";
  const aigcModels = useAigcModels({ type: 2, capability: generationCapability, preferredModelId: selectedAigcModelId, params });
  const storedAigcModel = aigcModels.models.find((item) => item.id === selectedAigcModelId);
  const activeAigcModel = storedAigcModel ?? aigcModels.selectedModel;
  const activeModelName = activeAigcModel?.name ?? data.modelName ?? "选择模型";
  const activeProviderModel = activeAigcModel?.model ?? data.providerModel ?? modelId;
  const activeAigcModelId = activeAigcModel?.id;
  const ratioTemplate = useMemo(() => findTemplate(aigcModels.templates, ["ratio", "aspectRatio"]), [aigcModels.templates]);
  const imageSizeTemplate = useMemo(() => findTemplate(aigcModels.templates, ["imageSize", "resolution"]), [aigcModels.templates]);
  const sizeTemplate = useMemo(() => findTemplate(aigcModels.templates, ["size"]), [aigcModels.templates]);
  const qualityTemplate = useMemo(() => findTemplate(aigcModels.templates, ["quality"]), [aigcModels.templates]);
  const formatTemplate = useMemo(() => findTemplate(aigcModels.templates, ["output_format"]), [aigcModels.templates]);
  const moderationTemplate = useMemo(() => findTemplate(aigcModels.templates, ["moderation"]), [aigcModels.templates]);
  const ratioOptions = useMemo(() => templateOptions(ratioTemplate, RATIOS), [ratioTemplate]);
  const tierOptions = useMemo(() => templateOptions(imageSizeTemplate, TIERS), [imageSizeTemplate]);
  const qualityOptions = useMemo(() => segmentedOptions(qualityTemplate, QUALITY_OPTIONS), [qualityTemplate]);
  const formatOptions = useMemo(() => segmentedOptions(formatTemplate, FORMAT_OPTIONS), [formatTemplate]);
  const moderationOptions = useMemo(() => segmentedOptions(moderationTemplate, MODERATION_OPTIONS), [moderationTemplate]);
  const selectedModelCapabilityBadge = useMemo(() => getSizeCapabilityBadge(aigcModels.templates), [aigcModels.templates]);
  const costLabel = aigcModels.priceLoading ? "…" : formatCost(aigcModels.price?.salePrice);
  const imageSrc = data.previewUrl || data.dataUrl;
  const mediaStoreScope = useMemo(() => {
    const urlProjectId = typeof window === "undefined" ? null : new URLSearchParams(window.location.search).get("projectId");
    const dataProjectId = typeof data.projectId === "string" || typeof data.projectId === "number" ? data.projectId : null;
    return {
      ownerKey: user?.id ?? null,
      projectId: dataProjectId ?? urlProjectId,
    };
  }, [data.projectId, user?.id]);
  const generatingStatusLabel = getGenerationStatusLabel(data.taskStatus || data.upstreamStatus || "RUNNING");
  const sizeSelection = useMemo(() => getModelSizeSelection(params, aigcModels.templates), [aigcModels.templates, params]);
  const effectiveParams = useMemo(() => filterModelParams(params, aigcModels.templates), [aigcModels.templates, params]);
  const sizeOptions = useMemo(() => templateOptions(sizeTemplate, []), [sizeTemplate]);
  const showSizeSection = Boolean(sizeTemplate || ratioTemplate || imageSizeTemplate);
  const showQualityControl = Boolean(qualityTemplate);
  const showFormatControl = Boolean(formatTemplate);
  const showModerationControl = Boolean(moderationTemplate);
  const showOutputSection = showQualityControl || showFormatControl || showModerationControl;
  const canGenerate = Boolean(prompt.trim()) && !isGenerating && !aigcModels.loading && !aigcModels.templateLoading && Boolean(activeAigcModelId);

  const isOnlySelectedNode = selected && selectedNodeCount === 1;
  const showNodeActions = selectedNodeCount <= 1;
  const fixedUiScale = 1 / zoom;

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
    (patch: Partial<ImageNodeData>, options?: { flush?: boolean }) => {
      setNodes((nds) =>
        nds.map((n) => (n.id === id ? { ...n, data: { ...n.data, ...patch } } : n))
      );
      window.dispatchEvent(new CustomEvent<NodeDataPatchEventDetail>("copse:node-data-patch", {
        detail: { nodeId: id, patch, flush: options?.flush },
      }));
    },
    [id, setNodes]
  );

  const waitAndApplyServerRun = useCallback(async (projectId: string | number, taskId: number, startedAt: string) => {
    const pollKey = `${projectId}:${id}:${taskId}`;
    if (activeRunPollRef.current === pollKey) return;
    activeRunPollRef.current = pollKey;
    try {
      const result = await waitCanvasNodeRunResult(projectId, id, {
        taskId,
        baseVersion: 0,
        nodeType: "image",
      });
      const patch = getCanvasNodeRunPatch(result, id);
      if (patch) updateData(patch as Partial<ImageNodeData>, { flush: true });
    } catch (error) {
      updateData({
        status: "failed",
        taskId: String(taskId),
        errorMessage: error instanceof Error ? error.message : "图片任务同步失败",
        safetyStatus: null,
        safetyReason: null,
        generationCompletedAt: new Date().toISOString(),
        elapsedMs: Date.now() - new Date(startedAt).getTime(),
      }, { flush: true });
    } finally {
      if (activeRunPollRef.current === pollKey) {
        activeRunPollRef.current = null;
      }
    }
  }, [id, updateData]);

  useEffect(() => {
    if (status !== "pending" || !data.taskId) return;
    const projectId = new URLSearchParams(window.location.search).get("projectId");
    if (!isServerCanvasProjectId(projectId)) return;
    const taskId = Number(data.taskId);
    if (!Number.isFinite(taskId)) return;
    void waitAndApplyServerRun(projectId, taskId, data.generationStartedAt ?? data.createdAt);
  }, [data.createdAt, data.generationStartedAt, data.taskId, status, waitAndApplyServerRun]);

  const deleteNode = useCallback(() => {
    setNodeMenu((prev) => ({ ...prev, visible: false }));
    setNodes((nds) => nds.filter((n) => n.id !== id));
    setEdges((eds) => eds.filter((e) => e.source !== id && e.target !== id));
    deleteImage(data.imageId, mediaStoreScope).catch(() => {});
  }, [data.imageId, id, mediaStoreScope, setEdges, setNodes]);

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
    saveImage(duplicateData, mediaStoreScope).catch(() => {});
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
  }, [data, getNode, id, mediaStoreScope, setNodes]);

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

  useEffect(() => {
    if (aigcModels.templateLoading || aigcModels.templates.length === 0) return;
    const patch: Record<string, unknown> = {};
    for (const template of aigcModels.templates) {
      if (!BUILT_IN_IMAGE_PARAM_KEYS.has(template.paramKey)) continue;
      if (hasParamValue(params, template.paramKey)) continue;
      const defaultValue = template.defaultValue ? normalizeTemplateOption(template.defaultValue) : "";
      const options = templateOptions(template, []);
      const nextValue = defaultValue || options[0];
      if (nextValue) patch[template.paramKey] = nextValue;
    }
    if (Object.keys(patch).length > 0) updateParams(patch);
  }, [aigcModels.templateLoading, aigcModels.templates, params, updateParams]);

  useEffect(() => {
    if (aigcModels.loading || aigcModels.models.length === 0) return;
    if (selectedAigcModelId && aigcModels.models.some((model) => model.id === selectedAigcModelId)) return;
    const nextModel = aigcModels.selectedModel ?? aigcModels.models[0];
    if (!nextModel) return;
    updateData({
      modelId: String(nextModel.id),
      providerModel: nextModel.model,
      modelName: nextModel.name,
      aigcModelId: nextModel.id,
    });
  }, [aigcModels.loading, aigcModels.models, aigcModels.selectedModel, selectedAigcModelId, updateData]);

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

  const handleTierChange = useCallback(
    (tier: string) => {
      if (imageSizeTemplate) {
        updateParams({ [imageSizeTemplate?.paramKey ?? "imageSize"]: tier });
      }
    },
    [imageSizeTemplate, updateParams]
  );

  const handleRatioChange = useCallback(
    (ratio: string) => {
      if (ratioTemplate) {
        updateParams({ [ratioTemplate?.paramKey ?? "ratio"]: ratio });
      }
    },
    [ratioTemplate, updateParams]
  );

  const handleSizeChange = useCallback(
    (size: string) => {
      if (sizeTemplate) updateParams({ [sizeTemplate.paramKey]: size });
    },
    [sizeTemplate, updateParams]
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
    const cleanPrompt = promptValueToSubmitPrompt(prompt, mentionOptions).trim();
    if (!cleanPrompt || isGenerating || aigcModels.loading || aigcModels.templateLoading) return;

    const startedAt = new Date().toISOString();
    const runStartedAtMs = Date.now();
    setStartedAtMs(runStartedAtMs);
    setNow(runStartedAtMs);
    const ctx = { nodes: getNodes() as AppNode[], edges: getEdges() as AppEdge[] };
    const { snapshots, ids, mode } = resolveInputImages(id, ctx);
    const runModel = activeAigcModel && aigcModels.models.some((model) => model.id === activeAigcModel.id)
      ? activeAigcModel
      : aigcModels.selectedModel;
    const runAigcModelId = runModel?.id;
    const runModelName = runModel?.name ?? activeModelName;
    const runProviderModel = runModel?.model ?? activeProviderModel;
    const resultDraft: ResultNodeData = {
      taskId: null,
      promptNodeId: id,
      prompt: cleanPrompt,
      params: effectiveParams,
      modelId,
      providerModel: runProviderModel,
      aigcModelId: runAigcModelId,
      modelName: runModelName,
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
      providerModel: runProviderModel,
      aigcModelId: runAigcModelId,
      modelName: runModelName,
      params: effectiveParams,
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
    if (isServerCanvasProjectId(projectId) && runAigcModelId) {
      const clientId = `node_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
      try {
        const run = await canvasNodeRunApi.runNode(projectId, id, {
          clientId,
          baseVersion: 0,
          runId: clientId,
          nodeType: "image",
          generateType: "IMAGE",
          generateMode: mode === "edit" ? "IMAGE_TO_IMAGE" : "TEXT_TO_IMAGE",
          modelId: runAigcModelId,
          prompt: cleanPrompt,
          inputParams: buildServerInputParams(effectiveParams, ids, snapshots),
          sync: false,
        });
        updateData({
          taskId: String(run.taskId),
          upstreamStatus: run.status,
        }, { flush: true });
        await waitAndApplyServerRun(projectId, run.taskId, startedAt);
        return;
      } catch (error) {
        updateData({
          status: "failed",
          taskId: null,
          errorMessage: error instanceof Error ? error.message : "图片任务提交失败",
          safetyStatus: null,
          safetyReason: null,
          generationCompletedAt: new Date().toISOString(),
          elapsedMs: Date.now() - new Date(startedAt).getTime(),
        });
        return;
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
      const outputAssetId = updates.assetIdList?.[0] ?? null;
      let previewUrl = updates.imageUrls[0];
      let assetUrlExpireTime: string | null = null;
      if (outputAssetId) {
        try {
          const asset = await getMyAsset(outputAssetId);
          previewUrl = getAssetPreviewUrl(asset) || previewUrl;
          assetUrlExpireTime = getAssetPreviewExpireTime(asset) ?? null;
        } catch {
        }
      }
      nextData.kind = "generated";
      nextData.assetId = outputAssetId;
      nextData.outputAssetId = outputAssetId;
      nextData.previewUrl = previewUrl;
      nextData.outputPreviewUrl = previewUrl;
      nextData.assetUrlExpireTime = assetUrlExpireTime;
      nextData.dataUrl = "";
      nextData.fileName = "generated-image.png";
      nextData.mimeType = effectiveParams.output_format === "jpeg" ? "image/jpeg" : `image/${effectiveParams.output_format}`;
    }

    setNodes((nds) =>
      nds.map((n) => {
        if (n.id !== id) return n;
        const merged = { ...(n.data as ImageNodeData), ...nextData };
        saveImage(merged, mediaStoreScope).catch(() => {});
        return { ...n, data: merged };
      })
    );
  }, [activeAigcModel, activeModelName, activeProviderModel, aigcModels.loading, aigcModels.models, aigcModels.selectedModel, aigcModels.templateLoading, effectiveParams, getEdges, getNodes, id, isGenerating, mediaStoreScope, mentionOptions, modelId, prompt, setNodes, updateData, waitAndApplyServerRun]);

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

  const elapsedMs = data.elapsedMs ?? (isGenerating && Number.isFinite(effectiveStartedAtMs) ? Math.max(0, now - effectiveStartedAtMs) : null);
  const safetyStatus = normalizeSafetyStatus(data.safetyStatus) !== "idle" ? normalizeSafetyStatus(data.safetyStatus) : normalizeSafetyStatusFromError(data.safetyReason ?? data.errorMessage);
  const safety = getSafetyCopy(safetyStatus, "generation");
  const previewItem = useMemo(() => imageNodeToMediaPreview({ ...data, elapsedMs }), [data, elapsedMs]);
  const displaySize = getDisplaySize(data, measuredSize, params.size);
  const displayScale = (measuredSize?.width ?? data.width)
    ? displaySize.width / (measuredSize?.width ?? data.width ?? displaySize.width)
    : 1;
  const titleLeft = Math.round((visibleImageInset?.left ?? 0) * displayScale);
  const titleWidth = Math.max(80, displaySize.width - Math.round((visibleImageInset?.left ?? 0) * displayScale));
  const pickerActiveForThisNode = referencePickerPromptId === id;
  useEffect(() => {
    const frame = window.requestAnimationFrame(() => updateNodeInternals(id));
    const timeout = window.setTimeout(() => updateNodeInternals(id), 260);

    return () => {
      window.cancelAnimationFrame(frame);
      window.clearTimeout(timeout);
    };
  }, [displaySize.width, displaySize.height, id, updateNodeInternals]);

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
        style={{ width: displaySize.width, height: displaySize.height }}
      >
        <div className="relative overflow-visible" style={{ width: displaySize.width, height: displaySize.height }}>
          <AnimatePresence>
            {isOnlySelectedNode && !dragging && previewItem && (
              <SelectedMediaToolbar
                canDownload={Boolean(previewItem.url)}
                onDownload={handlePreviewDownload}
                onOpenPreview={() => setPreviewOpen(true)}
                uiScale={fixedUiScale}
                wheelRef={toolbarWheelRef}
                style={{
                  left: displaySize.width / 2,
                  top: -54,
                  pointerEvents: "auto",
                }}
              />
            )}
          </AnimatePresence>
          <CanvasNodeTitle left={titleLeft} maxWidth={titleWidth}>
            <ImageIcon className="size-4" />
            <EditableNodeTitle
              value={data.fileName}
              fallback="Image"
              onCommit={(fileName) => updateData({ fileName }, { flush: true })}
            />
          </CanvasNodeTitle>

          <motion.div
            className="absolute"
            initial={false}
            style={{
              left: 0,
              top: 0,
              width: displaySize.width,
              height: displaySize.height,
            }}
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
              animate={{ opacity: 1, scale: 1 }}
              transition={{
                opacity: { duration: 0.18, ease: "easeOut" },
                scale: { duration: 0.18, ease: "easeOut" },
              }}
              className={cn(
                "canvas-node-drag-handle group relative overflow-visible rounded-xl",
                imageSrc ? "bg-transparent shadow-none" : "border border-border-warm bg-background shadow-[0_1px_3px_rgba(0,0,0,0.06)]",
                selected && (imageSrc ? "ring-2 ring-off-white/80" : "border-charcoal/60 ring-2 ring-charcoal/35"),
                isReferencedByActivePrompt && (imageSrc ? "ring-2 ring-off-white" : "border-charcoal ring-2 ring-charcoal/30")
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
                        setMeasuredSize((current) => (
                          current?.width === image.naturalWidth && current.height === image.naturalHeight
                            ? current
                            : { width: image.naturalWidth, height: image.naturalHeight }
                        ));
                        setVisibleImageInset((current) => current ?? { left: getVisibleImageLeftInset(image) });
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
                      className="absolute inset-0 flex items-center justify-center overflow-hidden bg-charcoal/35 text-off-white"
                    >
                      <motion.div
                        className="absolute inset-y-0 w-1/2 -skew-x-12 bg-gradient-to-r from-transparent via-white/55 to-transparent blur-[1px]"
                        initial={{ x: "-140%" }}
                        animate={{ x: ["-140%", "260%"] }}
                        transition={{ duration: 1.8, repeat: Infinity, ease: "easeInOut" }}
                      />
                      <div className="relative z-10 flex flex-col items-center gap-1 rounded-full bg-charcoal/45 px-4 py-2 text-center shadow-[0_8px_28px_rgba(0,0,0,0.18)] backdrop-blur-md">
                        <span className="text-xs font-medium">{generatingStatusLabel}</span>
                        <span className="font-mono text-[11px] tabular-nums text-off-white/80">{formatElapsed(elapsedMs)}</span>
                      </div>
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
            ref={composerWheelRef}
            className="nodrag nowheel absolute rounded-xl border border-border-warm bg-background p-4 shadow-[0_8px_24px_rgba(28,28,28,0.08)]"
            style={{
                width: COMPOSER_WIDTH,
                left: (displaySize.width - COMPOSER_WIDTH) / 2,
                top: displaySize.height + 12 * fixedUiScale,
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
              <PromptMentionInput
                value={prompt}
                onChange={(nextPrompt) => updateData({ prompt: nextPrompt })}
                mentions={mentionOptions}
                disabled={isGenerating}
                placeholder="Describe anything you want to generate"
                minHeightClassName="min-h-[130px]"
                onSubmit={() => {
                  if (canGenerate) void handleGenerate();
                }}
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
                      data-composer-local-wheel="true"
                      initial={{ opacity: 0, y: 4, scale: 0.98 }}
                      animate={{ opacity: 1, y: 0, scale: 1 }}
                      exit={{ opacity: 0, y: 4, scale: 0.98 }}
                      transition={{ duration: 0.14, ease: "easeOut" }}
                      className="absolute bottom-full left-0 z-[260] mb-2 w-[280px] overflow-hidden rounded-xl border border-border-warm bg-background p-2 shadow-[0_4px_12px_rgba(0,0,0,0.1)]"
                    >
                      <div className="max-h-[320px] overflow-y-auto">
                        {aigcModels.models.length > 0 ? aigcModels.models.map((model) => {
                          const isSelected = aigcModels.selectedModelId === model.id;
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
                        }) : (
                          <div className="px-3 py-4 text-sm text-muted-gray">暂无可用模型</div>
                        )}
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
                    <span>{formatModelSizeSummary(params, aigcModels.templates)}</span>
                  </button>
                  <AnimatePresence>
                    {paramsPopoverOpen && (
                    <motion.div
                      ref={paramsPopoverRef}
                      data-composer-local-wheel="true"
                      initial={{ opacity: 0, y: 4, scale: 0.98 }}
                      animate={{ opacity: 1, y: 0, scale: 1 }}
                      exit={{ opacity: 0, y: 4, scale: 0.98 }}
                      transition={{ duration: 0.14, ease: "easeOut" }}
                      className="absolute bottom-full left-0 z-[260] mb-2 w-[440px] rounded-xl border border-border-warm bg-background p-4 shadow-[0_4px_12px_rgba(0,0,0,0.1)]"
                    >
                      <div className="mb-4 flex items-center justify-between">
                        <div>
                          <p className="text-sm font-medium text-charcoal">生成参数</p>
                          <p className="mt-0.5 text-[11px] text-muted-gray">当前：{formatModelSizeSummary(params, aigcModels.templates)}</p>
                        </div>
                        <button type="button" onClick={() => setParamsPopoverOpen(false)} className="flex size-7 items-center justify-center rounded-full text-muted-gray transition-colors hover:bg-muted hover:text-charcoal" aria-label="关闭参数">
                          <X className="size-4" />
                        </button>
                      </div>

                      {showSizeSection && (
                        <section className="space-y-3">
                          <p className="text-xs font-medium text-muted-gray">Size</p>
                          {sizeTemplate && sizeOptions.length > 0 && (
                            <div className="grid grid-cols-3 gap-2">
                              {sizeOptions.map((size) => (
                                <button
                                  key={size}
                                  type="button"
                                  onClick={() => handleSizeChange(size)}
                                  className={cn(
                                    "rounded-lg bg-muted px-3 py-2 text-xs font-medium text-muted-gray transition-colors hover:text-charcoal",
                                    normalizeTemplateOption(String(params[sizeTemplate.paramKey] ?? templateDefault(sizeTemplate, ""))) === size && "bg-background text-charcoal shadow-sm"
                                  )}
                                >
                                  {size}
                                </button>
                              ))}
                            </div>
                          )}
                          {imageSizeTemplate && tierOptions.length > 0 && (
                            <div className="grid grid-cols-3 gap-2">
                              {tierOptions.map((tier) => (
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
                          )}
                          {ratioTemplate && ratioOptions.length > 0 && (
                            <div className="grid grid-cols-4 gap-1.5">
                              {ratioOptions.map((ratio) => (
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
                          )}
                        </section>
                      )}

                      {showOutputSection && (
                        <section className="mt-5 space-y-3">
                          <p className="text-xs font-medium text-muted-gray">Output</p>

                          <div className="space-y-2">
                            {showQualityControl && (
                              <ParamSegmented label="质量" value={params.quality} options={qualityOptions} onChange={(value) => updateParams({ quality: value })} />
                            )}
                            {showFormatControl && (
                              <ParamSegmented label="格式" value={params.output_format} options={formatOptions} onChange={handleFormatChange} />
                            )}
                            {showModerationControl && (
                              <ParamSegmented label="审核" value={params.moderation} options={moderationOptions} onChange={(value) => updateParams({ moderation: value })} />
                            )}
                          </div>
                        </section>
                      )}
                      {aigcModels.templateLoading && (
                        <p className="mt-4 text-[11px] text-muted-gray">参数加载中...</p>
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
                  disabled={!canGenerate}
                  className="flex h-11 items-center gap-3 rounded-full bg-charcoal/90 py-1 pl-4 pr-1 text-[#fcfbf8] shadow-[0_4px_12px_rgba(0,0,0,0.18)] transition-opacity active:opacity-85 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-black/80 dark:text-[#f4efe6]"
                  aria-label={isGenerating ? "生成中" : "开始生成"}
                >
                  <span
                    className={cn(
                      "flex items-center gap-2 text-sm font-semibold tabular-nums text-[#fcfbf8] transition-colors duration-200 ease-in-out dark:text-[#f4efe6]",
                      aigcModels.priceLoading && "text-[#fcfbf8]/70 dark:text-[#f4efe6]/70"
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
