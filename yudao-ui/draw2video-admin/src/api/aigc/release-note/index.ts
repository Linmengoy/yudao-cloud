import request from '@/config/axios'
import type { AigcReleaseNoteRespVO, PageParam } from '@/api/aigc/model/types'

export interface AigcReleaseNotePageReqVO extends PageParam {
  version?: string
  title?: string
  status?: number
  releaseDate?: string[]
}

export type AigcReleaseNoteSaveReqVO = AigcReleaseNoteRespVO

export const AigcReleaseNoteApi = {
  getReleaseNotePage: async (params: AigcReleaseNotePageReqVO) => {
    return await request.get({ url: '/aigc/release-note/page', params })
  },
  getReleaseNote: async (id: number) => {
    return await request.get({ url: '/aigc/release-note/get?id=' + id })
  },
  createReleaseNote: async (data: AigcReleaseNoteSaveReqVO) => {
    return await request.post({ url: '/aigc/release-note/create', data })
  },
  updateReleaseNote: async (data: AigcReleaseNoteSaveReqVO) => {
    return await request.put({ url: '/aigc/release-note/update', data })
  },
  deleteReleaseNote: async (id: number) => {
    return await request.delete({ url: '/aigc/release-note/delete?id=' + id })
  },
  updateReleaseNoteStatus: async (id: number, status: number) => {
    return await request.put({ url: '/aigc/release-note/status', params: { id, status } })
  }
}
