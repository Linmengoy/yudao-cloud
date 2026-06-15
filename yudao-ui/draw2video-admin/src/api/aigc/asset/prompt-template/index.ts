import request from '@/config/axios'

export interface AigcPromptTemplateImportRespVO {
  totalCount: number
  createCount: number
  updateCount: number
  skipCount: number
}

export const AigcPromptTemplateApi = {
  importAwesomeGptImageFiles: async (data: FormData) => {
    return await request.post<AigcPromptTemplateImportRespVO>({
      url: '/aigc/asset/prompt-template/import-awesome-gpt-image-files',
      headersType: 'multipart/form-data',
      data
    })
  }
}
