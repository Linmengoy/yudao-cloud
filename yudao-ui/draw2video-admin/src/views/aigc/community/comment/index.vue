<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="-mb-15px" label-width="90px">
      <el-form-item label="Post ID" prop="postId">
        <el-input v-model="queryParams.postId" class="!w-140px" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="User ID" prop="userId">
        <el-input v-model="queryParams.userId" class="!w-140px" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="Status" prop="status">
        <el-select v-model="queryParams.status" class="!w-150px" clearable>
          <el-option label="Normal" value="NORMAL" />
          <el-option label="Hidden" value="HIDDEN" />
          <el-option label="Deleted" value="DELETED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />Search</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />Reset</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="ID" prop="id" width="90" />
      <el-table-column label="Post ID" prop="postId" width="110" />
      <el-table-column label="User" prop="userNickname" width="140" />
      <el-table-column label="Content" prop="content" min-width="260" />
      <el-table-column label="Status" prop="status" width="110" />
      <el-table-column label="Audit" prop="auditStatus" width="110" />
      <el-table-column label="Created" prop="createTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="Actions" width="170" fixed="right">
        <template #default="scope">
          <el-button link type="warning" @click="openReason('hide', scope.row.id)">Hide</el-button>
          <el-button link type="danger" @click="openReason('delete', scope.row.id)">Delete</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <Dialog v-model="reasonVisible" :title="reasonAction === 'hide' ? 'Hide comment' : 'Delete comment'" width="520px">
    <el-form ref="reasonFormRef" :model="reasonForm" :rules="reasonRules" label-width="80px">
      <el-form-item label="Reason" prop="reason">
        <el-input v-model="reasonForm.reason" type="textarea" :rows="4" maxlength="200" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" @click="submitReason">OK</el-button>
      <el-button @click="reasonVisible = false">Cancel</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { AigcCommunityApi, type AigcCommunityCommentVO } from '@/api/aigc/community'

defineOptions({ name: 'AigcCommunityComment' })

const message = useMessage()
const loading = ref(false)
const list = ref<AigcCommunityCommentVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive({ pageNo: 1, pageSize: 10, postId: undefined, userId: undefined, status: undefined, auditStatus: undefined })
const reasonVisible = ref(false)
const reasonFormRef = ref()
const reasonAction = ref<'hide' | 'delete'>('hide')
const reasonForm = reactive({ id: 0, reason: '' })
const reasonRules = reactive({ reason: [{ required: true, message: 'Reason is required', trigger: 'blur' }] })

const getList = async () => {
  loading.value = true
  try {
    const data = await AigcCommunityApi.getCommentPage(queryParams)
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
  queryFormRef.value?.resetFields()
  handleQuery()
}
const openReason = (action: 'hide' | 'delete', id: number) => {
  reasonAction.value = action
  reasonForm.id = id
  reasonForm.reason = ''
  reasonVisible.value = true
}
const submitReason = async () => {
  await reasonFormRef.value.validate()
  if (reasonAction.value === 'hide') await AigcCommunityApi.hideComment(reasonForm)
  else await AigcCommunityApi.deleteComment(reasonForm.id, reasonForm.reason)
  message.success('Saved')
  reasonVisible.value = false
  await getList()
}

onMounted(() => getList())
</script>
