import request from '@/config/axios'
import type { AigcGenerateRecordPageReqVO } from '../types'

export const AigcGenerateRecordApi = {
  getGenerateRecordPage: async (params: AigcGenerateRecordPageReqVO) => {
    return await request.get({ url: '/aigc/gen/record/page', params })
  },
  getGenerateRecord: async (id: number) => {
    return await request.get({ url: '/aigc/gen/record/get?id=' + id })
  },
  syncGenerateTask: async (taskId: number) => {
    return await request.post({ url: '/aigc/gen/record/sync?taskId=' + taskId })
  }
}
