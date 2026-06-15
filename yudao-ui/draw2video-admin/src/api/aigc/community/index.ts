import request from '@/config/axios'

export interface AigcCommunityPostVO {
  id: number
  postNo?: string
  authorUserId?: number
  authorNickname?: string
  authorAvatarUrl?: string
  assetId?: number
  assetType?: string
  projectId?: number
  coverUrl?: string
  fileUrl?: string
  title?: string
  summary?: string
  tags?: string
  publishStatus?: string
  auditStatus?: string
  auditReason?: string
  offlineReason?: string
  publishTime?: string
  viewCount?: number
  likeCount?: number
  commentCount?: number
  shareCount?: number
  createTime?: string
}

export interface AigcCommunityCommentVO {
  id: number
  postId?: number
  userId?: number
  userNickname?: string
  content?: string
  auditStatus?: string
  auditReason?: string
  status?: string
  createTime?: string
}

export const AigcCommunityApi = {
  getPostPage: async (params: any) => {
    return await request.get({ url: '/aigc/community/admin/post/page', params })
  },
  getPost: async (id: number) => {
    return await request.get({ url: '/aigc/community/admin/post/get?id=' + id })
  },
  auditPassPost: async (id: number) => {
    return await request.put({ url: '/aigc/community/admin/post/audit-pass', data: { id } })
  },
  auditRejectPost: async (data: { id: number; reason: string }) => {
    return await request.put({ url: '/aigc/community/admin/post/audit-reject', data })
  },
  offlinePost: async (data: { id: number; reason: string }) => {
    return await request.put({ url: '/aigc/community/admin/post/offline', data })
  },
  restorePost: async (id: number) => {
    return await request.put({ url: '/aigc/community/admin/post/restore', data: { id } })
  },
  getCommentPage: async (params: any) => {
    return await request.get({ url: '/aigc/community/admin/comment/page', params })
  },
  hideComment: async (data: { id: number; reason: string }) => {
    return await request.put({ url: '/aigc/community/admin/comment/hide', data })
  },
  deleteComment: async (id: number, reason: string) => {
    return await request.delete({ url: '/aigc/community/admin/comment/delete?id=' + id + '&reason=' + encodeURIComponent(reason) })
  }
}
