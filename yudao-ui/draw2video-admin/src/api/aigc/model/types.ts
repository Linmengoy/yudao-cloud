export interface PageParam {
  pageNo: number
  pageSize: number
}

export interface AigcModelRespVO {
  id?: number
  providerId?: number
  providerName?: string
  channelId?: number
  providerModel?: string
  code?: string
  name?: string
  nameEn?: string
  model?: string
  type?: number
  publicVisible?: boolean
  defaultModel?: boolean
  sort?: number
  maxConcurrent?: number
  timeoutSeconds?: number
  queuePriority?: number
  status?: number
  capabilities?: string[]
  capabilityLabel?: string
  capabilityLabelEn?: string
  remark?: string
  createTime?: Date
}

export interface AigcModelChannelRespVO {
  id?: number
  modelId?: number
  providerId?: number
  providerModel?: string
  name?: string
  costPrice?: number
  currencyType?: string
  weight?: number
  priority?: number
  maxConcurrent?: number
  timeoutSeconds?: number
  rateLimitConfig?: string
  healthStatus?: string
  status?: number
  remark?: string
  createTime?: Date
}

export interface AigcModelProviderRespVO {
  id?: number
  code?: string
  name?: string
  apiBaseUrl?: string
  authType?: string
  apiKey?: string | null
  secretKey?: string | null
  extraConfig?: string
  timeoutSeconds?: number
  proxyEnabled?: boolean
  proxyId?: number
  proxyName?: string
  proxyProtocol?: string
  proxyHost?: string
  proxyPort?: number
  proxyUsername?: string | null
  proxyPassword?: string | null
  rateLimitConfig?: string
  healthStatus?: string
  balance?: number
  status?: number
  remark?: string
  createTime?: Date
}

export interface AigcModelProxyRespVO {
  id?: number
  name?: string
  protocol?: string
  host?: string
  port?: number
  username?: string | null
  password?: string | null
  status?: number
  remark?: string
  createTime?: Date
}

export interface AigcModelParamTemplateRespVO {
  id?: number
  modelId?: number
  capability?: string
  paramKey?: string
  paramName?: string
  paramType?: string
  requiredStatus?: boolean
  defaultValue?: string
  options?: string[] | string
  minValue?: number
  maxValue?: number
  regexPattern?: string
  sort?: number
  status?: number
  createTime?: Date
}

export interface AigcModelPriceRespVO {
  id?: number
  modelId?: number
  capability?: string
  billingUnit?: string
  costPrice?: number
  salePrice?: number
  currencyType?: string
  priceConfig?: string
  effectiveStartTime?: string
  effectiveEndTime?: string
  status?: number
  createTime?: Date
}

export interface AigcModelRouteRespVO {
  id?: number
  name?: string
  taskType?: string
  modelId?: number
  capability?: string
  strategy?: string
  modelIds?: string
  channelIds?: string
  userLevel?: string
  status?: number
  createTime?: Date
}

export interface AigcModelTenantRespVO {
  id?: number
  tenantId?: number
  modelId?: number
  enabled?: boolean
  publicVisible?: boolean
  defaultModel?: boolean
  sort?: number
  maxConcurrent?: number
  dailyLimit?: number
  remark?: string
  createTime?: Date
}

export interface AigcModelUsageLogRespVO {
  id?: number
  traceId?: string
  taskId?: number
  userId?: number
  modelId?: number
  providerId?: number
  capability?: string
  requestId?: string
  externalTaskId?: string
  promptTokens?: number
  completionTokens?: number
  totalTokens?: number
  inputTokens?: number
  outputTokens?: number
  costPrice?: number
  salePrice?: number
  currencyType?: string
  status?: number
  durationMillis?: number
  errorCode?: string
  errorMessage?: string
  createTime?: Date
}

export interface AigcModelUsageTypeStatisticsRespVO {
  dimensionType?: 'MODEL_TYPE' | 'CAPABILITY' | 'MODEL_TOP' | 'FAILURE_RATE'
  modelId?: number
  modelName?: string
  modelType?: number
  capability?: string
  usageCount?: number
  successCount?: number
  failedCount?: number
  failureRate?: number
  totalTokens?: number
  costPrice?: number
  salePrice?: number
  avgDurationMillis?: number
}
