import request from '@/config/axios'
import type { PageParam } from '@/api/aigc/task/types'

export interface AigcCommunityPostVO {
  id: number
  postNo?: string
  authorUserId?: number
  authorNickname?: string
  authorAvatarUrl?: string
  assetId?: number
  assetType?: string
  projectId?: number
  coverAssetId?: number
  coverUrl?: string
  fileUrl?: string
  title?: string
  summary?: string
  tags?: string
  promptSnapshot?: string
  metadata?: string
  visibility?: string
  publishStatus?: string
  auditStatus?: string
  auditReason?: string
  auditorUserId?: number
  auditTime?: string
  offlineReason?: string
  offlineTime?: string
  publishTime?: string
  viewCount?: number
  likeCount?: number
  commentCount?: number
  shareCount?: number
  downloadCount?: number
  hotScore?: number
  createTime?: string
}

export interface AigcCommunityCommentVO {
  id: number
  postId?: number
  userId?: number
  userNickname?: string
  userAvatarUrl?: string
  parentId?: number
  content?: string
  auditStatus?: string
  auditReason?: string
  status?: string
  likeCount?: number
  createTime?: string
}

export interface AigcCommunityPostPageReqVO extends PageParam {
  authorUserId?: number
  title?: string
  assetType?: string
  publishStatus?: string
  auditStatus?: string
  createTime?: string[]
}

export interface AigcCommunityCommentPageReqVO extends PageParam {
  postId?: number
  userId?: number
  status?: string
  auditStatus?: string
}

export interface AigcCommunityAuditReqVO {
  id: number
  reason?: string
}

export const AigcCommunityApi = {
  getPostPage: async (params: AigcCommunityPostPageReqVO) => {
    return await request.get({ url: '/aigc/community/admin/post/page', params })
  },
  getPost: async (id: number) => {
    return await request.get({ url: '/aigc/community/admin/post/get?id=' + id })
  },
  auditPassPost: async (id: number) => {
    return await request.put({ url: '/aigc/community/admin/post/audit-pass', data: { id } })
  },
  auditRejectPost: async (data: AigcCommunityAuditReqVO) => {
    return await request.put({ url: '/aigc/community/admin/post/audit-reject', data })
  },
  offlinePost: async (data: AigcCommunityAuditReqVO) => {
    return await request.put({ url: '/aigc/community/admin/post/offline', data })
  },
  restorePost: async (id: number) => {
    return await request.put({ url: '/aigc/community/admin/post/restore', data: { id } })
  },
  getCommentPage: async (params: AigcCommunityCommentPageReqVO) => {
    return await request.get({ url: '/aigc/community/admin/comment/page', params })
  },
  hideComment: async (data: AigcCommunityAuditReqVO) => {
    return await request.put({ url: '/aigc/community/admin/comment/hide', data })
  },
  deleteComment: async (id: number, reason: string) => {
    return await request.delete({ url: '/aigc/community/admin/comment/delete?id=' + id + '&reason=' + encodeURIComponent(reason) })
  }
}
