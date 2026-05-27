import { api } from "@/lib/api-client";
import type {
  AigcAsset,
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

export function getMyAsset(id: number | string) {
  return api.get<AigcAsset>(`/aigc/asset/my-get${toQuery({ id })}`);
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
