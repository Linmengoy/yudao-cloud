<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="-mb-15px" label-width="68px">
      <el-form-item label="渠道编码" prop="code">
        <el-input v-model="queryParams.code" class="!w-240px" clearable placeholder="请输入渠道编码" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="渠道名称" prop="name">
        <el-input v-model="queryParams.name" class="!w-240px" clearable placeholder="请输入渠道名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-240px" clearable placeholder="请选择状态">
          <el-option v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['aigc:model:provider:create']"><Icon icon="ep:plus" class="mr-5px" /> 新增</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="渠道编码" align="center" prop="code" min-width="120" />
      <el-table-column label="渠道名称" align="center" prop="name" min-width="140" />
      <el-table-column label="API 地址" align="center" prop="apiBaseUrl" min-width="220" />
      <el-table-column label="鉴权方式" align="center" prop="authType" min-width="120">
        <template #default="scope">{{ getOptionLabel(AIGC_PROVIDER_AUTH_TYPES, scope.row.authType) }}</template>
      </el-table-column>
      <el-table-column label="健康状态" align="center" prop="healthStatus" min-width="110">
        <template #default="scope">{{ getOptionLabel(AIGC_HEALTH_STATUSES, scope.row.healthStatus) }}</template>
      </el-table-column>
      <el-table-column label="代理" align="center" min-width="140">
        <template #default="scope">{{ scope.row.proxyEnabled ? scope.row.proxyName || '-' : '-' }}</template>
      </el-table-column>
      <el-table-column label="余额" align="center" prop="balance" min-width="100" />
      <el-table-column label="状态" align="center" prop="status" min-width="90">
        <template #default="scope"><dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" /></template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="操作" align="center" width="220" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['aigc:model:provider:update']">编辑</el-button>
          <el-button link type="success" @click="handleTest(scope.row.id)" v-hasPermi="['aigc:model:provider:query']">测试</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['aigc:model:provider:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>
  <ProviderForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { AigcModelProviderApi } from '@/api/aigc/model/provider'
import type { AigcModelProviderRespVO } from '@/api/aigc/model/types'
import ProviderForm from './ProviderForm.vue'
import { AIGC_HEALTH_STATUSES, AIGC_PROVIDER_AUTH_TYPES, getOptionLabel } from '../constants'

defineOptions({ name: 'AigcModelProvider' })

const message = useMessage()
const { t } = useI18n()
const loading = ref(true)
const list = ref<AigcModelProviderRespVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive({ pageNo: 1, pageSize: 10, code: undefined, name: undefined, status: undefined })

const getList = async () => {
  loading.value = true
  try {
    const data = await AigcModelProviderApi.getProviderPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}
const formRef = ref()
const openForm = (type: string, id?: number) => formRef.value.open(type, id)
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await AigcModelProviderApi.deleteProvider(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}
const handleTest = async (id: number) => {
  await AigcModelProviderApi.testProvider(id)
  message.success('渠道测试通过')
}

onMounted(() => getList())
</script>
