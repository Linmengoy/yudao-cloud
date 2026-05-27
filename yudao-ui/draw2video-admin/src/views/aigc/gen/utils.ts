export const generateTypeOptions = [
  { label: '文本', value: 'TEXT' },
  { label: '图片', value: 'IMAGE' },
  { label: '视频', value: 'VIDEO' },
  { label: '音频', value: 'AUDIO' },
  { label: '代码', value: 'CODE' },
  { label: '文档', value: 'DOCUMENT' },
  { label: 'PPT', value: 'PPT' },
  { label: '数字人', value: 'DIGITAL_HUMAN' }
]

export const generateModeOptions = [
  { label: '文本生成', value: 'TEXT_GENERATE' },
  { label: '文生图', value: 'TEXT_TO_IMAGE' },
  { label: '图生图', value: 'IMAGE_TO_IMAGE' },
  { label: '文生视频', value: 'TEXT_TO_VIDEO' },
  { label: '图生视频', value: 'IMAGE_TO_VIDEO' }
]

export const generateStatusOptions = [
  { label: '已创建', value: 'CREATED' },
  { label: '已提交渠道', value: 'SUBMITTED' },
  { label: '运行中', value: 'RUNNING' },
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
  { label: '已取消', value: 'CANCELED' },
  { label: '已取消', value: 'CANCELLED' }
]

export const callbackProcessStatusOptions = [
  { label: '已接收', value: 'RECEIVED' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已处理', value: 'PROCESSED' },
  { label: '处理失败', value: 'FAILED' },
  { label: '已忽略', value: 'IGNORED' }
]

export const typeLabel = (value?: string) => generateTypeOptions.find((item) => item.value === value)?.label || value || '-'

export const modeLabel = (value?: string) => generateModeOptions.find((item) => item.value === value)?.label || value || '-'

export const statusLabel = (value?: string) => generateStatusOptions.find((item) => item.value === value)?.label || value || '-'

export const processStatusLabel = (value?: string) => callbackProcessStatusOptions.find((item) => item.value === value)?.label || value || '-'

export const formatJson = (value?: string) => {
  if (!value) return '-'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

export const successLabel = (value?: boolean) => {
  if (value === true) return '成功'
  if (value === false) return '失败'
  return '-'
}
