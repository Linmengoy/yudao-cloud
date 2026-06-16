import request from '@/config/axios'
import type { AigcModelChannelRespVO, PageParam } from '@/api/aigc/model/types'

export interface AigcModelChannelPageReqVO extends PageParam {
  modelId?: number
  providerId?: number
  name?: string
  status?: number
}

export type AigcModelChannelSaveReqVO = AigcModelChannelRespVO
export interface AigcModelChannelCloneReqVO {
  sourceChannelId: number
  targetProviderId: number
  providerModel?: string
  name?: string
  weight?: number
}

export const AigcModelChannelApi = {
  getChannelPage: async (params: AigcModelChannelPageReqVO) => {
    return await request.get({ url: '/aigc/model/channel/page', params })
  },
  getChannel: async (id: number) => {
    return await request.get({ url: '/aigc/model/channel/get?id=' + id })
  },
  createChannel: async (data: AigcModelChannelSaveReqVO) => {
    return await request.post({ url: '/aigc/model/channel/create', data })
  },
  cloneChannel: async (data: AigcModelChannelCloneReqVO) => {
    return await request.post({ url: '/aigc/model/channel/clone', data })
  },
  updateChannel: async (data: AigcModelChannelSaveReqVO) => {
    return await request.put({ url: '/aigc/model/channel/update', data })
  },
  deleteChannel: async (id: number) => {
    return await request.delete({ url: '/aigc/model/channel/delete?id=' + id })
  },
  updateChannelStatus: async (id: number, status: number) => {
    return await request.put({ url: '/aigc/model/channel/status', params: { id, status } })
  }
}
