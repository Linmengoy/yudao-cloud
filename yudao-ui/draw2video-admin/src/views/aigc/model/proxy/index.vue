<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="-mb-15px" label-width="68px">
      <el-form-item label="代理名称" prop="name">
        <el-input v-model="queryParams.name" class="!w-240px" clearable placeholder="请输入代理名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="协议" prop="protocol">
        <el-select v-model="queryParams.protocol" class="!w-200px" clearable placeholder="请选择协议">
          <el-option v-for="item in AIGC_PROXY_PROTOCOLS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-200px" clearable placeholder="请选择状态">
          <el-option v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['aigc:model:proxy:create']"><Icon icon="ep:plus" class="mr-5px" /> 新增</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="代理名称" align="center" prop="name" min-width="150" />
      <el-table-column label="协议" align="center" prop="protocol" min-width="110">
        <template #default="scope">{{ getOptionLabel(AIGC_PROXY_PROTOCOLS, scope.row.protocol) }}</template>
      </el-table-column>
      <el-table-column label="代理地址" align="center" min-width="220">
        <template #default="scope">{{ scope.row.host }}:{{ scope.row.port }}</template>
      </el-table-column>
      <el-table-column label="用户名" align="center" prop="username" min-width="120">
        <template #default="scope">{{ scope.row.username || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" min-width="90">
        <template #default="scope"><dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" /></template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="操作" align="center" width="210" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['aigc:model:proxy:update']">编辑</el-button>
          <el-button link type="success" :loading="testingId === scope.row.id" @click="handleTest(scope.row.id)" v-hasPermi="['aigc:model:proxy:query']">测试</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['aigc:model:proxy:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>
  <ProxyForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { AigcModelProxyApi } from '@/api/aigc/model/proxy'
import type { AigcModelProxyRespVO } from '@/api/aigc/model/types'
import ProxyForm from './ProxyForm.vue'
import { AIGC_PROXY_PROTOCOLS, getOptionLabel } from '../constants'

defineOptions({ name: 'AigcModelProxy' })

const message = useMessage()
const { t } = useI18n()
const loading = ref(true)
const list = ref<AigcModelProxyRespVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const testingId = ref<number>()
const queryParams = reactive({ pageNo: 1, pageSize: 10, name: undefined, protocol: undefined, status: undefined })

const getList = async () => {
  loading.value = true
  try {
    const data = await AigcModelProxyApi.getProxyPage(queryParams)
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
    await AigcModelProxyApi.deleteProxy(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}
const handleTest = async (id: number) => {
  testingId.value = id
  try {
    const durationMillis = await AigcModelProxyApi.testProxy(id)
    message.success(`代理可用，延迟 ${durationMillis} ms`)
  } finally {
    testingId.value = undefined
  }
}

onMounted(() => getList())
</script>
