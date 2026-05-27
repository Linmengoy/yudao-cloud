import type { Node, Edge } from "@xyflow/react";
import type { ImageTaskParams } from "@/features/image-generation/types";
import { DEFAULT_IMAGE_PARAMS } from "@/features/image-generation/types";
import { DEFAULT_MODEL_ID } from "@/features/image-generation/models";

// --- Prompt Node ---

export interface PromptNodeData {
  [key: string]: unknown;
  prompt: string;
  modelId: string;
  providerModel?: string;
  modelName?: string;
  aigcModelId?: number;
  params: ImageTaskParams;
  isGenerating: boolean;
}

export type ReferencePickerEventDetail = {
  promptId: string | null;
};

export type NodeCreateMenuEventDetail = {
  nodeId: string;
  direction: "incoming" | "outgoing";
  clientX: number;
  clientY: number;
};

export type PromptNode = Node<PromptNodeData, "prompt">;

// --- Result Node ---

export type ResultStatus = "pending" | "complete" | "failed";
export type GenerationMode = "generate" | "edit";

export interface InputImageSnapshot {
  imageId: string;
  fileName: string;
  dataUrl: string;
  width?: number;
  height?: number;
  mimeType: string;
}

export interface ResultNodeData {
  [key: string]: unknown;
  taskId: string | null;
  promptNodeId: string;
  prompt: string;
  params: ImageTaskParams;
  modelId: string;
  providerModel?: string;
  aigcModelId?: number;
  modelName: string;
  mode: GenerationMode;
  inputImages: InputImageSnapshot[];
  inputImageIds: string[];
  status: ResultStatus;
  imageUrls: string[];
  errorMessage: string | null;
  createdAt: string;
  completedAt: string | null;
  elapsedMs: number | null;
}

export type ResultNode = Node<ResultNodeData, "result">;

// --- Image Node ---

export interface ImageNodeData {
  [key: string]: unknown;
  imageId: string;
  fileName: string;
  dataUrl: string;
  mimeType: string;
  width?: number;
  height?: number;
  createdAt: string;
  kind?: "uploaded" | "draft" | "generated";
  prompt?: string;
  modelId?: string;
  providerModel?: string;
  modelName?: string;
  aigcModelId?: number;
  params?: ImageTaskParams;
  status?: "idle" | "pending" | "failed";
  taskId?: string | null;
  errorMessage?: string | null;
  safetyStatus?: string | null;
  safetyReason?: string | null;
  generationStartedAt?: string | null;
  generationCompletedAt?: string | null;
  elapsedMs?: number | null;
}

export type ImageNode = Node<ImageNodeData, "image">;

// --- Text Node ---

export interface TextNodeData {
  [key: string]: unknown;
  content: string;
  prompt: string;
  modelId: string;
  status: "idle" | "pending" | "failed";
  errorMessage?: string | null;
  safetyStatus?: string | null;
  safetyReason?: string | null;
  width: number;
  height: number;
  createdAt: string;
  updatedAt?: string;
}

export type TextNode = Node<TextNodeData, "text">;

// --- Video Node ---

export interface VideoNodeData {
  [key: string]: unknown;
  videoId?: string;
  fileName?: string;
  mimeType?: string;
  width?: number;
  height?: number;
  durationSec?: number;
  sizeBytes?: number;
  kind?: "uploaded" | "draft" | "generated";
  prompt: string;
  provider?: "seedance" | "wan";
  modelId: string;
  modelName: string;
  status: "idle" | "pending" | "complete" | "failed";
  taskId?: string | null;
  videoUrl?: string | null;
  errorMessage?: string | null;
  safetyStatus?: string | null;
  safetyReason?: string | null;
  ratio: "16:9" | "4:3" | "1:1" | "3:4" | "9:16" | "21:9";
  resolution: "480p" | "720p" | "1080p";
  duration: 5 | 10;
  size?: "1280*704" | "704*1280";
  generateAudio: boolean;
  watermark: boolean;
  createdAt: string;
  generationStartedAt?: string | null;
  generationRunStartedAt?: string | null;
  elapsedMs?: number | null;
  upstreamStatus?: string | null;
}

export type VideoNode = Node<VideoNodeData, "video">;

// --- Union ---

export type AppNode = PromptNode | ResultNode | ImageNode | TextNode | VideoNode;
export type AppEdge = Edge;

// --- Canvas draft state (localStorage) ---

export interface CanvasState {
  nodes: AppNode[];
  edges: AppEdge[];
  viewport?: { x: number; y: number; zoom: number };
}

// --- Defaults ---

export const DEFAULT_PROMPT_DATA: PromptNodeData = {
  prompt: "",
  modelId: DEFAULT_MODEL_ID,
  params: { ...DEFAULT_IMAGE_PARAMS },
  isGenerating: false,
};
