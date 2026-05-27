import request from '@/config/axios'
import type { AigcGenerateProviderLogPageReqVO } from '../types'

export const AigcGenerateProviderLogApi = {
  getGenerateProviderLogPage: async (params: AigcGenerateProviderLogPageReqVO) => {
    return await request.get({ url: '/aigc/gen/provider-log/page', params })
  }
}
