<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="-mb-15px" label-width="68px">
      <el-form-item label="模型" prop="modelId"><el-select v-model="queryParams.modelId" class="!w-240px" clearable filterable placeholder="请选择模型"><el-option v-for="item in modelList" :key="item.id" :label="getModelName(item)" :value="getModelOptionValue(item)" /></el-select></el-form-item>
      <el-form-item label="能力" prop="capability"><el-select v-model="queryParams.capability" class="!w-240px" clearable placeholder="请选择能力"><el-option v-for="item in AIGC_MODEL_CAPABILITIES" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['aigc:model:param:create']"><Icon icon="ep:plus" class="mr-5px" /> 新增</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="模型名称" align="center" prop="modelId" min-width="160">
        <template #default="scope">{{ getModelNameById(scope.row.modelId) }}</template>
      </el-table-column>
      <el-table-column label="模型标识" align="center" prop="modelId" min-width="180">
        <template #default="scope">{{ getModelIdentifierById(scope.row.modelId) }}</template>
      </el-table-column>
      <el-table-column label="能力" align="center" prop="capability" min-width="140"><template #default="scope">{{ getOptionLabel(AIGC_MODEL_CAPABILITIES, scope.row.capability) }}</template></el-table-column>
      <el-table-column label="参数键" align="center" prop="paramKey" min-width="130" />
      <el-table-column label="参数名" align="center" prop="paramName" min-width="130" />
      <el-table-column label="类型" align="center" prop="paramType" min-width="90"><template #default="scope">{{ getOptionLabel(AIGC_PARAM_TYPES, scope.row.paramType) }}</template></el-table-column>
      <el-table-column label="必填" align="center" prop="requiredStatus" min-width="80"><template #default="scope"><el-tag :type="scope.row.requiredStatus ? 'warning' : 'info'">{{ scope.row.requiredStatus ? '必填' : '选填' }}</el-tag></template></el-table-column>
      <el-table-column label="默认值" align="center" prop="defaultValue" min-width="120" />
      <el-table-column label="排序" align="center" prop="sort" min-width="80" />
      <el-table-column label="状态" align="center" prop="status" min-width="90"><template #default="scope"><dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" /></template></el-table-column>
      <el-table-column label="操作" align="center" width="160" fixed="right"><template #default="scope"><el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['aigc:model:param:update']">编辑</el-button><el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['aigc:model:param:delete']">删除</el-button></template></el-table-column>
    </el-table>
  </ContentWrap>
  <ParamForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { DICT_TYPE } from '@/utils/dict'
import { AigcModelApi } from '@/api/aigc/model/model'
import { AigcModelParamApi } from '@/api/aigc/model/param'
import type { AigcModelParamTemplateRespVO, AigcModelRespVO } from '@/api/aigc/model/types'
import ParamForm from './ParamForm.vue'
import { AIGC_MODEL_CAPABILITIES, AIGC_PARAM_TYPES, getOptionLabel } from '../constants'

defineOptions({ name: 'AigcModelParam' })

const message = useMessage()
const { t } = useI18n()
const loading = ref(false)
const list = ref<AigcModelParamTemplateRespVO[]>([])
const modelList = ref<AigcModelRespVO[]>([])
const queryFormRef = ref()
const queryParams = reactive<{ modelId?: number; capability?: string }>({ modelId: undefined, capability: undefined })
const getList = async () => {
  loading.value = true
  try {
    list.value = await AigcModelParamApi.getParamList({ modelId: queryParams.modelId, capability: queryParams.capability })
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
    await AigcModelParamApi.deleteParam(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}
onMounted(async () => {
  await loadModelList()
  await getList()
})
</script>
