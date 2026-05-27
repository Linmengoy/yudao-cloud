import request from '@/config/axios'

export interface AigcCostRecordVO {
  id: number
  taskId: number
  taskNo?: string
  userId: number
  modelId?: number
  providerId?: number
  capability?: string
  billingUnit?: string
  usageAmount?: number
  costAmount: number
  saleAmount: number
  grossProfit: number
  grossProfitRate: number
  createTime?: Date
}

export const AigcBillingCostApi = {
  getCost: async (id: number) => {
    return await request.get({ url: '/aigc/billing/cost/get', params: { id } })
  },
  getCostPage: async (params: any) => {
    return await request.get({ url: '/aigc/billing/cost/page', params })
  },
  getCostSummary: async (params: any) => {
    return await request.get({ url: '/aigc/billing/cost/statistics', params })
  },
  exportCost: async (params: any) => {
    return await request.download({ url: '/aigc/billing/cost/export-excel', params })
  }
}
