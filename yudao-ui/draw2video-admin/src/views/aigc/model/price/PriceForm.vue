<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="960px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px" v-loading="formLoading">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="Model" prop="modelId">
            <el-select v-model="formData.modelId" class="!w-1/1" filterable>
              <el-option v-for="item in modelList" :key="item.id" :label="getModelName(item)" :value="getModelOptionValue(item)" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Capability" prop="capability">
            <el-select v-model="formData.capability" class="!w-1/1" :multiple="formType === 'create'" collapse-tags collapse-tags-tooltip>
              <el-option v-for="item in AIGC_MODEL_CAPABILITIES" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="Billing Unit" prop="billingUnit">
            <el-select v-model="formData.billingUnit" class="!w-1/1">
              <el-option v-for="item in AIGC_BILLING_UNITS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12"><el-form-item label="Currency" prop="currencyType"><el-input v-model="formData.currencyType" placeholder="POINT" /></el-form-item></el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item label="Cost Price" prop="costPrice"><el-input-number v-model="formData.costPrice" class="!w-1/1" :min="0" :precision="2" controls-position="right" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="Sale Price" prop="salePrice"><el-input-number v-model="formData.salePrice" class="!w-1/1" :min="0" :precision="2" controls-position="right" /></el-form-item></el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item label="Start Time" prop="effectiveStartTime"><el-date-picker v-model="formData.effectiveStartTime" class="!w-1/1" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="End Time" prop="effectiveEndTime"><el-date-picker v-model="formData.effectiveEndTime" class="!w-1/1" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item></el-col>
      </el-row>

      <el-form-item label="Rule Toggles">
        <el-checkbox v-model="priceRule.batchMultiplier">Batch multiplier</el-checkbox>
        <el-checkbox v-model="priceRule.durationMultiplier">Duration multiplier</el-checkbox>
      </el-form-item>

      <el-form-item label="Resolution Extra">
        <el-input v-model="resolutionExtraText" type="textarea" :rows="2" placeholder='{"720p":0,"1080p":20}' />
      </el-form-item>

      <el-form-item label="Param Multipliers">
        <el-table :data="priceRule.paramMultipliers" border>
          <el-table-column label="Param" min-width="140">
            <template #default="{ row }"><el-input v-model="row.param" placeholder="quality" /></template>
          </el-table-column>
          <el-table-column label="Operator" width="120">
            <template #default="{ row }">
              <el-select v-model="row.operator">
                <el-option label="eq" value="eq" />
                <el-option label="in" value="in" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="Value(s)" min-width="180">
            <template #default="{ row }"><el-input v-model="row.valueText" placeholder="high or high,ultra" /></template>
          </el-table-column>
          <el-table-column label="Sale x" width="120">
            <template #default="{ row }"><el-input-number v-model="row.saleMultiplier" :min="0.01" :max="100" :precision="2" controls-position="right" /></template>
          </el-table-column>
          <el-table-column label="Cost x" width="120">
            <template #default="{ row }"><el-input-number v-model="row.costMultiplier" :min="0.01" :max="100" :precision="2" controls-position="right" /></template>
          </el-table-column>
          <el-table-column label="Actions" width="90">
            <template #default="{ $index }"><el-button link type="danger" @click="removeMultiplier($index)">Remove</el-button></template>
          </el-table-column>
        </el-table>
        <el-button class="mt-10px" @click="addMultiplier"><Icon icon="ep:plus" class="mr-5px" />Add Rule</el-button>
      </el-form-item>

      <el-form-item label="Advanced JSON" prop="priceConfig">
        <el-input v-model="formData.priceConfig" type="textarea" :rows="5" @change="parsePriceConfig" />
      </el-form-item>
      <el-form-item label="Status" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">OK</el-button>
      <el-button @click="dialogVisible = false">Cancel</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { AigcModelApi } from '@/api/aigc/model/model'
import { AigcModelPriceApi, type AigcModelPriceSaveReqVO } from '@/api/aigc/model/price'
import type { AigcModelRespVO } from '@/api/aigc/model/types'
import { AIGC_BILLING_UNITS, AIGC_MODEL_CAPABILITIES } from '../constants'

interface ParamMultiplierRow {
  param: string
  operator: 'eq' | 'in'
  valueText: string
  saleMultiplier: number
  costMultiplier: number
}

defineOptions({ name: 'AigcModelPriceForm' })

const { t } = useI18n()
const message = useMessage()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const modelList = ref<AigcModelRespVO[]>([])
const formData = ref<AigcModelPriceSaveReqVO>({ id: undefined, modelId: undefined, capability: undefined, billingUnit: undefined, costPrice: 0, salePrice: 0, currencyType: 'POINT', priceConfig: undefined, effectiveStartTime: undefined, effectiveEndTime: undefined, status: CommonStatusEnum.ENABLE })
const priceRule = reactive({ batchMultiplier: false, durationMultiplier: false, paramMultipliers: [] as ParamMultiplierRow[] })
const resolutionExtraText = ref('')
const formRules = reactive({
  modelId: [{ required: true, message: 'Model is required', trigger: 'change' }],
  capability: [{ required: true, message: 'Capability is required', trigger: 'change' }],
  billingUnit: [{ required: true, message: 'Billing unit is required', trigger: 'change' }],
  salePrice: [{ required: true, message: 'Sale price is required', trigger: 'blur' }],
  currencyType: [{ required: true, message: 'Currency is required', trigger: 'blur' }],
  status: [{ required: true, message: 'Status is required', trigger: 'change' }]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  await loadModelList()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await AigcModelPriceApi.getPrice(id)
      parsePriceConfig()
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

const getModelName = (model: AigcModelRespVO) => model.name || `Model ${model.id}`
const getModelOptionValue = (model: AigcModelRespVO) => Number(model.id)

const emit = defineEmits(['success'])
const submitForm = async () => {
  await formRef.value.validate()
  syncPriceConfig()
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await Promise.all(getSelectedCapabilities().map((capability) => AigcModelPriceApi.createPrice({ ...formData.value, capability })))
      message.success(t('common.createSuccess'))
    } else {
      await AigcModelPriceApi.updatePrice(formData.value)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = { id: undefined, modelId: undefined, capability: [], billingUnit: undefined, costPrice: 0, salePrice: 0, currencyType: 'POINT', priceConfig: undefined, effectiveStartTime: undefined, effectiveEndTime: undefined, status: CommonStatusEnum.ENABLE }
  priceRule.batchMultiplier = false
  priceRule.durationMultiplier = false
  priceRule.paramMultipliers = []
  resolutionExtraText.value = ''
  formRef.value?.resetFields()
}

const addMultiplier = () => {
  priceRule.paramMultipliers.push({ param: 'quality', operator: 'eq', valueText: 'high', saleMultiplier: 1.5, costMultiplier: 1.5 })
}

const removeMultiplier = (index: number) => {
  priceRule.paramMultipliers.splice(index, 1)
}

const parsePriceConfig = () => {
  priceRule.batchMultiplier = false
  priceRule.durationMultiplier = false
  priceRule.paramMultipliers = []
  resolutionExtraText.value = ''
  if (!formData.value.priceConfig) return
  try {
    const config = JSON.parse(formData.value.priceConfig)
    priceRule.batchMultiplier = !!config.batchMultiplier
    priceRule.durationMultiplier = !!config.durationMultiplier
    resolutionExtraText.value = config.resolutionExtra ? JSON.stringify(config.resolutionExtra) : ''
    priceRule.paramMultipliers = (config.paramMultipliers || []).map((item: any) => ({
      param: item.param || '',
      operator: item.operator || 'eq',
      valueText: item.operator === 'in' ? (item.values || []).join(',') : String(item.value ?? ''),
      saleMultiplier: Number(item.saleMultiplier || 1),
      costMultiplier: Number(item.costMultiplier || item.saleMultiplier || 1)
    }))
  } catch {}
}

const syncPriceConfig = () => {
  const config: Record<string, any> = { version: 2 }
  if (priceRule.batchMultiplier) config.batchMultiplier = true
  if (priceRule.durationMultiplier) config.durationMultiplier = true
  if (resolutionExtraText.value.trim()) config.resolutionExtra = JSON.parse(resolutionExtraText.value)
  config.paramMultipliers = priceRule.paramMultipliers
    .filter((item) => item.param && item.valueText)
    .map((item) => {
      const values = item.valueText.split(',').map((value) => value.trim()).filter(Boolean)
      return item.operator === 'in'
        ? { param: item.param, operator: item.operator, values, saleMultiplier: item.saleMultiplier, costMultiplier: item.costMultiplier }
        : { param: item.param, operator: item.operator, value: values[0], saleMultiplier: item.saleMultiplier, costMultiplier: item.costMultiplier }
    })
  if (config.paramMultipliers.length === 0) delete config.paramMultipliers
  formData.value.priceConfig = JSON.stringify(config)
}

const getSelectedCapabilities = () => {
  const capability = formData.value.capability
  return Array.isArray(capability) ? capability : capability ? [capability] : []
}
</script>
