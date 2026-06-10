import { uploadAssetAndGetInfo } from "@/features/assets/asset-api";
import { getAssetPreviewExpireTime, getAssetPreviewUrl } from "@/features/assets/asset-dictionaries";
import type { ImageNodeData, VideoNodeData } from "@/features/canvas/types";

export async function attachImageAsset(file: File, data: ImageNodeData): Promise<ImageNodeData> {
  const asset = await uploadAssetAndGetInfo(file, "IMAGE", {
    title: data.fileName || file.name,
    width: data.width,
    height: data.height,
    metadata: {
      source: "canvas-image-node",
      imageId: data.imageId,
    },
  });
  const previewUrl = getAssetPreviewUrl(asset);
  return {
    ...data,
    assetId: asset.id,
    previewUrl: previewUrl ?? data.dataUrl,
    assetUrlExpireTime: getAssetPreviewExpireTime(asset) ?? null,
  };
}

export async function attachVideoAsset(file: File, data: VideoNodeData): Promise<VideoNodeData> {
  const asset = await uploadAssetAndGetInfo(file, "VIDEO", {
    title: data.fileName || file.name,
    width: data.width,
    height: data.height,
    duration: data.durationSec,
    metadata: {
      source: "canvas-video-node",
      videoId: data.videoId,
      sizeBytes: data.sizeBytes,
    },
  });
  const previewUrl = getAssetPreviewUrl(asset);
  return {
    ...data,
    assetId: asset.id,
    previewUrl: previewUrl ?? data.videoUrl ?? null,
    videoUrl: previewUrl ?? data.videoUrl,
    assetUrlExpireTime: getAssetPreviewExpireTime(asset) ?? null,
  };
}
