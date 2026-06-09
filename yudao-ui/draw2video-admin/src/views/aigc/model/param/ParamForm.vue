<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="720px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px" v-loading="formLoading">
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item label="模型" prop="modelId"><el-select v-model="formData.modelId" class="!w-1/1" filterable placeholder="请选择模型"><el-option v-for="item in modelList" :key="item.id" :label="getModelName(item)" :value="getModelOptionValue(item)" /></el-select></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="能力" prop="capability"><el-select v-model="formData.capability" class="!w-1/1" :multiple="formType === 'create'" collapse-tags collapse-tags-tooltip placeholder="请选择能力"><el-option v-for="item in AIGC_MODEL_CAPABILITIES" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item label="参数键" prop="paramKey"><el-input v-model="formData.paramKey" placeholder="请输入参数键" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="参数名称" prop="paramName"><el-input v-model="formData.paramName" placeholder="请输入参数名称" /></el-form-item></el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item label="参数类型" prop="paramType"><el-select v-model="formData.paramType" class="!w-1/1" placeholder="请选择参数类型"><el-option v-for="item in AIGC_PARAM_TYPES" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="默认值" prop="defaultValue"><el-input v-model="formData.defaultValue" placeholder="请输入默认值" /></el-form-item></el-col>
      </el-row>
      <el-form-item label="选项配置" prop="options"><el-input v-model="optionsText" type="textarea" :rows="3" placeholder="请输入 JSON 数组或逗号分隔选项" /></el-form-item>
      <el-row :gutter="20">
        <el-col :span="8"><el-form-item label="最小值" prop="minValue"><el-input-number v-model="formData.minValue" class="!w-1/1" controls-position="right" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="最大值" prop="maxValue"><el-input-number v-model="formData.maxValue" class="!w-1/1" controls-position="right" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="排序" prop="sort"><el-input-number v-model="formData.sort" class="!w-1/1" :min="0" controls-position="right" /></el-form-item></el-col>
      </el-row>
      <el-form-item label="正则校验" prop="regexPattern"><el-input v-model="formData.regexPattern" placeholder="请输入正则表达式" /></el-form-item>
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item label="是否必填" prop="requiredStatus"><el-switch v-model="formData.requiredStatus" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="状态" prop="status"><el-radio-group v-model="formData.status"><el-radio v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio></el-radio-group></el-form-item></el-col>
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
import { AigcModelApi } from '@/api/aigc/model/model'
import { AigcModelParamApi, type AigcModelParamTemplateSaveReqVO } from '@/api/aigc/model/param'
import type { AigcModelRespVO } from '@/api/aigc/model/types'
import { AIGC_MODEL_CAPABILITIES, AIGC_PARAM_TYPES } from '../constants'

defineOptions({ name: 'AigcModelParamForm' })

const { t } = useI18n()
const message = useMessage()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const optionsText = ref('')
const modelList = ref<AigcModelRespVO[]>([])
const formData = ref<AigcModelParamTemplateSaveReqVO>({ id: undefined, modelId: undefined, capability: undefined, paramKey: undefined, paramName: undefined, paramType: undefined, requiredStatus: false, defaultValue: undefined, options: undefined, minValue: undefined, maxValue: undefined, regexPattern: undefined, sort: 0, status: CommonStatusEnum.ENABLE })
const formRules = reactive({ modelId: [{ required: true, message: '模型不能为空', trigger: 'change' }], capability: [{ required: true, message: '能力不能为空', trigger: 'change' }], paramKey: [{ required: true, message: '参数键不能为空', trigger: 'blur' }], paramName: [{ required: true, message: '参数名称不能为空', trigger: 'blur' }], paramType: [{ required: true, message: '参数类型不能为空', trigger: 'change' }], status: [{ required: true, message: '状态不能为空', trigger: 'change' }] })

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  await loadModelList()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await AigcModelParamApi.getParam(id)
      optionsText.value = formatOptionsText(formData.value.options)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const loadModelList = async () => {
  const data = await AigcModelApi.getModelPage({ pageNo: 1, pageSize: 100 })
  modelList.value = data.list || []
}

const getModelName = (model: AigcModelRespVO) => {
  return model.name || `模型 ${model.id}`
}

const getModelOptionValue = (model: AigcModelRespVO) => Number(model.id)

const emit = defineEmits(['success'])
const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const data = { ...formData.value, options: normalizeOptionsForSubmit(optionsText.value) }
    if (formType.value === 'create') {
      await Promise.all(getSelectedCapabilities().map((capability) => AigcModelParamApi.createParam({ ...data, capability })))
      message.success(t('common.createSuccess'))
    } else {
      await AigcModelParamApi.updateParam(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = { id: undefined, modelId: undefined, capability: [], paramKey: undefined, paramName: undefined, paramType: undefined, requiredStatus: false, defaultValue: undefined, options: undefined, minValue: undefined, maxValue: undefined, regexPattern: undefined, sort: 0, status: CommonStatusEnum.ENABLE }
  optionsText.value = ''
  formRef.value?.resetFields()
}

const getSelectedCapabilities = () => {
  const capability = formData.value.capability
  return Array.isArray(capability) ? capability : capability ? [capability] : []
}

const formatOptionsText = (options?: string[] | string) => {
  const optionList = parseOptions(options)
  return optionList.length > 0 ? JSON.stringify(optionList) : ''
}

const normalizeOptionsForSubmit = (options: string) => {
  const optionList = parseOptions(options)
  return optionList.length > 0 ? JSON.stringify(optionList) : undefined
}

const parseOptions = (options?: string[] | string): string[] => {
  if (!options) return []
  const rawOptions = Array.isArray(options) ? options : parseOptionsText(options)
  return rawOptions.map(decodeOptionValue).filter(Boolean)
}

const parseOptionsText = (options: string): string[] => {
  const text = options.trim()
  if (!text) return []
  try {
    const parsed = JSON.parse(text)
    if (Array.isArray(parsed)) {
      return parsed.map((item) => String(item))
    }
  } catch {
    // 非 JSON 输入按逗号分隔处理
  }
  return text.split(',').map((item) => item.trim())
}

const decodeOptionValue = (value: string): string => {
  let result = String(value).trim()
  for (let i = 0; i < 3; i++) {
    try {
      const parsed = JSON.parse(result)
      if (typeof parsed !== 'string') break
      result = parsed.trim()
    } catch {
      break
    }
  }
  return result
}
</script>
