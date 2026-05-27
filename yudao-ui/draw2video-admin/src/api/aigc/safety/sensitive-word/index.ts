import request from '@/config/axios'
import type { AigcSensitiveWordPageReqVO, AigcSensitiveWordRespVO } from '../types'

export type AigcSensitiveWordSaveReqVO = AigcSensitiveWordRespVO

export interface AigcSensitiveWordStatusReqVO {
  id: number
  status: string
}

export const AigcSensitiveWordApi = {
  getSensitiveWordPage: async (params: AigcSensitiveWordPageReqVO) => {
    return await request.get({ url: '/aigc/safety/sensitive-word/page', params })
  },
  getSensitiveWord: async (id: number) => {
    return await request.get({ url: '/aigc/safety/sensitive-word/get?id=' + id })
  },
  createSensitiveWord: async (data: AigcSensitiveWordSaveReqVO) => {
    return await request.post({ url: '/aigc/safety/sensitive-word/create', data })
  },
  updateSensitiveWord: async (data: AigcSensitiveWordSaveReqVO) => {
    return await request.put({ url: '/aigc/safety/sensitive-word/update', data })
  },
  deleteSensitiveWord: async (id: number) => {
    return await request.delete({ url: '/aigc/safety/sensitive-word/delete?id=' + id })
  },
  updateSensitiveWordStatus: async (data: AigcSensitiveWordStatusReqVO) => {
    return await request.put({ url: '/aigc/safety/sensitive-word/update-status', data })
  }
}
