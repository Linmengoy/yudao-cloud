import request from '@/config/axios'

export interface AigcRechargeOrderVO {
  id: number
  rechargeNo: string
  walletId: number
  userId: number
  rechargeType: number | string
  payAmount: number
  pointAmount: number
  giftAmount: number
  totalPointAmount: number
  payOrderId?: number
  payOrderNo?: string
  payChannelCode?: string
  status: number | string
  payTime?: Date
  createTime?: Date
}

export interface AigcRechargeOrderDiagnosticVO {
  rechargeOrder?: AigcRechargeOrderVO
  payOrder?: {
    id: number
    channelCode?: string
    merchantOrderId?: string
    subject?: string
    price?: number
    status?: number
    successTime?: Date
    channelUserId?: string
    channelOrderNo?: string
  }
  billingRecord?: any
  payNotify?: {
    task?: any
    logs?: any[]
  }
  payOrderMatched?: boolean
  amountMatched?: boolean
  paySuccess?: boolean
  billingRecordExists?: boolean
  diagnosticMessage?: string
}

export const AigcBillingRechargeApi = {
  getRecharge: async (id: number) => {
    return await request.get({ url: '/aigc/billing/recharge/get', params: { id } })
  },
  getDiagnostic: async (id: number) => {
    return await request.get({ url: '/aigc/billing/recharge/diagnostic', params: { id } })
  },
  getRechargePage: async (params: any) => {
    return await request.get({ url: '/aigc/billing/recharge/page', params })
  },
  manualRecharge: async (data: any) => {
    return await request.post({ url: '/aigc/billing/recharge/manual-create', data })
  },
  closeRecharge: async (id: number) => {
    return await request.put({ url: '/aigc/billing/recharge/close', params: { id } })
  },
  exportRecharge: async (params: any) => {
    return await request.download({ url: '/aigc/billing/recharge/export-excel', params })
  }
}
