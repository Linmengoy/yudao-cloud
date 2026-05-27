export const billingRecordTypeMap: Record<string, string> = {
  RECHARGE: '充值',
  GIFT: '赠送',
  FREEZE: '冻结',
  CONSUME: '消费',
  RELEASE: '释放',
  REFUND: '退款',
  ADJUST_INCREASE: '余额调增',
  ADJUST_DECREASE: '余额调减',
  COMPENSATE: '系统补偿',
  '1': '充值',
  '2': '赠送',
  '3': '冻结',
  '4': '消费',
  '5': '释放',
  '6': '退款'
}

export const freezeStatusMap: Record<string, string> = {
  FROZEN: '冻结中',
  CONFIRMED: '已扣费',
  RELEASED: '已释放',
  EXPIRED: '已过期',
  PART_CONFIRMED: '部分扣费',
  PART_RELEASED: '部分释放',
  '1': '冻结中',
  '2': '已扣费',
  '3': '已释放',
  '4': '已过期'
}

export const rechargeStatusMap: Record<string, string> = {
  WAIT_PAY: '待支付',
  PAID: '已支付',
  CLOSED: '已关闭',
  REFUNDED: '已退款',
  MANUAL_SUCCESS: '人工成功',
  FAILED: '失败',
  '1': '待支付',
  '2': '已支付',
  '3': '已关闭',
  '4': '已退款',
  '5': '人工成功',
  '6': '失败'
}

export const formatPoints = (value?: number | null) => {
  return `${Number(value || 0).toLocaleString('zh-CN')} 积分`
}

export const formatPercent = (value?: number | null) => {
  return `${Number(value || 0).toFixed(2)}%`
}

export const mapText = (map: Record<string, string>, value?: number | string) => {
  if (value === undefined || value === null) return '-'
  return map[String(value)] || String(value)
}
