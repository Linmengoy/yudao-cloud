import request from '@/config/axios'
import type { AigcGenerateCallbackPageReqVO } from '../types'

export const AigcGenerateCallbackApi = {
  getGenerateCallbackPage: async (params: AigcGenerateCallbackPageReqVO) => {
    return await request.get({ url: '/aigc/gen/callback/page', params })
  }
}
