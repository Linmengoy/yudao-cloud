<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="-mb-15px" label-width="68px">
      <el-form-item :label="t('aigc.model.fields.proxyName')" prop="name">
        <el-input v-model="queryParams.name" class="!w-240px" clearable :placeholder="t('aigc.model.placeholders.proxyName')" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item :label="t('aigc.model.fields.protocol')" prop="protocol">
        <el-select v-model="queryParams.protocol" class="!w-200px" clearable :placeholder="t('aigc.model.placeholders.protocol')">
          <el-option v-for="item in AIGC_PROXY_PROTOCOLS" :key="item.value" :label="getOptionLabel([item], item.value, t)" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('common.status')" prop="status">
        <el-select v-model="queryParams.status" class="!w-200px" clearable :placeholder="t('aigc.model.placeholders.status')">
          <el-option v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> {{ t('aigc.model.actions.search') }}</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> {{ t('aigc.model.actions.reset') }}</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['aigc:model:proxy:create']"><Icon icon="ep:plus" class="mr-5px" /> {{ t('aigc.model.actions.create') }}</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column :label="t('aigc.model.fields.proxyName')" align="center" prop="name" min-width="150" />
      <el-table-column :label="t('aigc.model.fields.protocol')" align="center" prop="protocol" min-width="110">
        <template #default="scope">{{ getOptionLabel(AIGC_PROXY_PROTOCOLS, scope.row.protocol, t) }}</template>
      </el-table-column>
      <el-table-column :label="t('aigc.model.fields.proxyAddress')" align="center" min-width="220">
        <template #default="scope">{{ scope.row.host }}:{{ scope.row.port }}</template>
      </el-table-column>
      <el-table-column :label="t('aigc.model.fields.username')" align="center" prop="username" min-width="120">
        <template #default="scope">{{ scope.row.username || '-' }}</template>
      </el-table-column>
      <el-table-column :label="t('common.status')" align="center" prop="status" min-width="90">
        <template #default="scope"><dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" /></template>
      </el-table-column>
      <el-table-column :label="t('common.createTime')" align="center" prop="createTime" :formatter="dateFormatter" width="180" />
      <el-table-column :label="t('table.action')" align="center" width="210" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['aigc:model:proxy:update']">{{ t('aigc.model.actions.edit') }}</el-button>
          <el-button link type="success" :loading="testingId === scope.row.id" @click="handleTest(scope.row.id)" v-hasPermi="['aigc:model:proxy:query']">{{ t('aigc.model.actions.test') }}</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['aigc:model:proxy:delete']">{{ t('aigc.model.actions.delete') }}</el-button>
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
    message.success(t('aigc.model.messages.proxyTestPassed', { duration: durationMillis }))
  } finally {
    testingId.value = undefined
  }
}

onMounted(() => getList())
</script>
