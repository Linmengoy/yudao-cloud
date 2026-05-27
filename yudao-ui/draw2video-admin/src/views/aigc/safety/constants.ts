export const AIGC_SAFETY_SCENES = [
  { label: '提示词审核', value: 'PROMPT' },
  { label: '资产审核', value: 'ASSET' },
  { label: '任务审核', value: 'TASK' },
  { label: '评论审核', value: 'COMMENT' },
  { label: '帖子审核', value: 'POST' }
]

export const AIGC_AUDIT_OBJECT_TYPES = [
  { label: '提示词', value: 'PROMPT' },
  { label: '生成任务', value: 'TASK' },
  { label: '资产', value: 'ASSET' },
  { label: '评论', value: 'COMMENT' },
  { label: '帖子', value: 'POST' }
]

export const AIGC_AUDIT_STATUSES = [
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'PASS' },
  { label: '已拒绝', value: 'REJECT' }
]

export const AIGC_AUDIT_RESULTS = [
  { label: '自动通过', value: 'AUTO_PASS' },
  { label: '自动拒绝', value: 'AUTO_REJECT' },
  { label: '人工通过', value: 'MANUAL_PASS' },
  { label: '人工拒绝', value: 'MANUAL_REJECT' }
]

export const AIGC_SENSITIVE_WORD_STATUSES = [
  { label: '启用', value: 'ENABLE' },
  { label: '禁用', value: 'DISABLE' }
]

export const AIGC_SENSITIVE_WORD_MATCH_TYPES = [
  { label: '包含匹配', value: 'CONTAINS' },
  { label: '完全匹配', value: 'EXACT' }
]

export const AIGC_RISK_LEVELS = [
  { label: '1 级', value: 1 },
  { label: '2 级', value: 2 },
  { label: '3 级', value: 3 },
  { label: '4 级', value: 4 },
  { label: '5 级', value: 5 }
]

export const getOptionLabel = (options: Array<{ label: string; value: string | number }>, value?: string | number) => {
  return options.find((item) => item.value === value)?.label || value || '-'
}
