/* eslint-disable @next/next/no-img-element */
"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { Node, NodeProps } from "@xyflow/react";
import { useReactFlow, useStore, useUpdateNodeInternals } from "@xyflow/react";
import { AnimatePresence, motion } from "motion/react";
import {
  ArrowUp,
  Camera,
  Check,
  Gem,
  ImageIcon,
  Loader2,
  Pause,
  Play,
  Plus,
  SlidersHorizontal,
  Sparkles,
  Video,
  Volume2,
  VolumeX,
  X,
} from "lucide-react";
import type {
  AppEdge,
  AppNode,
  ImageNodeData,
  NodeDataPatchEventDetail,
  ReferencePickerEventDetail,
  SketchNodeData,
  VideoFrameCaptureEventDetail,
  VideoGenerationMode,
  VideoNodeData,
} from "./types";
import { NodeCreateHandle } from "./NodeCreateHandle";
import { generationApi } from "@/features/generation/generation-api";
import { waitGenerationResult } from "@/features/generation/generation-poll";
import type { AigcModelParamTemplate } from "@/features/generation/model-api";
import { filterAigcModelParams } from "@/features/generation/aigc-model-param-utils";
import { useAigcModels } from "@/features/generation/use-aigc-models";
import { DynamicParamForm } from "@/features/generation/DynamicParamForm";
import {
  canvasNodeRunApi,
  getCanvasNodeRunPatch,
  isServerCanvasProjectId,
  waitCanvasNodeRunResult,
} from "@/features/canvas/canvas-node-run-api";
import {
  captureVideoFrameAsset,
  getMyAsset,
} from "@/features/assets/asset-api";
import {
  getAssetPreviewExpireTime,
  getAssetPreviewUrl,
} from "@/features/assets/asset-dictionaries";
import { MediaPreviewDialog } from "@/features/media-preview/MediaPreviewDialog";
import { SelectedMediaToolbar } from "@/features/media-preview/SelectedMediaToolbar";
import {
  downloadMedia,
  videoNodeToMediaPreview,
} from "@/features/media-preview/media-preview-utils";
import { cn } from "@/lib/utils";
import { EditableNodeTitle } from "./EditableNodeTitle";
import { CanvasNodeTitle } from "./CanvasNodeTitle";
import {
  createPromptMentionToken,
  PromptMentionInput,
  promptValueToSubmitPrompt,
  useComposerWheelPan,
  type PromptMentionOption,
} from "./PromptMentionInput";

type VideoNodeProps = NodeProps<Node<VideoNodeData, "video">>;

const CARD_WIDTH = 420;
const CARD_HEIGHT = 236;
const PREVIEW_SLOT_WIDTH = 420;
const PREVIEW_SLOT_HEIGHT = 420;
const COMPOSER_WIDTH = 680;
const SEEDANCE_MODEL_NAME = "Seedance 2.0";
const WAN_MODEL_ID = "wan2.2-ti2v-5b";
const FRAME_CAPTURE_EPSILON_SEC = 0.05;

function normalizeTemplateOption(option: unknown) {
  let value = String(option ?? "").trim();
  for (let i = 0; i < 3; i += 1) {
    try {
      const parsed = JSON.parse(value);
      if (typeof parsed !== "string") break;
      value = parsed.trim();
    } catch {
      break;
    }
  }
  return value
    .replace(/\\/g, "")
    .replace(/^"+|"+$/g, "")
    .trim();
}

function templateDefault(
  template: AigcModelParamTemplate | undefined,
  fallback = "",
) {
  const normalized = template?.defaultValue
    ? normalizeTemplateOption(template.defaultValue)
    : "";
  const option = (template?.options ?? [])
    .map(normalizeTemplateOption)
    .find(Boolean);
  return normalized || option || fallback;
}

function hasParamValue(
  params: Record<string, unknown>,
  key: string | undefined,
) {
  if (!key) return false;
  const value = params[key];
  return value !== undefined && value !== null && String(value) !== "";
}

function formatCost(value: number | null | undefined) {
  if (value == null || !Number.isFinite(value)) return "1x";
  return Number.isInteger(value)
    ? String(value)
    : value.toFixed(2).replace(/\.?0+$/, "");
}

function getReferenceAssetId(data: ImageNodeData | SketchNodeData) {
  if ("assetId" in data && typeof data.assetId === "number")
    return data.assetId;
  if ("outputAssetId" in data && typeof data.outputAssetId === "number")
    return data.outputAssetId;
  return null;
}

async function resolveReferenceImagesForSubmit(
  images: { data: ImageNodeData | SketchNodeData }[],
) {
  return Promise.all(
    images.map(async ({ data }) => {
      const assetId = getReferenceAssetId(data);
      if (assetId) {
        try {
          const asset = await getMyAsset(assetId);
          const url = getAssetPreviewUrl(asset) || asset.fileUrl;
          if (url) return url;
        } catch {
          // Fall back to the node snapshot below; generation should still work for local data URLs.
        }
      }
      return data.dataUrl || data.previewUrl || "";
    }),
  );
}

function formatElapsed(ms: number | null | undefined) {
  if (!ms || ms < 0) return "0s";
  const seconds = Math.floor(ms / 1000);
  if (seconds < 60) return `${seconds}s`;
  return `${Math.floor(seconds / 60)}m ${seconds % 60}s`;
}

function isQueuedStatus(status: string | null | undefined) {
  const normalizedStatus = status?.toUpperCase();
  return (
    normalizedStatus === "QUEUED" ||
    normalizedStatus === "CREATED" ||
    normalizedStatus === "SUBMITTING" ||
    normalizedStatus === "SUBMITTED" ||
    normalizedStatus === "CALLBACK_WAITING"
  );
}

function isRunningStatus(status: string | null | undefined) {
  const normalizedStatus = status?.toUpperCase();
  return (
    normalizedStatus === "RUNNING" ||
    normalizedStatus === "PROCESSING" ||
    normalizedStatus === "SYNCING" ||
    normalizedStatus === "DOWNLOADING" ||
    normalizedStatus === "ASSET_CREATING"
  );
}

function ratioToSize(ratio: VideoNodeData["ratio"]) {
  const [w, h] = ratio.split(":").map(Number);
  if (!w || !h) return { width: CARD_WIDTH, height: CARD_HEIGHT };
  const scale = Math.min(PREVIEW_SLOT_WIDTH / w, PREVIEW_SLOT_HEIGHT / h);
  return {
    width: Math.round(w * scale),
    height: Math.round(h * scale),
  };
}

function wanSizeToPreview(size: VideoNodeData["size"]) {
  const [w, h] = (size ?? "1280*704").split("*").map(Number);
  if (!w || !h) return ratioToSize("16:9");
  const scale = Math.min(PREVIEW_SLOT_WIDTH / w, PREVIEW_SLOT_HEIGHT / h);
  return {
    width: Math.round(w * scale),
    height: Math.round(h * scale),
  };
}

function scaleVideoToPreview(width: number, height: number) {
  const scale = Math.min(
    1,
    PREVIEW_SLOT_WIDTH / width,
    PREVIEW_SLOT_HEIGHT / height,
  );
  return {
    width: Math.max(160, Math.round(width * scale)),
    height: Math.max(120, Math.round(height * scale)),
  };
}

function formatTime(seconds: number | null | undefined) {
  if (!seconds || !Number.isFinite(seconds) || seconds < 0) return "0:00";
  const total = Math.floor(seconds);
  return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, "0")}`;
}

function getRangeProgress(current: number, duration: number) {
  if (!Number.isFinite(current) || !Number.isFinite(duration) || duration <= 0)
    return "0%";
  return `${Math.min(100, Math.max(0, (current / duration) * 100))}%`;
}

function dataUrlToMimeType(dataUrl: string) {
  const match = dataUrl.match(/^data:([^;]+);/);
  return match?.[1] || "image/png";
}

function getVideoAssetId(data: VideoNodeData) {
  if (typeof data.assetId === "number") return data.assetId;
  if (typeof data.outputAssetId === "number") return data.outputAssetId;
  return null;
}

function getPatchVideoAssetId(patch: Record<string, unknown>) {
  const value = patch.outputAssetId ?? patch.assetId;
  const assetId = Number(value);
  return Number.isFinite(assetId) && assetId > 0 ? assetId : null;
}

function stopCanvasSelection(event: React.SyntheticEvent) {
  event.stopPropagation();
}

function getDisplaySize(data: VideoNodeData) {
  if (data.videoUrl && data.width && data.height) {
    return scaleVideoToPreview(data.width, data.height);
  }
  if (data.provider === "wan" || data.modelId === WAN_MODEL_ID) {
    return wanSizeToPreview(data.size);
  }
  return ratioToSize(data.ratio);
}

const VIDEO_MODE_OPTIONS: {
  mode: VideoGenerationMode;
  label: string;
  minRefs: number;
  maxRefs: number;
}[] = [
  { mode: "TEXT_TO_VIDEO", label: "文生视频", minRefs: 0, maxRefs: 0 },
  { mode: "IMAGE_TO_VIDEO", label: "图生视频", minRefs: 1, maxRefs: 1 },
  { mode: "FIRST_LAST_FRAME_VIDEO", label: "首尾帧", minRefs: 2, maxRefs: 2 },
  { mode: "MULTI_REF_VIDEO", label: "多参考", minRefs: 1, maxRefs: 9 },
];

/* 判断模型是否支持各种模式 */
function deriveVideoMode(
  explicitMode: VideoGenerationMode | null | undefined,
  refCount: number,
  capabilities?: string[] | null,
): VideoGenerationMode {
  if (
    explicitMode &&
    modelSupportsVideoMode(capabilities, explicitMode) &&
    isVideoModeSelectable(explicitMode, refCount)
  ) {
    return explicitMode;
  }
  const candidates: VideoGenerationMode[] =
    refCount === 0
      ? ["TEXT_TO_VIDEO"]
      : refCount === 1
        ? ["IMAGE_TO_VIDEO", "MULTI_REF_VIDEO", "FIRST_LAST_FRAME_VIDEO"]
        : ["MULTI_REF_VIDEO", "FIRST_LAST_FRAME_VIDEO"];
  const supported = candidates.find((mode) =>
    modelSupportsVideoMode(capabilities, mode),
  );
  if (supported) return supported;
  if (refCount === 0) return "TEXT_TO_VIDEO";
  if (refCount === 1) return "IMAGE_TO_VIDEO";
  return "MULTI_REF_VIDEO";
}

function modelSupportsVideoMode(
  capabilities: string[] | null | undefined,
  mode: VideoGenerationMode,
) {
  return !capabilities?.length || capabilities.includes(mode);
}

function isVideoModeSelectable(mode: VideoGenerationMode, refCount: number) {
  if (mode === "TEXT_TO_VIDEO") return refCount === 0;
  if (mode === "FIRST_LAST_FRAME_VIDEO") return refCount >= 1 && refCount <= 2;
  const option = VIDEO_MODE_OPTIONS.find((item) => item.mode === mode);
  if (!option) return false;
  return refCount >= option.minRefs && refCount <= option.maxRefs;
}

function isVideoModeAvailable(mode: VideoGenerationMode, refCount: number) {
  return isVideoModeSelectable(mode, refCount);
}

function validateVideoGeneration(
  mode: VideoGenerationMode,
  refCount: number,
): string | null {
  switch (mode) {
    case "TEXT_TO_VIDEO":
      if (refCount > 0) return "文生视频不支持参考图，请先断开连线或切换模式。";
      break;
    case "IMAGE_TO_VIDEO":
      if (refCount === 0) return "图生视频需要连接 1 张参考图。";
      if (refCount > 1)
        return "图生视频仅支持 1 张参考图，请移除多余的连线或切换模式。";
      break;
    case "FIRST_LAST_FRAME_VIDEO":
      if (refCount < 2) return "首尾帧视频需要 2 张图片（首帧 + 尾帧）。";
      if (refCount > 2)
        return "首尾帧视频仅支持 2 张图片，请移除多余的连线或切换到多参考模式。";
      break;
    case "MULTI_REF_VIDEO":
      if (refCount === 0) return "多参考模式至少需要 1 张参考图。";
      if (refCount > 9) return "最多支持 9 张参考图。";
      break;
  }
  return null;
}

function getModeOption(mode: VideoGenerationMode) {
  return VIDEO_MODE_OPTIONS.find((option) => option.mode === mode);
}

function getOrderedReferences(
  mode: VideoGenerationMode,
  referenceImages: {
    edgeId: string;
    nodeId: string;
    data: ImageNodeData | SketchNodeData;
  }[],
  data: VideoNodeData,
) {
  if (mode === "FIRST_LAST_FRAME_VIDEO" && referenceImages.length === 2) {
    const firstEdgeId = data.firstFrameEdgeId;
    const lastEdgeId = data.lastFrameEdgeId;
    if (firstEdgeId && lastEdgeId) {
      const first = referenceImages.find((img) => img.edgeId === firstEdgeId);
      const last = referenceImages.find((img) => img.edgeId === lastEdgeId);
      if (first && last) return [first, last];
    }
    return referenceImages;
  }
  if (mode === "MULTI_REF_VIDEO" && data.referenceImageOrder?.length) {
    const orderMap = new Map(
      data.referenceImageOrder.map((nodeId, idx) => [nodeId, idx]),
    );
    return [...referenceImages].sort(
      (a, b) =>
        (orderMap.get(a.nodeId) ?? Number.MAX_SAFE_INTEGER) -
        (orderMap.get(b.nodeId) ?? Number.MAX_SAFE_INTEGER),
    );
  }
  return referenceImages;
}

function maxRefsForMode(mode: VideoGenerationMode) {
  return VIDEO_MODE_OPTIONS.find((o) => o.mode === mode)?.maxRefs ?? 9;
}

function ReferenceThumbnail({
  image,
  label,
  isGenerating,
  removeReference,
}: {
  image: {
    edgeId: string;
    nodeId: string;
    data: ImageNodeData | SketchNodeData;
  };
  label?: string;
  isGenerating: boolean;
  removeReference: (edgeId: string) => void;
}) {
  return (
    <div className="group relative shrink-0">
      <div className="size-9 overflow-hidden rounded-lg border border-border-warm bg-muted">
        {image.data.previewUrl || image.data.dataUrl ? (
          <img
            src={image.data.previewUrl || image.data.dataUrl}
            alt={image.data.fileName}
            className="size-full object-cover"
            draggable={false}
          />
        ) : (
          <ImageIcon className="m-2 size-5 text-muted-gray/40" />
        )}
      </div>
      {label && (
        <span className="absolute -bottom-1 left-1/2 -translate-x-1/2 rounded bg-charcoal/75 px-1 text-[9px] leading-tight text-off-white">
          {label}
        </span>
      )}
      <button
        type="button"
        onClick={() => removeReference(image.edgeId)}
        disabled={isGenerating}
        className="absolute -right-1.5 -top-1.5 flex size-4 items-center justify-center rounded-full bg-charcoal text-off-white opacity-0 shadow transition-opacity group-hover:opacity-100 disabled:cursor-not-allowed"
        aria-label="移除参考图"
      >
        <X className="size-2.5" />
      </button>
    </div>
  );
}

export function VideoNodeComponent({
  id,
  data,
  selected,
  dragging,
}: VideoNodeProps) {
  const { setNodes, setEdges, getNodes, getEdges, getViewport, setViewport } =
    useReactFlow();
  // 编辑主区域鼠标滚动监控
  const composerWheelRef = useComposerWheelPan<HTMLDivElement>(
    getViewport,
    setViewport,
  );
  // 工具栏区域鼠标滚动监控
  const toolbarWheelRef = useComposerWheelPan<HTMLDivElement>(
    getViewport,
    setViewport,
  );

  const updateNodeInternals = useUpdateNodeInternals();

  const zoom = useStore((s) => s.transform[2] || 1);
  const selectedNodeCount = useStore((s) =>
    s.nodes.reduce((count, node) => count + (node.selected ? 1 : 0), 0),
  );
  // 参考图签名（边及参考图）
  const referenceImagesSignature = useStore((s) =>
    (s.edges as AppEdge[])
      .filter((edge) => edge.target === id)
      .map((edge) => {
        // 确定连接的节点
        const node = (s.nodes as AppNode[]).find(
          (item) => item.id === edge.source,
        );
        // 要允许连接图片节点和草图节点
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
      .join("|"),
  );

  const [referencePickerPromptId, setReferencePickerPromptId] = useState<
    string | null
  >(null);
  const [modelOpen, setModelOpen] = useState(false);
  const [paramsOpen, setParamsOpen] = useState(false);
  const [previewOpen, setPreviewOpen] = useState(false);
  // 记录视频生成任务的开始时间 todo:重新提交需要更新
  const [startedAtMs, setStartedAtMs] = useState(() =>
    data.generationRunStartedAt || data.generationStartedAt
      ? new Date(
          data.generationRunStartedAt ?? data.generationStartedAt ?? "",
        ).getTime()
      : 0,
  );

  const [now, setNow] = useState(startedAtMs);
  const paramsRef = useRef<HTMLDivElement>(null);
  const paramsBtnRef = useRef<HTMLButtonElement>(null);
  const modelRef = useRef<HTMLDivElement>(null);
  const modelBtnRef = useRef<HTMLButtonElement>(null);
  const activeRunPollRef = useRef<string | null>(null);
  const videoRef = useRef<HTMLVideoElement>(null);
  const captureMenuRef = useRef<HTMLDivElement>(null);
  // 视频是否正在播放
  const [isPlaying, setIsPlaying] = useState(false);
  // 视频播放进度
  const [playbackPosition, setPlaybackPosition] = useState(0);
  // 视频总时长
  const [durationSec, setDurationSec] = useState(data.durationSec ?? 0);
  const [audioLevel, setAudioLevel] = useState(1);
  const [captureMenuVisible, setCaptureMenuVisible] = useState(false);
  const [captureError, setCaptureError] = useState<string | null>(null);

  // todo 不知道后面要不要调整
  const isGenerating = data.status === "pending";
  const kind = data.kind ?? "draft";
  const upstreamStatus = data.upstreamStatus ?? null;
  const isQueued = isGenerating && isQueuedStatus(upstreamStatus);
  const isRunning = isGenerating && isRunningStatus(upstreamStatus);
  const canCompose = kind !== "uploaded";
  const pickerActiveForThisNode = referencePickerPromptId === id;
  const dataStartedAtMs =
    data.generationRunStartedAt || data.generationStartedAt
      ? new Date(
          data.generationRunStartedAt ?? data.generationStartedAt ?? "",
        ).getTime()
      : 0;
  const effectiveStartedAtMs = startedAtMs > 0 ? startedAtMs : dataStartedAtMs;
  const elapsedMs =
    data.elapsedMs ??
    (isGenerating &&
    Number.isFinite(effectiveStartedAtMs) &&
    effectiveStartedAtMs > 0
      ? Math.max(0, now - effectiveStartedAtMs)
      : null);
  const progressLabel = isQueued
    ? `排队中 ${formatElapsed(elapsedMs)}`
    : isRunning
      ? `生成中 ${formatElapsed(elapsedMs)}`
      : isGenerating
        ? `提交中 ${formatElapsed(elapsedMs)}`
        : "提交中";
  const previewElapsedMs = previewOpen ? elapsedMs : data.elapsedMs;
  const previewItem = useMemo(
    // todo 里面问题还很大
    () => videoNodeToMediaPreview({ ...data, elapsedMs: previewElapsedMs }),
    [data, previewElapsedMs],
  );
  // todo 历史遗留问题，后续需要调整/ 尺寸用户应该可以自己拖拽比例
  const displaySize = getDisplaySize(data);

  const isOnlySelectedNode = selected && selectedNodeCount === 1;
  const showNodeActions = selectedNodeCount <= 1;
  // 后期previewUrl 可以考虑gif
  const videoSrc = data.videoUrl || data.previewUrl;
  const videoAssetId = getVideoAssetId(data);
  const fixedUiScale = 1 / zoom;
  // 后续除了引用图片，视频也可以参与引用

  // 参考图（边及参考图）
  const referenceImages = useMemo(() => {
    void referenceImagesSignature;
    const currentNodes = getNodes() as AppNode[];
    const currentEdges = getEdges() as AppEdge[];
    const images: {
      edgeId: string;
      nodeId: string;
      data: ImageNodeData | SketchNodeData;
    }[] = [];
    for (const edge of currentEdges) {
      if (edge.target !== id) continue;
      const node = currentNodes.find((item) => item.id === edge.source);
      if (node?.type !== "image" && node?.type !== "sketch") continue;
      images.push({
        edgeId: edge.id,
        nodeId: node.id,
        data: node.data as ImageNodeData | SketchNodeData,
      });
    }
    return images;
  }, [getEdges, getNodes, id, referenceImagesSignature]);

  const rawParams = useMemo(() => data.params ?? {}, [data.params]);
  /**
   * 这里模型过滤的方式是否需要考虑？ 是否需要给用户说明一下当前的列表为什么模式可用？
   */
  const modelListCapability =
    data.explicitMode ?? deriveVideoMode(null, referenceImages.length);
  /**
   * 获取当前模型的能力？ - _ - 如果明确能够选择某个模型，模型的能力变化量会很小，可以在进入画布的时候将所有模型的能力在本地存储一份吧
   */
  const aigcModels = useAigcModels({
    type: 3,
    capability: modelListCapability,
    listCapability: null,
    preferredModelId: data.aigcModelId,
    params: rawParams,
  });
  const storedAigcModel = aigcModels.models.find(
    (model) => model.id === data.aigcModelId,
  );
  const activeAigcModel = storedAigcModel ?? aigcModels.selectedModel;
  // 生成模式
  const generationCapability = deriveVideoMode(
    data.explicitMode,
    referenceImages.length,
    activeAigcModel?.capabilities,
  );

  const orderedReferenceImages = useMemo(
    // 这个图片排序模式还值得商榷
    () => getOrderedReferences(generationCapability, referenceImages, data),
    [data, generationCapability, referenceImages],
  );

  // 模式校验，感觉是脱裤子放屁多此一举
  const generationValidationMessage = validateVideoGeneration(
    generationCapability,
    referenceImages.length,
  );

  // 参考图提及选项
  const mentionOptions = useMemo<PromptMentionOption[]>(
    () =>
      orderedReferenceImages.map((image, index) => ({
        id: image.nodeId,
        label: `图片 ${index + 1}`,
        token: createPromptMentionToken(image.nodeId),
        thumbnailUrl: image.data.previewUrl || image.data.dataUrl,
      })),
    [orderedReferenceImages],
  );
  const activeAigcModelId = activeAigcModel?.id ?? data.aigcModelId;
  const activeModelName =
    activeAigcModel?.name ?? data.modelName ?? SEEDANCE_MODEL_NAME;
  const activeProviderModel =
    activeAigcModel?.model ?? data.providerModel ?? data.modelId;
  const effectiveParams = useMemo(
    () => filterAigcModelParams(rawParams, aigcModels.templates),
    [aigcModels.templates, rawParams],
  );
  const costLabel = aigcModels.priceLoading
    ? "…"
    : formatCost(aigcModels.price?.salePrice);
  const canGenerate =
    Boolean(data.prompt.trim()) &&
    !isGenerating &&
    !aigcModels.loading &&
    !aigcModels.templateLoading &&
    Boolean(activeAigcModelId) &&
    !generationValidationMessage;
  const mediaDurationSec = durationSec || data.durationSec || 0;

  useEffect(() => {
    const frame = window.requestAnimationFrame(() => updateNodeInternals(id));
    const timeout = window.setTimeout(() => updateNodeInternals(id), 260);

    return () => {
      window.cancelAnimationFrame(frame);
      window.clearTimeout(timeout);
    };
  }, [displaySize.width, displaySize.height, id, updateNodeInternals]);

  const updateData = useCallback(
    (patch: Partial<VideoNodeData>, options?: { flush?: boolean }) => {
      setNodes((nds) =>
        nds.map((node) =>
          node.id === id ? { ...node, data: { ...node.data, ...patch } } : node,
        ),
      );
      window.dispatchEvent(
        new CustomEvent<NodeDataPatchEventDetail>("copse:node-data-patch", {
          detail: { nodeId: id, patch, flush: options?.flush },
        }),
      );
    },
    [id, setNodes],
  );

  const updateParams = useCallback(
    (patch: Record<string, unknown>) => {
      setNodes((nds) =>
        nds.map((node) => {
          if (node.id !== id) return node;
          const nodeData = node.data as VideoNodeData;
          return {
            ...node,
            data: {
              ...nodeData,
              params: { ...(nodeData.params ?? {}), ...patch },
              ...patch,
            },
          };
        }),
      );
      window.dispatchEvent(
        new CustomEvent<NodeDataPatchEventDetail>("copse:node-data-patch", {
          detail: {
            nodeId: id,
            patch: { params: { ...(data.params ?? {}), ...patch }, ...patch },
          },
        }),
      );
    },
    [data.params, id, setNodes],
  );

  const waitAndApplyServerRun = useCallback(
    async (projectId: string | number, taskId: number, startedAt: string) => {
      const pollKey = `${projectId}:${id}:${taskId}`;
      if (activeRunPollRef.current === pollKey) return;
      activeRunPollRef.current = pollKey;
      try {
        const result = await waitCanvasNodeRunResult(projectId, id, {
          taskId,
          baseVersion: 0,
          nodeType: "video",
        });
        const patch = getCanvasNodeRunPatch(result, id);
        if (patch) {
          const nextPatch = { ...patch } as Partial<VideoNodeData>;
          const assetId = getPatchVideoAssetId(patch);
          if (assetId && !nextPatch.videoUrl && !nextPatch.previewUrl) {
            try {
              const asset = await getMyAsset(assetId);
              const videoUrl = getAssetPreviewUrl(asset) || asset.fileUrl;
              if (videoUrl) {
                nextPatch.videoUrl = videoUrl;
                nextPatch.previewUrl = videoUrl;
                nextPatch.assetUrlExpireTime =
                  getAssetPreviewExpireTime(asset) ?? null;
              }
            } catch {
              // Keep the stable asset id from the server; project hydration can recover the preview URL later.
            }
          }
          updateData(nextPatch, { flush: true });
        }
      } catch (error) {
        updateData(
          {
            status: "failed",
            taskId: String(taskId),
            errorMessage:
              error instanceof Error ? error.message : "视频任务同步失败",
            upstreamStatus: "FAILED",
            generationCompletedAt: new Date().toISOString(),
            elapsedMs: Date.now() - new Date(startedAt).getTime(),
          },
          { flush: true },
        );
      } finally {
        if (activeRunPollRef.current === pollKey) {
          activeRunPollRef.current = null;
        }
      }
    },
    [id, updateData],
  );

  useEffect(() => {
    if (data.status !== "pending" || !data.taskId) return;
    const projectId = new URLSearchParams(window.location.search).get(
      "projectId",
    );
    if (!isServerCanvasProjectId(projectId)) return;
    const taskId = Number(data.taskId);
    if (!Number.isFinite(taskId)) return;
    void waitAndApplyServerRun(
      projectId,
      taskId,
      data.generationStartedAt ?? data.createdAt,
    );
  }, [
    data.createdAt,
    data.generationStartedAt,
    data.status,
    data.taskId,
    waitAndApplyServerRun,
  ]);

  useEffect(() => {
    if (aigcModels.loading || aigcModels.models.length === 0) return;
    if (
      data.aigcModelId &&
      aigcModels.models.some((model) => model.id === data.aigcModelId)
    )
      return;
    const nextModel = aigcModels.selectedModel ?? aigcModels.models[0];
    if (!nextModel) return;
    updateData({
      modelId: String(nextModel.id),
      providerModel: nextModel.model,
      modelName: nextModel.name,
      aigcModelId: nextModel.id,
    });
  }, [
    aigcModels.loading,
    aigcModels.models,
    aigcModels.selectedModel,
    data.aigcModelId,
    updateData,
  ]);

  useEffect(() => {
    if (aigcModels.templateLoading || aigcModels.templates.length === 0) return;
    const patch: Record<string, unknown> = {};
    for (const template of aigcModels.templates) {
      if (hasParamValue(rawParams, template.paramKey)) continue;
      const fallback =
        template.paramKey === "ratio" || template.paramKey === "aspectRatio"
          ? data.ratio
          : template.paramKey === "resolution"
            ? data.resolution
            : template.paramKey === "duration"
              ? String(data.duration)
              : template.paramKey === "size"
                ? (data.size ?? "1280*704")
                : template.paramKey === "generateAudio" ||
                    template.paramKey === "audio"
                  ? String(data.generateAudio)
                  : "";
      const nextValue = templateDefault(template, fallback);
      if (nextValue !== "") patch[template.paramKey] = nextValue;
    }
    if (Object.keys(patch).length > 0) updateParams(patch);
  }, [
    aigcModels.templateLoading,
    aigcModels.templates,
    data.duration,
    data.generateAudio,
    data.ratio,
    data.resolution,
    data.size,
    rawParams,
    updateParams,
  ]);

  const summary = useMemo(() => {
    if (aigcModels.templateLoading) return "参数加载中";
    if (aigcModels.templates.length === 0) return "默认参数";
    const values = aigcModels.templates
      .map((template) =>
        normalizeTemplateOption(
          rawParams[template.paramKey] ?? template.defaultValue ?? "",
        ),
      )
      .filter(Boolean)
      .slice(0, 3);
    return values.length > 0 ? values.join(" · ") : "模型参数";
  }, [aigcModels.templateLoading, aigcModels.templates, rawParams]);

  useEffect(() => {
    function handleReferencePicker(e: Event) {
      const detail = (e as CustomEvent<ReferencePickerEventDetail>).detail;
      setReferencePickerPromptId(detail?.promptId ?? null);
    }

    window.addEventListener("copse:reference-picker", handleReferencePicker);
    return () =>
      window.removeEventListener(
        "copse:reference-picker",
        handleReferencePicker,
      );
  }, []);

  useEffect(() => {
    if (!isRunning) return;
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [isRunning]);

  useEffect(() => {
    if (!selected && pickerActiveForThisNode) {
      window.dispatchEvent(
        new CustomEvent<ReferencePickerEventDetail>("copse:reference-picker", {
          detail: { promptId: null },
        }),
      );
    }
  }, [pickerActiveForThisNode, selected]);

  const handlePreviewDownload = useCallback(() => {
    if (!previewItem) return;
    downloadMedia(previewItem);
  }, [previewItem]);

  useEffect(() => {
    if (!paramsOpen && !modelOpen) return;

    function handlePointerDown(event: PointerEvent) {
      const target = event.target as HTMLElement;
      if (
        paramsRef.current?.contains(target) ||
        paramsBtnRef.current?.contains(target) ||
        modelRef.current?.contains(target) ||
        modelBtnRef.current?.contains(target)
      ) {
        return;
      }
      setParamsOpen(false);
      setModelOpen(false);
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setParamsOpen(false);
        setModelOpen(false);
      }
    }

    window.addEventListener("pointerdown", handlePointerDown, true);
    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.removeEventListener("pointerdown", handlePointerDown, true);
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [modelOpen, paramsOpen]);

  useEffect(() => {
    if (!captureMenuVisible) return;

    function handlePointerDown(event: PointerEvent) {
      if (captureMenuRef.current?.contains(event.target as HTMLElement)) return;
      setCaptureMenuVisible(false);
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setCaptureMenuVisible(false);
    }

    window.addEventListener("pointerdown", handlePointerDown, true);
    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.removeEventListener("pointerdown", handlePointerDown, true);
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [captureMenuVisible]);

  const removeReference = useCallback(
    (edgeId: string) =>
      setEdges((eds) => eds.filter((edge) => edge.id !== edgeId)),
    [setEdges],
  );

  const openReferencePicker = useCallback(() => {
    if (isGenerating) return;
    window.dispatchEvent(
      new CustomEvent<ReferencePickerEventDetail>("copse:reference-picker", {
        detail: { promptId: id },
      }),
    );
  }, [id, isGenerating]);

  const handleTogglePlayback = useCallback(() => {
    const video = videoRef.current;
    if (!video) return;
    if (video.paused) {
      video.play().catch(() => undefined);
    } else {
      video.pause();
    }
  }, []);

  const handleSeek = useCallback((value: string) => {
    const video = videoRef.current;
    if (!video) return;
    const nextTime = Number(value);
    if (!Number.isFinite(nextTime)) return;
    video.currentTime = nextTime;
    setPlaybackPosition(nextTime);
  }, []);

  const handleVolumeChange = useCallback((value: string) => {
    const video = videoRef.current;
    const nextVolume = Math.min(1, Math.max(0, Number(value)));
    if (!Number.isFinite(nextVolume)) return;
    setAudioLevel(nextVolume);
    if (video) {
      video.volume = nextVolume;
      video.muted = nextVolume === 0;
    }
  }, []);

  const handleToggleMute = useCallback(() => {
    const video = videoRef.current;
    if (!video) return;
    if (video.muted || video.volume === 0) {
      const nextVolume = audioLevel > 0 ? audioLevel : 1;
      video.muted = false;
      video.volume = nextVolume;
      setAudioLevel(nextVolume);
      return;
    }
    video.muted = true;
    setAudioLevel(0);
  }, [audioLevel]);

  const captureFrame = useCallback(
    async (capturedAt: VideoFrameCaptureEventDetail["capturedAt"]) => {
      const video = videoRef.current;
      if (!video || !videoSrc) return;
      if (!video.videoWidth || !video.videoHeight) {
        setCaptureError("视频还未加载完成");
        return;
      }

      setCaptureError(null);
      setCaptureMenuVisible(false);

      const previousTime = video.currentTime;
      const wasPaused = video.paused;
      const targetTime =
        capturedAt === "first"
          ? 0
          : capturedAt === "last"
            ? Math.max(
                0,
                (video.duration || mediaDurationSec || previousTime) -
                  FRAME_CAPTURE_EPSILON_SEC,
              )
            : previousTime;
      const fileName = `${data.fileName || "Video"} ${capturedAt === "first" ? "首帧" : capturedAt === "last" ? "尾帧" : "当前帧"}.png`;

      try {
        if (videoAssetId) {
          const assetId = await captureVideoFrameAsset({
            assetId: videoAssetId,
            capturedAt,
            timeSec: targetTime,
            title: fileName,
          });
          const asset = await getMyAsset(assetId);
          window.dispatchEvent(
            new CustomEvent<VideoFrameCaptureEventDetail>(
              "copse:video-frame-capture",
              {
                detail: {
                  sourceNodeId: id,
                  assetId,
                  previewUrl: getAssetPreviewUrl(asset) || asset.fileUrl,
                  assetUrlExpireTime: getAssetPreviewExpireTime(asset) ?? null,
                  width: asset.width,
                  height: asset.height,
                  mimeType: asset.mimeType || "image/png",
                  fileName,
                  capturedAt,
                },
              },
            ),
          );
          return;
        }

        if (
          capturedAt !== "current" &&
          Math.abs(video.currentTime - targetTime) > 0.01
        ) {
          await new Promise<void>((resolve, reject) => {
            const timeout = window.setTimeout(() => {
              cleanup();
              reject(new Error("视频定位超时"));
            }, 3000);
            const cleanup = () => {
              window.clearTimeout(timeout);
              video.removeEventListener("seeked", handleSeeked);
              video.removeEventListener("error", handleError);
            };
            const handleSeeked = () => {
              cleanup();
              resolve();
            };
            const handleError = () => {
              cleanup();
              reject(new Error("视频定位失败"));
            };
            video.addEventListener("seeked", handleSeeked, { once: true });
            video.addEventListener("error", handleError, { once: true });
            video.currentTime = targetTime;
          });
        }

        const canvas = document.createElement("canvas");
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        const context = canvas.getContext("2d");
        if (!context) throw new Error("浏览器不支持截帧");
        context.drawImage(video, 0, 0, canvas.width, canvas.height);
        const dataUrl = canvas.toDataURL("image/png");
        window.dispatchEvent(
          new CustomEvent<VideoFrameCaptureEventDetail>(
            "copse:video-frame-capture",
            {
              detail: {
                sourceNodeId: id,
                dataUrl,
                width: canvas.width,
                height: canvas.height,
                mimeType: dataUrlToMimeType(dataUrl),
                fileName,
                capturedAt,
              },
            },
          ),
        );
      } catch (error) {
        setCaptureError(
          error instanceof Error ? `截帧失败：${error.message}` : "截帧失败",
        );
      } finally {
        if (capturedAt !== "current") {
          video.currentTime = previousTime;
        }
        if (!wasPaused) {
          video.play().catch(() => undefined);
        }
      }
    },
    [data.fileName, id, mediaDurationSec, videoAssetId, videoSrc],
  );

  const handleGenerate = useCallback(async () => {
    const prompt = promptValueToSubmitPrompt(
      data.prompt,
      mentionOptions,
    ).trim();
    if (!prompt || isGenerating) return;

    if (generationValidationMessage) {
      updateData({
        status: "failed",
        errorMessage: generationValidationMessage,
        upstreamStatus: "failed",
      });
      return;
    }

    if (!activeAigcModelId) {
      updateData({
        status: "failed",
        errorMessage: "请选择 AIGC 视频模型后再生成。",
        upstreamStatus: "failed",
      });
      return;
    }

    const startedAt = new Date().toISOString();
    const runStartedAtMs = Date.now();
    setStartedAtMs(runStartedAtMs);
    setNow(runStartedAtMs);
    updateData({
      status: "pending",
      errorMessage: null,
      taskId: null,
      generationStartedAt: startedAt,
      generationRunStartedAt: startedAt,
      generationCompletedAt: null,
      upstreamStatus: "SUBMITTING",
      elapsedMs: null,
    });

    const resolvedReferenceImages = (
      await resolveReferenceImagesForSubmit(orderedReferenceImages)
    ).filter(Boolean);
    const referenceAssetIds = orderedReferenceImages
      .map((image) => getReferenceAssetId(image.data))
      .filter((assetId): assetId is number => typeof assetId === "number");

    const inputParamsBase: Record<string, unknown> = {
      ...effectiveParams,
      providerModel: activeProviderModel,
    };
    if (referenceAssetIds.length > 0)
      inputParamsBase.referenceAssetIds = referenceAssetIds;
    if (resolvedReferenceImages.length > 0)
      inputParamsBase.referenceImages = resolvedReferenceImages;
    if (orderedReferenceImages.length > 0)
      inputParamsBase.referenceImageIds = orderedReferenceImages.map(
        (img) => img.nodeId,
      );

    const projectId = new URLSearchParams(window.location.search).get(
      "projectId",
    );
    if (isServerCanvasProjectId(projectId)) {
      const clientId = `node_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
      try {
        const run = await canvasNodeRunApi.runNode(projectId, id, {
          clientId,
          baseVersion: 0,
          runId: clientId,
          nodeType: "video",
          generateType: "VIDEO",
          generateMode: generationCapability,
          modelId: activeAigcModelId,
          prompt,
          inputParams: JSON.stringify(inputParamsBase),
          sync: false,
        });
        updateData(
          {
            taskId: String(run.taskId),
            upstreamStatus: run.status,
          },
          { flush: true },
        );
        await waitAndApplyServerRun(projectId, run.taskId, startedAt);
        return;
      } catch (error) {
        updateData({
          status: "failed",
          taskId: null,
          errorMessage:
            error instanceof Error ? error.message : "视频任务提交失败",
          upstreamStatus: "FAILED",
          generationCompletedAt: new Date().toISOString(),
          elapsedMs: Date.now() - new Date(startedAt).getTime(),
        });
        return;
      }
    }

    try {
      const submit = await generationApi.submit({
        generateType: "VIDEO",
        generateMode: generationCapability,
        modelId: activeAigcModelId,
        prompt,
        inputParams: JSON.stringify(inputParamsBase),
        sync: false,
      });
      updateData({
        status: "pending",
        taskId: String(submit.taskId),
        upstreamStatus: submit.status,
        elapsedMs: null,
      });

      const result = await waitGenerationResult(submit.taskId);
      const completedAt = result.finishTime ?? new Date().toISOString();
      const outputAssetId = result.assetIdList[0] ?? null;
      let videoUrl = result.outputUrlList[0];
      let assetUrlExpireTime: string | null = null;
      if (outputAssetId) {
        try {
          const asset = await getMyAsset(outputAssetId);
          videoUrl = getAssetPreviewUrl(asset) || videoUrl;
          assetUrlExpireTime = getAssetPreviewExpireTime(asset) ?? null;
        } catch {}
      }

      if (result.status === "SUCCESS" && videoUrl) {
        updateData({
          status: "complete",
          kind: "generated",
          taskId: String(submit.taskId),
          assetId: outputAssetId,
          outputAssetId,
          videoUrl,
          previewUrl: videoUrl,
          assetUrlExpireTime,
          errorMessage: null,
          upstreamStatus: result.status,
          generationCompletedAt: completedAt,
          elapsedMs: Date.now() - new Date(startedAt).getTime(),
        });
        return;
      }

      updateData({
        status: "failed",
        taskId: String(submit.taskId),
        errorMessage: result.failMessage ?? "视频生成失败，请稍后重试。",
        upstreamStatus: result.status,
        generationCompletedAt: completedAt,
        elapsedMs: Date.now() - new Date(startedAt).getTime(),
      });
    } catch (error) {
      updateData({
        status: "failed",
        errorMessage:
          error instanceof Error ? error.message : "视频任务提交失败",
        upstreamStatus: "FAILED",
        generationCompletedAt: new Date().toISOString(),
        elapsedMs: Date.now() - new Date(startedAt).getTime(),
      });
    }
  }, [
    activeAigcModelId,
    activeProviderModel,
    data.prompt,
    effectiveParams,
    generationCapability,
    generationValidationMessage,
    id,
    isGenerating,
    mentionOptions,
    orderedReferenceImages,
    updateData,
    waitAndApplyServerRun,
  ]);

  return (
    <>
      <div
        className="relative"
        style={{ width: displaySize.width, height: displaySize.height }}
      >
        <div
          className="relative overflow-visible"
          style={{ width: displaySize.width, height: displaySize.height }}
        >
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
          <CanvasNodeTitle maxWidth={displaySize.width}>
            <Video className="size-4" />
            <EditableNodeTitle
              value={data.fileName}
              fallback="Video"
              onCommit={(fileName) => updateData({ fileName }, { flush: true })}
            />
          </CanvasNodeTitle>

          <motion.div
            className="absolute"
            initial={false}
            animate={{
              left: 0,
              top: 0,
              width: displaySize.width,
              height: displaySize.height,
            }}
            transition={{ type: "spring", stiffness: 360, damping: 34 }}
          >
            <motion.div
              data-node-preview-card
              data-node-id={id}
              initial={{ opacity: 0, scale: 0.97 }}
              animate={{
                opacity: 1,
                scale: 1,
                width: displaySize.width,
                height: displaySize.height,
              }}
              transition={{
                opacity: { duration: 0.18, ease: "easeOut" },
                scale: { duration: 0.18, ease: "easeOut" },
                width: { type: "spring", stiffness: 360, damping: 34 },
                height: { type: "spring", stiffness: 360, damping: 34 },
              }}
              className={cn(
                "canvas-node-drag-handle group relative rounded-xl border bg-background shadow-[0_1px_3px_rgba(0,0,0,0.06)]",
                selected
                  ? "border-charcoal/60 ring-2 ring-charcoal/35"
                  : "border-border-warm",
              )}
              style={{
                width: displaySize.width,
                height: displaySize.height,
                pointerEvents: "auto",
              }}
            >
              <div className="absolute inset-0 flex items-center justify-center overflow-hidden rounded-[inherit]">
                {videoSrc ? (
                  <video
                    ref={videoRef}
                    src={videoSrc}
                    className="size-full object-contain"
                    playsInline
                    onClick={handleTogglePlayback}
                    onPlay={() => setIsPlaying(true)}
                    onPause={() => setIsPlaying(false)}
                    onTimeUpdate={(event) =>
                      setPlaybackPosition(event.currentTarget.currentTime)
                    }
                    onLoadedMetadata={(event) => {
                      const video = event.currentTarget;
                      setDurationSec(
                        Number.isFinite(video.duration) ? video.duration : 0,
                      );
                      const patch: Partial<VideoNodeData> = {};
                      if (
                        video.videoWidth > 0 &&
                        video.videoHeight > 0 &&
                        (data.width !== video.videoWidth ||
                          data.height !== video.videoHeight)
                      ) {
                        patch.width = video.videoWidth;
                        patch.height = video.videoHeight;
                      }
                      if (
                        Number.isFinite(video.duration) &&
                        video.duration > 0 &&
                        data.durationSec !== video.duration
                      ) {
                        patch.durationSec = video.duration;
                      }
                      if (Object.keys(patch).length > 0) {
                        updateData(patch, { flush: true });
                      }
                    }}
                  />
                ) : (
                  <Play className="size-12 text-muted-gray/40" />
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
                        transition={{
                          duration: 1.4,
                          repeat: Infinity,
                          ease: "easeInOut",
                        }}
                      >
                        <Loader2 className="mb-2 size-7 animate-spin" />
                      </motion.div>
                      <span className="text-xs">{progressLabel}</span>
                      {data.taskId && (
                        <span className="mt-1 max-w-[260px] truncate text-[10px] opacity-70">
                          {data.taskId}
                        </span>
                      )}
                    </motion.div>
                  )}

                  {data.status === "failed" && (
                    <motion.div
                      key="failed"
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      exit={{ opacity: 0 }}
                      transition={{ duration: 0.16 }}
                      className="absolute inset-0 flex flex-col items-center justify-center bg-background/90 px-4 text-center text-destructive"
                    >
                      <span className="text-xs">
                        {data.errorMessage ?? "视频任务提交失败"}
                      </span>
                    </motion.div>
                  )}
                </AnimatePresence>

                {videoSrc && !isGenerating && data.status !== "failed" && (
                  <div
                    className="nodrag nowheel absolute inset-x-0 bottom-0 flex items-center gap-3 bg-gradient-to-t from-[#1c1c1c]/80 via-[#1c1c1c]/45 to-transparent px-4 pb-3 pt-8 text-[#fcfbf8] opacity-0 transition-opacity group-hover:opacity-100"
                    onPointerDownCapture={stopCanvasSelection}
                    onClick={stopCanvasSelection}
                  >
                    <button
                      type="button"
                      onClick={handleTogglePlayback}
                      className="flex size-8 shrink-0 items-center justify-center rounded-full hover:bg-[#fcfbf8]/12 focus-visible:outline focus-visible:outline-2 focus-visible:outline-[#fcfbf8]"
                      aria-label={isPlaying ? "暂停" : "播放"}
                    >
                      {isPlaying ? (
                        <Pause className="size-5" />
                      ) : (
                        <Play className="size-5 fill-current" />
                      )}
                    </button>
                    <span className="w-10 text-xs tabular-nums">
                      {formatTime(playbackPosition)}
                    </span>
                    <input
                      type="range"
                      min={0}
                      max={Math.max(0.01, mediaDurationSec)}
                      step={0.01}
                      value={Math.min(
                        playbackPosition,
                        Math.max(0.01, mediaDurationSec),
                      )}
                      onChange={(event) => handleSeek(event.target.value)}
                      className="video-control-range h-1 min-w-0 flex-1"
                      style={
                        {
                          "--video-range-progress": getRangeProgress(
                            playbackPosition,
                            mediaDurationSec,
                          ),
                        } as React.CSSProperties
                      }
                      aria-label="视频进度"
                    />
                    <span className="w-10 text-right text-xs tabular-nums">
                      {formatTime(mediaDurationSec)}
                    </span>
                    <div className="group/volume relative flex size-8 items-center justify-center">
                      <button
                        type="button"
                        onClick={handleToggleMute}
                        className="flex size-8 items-center justify-center rounded-full hover:bg-[#fcfbf8]/12 focus-visible:outline focus-visible:outline-2 focus-visible:outline-[#fcfbf8]"
                        aria-label={audioLevel === 0 ? "取消静音" : "静音"}
                      >
                        {audioLevel === 0 ? (
                          <VolumeX className="size-4" />
                        ) : (
                          <Volume2 className="size-4" />
                        )}
                      </button>
                      <div className="pointer-events-none absolute bottom-full left-1/2 mb-2 flex h-24 w-8 -translate-x-1/2 items-center justify-center rounded-full bg-[#1c1c1c]/90 opacity-0 shadow-xl transition-opacity group-hover/volume:pointer-events-auto group-hover/volume:opacity-100">
                        <input
                          type="range"
                          min={0}
                          max={1}
                          step={0.01}
                          value={audioLevel}
                          onChange={(event) =>
                            handleVolumeChange(event.target.value)
                          }
                          className="video-control-range h-20 w-1 [writing-mode:vertical-rl]"
                          aria-label="音量"
                          style={{ direction: "rtl" }}
                        />
                      </div>
                    </div>
                    <div
                      ref={captureMenuRef}
                      className="relative"
                      onMouseLeave={() => setCaptureMenuVisible(false)}
                    >
                      <button
                        type="button"
                        onClick={() => captureFrame("current")}
                        onMouseEnter={() => setCaptureMenuVisible(true)}
                        className="flex size-8 items-center justify-center rounded-full hover:bg-off-white/12 focus-visible:outline focus-visible:outline-2 focus-visible:outline-off-white"
                        aria-label="截取当前帧"
                      >
                        <Camera className="size-4" />
                      </button>
                      <AnimatePresence>
                        {captureMenuVisible && (
                          <motion.div
                            initial={{ opacity: 0, y: 6, scale: 0.98 }}
                            animate={{ opacity: 1, y: 0, scale: 1 }}
                            exit={{ opacity: 0, y: 6, scale: 0.98 }}
                            transition={{ duration: 0.14, ease: "easeOut" }}
                            onMouseEnter={() => setCaptureMenuVisible(true)}
                            className="absolute bottom-full right-0 z-[280] mb-2 w-32 overflow-hidden rounded-xl border border-off-white/10 bg-charcoal/95 py-1 text-sm text-off-white shadow-xl"
                          >
                            <button
                              type="button"
                              onClick={() => captureFrame("first")}
                              className="block w-full px-3 py-2 text-left hover:bg-off-white/10"
                            >
                              截取首帧
                            </button>
                            <button
                              type="button"
                              onClick={() => captureFrame("last")}
                              className="block w-full px-3 py-2 text-left hover:bg-off-white/10"
                            >
                              截取尾帧
                            </button>
                          </motion.div>
                        )}
                      </AnimatePresence>
                    </div>
                  </div>
                )}

                <AnimatePresence>
                  {captureError && (
                    <motion.div
                      initial={{ opacity: 0, y: 6 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0, y: 6 }}
                      transition={{ duration: 0.14, ease: "easeOut" }}
                      className="absolute bottom-14 left-4 rounded-lg bg-charcoal/85 px-3 py-2 text-xs text-off-white"
                    >
                      {captureError}
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>

              <NodeCreateHandle
                nodeId={id}
                direction="incoming"
                selected={selected}
                showButton={showNodeActions}
              />
            </motion.div>
          </motion.div>
        </div>

        <AnimatePresence>
          {isOnlySelectedNode && !dragging && canCompose && (
            <motion.div
              initial={{
                opacity: 0,
                y: -8 * fixedUiScale,
                scale: 0.99 * fixedUiScale,
              }}
              animate={{ opacity: 1, y: 0, scale: fixedUiScale }}
              exit={{
                opacity: 0,
                y: -8 * fixedUiScale,
                scale: 0.99 * fixedUiScale,
              }}
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
                    aria-label="视频工具"
                  >
                    <Sparkles className="size-4" />
                  </button>
                  {(() => {
                    const showSlots =
                      generationCapability === "FIRST_LAST_FRAME_VIDEO";
                    if (showSlots && orderedReferenceImages.length < 2) {
                      return (
                        <>
                          {orderedReferenceImages.map((image, index) => (
                            <ReferenceThumbnail
                              key={image.edgeId}
                              image={image}
                              label={index === 0 ? "首帧" : "尾帧"}
                              isGenerating={isGenerating}
                              removeReference={removeReference}
                            />
                          ))}
                          {Array.from({
                            length: 2 - orderedReferenceImages.length,
                          }).map((_, i) => (
                            <div
                              key={`slot-${i}`}
                              className="flex size-9 items-center justify-center rounded-lg border border-dashed border-border-warm bg-muted text-[10px] text-muted-gray"
                            >
                              {orderedReferenceImages.length + i === 0
                                ? "首帧"
                                : "尾帧"}
                            </div>
                          ))}
                        </>
                      );
                    }
                    return orderedReferenceImages.map((image, index) => (
                      <ReferenceThumbnail
                        key={image.edgeId}
                        image={image}
                        label={
                          showSlots
                            ? index === 0
                              ? "首帧"
                              : "尾帧"
                            : generationCapability === "MULTI_REF_VIDEO"
                              ? `${index + 1}`
                              : undefined
                        }
                        isGenerating={isGenerating}
                        removeReference={removeReference}
                      />
                    ));
                  })()}
                  {generationCapability !== "TEXT_TO_VIDEO" &&
                    referenceImages.length <
                      maxRefsForMode(generationCapability) && (
                      <button
                        type="button"
                        onClick={openReferencePicker}
                        disabled={isGenerating}
                        className={cn(
                          "flex size-9 items-center justify-center rounded-lg text-muted-gray transition-colors disabled:cursor-not-allowed disabled:opacity-40",
                          pickerActiveForThisNode
                            ? "bg-charcoal text-off-white"
                            : "bg-muted hover:text-charcoal",
                        )}
                        aria-label="选择参考图"
                      >
                        <Plus className="size-4" />
                      </button>
                    )}
                </div>
                {isGenerating && (
                  <span className="text-xs text-muted-gray">
                    {progressLabel}
                  </span>
                )}
              </div>

              <PromptMentionInput
                value={data.prompt}
                onChange={(nextPrompt) => updateData({ prompt: nextPrompt })}
                mentions={mentionOptions}
                disabled={isGenerating}
                placeholder="Describe anything you want to generate"
                minHeightClassName="min-h-[130px]"
                onSubmit={() => {
                  if (canGenerate) void handleGenerate();
                }}
              />

              <div className="mt-4 flex items-center justify-between gap-3 border-t border-border-warm pt-3">
                <div className="flex min-w-0 items-center gap-2">
                  <div className="relative">
                    <button
                      ref={modelBtnRef}
                      type="button"
                      disabled={isGenerating}
                      onClick={() => {
                        setModelOpen((open) => !open);
                        setParamsOpen(false);
                      }}
                      className="flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm font-medium text-charcoal hover:bg-muted disabled:cursor-not-allowed disabled:opacity-40"
                    >
                      <Sparkles className="size-4 text-muted-gray" />
                      <span>{activeModelName}</span>
                    </button>
                    <AnimatePresence>
                      {modelOpen && (
                        <motion.div
                          ref={modelRef}
                          data-composer-local-wheel="true"
                          initial={{ opacity: 0, y: 4, scale: 0.98 }}
                          animate={{ opacity: 1, y: 0, scale: 1 }}
                          exit={{ opacity: 0, y: 4, scale: 0.98 }}
                          transition={{ duration: 0.14, ease: "easeOut" }}
                          className="absolute bottom-full left-0 z-[260] mb-2 w-[320px] rounded-2xl border border-border-warm bg-background p-3 shadow-lg"
                        >
                          <div className="max-h-[320px] overflow-y-auto">
                            {aigcModels.models.length > 0 ? (
                              aigcModels.models.map((model) => {
                                const isSelected =
                                  activeAigcModelId === model.id;
                                return (
                                  <button
                                    key={model.id}
                                    type="button"
                                    onClick={() => {
                                      aigcModels.setSelectedModelId(model.id);
                                      updateData({
                                        modelId: String(model.id),
                                        providerModel: model.model,
                                        modelName: model.name,
                                        aigcModelId: model.id,
                                        provider:
                                          model.model === WAN_MODEL_ID
                                            ? "wan"
                                            : data.provider,
                                      });
                                      setModelOpen(false);
                                    }}
                                    className={cn(
                                      "mb-1 flex w-full items-center justify-between rounded-xl px-3 py-3 text-left last:mb-0",
                                      isSelected
                                        ? "bg-muted"
                                        : "hover:bg-muted",
                                    )}
                                  >
                                    <span className="flex min-w-0 flex-col gap-1">
                                      <span className="flex min-w-0 items-center gap-2 text-sm font-medium text-charcoal">
                                        <Video className="size-4 shrink-0 text-muted-gray" />
                                        <span className="truncate">
                                          {model.name}
                                        </span>
                                      </span>
                                      <span className="ml-6 text-xs text-muted-gray">
                                        {generationCapability}
                                      </span>
                                    </span>
                                    {isSelected && (
                                      <Check className="size-4 shrink-0 text-charcoal" />
                                    )}
                                  </button>
                                );
                              })
                            ) : (
                              <div className="px-3 py-4 text-sm text-muted-gray">
                                暂无可用视频模型
                              </div>
                            )}
                          </div>
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </div>
                  <span className="h-5 w-px bg-border-warm" />
                  <div className="flex items-center gap-0.5 rounded-lg bg-muted p-0.5">
                    {VIDEO_MODE_OPTIONS.map((opt) => {
                      const isActive = generationCapability === opt.mode;
                      const isSupported = modelSupportsVideoMode(
                        activeAigcModel?.capabilities,
                        opt.mode,
                      );
                      const isAvailable =
                        isSupported &&
                        isVideoModeAvailable(opt.mode, referenceImages.length);
                      return (
                        <button
                          key={opt.mode}
                          type="button"
                          disabled={isGenerating || !isAvailable}
                          onClick={() => updateData({ explicitMode: opt.mode })}
                          className={cn(
                            "nodrag nowheel rounded-md px-2 py-1 text-xs font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-40",
                            isActive
                              ? "bg-background text-charcoal shadow-sm"
                              : "text-muted-gray hover:text-charcoal",
                          )}
                          title={
                            isSupported && isAvailable && !isActive
                              ? `切换到${opt.label}`
                              : undefined
                          }
                        >
                          {opt.label}
                        </button>
                      );
                    })}
                  </div>
                  <span className="h-5 w-px bg-border-warm" />
                  <div className="relative">
                    <button
                      ref={paramsBtnRef}
                      type="button"
                      disabled={isGenerating}
                      onClick={() => {
                        setParamsOpen((open) => !open);
                        setModelOpen(false);
                      }}
                      className="flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm text-muted-gray hover:bg-muted hover:text-charcoal disabled:cursor-not-allowed disabled:opacity-40"
                    >
                      <SlidersHorizontal className="size-4" />
                      <span>{summary}</span>
                    </button>
                    <AnimatePresence>
                      {paramsOpen && (
                        <motion.div
                          ref={paramsRef}
                          data-composer-local-wheel="true"
                          initial={{ opacity: 0, y: 4, scale: 0.98 }}
                          animate={{ opacity: 1, y: 0, scale: 1 }}
                          exit={{ opacity: 0, y: 4, scale: 0.98 }}
                          transition={{ duration: 0.14, ease: "easeOut" }}
                          className="absolute bottom-full left-0 z-[260] mb-2 w-[420px] rounded-2xl border border-border-warm bg-background p-4 shadow-lg"
                        >
                          <div className="mb-3 rounded-xl bg-muted px-3 py-2 text-xs text-muted-gray">
                            当前模式：
                            {getModeOption(generationCapability)?.label ??
                              generationCapability}
                          </div>
                          {aigcModels.templates.length > 0 ? (
                            <DynamicParamForm
                              templates={aigcModels.templates}
                              values={rawParams}
                              disabled={isGenerating}
                              onChange={updateParams}
                            />
                          ) : (
                            <div className="rounded-xl border border-border-warm bg-muted px-3 py-4 text-sm text-muted-gray">
                              当前模型没有可配置参数
                            </div>
                          )}
                        </motion.div>
                      )}
                    </AnimatePresence>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  {generationValidationMessage && (
                    <span
                      className="max-w-[220px] truncate text-xs text-muted-gray"
                      title={generationValidationMessage}
                    >
                      {generationValidationMessage}
                    </span>
                  )}
                  <span className="flex items-center gap-1 rounded-lg px-2 py-1 text-sm text-muted-gray">
                    <Gem className="size-3.5" />
                    {costLabel}
                  </span>
                  <button
                    type="button"
                    onClick={handleGenerate}
                    disabled={!canGenerate}
                    className="flex size-10 items-center justify-center rounded-full bg-charcoal text-off-white shadow-sm transition-opacity active:opacity-80 disabled:cursor-not-allowed disabled:opacity-50"
                    aria-label={isGenerating ? "生成中" : "生成视频"}
                  >
                    {isGenerating ? (
                      <Loader2 className="size-5 animate-spin" />
                    ) : (
                      <ArrowUp className="size-5" />
                    )}
                  </button>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
      <MediaPreviewDialog
        item={previewItem}
        open={previewOpen}
        onClose={() => setPreviewOpen(false)}
      />
    </>
  );
}
