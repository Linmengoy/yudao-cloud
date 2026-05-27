export interface PageParam {
  pageNo: number
  pageSize: number
}

export interface AigcSensitiveWordRespVO {
  id?: number
  word?: string
  scene?: string
  level?: number
  matchType?: string
  status?: string
  remark?: string
  createTime?: Date
}

export interface AigcSensitiveWordPageReqVO extends PageParam {
  word?: string
  scene?: string
  level?: number
  matchType?: string
  status?: string
  createTime?: Date[]
}

export interface AigcAuditRecordRespVO {
  id?: number
  objectType?: string
  objectId?: number
  contentSummary?: string
  scene?: string
  auditStatus?: string
  auditResult?: string
  hitWords?: string
  riskLevel?: number
  rejectReason?: string
  auditorUserId?: number
  auditTime?: Date
  createTime?: Date
}

export interface AigcAuditRecordPageReqVO extends PageParam {
  objectType?: string
  objectId?: number
  scene?: string
  auditStatus?: string
  auditResult?: string
  riskLevel?: number
  createTime?: Date[]
}

export interface AigcAuditPassReqVO {
  auditId: number
  remark?: string
}

export interface AigcAuditRejectReqVO {
  auditId: number
  reason: string
}
