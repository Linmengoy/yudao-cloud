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

export type NodeDataPatchEventDetail = {
  nodeId: string;
  patch: Record<string, unknown>;
  flush?: boolean;
};

export type NodePositionPatchEventDetail = {
  nodeId: string;
  position: { x: number; y: number };
};

export type NodeEditingPresenceEventDetail = {
  nodeId: string | null;
};

export type GroupUngroupEventDetail = {
  groupId: string;
};

export type GroupArrangeEventDetail = {
  groupId: string;
  mode: "horizontal" | "grid";
};

export type EdgeDeleteEventDetail = {
  edgeId: string;
};

export type VideoFrameCaptureEventDetail = {
  sourceNodeId: string;
  dataUrl?: string;
  assetId?: number | null;
  assetVersionId?: number | null;
  previewUrl?: string | null;
  assetUrlExpireTime?: string | null;
  width?: number;
  height?: number;
  mimeType: string;
  fileName: string;
  capturedAt: "current" | "first" | "last";
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
  assetIdList?: number[];
  errorMessage: string | null;
  createdAt: string;
  completedAt: string | null;
  elapsedMs: number | null;
}

export type ResultNode = Node<ResultNodeData, "result">;

// --- Image Node ---

export interface ImageNodeOutput {
  id: string;
  previewUrl: string;
  assetId?: number | null;
  width?: number;
  height?: number;
  fileName?: string;
  mimeType?: string;
}

export interface ImageNodeData {
  [key: string]: unknown;
  imageId: string;
  assetId?: number | null;
  assetVersionId?: number | null;
  previewUrl?: string | null;
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
  taskStatus?: string | null;
  upstreamStatus?: string | null;
  progress?: number | null;
  outputAssetId?: number | null;
  outputPreviewUrl?: string | null;
  outputs?: ImageNodeOutput[];
  primaryOutputId?: string | null;
  outputsExpanded?: boolean;
  generationCount?: number;
  assetUrlExpireTime?: string | null;
  sourceTaskId?: number | null;
  safetyStatus?: string | null;
  safetyReason?: string | null;
  generationStartedAt?: string | null;
  generationCompletedAt?: string | null;
  elapsedMs?: number | null;
}

export type ImageNode = Node<ImageNodeData, "image">;

// --- Sketch Node ---

export interface SketchNodeData {
  [key: string]: unknown;
  sketchId: string;
  projectId?: string | number | null;
  assetIds?: number[];
  fileName: string;
  sceneJson?: unknown;
  previewUrl?: string | null;
  dataUrl?: string;
  mimeType: string;
  width?: number;
  height?: number;
  background?: "white" | "transparent";
  createdAt: string;
  updatedAt?: string;
}

export type SketchNode = Node<SketchNodeData, "sketch">;

// --- Text Node ---

export interface TextNodeData {
  [key: string]: unknown;
  fileName?: string;
  content: string;
  prompt: string;
  modelId: string;
  providerModel?: string;
  modelName?: string;
  aigcModelId?: number;
  status: "idle" | "pending" | "failed";
  taskId?: string | null;
  errorMessage?: string | null;
  taskStatus?: string | null;
  progress?: number | null;
  outputAssetId?: number | null;
  outputPreviewUrl?: string | null;
  assetUrlExpireTime?: string | null;
  sourceTaskId?: number | null;
  safetyStatus?: string | null;
  safetyReason?: string | null;
  generationStartedAt?: string | null;
  generationCompletedAt?: string | null;
  elapsedMs?: number | null;
  width: number;
  height: number;
  createdAt: string;
  updatedAt?: string;
}

export type TextNode = Node<TextNodeData, "text">;

// --- Video Node ---

export type VideoGenerationMode =
  | "TEXT_TO_VIDEO"
  | "IMAGE_TO_VIDEO"
  | "FIRST_LAST_FRAME_VIDEO"
  | "MULTI_REF_VIDEO";

export interface VideoNodeOutput {
  id: string;
  videoUrl: string;
  previewUrl?: string | null;
  assetId?: number | null;
  width?: number;
  height?: number;
  durationSec?: number;
  fileName?: string;
  mimeType?: string;
}

export interface VideoNodeData {
  [key: string]: unknown;
  videoId?: string;
  projectId?: string | number | null;
  assetId?: number | null;
  assetVersionId?: number | null;
  previewUrl?: string | null;
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
  providerModel?: string;
  aigcModelId?: number;
  modelName: string;
  params?: Record<string, unknown>;
  status: "idle" | "pending" | "complete" | "failed";
  taskId?: string | null;
  videoUrl?: string | null;
  errorMessage?: string | null;
  taskStatus?: string | null;
  progress?: number | null;
  outputAssetId?: number | null;
  outputPreviewUrl?: string | null;
  outputs?: VideoNodeOutput[];
  primaryOutputId?: string | null;
  outputsExpanded?: boolean;
  assetUrlExpireTime?: string | null;
  sourceTaskId?: number | null;
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
  generationCompletedAt?: string | null;
  generationRunStartedAt?: string | null;
  elapsedMs?: number | null;
  upstreamStatus?: string | null;
  explicitMode?: VideoGenerationMode | null;
  firstFrameEdgeId?: string | null;
  lastFrameEdgeId?: string | null;
  referenceImageOrder?: string[];
}

export type VideoNode = Node<VideoNodeData, "video">;

// --- Group Node ---

export interface GroupNodeData {
  [key: string]: unknown;
  fileName: string;
  childNodeIds: string[];
  width: number;
  height: number;
  createdAt: string;
  updatedAt?: string;
}

export type GroupNode = Node<GroupNodeData, "canvasGroup">;

// --- Union ---

export type AppNode = PromptNode | ResultNode | ImageNode | SketchNode | TextNode | VideoNode | GroupNode;
export type AppEdge = Edge;

export const SYNCABLE_NODE_DATA_KEYS = [
  "imageId",
  "sketchId",
  "videoId",
  "childNodeIds",
  "projectId",
  "assetId",
  "assetIds",
  "assetVersionId",
  "background",
  "fileName",
  "mimeType",
  "width",
  "height",
  "durationSec",
  "sizeBytes",
  "kind",
  "prompt",
  "content",
  "modelId",
  "provider",
  "providerModel",
  "modelName",
  "aigcModelId",
  "params",
  "status",
  "taskId",
  "errorMessage",
  "taskStatus",
  "progress",
  "outputAssetId",
  "outputs",
  "primaryOutputId",
  "outputsExpanded",
  "generationCount",
  "sourceTaskId",
  "safetyStatus",
  "safetyReason",
  "generationStartedAt",
  "generationCompletedAt",
  "generationRunStartedAt",
  "elapsedMs",
  "upstreamStatus",
  "explicitMode",
  "firstFrameEdgeId",
  "lastFrameEdgeId",
  "referenceImageOrder",
  "ratio",
  "resolution",
  "duration",
  "size",
  "generateAudio",
  "watermark",
  "createdAt",
  "updatedAt",
] as const;

export const BLOCKED_NODE_DATA_KEYS = [
  "dataUrl",
  "videoUrl",
  "blobUrl",
  "objectUrl",
  "localUrl",
  "inputImages",
  "imageUrls",
] as const;

// --- Canvas draft state (localStorage) ---

export interface CanvasState {
  nodes: AppNode[];
  edges: AppEdge[];
  viewport?: { x: number; y: number; zoom: number };
}

export type CanvasProjectRole = "owner" | "editor" | "viewer";

export interface CanvasProject {
  id: number;
  ownerUserId: number;
  name: string;
  coverAssetId?: number | null;
  coverUrl?: string | null;
  currentVersion: number;
  latestSnapshotId?: number | null;
  status: string;
  nodeCount: number;
  assetCount: number;
  role?: CanvasProjectRole | null;
  canEdit?: boolean;
  readonly?: boolean;
  createTime?: string;
  updateTime?: string;
}

export interface CanvasProjectRecycleBinRecord {
  id: number;
  projectId: number;
  ownerUserId: number;
  projectName: string;
  coverAssetId?: number | null;
  currentVersion: number;
  latestSnapshotId?: number | null;
  projectStatus: string;
  nodeCount: number;
  assetCount: number;
  deletedBy: number;
  deletedTime: string;
  deleteReason?: string | null;
  createTime?: string;
}

export interface CanvasMember {
  id: number;
  projectId: number;
  userId: number;
  nickname?: string | null;
  avatar?: string | null;
  mobile?: string | null;
  email?: string | null;
  userStatus?: number | null;
  role: CanvasProjectRole;
  inviteUserId?: number | null;
  joinedTime?: string;
  lastActiveTime?: string;
}

export interface CanvasMemberCandidate {
  userId: number;
  nickname?: string | null;
  avatar?: string | null;
  mobile?: string | null;
  email?: string | null;
  status?: number | null;
  alreadyMember?: boolean | null;
}

export interface InviteCanvasMemberRequest {
  userId: number;
  role: Exclude<CanvasProjectRole, "owner">;
}

export interface UpdateCanvasMemberRoleRequest {
  role: Exclude<CanvasProjectRole, "owner">;
}

export interface CanvasSnapshotRecord {
  id: number;
  projectId: number;
  version: number;
  nodesJson: string;
  edgesJson: string;
  viewportJson?: string | null;
  createdBy?: number | null;
  createTime?: string;
}

export interface CanvasOperationPayload {
  type: string;
  payload: Record<string, unknown>;
}

export interface CanvasOperationRecord {
  id: number;
  projectId: number;
  clientId: string;
  opId: string;
  actorUserId: number;
  baseVersion: number;
  nextVersion: number;
  operationType: string;
  operationJson: string;
  inverseOperationJson?: string | null;
  createTime?: string;
}

export interface CanvasOperationSyncResult {
  mode: "delta" | "snapshot";
  fromVersion?: number | null;
  toVersion?: number | null;
  operations?: CanvasOperationRecord[] | null;
  snapshot?: CanvasSnapshotRecord | null;
}

export interface CanvasPresence {
  projectId: string;
  userId?: number;
  clientId: string;
  cursor?: { x: number; y: number } | null;
  screenCursor?: { x: number; y: number } | null;
  selectedNodeIds?: string[];
  editingNodeId?: string | null;
  viewport?: { x: number; y: number; zoom: number };
}

// --- Defaults ---

export const DEFAULT_PROMPT_DATA: PromptNodeData = {
  prompt: "",
  modelId: DEFAULT_MODEL_ID,
  params: { ...DEFAULT_IMAGE_PARAMS },
  isGenerating: false,
};
