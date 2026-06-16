import request from '@/config/axios'

export interface AigcCanvasProjectVO {
  id: number
  ownerUserId?: number
  name?: string
  coverAssetId?: number
  coverUrl?: string
  currentVersion?: number
  latestSnapshotId?: number
  status?: string
  nodeCount?: number
  assetCount?: number
  createTime?: string
  updateTime?: string
  readonly?: boolean
}

export interface AigcCanvasProjectPageReqVO {
  pageNo: number
  pageSize: number
  ownerUserId?: number
  name?: string
  status?: string
}

export const AigcCanvasApi = {
  getProjectPage: async (params: AigcCanvasProjectPageReqVO) => {
    return await request.get({ url: '/aigc/canvas/project/page', params })
  }
}
