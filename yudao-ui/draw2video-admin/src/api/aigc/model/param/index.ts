import request from '@/config/axios'
import type { AigcModelParamTemplateRespVO } from '@/api/aigc/model/types'

export type AigcModelParamTemplateSaveReqVO = Omit<AigcModelParamTemplateRespVO, 'capability'> & {
  capability?: string | string[]
}

export interface AigcModelParamTemplateCopyReqVO {
  sourceModelId?: number
  targetModelIds: number[]
  capabilities?: string[]
  overwrite?: boolean
}

export interface AigcModelParamTemplateCopyRespVO {
  createdCount: number
  updatedCount: number
  skippedCount: number
}

export const AigcModelParamApi = {
  getParamList: async (params: { modelId?: number; capability?: string }) => {
    return await request.get({ url: '/aigc/model/param/list', params })
  },
  getParam: async (id: number) => {
    return await request.get({ url: '/aigc/model/param/get?id=' + id })
  },
  createParam: async (data: AigcModelParamTemplateSaveReqVO) => {
    return await request.post({ url: '/aigc/model/param/create', data })
  },
  copyParams: async (data: AigcModelParamTemplateCopyReqVO) => {
    return await request.post<AigcModelParamTemplateCopyRespVO>({ url: '/aigc/model/param/copy', data })
  },
  updateParam: async (data: AigcModelParamTemplateSaveReqVO) => {
    return await request.put({ url: '/aigc/model/param/update', data })
  },
  deleteParam: async (id: number) => {
    return await request.delete({ url: '/aigc/model/param/delete?id=' + id })
  }
}
