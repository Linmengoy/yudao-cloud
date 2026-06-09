<template>
  <ContentWrap>
    <el-button :loading="loading" @click="getStatistics"><Icon icon="ep:refresh" class="mr-5px" />刷新</el-button>
    <el-row :gutter="16" class="mt-20px">
      <el-col v-for="item in cards" :key="item.label" :span="6" class="mb-16px">
        <el-card shadow="never">
          <div class="text-13px color-#909399">{{ item.label }}</div>
          <div class="mt-8px text-26px font-bold">{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>
  </ContentWrap>
</template>

<script setup lang="ts">
import { AigcTaskApi } from '@/api/aigc/task'
import type { AigcTaskStatisticsRespVO } from '@/api/aigc/task/types'

defineOptions({ name: 'AigcTaskStatistics' })

const loading = ref(false)
const statistics = ref<AigcTaskStatisticsRespVO>({})
const cards = computed(() => [
  { label: '总任务数', value: statistics.value.totalCount || 0 },
  { label: '成功数', value: statistics.value.successCount || 0 },
  { label: '失败数', value: statistics.value.failedCount || 0 },
  { label: '退款中', value: statistics.value.refundingCount || 0 },
  { label: '积压数', value: statistics.value.backlogCount || 0 },
  { label: '超时数', value: statistics.value.timeoutCount || 0 },
  { label: '重试任务', value: statistics.value.retryTaskCount || 0 },
  { label: '成功率', value: formatRate(statistics.value.successRate) },
  { label: '失败率', value: formatRate(statistics.value.failedRate) },
  { label: '平均耗时', value: formatDuration(statistics.value.avgDurationMillis) },
  { label: 'P95 耗时', value: formatDuration(statistics.value.p95DurationMillis) }
])

const formatRate = (rate?: number) => `${(((rate || 0) * 100)).toFixed(1)}%`

const formatDuration = (millis?: number) => {
  if (!millis) {
    return '0s'
  }
  return `${(millis / 1000).toFixed(1)}s`
}

const getStatistics = async () => {
  loading.value = true
  try {
    statistics.value = await AigcTaskApi.getTaskStatistics()
  } finally {
    loading.value = false
  }
}

onMounted(() => getStatistics())
</script>
