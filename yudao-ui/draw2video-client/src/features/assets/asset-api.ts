import { api } from "@/lib/api-client";
import type {
  AigcAsset,
  AigcAssetAccessUrlReq,
  AigcAssetAccessUrlResp,
  AigcAssetCategoryCounts,
  AigcAssetDirectUploadPrepareResp,
  AigcAssetType,
  AigcAssetDownloadReq,
  AigcAssetPageParams,
  AigcAssetUpdateReq,
  AigcAssetVisibilityReq,
  PageResult,
} from "./asset-types";

function toQuery(params: object) {
  const search = new URLSearchParams();
  Object.entries(params as Record<string, unknown>).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, String(value));
    }
  });
  const query = search.toString();
  return query ? `?${query}` : "";
}

export function getMyAssetPage(params: AigcAssetPageParams) {
  return api.get<PageResult<AigcAsset>>(`/aigc/asset/my-page${toQuery(params)}`);
}

export function getMyAssetCategoryCounts(params: Partial<Omit<AigcAssetPageParams, "pageNo" | "pageSize">> = {}) {
  return api.get<AigcAssetCategoryCounts>(`/aigc/asset/my-category-counts${toQuery(params)}`);
}

export function getMyAssetList(params: Partial<Omit<AigcAssetPageParams, "pageNo" | "pageSize">> = {}) {
  return api.get<AigcAsset[]>(`/aigc/asset/my-list${toQuery(params)}`);
}

export function getMyAsset(id: number | string) {
  return api.get<AigcAsset>(`/aigc/asset/my-get${toQuery({ id })}`);
}

export function getAssetAccessUrls(requests: AigcAssetAccessUrlReq[]) {
  return api.post<AigcAssetAccessUrlResp[]>("/aigc/asset/access-urls", requests);
}

export function updateMyAsset(data: AigcAssetUpdateReq) {
  return api.put<boolean>("/aigc/asset/update", data);
}

export function updateMyAssetVisibility(data: AigcAssetVisibilityReq) {
  return api.put<boolean>("/aigc/asset/visibility", data);
}

export function deleteMyAsset(id: number | string) {
  return api.delete<boolean>(`/aigc/asset/delete${toQuery({ id })}`);
}

export function downloadMyAsset(data: AigcAssetDownloadReq) {
  return api.post<string>("/aigc/asset/download", data);
}

export function captureVideoFrameAsset(data: {
  assetId: number;
  capturedAt: "current" | "first" | "last";
  timeSec?: number | null;
  title?: string;
}) {
  return api.post<number>("/aigc/asset/capture-video-frame", data);
}

export interface UploadAssetOptions {
  title?: string;
  width?: number | null;
  height?: number | null;
  duration?: number | null;
  metadata?: string | Record<string, unknown> | null;
}

function normalizeUploadOptions(optionsOrTitle?: string | UploadAssetOptions): UploadAssetOptions {
  return typeof optionsOrTitle === "string" ? { title: optionsOrTitle } : optionsOrTitle ?? {};
}

function finiteInteger(value: unknown) {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) && numberValue > 0 ? Math.round(numberValue) : undefined;
}

function finiteNumber(value: unknown) {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) && numberValue > 0 ? numberValue : undefined;
}

function normalizeMetadata(metadata: UploadAssetOptions["metadata"]) {
  if (!metadata) return undefined;
  return typeof metadata === "string" ? metadata : JSON.stringify(metadata);
}

async function putFileToStorage(uploadUrl: string, file: File) {
  try {
    const response = await fetch(uploadUrl, {
      method: "PUT",
      body: new Blob([file]),
    });
    if (!response.ok) {
      const body = await response.text().catch(() => "");
      throw new Error(`Storage upload failed: HTTP ${response.status}${body ? ` ${body.slice(0, 160)}` : ""}`);
    }
  } catch (error) {
    if (error instanceof Error && error.message.startsWith("Storage upload failed:")) {
      throw error;
    }
    throw new Error("Storage upload failed before reaching complete step. Check object storage CORS and whether the signed upload URL is reachable from the browser.");
  }
}

export async function uploadAssetAndGetInfo(
  file: File,
  assetType: AigcAssetType,
  optionsOrTitle?: string | UploadAssetOptions
) {
  const options = normalizeUploadOptions(optionsOrTitle);
  const presign = await api.post<AigcAssetDirectUploadPrepareResp>("/aigc/asset/upload", {
    assetType,
    title: options.title,
    fileName: file.name,
    mimeType: file.type || "application/octet-stream",
    fileSize: file.size,
  });
  if (!presign.uploadToken || !presign.uploadUrl) {
    throw new Error("Asset direct upload was not signed correctly");
  }

  await putFileToStorage(presign.uploadUrl, file);

  const asset = await api.post<AigcAsset>("/aigc/asset/upload/complete", {
    uploadToken: presign.uploadToken,
    width: finiteInteger(options.width),
    height: finiteInteger(options.height),
    duration: finiteNumber(options.duration),
    metadata: normalizeMetadata(options.metadata),
  });
  if (!asset?.id) {
    throw new Error("Asset creation failed after storage upload");
  }
  return asset;
}

export async function uploadAsset(
  file: File,
  assetType: AigcAssetType,
  optionsOrTitle?: string | UploadAssetOptions
) {
  const asset = await uploadAssetAndGetInfo(file, assetType, optionsOrTitle);
  return asset.id;
}
