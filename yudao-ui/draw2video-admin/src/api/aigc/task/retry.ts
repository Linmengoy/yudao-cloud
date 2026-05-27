import request from '@/config/axios'
import type { AigcTaskRetryCreateReqVO, AigcTaskRetryPageReqVO } from './types'

export const AigcTaskRetryApi = {
  getTaskRetryPage: async (params: AigcTaskRetryPageReqVO) => {
    return await request.get({ url: '/aigc/task/retry/page', params })
  },
  cancelTaskRetry: async (id: number) => {
    return await request.put({ url: '/aigc/task/retry/cancel?id=' + id })
  },
  triggerTaskRetry: async (data: AigcTaskRetryCreateReqVO) => {
    return await request.post({ url: '/aigc/task/retry/trigger', data })
  }
}
