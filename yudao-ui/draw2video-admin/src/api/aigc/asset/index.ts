import request from '@/config/axios'

export interface AigcAssetFileVO {
  assetFileId: number
  fileRole?: string
  fileName?: string
  fileExt?: string
  mimeType?: string
  fileSize?: number
  width?: number
  height?: number
  duration?: number
  accessUrl?: string
  publicAccess?: boolean
}

export interface AigcAssetVO {
  id: number
  assetNo?: string
  userId?: number
  assetType?: string
  sourceType?: string
  bizType?: string
  bizId?: string
  taskId?: number
  taskNo?: string
  modelId?: number
  providerId?: number
  title?: string
  description?: string
  tags?: string
  files?: AigcAssetFileVO[]
  fileUrl?: string
  coverUrl?: string
  thumbnailUrl?: string
  mimeType?: string
  fileSize?: number
  width?: number
  height?: number
  duration?: number
  metadata?: string
  promptSnapshot?: string
  generateSnapshot?: string
  visibility?: string
  auditStatus?: string
  auditReason?: string
  status?: string
  viewCount?: number
  downloadCount?: number
  useCount?: number
  createTime?: string
}

export interface AigcAssetPageReqVO {
  pageNo: number
  pageSize: number
  userId?: number
  assetType?: string
  sourceType?: string
  auditStatus?: string
  visibility?: string
  status?: string
  title?: string
}

export const AigcAssetApi = {
  getAssetPage: async (params: AigcAssetPageReqVO) => {
    return await request.get({ url: '/aigc/asset/page', params })
  },
  updateAuditStatus: async (data: { id: number; auditStatus: string; auditReason?: string }) => {
    return await request.put({ url: '/aigc/asset/audit', data })
  },
  updateVisibility: async (data: { id: number; visibility: string }) => {
    return await request.put({ url: '/aigc/asset/visibility', data })
  },
  deleteAsset: async (id: number) => {
    return await request.delete({ url: '/aigc/asset/delete?id=' + id })
  },
  recoverAsset: async (id: number) => {
    return await request.put({ url: '/aigc/asset/recover?id=' + id })
  }
}
