<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="-mb-15px" label-width="68px">
      <el-form-item label="任务 ID" prop="taskId"><el-input-number v-model="queryParams.taskId" class="!w-180px" :min="1" clearable controls-position="right" /></el-form-item>
      <el-form-item label="用户 ID" prop="userId"><el-input-number v-model="queryParams.userId" class="!w-180px" :min="1" clearable controls-position="right" /></el-form-item>
      <el-form-item label="模型 ID" prop="modelId"><el-input-number v-model="queryParams.modelId" class="!w-180px" :min="1" clearable controls-position="right" /></el-form-item>
      <el-form-item label="渠道 ID" prop="providerId"><el-input-number v-model="queryParams.providerId" class="!w-180px" :min="1" clearable controls-position="right" /></el-form-item>
      <el-form-item label="能力" prop="capability"><el-select v-model="queryParams.capability" class="!w-220px" clearable placeholder="请选择能力"><el-option v-for="item in AIGC_MODEL_CAPABILITIES" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" class="!w-160px" clearable placeholder="请选择状态"><el-option v-for="item in AIGC_USAGE_STATUSES" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>
  <ContentWrap>
    <div class="usage-statistics-header">
      <div class="usage-statistics-title">模型种类使用统计</div>
      <el-button :loading="statisticsLoading" @click="getTypeStatistics">
        <Icon icon="ep:refresh" class="mr-5px" />刷新图表
      </el-button>
    </div>
    <el-row :gutter="16">
      <el-col :xs="24" :lg="10">
        <Echart :height="320" :options="typePieOptions" />
      </el-col>
      <el-col :xs="24" :lg="14">
        <Echart :height="320" :options="typeBarOptions" />
      </el-col>
    </el-row>
    <el-table v-loading="statisticsLoading" :data="typeStatistics" :stripe="true" class="mt-16px">
      <el-table-column label="模型种类" prop="modelType" min-width="100">
        <template #default="scope">{{ getModelTypeLabel(scope.row.modelType) }}</template>
      </el-table-column>
      <el-table-column label="调用次数" align="center" prop="usageCount" min-width="100" />
      <el-table-column label="成功" align="center" prop="successCount" min-width="90" />
      <el-table-column label="失败" align="center" prop="failedCount" min-width="90" />
      <el-table-column label="总 Tokens" align="center" prop="totalTokens" min-width="110" />
      <el-table-column label="销售价" align="center" prop="salePrice" min-width="100" />
      <el-table-column label="成本价" align="center" prop="costPrice" min-width="100" />
      <el-table-column label="平均耗时" align="center" min-width="110">
        <template #default="scope">{{ formatDuration(scope.row.avgDurationMillis) }}</template>
      </el-table-column>
    </el-table>
  </ContentWrap>
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="日志 ID" align="center" prop="id" min-width="90" />
      <el-table-column label="Trace ID" align="center" prop="traceId" min-width="180" />
      <el-table-column label="任务 ID" align="center" prop="taskId" min-width="90" />
      <el-table-column label="用户 ID" align="center" prop="userId" min-width="90" />
      <el-table-column label="模型 ID" align="center" prop="modelId" min-width="90" />
      <el-table-column label="渠道 ID" align="center" prop="providerId" min-width="90" />
      <el-table-column label="能力" align="center" prop="capability" min-width="140"><template #default="scope">{{ getOptionLabel(AIGC_MODEL_CAPABILITIES, scope.row.capability) }}</template></el-table-column>
      <el-table-column label="请求 ID" align="center" prop="requestId" min-width="160" />
      <el-table-column label="外部任务 ID" align="center" prop="externalTaskId" min-width="160" />
      <el-table-column label="总 Tokens" align="center" prop="totalTokens" min-width="100" />
      <el-table-column label="输入 Tokens" align="center" prop="inputTokens" min-width="110" />
      <el-table-column label="输出 Tokens" align="center" prop="outputTokens" min-width="110" />
      <el-table-column label="成本价" align="center" prop="costPrice" min-width="100" />
      <el-table-column label="销售价" align="center" prop="salePrice" min-width="100" />
      <el-table-column label="币种" align="center" prop="currencyType" min-width="90" />
      <el-table-column label="状态" align="center" prop="status" min-width="90"><template #default="scope"><el-tag :type="scope.row.status === 0 ? 'success' : 'danger'">{{ getOptionLabel(AIGC_USAGE_STATUSES, scope.row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="耗时(ms)" align="center" prop="durationMillis" min-width="100" />
      <el-table-column label="错误码" align="center" prop="errorCode" min-width="120" />
      <el-table-column label="错误信息" align="center" prop="errorMessage" min-width="180" />
      <el-table-column label="创建时间" align="center" prop="createTime" min-width="160" :formatter="dateFormatter" />
      <el-table-column label="操作" align="center" width="80" fixed="right"><template #default="scope"><el-button link type="primary" @click="openDetail(scope.row.id)">详情</el-button></template></el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>
  <Dialog title="用量日志详情" v-model="detailVisible" width="760px">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="日志 ID">{{ detailData.id || '-' }}</el-descriptions-item>
      <el-descriptions-item label="Trace ID">{{ detailData.traceId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="任务 ID">{{ detailData.taskId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="用户 ID">{{ detailData.userId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="模型 ID">{{ detailData.modelId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="渠道 ID">{{ detailData.providerId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="能力">{{ getOptionLabel(AIGC_MODEL_CAPABILITIES, detailData.capability) }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ getOptionLabel(AIGC_USAGE_STATUSES, detailData.status) }}</el-descriptions-item>
      <el-descriptions-item label="请求 ID">{{ detailData.requestId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="外部任务 ID">{{ detailData.externalTaskId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="Prompt Tokens">{{ detailData.promptTokens || 0 }}</el-descriptions-item>
      <el-descriptions-item label="Completion Tokens">{{ detailData.completionTokens || 0 }}</el-descriptions-item>
      <el-descriptions-item label="总 Tokens">{{ detailData.totalTokens || 0 }}</el-descriptions-item>
      <el-descriptions-item label="耗时(ms)">{{ detailData.durationMillis || 0 }}</el-descriptions-item>
      <el-descriptions-item label="成本价">{{ detailData.costPrice || 0 }}</el-descriptions-item>
      <el-descriptions-item label="销售价">{{ detailData.salePrice || 0 }}</el-descriptions-item>
      <el-descriptions-item label="错误码">{{ detailData.errorCode || '-' }}</el-descriptions-item>
      <el-descriptions-item label="错误信息">{{ detailData.errorMessage || '-' }}</el-descriptions-item>
    </el-descriptions>
  </Dialog>
</template>
<script setup lang="ts">
import type { EChartsOption } from 'echarts'
import { dateFormatter } from '@/utils/formatTime'
import { AigcModelUsageApi, type AigcModelUsageLogPageReqVO } from '@/api/aigc/model/usage'
import { Echart } from '@/components/Echart'
import type {
  AigcModelUsageLogRespVO,
  AigcModelUsageTypeStatisticsRespVO
} from '@/api/aigc/model/types'
import { AIGC_MODEL_CAPABILITIES, AIGC_MODEL_TYPES, AIGC_USAGE_STATUSES, getOptionLabel } from '../constants'

defineOptions({ name: 'AigcModelUsage' })

const loading = ref(true)
const statisticsLoading = ref(false)
const list = ref<AigcModelUsageLogRespVO[]>([])
const typeStatistics = ref<AigcModelUsageTypeStatisticsRespVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive<AigcModelUsageLogPageReqVO>({ pageNo: 1, pageSize: 10, taskId: undefined, userId: undefined, modelId: undefined, providerId: undefined, capability: undefined, status: undefined })
const detailVisible = ref(false)
const detailData = ref<AigcModelUsageLogRespVO>({})

const getModelTypeLabel = (type?: number) => {
  return AIGC_MODEL_TYPES.find((item) => item.value === type)?.label || '未知'
}

const formatDuration = (millis?: number) => {
  if (!millis) {
    return '0s'
  }
  return `${(millis / 1000).toFixed(1)}s`
}

const typePieOptions = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [
    {
      name: '调用次数',
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '44%'],
      data: typeStatistics.value.map((item) => ({
        name: getModelTypeLabel(item.modelType),
        value: item.usageCount || 0
      })),
      label: { formatter: '{b}: {c}' }
    }
  ]
}))

const typeBarOptions = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0 },
  grid: { left: 40, right: 20, top: 40, bottom: 32 },
  xAxis: {
    type: 'category',
    data: typeStatistics.value.map((item) => getModelTypeLabel(item.modelType))
  },
  yAxis: { type: 'value' },
  series: [
    {
      name: '成功',
      type: 'bar',
      stack: 'usage',
      data: typeStatistics.value.map((item) => item.successCount || 0)
    },
    {
      name: '失败',
      type: 'bar',
      stack: 'usage',
      data: typeStatistics.value.map((item) => item.failedCount || 0)
    }
  ]
}))

const getList = async () => {
  loading.value = true
  try {
    const data = await AigcModelUsageApi.getUsagePage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}
const getTypeStatistics = async () => {
  statisticsLoading.value = true
  try {
    typeStatistics.value = await AigcModelUsageApi.getUsageTypeStatistics(queryParams)
  } finally {
    statisticsLoading.value = false
  }
}
const handleQuery = () => { queryParams.pageNo = 1; Promise.all([getList(), getTypeStatistics()]) }
const resetQuery = () => { queryFormRef.value.resetFields(); handleQuery() }
const openDetail = async (id: number) => {
  detailData.value = await AigcModelUsageApi.getUsage(id)
  detailVisible.value = true
}
onMounted(() => {
  getList()
  getTypeStatistics()
})
</script>
<style scoped>
.usage-statistics-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.usage-statistics-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
</style>
