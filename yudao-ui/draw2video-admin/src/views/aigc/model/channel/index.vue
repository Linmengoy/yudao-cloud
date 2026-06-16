<template>
  <ContentWrap>
    <el-form :model="queryParams" ref="queryFormRef" inline>
      <el-form-item :label="t('aigc.model.fields.businessModel')" prop="modelId"><el-select v-model="queryParams.modelId" class="!w-240px" clearable filterable :placeholder="t('aigc.model.placeholders.businessModel')"><el-option v-for="item in modelList" :key="item.id" :label="item.name" :value="Number(item.id)" /></el-select></el-form-item>
      <el-form-item :label="t('aigc.model.fields.provider')" prop="providerId"><el-select v-model="queryParams.providerId" class="!w-220px" clearable filterable :placeholder="t('aigc.model.placeholders.provider')"><el-option v-for="item in providerList" :key="item.id" :label="item.name" :value="Number(item.id)" /></el-select></el-form-item>
      <el-form-item :label="t('common.status')" prop="status"><el-select v-model="queryParams.status" class="!w-160px" clearable :placeholder="t('aigc.model.placeholders.status')"><el-option v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value" :label="dict.label" :value="dict.value" /></el-select></el-form-item>
      <el-form-item><el-button @click="handleQuery"><Icon icon="ep:search" />{{ t('aigc.model.actions.search') }}</el-button><el-button @click="resetQuery"><Icon icon="ep:refresh" />{{ t('aigc.model.actions.reset') }}</el-button><el-button type="primary" @click="openForm('create')"><Icon icon="ep:plus" />{{ t('aigc.model.actions.create') }}</el-button></el-form-item>
    </el-form>
  </ContentWrap>
  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column :label="t('aigc.model.fields.businessModel')" align="center" prop="modelId" min-width="160"><template #default="scope">{{ getModelName(scope.row.modelId) }}</template></el-table-column>
      <el-table-column :label="t('aigc.model.fields.provider')" align="center" prop="providerId" min-width="140"><template #default="scope">{{ getProviderName(scope.row.providerId) }}</template></el-table-column>
      <el-table-column :label="t('aigc.model.fields.upstreamModel')" align="center" prop="providerModel" min-width="180" />
      <el-table-column :label="t('aigc.model.fields.costPrice')" align="center" prop="costPrice" width="110" />
      <el-table-column :label="t('aigc.model.fields.weight')" align="center" prop="weight" width="90" />
      <el-table-column :label="t('aigc.model.fields.priority')" align="center" prop="priority" width="90" />
      <el-table-column :label="t('common.status')" align="center" prop="status" width="100"><template #default="scope"><dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" /></template></el-table-column>
      <el-table-column :label="t('table.action')" align="center" width="180" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)">{{ t('aigc.model.actions.edit') }}</el-button>
          <el-button link type="primary" @click="openForm('clone', scope.row.id)">{{ t('aigc.model.actions.clone') }}</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)">{{ t('aigc.model.actions.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>
  <ChannelForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { AigcModelApi } from '@/api/aigc/model/model'
import { AigcModelChannelApi, type AigcModelChannelPageReqVO } from '@/api/aigc/model/channel'
import { AigcModelProviderApi } from '@/api/aigc/model/provider'
import type { AigcModelChannelRespVO, AigcModelProviderRespVO, AigcModelRespVO } from '@/api/aigc/model/types'
import ChannelForm from './ChannelForm.vue'

defineOptions({ name: 'AigcModelChannel' })

const message = useMessage()
const { t } = useI18n()
const loading = ref(false)
const list = ref<AigcModelChannelRespVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const formRef = ref()
const modelList = ref<AigcModelRespVO[]>([])
const providerList = ref<AigcModelProviderRespVO[]>([])
const queryParams = reactive<AigcModelChannelPageReqVO>({ pageNo: 1, pageSize: 10, modelId: undefined, providerId: undefined, status: undefined })

const getList = async () => {
  loading.value = true
  try {
    const data = await AigcModelChannelApi.getChannelPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}
const handleQuery = () => { queryParams.pageNo = 1; getList() }
const resetQuery = () => { queryFormRef.value?.resetFields(); handleQuery() }
const openForm = (type: string, id?: number) => formRef.value.open(type, id, queryParams.modelId)
const handleDelete = async (id?: number) => {
  if (!id) return
  await message.delConfirm()
  await AigcModelChannelApi.deleteChannel(id)
  message.success(t('common.delSuccess'))
  await getList()
}
const getModelName = (id?: number) => modelList.value.find((item) => item.id === id)?.name || `Model ${id}`
const getProviderName = (id?: number) => providerList.value.find((item) => item.id === id)?.name || `Provider ${id}`

onMounted(async () => {
  const [modelPage, providerPage] = await Promise.all([
    AigcModelApi.getModelPage({ pageNo: 1, pageSize: 100 }),
    AigcModelProviderApi.getProviderPage({ pageNo: 1, pageSize: 100 })
  ])
  modelList.value = modelPage.list || []
  providerList.value = providerPage.list || []
  await getList()
})
</script>
