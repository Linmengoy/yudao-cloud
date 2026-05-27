import request from '@/config/axios'

export interface AigcBillingRecordVO {
  id: number
  recordNo: string
  walletId: number
  userId: number
  bizType: number | string
  bizId: string
  recordType: number | string
  amount: number
  balanceAfter: number
  frozenBalanceAfter: number
  taskId?: number
  taskNo?: string
  modelId?: number
  providerId?: number
  currencyType?: number | string
  remark?: string
  createTime?: Date
}

export const AigcBillingRecordApi = {
  getRecord: async (id: number) => {
    return await request.get({ url: '/aigc/billing/record/get', params: { id } })
  },
  getRecordPage: async (params: any) => {
    return await request.get({ url: '/aigc/billing/record/page', params })
  },
  exportRecord: async (params: any) => {
    return await request.download({ url: '/aigc/billing/record/export-excel', params })
  }
}
