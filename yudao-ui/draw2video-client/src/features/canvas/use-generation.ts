import type { AppNode, AppEdge, PromptNodeData, ResultNodeData, ImageNodeData, InputImageSnapshot, GenerationMode, ResultStatus } from "./types";
import { generationApi } from "@/features/generation/generation-api";
import { parseGenerateResult } from "@/features/generation/generation-result";
import type { GenerateResult, GenerateSubmitResponse } from "@/features/generation/generation-types";
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

const POLL_INTERVAL_MS = 1600;
const POLL_TIMEOUT_MS = 180000;

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

function isTerminalStatus(status?: string): boolean {
  return status === "SUCCESS" || status === "FAILED" || status === "CANCELED" || status === "CANCELLED";
}

async function pollGenerationResult(taskId: number | string, startedAt: number): Promise<GenerateResult> {
  let latest: GenerateResult | null = null;

  while (Date.now() - startedAt < POLL_TIMEOUT_MS) {
    latest = await generationApi.getResult(taskId);
    if (isTerminalStatus(latest.status)) return latest;
    await sleep(POLL_INTERVAL_MS);
  }

  if (latest) return latest;
  throw new Error("生成结果查询超时，请稍后到任务中心查看进度。");
}

function buildInputParams(data: PromptNodeData, resultDraft: ResultNodeData): string {
  return JSON.stringify({
    ...data.params,
    inputImages: resultDraft.inputImages.map((image) => ({
      imageId: image.imageId,
      fileName: image.fileName,
      mimeType: image.mimeType,
      dataUrl: image.dataUrl,
    })),
  });
}

function submitImageGeneration(data: PromptNodeData, resultDraft: ResultNodeData): Promise<GenerateSubmitResponse> {
  const modelId = data.aigcModelId;
  if (!modelId) throw new Error("请选择 AIGC 模型后再生成。");

  if (resultDraft.mode === "edit") {
    return generationApi.submit({
      generateType: "IMAGE",
      generateMode: "IMAGE_TO_IMAGE",
      modelId,
      prompt: data.prompt,
      inputParams: buildInputParams(data, resultDraft),
      sync: false,
    });
  }

  return generationApi.textToImage({
    modelId,
    prompt: data.prompt,
    inputParams: buildInputParams(data, resultDraft),
    sync: false,
  });
}

export async function createGenerationTask(
  data: PromptNodeData,
  resultDraft: ResultNodeData
): Promise<Partial<ResultNodeData>> {
  const startedAt = Date.now();
  const submitResult = await submitImageGeneration(data, resultDraft);
  const result = await pollGenerationResult(submitResult.taskId, startedAt);
  const parsed = parseGenerateResult(result);
  const elapsedMs = Date.now() - new Date(resultDraft.createdAt).getTime();

  if (parsed.status !== "SUCCESS") {
    return {
      taskId: String(submitResult.taskId),
      status: "failed" as ResultStatus,
      imageUrls: parsed.outputUrlList,
      errorMessage: parsed.failMessage ?? "生成失败，请稍后重试。",
      safetyStatus: null,
      safetyReason: parsed.failMessage ?? null,
      elapsedMs,
      completedAt: new Date().toISOString(),
    };
  }

  return {
    taskId: String(submitResult.taskId),
    status: "complete" as ResultStatus,
    imageUrls: parsed.outputUrlList,
    errorMessage: null,
    safetyStatus: null,
    safetyReason: null,
    elapsedMs,
    completedAt: new Date().toISOString(),
  };
}
