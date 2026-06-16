<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" label-width="120px" class="-mb-15px">
      <el-form-item label="User ID" prop="userId">
        <el-input-number v-model="queryParams.userId" :min="1" controls-position="right" class="!w-200px" clearable />
      </el-form-item>
      <el-form-item label="Task ID" prop="taskId">
        <el-input-number v-model="queryParams.taskId" :min="1" controls-position="right" class="!w-200px" clearable />
      </el-form-item>
      <el-form-item label="Generate No" prop="generateNo">
        <el-input v-model="queryParams.generateNo" clearable class="!w-240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="Provider Task" prop="providerTaskId">
        <el-input v-model="queryParams.providerTaskId" clearable class="!w-240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="Fail Reason" prop="failReason">
        <el-input v-model="queryParams.failReason" clearable class="!w-220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="Create Time" prop="createTime">
        <el-date-picker v-model="queryParams.createTime" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss" class="!w-360px" />
      </el-form-item>
      <el-form-item label="Mode" prop="generateMode">
        <el-select v-model="queryParams.generateMode" clearable class="!w-180px">
          <el-option v-for="item in generateModeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="Model ID" prop="modelId">
        <el-input-number v-model="queryParams.modelId" :min="1" controls-position="right" class="!w-200px" clearable />
      </el-form-item>
      <el-form-item label="Provider" prop="providerCode">
        <el-input v-model="queryParams.providerCode" clearable class="!w-180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="Status" prop="status">
        <el-select v-model="queryParams.status" clearable class="!w-180px">
          <el-option v-for="item in generateStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="Errors Only" prop="hasError">
        <el-switch v-model="queryParams.hasError" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />Search</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />Reset</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="Generate No" prop="generateNo" width="190" fixed="left" />
      <el-table-column label="Task ID" prop="taskId" width="110" />
      <el-table-column label="User ID" prop="userId" width="110" />
      <el-table-column label="Mode" prop="generateMode" width="130">
        <template #default="{ row }">{{ modeLabel(row.generateMode) }}</template>
      </el-table-column>
      <el-table-column label="Model" prop="modelCode" width="140" />
      <el-table-column label="Provider" prop="providerCode" width="130" />
      <el-table-column label="Provider Task" prop="providerTaskId" width="180" />
      <el-table-column label="Provider Status" prop="providerStatus" width="140" />
      <el-table-column label="Status" prop="status" width="120">
        <template #default="{ row }"><el-tag>{{ statusLabel(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="Sale" prop="priceAmount" width="110" />
      <el-table-column label="Cost" prop="costAmount" width="110" />
      <el-table-column label="Fail Code" prop="failReason" width="140" />
      <el-table-column label="Fail Message" prop="failMessage" min-width="220" />
      <el-table-column label="Created" prop="createTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="Submitted" prop="submitTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="Finished" prop="finishTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="Actions" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)" v-hasPermi="['aigc:gen:query']">Detail</el-button>
          <el-button link type="warning" :disabled="!row.taskId" @click="handleSync(row.taskId)" v-hasPermi="['aigc:gen:update']">Sync</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { AigcGenerateRecordApi } from '@/api/aigc/gen/record'
import type { AigcGenerateRecordPageReqVO, AigcGenerateRecordRespVO } from '@/api/aigc/gen/types'
import { generateModeOptions, generateStatusOptions, modeLabel, statusLabel } from '../utils'

defineOptions({ name: 'AigcGenerateRecord' })

const router = useRouter()
const message = useMessage()
const loading = ref(true)
const list = ref<AigcGenerateRecordRespVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive<AigcGenerateRecordPageReqVO>({ pageNo: 1, pageSize: 10 })

const getList = async () => {
  loading.value = true
  try {
    const data = await AigcGenerateRecordApi.getGenerateRecordPage(queryParams)
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

const openDetail = (id: number) => {
  router.push({ path: '/aigc/gen/record/detail/' + id })
}

const handleSync = async (taskId?: number) => {
  if (!taskId) return
  try {
    await message.confirm('Sync this provider task?')
    await AigcGenerateRecordApi.syncGenerateTask(taskId)
    message.success('Synced')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
