import type { ImageNodeData, VideoNodeData } from "./types";

const ACCEPTED_TYPES = new Set(["image/png", "image/jpeg", "image/webp", "image/gif"]);
const ACCEPTED_VIDEO_TYPES = new Set(["video/mp4", "video/quicktime"]);
const ACCEPTED_VIDEO_EXTENSIONS = [".mp4", ".mov"];
export const MAX_VIDEO_UPLOAD_BYTES = 200 * 1024 * 1024;
export const MIN_VIDEO_UPLOAD_SECONDS = 2;
export const MAX_VIDEO_UPLOAD_SECONDS = 180;

export function isAcceptedImageType(mimeType: string): boolean {
  return ACCEPTED_TYPES.has(mimeType);
}

export function isAcceptedVideoFile(file: File): boolean {
  const name = file.name.toLowerCase();
  return ACCEPTED_VIDEO_TYPES.has(file.type) || ACCEPTED_VIDEO_EXTENSIONS.some((ext) => name.endsWith(ext));
}

export function isAcceptedCanvasFile(file: File): boolean {
  return isAcceptedImageType(file.type) || isAcceptedVideoFile(file);
}

export interface VideoFileNodeData {
  data: VideoNodeData;
  blob: Blob;
}

export async function fileToImageNodeData(file: File): Promise<ImageNodeData> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onerror = () => reject(new Error("Failed to read file"));
    reader.onload = () => {
      const dataUrl = reader.result as string;
      const img = new Image();
      img.onload = () => {
        resolve({
          imageId: `img_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
          fileName: file.name,
          dataUrl,
          mimeType: file.type,
          width: img.naturalWidth,
          height: img.naturalHeight,
          createdAt: new Date().toISOString(),
          kind: "uploaded",
          status: "idle",
        });
      };
      img.onerror = () => {
        // Still resolve without dimensions
        resolve({
          imageId: `img_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
          fileName: file.name,
          dataUrl,
          mimeType: file.type,
          createdAt: new Date().toISOString(),
          kind: "uploaded",
          status: "idle",
        });
      };
      img.src = dataUrl;
    };
    reader.readAsDataURL(file);
  });
}

export async function fileToVideoNodeData(file: File): Promise<VideoFileNodeData> {
  if (!isAcceptedVideoFile(file)) {
    throw new Error("仅支持 MP4 或 MOV 视频");
  }
  if (file.size > MAX_VIDEO_UPLOAD_BYTES) {
    throw new Error("视频不能超过 200MB");
  }

  const objectUrl = URL.createObjectURL(file);
  let width = 0;
  let height = 0;
  let durationSec = 0;

  try {
    await new Promise<void>((resolve, reject) => {
      const video = document.createElement("video");
      video.preload = "metadata";
      video.onloadedmetadata = () => {
        width = video.videoWidth;
        height = video.videoHeight;
        durationSec = video.duration;
        resolve();
      };
      video.onerror = () => reject(new Error("无法读取视频信息"));
      video.src = objectUrl;
    });
  } finally {
    URL.revokeObjectURL(objectUrl);
  }

  if (!Number.isFinite(durationSec) || durationSec < MIN_VIDEO_UPLOAD_SECONDS || durationSec > MAX_VIDEO_UPLOAD_SECONDS) {
    throw new Error("视频时长需在 2 秒到 3 分钟之间");
  }

  const videoId = `vid_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
  return {
    blob: file,
    data: {
      videoId,
      fileName: file.name,
      mimeType: file.type || (file.name.toLowerCase().endsWith(".mov") ? "video/quicktime" : "video/mp4"),
      width: width || undefined,
      height: height || undefined,
      durationSec,
      sizeBytes: file.size,
      videoUrl: URL.createObjectURL(file),
      prompt: "",
      provider: "seedance",
      modelId: "doubao-seedance-2-0-260128",
      modelName: "Seedance 2.0",
      kind: "uploaded",
      status: "complete",
      taskId: null,
      errorMessage: null,
      ratio: "16:9",
      resolution: "1080p",
      duration: 5,
      size: "1280*704",
      generateAudio: true,
      watermark: false,
      createdAt: new Date().toISOString(),
      generationStartedAt: null,
      generationRunStartedAt: null,
      elapsedMs: null,
      upstreamStatus: null,
    },
  };
}

export function getFilesFromDrop(e: React.DragEvent | DragEvent): File[] {
  const files: File[] = [];
  if (e.dataTransfer?.items) {
    for (let i = 0; i < e.dataTransfer.items.length; i++) {
      const item = e.dataTransfer.items[i];
      if (item.kind === "file") {
        const file = item.getAsFile();
        if (file && isAcceptedCanvasFile(file)) {
          files.push(file);
        }
      }
    }
  } else if (e.dataTransfer?.files) {
    for (let i = 0; i < e.dataTransfer.files.length; i++) {
      const file = e.dataTransfer.files[i];
      if (isAcceptedCanvasFile(file)) {
        files.push(file);
      }
    }
  }
  return files;
}

// TODO: upload images to /app-api/infra/file/upload and store fileId/url instead of dataUrl
// POST /app-api/infra/file/upload — multipart form with file
// Response: { data: { url: string } }
// Then store only the URL in ImageNodeData instead of the full dataUrl
