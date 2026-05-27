import request from '@/config/axios'
import type { AigcTaskLogPageReqVO } from './types'

export const AigcTaskLogApi = {
  getTaskLogPage: async (params: AigcTaskLogPageReqVO) => {
    return await request.get({ url: '/aigc/task/log/page', params })
  }
}
