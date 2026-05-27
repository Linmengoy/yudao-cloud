<template>
  <ContentWrap>
    <el-button @click="router.back()"><Icon icon="ep:arrow-left" class="mr-5px" />返回</el-button>
    <el-button class="ml-10px" type="warning" :disabled="!record.taskId" @click="handleSync" v-hasPermi="['aigc:gen:update']">同步第三方任务</el-button>
    <el-descriptions v-loading="loading" class="mt-20px" :column="3" border>
      <el-descriptions-item label="生成流水号">{{ record.generateNo }}</el-descriptions-item>
      <el-descriptions-item label="任务 ID">{{ record.taskId }}</el-descriptions-item>
      <el-descriptions-item label="用户 ID">{{ record.userId }}</el-descriptions-item>
      <el-descriptions-item label="客户端请求号">{{ record.clientRequestId }}</el-descriptions-item>
      <el-descriptions-item label="生成类型">{{ typeLabel(record.generateType) }}</el-descriptions-item>
      <el-descriptions-item label="生成模式">{{ modeLabel(record.generateMode) }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ statusLabel(record.status) }}</el-descriptions-item>
      <el-descriptions-item label="模型 ID">{{ record.modelId }}</el-descriptions-item>
      <el-descriptions-item label="模型编码">{{ record.modelCode }}</el-descriptions-item>
      <el-descriptions-item label="渠道 ID">{{ record.providerId }}</el-descriptions-item>
      <el-descriptions-item label="渠道编码">{{ record.providerCode }}</el-descriptions-item>
      <el-descriptions-item label="第三方任务号">{{ record.providerTaskId }}</el-descriptions-item>
      <el-descriptions-item label="第三方状态">{{ record.providerStatus }}</el-descriptions-item>
      <el-descriptions-item label="冻结记录 ID">{{ record.freezeId }}</el-descriptions-item>
      <el-descriptions-item label="销售价">{{ record.priceAmount }}</el-descriptions-item>
      <el-descriptions-item label="成本价">{{ record.costAmount }}</el-descriptions-item>
      <el-descriptions-item label="提交时间">{{ record.submitTime }}</el-descriptions-item>
      <el-descriptions-item label="回调时间">{{ record.callbackTime }}</el-descriptions-item>
      <el-descriptions-item label="完成时间">{{ record.finishTime }}</el-descriptions-item>
      <el-descriptions-item label="失败原因">{{ record.failReason }}</el-descriptions-item>
      <el-descriptions-item label="失败信息">{{ record.failMessage }}</el-descriptions-item>
    </el-descriptions>
  </ContentWrap>

  <ContentWrap>
    <el-tabs>
      <el-tab-pane label="提示词"><pre class="whitespace-pre-wrap">{{ record.prompt || '-' }}</pre></el-tab-pane>
      <el-tab-pane label="输入参数"><pre>{{ formatJson(record.inputParams) }}</pre></el-tab-pane>
      <el-tab-pane label="输出文本"><pre class="whitespace-pre-wrap">{{ record.outputText || '-' }}</pre></el-tab-pane>
      <el-tab-pane label="结构化输出"><pre>{{ formatJson(record.outputData) }}</pre></el-tab-pane>
      <el-tab-pane label="结果 URL"><pre>{{ formatJson(record.outputUrls) }}</pre></el-tab-pane>
      <el-tab-pane label="资产 ID"><pre>{{ formatJson(record.assetIds) }}</pre></el-tab-pane>
    </el-tabs>
  </ContentWrap>
</template>

<script setup lang="ts">
import { AigcGenerateRecordApi } from '@/api/aigc/gen/record'
import type { AigcGenerateRecordRespVO } from '@/api/aigc/gen/types'
import { formatJson, modeLabel, statusLabel, typeLabel } from '../utils'

defineOptions({ name: 'AigcGenerateRecordDetail' })

const route = useRoute()
const router = useRouter()
const message = useMessage()
const loading = ref(true)
const record = ref<AigcGenerateRecordRespVO>({ id: Number(route.params.id) })

const getDetail = async () => {
  loading.value = true
  try {
    record.value = await AigcGenerateRecordApi.getGenerateRecord(Number(route.params.id))
  } finally {
    loading.value = false
  }
}

const handleSync = async () => {
  if (!record.value.taskId) return
  try {
    await message.confirm('确认同步该第三方任务吗？')
    await AigcGenerateRecordApi.syncGenerateTask(record.value.taskId)
    message.success('同步成功')
    await getDetail()
  } catch {}
}

onMounted(() => getDetail())
</script>
