<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" label-width="90px" class="-mb-15px">
      <el-form-item label="任务 ID" prop="taskId">
        <el-input-number v-model="queryParams.taskId" :min="1" controls-position="right" class="!w-200px" clearable />
      </el-form-item>
      <el-form-item label="任务编号" prop="taskNo">
        <el-input v-model="queryParams.taskNo" placeholder="请输入任务编号" clearable class="!w-240px" />
      </el-form-item>
      <el-form-item label="状态" prop="retryStatus">
        <el-select v-model="queryParams.retryStatus" placeholder="请选择状态" clearable class="!w-180px">
          <el-option label="等待中" value="WAITING" />
          <el-option label="运行中" value="RUNNING" />
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>
  <ContentWrap>
    <TaskRetryList
      :task-id="queryParams.taskId"
      :task-no="queryParams.taskNo"
      :retry-status="queryParams.retryStatus"
      :key="listKey"
    />
  </ContentWrap>
</template>

<script setup lang="ts">
import TaskRetryList from './TaskRetryList.vue'

defineOptions({ name: 'AigcTaskRetry' })

const queryFormRef = ref()
const listKey = ref(0)
const queryParams = reactive({ taskId: undefined, taskNo: undefined, retryStatus: undefined })

const handleQuery = () => {
  listKey.value++
}

const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}
</script>
