<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="-mb-15px" label-width="68px">
      <el-form-item :label="t('aigc.model.fields.modelName')" prop="name"><el-input v-model="queryParams.name" class="!w-240px" clearable :placeholder="t('aigc.model.placeholders.modelName')" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item :label="t('aigc.model.fields.modelCode')" prop="code"><el-input v-model="queryParams.code" class="!w-240px" clearable :placeholder="t('aigc.model.placeholders.modelCode')" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item :label="t('aigc.model.fields.modelType')" prop="type">
        <el-select v-model="queryParams.type" class="!w-240px" clearable :placeholder="t('aigc.model.placeholders.modelType')"><el-option v-for="item in AIGC_MODEL_TYPES" :key="item.value" :label="getOptionLabel([item], item.value, t)" :value="item.value" /></el-select>
      </el-form-item>
      <el-form-item :label="t('common.status')" prop="status">
        <el-select v-model="queryParams.status" class="!w-240px" clearable :placeholder="t('aigc.model.placeholders.status')"><el-option v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value" :label="dict.label" :value="dict.value" /></el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> {{ t('aigc.model.actions.search') }}</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> {{ t('aigc.model.actions.reset') }}</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['aigc:model:create']"><Icon icon="ep:plus" class="mr-5px" /> {{ t('aigc.model.actions.create') }}</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column :label="t('aigc.model.fields.modelCode')" align="center" prop="code" min-width="120" />
      <el-table-column :label="t('aigc.model.fields.modelName')" align="center" prop="name" min-width="140" />
      <el-table-column :label="t('aigc.model.fields.nameEn')" align="center" prop="nameEn" min-width="160" />
      <el-table-column :label="t('aigc.model.fields.modelIdentifier')" align="center" prop="model" min-width="180" />
      <el-table-column :label="t('aigc.model.fields.type')" align="center" prop="type" min-width="90"><template #default="scope">{{ getOptionLabel(AIGC_MODEL_TYPES, scope.row.type, t) }}</template></el-table-column>
      <el-table-column :label="t('aigc.model.fields.provider')" align="center" prop="providerName" min-width="140" />
      <el-table-column :label="t('aigc.model.fields.capability')" align="center" prop="capabilities" min-width="220"><template #default="scope">{{ (scope.row.capabilities || []).join('、') || '-' }}</template></el-table-column>
      <el-table-column :label="t('aigc.model.fields.publicVisible')" align="center" prop="publicVisible" min-width="110"><template #default="scope"><el-tag :type="scope.row.publicVisible ? 'success' : 'info'">{{ scope.row.publicVisible ? t('aigc.model.fields.show') : t('aigc.model.fields.hide') }}</el-tag></template></el-table-column>
      <el-table-column :label="t('aigc.model.fields.default')" align="center" prop="defaultModel" min-width="80"><template #default="scope"><el-tag v-if="scope.row.defaultModel" type="warning">{{ t('aigc.model.fields.default') }}</el-tag><span v-else>-</span></template></el-table-column>
      <el-table-column :label="t('common.status')" align="center" prop="status" min-width="90"><template #default="scope"><dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" /></template></el-table-column>
      <el-table-column :label="t('table.action')" align="center" width="180" fixed="right"><template #default="scope"><el-button link type="primary" @click="openDetail(scope.row.id)" v-hasPermi="['aigc:model:update']">{{ t('aigc.model.actions.edit') }}</el-button><el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['aigc:model:delete']">{{ t('aigc.model.actions.delete') }}</el-button></template></el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>
  <ModelForm ref="formRef" @success="getList" />
</template>
<script setup lang="ts">
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { AigcModelApi } from '@/api/aigc/model/model'
import type { AigcModelRespVO } from '@/api/aigc/model/types'
import ModelForm from './ModelForm.vue'
import { AIGC_MODEL_TYPES, getOptionLabel } from '../constants'

defineOptions({ name: 'AigcModel' })

const message = useMessage()
const { t } = useI18n()
const router = useRouter()
const loading = ref(true)
const list = ref<AigcModelRespVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive({ pageNo: 1, pageSize: 10, name: undefined, code: undefined, type: undefined, status: undefined })
const getList = async () => {
  loading.value = true
  try {
    const data = await AigcModelApi.getModelPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}
const handleQuery = () => { queryParams.pageNo = 1; getList() }
const resetQuery = () => { queryFormRef.value.resetFields(); handleQuery() }
const formRef = ref()
const openForm = (type: string, id?: number) => formRef.value.open(type, id)
const openDetail = (id: number) => router.push(`/aigc/model/detail/${id}`)
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await AigcModelApi.deleteModel(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}
onMounted(() => getList())
</script>
