export interface PageParam {
  pageNo: number
  pageSize: number
}

export interface AigcGenerateRecordPageReqVO extends PageParam {
  userId?: number
  taskId?: number
  generateNo?: string
  generateType?: string
  generateMode?: string
  modelId?: number
  providerCode?: string
  status?: string
  providerTaskId?: string
  failReason?: string
  createTime?: string[]
  submitTime?: string[]
  hasError?: boolean
}

export interface AigcGenerateRecordRespVO {
  id: number
  taskId?: number
  userId?: number
  generateNo?: string
  clientRequestId?: string
  generateType?: string
  generateMode?: string
  modelId?: number
  modelCode?: string
  providerId?: number
  providerCode?: string
  providerTaskId?: string
  providerStatus?: string
  status?: string
  prompt?: string
  inputParams?: string
  outputText?: string
  outputData?: string
  outputUrls?: string
  assetIds?: string
  freezeId?: number
  priceAmount?: number
  costAmount?: number
  submitTime?: string
  callbackTime?: string
  finishTime?: string
  failReason?: string
  failMessage?: string
  createTime?: string
}

export interface AigcGenerateCallbackPageReqVO extends PageParam {
  recordId?: number
  taskId?: number
  providerCode?: string
  providerTaskId?: string
  processStatus?: string
}

export interface AigcGenerateCallbackRespVO {
  id: number
  recordId?: number
  taskId?: number
  providerCode?: string
  providerTaskId?: string
  callbackType?: string
  callbackNo?: string
  signatureValid?: boolean
  rawBody?: string
  parsedData?: string
  processStatus?: string
  processMessage?: string
  processTime?: string
  createTime?: string
}

export interface AigcGenerateProviderLogPageReqVO extends PageParam {
  recordId?: number
  taskId?: number
  attemptId?: number
  providerCode?: string
  modelCode?: string
  apiAction?: string
  success?: boolean
}

export interface AigcGenerateProviderLogRespVO {
  id: number
  recordId?: number
  attemptId?: number
  taskId?: number
  providerCode?: string
  modelCode?: string
  apiAction?: string
  requestId?: string
  requestSummary?: string
  responseSummary?: string
  success?: boolean
  httpStatus?: number
  errorCode?: string
  errorMessage?: string
  durationMs?: number
  createTime?: string
}
