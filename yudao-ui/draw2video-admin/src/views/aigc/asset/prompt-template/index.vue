<template>
  <ContentWrap>
    <el-alert
      title="上传 awesome-gpt-image-2 的 cases.json 和 data/images 目录内图片，系统会按文件名匹配并上传到 OSS。"
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
      <el-form-item label="OSS 目录" prop="storageDirectory">
        <el-input
          v-model="formData.storageDirectory"
          class="!w-360px"
          placeholder="请输入 OSS 存储目录"
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
            <Icon icon="ep:document" class="mr-5px" />选择 JSON
          </el-button>
          <template #tip>
            <div class="el-upload__tip">请选择 data/cases.json，中文内容会按 UTF-8 上传。</div>
          </template>
        </el-upload>
      </el-form-item>
      <el-form-item label="图片文件" prop="imageFiles">
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
            <Icon icon="ep:picture" class="mr-5px" />选择图片
          </el-button>
          <template #tip>
            <div class="el-upload__tip">
              在 data/images 目录中全选图片上传，文件名需与 cases.json 的 image 字段一致。
            </div>
          </template>
        </el-upload>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleImport" v-hasPermi="['aigc:asset:create']">
          <Icon icon="ep:upload-filled" class="mr-5px" />开始导入
        </el-button>
        <el-button :disabled="loading" @click="resetForm">
          <Icon icon="ep:refresh" class="mr-5px" />重置
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
  storageDirectory: [{ required: true, message: '请输入 OSS 存储目录', trigger: 'blur' }],
  casesJsonFile: [{ required: true, message: '请选择 cases.json', trigger: 'change' }],
  imageFiles: [{ required: true, message: '请选择图片文件', trigger: 'change' }]
})

const resultCards = computed(() => [
  { label: '读取案例数', value: importResult.value?.totalCount || 0 },
  { label: '新增数量', value: importResult.value?.createCount || 0 },
  { label: '更新数量', value: importResult.value?.updateCount || 0 },
  { label: '跳过数量', value: importResult.value?.skipCount || 0 }
])

const importProgress = computed(() => {
  if (!totalBatch.value) {
    return 0
  }
  return Math.min(100, Math.round((currentBatch.value / totalBatch.value) * 100))
})

const formatProgress = () => `${currentBatch.value}/${totalBatch.value} 批`

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
  try {
    for (const batch of imageBatches) {
      const data = new FormData()
      data.append('casesJson', buildBatchCasesJsonFile(casesJson, batch))
      batch.forEach((file) => data.append('images', file))
      data.append('storageDirectory', formData.storageDirectory)
      const result = await AigcPromptTemplateApi.importAwesomeGptImageFiles(data).catch((error) => {
        throw new Error(`第 ${currentBatch.value + 1} 批上传失败：${batch.map((file) => file.name).join(', ')}。${error?.message || ''}`)
      })
      importResult.value.totalCount += result.totalCount || 0
      importResult.value.createCount += result.createCount || 0
      importResult.value.updateCount += result.updateCount || 0
      importResult.value.skipCount += result.skipCount || 0
      currentBatch.value += 1
    }
    message.success('导入完成')
  } finally {
    loading.value = false
  }
}

const readCasesJson = async (file: File): Promise<AwesomeGptImageCasesJson> => {
  const text = await file.text()
  const parsed = JSON.parse(text) as AwesomeGptImageCasesJson
  if (!Array.isArray(parsed.cases)) {
    throw new Error('cases.json 缺少 cases 数组')
  }
  return parsed
}

const buildBatchCasesJsonFile = (casesJson: AwesomeGptImageCasesJson, images: File[]) => {
  const imageNames = new Set(images.map((file) => file.name))
  const batchCases = (casesJson.cases || []).filter((item) => imageNames.has(getImageFileName(item.image)))
  if (batchCases.length === 0) {
    throw new Error(`本批图片没有在 cases.json 中匹配到案例：${images.map((file) => file.name).join(', ')}`)
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
