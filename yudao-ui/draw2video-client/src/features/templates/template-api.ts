import { api } from "@/lib/api-client";
import type { PageResult, PromptTemplate, PromptTemplatePageParams } from "./template-types";

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

export function getPromptTemplatePage(params: PromptTemplatePageParams) {
  return api.get<PageResult<PromptTemplate>>(`/aigc/asset/prompt-template/page${toQuery(params)}`);
}

export function getPromptTemplate(id: number | string) {
  return api.get<PromptTemplate>(`/aigc/asset/prompt-template/get${toQuery({ id })}`);
}

export function getPromptTemplateCategories() {
  return api.get<string[]>("/aigc/asset/prompt-template/categories");
}

export function copyPromptTemplate(id: number | string) {
  return api.post<boolean>(`/aigc/asset/prompt-template/copy${toQuery({ id })}`);
}

export function markPromptTemplateUsed(id: number | string) {
  return api.post<boolean>(`/aigc/asset/prompt-template/use${toQuery({ id })}`);
}
