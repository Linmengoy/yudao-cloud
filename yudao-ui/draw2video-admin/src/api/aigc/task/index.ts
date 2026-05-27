import request from '@/config/axios'
import type { AigcTaskPageReqVO, AigcTaskStatusUpdateReqVO } from './types'

export const AigcTaskApi = {
  getTaskPage: async (params: AigcTaskPageReqVO) => {
    return await request.get({ url: '/aigc/task/page', params })
  },
  getTask: async (id: number) => {
    return await request.get({ url: '/aigc/task/get?id=' + id })
  },
  cancelTask: async (id: number) => {
    return await request.put({ url: '/aigc/task/cancel?id=' + id })
  },
  markTaskFailed: async (data: AigcTaskStatusUpdateReqVO) => {
    return await request.put({ url: '/aigc/task/mark-failed', data })
  },
  getTaskStatistics: async () => {
    return await request.get({ url: '/aigc/task/statistics' })
  }
}
