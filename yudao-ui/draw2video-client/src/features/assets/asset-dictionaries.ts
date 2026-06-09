import type { AigcAsset, AigcAssetAuditStatus, AigcAssetType, AigcAssetVisibility } from "./asset-types";

export const ASSET_TYPE_LABELS: Record<string, string> = {
  IMAGE: "图片",
  VIDEO: "视频",
  AUDIO: "音频",
  DOCUMENT: "文档",
  PPT: "PPT",
  SUBTITLE: "字幕",
  COVER: "封面",
  DIGITAL_HUMAN_VIDEO: "数字人视频",
  OTHER: "其它",
};

export const ASSET_VISIBILITY_LABELS: Record<string, string> = {
  PRIVATE: "仅自己可见",
  PUBLIC: "公开",
  LINK: "链接可见",
  TENANT: "团队内可见",
};

export const ASSET_AUDIT_STATUS_LABELS: Record<string, string> = {
  PENDING: "审核中",
  PASS: "已通过",
  REJECT: "未通过",
  MANUAL_REVIEW: "人工复审中",
};

export function getAssetTypeLabel(type?: string) {
  return type ? ASSET_TYPE_LABELS[type] ?? type : "其它";
}

export function getAssetVisibilityLabel(visibility?: string) {
  return visibility ? ASSET_VISIBILITY_LABELS[visibility] ?? visibility : "仅自己可见";
}

export function getAssetAuditStatusLabel(status?: string) {
  return status ? ASSET_AUDIT_STATUS_LABELS[status] ?? status : "审核中";
}

export function getAssetPreviewUrl(asset: AigcAsset) {
  return asset.thumbnailUrl || asset.coverUrl || asset.fileUrl || getAssetPreviewFile(asset)?.accessUrl || "";
}

export function getAssetPreviewExpireTime(asset: AigcAsset) {
  return getAssetPreviewFile(asset)?.expireTime;
}

function getAssetPreviewFile(asset: AigcAsset) {
  const files = asset.files ?? [];
  return files.find((file) => file.fileRole === "THUMBNAIL")
    ?? files.find((file) => file.fileRole === "COVER")
    ?? files.find((file) => file.fileRole === "PREVIEW")
    ?? files.find((file) => file.fileRole === "ORIGINAL")
    ?? files[0];
}

export function formatFileSize(size?: number) {
  if (!size) return "-";
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  if (size < 1024 * 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`;
  return `${(size / 1024 / 1024 / 1024).toFixed(1)} GB`;
}

export function formatDuration(duration?: number) {
  if (!duration) return "-";
  const seconds = duration > 1000 ? Math.round(duration / 1000) : Math.round(duration);
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return minutes > 0 ? `${minutes}:${String(rest).padStart(2, "0")}` : `${rest}s`;
}

export function formatDateTime(value?: string) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";
  return date.toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function isFileAssetType(type?: string): type is AigcAssetType {
  return Boolean(type && ASSET_TYPE_LABELS[type]);
}

export function isAssetVisibility(visibility?: string): visibility is AigcAssetVisibility {
  return Boolean(visibility && ASSET_VISIBILITY_LABELS[visibility]);
}

export function isAssetAuditStatus(status?: string): status is AigcAssetAuditStatus {
  return Boolean(status && ASSET_AUDIT_STATUS_LABELS[status]);
}

export function canDownloadAsset(asset: Pick<AigcAsset, "auditStatus" | "status">) {
  return asset.status !== "DISABLED" && asset.status !== "DELETED" && asset.auditStatus === "PASS";
}

export function canPublishAsset(asset: Pick<AigcAsset, "auditStatus" | "status">) {
  return asset.status === "NORMAL" && asset.auditStatus === "PASS";
}
