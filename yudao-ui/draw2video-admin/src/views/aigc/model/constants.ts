export const AIGC_MODEL_TYPES = [
  { label: '文本', labelKey: 'aigc.model.options.modelTypes.text', value: 1 },
  { label: '图片', labelKey: 'aigc.model.options.modelTypes.image', value: 2 },
  { label: '视频', labelKey: 'aigc.model.options.modelTypes.video', value: 3 },
  { label: '音频', labelKey: 'aigc.model.options.modelTypes.audio', value: 4 },
  { label: '审核', labelKey: 'aigc.model.options.modelTypes.audit', value: 5 }
]

export const AIGC_MODEL_CAPABILITIES = [
  { label: '文生图', labelKey: 'aigc.model.options.capabilities.textToImage', value: 'TEXT_TO_IMAGE' },
  { label: '图生图', labelKey: 'aigc.model.options.capabilities.imageToImage', value: 'IMAGE_TO_IMAGE' },
  { label: '文生视频', labelKey: 'aigc.model.options.capabilities.textToVideo', value: 'TEXT_TO_VIDEO' },
  { label: '图生视频', labelKey: 'aigc.model.options.capabilities.imageToVideo', value: 'IMAGE_TO_VIDEO' },
  { label: '首尾帧视频', labelKey: 'aigc.model.options.capabilities.firstLastFrameVideo', value: 'FIRST_LAST_FRAME_VIDEO' },
  { label: '多参考视频', labelKey: 'aigc.model.options.capabilities.multiRefVideo', value: 'MULTI_REF_VIDEO' },
  { label: '文本生成', labelKey: 'aigc.model.options.capabilities.textGenerate', value: 'TEXT_GENERATE' },
  { label: '提示词优化', labelKey: 'aigc.model.options.capabilities.promptOptimize', value: 'PROMPT_OPTIMIZE' },
  { label: '剧本生成', labelKey: 'aigc.model.options.capabilities.scriptGenerate', value: 'SCRIPT_GENERATE' }
]

export const AIGC_PARAM_TYPES = [
  { label: '文本', labelKey: 'aigc.model.options.paramTypes.string', value: 'STRING' },
  { label: '数字', labelKey: 'aigc.model.options.paramTypes.number', value: 'NUMBER' },
  { label: '布尔', labelKey: 'aigc.model.options.paramTypes.boolean', value: 'BOOLEAN' },
  { label: '单选', labelKey: 'aigc.model.options.paramTypes.select', value: 'SELECT' },
  { label: '多选', labelKey: 'aigc.model.options.paramTypes.multiSelect', value: 'MULTI_SELECT' },
  { label: 'JSON', labelKey: 'aigc.model.options.paramTypes.json', value: 'JSON' }
]

export const AIGC_BILLING_UNITS = [
  { label: '按任务', labelKey: 'aigc.model.options.billingUnits.perTask', value: 'PER_TASK' },
  { label: '按张', labelKey: 'aigc.model.options.billingUnits.perImage', value: 'PER_IMAGE' },
  { label: '按秒', labelKey: 'aigc.model.options.billingUnits.perSecond', value: 'PER_SECOND' },
  { label: '每 5 秒', labelKey: 'aigc.model.options.billingUnits.per5Seconds', value: 'PER_5_SECONDS' },
  { label: '按批次', labelKey: 'aigc.model.options.billingUnits.perBatch', value: 'PER_BATCH' }
]

export const AIGC_ROUTE_STRATEGIES = [
  { label: '固定模型', labelKey: 'aigc.model.options.routeStrategies.fixedModel', value: 'FIXED_MODEL' },
  { label: '最低成本', labelKey: 'aigc.model.options.routeStrategies.lowestCost', value: 'LOWEST_COST' },
  { label: '最高成功率', labelKey: 'aigc.model.options.routeStrategies.highestSuccessRate', value: 'HIGHEST_SUCCESS_RATE' },
  { label: '最快响应', labelKey: 'aigc.model.options.routeStrategies.fastestResponse', value: 'FASTEST_RESPONSE' },
  { label: '轮询', labelKey: 'aigc.model.options.routeStrategies.roundRobin', value: 'ROUND_ROBIN' }
]

export const AIGC_PROVIDER_AUTH_TYPES = [
  { label: 'Bearer Token', value: 'BEARER' },
  { label: 'API Key', value: 'API_KEY' },
  { label: 'Basic Auth', value: 'BASIC' },
  { label: '自定义', labelKey: 'aigc.model.options.authTypes.custom', value: 'CUSTOM' }
]

export const AIGC_HEALTH_STATUSES = [
  { label: '未知', labelKey: 'aigc.model.options.healthStatuses.unknown', value: 'UNKNOWN' },
  { label: '健康', labelKey: 'aigc.model.options.healthStatuses.healthy', value: 'HEALTHY' },
  { label: '异常', labelKey: 'aigc.model.options.healthStatuses.unhealthy', value: 'UNHEALTHY' },
  { label: '受限', labelKey: 'aigc.model.options.healthStatuses.limited', value: 'LIMITED' },
  { label: '余额不足', labelKey: 'aigc.model.options.healthStatuses.balanceLow', value: 'BALANCE_LOW' }
]

export const AIGC_PROXY_PROTOCOLS = [
  { label: 'HTTP', value: 'HTTP' },
  { label: 'HTTPS', value: 'HTTPS' },
  { label: 'SOCKS5', value: 'SOCKS5' },
  { label: 'SOCKS5H（远程 DNS）', labelKey: 'aigc.model.options.proxyProtocols.socks5h', value: 'SOCKS5H' }
]

export const AIGC_USAGE_STATUSES = [
  { label: '成功', value: 0 },
  { label: '失败', value: 1 }
]

type OptionTranslator = (key: string) => string

export const getOptionLabel = (
  options: Array<{ label: string; labelKey?: string; value: string | number }>,
  value?: string | number,
  t?: OptionTranslator
) => {
  const option = options.find((item) => item.value === value)
  if (!option) return value || '-'
  return option.labelKey && t ? t(option.labelKey) : option.label
}
