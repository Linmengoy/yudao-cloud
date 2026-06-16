<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="720px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px" v-loading="formLoading">
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item :label="t('aigc.model.fields.model')" prop="modelId"><el-select v-model="formData.modelId" class="!w-1/1" filterable :placeholder="t('aigc.model.placeholders.model')"><el-option v-for="item in modelList" :key="item.id" :label="getModelName(item)" :value="getModelOptionValue(item)" /></el-select></el-form-item></el-col>
        <el-col :span="12"><el-form-item :label="t('aigc.model.fields.capability')" prop="capability"><el-select v-model="formData.capability" class="!w-1/1" :multiple="formType === 'create'" collapse-tags collapse-tags-tooltip :placeholder="t('aigc.model.placeholders.capability')"><el-option v-for="item in AIGC_MODEL_CAPABILITIES" :key="item.value" :label="getOptionLabel([item], item.value, t)" :value="item.value" /></el-select></el-form-item></el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item :label="t('aigc.model.fields.paramKey')" prop="paramKey"><el-input v-model="formData.paramKey" :placeholder="t('aigc.model.placeholders.paramKey')" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item :label="t('aigc.model.fields.paramName')" prop="paramName"><el-input v-model="formData.paramName" :placeholder="t('aigc.model.placeholders.paramName')" /></el-form-item></el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item :label="t('aigc.model.fields.paramType')" prop="paramType"><el-select v-model="formData.paramType" class="!w-1/1" :placeholder="t('aigc.model.placeholders.paramType')"><el-option v-for="item in AIGC_PARAM_TYPES" :key="item.value" :label="getOptionLabel([item], item.value, t)" :value="item.value" /></el-select></el-form-item></el-col>
        <el-col :span="12"><el-form-item :label="t('aigc.model.fields.defaultValue')" prop="defaultValue"><el-input v-model="formData.defaultValue" :placeholder="t('aigc.model.placeholders.defaultValue')" /></el-form-item></el-col>
      </el-row>
      <el-form-item :label="t('aigc.model.fields.options')" prop="options"><el-input v-model="optionsText" type="textarea" :rows="3" :placeholder="t('aigc.model.placeholders.options')" /></el-form-item>
      <el-row :gutter="20">
        <el-col :span="8"><el-form-item :label="t('aigc.model.fields.minValue')" prop="minValue"><el-input-number v-model="formData.minValue" class="!w-1/1" controls-position="right" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item :label="t('aigc.model.fields.maxValue')" prop="maxValue"><el-input-number v-model="formData.maxValue" class="!w-1/1" controls-position="right" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item :label="t('aigc.model.fields.sort')" prop="sort"><el-input-number v-model="formData.sort" class="!w-1/1" :min="0" controls-position="right" /></el-form-item></el-col>
      </el-row>
      <el-form-item :label="t('aigc.model.fields.regexPattern')" prop="regexPattern"><el-input v-model="formData.regexPattern" :placeholder="t('aigc.model.placeholders.regexPattern')" /></el-form-item>
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item :label="t('aigc.model.fields.requiredStatus')" prop="requiredStatus"><el-switch v-model="formData.requiredStatus" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item :label="t('common.status')" prop="status"><el-radio-group v-model="formData.status"><el-radio v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio></el-radio-group></el-form-item></el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">{{ t('common.ok') }}</el-button>
      <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { AigcModelApi } from '@/api/aigc/model/model'
import { AigcModelParamApi, type AigcModelParamTemplateSaveReqVO } from '@/api/aigc/model/param'
import type { AigcModelRespVO } from '@/api/aigc/model/types'
import { AIGC_MODEL_CAPABILITIES, AIGC_PARAM_TYPES, getOptionLabel } from '../constants'

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
