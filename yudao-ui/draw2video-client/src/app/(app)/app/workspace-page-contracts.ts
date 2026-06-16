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

export function pickDefaultModelId(models: AigcModel[], tab: GenerationTab = "image") {
  const tabModels = getSelectedTabModels(models, tab);
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
