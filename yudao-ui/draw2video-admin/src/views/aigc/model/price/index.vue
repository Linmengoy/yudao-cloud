<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="-mb-15px" label-width="68px">
      <el-form-item :label="t('aigc.model.fields.model')" prop="modelId"><el-select v-model="queryParams.modelId" class="!w-240px" clearable filterable :placeholder="t('aigc.model.placeholders.model')"><el-option v-for="item in modelList" :key="item.id" :label="getModelName(item)" :value="getModelOptionValue(item)" /></el-select></el-form-item>
      <el-form-item :label="t('aigc.model.fields.capability')" prop="capability"><el-select v-model="queryParams.capability" class="!w-240px" clearable :placeholder="t('aigc.model.placeholders.capability')"><el-option v-for="item in AIGC_MODEL_CAPABILITIES" :key="item.value" :label="getOptionLabel([item], item.value, t)" :value="item.value" /></el-select></el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> {{ t('aigc.model.actions.search') }}</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> {{ t('aigc.model.actions.reset') }}</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['aigc:model:price:create']"><Icon icon="ep:plus" class="mr-5px" /> {{ t('aigc.model.actions.create') }}</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column :label="t('aigc.model.fields.modelName')" align="center" prop="modelId" min-width="160">
        <template #default="scope">{{ getModelNameById(scope.row.modelId) }}</template>
      </el-table-column>
      <el-table-column :label="t('aigc.model.fields.modelIdentifier')" align="center" prop="modelId" min-width="180">
        <template #default="scope">{{ getModelIdentifierById(scope.row.modelId) }}</template>
      </el-table-column>
      <el-table-column :label="t('aigc.model.fields.capability')" align="center" prop="capability" min-width="140"><template #default="scope">{{ getOptionLabel(AIGC_MODEL_CAPABILITIES, scope.row.capability, t) }}</template></el-table-column>
      <el-table-column :label="t('aigc.model.fields.billingUnit')" align="center" prop="billingUnit" min-width="110"><template #default="scope">{{ getOptionLabel(AIGC_BILLING_UNITS, scope.row.billingUnit, t) }}</template></el-table-column>
      <el-table-column :label="t('aigc.model.fields.costPrice')" align="center" prop="costPrice" min-width="100" />
      <el-table-column :label="t('aigc.model.fields.salePrice')" align="center" prop="salePrice" min-width="100" />
      <el-table-column :label="t('aigc.model.fields.currency')" align="center" prop="currencyType" min-width="90" />
      <el-table-column :label="t('aigc.model.fields.effectiveStart')" align="center" prop="effectiveStartTime" min-width="160" :formatter="dateFormatter" />
      <el-table-column :label="t('aigc.model.fields.effectiveEnd')" align="center" prop="effectiveEndTime" min-width="160" :formatter="dateFormatter" />
      <el-table-column :label="t('common.status')" align="center" prop="status" min-width="90"><template #default="scope"><dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" /></template></el-table-column>
      <el-table-column :label="t('table.action')" align="center" width="160" fixed="right"><template #default="scope"><el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['aigc:model:price:update']">{{ t('aigc.model.actions.edit') }}</el-button><el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['aigc:model:price:delete']">{{ t('aigc.model.actions.delete') }}</el-button></template></el-table-column>
    </el-table>
  </ContentWrap>
  <PriceForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { DICT_TYPE } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { AigcModelApi } from '@/api/aigc/model/model'
import { AigcModelPriceApi } from '@/api/aigc/model/price'
import type { AigcModelPriceRespVO, AigcModelRespVO } from '@/api/aigc/model/types'
import PriceForm from './PriceForm.vue'
import { AIGC_BILLING_UNITS, AIGC_MODEL_CAPABILITIES, getOptionLabel } from '../constants'

defineOptions({ name: 'AigcModelPrice' })

const message = useMessage()
const { t } = useI18n()
const loading = ref(false)
const list = ref<AigcModelPriceRespVO[]>([])
const modelList = ref<AigcModelRespVO[]>([])
const queryFormRef = ref()
const queryParams = reactive<{ modelId?: number; capability?: string }>({ modelId: undefined, capability: undefined })
const getList = async () => {
  loading.value = true
  try {
    list.value = await AigcModelPriceApi.getPriceList({ modelId: queryParams.modelId, capability: queryParams.capability })
  } finally {
    loading.value = false
  }
}
const handleQuery = () => getList()
const resetQuery = () => { queryFormRef.value.resetFields(); handleQuery() }
const loadModelList = async () => {
  const data = await AigcModelApi.getModelPage({ pageNo: 1, pageSize: 100 })
  modelList.value = data.list || []
}
const getModelName = (model: AigcModelRespVO) => {
  return model.name || `模型 ${model.id}`
}
const getModelById = (modelId?: number) => modelList.value.find((item) => item.id === modelId)
const getModelNameById = (modelId?: number) => {
  const model = getModelById(modelId)
  return model ? getModelName(model) : `模型 ${modelId}`
}
const getModelIdentifierById = (modelId?: number) => {
  return getModelById(modelId)?.model || '-'
}
const getModelOptionValue = (model: AigcModelRespVO) => Number(model.id)
const formRef = ref()
const openForm = (type: string, id?: number) => formRef.value.open(type, id)
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await AigcModelPriceApi.deletePrice(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}
onMounted(async () => {
  await loadModelList()
  await getList()
})
</script>
