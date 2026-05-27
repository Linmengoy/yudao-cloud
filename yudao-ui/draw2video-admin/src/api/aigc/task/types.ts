export interface PageParam {
  pageNo: number
  pageSize: number
}

export interface AigcTaskPageReqVO extends PageParam {
  userId?: number
  taskNo?: string
  taskType?: string
  modelId?: number
  status?: string
}

export interface AigcTaskRespVO {
  id: number
  taskNo?: string
  clientRequestId?: string
  userId?: number
  taskType?: string
  capability?: string
  modelId?: number
  providerId?: number
  status?: string
  progress?: number
  freezeId?: number
  salePrice?: number
  costPrice?: number
  currencyType?: string
  externalTaskId?: string
  outputAssetId?: number
  outputAssetType?: string
  outputText?: string
  outputData?: string
  failCode?: string
  failReason?: string
  createTime?: string
  finishTime?: string
}

export interface AigcTaskStatusUpdateReqVO {
  taskId: number
  failCode?: string
  failReason?: string
  progress?: number
  outputAssetId?: number
  outputAssetType?: string
  outputText?: string
  outputData?: string
}

export interface AigcTaskStatisticsRespVO {
  totalCount?: number
  successCount?: number
  failedCount?: number
  refundingCount?: number
  backlogCount?: number
  timeoutCount?: number
  successRate?: number
  failedRate?: number
  avgDurationSeconds?: number
}

export interface AigcTaskLogPageReqVO extends PageParam {
  taskId?: number
  taskNo?: string
}

export interface AigcTaskLogRespVO {
  id: number
  taskId?: number
  taskNo?: string
  fromStatus?: string
  toStatus?: string
  action?: string
  message?: string
  operatorType?: string
  operatorId?: number
  extraInfo?: string
  createTime?: string
}

export interface AigcTaskCallbackPageReqVO extends PageParam {
  taskId?: number
  providerCode?: string
  externalTaskId?: string
  callbackStatus?: string
}

export interface AigcTaskCallbackRespVO {
  id: number
  callbackNo?: string
  taskId?: number
  taskNo?: string
  providerCode?: string
  externalTaskId?: string
  callbackType?: string
  callbackStatus?: string
  callbackData?: string
  headers?: string
  signature?: string
  processResult?: string
  failReason?: string
  receiveTime?: string
  processTime?: string
  createTime?: string
}

export interface AigcTaskRetryPageReqVO extends PageParam {
  taskId?: number
  taskNo?: string
  retryStatus?: string
}

export interface AigcTaskRetryRespVO {
  id: number
  retryNo?: string
  taskId?: number
  taskNo?: string
  retryType?: string
  retryStatus?: string
  retryTimes?: number
  nextRetryTime?: string
  startTime?: string
  endTime?: string
  failReason?: string
  operatorId?: number
  createTime?: string
}

export interface AigcTaskRetryCreateReqVO {
  taskId: number
  retryType?: string
  nextRetryTime?: string
  operatorId?: number
}
