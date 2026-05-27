import request from '@/config/axios'

export const AigcBillingStatisticsApi = {
  getOverview: async (params: any) => {
    return await request.get({ url: '/aigc/billing/statistics/overview', params })
  },
  getDaily: async (params: any) => {
    return await request.get({ url: '/aigc/billing/statistics/daily', params })
  },
  getModel: async (params: any) => {
    return await request.get({ url: '/aigc/billing/statistics/model', params })
  },
  getProvider: async (params: any) => {
    return await request.get({ url: '/aigc/billing/statistics/provider', params })
  },
  getUserRank: async (params: any) => {
    return await request.get({ url: '/aigc/billing/statistics/user-rank', params })
  }
}
