export type AigcAssetType =
  | "IMAGE"
  | "VIDEO"
  | "AUDIO"
  | "DOCUMENT"
  | "PPT"
  | "SUBTITLE"
  | "COVER"
  | "DIGITAL_HUMAN_VIDEO"
  | "OTHER";

export type AigcAssetSourceType = "GENERATE" | "UPLOAD" | "IMPORT" | "EDIT" | "CLONE";

export type AigcAssetVisibility = "PRIVATE" | "PUBLIC" | "LINK" | "TENANT";

export type AigcAssetAuditStatus = "PENDING" | "PASS" | "REJECT" | "MANUAL_REVIEW";

export type AigcAssetStatus = "NORMAL" | "DELETED" | "DISABLED";

export interface PageResult<T> {
  list: T[];
  total: number;
}

export interface AigcAsset {
  id: number;
  assetNo?: string;
  userId?: number;
  assetType: AigcAssetType | string;
  sourceType?: AigcAssetSourceType | string;
  bizType?: string;
  bizId?: string | number;
  taskId?: number;
  taskNo?: string;
  title?: string;
  description?: string;
  tags?: string;
  fileUrl?: string;
  coverUrl?: string;
  thumbnailUrl?: string;
  mimeType?: string;
  fileExt?: string;
  fileSize?: number;
  width?: number;
  height?: number;
  duration?: number;
  metadata?: string;
  visibility?: AigcAssetVisibility | string;
  auditStatus?: AigcAssetAuditStatus | string;
  auditReason?: string;
  status?: AigcAssetStatus | string;
  viewCount?: number;
  downloadCount?: number;
  useCount?: number;
  createTime?: string;
  localProjectId?: string;
  localNodeId?: string;
  files?: AigcAssetFile[];
}

export interface AigcAssetFile {
  assetFileId: number;
  fileRole: string;
  fileName?: string;
  fileExt?: string;
  mimeType?: string;
  fileSize?: number;
  width?: number;
  height?: number;
  duration?: number;
  accessUrl?: string;
  expireSeconds?: number;
  expireTime?: string;
  publicAccess?: boolean;
}

export interface AigcAssetPageParams {
  pageNo: number;
  pageSize: number;
  assetType?: string;
  category?: string;
  sourceType?: string;
  title?: string;
}

export interface AigcAssetCategoryCounts {
  allCount: number;
  generatedImageCount: number;
  uploadedImageCount: number;
  videoCount: number;
  otherCount: number;
}

export interface AigcAssetAccessUrlReq {
  assetId: number | string;
  fileRole: string;
  accessType: string;
}

export interface AigcAssetAccessUrlResp {
  assetId: number;
  assetFileId?: number;
  fileRole: string;
  accessType: string;
  url: string;
  expireSeconds?: number;
  expireTime?: string;
  publicAccess?: boolean;
  cacheHit?: boolean;
}

export interface AigcAssetUpdateReq {
  id: number | string;
  title?: string;
  description?: string;
  tags?: string;
}

export interface AigcAssetVisibilityReq {
  id: number | string;
  visibility: AigcAssetVisibility | string;
}

export interface AigcAssetDownloadReq {
  assetId: number | string;
}
