import request from '@/config/axios'
import type { AigcModelRespVO, PageParam } from '@/api/aigc/model/types'

export interface AigcModelPageReqVO extends PageParam {
  providerId?: number
  code?: string
  name?: string
  type?: number
  status?: number
}

export type AigcModelSaveReqVO = AigcModelRespVO

export const AigcModelApi = {
  getModelPage: async (params: AigcModelPageReqVO) => {
    return await request.get({ url: '/aigc/model/page', params })
  },
  getModel: async (id: number) => {
    return await request.get({ url: '/aigc/model/get?id=' + id })
  },
  createModel: async (data: AigcModelSaveReqVO) => {
    return await request.post({ url: '/aigc/model/create', data })
  },
  updateModel: async (data: AigcModelSaveReqVO) => {
    return await request.put({ url: '/aigc/model/update', data })
  },
  deleteModel: async (id: number) => {
    return await request.delete({ url: '/aigc/model/delete?id=' + id })
  },
  updateModelStatus: async (id: number, status: number) => {
    return await request.put({ url: '/aigc/model/status', data: { id, status } })
  },
  updateModelVisible: async (id: number, publicVisible: boolean) => {
    return await request.put({ url: '/aigc/model/visible', data: { id, publicVisible } })
  },
  updateModelDefault: async (id: number, defaultModel: boolean) => {
    return await request.put({ url: '/aigc/model/default', data: { id, defaultModel } })
  }
}

