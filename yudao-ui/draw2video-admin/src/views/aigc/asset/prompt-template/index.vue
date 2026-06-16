<template>
  <ContentWrap>
    <el-alert
      :title="t('aigc.asset.promptTemplate.importTip')"
      type="info"
      :closable="false"
      show-icon
    />
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      class="mt-20px max-w-860px"
    >
      <el-form-item :label="t('aigc.asset.promptTemplate.storageDirectory')" prop="storageDirectory">
        <el-input
          v-model="formData.storageDirectory"
          class="!w-360px"
          :placeholder="t('aigc.asset.promptTemplate.storageDirectoryPlaceholder')"
        />
      </el-form-item>
      <el-form-item label="cases.json" prop="casesJsonFile">
        <el-upload
          ref="casesUploadRef"
          action="#"
          :auto-upload="false"
          :limit="1"
          :file-list="casesJsonFileList"
          accept=".json,application/json"
          :on-change="handleCasesChange"
          :on-remove="handleCasesRemove"
          :on-exceed="handleCasesExceed"
        >
          <el-button type="primary" plain>
            <Icon icon="ep:document" class="mr-5px" />{{ t('aigc.asset.promptTemplate.selectJson') }}
          </el-button>
          <template #tip>
            <div class="el-upload__tip">{{ t('aigc.asset.promptTemplate.casesTip') }}</div>
          </template>
        </el-upload>
      </el-form-item>
      <el-form-item :label="t('aigc.asset.promptTemplate.imageFiles')" prop="imageFiles">
        <el-upload
          ref="imagesUploadRef"
          action="#"
          multiple
          :auto-upload="false"
          :file-list="imageFileList"
          accept="image/png,image/jpeg,image/webp"
          :on-change="handleImagesChange"
          :on-remove="handleImagesRemove"
        >
          <el-button type="primary" plain>
            <Icon icon="ep:picture" class="mr-5px" />{{ t('aigc.asset.promptTemplate.selectImages') }}
          </el-button>
          <template #tip>
            <div class="el-upload__tip">
              {{ t('aigc.asset.promptTemplate.imagesTip') }}
            </div>
          </template>
        </el-upload>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleImport" v-hasPermi="['aigc:asset:create']">
          <Icon icon="ep:upload-filled" class="mr-5px" />{{ t('aigc.asset.promptTemplate.startImport') }}
        </el-button>
        <el-button :disabled="loading" @click="resetForm">
          <Icon icon="ep:refresh" class="mr-5px" />{{ t('common.reset') }}
        </el-button>
      </el-form-item>
    </el-form>
    <el-progress
      v-if="loading"
      class="max-w-860px"
      :percentage="importProgress"
      :stroke-width="10"
      :format="formatProgress"
    />
  </ContentWrap>

  <ContentWrap v-if="importResult">
    <el-row :gutter="16">
      <el-col v-for="item in resultCards" :key="item.label" :span="6">
        <el-card shadow="never">
          <div class="text-13px color-#909399">{{ item.label }}</div>
          <div class="mt-8px text-26px font-bold">{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>
  </ContentWrap>
</template>

<script setup lang="ts">
import type {
  FormInstance,
  FormRules,
  UploadFile,
  UploadFiles,
  UploadInstance,
  UploadRawFile,
  UploadUserFile
} from 'element-plus'
import { genFileId } from 'element-plus'
import { AigcPromptTemplateApi, type AigcPromptTemplateImportRespVO } from '@/api/aigc/asset/prompt-template'

defineOptions({ name: 'AigcPromptTemplate' })

const message = useMessage()
const { t } = useI18n()
const BATCH_SIZE = 1
const formRef = ref<FormInstance>()
const casesUploadRef = ref<UploadInstance>()
const imagesUploadRef = ref<UploadInstance>()
const loading = ref(false)
const currentBatch = ref(0)
const totalBatch = ref(0)
const importResult = ref<AigcPromptTemplateImportRespVO>()
const casesJsonFileList = ref<UploadUserFile[]>([])
const imageFileList = ref<UploadUserFile[]>([])
const formData = reactive({
  storageDirectory: 'aigc/templates',
  casesJsonFile: undefined as File | undefined,
  imageFiles: [] as File[]
})

const formRules = reactive<FormRules>({
  storageDirectory: [{ required: true, message: t('aigc.asset.promptTemplate.rules.storageDirectory'), trigger: 'blur' }],
  casesJsonFile: [{ required: true, message: t('aigc.asset.promptTemplate.rules.casesJsonFile'), trigger: 'change' }],
  imageFiles: [{ required: true, message: t('aigc.asset.promptTemplate.rules.imageFiles'), trigger: 'change' }]
})

const resultCards = computed(() => [
  { label: t('aigc.asset.promptTemplate.result.totalCount'), value: importResult.value?.totalCount || 0 },
  { label: t('aigc.asset.promptTemplate.result.createCount'), value: importResult.value?.createCount || 0 },
  { label: t('aigc.asset.promptTemplate.result.updateCount'), value: importResult.value?.updateCount || 0 },
  { label: t('aigc.asset.promptTemplate.result.skipCount'), value: importResult.value?.skipCount || 0 }
])

const importProgress = computed(() => {
  if (!totalBatch.value) {
    return 0
  }
  return Math.min(100, Math.round((currentBatch.value / totalBatch.value) * 100))
})

const formatProgress = () => t('aigc.asset.promptTemplate.batchProgress', { current: currentBatch.value, total: totalBatch.value })

interface AwesomeGptImageCase {
  image?: string
  [key: string]: unknown
}

interface AwesomeGptImageCasesJson {
  cases?: AwesomeGptImageCase[]
  [key: string]: unknown
}

const handleCasesChange = (file: UploadFile) => {
  formData.casesJsonFile = file.raw
  casesJsonFileList.value = file.raw ? [{ name: file.name, raw: file.raw }] : []
  formRef.value?.validateField('casesJsonFile')
}

const handleCasesRemove = () => {
  formData.casesJsonFile = undefined
  casesJsonFileList.value = []
  formRef.value?.validateField('casesJsonFile')
}

const handleCasesExceed = (files: File[]) => {
  casesUploadRef.value?.clearFiles()
  const file = files[0] as UploadRawFile
  file.uid = genFileId()
  formData.casesJsonFile = file
  casesJsonFileList.value = [{ name: file.name, raw: file }]
  formRef.value?.validateField('casesJsonFile')
}

const handleImagesChange = (_file: UploadFile, files: UploadFiles) => {
  formData.imageFiles = files.map((item) => item.raw).filter(Boolean) as File[]
  imageFileList.value = files
  formRef.value?.validateField('imageFiles')
}

const handleImagesRemove = (_file: UploadFile, files: UploadFiles) => {
  formData.imageFiles = files.map((item) => item.raw).filter(Boolean) as File[]
  imageFileList.value = files
  formRef.value?.validateField('imageFiles')
}

const handleImport = async () => {
  try {
    await formRef.value?.validate()
    const casesJson = await readCasesJson(formData.casesJsonFile as File)
    const imageBatches = chunkFiles(formData.imageFiles, BATCH_SIZE)
    totalBatch.value = imageBatches.length
    currentBatch.value = 0
    importResult.value = {
      totalCount: 0,
      createCount: 0,
      updateCount: 0,
      skipCount: 0
    }
    loading.value = true
    for (const batch of imageBatches) {
      const data = new FormData()
      data.append('casesJson', buildBatchCasesJsonFile(casesJson, batch))
      batch.forEach((file) => data.append('images', file))
      data.append('storageDirectory', formData.storageDirectory)
      const result = await AigcPromptTemplateApi.importAwesomeGptImageFiles(data).catch((error) => {
        throw new Error(t('aigc.asset.promptTemplate.errors.batchUploadFailed', {
          batch: currentBatch.value + 1,
          files: batch.map((file) => file.name).join(', '),
          message: error?.message || ''
        }))
      })
      importResult.value.totalCount += result.totalCount || 0
      importResult.value.createCount += result.createCount || 0
      importResult.value.updateCount += result.updateCount || 0
      importResult.value.skipCount += result.skipCount || 0
      currentBatch.value += 1
    }
    message.success(t('aigc.asset.promptTemplate.messages.importDone'))
  } catch (error) {
    message.error(error instanceof Error ? error.message : t('aigc.asset.promptTemplate.messages.importFailed'))
  } finally {
    loading.value = false
  }
}

const readCasesJson = async (file: File): Promise<AwesomeGptImageCasesJson> => {
  const text = await file.text()
  const parsed = JSON.parse(text) as AwesomeGptImageCasesJson
  if (!Array.isArray(parsed.cases)) {
    throw new Error(t('aigc.asset.promptTemplate.errors.casesArrayMissing'))
  }
  return parsed
}

const buildBatchCasesJsonFile = (casesJson: AwesomeGptImageCasesJson, images: File[]) => {
  const imageNames = new Set(images.map((file) => file.name))
  const batchCases = (casesJson.cases || []).filter((item) => imageNames.has(getImageFileName(item.image)))
  if (batchCases.length === 0) {
    throw new Error(t('aigc.asset.promptTemplate.errors.noMatchedCases', {
      files: images.map((file) => file.name).join(', ')
    }))
  }
  const batchJson = JSON.stringify({ ...casesJson, cases: batchCases })
  return new File([batchJson], 'cases.json', { type: 'application/json' })
}

const getImageFileName = (image?: string) => {
  if (!image) {
    return ''
  }
  return image.split('/').pop() || image
}

const chunkFiles = (files: File[], size: number) => {
  const chunks: File[][] = []
  for (let index = 0; index < files.length; index += size) {
    chunks.push(files.slice(index, index + size))
  }
  return chunks
}

const resetForm = () => {
  casesUploadRef.value?.clearFiles()
  imagesUploadRef.value?.clearFiles()
  formData.storageDirectory = 'aigc/templates'
  formData.casesJsonFile = undefined
  formData.imageFiles = []
  casesJsonFileList.value = []
  imageFileList.value = []
  currentBatch.value = 0
  totalBatch.value = 0
  importResult.value = undefined
  formRef.value?.clearValidate()
}
</script>
