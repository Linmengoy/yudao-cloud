import request from '@/config/axios'
import type { AigcAuditPassReqVO, AigcAuditRecordPageReqVO, AigcAuditRejectReqVO } from '../types'

export const AigcAuditRecordApi = {
  getAuditRecordPage: async (params: AigcAuditRecordPageReqVO) => {
    return await request.get({ url: '/aigc/safety/audit-record/page', params })
  },
  getAuditRecord: async (id: number) => {
    return await request.get({ url: '/aigc/safety/audit-record/get?id=' + id })
  },
  markPass: async (data: AigcAuditPassReqVO) => {
    return await request.put({ url: '/aigc/safety/audit-record/pass', data })
  },
  markReject: async (data: AigcAuditRejectReqVO) => {
    return await request.put({ url: '/aigc/safety/audit-record/reject', data })
  }
}
