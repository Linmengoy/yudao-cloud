import request from '@/config/axios'

export interface AigcTextSystemPromptVO {
  key: string
  value: string
}

export const AigcTextSystemPromptApi = {
  get: async (): Promise<AigcTextSystemPromptVO> => {
    return await request.get({ url: '/aigc/gen/text-system-prompt/get' })
  },

  save: async (value: string): Promise<boolean> => {
    return await request.put({ url: '/aigc/gen/text-system-prompt/save', data: { value } })
  }
}
