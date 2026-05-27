<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" label-width="90px" class="-mb-15px">
      <el-form-item label="用户 ID" prop="userId">
        <el-input-number v-model="queryParams.userId" :min="1" controls-position="right" class="!w-200px" clearable />
      </el-form-item>
      <el-form-item label="任务编号" prop="taskNo">
        <el-input v-model="queryParams.taskNo" placeholder="请输入任务编号" clearable class="!w-240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="任务类型" prop="taskType">
        <el-select v-model="queryParams.taskType" placeholder="请选择任务类型" clearable class="!w-200px">
          <el-option v-for="item in taskTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="模型 ID" prop="modelId">
        <el-input-number v-model="queryParams.modelId" :min="1" controls-position="right" class="!w-200px" clearable />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="!w-200px">
          <el-option v-for="item in taskStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
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
      <el-table-column label="任务编号" prop="taskNo" width="190" fixed="left" />
      <el-table-column label="用户 ID" prop="userId" width="110" />
      <el-table-column label="任务类型" prop="taskType" width="150">
        <template #default="{ row }">{{ typeLabel(row.taskType) }}</template>
      </el-table-column>
      <el-table-column label="模型 ID" prop="modelId" width="110" />
      <el-table-column label="状态" prop="status" width="130">
        <template #default="{ row }"><el-tag>{{ statusLabel(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="进度" prop="progress" width="160">
        <template #default="{ row }"><el-progress :percentage="Number(row.progress || 0)" /></template>
      </el-table-column>
      <el-table-column label="销售价" prop="salePrice" width="110" />
      <el-table-column label="币种" prop="currencyType" width="90" />
      <el-table-column label="输出类型" prop="outputAssetType" width="120" />
      <el-table-column label="失败原因" prop="failReason" min-width="180" />
      <el-table-column label="创建时间" prop="createTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="完成时间" prop="finishTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)" v-hasPermi="['aigc:task:query']">详情</el-button>
          <el-button link type="warning" :disabled="!cancellableStatuses.includes(row.status)" @click="handleCancel(row.id)" v-hasPermi="['aigc:task:cancel']">取消</el-button>
          <el-button link type="danger" @click="handleMarkFailed(row.id)" v-hasPermi="['aigc:task:update']">标记失败</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { AigcTaskApi } from '@/api/aigc/task'
import type { AigcTaskPageReqVO, AigcTaskRespVO } from '@/api/aigc/task/types'
import { cancellableStatuses, statusLabel, taskStatusOptions, taskTypeOptions, typeLabel } from './utils'

defineOptions({ name: 'AigcTask' })

const router = useRouter()
const message = useMessage()
const loading = ref(true)
const list = ref<AigcTaskRespVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive<AigcTaskPageReqVO>({ pageNo: 1, pageSize: 10 })

const getList = async () => {
  loading.value = true
  try {
    const data = await AigcTaskApi.getTaskPage(queryParams)
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
  router.push({ path: '/aigc/task/detail/' + id })
}

const handleCancel = async (id: number) => {
  try {
    await message.confirm('确认取消该任务吗？')
    await AigcTaskApi.cancelTask(id)
    message.success('取消成功')
    await getList()
  } catch {}
}

const handleMarkFailed = async (id: number) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入失败原因', '标记失败', { inputType: 'textarea' })
    await AigcTaskApi.markTaskFailed({ taskId: id, failReason: value })
    message.success('标记成功')
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
