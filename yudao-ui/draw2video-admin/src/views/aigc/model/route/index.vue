<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="-mb-15px" label-width="68px">
      <el-form-item label="规则名称" prop="name"><el-input v-model="queryParams.name" class="!w-240px" clearable placeholder="请输入规则名称" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="展示模型编码" prop="taskType"><el-input v-model="queryParams.taskType" class="!w-180px" clearable placeholder="请输入展示模型编码" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="能力" prop="capability"><el-select v-model="queryParams.capability" class="!w-220px" clearable placeholder="请选择能力"><el-option v-for="item in AIGC_MODEL_CAPABILITIES" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" class="!w-160px" clearable placeholder="请选择状态"><el-option v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value" :label="dict.label" :value="dict.value" /></el-select></el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['aigc:model:route:create']"><Icon icon="ep:plus" class="mr-5px" /> 新增</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="规则名称" align="center" prop="name" min-width="140" />
      <el-table-column label="展示模型编码" align="center" prop="taskType" min-width="140" />
      <el-table-column label="能力" align="center" prop="capability" min-width="140"><template #default="scope">{{ getOptionLabel(AIGC_MODEL_CAPABILITIES, scope.row.capability) }}</template></el-table-column>
      <el-table-column label="路由策略" align="center" prop="strategy" min-width="120"><template #default="scope">{{ getOptionLabel(AIGC_ROUTE_STRATEGIES, scope.row.strategy) }}</template></el-table-column>
      <el-table-column label="候选模型" align="center" prop="modelIds" min-width="180">
        <template #default="scope">{{ getModelNames(scope.row.modelIds) }}</template>
      </el-table-column>
      <el-table-column label="用户等级" align="center" prop="userLevel" min-width="100" />
      <el-table-column label="状态" align="center" prop="status" min-width="90"><template #default="scope"><dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" /></template></el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" min-width="160" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="160" fixed="right"><template #default="scope"><el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['aigc:model:route:update']">编辑</el-button><el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['aigc:model:route:delete']">删除</el-button></template></el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>
  <RouteForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { AigcModelApi } from '@/api/aigc/model/model'
import { AigcModelRouteApi } from '@/api/aigc/model/route'
import type { AigcModelRespVO, AigcModelRouteRespVO } from '@/api/aigc/model/types'
import RouteForm from './RouteForm.vue'
import { AIGC_MODEL_CAPABILITIES, AIGC_ROUTE_STRATEGIES, getOptionLabel } from '../constants'

defineOptions({ name: 'AigcModelRoute' })

const message = useMessage()
const { t } = useI18n()
const loading = ref(true)
const list = ref<AigcModelRouteRespVO[]>([])
const total = ref(0)
const modelList = ref<AigcModelRespVO[]>([])
const queryFormRef = ref()
const queryParams = reactive({ pageNo: 1, pageSize: 10, name: undefined, taskType: undefined, capability: undefined, status: undefined })
const getList = async () => {
  loading.value = true
  try {
    const data = await AigcModelRouteApi.getRoutePage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}
const handleQuery = () => { queryParams.pageNo = 1; getList() }
const resetQuery = () => { queryFormRef.value.resetFields(); handleQuery() }
const loadModelList = async () => {
  const data = await AigcModelApi.getModelPage({ pageNo: 1, pageSize: 100 })
  modelList.value = data.list || []
}
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
const getModelNames = (modelIds?: string) => {
  const ids = parseModelIds(modelIds)
  if (ids.length === 0) return '-'
  return ids.map((id) => modelList.value.find((item) => item.id === id)?.name || `模型 ${id}`).join('、')
}
const formRef = ref()
const openForm = (type: string, id?: number) => formRef.value.open(type, id)
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await AigcModelRouteApi.deleteRoute(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}
onMounted(async () => {
  await loadModelList()
  await getList()
})
</script>
