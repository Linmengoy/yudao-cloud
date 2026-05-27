<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" label-width="100px" class="-mb-15px">
      <el-form-item label="用户 ID" prop="userId">
        <el-input-number v-model="queryParams.userId" :min="1" controls-position="right" class="!w-200px" clearable />
      </el-form-item>
      <el-form-item label="任务 ID" prop="taskId">
        <el-input-number v-model="queryParams.taskId" :min="1" controls-position="right" class="!w-200px" clearable />
      </el-form-item>
      <el-form-item label="生成流水号" prop="generateNo">
        <el-input v-model="queryParams.generateNo" placeholder="请输入生成流水号" clearable class="!w-240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="生成类型" prop="generateType">
        <el-select v-model="queryParams.generateType" placeholder="请选择生成类型" clearable class="!w-180px">
          <el-option v-for="item in generateTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="生成模式" prop="generateMode">
        <el-select v-model="queryParams.generateMode" placeholder="请选择生成模式" clearable class="!w-180px">
          <el-option v-for="item in generateModeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="模型 ID" prop="modelId">
        <el-input-number v-model="queryParams.modelId" :min="1" controls-position="right" class="!w-200px" clearable />
      </el-form-item>
      <el-form-item label="渠道编码" prop="providerCode">
        <el-input v-model="queryParams.providerCode" placeholder="请输入渠道编码" clearable class="!w-180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="!w-180px">
          <el-option v-for="item in generateStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="生成流水号" prop="generateNo" width="190" fixed="left" />
      <el-table-column label="任务 ID" prop="taskId" width="110" />
      <el-table-column label="用户 ID" prop="userId" width="110" />
      <el-table-column label="生成类型" prop="generateType" width="120">
        <template #default="{ row }">{{ typeLabel(row.generateType) }}</template>
      </el-table-column>
      <el-table-column label="生成模式" prop="generateMode" width="130">
        <template #default="{ row }">{{ modeLabel(row.generateMode) }}</template>
      </el-table-column>
      <el-table-column label="模型编码" prop="modelCode" width="140" />
      <el-table-column label="渠道编码" prop="providerCode" width="130" />
      <el-table-column label="第三方状态" prop="providerStatus" width="130" />
      <el-table-column label="状态" prop="status" width="120">
        <template #default="{ row }"><el-tag>{{ statusLabel(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="销售价" prop="priceAmount" width="110" />
      <el-table-column label="成本价" prop="costAmount" width="110" />
      <el-table-column label="失败信息" prop="failMessage" min-width="180" />
      <el-table-column label="提交时间" prop="submitTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="完成时间" prop="finishTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)" v-hasPermi="['aigc:gen:query']">详情</el-button>
          <el-button link type="warning" :disabled="!row.taskId" @click="handleSync(row.taskId)" v-hasPermi="['aigc:gen:update']">同步</el-button>
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
import { generateModeOptions, generateStatusOptions, generateTypeOptions, modeLabel, statusLabel, typeLabel } from '../utils'

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
    await message.confirm('确认同步该第三方任务吗？')
    await AigcGenerateRecordApi.syncGenerateTask(taskId)
    message.success('同步成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
