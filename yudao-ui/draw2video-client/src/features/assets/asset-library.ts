"use client";

import type { AppNode, ImageNodeData, VideoNodeData } from "@/features/canvas/types";
import { loadImage, loadVideo, type CanvasMediaStoreScope } from "@/features/canvas/image-store";
import { listProjects } from "@/features/projects/project-store";

export type GeneratedAssetKind = "image" | "video";

export interface GeneratedAsset {
  id: string;
  kind: GeneratedAssetKind;
  projectId: string;
  projectName: string;
  nodeId: string;
  title: string;
  prompt: string;
  url: string;
  modelName: string;
  createdAt: string;
  elapsedMs: number | null;
  auditStatus?: string | null;
  auditReason?: string | null;
}

const CANVAS_KEY_PREFIX = "copse_canvas_draft:";

function readCanvasNodes(projectId: string, ownerKey?: string | number | null): AppNode[] {
  try {
    const scopedKey = ownerKey == null ? `${CANVAS_KEY_PREFIX}${projectId}` : `${CANVAS_KEY_PREFIX}${ownerKey}:${projectId}`;
    const raw = localStorage.getItem(scopedKey);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed?.nodes) ? parsed.nodes : [];
  } catch {
    return [];
  }
}

async function imageAssetFromNode(node: AppNode, projectId: string, projectName: string, scope: CanvasMediaStoreScope): Promise<GeneratedAsset | null> {
  if (node.type !== "image") return null;
  const data = node.data as ImageNodeData;
  if (data.kind !== "generated") return null;
  const stored = data.dataUrl ? data : await loadImage(data.imageId, scope).catch(() => null);
  const dataUrl = data.dataUrl || stored?.dataUrl;
  if (!dataUrl) return null;
  return {
    id: `${projectId}:${node.id}`,
    kind: "image",
    projectId,
    projectName,
    nodeId: node.id,
    title: data.fileName || "Generated image",
    prompt: data.prompt || "",
    url: dataUrl,
    modelName: data.modelId || "GPT Image 2",
    createdAt: data.generationCompletedAt || data.createdAt,
    elapsedMs: data.elapsedMs ?? null,
    auditStatus: data.safetyStatus,
    auditReason: data.safetyReason,
  };
}

async function videoAssetFromNode(node: AppNode, projectId: string, projectName: string, scope: CanvasMediaStoreScope): Promise<GeneratedAsset | null> {
  if (node.type !== "video") return null;
  const data = node.data as VideoNodeData;
  if (data.kind !== "generated") return null;
  const stored = data.videoUrl ? data : data.videoId ? await loadVideo(data.videoId, scope).catch(() => null) : null;
  const videoUrl = data.videoUrl || stored?.videoUrl;
  if (!videoUrl) return null;
  return {
    id: `${projectId}:${node.id}`,
    kind: "video",
    projectId,
    projectName,
    nodeId: node.id,
    title: data.fileName || "Generated video",
    prompt: data.prompt || "",
    url: videoUrl,
    modelName: data.modelName || "Seedance 2.0",
    createdAt: data.generationRunStartedAt || data.generationStartedAt || data.createdAt,
    elapsedMs: data.elapsedMs ?? null,
    auditStatus: data.safetyStatus,
    auditReason: data.safetyReason,
  };
}

export async function listGeneratedAssets(ownerKey?: string | number | null): Promise<GeneratedAsset[]> {
  const projects = listProjects(ownerKey);
  const assets = (await Promise.all(projects.map(async (project) => {
    const nodes = readCanvasNodes(project.id, ownerKey);
    const scope = { ownerKey, projectId: project.id };
    const projectAssets = await Promise.all(nodes.map(async (node) => {
      const image = await imageAssetFromNode(node, project.id, project.name, scope);
      if (image) return [image];
      const video = await videoAssetFromNode(node, project.id, project.name, scope);
      return video ? [video] : [];
    }));
    return projectAssets.flat();
  }))).flat();

  return assets.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
}

