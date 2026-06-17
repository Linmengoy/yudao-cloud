<template>
  <ContentWrap>
    <el-button @click="router.back()"><Icon icon="ep:arrow-left" class="mr-5px" />返回</el-button>
    <el-descriptions v-loading="loading" class="mt-20px" :column="3" border>
      <el-descriptions-item label="任务编号">{{ task.taskNo }}</el-descriptions-item>
      <el-descriptions-item label="用户 ID">{{ task.userId }}</el-descriptions-item>
      <el-descriptions-item label="任务类型">{{ typeLabel(task.taskType) }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ statusLabel(task.status) }}</el-descriptions-item>
      <el-descriptions-item label="进度">{{ task.progress || 0 }}%</el-descriptions-item>
      <el-descriptions-item label="模型 ID">{{ task.modelId }}</el-descriptions-item>
      <el-descriptions-item label="供应商 ID">{{ task.providerId }}</el-descriptions-item>
      <el-descriptions-item label="第三方任务号">{{ task.externalTaskId }}</el-descriptions-item>
      <el-descriptions-item label="冻结记录 ID">{{ task.freezeId }}</el-descriptions-item>
      <el-descriptions-item label="销售价">{{ task.salePrice }}</el-descriptions-item>
      <el-descriptions-item label="成本价">{{ task.costPrice }}</el-descriptions-item>
      <el-descriptions-item label="币种">{{ task.currencyType }}</el-descriptions-item>
      <el-descriptions-item label="输出资产 ID">{{ task.outputAssetId }}</el-descriptions-item>
      <el-descriptions-item label="输出资产类型">{{ task.outputAssetType }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ task.createTime }}</el-descriptions-item>
      <el-descriptions-item label="完成时间">{{ task.finishTime }}</el-descriptions-item>
      <el-descriptions-item label="失败码">{{ task.failCode }}</el-descriptions-item>
      <el-descriptions-item label="失败原因">{{ task.failReason }}</el-descriptions-item>
    </el-descriptions>
  </ContentWrap>

  <ContentWrap>
    <el-tabs>
      <el-tab-pane label="详细信息">
        <pre class="whitespace-pre-wrap">{{ displayOutput }}</pre>
      </el-tab-pane>
      <el-tab-pane label="状态日志"><TaskLogList :task-id="task.id" /></el-tab-pane>
      <el-tab-pane label="回调记录"><TaskCallbackList :task-id="task.id" /></el-tab-pane>
      <el-tab-pane label="重试记录"><TaskRetryList :task-id="task.id" /></el-tab-pane>
    </el-tabs>
  </ContentWrap>
</template>

<script setup lang="ts">
import { AigcTaskApi } from '@/api/aigc/task'
import type { AigcTaskRespVO } from '@/api/aigc/task/types'
import { statusLabel, typeLabel } from './utils'
import TaskLogList from './log/TaskLogList.vue'
import TaskCallbackList from './callback/TaskCallbackList.vue'
import TaskRetryList from './retry/TaskRetryList.vue'

defineOptions({ name: 'AigcTaskDetail' })

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const task = ref<AigcTaskRespVO>({ id: Number(route.params.id), status: '' })
const displayOutput = computed(() => task.value.outputText || task.value.outputSummary || '-')

const getDetail = async () => {
  loading.value = true
  try {
    task.value = await AigcTaskApi.getTask(Number(route.params.id))
  } finally {
    loading.value = false
  }
}

onMounted(() => getDetail())
</script>
