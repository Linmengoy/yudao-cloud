import request from '@/config/axios'
import type { AigcModelProxyRespVO, PageParam } from '@/api/aigc/model/types'

export interface AigcModelProxyPageReqVO extends PageParam {
  name?: string
  protocol?: string
  status?: number
}

export type AigcModelProxySaveReqVO = AigcModelProxyRespVO

export const AigcModelProxyApi = {
  getProxyPage: async (params: AigcModelProxyPageReqVO) => {
    return await request.get({ url: '/aigc/model/proxy/page', params })
  },
  getProxy: async (id: number) => {
    return await request.get({ url: '/aigc/model/proxy/get?id=' + id })
  },
  getSimpleProxyList: async () => {
    return await request.get({ url: '/aigc/model/proxy/simple-list' })
  },
  createProxy: async (data: AigcModelProxySaveReqVO) => {
    return await request.post({ url: '/aigc/model/proxy/create', data })
  },
  updateProxy: async (data: AigcModelProxySaveReqVO) => {
    return await request.put({ url: '/aigc/model/proxy/update', data })
  },
  deleteProxy: async (id: number) => {
    return await request.delete({ url: '/aigc/model/proxy/delete?id=' + id })
  },
  updateProxyStatus: async (id: number, status: number) => {
    return await request.put({ url: '/aigc/model/proxy/status', params: { id, status } })
  },
  testProxy: async (id: number) => {
    return await request.get({ url: '/aigc/model/proxy/test?id=' + id })
  }
}
