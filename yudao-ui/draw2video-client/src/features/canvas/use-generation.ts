import type { AppNode, AppEdge, PromptNodeData, ResultNodeData, ImageNodeData, InputImageSnapshot, GenerationMode, ResultStatus } from "./types";
import { getModelById } from "@/features/image-generation/models";
import { findOpenNodePosition } from "./positioning";

let nodeCounter = 100;

function uid(): string {
  return `result_${Date.now()}_${++nodeCounter}`;
}

export interface GenerateOutput {
  newNodes: AppNode[];
  newEdges: AppEdge[];
}

export interface GenerateContext {
  nodes: AppNode[];
  edges: AppEdge[];
}

export function resolveInputImages(promptNodeId: string, ctx: GenerateContext): {
  snapshots: InputImageSnapshot[];
  ids: string[];
  mode: GenerationMode;
} {
  const incoming = ctx.edges.filter((e) => e.target === promptNodeId);
  const imageNodes = incoming
    .map((e) => ctx.nodes.find((n) => n.id === e.source))
    .filter((n): n is AppNode => n?.type === "image");

  const snapshots: InputImageSnapshot[] = imageNodes.map((n) => {
    const d = n.data as ImageNodeData;
    return {
      imageId: d.imageId,
      fileName: d.fileName,
      dataUrl: d.dataUrl,
      width: d.width,
      height: d.height,
      mimeType: d.mimeType,
    };
  });

  return {
    snapshots,
    ids: imageNodes.map((n) => (n.data as ImageNodeData).imageId),
    mode: snapshots.length > 0 ? "edit" : "generate",
  };
}

/**
 * Pure function: takes prompt node data + position + context, returns one ResultNode + Edge.
 */
export function createResultDraft(
  promptNodeId: string,
  data: PromptNodeData,
  sourcePosition: { x: number; y: number },
  ctx: GenerateContext
): GenerateOutput {
  const newNodes: AppNode[] = [];
  const newEdges: AppEdge[] = [];
  const model = getModelById(data.modelId);
  const { snapshots, ids, mode } = resolveInputImages(promptNodeId, ctx);
  const resultId = uid();

  const resultData: ResultNodeData = {
    taskId: null,
    promptNodeId,
    prompt: data.prompt,
    params: { ...data.params },
    modelId: data.modelId,
    modelName: model?.name ?? data.modelId,
    mode,
    inputImages: snapshots,
    inputImageIds: ids,
    status: "pending",
    imageUrls: [],
    errorMessage: null,
    createdAt: new Date().toISOString(),
    completedAt: null,
    elapsedMs: null,
  };

  newNodes.push({
    id: resultId,
    type: "result",
    position: findOpenNodePosition(
      { x: sourcePosition.x + 460, y: sourcePosition.y },
      { width: 260, height: 320 },
      ctx.nodes,
      { padding: 36, stepX: 180, stepY: 160 }
    ),
    data: resultData,
  });

  newEdges.push({
    id: `e-${promptNodeId}-${resultId}`,
    source: promptNodeId,
    target: resultId,
    type: "default",
  });

  return { newNodes, newEdges };
}

type CreateGenerationResponse = {
  code: number;
  msg: string;
  data: {
    taskId: string;
    status: ResultStatus;
    imageUrls: string[];
    elapsedMs: number;
    errorMessage?: string;
    safetyStatus?: string | null;
    safetyReason?: string | null;
    upstreamStatus?: number;
    upstreamDetail?: string;
  } | null;
};

export async function createGenerationTask(
  data: PromptNodeData,
  resultDraft: ResultNodeData
): Promise<Partial<ResultNodeData>> {
  const response = await fetch("/app-api/ai/generation/task/create", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      type: "image",
      mode: resultDraft.mode,
      model: data.providerModel ?? data.modelId,
      aigcModelId: data.aigcModelId,
      prompt: data.prompt,
      params: data.params,
      inputImages: resultDraft.inputImages.map((image) => ({
        dataUrl: image.dataUrl,
        fileName: image.fileName,
        mimeType: image.mimeType,
      })),
    }),
  });

  const body = (await response.json()) as CreateGenerationResponse;
  const payload = body.data;

  if (!response.ok || body.code !== 0 || !payload) {
    return {
      taskId: payload?.taskId ?? null,
      status: "failed",
      imageUrls: payload?.imageUrls ?? [],
      errorMessage: payload?.upstreamDetail ?? payload?.errorMessage ?? body.msg ?? "生成失败",
      safetyStatus: payload?.safetyStatus ?? null,
      safetyReason: payload?.safetyReason ?? null,
      elapsedMs: payload?.elapsedMs ?? Date.now() - new Date(resultDraft.createdAt).getTime(),
      completedAt: new Date().toISOString(),
    };
  }

  return {
    taskId: payload.taskId,
    status: "complete",
    imageUrls: payload.imageUrls,
    errorMessage: null,
    safetyStatus: null,
    safetyReason: null,
    elapsedMs: payload.elapsedMs,
    completedAt: new Date().toISOString(),
  };
}
