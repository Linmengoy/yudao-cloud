export const AIGC_MODEL_TYPES = [
  { label: '文本', value: 1 },
  { label: '图片', value: 2 },
  { label: '视频', value: 3 },
  { label: '音频', value: 4 },
  { label: '审核', value: 5 }
]

export const AIGC_MODEL_CAPABILITIES = [
  { label: '文生图', value: 'TEXT_TO_IMAGE' },
  { label: '图生图', value: 'IMAGE_TO_IMAGE' },
  { label: '文生视频', value: 'TEXT_TO_VIDEO' },
  { label: '图生视频', value: 'IMAGE_TO_VIDEO' },
  { label: '首尾帧视频', value: 'FIRST_LAST_FRAME_VIDEO' },
  { label: '多参考视频', value: 'MULTI_REF_VIDEO' },
  { label: '文本生成', value: 'TEXT_GENERATE' },
  { label: '提示词优化', value: 'PROMPT_OPTIMIZE' },
  { label: '剧本生成', value: 'SCRIPT_GENERATE' }
]

export const AIGC_PARAM_TYPES = [
  { label: '文本', value: 'STRING' },
  { label: '数字', value: 'NUMBER' },
  { label: '布尔', value: 'BOOLEAN' },
  { label: '单选', value: 'SELECT' },
  { label: '多选', value: 'MULTI_SELECT' },
  { label: 'JSON', value: 'JSON' }
]

export const AIGC_BILLING_UNITS = [
  { label: '按任务', value: 'PER_TASK' },
  { label: '按张', value: 'PER_IMAGE' },
  { label: '按秒', value: 'PER_SECOND' },
  { label: '每 5 秒', value: 'PER_5_SECONDS' },
  { label: '按批次', value: 'PER_BATCH' }
]

export const AIGC_ROUTE_STRATEGIES = [
  { label: '固定模型', value: 'FIXED_MODEL' },
  { label: '最低成本', value: 'LOWEST_COST' },
  { label: '最高成功率', value: 'HIGHEST_SUCCESS_RATE' },
  { label: '最快响应', value: 'FASTEST_RESPONSE' },
  { label: '轮询', value: 'ROUND_ROBIN' }
]

export const AIGC_PROVIDER_AUTH_TYPES = [
  { label: 'Bearer Token', value: 'BEARER' },
  { label: 'API Key', value: 'API_KEY' },
  { label: 'Basic Auth', value: 'BASIC' },
  { label: '自定义', value: 'CUSTOM' }
]

export const AIGC_HEALTH_STATUSES = [
  { label: '未知', value: 'UNKNOWN' },
  { label: '健康', value: 'HEALTHY' },
  { label: '异常', value: 'UNHEALTHY' },
  { label: '受限', value: 'LIMITED' },
  { label: '余额不足', value: 'BALANCE_LOW' }
]

export const AIGC_PROXY_PROTOCOLS = [
  { label: 'HTTP', value: 'HTTP' },
  { label: 'HTTPS', value: 'HTTPS' },
  { label: 'SOCKS5', value: 'SOCKS5' },
  { label: 'SOCKS5H（远程 DNS）', value: 'SOCKS5H' }
]

export const AIGC_USAGE_STATUSES = [
  { label: '成功', value: 0 },
  { label: '失败', value: 1 }
]

export const getOptionLabel = (options: Array<{ label: string; value: string | number }>, value?: string | number) => {
  return options.find((item) => item.value === value)?.label || value || '-'
}
