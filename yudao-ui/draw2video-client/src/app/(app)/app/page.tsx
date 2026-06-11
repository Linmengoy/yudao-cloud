"use client";

import { useCallback, useEffect, useMemo, useRef, useState, type ChangeEvent, type DragEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Paperclip, Plus, Send, SlidersHorizontal, X } from "lucide-react";
import { canvasApi } from "@/features/canvas/canvas-api";
import type { CanvasProject } from "@/features/canvas/types";
import { getMyAssetPage, uploadAssetAndGetInfo } from "@/features/assets/asset-api";
import { getAssetPreviewUrl } from "@/features/assets/asset-dictionaries";
import type { AigcAsset } from "@/features/assets/asset-types";
import { getImageFilesFromPasteEvent } from "@/features/canvas/clipboard";
import { DynamicParamForm } from "@/features/generation/DynamicParamForm";
import {
  getAigcModelList,
  getAigcModelParamList,
  type AigcModel,
  type AigcModelParamTemplate,
} from "@/features/generation/model-api";
import { listProjects, type ProjectMeta } from "@/features/projects/project-store";
import { useAuth } from "@/features/auth/auth-store";
import { mergeStableList, readPageCache, writePageCache } from "@/lib/page-cache";

const MODEL_TYPE_LABELS: Record<number, string> = {
  1: "文本",
  2: "图片",
  3: "视频",
  4: "音频",
  5: "审核",
};

const MODEL_TYPE_ORDER = [2, 3, 4, 1, 5];
const REFERENCE_IMAGE_CACHE_KEY = "copse:workspace:reference-images";
const REFERENCE_IMAGE_DRAG_DATA_TYPE = "application/x-copse-reference-image-index";

type QuickGenerationMode = "TEXT_TO_IMAGE" | "IMAGE_TO_IMAGE" | "TEXT_TO_VIDEO" | "IMAGE_TO_VIDEO";

const QUICK_GENERATION_MODE_LABELS: Record<QuickGenerationMode, string> = {
  TEXT_TO_IMAGE: "文生图",
  IMAGE_TO_IMAGE: "图生图",
  TEXT_TO_VIDEO: "文生视频",
  IMAGE_TO_VIDEO: "图生视频",
};

function formatDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

type ProjectListItem = {
  id: string;
  name: string;
  lastOpenedAt: string;
  coverUrl?: string | null;
  role?: string | null;
  source: "server" | "local";
};

type WorkspaceProjectsCache = {
  projects: ProjectListItem[];
};

type WorkspaceModelsCache = {
  models: AigcModel[];
};

type ReferenceImage = {
  assetId: number;
  previewUrl: string | null;
  fileName: string;
  mimeType: string;
};

function imageExtensionFromMimeType(mimeType: string) {
  if (mimeType === "image/jpeg") return "jpg";
  if (mimeType === "image/webp") return "webp";
  if (mimeType === "image/gif") return "gif";
  return "png";
}

function normalizeReferenceFile(file: File) {
  const mimeType = file.type || "image/png";
  const fileName = file.name?.trim() || `clipboard-${Date.now()}.${imageExtensionFromMimeType(mimeType)}`;
  if (file.name === fileName && file.type === mimeType) return file;
  return new File([file], fileName, {
    type: mimeType,
    lastModified: file.lastModified || Date.now(),
  });
}

function readImageDimensions(file: File): Promise<{ width?: number; height?: number }> {
  return new Promise((resolve) => {
    const objectUrl = URL.createObjectURL(file);
    const image = new Image();
    const cleanup = () => URL.revokeObjectURL(objectUrl);
    image.onload = () => {
      const width = image.naturalWidth || undefined;
      const height = image.naturalHeight || undefined;
      cleanup();
      resolve({ width, height });
    };
    image.onerror = () => {
      cleanup();
      resolve({});
    };
    image.src = objectUrl;
  });
}

function normalizeTemplateOption(option: unknown) {
  let value = String(option ?? "").trim();
  for (let i = 0; i < 3; i += 1) {
    try {
      const parsed = JSON.parse(value);
      if (typeof parsed !== "string") break;
      value = parsed.trim();
    } catch {
      break;
    }
  }
  return value.replace(/\\/g, "").replace(/^"+|"+$/g, "").trim();
}

function hasUsableParamValue(value: unknown) {
  if (value === undefined || value === null) return false;
  if (typeof value === "string") return value.trim() !== "";
  if (Array.isArray(value)) return value.length > 0;
  return true;
}

function getRawTemplateDefault(template: AigcModelParamTemplate) {
  if (hasUsableParamValue(template.defaultValue)) return template.defaultValue;
  const option = (template.options ?? []).map(normalizeTemplateOption).find(Boolean);
  if (option) return option;
  return undefined;
}

function coerceTemplateValue(template: AigcModelParamTemplate, rawValue: unknown) {
  if (!hasUsableParamValue(rawValue)) return undefined;

  if (template.paramType === "NUMBER") {
    const value = Number(normalizeTemplateOption(rawValue));
    return Number.isFinite(value) ? value : undefined;
  }

  if (template.paramType === "BOOLEAN") {
    if (typeof rawValue === "boolean") return rawValue;
    const value = normalizeTemplateOption(rawValue).toLowerCase();
    return value === "true" || value === "1" || value === "yes" || value === "on";
  }

  if (template.paramType === "MULTI_SELECT") {
    if (Array.isArray(rawValue)) return rawValue.map(String).filter(Boolean);
    const value = normalizeTemplateOption(rawValue);
    try {
      const parsed = JSON.parse(value);
      if (Array.isArray(parsed)) return parsed.map(String).filter(Boolean);
    } catch {
      // Fall back to comma-separated values.
    }
    return value.split(",").map((item) => item.trim()).filter(Boolean);
  }

  if (template.paramType === "JSON") {
    if (typeof rawValue === "object") return rawValue;
    const value = normalizeTemplateOption(rawValue);
    try {
      return JSON.parse(value);
    } catch {
      return value;
    }
  }

  return normalizeTemplateOption(rawValue);
}

function buildDefaultModelParams(templates: AigcModelParamTemplate[]) {
  const params: Record<string, unknown> = {};
  for (const template of templates) {
    const value = coerceTemplateValue(template, getRawTemplateDefault(template));
    if (hasUsableParamValue(value)) params[template.paramKey] = value;
  }
  return params;
}

function buildEffectiveModelParams(
  templates: AigcModelParamTemplate[],
  currentParams: Record<string, unknown>
) {
  const defaults = buildDefaultModelParams(templates);
  const keys = new Set(templates.map((template) => template.paramKey));
  const merged = { ...defaults };
  for (const [key, value] of Object.entries(currentParams)) {
    if (keys.has(key) && hasUsableParamValue(value)) merged[key] = value;
  }
  return merged;
}

function summarizeModelParams(
  templates: AigcModelParamTemplate[],
  values: Record<string, unknown>,
  loading: boolean,
  error: string | null
) {
  if (loading) return "参数加载中";
  if (error) return "参数异常";
  if (templates.length === 0) return "默认参数";
  const summary = templates
    .map((template) => values[template.paramKey] ?? template.defaultValue ?? "")
    .map((value) => Array.isArray(value) ? value.join(",") : normalizeTemplateOption(value))
    .filter(Boolean)
    .slice(0, 3);
  return summary.length > 0 ? summary.join(" · ") : "模型参数";
}

function getMissingRequiredParamNames(templates: AigcModelParamTemplate[], values: Record<string, unknown>) {
  return templates
    .filter((template) => template.requiredStatus && !hasUsableParamValue(values[template.paramKey]))
    .map((template) => template.paramName || template.paramKey);
}

async function loadActiveModelParamTemplates(modelId: number, capability: string) {
  const templates = await getAigcModelParamList(modelId, capability);
  return templates.filter((item) => item.status === 0).sort((a, b) => a.sort - b.sort);
}

function buildReferenceInputParams(referenceImages: ReferenceImage[]) {
  if (referenceImages.length === 0) return {};
  const referenceAssetIds = referenceImages.map((image) => image.assetId);
  const referenceImageIds = referenceAssetIds.map(String);
  const referenceImageUrls = referenceImages.map((image) => image.previewUrl).filter(Boolean) as string[];

  return {
    referenceAssetIds,
    referenceImageIds,
    referenceImages: referenceImageUrls,
    referencePreviewUrls: referenceImageUrls,
    inputImageIds: referenceImageIds,
    inputImageUrls: referenceImageUrls,
    inputImages: referenceImages.map((image) => ({
      imageId: String(image.assetId),
      fileName: image.fileName,
      dataUrl: image.previewUrl ?? "",
      mimeType: image.mimeType,
    })),
  };
}

function assetToReferenceImage(asset: AigcAsset): ReferenceImage | null {
  const previewUrl = getAssetPreviewUrl(asset) || null;
  if (!previewUrl) return null;
  return {
    assetId: asset.id,
    previewUrl,
    fileName: asset.title || asset.assetNo || `asset-${asset.id}`,
    mimeType: asset.mimeType || "image/png",
  };
}

function referenceImageCacheKey(ownerKey?: string | number | null) {
  return ownerKey == null ? REFERENCE_IMAGE_CACHE_KEY : `${REFERENCE_IMAGE_CACHE_KEY}:${ownerKey}`;
}

function readCachedReferenceImages(ownerKey?: string | number | null) {
  try {
    const raw = window.localStorage.getItem(referenceImageCacheKey(ownerKey));
    if (!raw) return [];
    const parsed = JSON.parse(raw) as ReferenceImage[];
    return Array.isArray(parsed)
      ? parsed.filter((item) => Number.isFinite(item.assetId) && (item.previewUrl || item.fileName))
      : [];
  } catch {
    return [];
  }
}

function writeCachedReferenceImages(images: ReferenceImage[], ownerKey?: string | number | null) {
  try {
    if (images.length === 0) {
      window.localStorage.removeItem(referenceImageCacheKey(ownerKey));
      return;
    }
    window.localStorage.setItem(referenceImageCacheKey(ownerKey), JSON.stringify(images));
  } catch {
    // Cache is best-effort; generation should not depend on localStorage.
  }
}

function workspaceProjectsCacheKey(ownerKey: string | number | null | undefined) {
  return `workspace:projects:${ownerKey ?? "current"}`;
}

function workspaceModelsCacheKey(imageCapability: QuickGenerationMode, videoCapability: QuickGenerationMode) {
  return `workspace:models:${imageCapability}:${videoCapability}`;
}

function getProjectKey(project: ProjectListItem) {
  return `${project.source}:${project.id}`;
}

function getModelKey(model: AigcModel) {
  return model.id;
}

function pickDefaultModelId(models: AigcModel[]) {
  return models.find((item) => item.defaultModel && (item.type === 2 || item.type === 3))?.id
    ?? models.find((item) => item.type === 2 || item.type === 3)?.id
    ?? models[0]?.id
    ?? null;
}

function reorderItems<T>(items: T[], fromIndex: number, toIndex: number) {
  if (fromIndex === toIndex) return items;
  if (fromIndex < 0 || toIndex < 0 || fromIndex >= items.length || toIndex >= items.length) return items;
  const nextItems = [...items];
  const [moved] = nextItems.splice(fromIndex, 1);
  nextItems.splice(toIndex, 0, moved);
  return nextItems;
}

async function uploadReferenceFile(sourceFile: File): Promise<ReferenceImage> {
  const file = normalizeReferenceFile(sourceFile);
  const dimensions = await readImageDimensions(file);
  const asset = await uploadAssetAndGetInfo(file, "IMAGE", {
    title: file.name,
    width: dimensions.width,
    height: dimensions.height,
    metadata: {
      source: "workspace-reference-image",
    },
  });
  return {
    assetId: asset.id,
    previewUrl: getAssetPreviewUrl(asset) || null,
    fileName: file.name,
    mimeType: file.type || "image/png",
  };
}

function toProjectListItem(project: CanvasProject): ProjectListItem {
  return {
    id: String(project.id),
    name: project.name,
    lastOpenedAt: project.updateTime ?? project.createTime ?? "",
    coverUrl: project.coverUrl,
    role: project.role,
    source: "server",
  };
}

function localProjectToListItem(project: ProjectMeta): ProjectListItem {
  return {
    id: project.id,
    name: project.name,
    lastOpenedAt: project.lastOpenedAt,
    coverUrl: project.thumbnailUrl,
    source: "local",
  };
}

function projectRoleLabel(project: ProjectListItem) {
  if (project.source === "local" || project.role === "owner") return "可管理";
  if (project.role === "viewer") return "可浏览";
  return "可编辑";
}

function ProjectCover({ project }: { project: ProjectListItem }) {
  if (project.coverUrl) {
    return (
      <div className="aspect-square bg-muted">
        <img
          src={project.coverUrl}
          alt={`${project.name} 封面`}
          draggable={false}
          className="size-full object-cover transition-transform duration-300 group-hover:scale-105"
        />
      </div>
    );
  }
  return (
    <div className="flex aspect-square items-center justify-center bg-muted px-3 text-center text-charcoal">
      <span className="line-clamp-3 text-sm font-semibold leading-snug">{project.name}</span>
    </div>
  );
}

export default function WorkspacePage() {
  const router = useRouter();
  const { user } = useAuth();
  const initialProjectsCache = useMemo(
    () => readPageCache<WorkspaceProjectsCache>(workspaceProjectsCacheKey(user?.id)),
    [user?.id]
  );
  const initialReferenceImages = useMemo(() => {
    if (typeof window === "undefined") return [];
    return readCachedReferenceImages(user?.id);
  }, [user?.id]);
  const initialImageGenerationCapability: QuickGenerationMode = initialReferenceImages.length > 0 ? "IMAGE_TO_IMAGE" : "TEXT_TO_IMAGE";
  const initialVideoGenerationCapability: QuickGenerationMode = initialReferenceImages.length > 0 ? "IMAGE_TO_VIDEO" : "TEXT_TO_VIDEO";
  const initialModelsCache = useMemo(
    () => readPageCache<WorkspaceModelsCache>(workspaceModelsCacheKey(initialImageGenerationCapability, initialVideoGenerationCapability)),
    [initialImageGenerationCapability, initialVideoGenerationCapability]
  );
  const [prompt, setPrompt] = useState("");
  const [models, setModels] = useState<AigcModel[]>(() => initialModelsCache?.models ?? []);
  const [selectedModelId, setSelectedModelId] = useState<number | null>(() => pickDefaultModelId(initialModelsCache?.models ?? []));
  const [modelsLoading, setModelsLoading] = useState(() => !initialModelsCache);
  const [modelsError, setModelsError] = useState<string | null>(null);
  const [paramTemplates, setParamTemplates] = useState<AigcModelParamTemplate[]>([]);
  const [modelParams, setModelParams] = useState<Record<string, unknown>>({});
  const [paramsLoading, setParamsLoading] = useState(false);
  const [paramsError, setParamsError] = useState<string | null>(null);
  const [loadedParamKey, setLoadedParamKey] = useState<string | null>(null);
  const [paramsOpen, setParamsOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [referenceImages, setReferenceImages] = useState<ReferenceImage[]>(() => initialReferenceImages);
  const [referenceUploading, setReferenceUploading] = useState(false);
  const [draggingReferenceIndex, setDraggingReferenceIndex] = useState<number | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [projects, setProjects] = useState<ProjectListItem[]>(() => initialProjectsCache?.projects ?? []);
  const [assetPickerOpen, setAssetPickerOpen] = useState(false);
  const [assetPickerAssets, setAssetPickerAssets] = useState<AigcAsset[]>([]);
  const [assetPickerLoading, setAssetPickerLoading] = useState(false);
  const [assetPickerError, setAssetPickerError] = useState<string | null>(null);
  const referenceInputRef = useRef<HTMLInputElement | null>(null);
  const paramsPanelRef = useRef<HTMLDivElement | null>(null);
  const paramsButtonRef = useRef<HTMLButtonElement | null>(null);
  const recentProjects = useMemo(() => projects.slice(0, 7), [projects]);
  const quickModels = useMemo(() => models.filter((model) => model.type === 2 || model.type === 3), [models]);
  const modelGroups = useMemo(() => {
    const groups = new Map<number, AigcModel[]>();
    for (const model of quickModels) {
      groups.set(model.type, [...(groups.get(model.type) ?? []), model]);
    }
    return [...groups.entries()].sort(([a], [b]) => {
      const aIndex = MODEL_TYPE_ORDER.indexOf(a);
      const bIndex = MODEL_TYPE_ORDER.indexOf(b);
      if (aIndex === -1 && bIndex === -1) return a - b;
      if (aIndex === -1) return 1;
      if (bIndex === -1) return -1;
      return aIndex - bIndex;
    });
  }, [quickModels]);
  const selectedModel = useMemo(
    () => quickModels.find((model) => model.id === selectedModelId) ?? null,
    [quickModels, selectedModelId]
  );
  const selectedModelParamId = selectedModel?.id ?? null;
  const referenceImage = referenceImages[0] ?? null;
  const hasReferenceImages = referenceImages.length > 0;
  const imageGenerationCapability: QuickGenerationMode = hasReferenceImages ? "IMAGE_TO_IMAGE" : "TEXT_TO_IMAGE";
  const videoGenerationCapability: QuickGenerationMode = hasReferenceImages ? "IMAGE_TO_VIDEO" : "TEXT_TO_VIDEO";
  const quickGenerationMode = useMemo<QuickGenerationMode | null>(() => {
    if (!selectedModel || (selectedModel.type !== 2 && selectedModel.type !== 3)) return null;
    return selectedModel.type === 3 ? videoGenerationCapability : imageGenerationCapability;
  }, [imageGenerationCapability, selectedModel, videoGenerationCapability]);
  const isQuickGenerationModel = Boolean(quickGenerationMode);
  const effectiveModelParams = useMemo(
    () => selectedModel
      ? buildEffectiveModelParams(paramTemplates, modelParams)
      : {},
    [modelParams, paramTemplates, selectedModel]
  );
  const paramSummary = useMemo(
    () => summarizeModelParams(paramTemplates, effectiveModelParams, paramsLoading, paramsError),
    [effectiveModelParams, paramTemplates, paramsError, paramsLoading]
  );
  const paramsReady = Boolean(selectedModel && quickGenerationMode && loadedParamKey === `${selectedModel.id}:${quickGenerationMode}`);
  const submitBlockReason = useMemo(() => {
    if (!prompt.trim()) return null;
    if (referenceUploading) return "参考图还在上传，请稍后再提交";
    if (modelsLoading) return "模型列表还在加载，请稍后再提交";
    if (!selectedModel || !quickGenerationMode) return "请选择支持当前输入的图片或视频模型";
    if (paramsLoading || !paramsReady) return "模型参数还在加载，请稍后再提交";
    if (paramsError) return paramsError;
    return null;
  }, [modelsLoading, paramsError, paramsLoading, paramsReady, prompt, quickGenerationMode, referenceUploading, selectedModel]);
  const canSubmit = Boolean(prompt.trim())
    && !submitting
    && isQuickGenerationModel
    && !submitBlockReason;
  const submitStatusMessage = submitError ?? submitBlockReason;

  useEffect(() => {
    writeCachedReferenceImages(referenceImages, user?.id);
  }, [referenceImages, user?.id]);

  useEffect(() => {
    if (!user?.id) return;
    const timer = window.setTimeout(() => setReferenceImages(readCachedReferenceImages(user.id)), 0);
    return () => window.clearTimeout(timer);
  }, [user?.id]);

  const refreshProjects = useCallback(async () => {
    const cacheKey = workspaceProjectsCacheKey(user?.id);
    const cached = readPageCache<WorkspaceProjectsCache>(cacheKey);
    if (cached) {
      setProjects((items) => mergeStableList(items, cached.projects, getProjectKey));
    }
    try {
      const page = await canvasApi.listProjects({ pageNo: 1, pageSize: 12 });
      const nextProjects = page.list.map(toProjectListItem);
      writePageCache<WorkspaceProjectsCache>(cacheKey, { projects: nextProjects });
      setProjects((items) => mergeStableList(items, nextProjects, getProjectKey));
    } catch {
      const nextProjects = listProjects(user?.id).map(localProjectToListItem);
      writePageCache<WorkspaceProjectsCache>(cacheKey, { projects: nextProjects });
      setProjects((items) => mergeStableList(items, nextProjects, getProjectKey));
    }
  }, [user?.id]);

  useEffect(() => {
    const timer = window.setTimeout(refreshProjects, 0);
    return () => window.clearTimeout(timer);
  }, [refreshProjects]);

  useEffect(() => {
    let ignore = false;
    const timer = window.setTimeout(() => {
      const cacheKey = workspaceModelsCacheKey(imageGenerationCapability, videoGenerationCapability);
      const cached = readPageCache<WorkspaceModelsCache>(cacheKey);
      if (cached) {
        setModels((items) => mergeStableList(items, cached.models, getModelKey));
        setSelectedModelId((current) => {
          if (cached.models.some((item) => item.id === current)) return current;
          return pickDefaultModelId(cached.models);
        });
        setModelsLoading(false);
      } else {
        setModelsLoading(true);
      }
      setModelsError(null);
      Promise.allSettled([
        getAigcModelList(2, imageGenerationCapability),
        getAigcModelList(3, videoGenerationCapability),
      ])
        .then((results) => {
          if (ignore) return;
          const data = results.flatMap((result) => result.status === "fulfilled" ? result.value : []);
          writePageCache<WorkspaceModelsCache>(cacheKey, { models: data });
          setModels((items) => mergeStableList(items, data, getModelKey));
          if (data.length === 0) {
            const errorResult = results.find((result) => result.status === "rejected");
            const reason = errorResult?.status === "rejected" ? errorResult.reason : null;
            if (reason) setModelsError(reason instanceof Error ? reason.message : "模型列表加载失败");
            return;
          }
          setSelectedModelId((current) => {
            if (data.some((item) => item.id === current)) return current;
            return pickDefaultModelId(data);
          });
        })
        .finally(() => {
          if (!ignore) setModelsLoading(false);
        });
    }, 0);
    return () => {
      ignore = true;
      window.clearTimeout(timer);
    };
  }, [imageGenerationCapability, videoGenerationCapability]);

  useEffect(() => {
    let ignore = false;
    const timer = window.setTimeout(() => {
      if (selectedModelParamId == null || !quickGenerationMode) {
        setParamTemplates([]);
        setModelParams({});
        setParamsLoading(false);
        setParamsError(null);
        setLoadedParamKey(null);
        setParamsOpen(false);
        return;
      }

      const paramKey = `${selectedModelParamId}:${quickGenerationMode}`;
      setParamsLoading(true);
      setParamsError(null);
      setLoadedParamKey(null);
      loadActiveModelParamTemplates(selectedModelParamId, quickGenerationMode)
        .then((templates) => {
          if (ignore) return;
          setParamTemplates(templates);
          setModelParams((current) => buildEffectiveModelParams(templates, current));
          setLoadedParamKey(paramKey);
        })
        .catch((error) => {
          if (ignore) return;
          setParamTemplates([]);
          setParamsError(error instanceof Error ? error.message : "参数模板加载失败");
          setLoadedParamKey(null);
        })
        .finally(() => {
          if (!ignore) setParamsLoading(false);
        });
    }, 0);

    return () => {
      ignore = true;
      window.clearTimeout(timer);
    };
  }, [quickGenerationMode, selectedModelParamId]);

  useEffect(() => {
    if (!paramsOpen) return;
    function handlePointerDown(event: MouseEvent) {
      const target = event.target;
      if (!(target instanceof Node)) return;
      if (paramsPanelRef.current?.contains(target) || paramsButtonRef.current?.contains(target)) return;
      setParamsOpen(false);
    }
    window.addEventListener("mousedown", handlePointerDown);
    return () => window.removeEventListener("mousedown", handlePointerDown);
  }, [paramsOpen]);

  const uploadReferenceImages = useCallback(async (sourceFiles: File[]) => {
    if (referenceUploading || submitting) return;
    const files = sourceFiles.filter(Boolean);
    if (files.length === 0) return;
    setReferenceUploading(true);
    setSubmitError(null);
    try {
      const uploaded = await Promise.all(files.map(uploadReferenceFile));
      setReferenceImages((current) => [...current, ...uploaded]);
    } catch (error) {
      setSubmitError(error instanceof Error ? error.message : "参考图上传失败");
    } finally {
      setReferenceUploading(false);
    }
  }, [referenceUploading, submitting]);

  const loadAssetPickerImages = useCallback(async () => {
    setAssetPickerLoading(true);
    setAssetPickerError(null);
    try {
      const data = await getMyAssetPage({
        pageNo: 1,
        pageSize: 60,
        assetType: "IMAGE",
      });
      setAssetPickerAssets(data.list ?? []);
    } catch (error) {
      setAssetPickerError(error instanceof Error ? error.message : "资产加载失败");
    } finally {
      setAssetPickerLoading(false);
    }
  }, []);

  const openAssetPicker = useCallback(() => {
    setAssetPickerOpen(true);
    void loadAssetPickerImages();
  }, [loadAssetPickerImages]);

  const selectAssetReference = useCallback((asset: AigcAsset) => {
    const reference = assetToReferenceImage(asset);
    if (!reference) {
      setAssetPickerError("这张资产暂时没有可用预览图");
      return;
    }
    setReferenceImages((current) => current.some((item) => item.assetId === reference.assetId)
      ? current
      : [...current, reference]);
  }, []);

  const handleReferenceDragStart = useCallback((event: DragEvent<HTMLDivElement>, index: number) => {
    if (submitting || referenceUploading) return;
    setDraggingReferenceIndex(index);
    event.dataTransfer.effectAllowed = "move";
    event.dataTransfer.setData(REFERENCE_IMAGE_DRAG_DATA_TYPE, String(index));
  }, [referenceUploading, submitting]);

  const handleReferenceDragOver = useCallback((event: DragEvent<HTMLDivElement>) => {
    if (submitting || referenceUploading) return;
    event.preventDefault();
    event.dataTransfer.dropEffect = "move";
  }, [referenceUploading, submitting]);

  const handleReferenceDrop = useCallback((event: DragEvent<HTMLDivElement>, toIndex: number) => {
    event.preventDefault();
    if (submitting || referenceUploading) return;
    const rawIndex = event.dataTransfer.getData(REFERENCE_IMAGE_DRAG_DATA_TYPE);
    const fromIndex = Number(rawIndex || draggingReferenceIndex);
    if (!Number.isInteger(fromIndex)) return;
    setReferenceImages((current) => reorderItems(current, fromIndex, toIndex));
    setDraggingReferenceIndex(null);
  }, [draggingReferenceIndex, referenceUploading, submitting]);

  const handleReferenceDragEnd = useCallback(() => {
    setDraggingReferenceIndex(null);
  }, []);

  async function handleReferenceFileChange(event: ChangeEvent<HTMLInputElement>) {
    const files = Array.from(event.target.files ?? []);
    event.target.value = "";
    await uploadReferenceImages(files);
  }

  useEffect(() => {
    const handlePaste = (event: ClipboardEvent) => {
      const files = getImageFilesFromPasteEvent(event);
      if (files.length === 0) return;
      event.preventDefault();
      void uploadReferenceImages(files);
    };

    window.addEventListener("paste", handlePaste);
    return () => window.removeEventListener("paste", handlePaste);
  }, [uploadReferenceImages]);

  async function handleSubmit() {
    if (!prompt.trim() || submitting) return;
    if (referenceUploading) {
      setSubmitError("参考图还在上传，请稍后再提交");
      return;
    }
    if (!selectedModel || !quickGenerationMode) {
      setSubmitError("请选择图片或视频模型后再提交");
      return;
    }
    if (paramsLoading || loadedParamKey !== `${selectedModel.id}:${quickGenerationMode}`) {
      setSubmitError("模型参数还在加载，请稍后再提交");
      return;
    }
    if (paramsError) {
      setSubmitError(paramsError);
      setParamsOpen(true);
      return;
    }

    const modelParamsForSubmit = buildEffectiveModelParams(paramTemplates, modelParams);
    const missingRequiredParams = getMissingRequiredParamNames(paramTemplates, modelParamsForSubmit);
    if (missingRequiredParams.length > 0) {
      setSubmitError(`请先配置必填参数：${missingRequiredParams.join("、")}`);
      setParamsOpen(true);
      return;
    }

    setSubmitting(true);
    setSubmitError(null);
    const promptText = prompt.trim();
    const projectName = promptText.slice(0, 30) || "未命名项目";
    try {
      const isVideo = selectedModel.type === 3;
      const firstReferenceImage = referenceImages[0] ?? null;
      const referencePreviewUrls = referenceImages.map((image) => image.previewUrl).filter(Boolean) as string[];
      const result = await canvasApi.quickGenerateProject({
        name: projectName,
        prompt: promptText,
        nodeType: isVideo ? "video" : "image",
        generateType: isVideo ? "VIDEO" : "IMAGE",
        generateMode: quickGenerationMode,
        modelId: selectedModel.id,
        modelName: selectedModel.name,
        providerModel: selectedModel.model,
        inputParams: JSON.stringify({
          ...modelParamsForSubmit,
          providerModel: selectedModel.model,
          ...buildReferenceInputParams(referenceImages),
        }),
        referenceAssetId: firstReferenceImage?.assetId ?? null,
        referenceAssetIds: referenceImages.map((image) => image.assetId),
        referencePreviewUrl: firstReferenceImage?.previewUrl ?? null,
        referencePreviewUrls,
      });
      router.push(`/canvas?projectId=${encodeURIComponent(String(result.projectId))}`);
      setPrompt("");
    } catch (error) {
      setSubmitError(error instanceof Error ? error.message : "任务提交失败，请稍后再试");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex flex-col items-center px-4 pt-16 pb-20">
      <div className="flex items-center gap-2 text-lg font-semibold text-charcoal">
        <div className="flex size-8 items-center justify-center rounded-full bg-charcoal text-sm text-off-white">
          C
        </div>
        Copse 让创作更简单
      </div>
      <p className="mt-2 text-sm text-muted-gray">
        懂你的创意代理，帮你搞定一切
      </p>

      <div className="mt-8 w-full max-w-[840px]">
        <div className="rounded-[24px] border border-border-warm bg-background p-4 shadow-[0_1px_3px_rgba(0,0,0,0.04)]">
          <textarea
            value={prompt}
            onChange={(e) => setPrompt(e.target.value)}
            placeholder="让 Copse 创作一张..."
            rows={4}
            className="w-full resize-none bg-transparent text-sm text-charcoal placeholder:text-muted-gray focus:outline-none"
          />
          <div className="mt-2 flex items-center justify-between">
            <div className="flex flex-wrap items-center gap-1.5">
              <button
                type="button"
                onClick={openAssetPicker}
                disabled={submitting}
                className="flex size-8 items-center justify-center rounded-lg text-muted-gray hover:bg-muted hover:text-charcoal"
                title="添加素材"
              >
                <Plus className="size-4" />
              </button>
              <button
                type="button"
                onClick={() => referenceInputRef.current?.click()}
                disabled={referenceUploading || submitting}
                className="flex size-8 items-center justify-center rounded-lg text-muted-gray hover:bg-muted hover:text-charcoal"
                title={referenceImage ? `参考图：${referenceImage.fileName}` : "参考图"}
              >
                <Paperclip className="size-4" />
              </button>
              <input
                ref={referenceInputRef}
                type="file"
                accept="image/*"
                multiple
                className="hidden"
                onChange={handleReferenceFileChange}
              />
              {modelsLoading && models.length === 0 && (
                <span className="px-2 text-xs text-muted-gray">模型加载中...</span>
              )}
              {!modelsLoading && modelsError && models.length === 0 && (
                <span className="px-2 text-xs text-muted-gray">{modelsError}</span>
              )}
              {!modelsLoading && !modelsError && quickModels.length === 0 && (
                <span className="px-2 text-xs text-muted-gray">暂无可用模型</span>
              )}
              {modelGroups.map(([type, items]) => (
                <select
                  key={type}
                  value={selectedModel?.type === type ? String(selectedModel.id) : ""}
                  onChange={(event) => {
                    setSelectedModelId(Number(event.target.value));
                    setParamsOpen(false);
                    setSubmitError(null);
                  }}
                  className="h-8 rounded-lg border border-border-warm bg-background px-2 text-xs text-muted-gray outline-none transition-colors hover:border-[rgba(28,28,28,0.4)] hover:text-charcoal focus:border-[rgba(28,28,28,0.55)]"
                >
                  <option value="" disabled>{MODEL_TYPE_LABELS[type] ?? `类型 ${type}`}</option>
                  {items.map((model) => (
                    <option key={model.id} value={model.id}>{model.name}</option>
                  ))}
                </select>
              ))}
            </div>
            <div className="flex items-center gap-2">
              {selectedModel && (
                <span className="max-w-40 truncate rounded-full bg-muted px-2.5 py-1 text-xs text-muted-gray" title={selectedModel.name}>
                  {selectedModel.name}
                </span>
              )}
              {quickGenerationMode && (
                <div className="relative">
                  <button
                    ref={paramsButtonRef}
                    type="button"
                    onClick={() => setParamsOpen((open) => !open)}
                    disabled={submitting}
                    className="flex h-8 items-center gap-1.5 rounded-lg px-2 text-xs text-muted-gray hover:bg-muted hover:text-charcoal disabled:cursor-not-allowed disabled:opacity-50"
                    title="模型参数"
                  >
                    <SlidersHorizontal className="size-4" />
                    <span className="hidden max-w-32 truncate sm:inline">{paramSummary}</span>
                  </button>
                  {paramsOpen && (
                    <div
                      ref={paramsPanelRef}
                      className="absolute right-0 top-full z-40 mt-2 max-h-[70vh] w-[min(420px,calc(100vw-2rem))] overflow-y-auto rounded-2xl border border-border-warm bg-background p-4 shadow-[0_16px_48px_rgba(0,0,0,0.16)]"
                    >
                      <div className="mb-3 flex items-center justify-between gap-3 rounded-xl bg-muted px-3 py-2 text-xs text-muted-gray">
                        <span>当前能力：{QUICK_GENERATION_MODE_LABELS[quickGenerationMode]}</span>
                        {hasReferenceImages && <span>{referenceImages.length} 张参考图</span>}
                      </div>
                      {paramsError ? (
                        <div className="rounded-xl border border-border-warm bg-muted px-3 py-4 text-sm text-muted-gray">
                          {paramsError}
                        </div>
                      ) : paramsLoading ? (
                        <div className="rounded-xl border border-border-warm bg-muted px-3 py-4 text-sm text-muted-gray">
                          参数加载中...
                        </div>
                      ) : paramTemplates.length > 0 ? (
                        <DynamicParamForm
                          templates={paramTemplates}
                          values={effectiveModelParams}
                          disabled={submitting}
                          onChange={(patch) => {
                            setModelParams((current) => ({ ...current, ...patch }));
                            setSubmitError(null);
                          }}
                        />
                      ) : (
                        <div className="rounded-xl border border-border-warm bg-muted px-3 py-4 text-sm text-muted-gray">
                          当前模型没有可配置参数
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )}
              <button
                onClick={handleSubmit}
                disabled={!canSubmit}
                title={submitBlockReason ?? (quickGenerationMode ? QUICK_GENERATION_MODE_LABELS[quickGenerationMode] : "提交")}
                className="flex size-8 items-center justify-center rounded-lg bg-charcoal text-off-white shadow-[rgba(255,255,255,0.2)_0px_0.5px_0px_0px_inset,rgba(0,0,0,0.2)_0px_0px_0px_0.5px_inset,rgba(0,0,0,0.05)_0px_1px_2px_0px] active:opacity-80 disabled:cursor-not-allowed disabled:opacity-50"
              >
                <Send className="size-4" />
              </button>
            </div>
          </div>
          {referenceImage && (
            <div className="mt-3 flex items-center gap-3 rounded-lg border border-border-warm bg-muted/40 p-2">
              <div className="size-14 shrink-0 overflow-hidden rounded-md bg-background">
                {referenceImage.previewUrl ? (
                  <img
                    src={referenceImage.previewUrl}
                    alt={referenceImage.fileName || "参考图"}
                    className="size-full object-cover"
                    draggable={false}
                  />
                ) : (
                  <div className="flex size-full items-center justify-center text-xs text-muted-gray">
                    参考图
                  </div>
                )}
              </div>
              <div className="min-w-0 flex-1">
                <p className="truncate text-xs font-medium text-charcoal" title={referenceImage.fileName}>
                  {referenceImage.fileName}
                </p>
                <p className="mt-0.5 text-xs text-muted-gray">已作为参考图</p>
              </div>
              <button
                type="button"
                onClick={() => setReferenceImages((current) => current.slice(1))}
                disabled={submitting || referenceUploading}
                className="flex size-7 shrink-0 items-center justify-center rounded-md text-muted-gray hover:bg-background hover:text-charcoal disabled:opacity-50"
                aria-label="移除参考图"
                title="移除参考图"
              >
                <X className="size-4" />
              </button>
            </div>
          )}
          {referenceImages.length > 1 && (
            <div className="mt-2 flex gap-2 overflow-x-auto pb-1">
              {referenceImages.map((image, index) => (
                <div
                  key={`${image.assetId}-${index}`}
                  draggable={!submitting && !referenceUploading}
                  onDragStart={(event) => handleReferenceDragStart(event, index)}
                  onDragOver={handleReferenceDragOver}
                  onDrop={(event) => handleReferenceDrop(event, index)}
                  onDragEnd={handleReferenceDragEnd}
                  className={`group relative size-14 shrink-0 cursor-grab overflow-hidden rounded-md bg-muted active:cursor-grabbing ${
                    draggingReferenceIndex === index ? "opacity-50" : ""
                  }`}
                  title={image.fileName}
                >
                  {image.previewUrl ? (
                    <img
                      src={image.previewUrl}
                      alt={image.fileName || "参考图"}
                      className="size-full object-cover"
                      draggable={false}
                    />
                  ) : (
                    <div className="flex size-full items-center justify-center text-xs text-muted-gray">
                      参考图
                    </div>
                  )}
                  {index === 0 && (
                    <span className="absolute left-1 top-1 rounded bg-black/60 px-1.5 py-0.5 text-[10px] text-white">
                      首图
                    </span>
                  )}
                  <button
                    type="button"
                    onClick={() => setReferenceImages((current) => current.filter((_, itemIndex) => itemIndex !== index))}
                    disabled={submitting || referenceUploading}
                    className="absolute right-1 top-1 flex size-5 items-center justify-center rounded bg-black/60 text-white opacity-0 transition-opacity group-hover:opacity-100 disabled:opacity-50"
                    aria-label="移除参考图"
                    title="移除参考图"
                  >
                    <X className="size-3" />
                  </button>
                </div>
              ))}
            </div>
          )}
          {submitStatusMessage && (
            <p className={`mt-2 text-xs ${submitError ? "text-red-500" : "text-muted-gray"}`}>
              {submitStatusMessage}
            </p>
          )}
        </div>
      </div>

      <div className="mt-20 w-full max-w-[840px]">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-medium text-charcoal">最近项目</h2>
          <Link
            href="/projects"
            className="text-xs text-muted-gray hover:text-charcoal"
          >
            查看全部
          </Link>
        </div>

        <div className="mt-5 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
          {recentProjects.map((project) => (
            <Link
              key={project.id}
              href={`/canvas?projectId=${encodeURIComponent(project.id)}`}
              className="group relative block overflow-hidden rounded-xl border border-border-warm bg-background transition-colors hover:border-[rgba(28,28,28,0.4)]"
            >
              <ProjectCover project={project} />
              <div className="pointer-events-none absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/55 to-transparent px-3 pt-10 pb-3 text-center">
                <p className="text-[11px] font-medium text-white/90 drop-shadow-[0_1px_2px_rgba(0,0,0,0.7)]">{projectRoleLabel(project)} · {formatDate(project.lastOpenedAt)}</p>
              </div>
            </Link>
          ))}
        </div>
      </div>
      {assetPickerOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 px-4"
          onMouseDown={() => setAssetPickerOpen(false)}
        >
          <div
            className="w-full max-w-[720px] rounded-xl border border-border-warm bg-background p-4 shadow-[0_16px_48px_rgba(0,0,0,0.18)]"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <div className="flex items-center justify-between gap-3">
              <div>
                <h3 className="text-sm font-semibold text-charcoal">选择资产图片</h3>
                <p className="mt-0.5 text-xs text-muted-gray">可从生成图片或上传图片中选择参考图</p>
              </div>
              <button
                type="button"
                onClick={() => setAssetPickerOpen(false)}
                className="flex size-8 items-center justify-center rounded-md text-muted-gray hover:bg-muted hover:text-charcoal"
                aria-label="关闭资产选择"
                title="关闭"
              >
                <X className="size-4" />
              </button>
            </div>
            {assetPickerError && (
              <div className="mt-3 rounded-md border border-border-warm bg-muted/40 px-3 py-2 text-xs text-muted-gray">
                {assetPickerError}
              </div>
            )}
            {assetPickerLoading ? (
              <div className="mt-6 flex h-48 items-center justify-center text-sm text-muted-gray">
                资产加载中...
              </div>
            ) : assetPickerAssets.length === 0 ? (
              <div className="mt-6 flex h-48 items-center justify-center text-sm text-muted-gray">
                暂无可选图片资产
              </div>
            ) : (
              <div className="mt-4 grid max-h-[60vh] grid-cols-3 gap-2 overflow-y-auto sm:grid-cols-4 md:grid-cols-5">
                {assetPickerAssets.map((asset) => {
                  const previewUrl = getAssetPreviewUrl(asset);
                  const selected = referenceImages.some((image) => image.assetId === asset.id);
                  return (
                    <button
                      key={asset.id}
                      type="button"
                      onClick={() => selectAssetReference(asset)}
                      className={`group relative aspect-square overflow-hidden rounded-md border text-left transition-colors ${selected ? "border-charcoal" : "border-border-warm hover:border-charcoal/50"}`}
                      title={asset.title || asset.assetNo || String(asset.id)}
                    >
                      {previewUrl ? (
                        <img src={previewUrl} alt={asset.title || "资产图片"} className="size-full object-cover" draggable={false} />
                      ) : (
                        <div className="flex size-full items-center justify-center bg-muted text-xs text-muted-gray">图片</div>
                      )}
                      {selected && (
                        <span className="absolute right-1 top-1 rounded bg-black/60 px-1.5 py-0.5 text-[10px] text-white">
                          已选
                        </span>
                      )}
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
