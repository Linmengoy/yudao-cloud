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
  return api.get<PageResult<PromptTemplate>>(`/aigc/prompt-template/page${toQuery(params)}`);
}

export function getPromptTemplate(id: number | string) {
  return api.get<PromptTemplate>(`/aigc/prompt-template/get${toQuery({ id })}`);
}

export function getPromptTemplateCategories() {
  return api.get<string[]>("/aigc/prompt-template/categories");
}

export function copyPromptTemplate(id: number | string) {
  return api.post<boolean>(`/aigc/prompt-template/copy${toQuery({ id })}`);
}

export function markPromptTemplateUsed(id: number | string) {
  return api.post<boolean>(`/aigc/prompt-template/use${toQuery({ id })}`);
}
