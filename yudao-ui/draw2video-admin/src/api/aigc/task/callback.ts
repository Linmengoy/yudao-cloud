import request from '@/config/axios'
import type { AigcTaskCallbackPageReqVO } from './types'

export const AigcTaskCallbackApi = {
  getTaskCallbackPage: async (params: AigcTaskCallbackPageReqVO) => {
    return await request.get({ url: '/aigc/task/callback/page', params })
  },
  getTaskCallback: async (id: number) => {
    return await request.get({ url: '/aigc/task/callback/get?id=' + id })
  },
  replayTaskCallback: async (id: number) => {
    return await request.post({ url: '/aigc/task/callback/replay?id=' + id })
  }
}
