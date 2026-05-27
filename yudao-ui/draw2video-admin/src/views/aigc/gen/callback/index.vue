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
      <el-form-item label="第三方任务号" prop="providerTaskId">
        <el-input v-model="queryParams.providerTaskId" placeholder="请输入第三方任务号" clearable class="!w-240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="处理状态" prop="processStatus">
        <el-select v-model="queryParams.processStatus" placeholder="请选择处理状态" clearable class="!w-180px">
          <el-option v-for="item in callbackProcessStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
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
      <el-table-column label="回调 ID" prop="id" width="100" fixed="left" />
      <el-table-column label="生成记录 ID" prop="recordId" width="120" />
      <el-table-column label="任务 ID" prop="taskId" width="110" />
      <el-table-column label="渠道编码" prop="providerCode" width="130" />
      <el-table-column label="第三方任务号" prop="providerTaskId" width="190" />
      <el-table-column label="回调类型" prop="callbackType" width="120" />
      <el-table-column label="回调编号" prop="callbackNo" width="180" />
      <el-table-column label="验签" prop="signatureValid" width="90">
        <template #default="{ row }">
          <el-tag :type="row.signatureValid === false ? 'danger' : 'success'">{{ row.signatureValid === false ? '失败' : '通过' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="处理状态" prop="processStatus" width="120">
        <template #default="{ row }">{{ processStatusLabel(row.processStatus) }}</template>
      </el-table-column>
      <el-table-column label="处理说明" prop="processMessage" min-width="180" />
      <el-table-column label="处理时间" prop="processTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)" v-hasPermi="['aigc:gen:query']">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <el-drawer v-model="drawerVisible" title="回调详情" size="60%">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="回调 ID">{{ current?.id }}</el-descriptions-item>
      <el-descriptions-item label="处理状态">{{ processStatusLabel(current?.processStatus) }}</el-descriptions-item>
      <el-descriptions-item label="处理说明" :span="2">{{ current?.processMessage }}</el-descriptions-item>
    </el-descriptions>
    <el-tabs class="mt-20px">
      <el-tab-pane label="原始回调"><pre>{{ formatJson(current?.rawBody) }}</pre></el-tab-pane>
      <el-tab-pane label="解析数据"><pre>{{ formatJson(current?.parsedData) }}</pre></el-tab-pane>
    </el-tabs>
  </el-drawer>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { AigcGenerateCallbackApi } from '@/api/aigc/gen/callback'
import type { AigcGenerateCallbackPageReqVO, AigcGenerateCallbackRespVO } from '@/api/aigc/gen/types'
import { callbackProcessStatusOptions, formatJson, processStatusLabel } from '../utils'

defineOptions({ name: 'AigcGenerateCallback' })

const loading = ref(true)
const list = ref<AigcGenerateCallbackRespVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const drawerVisible = ref(false)
const current = ref<AigcGenerateCallbackRespVO>()
const queryParams = reactive<AigcGenerateCallbackPageReqVO>({ pageNo: 1, pageSize: 10 })

const getList = async () => {
  loading.value = true
  try {
    const data = await AigcGenerateCallbackApi.getGenerateCallbackPage(queryParams)
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

const openDetail = (row: AigcGenerateCallbackRespVO) => {
  current.value = row
  drawerVisible.value = true
}

onMounted(() => getList())
</script>
