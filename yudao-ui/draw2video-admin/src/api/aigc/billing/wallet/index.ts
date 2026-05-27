import request from '@/config/axios'

export interface AigcWalletVO {
  id: number
  userId: number
  balance: number
  frozenBalance: number
  totalRecharge: number
  totalGift: number
  totalConsume: number
  totalRefund: number
  status: number
  lastTransTime?: Date
  createTime?: Date
}

export interface AigcWalletAdjustReqVO {
  userId: number
  amount: number
  remark: string
}

export interface AigcWalletGiftReqVO {
  userId: number
  amount: number
  remark: string
}

export const AigcBillingWalletApi = {
  getWallet: async (id: number) => {
    return await request.get({ url: '/aigc/billing/wallet/get', params: { userId: id } })
  },
  getWalletPage: async (params: any) => {
    return await request.get({ url: '/aigc/billing/wallet/page', params })
  },
  adjustWallet: async (data: AigcWalletAdjustReqVO) => {
    return await request.put({ url: '/aigc/billing/wallet/adjust', data })
  },
  giftWallet: async (data: AigcWalletGiftReqVO) => {
    return await request.post({ url: '/aigc/billing/wallet/gift', data })
  }
}
