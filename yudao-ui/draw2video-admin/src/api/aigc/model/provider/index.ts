import request from '@/config/axios'
import type { AigcModelProviderRespVO, PageParam } from '@/api/aigc/model/types'

export interface AigcModelProviderPageReqVO extends PageParam {
  code?: string
  name?: string
  status?: number
}

export type AigcModelProviderSaveReqVO = AigcModelProviderRespVO

export const AigcModelProviderApi = {
  getProviderPage: async (params: AigcModelProviderPageReqVO) => {
    return await request.get({ url: '/aigc/model/provider/page', params })
  },
  getProvider: async (id: number) => {
    return await request.get({ url: '/aigc/model/provider/get?id=' + id })
  },
  createProvider: async (data: AigcModelProviderSaveReqVO) => {
    return await request.post({ url: '/aigc/model/provider/create', data })
  },
  updateProvider: async (data: AigcModelProviderSaveReqVO) => {
    return await request.put({ url: '/aigc/model/provider/update', data })
  },
  deleteProvider: async (id: number) => {
    return await request.delete({ url: '/aigc/model/provider/delete?id=' + id })
  },
  updateProviderStatus: async (id: number, status: number) => {
    return await request.put({ url: '/aigc/model/provider/status', data: { id, status } })
  },
  testProvider: async (id: number) => {
    return await request.post({ url: '/aigc/model/provider/test', data: { id } })
  }
}

