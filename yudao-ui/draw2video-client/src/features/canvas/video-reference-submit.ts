import type { ImageNodeData, SketchNodeData } from "./types";

type ReferenceInput = {
  nodeId: string;
  data: ImageNodeData | SketchNodeData;
};

export function getReferenceAssetId(data: ImageNodeData | SketchNodeData) {
  if ("assetId" in data && typeof data.assetId === "number")
    return data.assetId;
  if ("outputAssetId" in data && typeof data.outputAssetId === "number")
    return data.outputAssetId;
  if ("assetIds" in data && Array.isArray(data.assetIds)) {
    const assetId = data.assetIds.find(
      (item) => typeof item === "number" && item > 0,
    );
    if (assetId) return assetId;
  }
  return null;
}

export function appendReferenceSubmitParams(
  params: Record<string, unknown>,
  references: ReferenceInput[],
  referenceImages: string[],
) {
  const referenceAssetIds = references
    .map((image) => getReferenceAssetId(image.data))
    .filter((assetId): assetId is number => typeof assetId === "number");

  if (referenceAssetIds.length > 0) params.referenceAssetIds = referenceAssetIds;
  if (referenceImages.length > 0) params.referenceImages = referenceImages;
  if (references.length > 0) {
    params.referenceImageIds = references.map((img) => img.nodeId);
  }

  return params;
}
