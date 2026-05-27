<template>
  <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
    <el-table-column label="任务编号" prop="taskNo" width="180" />
    <el-table-column label="原状态" prop="fromStatus" width="130">
      <template #default="{ row }">{{ statusLabel(row.fromStatus) }}</template>
    </el-table-column>
    <el-table-column label="新状态" prop="toStatus" width="130">
      <template #default="{ row }">{{ statusLabel(row.toStatus) }}</template>
    </el-table-column>
    <el-table-column label="动作" prop="action" width="130" />
    <el-table-column label="消息" prop="message" min-width="180" />
    <el-table-column label="操作类型" prop="operatorType" width="120" />
    <el-table-column label="操作人" prop="operatorId" width="100" />
    <el-table-column label="扩展信息" prop="extraInfo" min-width="180" />
    <el-table-column label="创建时间" prop="createTime" :formatter="dateFormatter" width="180" />
  </el-table>
  <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { AigcTaskLogApi } from '@/api/aigc/task/log'
import type { AigcTaskLogRespVO } from '@/api/aigc/task/types'
import { statusLabel } from '../utils'

const props = defineProps<{ taskId?: number; taskNo?: string }>()
const loading = ref(false)
const list = ref<AigcTaskLogRespVO[]>([])
const total = ref(0)
const queryParams = reactive({ pageNo: 1, pageSize: 10, taskId: props.taskId, taskNo: props.taskNo })

const getList = async () => {
  loading.value = true
  try {
    const data = await AigcTaskLogApi.getTaskLogPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

onMounted(() => getList())
</script>
