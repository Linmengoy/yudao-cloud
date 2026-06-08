import { api } from "@/lib/api-client";
import { API_BASE_URL, API_TENANT_ID, API_TERMINAL, getAccessToken } from "@/lib/api-client";
import type {
  AigcAsset,
  AigcAssetAccessUrlReq,
  AigcAssetAccessUrlResp,
  AigcAssetCategoryCounts,
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

export async function uploadAsset(file: File, assetType: AigcAssetType, title?: string) {
  const form = new FormData();
  form.append("file", file);
  form.append("assetType", assetType);
  if (title) form.append("title", title);

  const headers: Record<string, string> = {
    Accept: "*/*",
    "tenant-id": API_TENANT_ID,
    terminal: API_TERMINAL,
  };
  const token = getAccessToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  const response = await fetch(`${API_BASE_URL}/aigc/asset/upload`, {
    method: "POST",
    headers,
    body: form,
  });
  const body = await response.json() as { code: number; data: number; msg: string };
  if (body.code !== 0) {
    throw new Error(body.msg || `API error ${body.code}`);
  }
  return body.data;
}
