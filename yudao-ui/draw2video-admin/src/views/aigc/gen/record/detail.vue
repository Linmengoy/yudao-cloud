<template>
  <ContentWrap>
    <el-button @click="router.back()"><Icon icon="ep:arrow-left" class="mr-5px" />Back</el-button>
    <el-button class="ml-10px" type="warning" :disabled="!record.taskId" @click="handleSync" v-hasPermi="['aigc:gen:update']">Sync Provider Task</el-button>
    <el-descriptions v-loading="loading" class="mt-20px" :column="3" border>
      <el-descriptions-item label="Generate No">{{ record.generateNo }}</el-descriptions-item>
      <el-descriptions-item label="Task ID">{{ record.taskId }}</el-descriptions-item>
      <el-descriptions-item label="User ID">{{ record.userId }}</el-descriptions-item>
      <el-descriptions-item label="Client Request">{{ record.clientRequestId }}</el-descriptions-item>
      <el-descriptions-item label="Type">{{ typeLabel(record.generateType) }}</el-descriptions-item>
      <el-descriptions-item label="Mode">{{ modeLabel(record.generateMode) }}</el-descriptions-item>
      <el-descriptions-item label="Status">{{ statusLabel(record.status) }}</el-descriptions-item>
      <el-descriptions-item label="Model ID">{{ record.modelId }}</el-descriptions-item>
      <el-descriptions-item label="Model Code">{{ record.modelCode }}</el-descriptions-item>
      <el-descriptions-item label="Provider ID">{{ record.providerId }}</el-descriptions-item>
      <el-descriptions-item label="Provider Code">{{ record.providerCode }}</el-descriptions-item>
      <el-descriptions-item label="Provider Task">{{ record.providerTaskId }}</el-descriptions-item>
      <el-descriptions-item label="Provider Status">{{ record.providerStatus }}</el-descriptions-item>
      <el-descriptions-item label="Freeze ID">{{ record.freezeId }}</el-descriptions-item>
      <el-descriptions-item label="Sale Price">{{ record.priceAmount }}</el-descriptions-item>
      <el-descriptions-item label="Cost Price">{{ record.costAmount }}</el-descriptions-item>
      <el-descriptions-item label="Submitted">{{ record.submitTime }}</el-descriptions-item>
      <el-descriptions-item label="Callback">{{ record.callbackTime }}</el-descriptions-item>
      <el-descriptions-item label="Finished">{{ record.finishTime }}</el-descriptions-item>
      <el-descriptions-item label="Fail Code">{{ record.failReason }}</el-descriptions-item>
      <el-descriptions-item label="Fail Message">{{ record.failMessage }}</el-descriptions-item>
    </el-descriptions>
  </ContentWrap>

  <ContentWrap>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="Prompt" name="prompt"><pre class="whitespace-pre-wrap">{{ record.prompt || '-' }}</pre></el-tab-pane>
      <el-tab-pane label="Input Params" name="input"><pre>{{ formatJson(record.inputParams) }}</pre></el-tab-pane>
      <el-tab-pane label="Output Text" name="outputText"><pre class="whitespace-pre-wrap">{{ record.outputText || '-' }}</pre></el-tab-pane>
      <el-tab-pane label="Output Data" name="outputData"><pre>{{ formatJson(record.outputData) }}</pre></el-tab-pane>
      <el-tab-pane label="Result URLs" name="urls"><pre>{{ formatJson(record.outputUrls) }}</pre></el-tab-pane>
      <el-tab-pane label="Asset IDs" name="assets"><pre>{{ formatJson(record.assetIds) }}</pre></el-tab-pane>
      <el-tab-pane label="Provider Logs" name="providerLogs">
        <el-table v-loading="providerLogLoading" :data="providerLogs" :stripe="true" :show-overflow-tooltip="true">
          <el-table-column label="Action" prop="apiAction" width="130" />
          <el-table-column label="Provider" prop="providerCode" width="120" />
          <el-table-column label="Model" prop="modelCode" width="150" />
          <el-table-column label="HTTP" prop="httpStatus" width="90" />
          <el-table-column label="Success" prop="success" width="100">
            <template #default="{ row }"><el-tag :type="row.success ? 'success' : 'danger'">{{ successLabel(row.success) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="Error Code" prop="errorCode" width="130" />
          <el-table-column label="Error Message" prop="errorMessage" min-width="220" />
          <el-table-column label="Duration(ms)" prop="durationMs" width="120" />
          <el-table-column label="Created" prop="createTime" :formatter="dateFormatter" width="180" />
          <el-table-column label="Actions" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openProviderLog(row)">Detail</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!providerLogLoading && providerLogs.length === 0" description="No provider logs" />
        <Pagination :total="providerLogTotal" v-model:page="providerLogQuery.pageNo" v-model:limit="providerLogQuery.pageSize" @pagination="getProviderLogs" />
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>

  <el-drawer v-model="drawerVisible" title="Provider Log Detail" size="60%">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="Log ID">{{ currentLog?.id }}</el-descriptions-item>
      <el-descriptions-item label="Result">{{ successLabel(currentLog?.success) }}</el-descriptions-item>
      <el-descriptions-item label="Error Code">{{ currentLog?.errorCode }}</el-descriptions-item>
      <el-descriptions-item label="Error Message">{{ currentLog?.errorMessage }}</el-descriptions-item>
    </el-descriptions>
    <el-tabs class="mt-20px">
      <el-tab-pane label="Request Summary"><pre>{{ formatJson(currentLog?.requestSummary) }}</pre></el-tab-pane>
      <el-tab-pane label="Response Summary"><pre>{{ formatJson(currentLog?.responseSummary) }}</pre></el-tab-pane>
    </el-tabs>
  </el-drawer>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { AigcGenerateRecordApi } from '@/api/aigc/gen/record'
import { AigcGenerateProviderLogApi } from '@/api/aigc/gen/provider-log'
import type { AigcGenerateProviderLogPageReqVO, AigcGenerateProviderLogRespVO, AigcGenerateRecordRespVO } from '@/api/aigc/gen/types'
import { formatJson, modeLabel, statusLabel, successLabel, typeLabel } from '../utils'

defineOptions({ name: 'AigcGenerateRecordDetail' })

const route = useRoute()
const router = useRouter()
const message = useMessage()
const loading = ref(true)
const activeTab = ref('prompt')
const record = ref<AigcGenerateRecordRespVO>({ id: Number(route.params.id) })
const providerLogLoading = ref(false)
const providerLogs = ref<AigcGenerateProviderLogRespVO[]>([])
const providerLogTotal = ref(0)
const providerLogQuery = reactive<AigcGenerateProviderLogPageReqVO>({ pageNo: 1, pageSize: 10, recordId: Number(route.params.id) })
const drawerVisible = ref(false)
const currentLog = ref<AigcGenerateProviderLogRespVO>()

const getDetail = async () => {
  loading.value = true
  try {
    record.value = await AigcGenerateRecordApi.getGenerateRecord(Number(route.params.id))
    providerLogQuery.taskId = record.value.taskId
    await getProviderLogs()
  } finally {
    loading.value = false
  }
}

const getProviderLogs = async () => {
  providerLogLoading.value = true
  try {
    const data = await AigcGenerateProviderLogApi.getGenerateProviderLogPage(providerLogQuery)
    providerLogs.value = data.list
    providerLogTotal.value = data.total
  } finally {
    providerLogLoading.value = false
  }
}

const openProviderLog = (row: AigcGenerateProviderLogRespVO) => {
  currentLog.value = row
  drawerVisible.value = true
}

const handleSync = async () => {
  if (!record.value.taskId) return
  try {
    await message.confirm('Sync this provider task?')
    await AigcGenerateRecordApi.syncGenerateTask(record.value.taskId)
    message.success('Synced')
    await getDetail()
  } catch {}
}

onMounted(() => getDetail())
</script>
