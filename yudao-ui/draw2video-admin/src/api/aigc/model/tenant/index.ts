import request from '@/config/axios'
import type { AigcModelTenantRespVO } from '@/api/aigc/model/types'

export type AigcModelTenantSaveReqVO = AigcModelTenantRespVO

export const AigcModelTenantApi = {
  getTenantModelList: async (tenantId: number) => {
    return await request.get({ url: '/aigc/model/tenant/list', params: { tenantId } })
  },
  getTenantModel: async (id: number) => {
    return await request.get({ url: '/aigc/model/tenant/get?id=' + id })
  },
  createTenantModel: async (data: AigcModelTenantSaveReqVO) => {
    return await request.post({ url: '/aigc/model/tenant/create', data })
  },
  updateTenantModel: async (data: AigcModelTenantSaveReqVO) => {
    return await request.put({ url: '/aigc/model/tenant/update', data })
  },
  deleteTenantModel: async (id: number) => {
    return await request.delete({ url: '/aigc/model/tenant/delete?id=' + id })
  },
  updateTenantModelStatus: async (id: number, enabled: boolean) => {
    return await request.put({ url: '/aigc/model/tenant/status', data: { id, enabled } })
  },
  updateTenantModelVisible: async (id: number, publicVisible: boolean) => {
    return await request.put({ url: '/aigc/model/tenant/visible', data: { id, publicVisible } })
  },
  updateTenantModelDefault: async (id: number, defaultModel: boolean) => {
    return await request.put({ url: '/aigc/model/tenant/default', data: { id, defaultModel } })
  }
}

