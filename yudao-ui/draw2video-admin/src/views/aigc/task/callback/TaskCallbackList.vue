<template>
  <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
    <el-table-column label="回调编号" prop="callbackNo" width="180" />
    <el-table-column label="任务编号" prop="taskNo" width="180" />
    <el-table-column label="渠道编码" prop="providerCode" width="130" />
    <el-table-column label="第三方任务号" prop="externalTaskId" width="180" />
    <el-table-column label="回调类型" prop="callbackType" width="180" />
    <el-table-column label="处理状态" prop="callbackStatus" width="120" />
    <el-table-column label="失败原因" prop="failReason" min-width="160" />
    <el-table-column label="接收时间" prop="receiveTime" :formatter="dateFormatter" width="180" />
    <el-table-column label="处理时间" prop="processTime" :formatter="dateFormatter" width="180" />
    <el-table-column label="操作" width="140" fixed="right">
      <template #default="{ row }">
        <el-button link type="primary" @click="openDetail(row)">详情</el-button>
        <el-button link type="warning" @click="handleReplay(row.id)" v-hasPermi="['aigc:task:callback:replay']">重放</el-button>
      </template>
    </el-table-column>
  </el-table>
  <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />

  <el-dialog v-model="dialogVisible" title="回调详情" width="720px">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="回调内容"><pre>{{ formatJson(current.callbackData) }}</pre></el-descriptions-item>
      <el-descriptions-item label="请求头"><pre>{{ formatJson(current.headers) }}</pre></el-descriptions-item>
      <el-descriptions-item label="处理结果"><pre>{{ formatJson(current.processResult) }}</pre></el-descriptions-item>
      <el-descriptions-item label="签名">{{ current.signature }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { AigcTaskCallbackApi } from '@/api/aigc/task/callback'
import type { AigcTaskCallbackRespVO } from '@/api/aigc/task/types'
import { formatJson } from '../utils'

const props = defineProps<{
  taskId?: number
  providerCode?: string
  externalTaskId?: string
  callbackStatus?: string
}>()
const message = useMessage()
const loading = ref(false)
const list = ref<AigcTaskCallbackRespVO[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const current = ref<AigcTaskCallbackRespVO>({ id: 0 })
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  taskId: props.taskId,
  providerCode: props.providerCode,
  externalTaskId: props.externalTaskId,
  callbackStatus: props.callbackStatus
})

const getList = async () => {
  loading.value = true
  try {
    const data = await AigcTaskCallbackApi.getTaskCallbackPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const openDetail = (row: AigcTaskCallbackRespVO) => {
  current.value = row
  dialogVisible.value = true
}

const handleReplay = async (id: number) => {
  try {
    await message.confirm('确认重放该回调吗？')
    await AigcTaskCallbackApi.replayTaskCallback(id)
    message.success('重放成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
