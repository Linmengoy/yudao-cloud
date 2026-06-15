import request from '@/config/axios'
import type { PageParam } from '@/api/aigc/task/types'

export interface AigcGuideContentVO {
  id: number
  slug?: string
  title?: string
  category?: string
  summary?: string
  content?: string
  sort?: number
  publishStatus?: string
  publishTime?: string
  publisherUserId?: number
  createTime?: string
  updateTime?: string
}

export interface AigcGuideContentPageReqVO extends PageParam {
  title?: string
  category?: string
  publishStatus?: string
  createTime?: string[]
}

export interface AigcGuideContentSaveReqVO {
  id?: number
  slug: string
  title: string
  category?: string
  summary?: string
  content: string
  sort?: number
}

export const AigcGuideContentApi = {
  getPage: async (params: AigcGuideContentPageReqVO) => {
    return await request.get({ url: '/aigc/guide/content/page', params })
  },
  get: async (id: number) => {
    return await request.get({ url: '/aigc/guide/content/get?id=' + id })
  },
  create: async (data: AigcGuideContentSaveReqVO) => {
    return await request.post({ url: '/aigc/guide/content/create', data })
  },
  update: async (data: AigcGuideContentSaveReqVO) => {
    return await request.put({ url: '/aigc/guide/content/update', data })
  },
  delete: async (id: number) => {
    return await request.delete({ url: '/aigc/guide/content/delete?id=' + id })
  },
  publish: async (id: number) => {
    return await request.put({ url: '/aigc/guide/content/publish?id=' + id })
  },
  unpublish: async (id: number) => {
    return await request.put({ url: '/aigc/guide/content/unpublish?id=' + id })
  }
}
