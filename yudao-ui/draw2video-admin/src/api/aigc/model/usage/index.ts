import request from '@/config/axios'
import type { AigcModelUsageTypeStatisticsRespVO, PageParam } from '@/api/aigc/model/types'

export interface AigcModelUsageLogPageReqVO extends PageParam {
  taskId?: number
  userId?: number
  modelId?: number
  providerId?: number
  capability?: string
  status?: number
}

export const AigcModelUsageApi = {
  getUsagePage: async (params: AigcModelUsageLogPageReqVO) => {
    return await request.get({ url: '/aigc/model/usage/page', params })
  },
  getUsage: async (id: number) => {
    return await request.get({ url: '/aigc/model/usage/get?id=' + id })
  },
  getUsageTypeStatistics: async (
    params: AigcModelUsageLogPageReqVO
  ): Promise<AigcModelUsageTypeStatisticsRespVO[]> => {
    return await request.get({ url: '/aigc/model/usage/type-statistics', params })
  }
}
