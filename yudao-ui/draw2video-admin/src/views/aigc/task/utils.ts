export const taskStatusOptions = [
  { label: '已创建', value: 'CREATED' },
  { label: '已计价', value: 'PRICE_CALCULATED' },
  { label: '已冻结积分', value: 'FROZEN' },
  { label: '排队中', value: 'QUEUED' },
  { label: '运行中', value: 'RUNNING' },
  { label: '已提交供应商', value: 'SUBMITTED' },
  { label: '等待回调', value: 'CALLBACK_WAITING' },
  { label: '下载结果中', value: 'DOWNLOADING' },
  { label: '资产创建中', value: 'ASSET_CREATING' },
  { label: '审核中', value: 'AUDITING' },
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '退款中', value: 'REFUNDING' },
  { label: '已退款', value: 'REFUNDED' }
]

export const taskTypeOptions = [
  { label: '文本生成', value: 'TEXT_GENERATE' },
  { label: '文本对话', value: 'TEXT_CHAT' },
  { label: '文生图', value: 'IMAGE_TEXT_TO_IMAGE' },
  { label: '图生图', value: 'IMAGE_TO_IMAGE' },
  { label: '文生视频', value: 'VIDEO_TEXT_TO_VIDEO' },
  { label: '图生视频', value: 'VIDEO_IMAGE_TO_VIDEO' },
  { label: '文本转语音', value: 'AUDIO_TEXT_TO_SPEECH' },
  { label: '代码生成', value: 'CODE_GENERATE' },
  { label: '文档生成', value: 'DOCUMENT_GENERATE' }
]

export const cancellableStatuses = ['CREATED', 'PRICE_CALCULATED', 'FROZEN', 'QUEUED']

export const statusLabel = (value?: string) => taskStatusOptions.find((item) => item.value === value)?.label || value || '-'

export const typeLabel = (value?: string) => taskTypeOptions.find((item) => item.value === value)?.label || value || '-'

export const formatJson = (value?: string) => {
  if (!value) return '-'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}
