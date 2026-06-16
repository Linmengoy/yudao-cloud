<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="780px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px" v-loading="formLoading">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="版本号" prop="version">
            <el-input v-model="formData.version" placeholder="例如 v1.4.0" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="发布日期" prop="releaseDate">
            <el-date-picker v-model="formData.releaseDate" class="!w-1/1" type="date" value-format="YYYY-MM-DD" placeholder="请选择发布日期" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="标题" prop="title">
        <el-input v-model="formData.title" placeholder="请输入标题" />
      </el-form-item>
      <el-form-item label="摘要" prop="summary">
        <el-input v-model="formData.summary" type="textarea" :rows="3" maxlength="512" show-word-limit placeholder="请输入更新摘要" />
      </el-form-item>
      <el-form-item label="内容" prop="content">
        <el-input v-model="formData.content" type="textarea" :rows="8" placeholder="可按功能、修复等分类填写更新内容" />
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="发布人" prop="publisher">
            <el-input v-model="formData.publisher" placeholder="请输入发布人" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { AigcReleaseNoteApi, AigcReleaseNoteSaveReqVO } from '@/api/aigc/release-note'

defineOptions({ name: 'AigcReleaseNoteForm' })

const { t } = useI18n()
const message = useMessage()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const formData = ref<AigcReleaseNoteSaveReqVO>({
  id: undefined,
  version: undefined,
  releaseDate: undefined,
  title: undefined,
  summary: undefined,
  content: undefined,
  status: CommonStatusEnum.DISABLE,
  publisher: undefined
})
const formRules = reactive({
  version: [{ required: true, message: '版本号不能为空', trigger: 'blur' }],
  releaseDate: [{ required: true, message: '发布日期不能为空', trigger: 'change' }],
  title: [{ required: true, message: '标题不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await AigcReleaseNoteApi.getReleaseNote(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])
const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const data = { ...formData.value }
    if (formType.value === 'create') {
      await AigcReleaseNoteApi.createReleaseNote(data)
      message.success(t('common.createSuccess'))
    } else {
      await AigcReleaseNoteApi.updateReleaseNote(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = {
    id: undefined,
    version: undefined,
    releaseDate: undefined,
    title: undefined,
    summary: undefined,
    content: undefined,
    status: CommonStatusEnum.DISABLE,
    publisher: undefined
  }
  formRef.value?.resetFields()
}
</script>
