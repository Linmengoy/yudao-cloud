/* eslint-disable @next/next/no-img-element */
"use client";

import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import type { Node, NodeProps } from "@xyflow/react";
import { useReactFlow, useStore, useUpdateNodeInternals } from "@xyflow/react";
import { AnimatePresence, motion } from "motion/react";
import {
  ArrowUp,
  Check,
  ImageIcon,
  Loader2,
  Play,
  Plus,
  SlidersHorizontal,
  Sparkles,
  Video,
  Volume2,
  VolumeX,
  X,
} from "lucide-react";
import type { AppEdge, AppNode, ImageNodeData, ReferencePickerEventDetail, VideoNodeData } from "./types";
import { NodeCreateHandle } from "./NodeCreateHandle";
import { generationApi } from "@/features/generation/generation-api";
import { waitGenerationResult } from "@/features/generation/generation-poll";
import { cn } from "@/lib/utils";

type VideoNodeProps = NodeProps<Node<VideoNodeData, "video">>;

const CARD_WIDTH = 420;
const CARD_HEIGHT = 236;
const PREVIEW_SLOT_WIDTH = 420;
const PREVIEW_SLOT_HEIGHT = 420;
const COMPOSER_WIDTH = 680;
const SEEDANCE_MODEL_ID = "doubao-seedance-2-0-260128";
const SEEDANCE_MODEL_NAME = "Seedance 2.0";
const WAN_MODEL_ID = "wan2.2-ti2v-5b";
const WAN_MODEL_NAME = "Wan 2.2";
const RATIOS: VideoNodeData["ratio"][] = ["16:9", "4:3", "1:1", "3:4", "9:16", "21:9"];
const RESOLUTIONS: VideoNodeData["resolution"][] = ["480p", "720p", "1080p"];
const DURATIONS: VideoNodeData["duration"][] = [5, 10];
const WAN_SIZES = ["1280*704", "704*1280"] as const;

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

function VideoParamButton<T extends string | number>({
  children,
  group,
  selected,
  value,
  onClick,
  className,
}: {
  children: ReactNode;
  group: string;
  selected: boolean;
  value: T;
  onClick: () => void;
  className?: string;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn("relative overflow-hidden rounded-lg px-2 py-2 text-muted-gray transition-colors", className, selected && "text-charcoal")}
    >
      {selected && (
        <motion.span
          layoutId={`video-${group}-indicator`}
          className="absolute inset-0 rounded-lg bg-background shadow-sm"
          transition={{ type: "spring", stiffness: 420, damping: 34 }}
        />
      )}
      <span className="relative z-10">{children}</span>
      <span className="sr-only">{String(value)}</span>
    </button>
  );
}

export function VideoNodeComponent({ id, data, selected, dragging }: VideoNodeProps) {
  const { setNodes, setEdges } = useReactFlow();
  const updateNodeInternals = useUpdateNodeInternals();
  const edges = useStore((s) => s.edges) as AppEdge[];
  const nodes = useStore((s) => s.nodes) as AppNode[];
  const zoom = useStore((s) => s.transform[2] || 1);
  const [referencePickerPromptId, setReferencePickerPromptId] = useState<string | null>(null);
  const [modelOpen, setModelOpen] = useState(false);
  const [paramsOpen, setParamsOpen] = useState(false);
  const [startedAtMs, setStartedAtMs] = useState(() =>
    data.generationRunStartedAt ? new Date(data.generationRunStartedAt).getTime() : 0
  );
  const [now, setNow] = useState(startedAtMs);
  const paramsRef = useRef<HTMLDivElement>(null);
  const paramsBtnRef = useRef<HTMLButtonElement>(null);
  const modelRef = useRef<HTMLDivElement>(null);
  const modelBtnRef = useRef<HTMLButtonElement>(null);

  const isGenerating = data.status === "pending";
  const kind = data.kind ?? "draft";
  const isWanModel = data.provider === "wan" || data.modelId === WAN_MODEL_ID;
  const upstreamStatus = data.upstreamStatus ?? null;
  const isQueued = isGenerating && isQueuedStatus(upstreamStatus);
  const isRunning = isGenerating && isRunningStatus(upstreamStatus);
  const canCompose = kind !== "uploaded";
  const pickerActiveForThisNode = referencePickerPromptId === id;
  const elapsedMs = isRunning && startedAtMs > 0 ? now - startedAtMs : data.elapsedMs;
  const progressLabel = isQueued ? "排队中" : isRunning ? `生成中 ${formatElapsed(elapsedMs)}` : "提交中";
  const displaySize = getDisplaySize(data);
  const displayLeft = (PREVIEW_SLOT_WIDTH - displaySize.width) / 2;
  const displayTop = 28 + PREVIEW_SLOT_HEIGHT - displaySize.height;
  const selectedNodeCount = nodes.filter((node) => node.selected).length;
  const isOnlySelectedNode = selected && selectedNodeCount === 1;
  const showNodeActions = selectedNodeCount <= 1;
  const fixedUiScale = 1 / zoom;

  useEffect(() => {
    const frame = window.requestAnimationFrame(() => updateNodeInternals(id));
    const timeout = window.setTimeout(() => updateNodeInternals(id), 260);

    return () => {
      window.cancelAnimationFrame(frame);
      window.clearTimeout(timeout);
    };
  }, [displayLeft, displayTop, displaySize.width, displaySize.height, id, updateNodeInternals]);

  const updateData = useCallback(
    (patch: Partial<VideoNodeData>) => {
      setNodes((nds) =>
        nds.map((node) =>
          node.id === id ? { ...node, data: { ...node.data, ...patch } } : node
        )
      );
    },
    [id, setNodes]
  );

  const referenceImages = edges
    .filter((edge) => edge.target === id)
    .map((edge) => {
      const node = nodes.find((n) => n.id === edge.source);
      return node?.type === "image" ? { edgeId: edge.id, data: node.data as ImageNodeData } : null;
    })
    .filter((item): item is { edgeId: string; data: ImageNodeData } => item !== null);

  const summary = useMemo(() => {
    if (isWanModel) return `Frames · ${data.size ?? "1280*704"} · 121f · 5s`;
    return `Frames · ${data.ratio} · ${data.resolution} · ${data.duration}s · ${data.generateAudio ? "音频" : "静音"}`;
  }, [data.duration, data.generateAudio, data.ratio, data.resolution, data.size, isWanModel]);

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

    if (!data.aigcModelId) {
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

    try {
      const submit = await generationApi.submit({
        generateType: "VIDEO",
        generateMode: referenceImages.length > 0 ? "IMAGE_TO_VIDEO" : "TEXT_TO_VIDEO",
        modelId: data.aigcModelId,
        prompt,
        inputParams: JSON.stringify({
          providerModel: data.providerModel ?? data.modelId,
          ratio: data.ratio,
          resolution: data.resolution,
          duration: data.duration,
          size: data.size,
          generateAudio: data.generateAudio,
          watermark: data.watermark,
          referenceImages: referenceImages.map((image) => image.data.dataUrl).filter(Boolean),
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
  }, [data.aigcModelId, data.duration, data.generateAudio, data.modelId, data.prompt, data.providerModel, data.ratio, data.resolution, data.size, data.watermark, isGenerating, referenceImages, updateData]);

  return (
    <div className="relative" style={{ width: CARD_WIDTH }}>
      <div className="relative" style={{ width: PREVIEW_SLOT_WIDTH, height: PREVIEW_SLOT_HEIGHT + 28 }}>
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
          <Video className="size-4" />
          <span className="line-clamp-1" title={data.fileName}>
            Video
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
              "group relative rounded-xl border bg-background shadow-[0_1px_3px_rgba(0,0,0,0.06)]",
              selected ? "border-charcoal/60 ring-2 ring-charcoal/35" : "border-border-warm"
            )}
            style={{ width: displaySize.width, height: displaySize.height }}
          >
            <div className="absolute inset-0 flex items-center justify-center overflow-hidden rounded-[inherit]">
              {data.videoUrl ? (
                <video src={data.videoUrl} className="size-full object-contain" controls />
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
            className="nodrag nowheel rounded-xl border border-border-warm bg-background p-4 shadow-[0_8px_24px_rgba(28,28,28,0.08)]"
            style={{
              width: COMPOSER_WIDTH,
              marginLeft: (PREVIEW_SLOT_WIDTH - COMPOSER_WIDTH) / 2,
              marginTop: 12 * fixedUiScale,
              transformOrigin: "top center",
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
                    {image.data.dataUrl ? (
                      <img src={image.data.dataUrl} alt={image.data.fileName} className="size-full object-cover" draggable={false} />
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
                  <span>{data.modelName || SEEDANCE_MODEL_NAME}</span>
                </button>
                <AnimatePresence>
                  {modelOpen && (
                  <motion.div
                    ref={modelRef}
                    initial={{ opacity: 0, y: 4, scale: 0.98 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    exit={{ opacity: 0, y: 4, scale: 0.98 }}
                    transition={{ duration: 0.14, ease: "easeOut" }}
                    className="absolute bottom-full left-0 z-[200] mb-2 w-[320px] rounded-2xl border border-border-warm bg-background p-3 shadow-lg"
                  >
                    <div className="flex flex-col gap-2">
                      <button
                        type="button"
                        onClick={() => {
                          updateData({
                            provider: "seedance",
                            modelId: SEEDANCE_MODEL_ID,
                            modelName: SEEDANCE_MODEL_NAME,
                          });
                          setModelOpen(false);
                        }}
                        className={cn(
                          "flex w-full items-center justify-between rounded-xl px-3 py-3 text-left",
                          !isWanModel ? "bg-muted" : "hover:bg-muted"
                        )}
                      >
                        <span className="flex min-w-0 flex-col gap-1">
                          <span className="flex items-center gap-2 text-sm font-medium text-charcoal">
                            <Sparkles className="size-4 text-muted-gray" />
                            {SEEDANCE_MODEL_NAME}
                          </span>
                          <span className="text-xs text-muted-gray">1080p · 5-10s · 音频</span>
                        </span>
                        {!isWanModel && <Check className="size-4 text-charcoal" />}
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          updateData({
                            provider: "wan",
                            modelId: WAN_MODEL_ID,
                            modelName: WAN_MODEL_NAME,
                            size: data.size ?? "1280*704",
                            generateAudio: false,
                          });
                          setModelOpen(false);
                        }}
                        className={cn(
                          "flex w-full items-center justify-between rounded-xl px-3 py-3 text-left",
                          isWanModel ? "bg-muted" : "hover:bg-muted"
                        )}
                      >
                        <span className="flex min-w-0 flex-col gap-1">
                          <span className="flex items-center gap-2 text-sm font-medium text-charcoal">
                            <Video className="size-4 text-muted-gray" />
                            {WAN_MODEL_NAME}
                          </span>
                          <span className="text-xs text-muted-gray">T2V / I2V · 121 frames · 2 sizes</span>
                        </span>
                        {isWanModel && <Check className="size-4 text-charcoal" />}
                      </button>
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
                    className="absolute bottom-full left-0 z-[200] mb-2 w-[420px] rounded-2xl border border-border-warm bg-background p-4 shadow-lg"
                  >
                    <p className="mb-3 text-sm font-medium text-muted-gray">Generate method</p>
                    <div className="mb-4 rounded-xl bg-muted p-1">
                      <button type="button" className="w-full rounded-lg bg-background px-3 py-2 text-sm font-medium text-charcoal">
                        Frames
                      </button>
                    </div>

                    {isWanModel ? (
                      <>
                        <p className="mb-2 text-sm font-medium text-muted-gray">Size</p>
                        <div className="mb-3 grid grid-cols-2 gap-1 rounded-xl bg-muted p-1">
                          {WAN_SIZES.map((size) => (
                            <VideoParamButton
                              key={size}
                              group="wan-size"
                              value={size}
                              selected={(data.size ?? "1280*704") === size}
                              onClick={() => updateData({ size })}
                              className="text-sm"
                            >
                              {size}
                            </VideoParamButton>
                          ))}
                        </div>
                        <div className="rounded-xl bg-muted px-3 py-3 text-xs leading-5 text-muted-gray">
                          <p>frame_num 121 · sample_steps 20</p>
                          <p>无参考图走文生视频；连接 1 张图片后走图生视频。</p>
                        </div>
                      </>
                    ) : (
                      <>
                        <p className="mb-2 text-sm font-medium text-muted-gray">Aspect Ratio</p>
                        <div className="mb-4 grid grid-cols-6 gap-1 rounded-xl bg-muted p-1">
                          {RATIOS.map((ratio) => (
                            <VideoParamButton
                              key={ratio}
                              group="ratio"
                              value={ratio}
                              selected={data.ratio === ratio}
                              onClick={() => updateData({ ratio })}
                              className="text-xs"
                            >
                              {ratio}
                            </VideoParamButton>
                          ))}
                        </div>

                        <p className="mb-2 text-sm font-medium text-muted-gray">Resolution</p>
                        <div className="mb-4 grid grid-cols-3 gap-1 rounded-xl bg-muted p-1">
                          {RESOLUTIONS.map((resolution) => (
                            <VideoParamButton
                              key={resolution}
                              group="resolution"
                              value={resolution}
                              selected={data.resolution === resolution}
                              onClick={() => updateData({ resolution })}
                              className="text-sm"
                            >
                              {resolution}
                            </VideoParamButton>
                          ))}
                        </div>

                        <p className="mb-2 text-sm font-medium text-muted-gray">Duration</p>
                        <div className="mb-4 grid grid-cols-2 gap-1 rounded-xl bg-muted p-1">
                          {DURATIONS.map((duration) => (
                            <VideoParamButton
                              key={duration}
                              group="duration"
                              value={duration}
                              selected={data.duration === duration}
                              onClick={() => updateData({ duration })}
                              className="text-sm"
                            >
                              {duration}s
                            </VideoParamButton>
                          ))}
                        </div>

                        <p className="mb-2 flex items-center gap-1 text-sm font-medium text-muted-gray">
                          Generate Audio
                          {data.generateAudio ? <Volume2 className="size-3.5" /> : <VolumeX className="size-3.5" />}
                        </p>
                        <div className="grid grid-cols-2 gap-1 rounded-xl bg-muted p-1">
                          <VideoParamButton
                            group="audio"
                            value="on"
                            selected={data.generateAudio}
                            onClick={() => updateData({ generateAudio: true })}
                            className="text-sm"
                          >
                            On
                          </VideoParamButton>
                          <VideoParamButton
                            group="audio"
                            value="off"
                            selected={!data.generateAudio}
                            onClick={() => updateData({ generateAudio: false })}
                            className="text-sm"
                          >
                            Off
                          </VideoParamButton>
                        </div>
                      </>
                    )}
                  </motion.div>
                  )}
                </AnimatePresence>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <span className="rounded-lg px-2 py-1 text-sm text-muted-gray">1x</span>
              <button
                type="button"
                onClick={handleGenerate}
                disabled={!data.prompt.trim() || isGenerating}
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
  );
}
