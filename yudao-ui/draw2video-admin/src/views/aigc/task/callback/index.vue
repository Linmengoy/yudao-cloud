<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" label-width="110px" class="-mb-15px">
      <el-form-item label="任务 ID" prop="taskId">
        <el-input-number v-model="queryParams.taskId" :min="1" controls-position="right" class="!w-200px" clearable />
      </el-form-item>
      <el-form-item label="渠道编码" prop="providerCode">
        <el-input v-model="queryParams.providerCode" placeholder="请输入渠道编码" clearable class="!w-200px" />
      </el-form-item>
      <el-form-item label="第三方任务号" prop="externalTaskId">
        <el-input v-model="queryParams.externalTaskId" placeholder="请输入第三方任务号" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="处理状态" prop="callbackStatus">
        <el-select v-model="queryParams.callbackStatus" placeholder="请选择处理状态" clearable class="!w-180px">
          <el-option label="已接收" value="RECEIVED" />
          <el-option label="已处理" value="PROCESSED" />
          <el-option label="处理失败" value="FAILED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>
  <ContentWrap>
    <TaskCallbackList
      :task-id="queryParams.taskId"
      :provider-code="queryParams.providerCode"
      :external-task-id="queryParams.externalTaskId"
      :callback-status="queryParams.callbackStatus"
      :key="listKey"
    />
  </ContentWrap>
</template>

<script setup lang="ts">
import TaskCallbackList from './TaskCallbackList.vue'

defineOptions({ name: 'AigcTaskCallback' })

const queryFormRef = ref()
const listKey = ref(0)
const queryParams = reactive({ taskId: undefined, providerCode: undefined, externalTaskId: undefined, callbackStatus: undefined })

const handleQuery = () => {
  listKey.value++
}

const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}
</script>
