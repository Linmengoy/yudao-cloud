<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" label-width="110px" class="-mb-15px">
      <el-form-item label="生成记录 ID" prop="recordId">
        <el-input-number v-model="queryParams.recordId" :min="1" controls-position="right" class="!w-200px" clearable />
      </el-form-item>
      <el-form-item label="任务 ID" prop="taskId">
        <el-input-number v-model="queryParams.taskId" :min="1" controls-position="right" class="!w-200px" clearable />
      </el-form-item>
      <el-form-item label="渠道编码" prop="providerCode">
        <el-input v-model="queryParams.providerCode" placeholder="请输入渠道编码" clearable class="!w-200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="模型编码" prop="modelCode">
        <el-input v-model="queryParams.modelCode" placeholder="请输入模型编码" clearable class="!w-200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="调用结果" prop="success">
        <el-select v-model="queryParams.success" placeholder="请选择调用结果" clearable class="!w-160px">
          <el-option label="成功" :value="true" />
          <el-option label="失败" :value="false" />
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
      <el-table-column label="日志 ID" prop="id" width="100" fixed="left" />
      <el-table-column label="生成记录 ID" prop="recordId" width="120" />
      <el-table-column label="任务 ID" prop="taskId" width="110" />
      <el-table-column label="渠道编码" prop="providerCode" width="130" />
      <el-table-column label="模型编码" prop="modelCode" width="140" />
      <el-table-column label="调用动作" prop="apiAction" width="140" />
      <el-table-column label="请求 ID" prop="requestId" width="180" />
      <el-table-column label="结果" prop="success" width="90">
        <template #default="{ row }"><el-tag :type="row.success ? 'success' : 'danger'">{{ successLabel(row.success) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="HTTP 状态" prop="httpStatus" width="110" />
      <el-table-column label="错误码" prop="errorCode" width="130" />
      <el-table-column label="错误信息" prop="errorMessage" min-width="180" />
      <el-table-column label="耗时(ms)" prop="durationMs" width="110" />
      <el-table-column label="创建时间" prop="createTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)" v-hasPermi="['aigc:gen:query']">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <el-drawer v-model="drawerVisible" title="渠道调用详情" size="60%">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="日志 ID">{{ current?.id }}</el-descriptions-item>
      <el-descriptions-item label="调用结果">{{ successLabel(current?.success) }}</el-descriptions-item>
      <el-descriptions-item label="错误码">{{ current?.errorCode }}</el-descriptions-item>
      <el-descriptions-item label="错误信息">{{ current?.errorMessage }}</el-descriptions-item>
    </el-descriptions>
    <el-tabs class="mt-20px">
      <el-tab-pane label="请求摘要"><pre>{{ formatJson(current?.requestSummary) }}</pre></el-tab-pane>
      <el-tab-pane label="响应摘要"><pre>{{ formatJson(current?.responseSummary) }}</pre></el-tab-pane>
    </el-tabs>
  </el-drawer>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { AigcGenerateProviderLogApi } from '@/api/aigc/gen/provider-log'
import type { AigcGenerateProviderLogPageReqVO, AigcGenerateProviderLogRespVO } from '@/api/aigc/gen/types'
import { formatJson, successLabel } from '../utils'

defineOptions({ name: 'AigcGenerateProviderLog' })

const loading = ref(true)
const list = ref<AigcGenerateProviderLogRespVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const drawerVisible = ref(false)
const current = ref<AigcGenerateProviderLogRespVO>()
const queryParams = reactive<AigcGenerateProviderLogPageReqVO>({ pageNo: 1, pageSize: 10 })

const getList = async () => {
  loading.value = true
  try {
    const data = await AigcGenerateProviderLogApi.getGenerateProviderLogPage(queryParams)
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

const openDetail = (row: AigcGenerateProviderLogRespVO) => {
  current.value = row
  drawerVisible.value = true
}

onMounted(() => getList())
</script>
