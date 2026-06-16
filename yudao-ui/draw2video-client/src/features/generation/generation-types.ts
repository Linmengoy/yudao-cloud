export type GenerateType = "TEXT" | "IMAGE" | "VIDEO" | "AUDIO" | "CODE" | "DOCUMENT" | "PPT" | "DIGITAL_HUMAN";

export type GenerateMode = "TEXT_GENERATE" | "TEXT_TO_IMAGE" | "IMAGE_TO_IMAGE" | "TEXT_TO_VIDEO" | "IMAGE_TO_VIDEO" | string;

export type GenerateStatus =
  | "CREATED"
  | "SUBMITTING"
  | "SUBMITTED"
  | "RUNNING"
  | "CALLBACK_WAITING"
  | "SYNCING"
  | "DOWNLOADING"
  | "ASSET_CREATING"
  | "SUCCESS"
  | "FAILED"
  | "CANCELED"
  | "CANCELLED"
  | string;

export type GenerateSubmitRequest = {
  clientRequestId?: string;
  generateType: GenerateType;
  generateMode: GenerateMode;
  modelId: number;
  providerId?: number;
  prompt?: string;
  inputParams?: string;
  sync?: boolean;
  priceAmount?: number;
};

export type VerticalGenerateSubmitRequest = Omit<GenerateSubmitRequest, "generateType" | "generateMode">;

export type GenerateSubmitResponse = {
  id: number;
  taskId: number;
  generateNo: string;
  status: GenerateStatus;
};

export type GenerateResult = {
  id: number;
  taskId: number;
  generateNo: string;
  generateType: GenerateType | string;
  generateMode: GenerateMode;
  status: GenerateStatus;
  providerTaskId?: string;
  outputText?: string;
  outputData?: string;
  outputUrls?: string;
  assetIds?: string;
  failReason?: string;
  failMessage?: string;
  createTime?: string;
  finishTime?: string;
};

export type ParsedGenerateResult = GenerateResult & {
  outputUrlList: string[];
  assetIdList: number[];
  outputDataValue: unknown;
};
