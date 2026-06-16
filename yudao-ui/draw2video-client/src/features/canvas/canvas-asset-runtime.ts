import type { AppNode, ImageNodeData, VideoNodeData } from "@/features/canvas/types";

export function getNodeAssetId(node: AppNode) {
  if (node.type !== "image" && node.type !== "video") return null;
  const data = node.data as ImageNodeData | VideoNodeData;
  return data.assetId ?? data.outputAssetId ?? null;
}

export function collectNodeAssetIds(node: AppNode) {
  if (node.type !== "image" && node.type !== "video") return [];
  const data = node.data as ImageNodeData | VideoNodeData;
  const ids = new Set<number>();
  [data.assetId, data.outputAssetId].forEach((assetId) => {
    if (typeof assetId === "number") ids.add(assetId);
  });
  if (Array.isArray(data.outputs)) {
    data.outputs.forEach((output) => {
      if (typeof output.assetId === "number") ids.add(output.assetId);
    });
  }
  return [...ids];
}

export function getNodeAssetAccessRequest(node: AppNode, assetId: number) {
  if (node.type === "video") {
    return { assetId, fileRole: "ORIGINAL", accessType: "PREVIEW" };
  }
  return { assetId, fileRole: "ORIGINAL", accessType: "PREVIEW" };
}

export function withFreshAssetUrl(
  node: AppNode,
  url: string,
  expireTime?: string | null,
  assetId = getNodeAssetId(node),
): AppNode {
  if (node.type === "video") {
    const outputs = Array.isArray(node.data.outputs)
      ? node.data.outputs.map((output) => (
          output.assetId === assetId
            ? { ...output, previewUrl: url, videoUrl: url }
            : output
        ))
      : node.data.outputs;
    const shouldUpdatePrimary = assetId === getNodeAssetId(node);
    return {
      ...node,
      data: {
        ...node.data,
        ...(shouldUpdatePrimary ? { assetId, previewUrl: url, videoUrl: url } : {}),
        outputs,
        assetUrlExpireTime: expireTime ?? null,
      },
    } as AppNode;
  }
  if (node.type === "image") {
    const outputs = Array.isArray(node.data.outputs)
      ? node.data.outputs.map((output) => (
          output.assetId === assetId
            ? { ...output, previewUrl: url }
            : output
        ))
      : node.data.outputs;
    const shouldUpdatePrimary = assetId === getNodeAssetId(node);
    return {
      ...node,
      data: {
        ...node.data,
        ...(shouldUpdatePrimary ? { assetId, previewUrl: url, outputPreviewUrl: url } : {}),
        outputs,
        assetUrlExpireTime: expireTime ?? null,
      },
    } as AppNode;
  }
  return node;
}
