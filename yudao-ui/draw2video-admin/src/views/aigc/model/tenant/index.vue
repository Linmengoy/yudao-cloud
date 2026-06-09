<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="-mb-15px" label-width="68px">
      <el-form-item label="租户 ID" prop="tenantId"><el-input-number v-model="queryParams.tenantId" class="!w-240px" :min="1" controls-position="right" /></el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['aigc:model:tenant:create']"><Icon icon="ep:plus" class="mr-5px" /> 新增</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="租户 ID" align="center" prop="tenantId" min-width="90" />
      <el-table-column label="模型名称" align="center" prop="modelId" min-width="160">
        <template #default="scope">{{ getModelNameById(scope.row.modelId) }}</template>
      </el-table-column>
      <el-table-column label="启用" align="center" prop="enabled" min-width="90"><template #default="scope"><el-tag :type="scope.row.enabled ? 'success' : 'info'">{{ scope.row.enabled ? '启用' : '停用' }}</el-tag></template></el-table-column>
      <el-table-column label="用户端展示" align="center" prop="publicVisible" min-width="110"><template #default="scope"><el-tag :type="scope.row.publicVisible ? 'success' : 'info'">{{ scope.row.publicVisible ? '展示' : '隐藏' }}</el-tag></template></el-table-column>
      <el-table-column label="默认" align="center" prop="defaultModel" min-width="80"><template #default="scope"><el-tag v-if="scope.row.defaultModel" type="warning">默认</el-tag><span v-else>-</span></template></el-table-column>
      <el-table-column label="排序" align="center" prop="sort" min-width="80" />
      <el-table-column label="最大并发" align="center" prop="maxConcurrent" min-width="100" />
      <el-table-column label="日限额" align="center" prop="dailyLimit" min-width="100" />
      <el-table-column label="备注" align="center" prop="remark" min-width="140" />
      <el-table-column label="操作" align="center" width="160" fixed="right"><template #default="scope"><el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['aigc:model:tenant:update']">编辑</el-button><el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['aigc:model:tenant:delete']">删除</el-button></template></el-table-column>
    </el-table>
  </ContentWrap>
  <TenantForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { AigcModelTenantApi } from '@/api/aigc/model/tenant'
import { AigcModelApi } from '@/api/aigc/model/model'
import type { AigcModelRespVO, AigcModelTenantRespVO } from '@/api/aigc/model/types'
import TenantForm from './TenantForm.vue'

defineOptions({ name: 'AigcModelTenant' })

const message = useMessage()
const { t } = useI18n()
const loading = ref(false)
const list = ref<AigcModelTenantRespVO[]>([])
const modelList = ref<AigcModelRespVO[]>([])
const queryFormRef = ref()
const queryParams = reactive<{ tenantId?: number }>({ tenantId: 1 })
const getList = async () => {
  if (!queryParams.tenantId) {
    list.value = []
    return
  }
  loading.value = true
  try {
    list.value = await AigcModelTenantApi.getTenantModelList(queryParams.tenantId)
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
const getModelNameById = (modelId?: number) => {
  const model = modelList.value.find((item) => item.id === modelId)
  return model ? getModelName(model) : `模型 ${modelId}`
}
const formRef = ref()
const openForm = (type: string, id?: number) => formRef.value.open(type, id, queryParams.tenantId)
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await AigcModelTenantApi.deleteTenantModel(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}
onMounted(async () => {
  await loadModelList()
  await getList()
})
</script>
