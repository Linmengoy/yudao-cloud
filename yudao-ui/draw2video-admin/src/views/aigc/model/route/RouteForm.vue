<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="720px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px" v-loading="formLoading">
      <el-form-item label="规则名称" prop="name"><el-input v-model="formData.name" placeholder="请输入规则名称" /></el-form-item>
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item label="展示模型编码" prop="taskType"><el-input v-model="formData.taskType" placeholder="请输入展示模型编码" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="能力" prop="capability"><el-select v-model="formData.capability" class="!w-1/1" :multiple="formType === 'create'" collapse-tags collapse-tags-tooltip clearable placeholder="请选择能力"><el-option v-for="item in AIGC_MODEL_CAPABILITIES" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item label="路由策略" prop="strategy"><el-select v-model="formData.strategy" class="!w-1/1" placeholder="请选择路由策略"><el-option v-for="item in AIGC_ROUTE_STRATEGIES" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="用户等级" prop="userLevel"><el-input v-model="formData.userLevel" placeholder="请输入用户等级" /></el-form-item></el-col>
      </el-row>
      <el-form-item label="候选模型" prop="modelIds">
        <el-select v-model="selectedModelIds" class="!w-1/1" multiple filterable collapse-tags collapse-tags-tooltip placeholder="请选择候选模型">
          <el-option v-for="item in modelList" :key="item.id" :label="getModelName(item)" :value="getModelOptionValue(item)" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status"><el-radio-group v-model="formData.status"><el-radio v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio></el-radio-group></el-form-item>
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
import { AigcModelRouteApi, type AigcModelRouteSaveReqVO } from '@/api/aigc/model/route'
import type { AigcModelRespVO } from '@/api/aigc/model/types'
import { AIGC_MODEL_CAPABILITIES, AIGC_ROUTE_STRATEGIES } from '../constants'

defineOptions({ name: 'AigcModelRouteForm' })

type RouteFormData = Omit<AigcModelRouteSaveReqVO, 'capability'> & { capability?: string | string[] }

const { t } = useI18n()
const message = useMessage()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const selectedModelIds = ref<number[]>([])
const modelList = ref<AigcModelRespVO[]>([])
const formData = ref<RouteFormData>({ id: undefined, name: undefined, taskType: undefined, capability: undefined, strategy: 'FIXED_MODEL', modelIds: undefined, userLevel: undefined, status: CommonStatusEnum.ENABLE })
const formRules = reactive({ name: [{ required: true, message: '规则名称不能为空', trigger: 'blur' }], strategy: [{ required: true, message: '路由策略不能为空', trigger: 'change' }], status: [{ required: true, message: '状态不能为空', trigger: 'change' }] })

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  await loadModelList()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await AigcModelRouteApi.getRoute(id)
      selectedModelIds.value = parseModelIds(formData.value.modelIds)
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

const parseModelIds = (modelIds?: string): number[] => {
  if (!modelIds) return []
  try {
    const parsed = JSON.parse(modelIds)
    if (Array.isArray(parsed)) {
      return parsed.map((item) => Number(item)).filter(Boolean)
    }
  } catch {
    // 兼容历史逗号分隔的模型 ID 配置
  }
  return modelIds.split(',').map((item) => Number(item.trim())).filter(Boolean)
}

const emit = defineEmits(['success'])
const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const data = { ...formData.value, modelIds: JSON.stringify(selectedModelIds.value) }
    if (formType.value === 'create') {
      await Promise.all(getSelectedCapabilities().map((capability) => AigcModelRouteApi.createRoute({ ...data, capability })))
      message.success(t('common.createSuccess'))
    } else {
      await AigcModelRouteApi.updateRoute({ ...data, capability: getSelectedCapabilities()[0] })
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = { id: undefined, name: undefined, taskType: undefined, capability: [], strategy: 'FIXED_MODEL', modelIds: undefined, userLevel: undefined, status: CommonStatusEnum.ENABLE }
  selectedModelIds.value = []
  formRef.value?.resetFields()
}

const getSelectedCapabilities = () => {
  const capability = formData.value.capability
  return Array.isArray(capability) ? capability : capability ? [capability] : []
}
</script>
