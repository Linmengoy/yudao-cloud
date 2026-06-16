<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="-mb-15px" label-width="68px">
      <el-form-item :label="t('aigc.model.fields.providerCode')" prop="code">
        <el-input v-model="queryParams.code" class="!w-240px" clearable :placeholder="t('aigc.model.placeholders.providerCode')" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item :label="t('aigc.model.fields.providerName')" prop="name">
        <el-input v-model="queryParams.name" class="!w-240px" clearable :placeholder="t('aigc.model.placeholders.providerName')" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item :label="t('common.status')" prop="status">
        <el-select v-model="queryParams.status" class="!w-240px" clearable :placeholder="t('aigc.model.placeholders.status')">
          <el-option v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> {{ t('aigc.model.actions.search') }}</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> {{ t('aigc.model.actions.reset') }}</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['aigc:model:provider:create']"><Icon icon="ep:plus" class="mr-5px" /> {{ t('aigc.model.actions.create') }}</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column :label="t('aigc.model.fields.providerCode')" align="center" prop="code" min-width="120" />
      <el-table-column :label="t('aigc.model.fields.providerName')" align="center" prop="name" min-width="140" />
      <el-table-column :label="t('aigc.model.fields.apiBaseUrl')" align="center" prop="apiBaseUrl" min-width="220" />
      <el-table-column :label="t('aigc.model.fields.authType')" align="center" prop="authType" min-width="120">
        <template #default="scope">{{ getOptionLabel(AIGC_PROVIDER_AUTH_TYPES, scope.row.authType, t) }}</template>
      </el-table-column>
      <el-table-column :label="t('aigc.model.fields.healthStatus')" align="center" prop="healthStatus" min-width="110">
        <template #default="scope">{{ getOptionLabel(AIGC_HEALTH_STATUSES, scope.row.healthStatus, t) }}</template>
      </el-table-column>
      <el-table-column :label="t('aigc.model.fields.proxy')" align="center" min-width="140">
        <template #default="scope">{{ scope.row.proxyEnabled ? scope.row.proxyName || '-' : '-' }}</template>
      </el-table-column>
      <el-table-column :label="t('aigc.model.fields.balance')" align="center" prop="balance" min-width="100" />
      <el-table-column :label="t('common.status')" align="center" prop="status" min-width="90">
        <template #default="scope"><dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" /></template>
      </el-table-column>
      <el-table-column :label="t('common.createTime')" align="center" prop="createTime" :formatter="dateFormatter" width="180" />
      <el-table-column :label="t('table.action')" align="center" width="220" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['aigc:model:provider:update']">{{ t('aigc.model.actions.edit') }}</el-button>
          <el-button link type="success" @click="handleTest(scope.row.id)" v-hasPermi="['aigc:model:provider:query']">{{ t('aigc.model.actions.test') }}</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['aigc:model:provider:delete']">{{ t('aigc.model.actions.delete') }}</el-button>
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
  message.success(t('aigc.model.messages.providerTestPassed'))
}

onMounted(() => getList())
</script>
