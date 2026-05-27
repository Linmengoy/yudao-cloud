import request from '@/config/axios'
import type { AigcModelRouteRespVO, PageParam } from '@/api/aigc/model/types'

export interface AigcModelRoutePageReqVO extends PageParam {
  name?: string
  taskType?: string
  capability?: string
  status?: number
}

export type AigcModelRouteSaveReqVO = AigcModelRouteRespVO

export const AigcModelRouteApi = {
  getRoutePage: async (params: AigcModelRoutePageReqVO) => {
    return await request.get({ url: '/aigc/model/route/page', params })
  },
  getRoute: async (id: number) => {
    return await request.get({ url: '/aigc/model/route/get?id=' + id })
  },
  createRoute: async (data: AigcModelRouteSaveReqVO) => {
    return await request.post({ url: '/aigc/model/route/create', data })
  },
  updateRoute: async (data: AigcModelRouteSaveReqVO) => {
    return await request.put({ url: '/aigc/model/route/update', data })
  },
  deleteRoute: async (id: number) => {
    return await request.delete({ url: '/aigc/model/route/delete?id=' + id })
  },
  updateRouteStatus: async (id: number, status: number) => {
    return await request.put({ url: '/aigc/model/route/status', params: { id, status } })
  }
}
