export type AigcTaskStatus =
  | "CREATED"
  | "PRICE_CALCULATED"
  | "FROZEN"
  | "QUEUED"
  | "RUNNING"
  | "SUBMITTED"
  | "CALLBACK_WAITING"
  | "DOWNLOADING"
  | "ASSET_CREATING"
  | "AUDITING"
  | "SUCCESS"
  | "FAILED"
  | "CANCELLED"
  | "REFUNDING"
  | "REFUNDED";

export type AigcTaskType =
  | "TEXT_GENERATE"
  | "TEXT_CHAT"
  | "IMAGE_TEXT_TO_IMAGE"
  | "IMAGE_TO_IMAGE"
  | "VIDEO_TEXT_TO_VIDEO"
  | "VIDEO_IMAGE_TO_VIDEO"
  | "AUDIO_TEXT_TO_SPEECH"
  | "CODE_GENERATE"
  | "DOCUMENT_GENERATE";

export interface PageResult<T> {
  list: T[];
  total: number;
}

export interface AigcTask {
  id: number;
  taskNo?: string;
  clientRequestId?: string;
  userId?: number;
  taskType?: AigcTaskType | string;
  capability?: string;
  modelId?: number;
  status: AigcTaskStatus | string;
  progress?: number;
  estimatedDurationMillis?: number;
  freezeId?: number;
  salePrice?: number;
  currencyType?: string;
  outputAssetId?: number;
  outputAssetType?: string;
  outputSummary?: string;
  outputText?: string;
  failReason?: string;
  safetyStatus?: string | null;
  auditStatus?: string | null;
  auditReason?: string | null;
  createTime?: string;
  submitTime?: string;
  startTime?: string;
  finishTime?: string;
}

export interface AigcTaskPageParams {
  pageNo: number;
  pageSize: number;
}
