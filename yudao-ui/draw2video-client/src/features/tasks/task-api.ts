import { api } from "@/lib/api-client";
import type { AigcTask, AigcTaskPageParams, PageResult } from "./task-types";

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

export function getAigcTaskPage(params: AigcTaskPageParams) {
  return api.get<PageResult<AigcTask>>(`/aigc/task/page${toQuery(params)}`);
}

export function getAigcTask(id: number | string) {
  return api.get<AigcTask>(`/aigc/task/get${toQuery({ id })}`);
}

export function getAigcTaskProgress(id: number | string) {
  return api.get<AigcTask>(`/aigc/task/progress${toQuery({ id })}`);
}

export function cancelAigcTask(id: number | string) {
  return api.put<boolean>(`/aigc/task/cancel${toQuery({ id })}`);
}
