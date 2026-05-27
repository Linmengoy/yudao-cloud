<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :inline="true" :model="queryParams" class="-mb-15px" label-width="80px">
      <el-form-item label="用户编号" prop="userId">
        <el-input v-model="queryParams.userId" class="!w-220px" clearable placeholder="请输入用户编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="任务编号" prop="taskNo">
        <el-input v-model="queryParams.taskNo" class="!w-220px" clearable placeholder="请输入任务编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="流水类型" prop="recordType">
        <el-select v-model="queryParams.recordType" class="!w-180px" clearable placeholder="请选择流水类型">
          <el-option v-for="item in recordTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon class="mr-5px" icon="ep:search" />搜索</el-button>
        <el-button @click="resetQuery"><Icon class="mr-5px" icon="ep:refresh" />重置</el-button>
        <el-button v-hasPermi="['aigc:billing:record:export']" :loading="exportLoading" plain type="success" @click="handleExport"><Icon class="mr-5px" icon="ep:download" />导出</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" :stripe="true">
      <el-table-column align="center" label="流水号" prop="recordNo" min-width="180" />
      <el-table-column align="center" label="用户编号" prop="userId" width="120" />
      <el-table-column align="center" label="流水类型" width="120">
        <template #default="scope">{{ mapText(billingRecordTypeMap, scope.row.recordType) }}</template>
      </el-table-column>
      <el-table-column align="center" label="变动积分" width="130">
        <template #default="scope">{{ formatPoints(scope.row.amount) }}</template>
      </el-table-column>
      <el-table-column align="center" label="可用余额" width="130">
        <template #default="scope">{{ formatPoints(scope.row.balanceAfter) }}</template>
      </el-table-column>
      <el-table-column align="center" label="冻结余额" width="130">
        <template #default="scope">{{ formatPoints(scope.row.frozenBalanceAfter) }}</template>
      </el-table-column>
      <el-table-column align="center" label="任务编号" prop="taskNo" min-width="160" />
      <el-table-column align="center" label="模型编号" prop="modelId" width="120" />
      <el-table-column :formatter="dateFormatter" align="center" label="创建时间" prop="createTime" width="180" />
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import { AigcBillingRecordApi, AigcBillingRecordVO } from '@/api/aigc/billing/record'
import { billingRecordTypeMap, formatPoints, mapText } from '../utils'

defineOptions({ name: 'AigcBillingRecord' })

const message = useMessage()
const loading = ref(true)
const exportLoading = ref(false)
const list = ref<AigcBillingRecordVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive({ pageNo: 1, pageSize: 10, userId: undefined, taskNo: undefined, recordType: undefined })
const recordTypeOptions = Object.entries(billingRecordTypeMap).filter(([key]) => Number.isNaN(Number(key))).map(([value, label]) => ({ value, label }))

const getList = async () => {
  loading.value = true
  try {
    const data = await AigcBillingRecordApi.getRecordPage(queryParams)
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

const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await AigcBillingRecordApi.exportRecord(queryParams)
    download.excel(data, 'AIGC计费流水.xls')
  } finally {
    exportLoading.value = false
  }
}

onMounted(() => getList())
</script>
