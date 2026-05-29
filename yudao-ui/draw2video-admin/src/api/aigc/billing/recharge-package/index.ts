import request from '@/config/axios'

export interface AigcRechargePackageVO {
  id?: number
  name: string
  payAmount: number
  pointAmount: number
  giftAmount: number
  totalPointAmount?: number
  description?: string
  features?: string
  recommendStatus: boolean
  sort: number
  status: number
  remark?: string
  createTime?: Date
}

export const AigcRechargePackageApi = {
  getPackagePage: async (params: any) => {
    return await request.get({ url: '/aigc/billing/recharge-package/page', params })
  },
  getPackage: async (id: number) => {
    return await request.get({ url: '/aigc/billing/recharge-package/get?id=' + id })
  },
  createPackage: async (data: AigcRechargePackageVO) => {
    return await request.post({ url: '/aigc/billing/recharge-package/create', data })
  },
  updatePackage: async (data: AigcRechargePackageVO) => {
    return await request.put({ url: '/aigc/billing/recharge-package/update', data })
  },
  deletePackage: async (id: number) => {
    return await request.delete({ url: '/aigc/billing/recharge-package/delete?id=' + id })
  }
}
