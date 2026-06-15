<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="-mb-15px" label-width="90px">
      <el-form-item label="作品 ID" prop="postId">
        <el-input-number v-model="queryParams.postId" :min="1" controls-position="right" class="!w-160px" />
      </el-form-item>
      <el-form-item label="用户 ID" prop="userId">
        <el-input-number v-model="queryParams.userId" :min="1" controls-position="right" class="!w-160px" />
      </el-form-item>
      <el-form-item label="评论状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-150px" clearable placeholder="请选择">
          <el-option v-for="item in commentStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="审核状态" prop="auditStatus">
        <el-select v-model="queryParams.auditStatus" class="!w-150px" clearable placeholder="请选择">
          <el-option v-for="item in auditStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
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
      <el-table-column label="ID" prop="id" width="90" />
      <el-table-column label="作品 ID" prop="postId" width="110" />
      <el-table-column label="用户" min-width="150">
        <template #default="{ row }">
          <div>{{ row.userNickname || '-' }}</div>
          <div class="mt-4px text-12px text-gray-500">ID: {{ row.userId || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="评论内容" prop="content" min-width="320" />
      <el-table-column label="评论状态" prop="status" width="120">
        <template #default="{ row }">
          <el-tag :type="commentTagType(row.status)">{{ commentStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审核状态" prop="auditStatus" width="120">
        <template #default="{ row }">
          <el-tag :type="auditTagType(row.auditStatus)">{{ auditStatusLabel(row.auditStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="处理原因" prop="auditReason" min-width="180" />
      <el-table-column label="点赞数" prop="likeCount" width="90" />
      <el-table-column label="创建时间" prop="createTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button v-hasPermi="['aigc:community-comment:audit']" link type="warning" :disabled="row.status === 'HIDDEN' || row.status === 'DELETED'" @click="openReason('hide', row.id)">屏蔽</el-button>
          <el-button v-hasPermi="['aigc:community-comment:audit']" link type="danger" :disabled="row.status === 'DELETED'" @click="openReason('delete', row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <Dialog v-model="reasonVisible" :title="reasonAction === 'hide' ? '屏蔽评论' : '删除评论'" width="520px">
    <el-form ref="reasonFormRef" :model="reasonForm" :rules="reasonRules" label-width="90px">
      <el-form-item label="处理原因" prop="reason">
        <el-input v-model="reasonForm.reason" type="textarea" :rows="4" maxlength="200" show-word-limit placeholder="请输入处理原因" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" @click="submitReason">确定</el-button>
      <el-button @click="reasonVisible = false">取消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import {
  AigcCommunityApi,
  type AigcCommunityAuditReqVO,
  type AigcCommunityCommentPageReqVO,
  type AigcCommunityCommentVO
} from '@/api/aigc/community'

defineOptions({ name: 'AigcCommunityComment' })

const message = useMessage()
const loading = ref(false)
const list = ref<AigcCommunityCommentVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive<AigcCommunityCommentPageReqVO>({ pageNo: 1, pageSize: 10 })
const reasonVisible = ref(false)
const reasonFormRef = ref()
const reasonAction = ref<'hide' | 'delete'>('hide')
const reasonForm = reactive<AigcCommunityAuditReqVO>({ id: 0, reason: '' })
const reasonRules = reactive({ reason: [{ required: true, message: '请输入处理原因', trigger: 'blur' }] })

const commentStatusOptions = [
  { label: '正常', value: 'NORMAL' },
  { label: '已屏蔽', value: 'HIDDEN' },
  { label: '已删除', value: 'DELETED' }
]
const auditStatusOptions = [
  { label: '待审核', value: 'PENDING' },
  { label: '审核通过', value: 'PASS' },
  { label: '审核驳回', value: 'REJECT' },
  { label: '人工复核', value: 'MANUAL_REVIEW' }
]
const optionLabel = (options: Array<{ label: string; value: string }>, value?: string) => options.find((item) => item.value === value)?.label || value || '-'
const commentStatusLabel = (value?: string) => optionLabel(commentStatusOptions, value)
const auditStatusLabel = (value?: string) => optionLabel(auditStatusOptions, value)
const commentTagType = (value?: string) => value === 'NORMAL' ? 'success' : value === 'DELETED' ? 'danger' : 'warning'
const auditTagType = (value?: string) => value === 'PASS' ? 'success' : value === 'REJECT' ? 'danger' : 'warning'

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
  if (reasonAction.value === 'hide') {
    await AigcCommunityApi.hideComment(reasonForm)
    message.success('评论已屏蔽')
  } else {
    await AigcCommunityApi.deleteComment(reasonForm.id, reasonForm.reason || '')
    message.success('评论已删除')
  }
  reasonVisible.value = false
  await getList()
}

onMounted(() => getList())
</script>
