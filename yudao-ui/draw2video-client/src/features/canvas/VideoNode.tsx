/* eslint-disable @next/next/no-img-element */
"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { Node, NodeProps } from "@xyflow/react";
import { useReactFlow, useStore, useUpdateNodeInternals } from "@xyflow/react";
import { AnimatePresence, motion } from "motion/react";
import {
  ArrowUp,
  Check,
  Gem,
  ImageIcon,
  Loader2,
  Play,
  Plus,
  SlidersHorizontal,
  Sparkles,
  Video,
  X,
} from "lucide-react";
import type { AppEdge, AppNode, ImageNodeData, NodeDataPatchEventDetail, ReferencePickerEventDetail, SketchNodeData, VideoNodeData } from "./types";
import { NodeCreateHandle } from "./NodeCreateHandle";
import { generationApi } from "@/features/generation/generation-api";
import { waitGenerationResult } from "@/features/generation/generation-poll";
import type { AigcModelParamTemplate } from "@/features/generation/model-api";
import { useAigcModels } from "@/features/generation/use-aigc-models";
import { DynamicParamForm } from "@/features/generation/DynamicParamForm";
import { canvasNodeRunApi, getCanvasNodeRunPatch, isServerCanvasProjectId, waitCanvasNodeRunResult } from "@/features/canvas/canvas-node-run-api";
import { MediaPreviewDialog } from "@/features/media-preview/MediaPreviewDialog";
import { SelectedMediaToolbar } from "@/features/media-preview/SelectedMediaToolbar";
import { downloadMedia, videoNodeToMediaPreview } from "@/features/media-preview/media-preview-utils";
import { cn } from "@/lib/utils";
import { EditableNodeTitle } from "./EditableNodeTitle";
import { CanvasNodeTitle } from "./CanvasNodeTitle";

type VideoNodeProps = NodeProps<Node<VideoNodeData, "video">>;

const CARD_WIDTH = 420;
const CARD_HEIGHT = 236;
const PREVIEW_SLOT_WIDTH = 420;
const PREVIEW_SLOT_HEIGHT = 420;
const COMPOSER_WIDTH = 680;
const SEEDANCE_MODEL_NAME = "Seedance 2.0";
const WAN_MODEL_ID = "wan2.2-ti2v-5b";

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
  return value.replace(/\\/g, "").replace(/^"+|"+$/g, "").trim();
}

function templateDefault(template: AigcModelParamTemplate | undefined, fallback = "") {
  const normalized = template?.defaultValue ? normalizeTemplateOption(template.defaultValue) : "";
  const option = (template?.options ?? []).map(normalizeTemplateOption).find(Boolean);
  return normalized || option || fallback;
}

function hasParamValue(params: Record<string, unknown>, key: string | undefined) {
  if (!key) return false;
  const value = params[key];
  return value !== undefined && value !== null && String(value) !== "";
}

function formatCost(value: number | null | undefined) {
  if (value == null || !Number.isFinite(value)) return "1x";
  return Number.isInteger(value) ? String(value) : value.toFixed(2).replace(/\.?0+$/, "");
}

function filterModelParams(params: Record<string, unknown>, templates: AigcModelParamTemplate[]) {
  const keys = new Set(templates.map((template) => template.paramKey));
  return Object.fromEntries(
    Object.entries(params).filter(([key, value]) => keys.has(key) && value !== undefined && value !== null && value !== "")
  );
}

function formatElapsed(ms: number | null | undefined) {
  if (!ms || ms < 0) return "0s";
  const seconds = Math.floor(ms / 1000);
  if (seconds < 60) return `${seconds}s`;
  return `${Math.floor(seconds / 60)}m ${seconds % 60}s`;
}

function isQueuedStatus(status: string | null | undefined) {
  return status === "queued" || status === "CREATED" || status === "SUBMITTING" || status === "SUBMITTED" || status === "CALLBACK_WAITING";
}

function isRunningStatus(status: string | null | undefined) {
  return status === "running" || status === "processing" || status === "submitted" || status === "RUNNING" || status === "SYNCING" || status === "DOWNLOADING" || status === "ASSET_CREATING";
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
  const scale = Math.min(1, PREVIEW_SLOT_WIDTH / width, PREVIEW_SLOT_HEIGHT / height);
  return {
    width: Math.max(160, Math.round(width * scale)),
    height: Math.max(120, Math.round(height * scale)),
  };
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

export function VideoNodeComponent({ id, data, selected, dragging }: VideoNodeProps) {
  const { setNodes, setEdges, getNodes, getEdges } = useReactFlow();
  const updateNodeInternals = useUpdateNodeInternals();
  const zoom = useStore((s) => s.transform[2] || 1);
  const selectedNodeCount = useStore((s) => s.nodes.reduce((count, node) => count + (node.selected ? 1 : 0), 0));
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
  const [referencePickerPromptId, setReferencePickerPromptId] = useState<string | null>(null);
  const [modelOpen, setModelOpen] = useState(false);
  const [paramsOpen, setParamsOpen] = useState(false);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [startedAtMs, setStartedAtMs] = useState(() =>
    data.generationRunStartedAt ? new Date(data.generationRunStartedAt).getTime() : 0
  );
  const [now, setNow] = useState(startedAtMs);
  const paramsRef = useRef<HTMLDivElement>(null);
  const paramsBtnRef = useRef<HTMLButtonElement>(null);
  const modelRef = useRef<HTMLDivElement>(null);
  const modelBtnRef = useRef<HTMLButtonElement>(null);
  const activeRunPollRef = useRef<string | null>(null);

  const isGenerating = data.status === "pending";
  const kind = data.kind ?? "draft";
  const upstreamStatus = data.upstreamStatus ?? null;
  const isQueued = isGenerating && isQueuedStatus(upstreamStatus);
  const isRunning = isGenerating && isRunningStatus(upstreamStatus);
  const canCompose = kind !== "uploaded";
  const pickerActiveForThisNode = referencePickerPromptId === id;
  const elapsedMs = isRunning && startedAtMs > 0 ? now - startedAtMs : data.elapsedMs;
  const progressLabel = isQueued ? "排队中" : isRunning ? `生成中 ${formatElapsed(elapsedMs)}` : "提交中";
  const previewItem = useMemo(() => videoNodeToMediaPreview({ ...data, elapsedMs }), [data, elapsedMs]);
  const displaySize = getDisplaySize(data);
  const isOnlySelectedNode = selected && selectedNodeCount === 1;
  const showNodeActions = selectedNodeCount <= 1;
  const videoSrc = data.videoUrl || data.previewUrl;
  const fixedUiScale = 1 / zoom;
  const referenceImages = useMemo(() => {
    void referenceImagesSignature;
    const currentNodes = getNodes() as AppNode[];
    const currentEdges = getEdges() as AppEdge[];
    const images: { edgeId: string; nodeId: string; data: ImageNodeData | SketchNodeData }[] = [];
    for (const edge of currentEdges) {
      if (edge.target !== id) continue;
      const node = currentNodes.find((item) => item.id === edge.source);
      if (node?.type !== "image" && node?.type !== "sketch") continue;
      images.push({ edgeId: edge.id, nodeId: node.id, data: node.data as ImageNodeData | SketchNodeData });
    }
    return images;
  }, [getEdges, getNodes, id, referenceImagesSignature]);
  const generationCapability = referenceImages.length > 0 ? "IMAGE_TO_VIDEO" : "TEXT_TO_VIDEO";
  const rawParams = useMemo(() => data.params ?? {}, [data.params]);
  const aigcModels = useAigcModels({ type: 3, capability: generationCapability, preferredModelId: data.aigcModelId, params: rawParams });
  const storedAigcModel = aigcModels.models.find((model) => model.id === data.aigcModelId);
  const activeAigcModel = storedAigcModel ?? aigcModels.selectedModel;
  const activeAigcModelId = activeAigcModel?.id ?? data.aigcModelId;
  const activeModelName = activeAigcModel?.name ?? data.modelName ?? SEEDANCE_MODEL_NAME;
  const activeProviderModel = activeAigcModel?.model ?? data.providerModel ?? data.modelId;
  const effectiveParams = useMemo(() => filterModelParams(rawParams, aigcModels.templates), [aigcModels.templates, rawParams]);
  const costLabel = aigcModels.priceLoading ? "…" : formatCost(aigcModels.price?.salePrice);
  const canGenerate = Boolean(data.prompt.trim()) && !isGenerating && !aigcModels.loading && !aigcModels.templateLoading && Boolean(activeAigcModelId);

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
          node.id === id ? { ...node, data: { ...node.data, ...patch } } : node
        )
      );
      window.dispatchEvent(new CustomEvent<NodeDataPatchEventDetail>("copse:node-data-patch", {
        detail: { nodeId: id, patch, flush: options?.flush },
      }));
    },
    [id, setNodes]
  );

  const updateParams = useCallback(
    (patch: Record<string, unknown>) => {
      setNodes((nds) =>
        nds.map((node) => {
          if (node.id !== id) return node;
          const nodeData = node.data as VideoNodeData;
          return { ...node, data: { ...nodeData, params: { ...(nodeData.params ?? {}), ...patch }, ...patch } };
        })
      );
      window.dispatchEvent(new CustomEvent<NodeDataPatchEventDetail>("copse:node-data-patch", {
        detail: { nodeId: id, patch: { params: { ...(data.params ?? {}), ...patch }, ...patch } },
      }));
    },
    [data.params, id, setNodes]
  );

  const waitAndApplyServerRun = useCallback(async (projectId: string | number, taskId: number, startedAt: string) => {
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
      if (patch) updateData(patch as Partial<VideoNodeData>, { flush: true });
    } catch (error) {
      updateData({
        status: "failed",
        taskId: String(taskId),
        errorMessage: error instanceof Error ? error.message : "视频任务同步失败",
        upstreamStatus: "FAILED",
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
    if (data.status !== "pending" || !data.taskId) return;
    const projectId = new URLSearchParams(window.location.search).get("projectId");
    if (!isServerCanvasProjectId(projectId)) return;
    const taskId = Number(data.taskId);
    if (!Number.isFinite(taskId)) return;
    void waitAndApplyServerRun(projectId, taskId, data.generationStartedAt ?? data.createdAt);
  }, [data.createdAt, data.generationStartedAt, data.status, data.taskId, waitAndApplyServerRun]);

  useEffect(() => {
    if (aigcModels.loading || aigcModels.models.length === 0) return;
    if (data.aigcModelId && aigcModels.models.some((model) => model.id === data.aigcModelId)) return;
    const nextModel = aigcModels.selectedModel ?? aigcModels.models[0];
    if (!nextModel) return;
    updateData({
      modelId: String(nextModel.id),
      providerModel: nextModel.model,
      modelName: nextModel.name,
      aigcModelId: nextModel.id,
    });
  }, [aigcModels.loading, aigcModels.models, aigcModels.selectedModel, data.aigcModelId, updateData]);

  useEffect(() => {
    if (aigcModels.templateLoading || aigcModels.templates.length === 0) return;
    const patch: Record<string, unknown> = {};
    for (const template of aigcModels.templates) {
      if (hasParamValue(rawParams, template.paramKey)) continue;
      const fallback = template.paramKey === "ratio" || template.paramKey === "aspectRatio"
        ? data.ratio
        : template.paramKey === "resolution"
          ? data.resolution
          : template.paramKey === "duration"
            ? String(data.duration)
            : template.paramKey === "size"
              ? data.size ?? "1280*704"
              : template.paramKey === "generateAudio" || template.paramKey === "audio"
                ? String(data.generateAudio)
                : "";
      const nextValue = templateDefault(template, fallback);
      if (nextValue !== "") patch[template.paramKey] = nextValue;
    }
    if (Object.keys(patch).length > 0) updateParams(patch);
  }, [aigcModels.templateLoading, aigcModels.templates, data.duration, data.generateAudio, data.ratio, data.resolution, data.size, rawParams, updateParams]);

  const summary = useMemo(() => {
    if (aigcModels.templateLoading) return "参数加载中";
    if (aigcModels.templates.length === 0) return "默认参数";
    const values = aigcModels.templates
      .map((template) => normalizeTemplateOption(rawParams[template.paramKey] ?? template.defaultValue ?? ""))
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
    return () => window.removeEventListener("copse:reference-picker", handleReferencePicker);
  }, []);

  useEffect(() => {
    if (!isRunning) return;
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [isRunning]);

  useEffect(() => {
    if (!selected && pickerActiveForThisNode) {
      window.dispatchEvent(new CustomEvent<ReferencePickerEventDetail>("copse:reference-picker", {
        detail: { promptId: null },
      }));
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

  const removeReference = useCallback(
    (edgeId: string) => setEdges((eds) => eds.filter((edge) => edge.id !== edgeId)),
    [setEdges]
  );

  const openReferencePicker = useCallback(() => {
    if (isGenerating) return;
    window.dispatchEvent(new CustomEvent<ReferencePickerEventDetail>("copse:reference-picker", {
      detail: { promptId: id },
    }));
  }, [id, isGenerating]);

  const handleGenerate = useCallback(async () => {
    const prompt = data.prompt.trim();
    if (!prompt || isGenerating) return;

    if (!activeAigcModelId) {
      updateData({ status: "failed", errorMessage: "请选择 AIGC 视频模型后再生成。", upstreamStatus: "failed" });
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

    const projectId = new URLSearchParams(window.location.search).get("projectId");
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
          inputParams: JSON.stringify({
            ...effectiveParams,
            providerModel: activeProviderModel,
            referenceImageIds: referenceImages.map((image) => image.nodeId),
            referenceImages: referenceImages.map((image) => image.data.dataUrl || image.data.previewUrl).filter(Boolean),
          }),
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
          errorMessage: error instanceof Error ? error.message : "视频任务提交失败",
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
        inputParams: JSON.stringify({
          ...effectiveParams,
          providerModel: activeProviderModel,
          referenceImages: referenceImages.map((image) => image.data.dataUrl || image.data.previewUrl).filter(Boolean),
        }),
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
      const videoUrl = result.outputUrlList[0];

      if (result.status === "SUCCESS" && videoUrl) {
        updateData({
          status: "complete",
          kind: "generated",
          taskId: String(submit.taskId),
          videoUrl,
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
        errorMessage: error instanceof Error ? error.message : "视频任务提交失败",
        upstreamStatus: "FAILED",
        generationCompletedAt: new Date().toISOString(),
        elapsedMs: Date.now() - new Date(startedAt).getTime(),
      });
    }
  }, [activeAigcModelId, activeProviderModel, data.prompt, effectiveParams, generationCapability, id, isGenerating, referenceImages, updateData, waitAndApplyServerRun]);

  return (
    <>
    <div className="relative" style={{ width: displaySize.width, height: displaySize.height }}>
      <div className="relative overflow-visible" style={{ width: displaySize.width, height: displaySize.height }}>
        <AnimatePresence>
          {isOnlySelectedNode && !dragging && previewItem && (
            <SelectedMediaToolbar
              canDownload={Boolean(previewItem.url)}
              onDownload={handlePreviewDownload}
              onOpenPreview={() => setPreviewOpen(true)}
              uiScale={fixedUiScale}
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
            animate={{ opacity: 1, scale: 1, width: displaySize.width, height: displaySize.height }}
            transition={{
              opacity: { duration: 0.18, ease: "easeOut" },
              scale: { duration: 0.18, ease: "easeOut" },
              width: { type: "spring", stiffness: 360, damping: 34 },
              height: { type: "spring", stiffness: 360, damping: 34 },
            }}
            className={cn(
              "canvas-node-drag-handle group relative rounded-xl border bg-background shadow-[0_1px_3px_rgba(0,0,0,0.06)]",
              selected ? "border-charcoal/60 ring-2 ring-charcoal/35" : "border-border-warm"
            )}
            style={{ width: displaySize.width, height: displaySize.height, pointerEvents: "auto" }}
          >
            <div className="absolute inset-0 flex items-center justify-center overflow-hidden rounded-[inherit]">
              {videoSrc ? (
                <video src={videoSrc} className="size-full object-contain" controls />
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
                      transition={{ duration: 1.4, repeat: Infinity, ease: "easeInOut" }}
                    >
                      <Loader2 className="mb-2 size-7 animate-spin" />
                    </motion.div>
                    <span className="text-xs">{progressLabel}</span>
                    {data.taskId && <span className="mt-1 max-w-[260px] truncate text-[10px] opacity-70">{data.taskId}</span>}
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
                    <span className="text-xs">{data.errorMessage ?? "视频任务提交失败"}</span>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>

            <NodeCreateHandle nodeId={id} direction="incoming" selected={selected} showButton={showNodeActions} />
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
              {referenceImages.map((image) => (
                <div key={image.edgeId} className="group relative shrink-0">
                  <div className="size-9 overflow-hidden rounded-lg border border-border-warm bg-muted">
                    {(image.data.previewUrl || image.data.dataUrl) ? (
                      <img src={image.data.previewUrl || image.data.dataUrl} alt={image.data.fileName} className="size-full object-cover" draggable={false} />
                    ) : (
                      <ImageIcon className="m-2 size-5 text-muted-gray/40" />
                    )}
                  </div>
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
            {isGenerating && <span className="text-xs text-muted-gray">{progressLabel}</span>}
          </div>

          <textarea
            value={data.prompt}
            onChange={(event) => updateData({ prompt: event.target.value })}
            disabled={isGenerating}
            placeholder="Describe anything you want to generate"
            className="min-h-[130px] w-full resize-none bg-transparent text-base leading-7 text-charcoal placeholder:text-muted-gray focus:outline-none disabled:cursor-not-allowed disabled:text-muted-gray"
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
                    initial={{ opacity: 0, y: 4, scale: 0.98 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    exit={{ opacity: 0, y: 4, scale: 0.98 }}
                    transition={{ duration: 0.14, ease: "easeOut" }}
                    className="absolute bottom-full left-0 z-[260] mb-2 w-[320px] rounded-2xl border border-border-warm bg-background p-3 shadow-lg"
                  >
                    <div className="max-h-[320px] overflow-y-auto">
                      {aigcModels.models.length > 0 ? aigcModels.models.map((model) => {
                        const isSelected = activeAigcModelId === model.id;
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
                                provider: model.model === WAN_MODEL_ID ? "wan" : data.provider,
                              });
                              setModelOpen(false);
                            }}
                            className={cn(
                              "mb-1 flex w-full items-center justify-between rounded-xl px-3 py-3 text-left last:mb-0",
                              isSelected ? "bg-muted" : "hover:bg-muted"
                            )}
                          >
                            <span className="flex min-w-0 flex-col gap-1">
                              <span className="flex min-w-0 items-center gap-2 text-sm font-medium text-charcoal">
                                <Video className="size-4 shrink-0 text-muted-gray" />
                                <span className="truncate">{model.name}</span>
                              </span>
                              <span className="ml-6 text-xs text-muted-gray">{generationCapability}</span>
                            </span>
                            {isSelected && <Check className="size-4 shrink-0 text-charcoal" />}
                          </button>
                        );
                      }) : (
                        <div className="px-3 py-4 text-sm text-muted-gray">暂无可用视频模型</div>
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
                    initial={{ opacity: 0, y: 4, scale: 0.98 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    exit={{ opacity: 0, y: 4, scale: 0.98 }}
                    transition={{ duration: 0.14, ease: "easeOut" }}
                    className="absolute bottom-full left-0 z-[260] mb-2 w-[420px] rounded-2xl border border-border-warm bg-background p-4 shadow-lg"
                  >
                    <div className="mb-3 rounded-xl bg-muted px-3 py-2 text-xs text-muted-gray">
                      当前能力：{generationCapability}
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
                {isGenerating ? <Loader2 className="size-5 animate-spin" /> : <ArrowUp className="size-5" />}
              </button>
            </div>
          </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
    <MediaPreviewDialog item={previewItem} open={previewOpen} onClose={() => setPreviewOpen(false)} />
    </>
  );
}
