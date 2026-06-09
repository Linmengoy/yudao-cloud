import request from '@/config/axios'
import type { AigcModelPriceRespVO } from '@/api/aigc/model/types'

export type AigcModelPriceSaveReqVO = Omit<AigcModelPriceRespVO, 'capability'> & {
  capability?: string | string[]
}

export const AigcModelPriceApi = {
  getPriceList: async (params: { modelId?: number; capability?: string }) => {
    return await request.get({ url: '/aigc/model/price/list', params })
  },
  getPrice: async (id: number) => {
    return await request.get({ url: '/aigc/model/price/get?id=' + id })
  },
  createPrice: async (data: AigcModelPriceSaveReqVO) => {
    return await request.post({ url: '/aigc/model/price/create', data })
  },
  updatePrice: async (data: AigcModelPriceSaveReqVO) => {
    return await request.put({ url: '/aigc/model/price/update', data })
  },
  deletePrice: async (id: number) => {
    return await request.delete({ url: '/aigc/model/price/delete?id=' + id })
  },
  updatePriceStatus: async (id: number, status: number) => {
    return await request.put({ url: '/aigc/model/price/status', params: { id, status } })
  }
}
