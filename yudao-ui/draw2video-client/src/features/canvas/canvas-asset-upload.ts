import { getMyAsset, uploadAsset } from "@/features/assets/asset-api";
import { getAssetPreviewExpireTime, getAssetPreviewUrl } from "@/features/assets/asset-dictionaries";
import type { ImageNodeData, VideoNodeData } from "@/features/canvas/types";

export async function attachImageAsset(file: File, data: ImageNodeData): Promise<ImageNodeData> {
  const assetId = await uploadAsset(file, "IMAGE", data.fileName || file.name);
  const asset = await getMyAsset(assetId);
  const previewUrl = getAssetPreviewUrl(asset);
  return {
    ...data,
    assetId,
    previewUrl: previewUrl ?? data.dataUrl,
    assetUrlExpireTime: getAssetPreviewExpireTime(asset) ?? null,
  };
}

export async function attachVideoAsset(file: File, data: VideoNodeData): Promise<VideoNodeData> {
  const assetId = await uploadAsset(file, "VIDEO", data.fileName || file.name);
  const asset = await getMyAsset(assetId);
  const previewUrl = getAssetPreviewUrl(asset);
  return {
    ...data,
    assetId,
    previewUrl: previewUrl ?? data.videoUrl ?? null,
    videoUrl: previewUrl ?? data.videoUrl,
    assetUrlExpireTime: getAssetPreviewExpireTime(asset) ?? null,
  };
}
