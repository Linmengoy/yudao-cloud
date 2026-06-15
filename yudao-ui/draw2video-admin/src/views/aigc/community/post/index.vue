<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="-mb-15px" label-width="90px">
      <el-form-item label="Title" prop="title">
        <el-input v-model="queryParams.title" class="!w-180px" clearable placeholder="Title" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="Author" prop="authorUserId">
        <el-input v-model="queryParams.authorUserId" class="!w-140px" clearable placeholder="User ID" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="Publish" prop="publishStatus">
        <el-select v-model="queryParams.publishStatus" class="!w-150px" clearable placeholder="Publish">
          <el-option v-for="item in publishStatuses" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="Audit" prop="auditStatus">
        <el-select v-model="queryParams.auditStatus" class="!w-150px" clearable placeholder="Audit">
          <el-option v-for="item in auditStatuses" :key="item.value" :label="item.label" :value="item.value" />
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
      <el-table-column label="Preview" width="100">
        <template #default="scope">
          <el-image v-if="scope.row.coverUrl || scope.row.fileUrl" :src="scope.row.coverUrl || scope.row.fileUrl" fit="cover" class="h-56px w-72px rounded" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="Title" prop="title" min-width="180" />
      <el-table-column label="Author" prop="authorNickname" width="130" />
      <el-table-column label="Asset" prop="assetType" width="100" />
      <el-table-column label="Publish" prop="publishStatus" width="110">
        <template #default="scope"><el-tag>{{ scope.row.publishStatus }}</el-tag></template>
      </el-table-column>
      <el-table-column label="Audit" prop="auditStatus" width="110">
        <template #default="scope"><el-tag :type="auditTag(scope.row.auditStatus)">{{ scope.row.auditStatus }}</el-tag></template>
      </el-table-column>
      <el-table-column label="Stats" width="170">
        <template #default="scope">
          {{ scope.row.likeCount || 0 }} likes / {{ scope.row.commentCount || 0 }} comments
        </template>
      </el-table-column>
      <el-table-column label="Created" prop="createTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="Actions" width="250" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row.id)">Detail</el-button>
          <el-button link type="success" @click="handlePass(scope.row.id)">Pass</el-button>
          <el-button link type="warning" @click="openReason('reject', scope.row.id)">Reject</el-button>
          <el-button link type="danger" @click="openReason('offline', scope.row.id)">Offline</el-button>
          <el-button v-if="scope.row.publishStatus === 'OFFLINE'" link type="success" @click="handleRestore(scope.row.id)">Restore</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <el-drawer v-model="detailVisible" title="Community post" size="560px">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="ID">{{ detailData.id || '-' }}</el-descriptions-item>
      <el-descriptions-item label="Title">{{ detailData.title || '-' }}</el-descriptions-item>
      <el-descriptions-item label="Author">{{ detailData.authorNickname || detailData.authorUserId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="Summary">{{ detailData.summary || '-' }}</el-descriptions-item>
      <el-descriptions-item label="Tags">{{ detailData.tags || '-' }}</el-descriptions-item>
      <el-descriptions-item label="Publish">{{ detailData.publishStatus || '-' }}</el-descriptions-item>
      <el-descriptions-item label="Audit">{{ detailData.auditStatus || '-' }}</el-descriptions-item>
      <el-descriptions-item label="Audit reason">{{ detailData.auditReason || '-' }}</el-descriptions-item>
      <el-descriptions-item label="Offline reason">{{ detailData.offlineReason || '-' }}</el-descriptions-item>
    </el-descriptions>
  </el-drawer>

  <Dialog v-model="reasonVisible" :title="reasonAction === 'reject' ? 'Reject post' : 'Offline post'" width="520px">
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
import { AigcCommunityApi, type AigcCommunityPostVO } from '@/api/aigc/community'

defineOptions({ name: 'AigcCommunityPost' })

const message = useMessage()
const loading = ref(false)
const list = ref<AigcCommunityPostVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive({ pageNo: 1, pageSize: 10, title: undefined, authorUserId: undefined, publishStatus: undefined, auditStatus: undefined })
const detailVisible = ref(false)
const detailData = ref<AigcCommunityPostVO>({ id: 0 })
const reasonVisible = ref(false)
const reasonFormRef = ref()
const reasonAction = ref<'reject' | 'offline'>('reject')
const reasonForm = reactive({ id: 0, reason: '' })
const reasonRules = reactive({ reason: [{ required: true, message: 'Reason is required', trigger: 'blur' }] })
const publishStatuses = [{ label: 'Pending', value: 'PENDING' }, { label: 'Published', value: 'PUBLISHED' }, { label: 'Offline', value: 'OFFLINE' }, { label: 'Rejected', value: 'REJECTED' }]
const auditStatuses = [{ label: 'Pending', value: 'PENDING' }, { label: 'Pass', value: 'PASS' }, { label: 'Reject', value: 'REJECT' }, { label: 'Manual', value: 'MANUAL_REVIEW' }]

const getList = async () => {
  loading.value = true
  try {
    const data = await AigcCommunityApi.getPostPage(queryParams)
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
const openDetail = async (id: number) => {
  detailData.value = await AigcCommunityApi.getPost(id)
  detailVisible.value = true
}
const handlePass = async (id: number) => {
  await message.confirm('Pass this community post?')
  await AigcCommunityApi.auditPassPost(id)
  message.success('Passed')
  await getList()
}
const handleRestore = async (id: number) => {
  await message.confirm('Restore this community post?')
  await AigcCommunityApi.restorePost(id)
  message.success('Restored')
  await getList()
}
const openReason = (action: 'reject' | 'offline', id: number) => {
  reasonAction.value = action
  reasonForm.id = id
  reasonForm.reason = ''
  reasonVisible.value = true
}
const submitReason = async () => {
  await reasonFormRef.value.validate()
  if (reasonAction.value === 'reject') await AigcCommunityApi.auditRejectPost(reasonForm)
  else await AigcCommunityApi.offlinePost(reasonForm)
  message.success('Saved')
  reasonVisible.value = false
  await getList()
}
const auditTag = (status?: string) => status === 'PASS' ? 'success' : status === 'REJECT' ? 'danger' : 'warning'

onMounted(() => getList())
</script>
