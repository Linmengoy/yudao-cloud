import request from '@/config/axios'

export interface AigcQuotaFreezeVO {
  id: number
  freezeNo: string
  walletId: number
  userId: number
  bizType: number | string
  bizId: string
  taskId?: number
  taskNo?: string
  amount: number
  confirmedAmount: number
  releasedAmount: number
  status: number | string
  expireTime?: Date
  createTime?: Date
}

export const AigcBillingFreezeApi = {
  getFreeze: async (id: number) => {
    return await request.get({ url: '/aigc/billing/freeze/get', params: { id } })
  },
  getFreezePage: async (params: any) => {
    return await request.get({ url: '/aigc/billing/freeze/page', params })
  },
  releaseFreeze: async (data: { freezeId: number; taskId?: number; taskNo?: string; reason?: string }) => {
    return await request.put({ url: '/aigc/billing/freeze/release', data })
  },
  confirmFreeze: async (data: { freezeId: number; taskId?: number; taskNo?: string; actualAmount: number; modelId?: number; providerId?: number }) => {
    return await request.put({ url: '/aigc/billing/freeze/confirm', data })
  }
}
