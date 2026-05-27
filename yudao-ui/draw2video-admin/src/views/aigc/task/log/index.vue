<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" label-width="90px" class="-mb-15px">
      <el-form-item label="任务 ID" prop="taskId">
        <el-input-number v-model="queryParams.taskId" :min="1" controls-position="right" class="!w-200px" clearable />
      </el-form-item>
      <el-form-item label="任务编号" prop="taskNo">
        <el-input v-model="queryParams.taskNo" placeholder="请输入任务编号" clearable class="!w-240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>
  <ContentWrap>
    <TaskLogList :task-id="queryParams.taskId" :task-no="queryParams.taskNo" :key="listKey" />
  </ContentWrap>
</template>

<script setup lang="ts">
import TaskLogList from './TaskLogList.vue'

defineOptions({ name: 'AigcTaskLog' })

const queryFormRef = ref()
const listKey = ref(0)
const queryParams = reactive({ taskId: undefined, taskNo: undefined })

const handleQuery = () => {
  listKey.value++
}

const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}
</script>
