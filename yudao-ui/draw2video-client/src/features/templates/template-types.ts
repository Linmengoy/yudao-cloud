import type { PageResult } from "@/features/assets/asset-types";

export type { PageResult };

export interface PromptTemplate {
  id: number;
  templateNo?: string;
  title: string;
  description?: string;
  prompt: string;
  promptPreview?: string;
  category?: string;
  modelCode?: string;
  modelName?: string;
  styles?: string;
  scenes?: string;
  tags?: string;
  imageUrl?: string;
  imageUrlExpireTime?: string;
  publicAccess?: boolean;
  width?: number;
  height?: number;
  mimeType?: string;
  fileSize?: number;
  sourceLabel?: string;
  sourceUrl?: string;
  githubUrl?: string;
  featured?: boolean;
  viewCount?: number;
  copyCount?: number;
  useCount?: number;
  createTime?: string;
}

export interface PromptTemplateModel {
  modelCode: string;
  modelName?: string;
}

export interface PromptTemplatePageParams {
  pageNo: number;
  pageSize: number;
  keyword?: string;
  category?: string;
  modelCode?: string;
  style?: string;
  scene?: string;
  featured?: boolean;
}
