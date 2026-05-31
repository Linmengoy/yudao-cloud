import { getMyAsset, uploadAsset } from "@/features/assets/asset-api";
import type { ImageNodeData, VideoNodeData } from "@/features/canvas/types";

function getAssetPreviewUrl(asset: Awaited<ReturnType<typeof getMyAsset>>) {
  return asset.thumbnailUrl || asset.coverUrl || asset.fileUrl || null;
}

export async function attachImageAsset(file: File, data: ImageNodeData): Promise<ImageNodeData> {
  const assetId = await uploadAsset(file, "IMAGE", data.fileName || file.name);
  const asset = await getMyAsset(assetId);
  return {
    ...data,
    assetId,
    previewUrl: getAssetPreviewUrl(asset) ?? data.dataUrl,
  };
}

export async function attachVideoAsset(file: File, data: VideoNodeData): Promise<VideoNodeData> {
  const assetId = await uploadAsset(file, "VIDEO", data.fileName || file.name);
  const asset = await getMyAsset(assetId);
  return {
    ...data,
    assetId,
    previewUrl: getAssetPreviewUrl(asset) ?? data.videoUrl ?? null,
    videoUrl: asset.fileUrl ?? data.videoUrl,
  };
}
