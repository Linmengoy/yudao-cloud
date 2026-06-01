import type { ImageNodeData, VideoNodeData } from "@/features/canvas/types";
import { getModelById } from "@/features/image-generation/models";
import type { MediaPreviewInfoItem, MediaPreviewItem } from "./types";

function formatDateTime(value: string | null | undefined) {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return date.toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function formatElapsed(ms: number | null | undefined) {
  if (!ms || ms < 0) return null;
  const seconds = Math.max(1, Math.round(ms / 1000));
  if (seconds < 60) return `${seconds}s`;
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return rest ? `${minutes}m ${rest}s` : `${minutes}m`;
}

function formatDimensions(width: number | undefined, height: number | undefined) {
  if (!width || !height) return null;
  return `${width} x ${height}`;
}

function formatAspectRatio(width: number | undefined, height: number | undefined) {
  if (!width || !height) return null;
  const gcd = (a: number, b: number): number => (b === 0 ? a : gcd(b, a % b));
  const divisor = gcd(width, height);
  return `${Math.round(width / divisor)}:${Math.round(height / divisor)}`;
}

function formatBoolean(value: boolean | null | undefined) {
  if (value === null || value === undefined) return null;
  return value ? "On" : "Off";
}

export function compactInfo(items: MediaPreviewInfoItem[]) {
  return items.filter((item) => {
    if (item.value === null || item.value === undefined) return false;
    if (typeof item.value === "string" && item.value.trim() === "") return false;
    return true;
  });
}

export function imageNodeToMediaPreview(data: ImageNodeData): MediaPreviewItem | null {
  const url = data.previewUrl || data.dataUrl;
  if (!url) return null;
  const params = data.params;
  const modelName = data.modelName || (data.modelId ? getModelById(data.modelId)?.name ?? data.modelId : null);
  return {
    kind: "image",
    url,
    title: data.fileName || "Image",
    fileName: data.fileName,
    prompt: data.prompt,
    createdAt: data.generationCompletedAt ?? data.createdAt,
    information: compactInfo([
      { label: "Model", value: modelName },
      { label: "Provider", value: data.providerModel },
      { label: "Quality", value: params?.quality },
      { label: "Size", value: params?.size },
      { label: "Dimensions", value: formatDimensions(data.width, data.height) },
      { label: "Aspect Ratio", value: formatAspectRatio(data.width, data.height) },
      { label: "Format", value: params?.output_format?.toUpperCase() },
      { label: "Compression", value: params?.output_compression },
      { label: "Moderation", value: params?.moderation },
      { label: "Count", value: params?.n },
      { label: "Elapsed", value: formatElapsed(data.elapsedMs) },
      { label: "Date", value: formatDateTime(data.generationCompletedAt ?? data.createdAt) },
      { label: "File", value: data.fileName },
    ]),
  };
}

export function videoNodeToMediaPreview(data: VideoNodeData): MediaPreviewItem | null {
  const url = data.videoUrl || data.previewUrl;
  if (!url) return null;
  const isWan = data.provider === "wan" || data.modelId === "wan2.2-ti2v-5b";
  return {
    kind: "video",
    url,
    title: data.fileName || "Video",
    fileName: data.fileName,
    prompt: data.prompt,
    createdAt: data.generationCompletedAt ?? data.generationStartedAt ?? data.createdAt,
    information: compactInfo([
      { label: "Model", value: data.modelName || data.modelId },
      { label: "Provider", value: data.providerModel || data.provider },
      { label: "Size", value: isWan ? data.size : null },
      { label: "Aspect Ratio", value: isWan ? null : data.ratio },
      { label: "Resolution", value: isWan ? null : data.resolution },
      { label: "Duration", value: data.duration ? `${data.duration}s` : null },
      { label: "Audio", value: isWan ? null : formatBoolean(data.generateAudio) },
      { label: "Watermark", value: isWan ? null : formatBoolean(data.watermark) },
      { label: "Dimensions", value: formatDimensions(data.width, data.height) },
      { label: "Elapsed", value: formatElapsed(data.elapsedMs) },
      { label: "Status", value: data.upstreamStatus ?? data.taskStatus },
      { label: "Date", value: formatDateTime(data.generationCompletedAt ?? data.generationStartedAt ?? data.createdAt) },
      { label: "File", value: data.fileName },
    ]),
  };
}

export function downloadMedia(item: MediaPreviewItem) {
  const link = document.createElement("a");
  link.href = item.url;
  link.download = item.fileName || item.title || (item.kind === "video" ? "video" : "image");
  document.body.appendChild(link);
  link.click();
  link.remove();
}
