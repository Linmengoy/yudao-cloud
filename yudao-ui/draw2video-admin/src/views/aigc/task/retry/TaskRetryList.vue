<template>
  <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
    <el-table-column label="重试编号" prop="retryNo" width="180" />
    <el-table-column label="任务编号" prop="taskNo" width="180" />
    <el-table-column label="重试类型" prop="retryType" width="120" />
    <el-table-column label="状态" prop="retryStatus" width="120" />
    <el-table-column label="第几次" prop="retryTimes" width="90" />
    <el-table-column label="下次重试时间" prop="nextRetryTime" :formatter="dateFormatter" width="180" />
    <el-table-column label="开始时间" prop="startTime" :formatter="dateFormatter" width="180" />
    <el-table-column label="结束时间" prop="endTime" :formatter="dateFormatter" width="180" />
    <el-table-column label="失败原因" prop="failReason" min-width="160" />
    <el-table-column label="操作人" prop="operatorId" width="100" />
    <el-table-column label="操作" width="110" fixed="right">
      <template #default="{ row }">
        <el-button link type="warning" :disabled="row.retryStatus !== 'WAITING'" @click="handleCancel(row.id)" v-hasPermi="['aigc:task:retry:update']">取消</el-button>
      </template>
    </el-table-column>
  </el-table>
  <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { AigcTaskRetryApi } from '@/api/aigc/task/retry'
import type { AigcTaskRetryRespVO } from '@/api/aigc/task/types'

const props = defineProps<{ taskId?: number; taskNo?: string; retryStatus?: string }>()
const message = useMessage()
const loading = ref(false)
const list = ref<AigcTaskRetryRespVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  taskId: props.taskId,
  taskNo: props.taskNo,
  retryStatus: props.retryStatus
})

const getList = async () => {
  loading.value = true
  try {
    const data = await AigcTaskRetryApi.getTaskRetryPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleCancel = async (id: number) => {
  try {
    await message.confirm('确认取消该重试记录吗？')
    await AigcTaskRetryApi.cancelTaskRetry(id)
    message.success('取消成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
