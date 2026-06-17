import type { CommunityPost } from "@/features/community/community-types";
import type { AigcModel } from "@/features/generation/model-api";

export type QuickGenerationMode = "TEXT_TO_IMAGE" | "IMAGE_TO_IMAGE" | "TEXT_TO_VIDEO" | "IMAGE_TO_VIDEO";
export type GenerationTab = "image" | "video";

export const QUICK_GENERATION_MODE_LABELS: Record<QuickGenerationMode, string> = {
  TEXT_TO_IMAGE: "文生图",
  IMAGE_TO_IMAGE: "图生图",
  TEXT_TO_VIDEO: "文生视频",
  IMAGE_TO_VIDEO: "图生视频",
};

export function workspaceModelsCacheKey(imageCapability: QuickGenerationMode, videoCapability: QuickGenerationMode) {
  return `workspace:models:${imageCapability}:${videoCapability}`;
}

export function workspaceCommunityCacheKey(ownerKey: string | number | null | undefined) {
  return `workspace:community-hot:${ownerKey ?? "current"}`;
}

export function selectedTabType(tab: GenerationTab) {
  return tab === "video" ? 3 : 2;
}

export function getSelectedTabModels(models: AigcModel[], tab: GenerationTab) {
  const type = selectedTabType(tab);
  return models.filter((model) => model.type === type);
}

export function modelSupportsCapability(model: AigcModel, capability: QuickGenerationMode) {
  return !model.capabilities?.length || model.capabilities.includes(capability);
}

export function getModelMaxReferenceImages(model: AigcModel) {
  const hintText = [model.code, model.name, model.model, model.providerModel, model.remark, ...(model.capabilities ?? [])]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();
  const explicitMatch = hintText.match(/(?:max(?:imum)?[_\s-]*reference(?:s|[_\s-]*images)?|最多|至多|参考图)\D{0,12}([1-9]\d*)/);
  if (explicitMatch) return Number(explicitMatch[1]);
  if (hintText.includes("multi_ref") || hintText.includes("multi-ref") || hintText.includes("multi reference") || hintText.includes("multi-reference") || hintText.includes("多参考")) {
    return Number.POSITIVE_INFINITY;
  }
  if (modelSupportsCapability(model, "IMAGE_TO_IMAGE") || modelSupportsCapability(model, "IMAGE_TO_VIDEO")) {
    return 1;
  }
  return 0;
}

export function getCompatibleModels(
  models: AigcModel[],
  tab: GenerationTab,
  capability: QuickGenerationMode,
  referenceImageCount = 0
) {
  return getSelectedTabModels(models, tab).filter((model) => (
    modelSupportsCapability(model, capability)
    && (referenceImageCount === 0 || getModelMaxReferenceImages(model) >= referenceImageCount)
  ));
}

export function pickDefaultModelId(
  models: AigcModel[],
  tab: GenerationTab = "image",
  capability: QuickGenerationMode = getQuickGenerationMode(tab, false),
  referenceImageCount = 0
) {
  const tabModels = getCompatibleModels(models, tab, capability, referenceImageCount);
  return tabModels.find((item) => item.defaultModel)?.id
    ?? tabModels[0]?.id
    ?? null;
}

export function getQuickGenerationMode(tab: GenerationTab, hasReferenceImages: boolean): QuickGenerationMode {
  if (tab === "video") return hasReferenceImages ? "IMAGE_TO_VIDEO" : "TEXT_TO_VIDEO";
  return hasReferenceImages ? "IMAGE_TO_IMAGE" : "TEXT_TO_IMAGE";
}

export function communityPostCoverUrl(post: CommunityPost) {
  return post.coverUrl || post.fileUrl || "";
}
