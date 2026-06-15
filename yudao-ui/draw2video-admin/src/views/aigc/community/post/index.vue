<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="-mb-15px" label-width="90px">
      <el-form-item label="作品标题" prop="title">
        <el-input v-model="queryParams.title" class="!w-220px" clearable placeholder="请输入作品标题" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="作者 ID" prop="authorUserId">
        <el-input-number v-model="queryParams.authorUserId" :min="1" controls-position="right" class="!w-160px" />
      </el-form-item>
      <el-form-item label="发布状态" prop="publishStatus">
        <el-select v-model="queryParams.publishStatus" class="!w-160px" clearable placeholder="请选择">
          <el-option v-for="item in publishStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="审核状态" prop="auditStatus">
        <el-select v-model="queryParams.auditStatus" class="!w-160px" clearable placeholder="请选择">
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
      <el-table-column label="封面" width="108">
        <template #default="{ row }">
          <el-image
            v-if="row.coverUrl || row.fileUrl"
            :src="row.coverUrl || row.fileUrl"
            :preview-src-list="[row.coverUrl || row.fileUrl]"
            fit="cover"
            preview-teleported
            class="h-56px w-76px rounded"
          />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="作品" min-width="220">
        <template #default="{ row }">
          <div class="font-medium">{{ row.title || '-' }}</div>
          <div class="mt-4px text-12px text-gray-500">{{ row.postNo || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="作者" min-width="150">
        <template #default="{ row }">
          <div>{{ row.authorNickname || '-' }}</div>
          <div class="mt-4px text-12px text-gray-500">ID: {{ row.authorUserId || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="来源" width="130">
        <template #default="{ row }">
          {{ row.assetType || (row.projectId ? 'PROJECT' : '-') }}
        </template>
      </el-table-column>
      <el-table-column label="发布状态" prop="publishStatus" width="120">
        <template #default="{ row }">
          <el-tag :type="publishTagType(row.publishStatus)">{{ publishStatusLabel(row.publishStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审核状态" prop="auditStatus" width="120">
        <template #default="{ row }">
          <el-tag :type="auditTagType(row.auditStatus)">{{ auditStatusLabel(row.auditStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="互动" width="190">
        <template #default="{ row }">
          {{ row.viewCount || 0 }} 浏览 / {{ row.likeCount || 0 }} 赞 / {{ row.commentCount || 0 }} 评
        </template>
      </el-table-column>
      <el-table-column label="审核时间" prop="auditTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="创建时间" prop="createTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button v-hasPermi="['aigc:community-post:query']" link type="primary" @click="openDetail(row.id)">详情</el-button>
          <el-button v-hasPermi="['aigc:community-post:audit']" link type="success" :disabled="row.auditStatus === 'PASS' && row.publishStatus === 'PUBLISHED'" @click="handlePass(row.id)">通过</el-button>
          <el-button v-hasPermi="['aigc:community-post:audit']" link type="warning" @click="openReason('reject', row.id)">驳回</el-button>
          <el-button v-hasPermi="['aigc:community-post:audit']" link type="danger" :disabled="row.publishStatus !== 'PUBLISHED'" @click="openReason('offline', row.id)">下架</el-button>
          <el-button v-if="row.publishStatus === 'OFFLINE'" v-hasPermi="['aigc:community-post:audit']" link type="success" @click="handleRestore(row.id)">恢复</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <el-drawer v-model="detailVisible" title="社区作品详情" size="640px">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="作品 ID">{{ detailData.id || '-' }}</el-descriptions-item>
      <el-descriptions-item label="作品编号">{{ detailData.postNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="标题">{{ detailData.title || '-' }}</el-descriptions-item>
      <el-descriptions-item label="作者">{{ detailData.authorNickname || '-' }}（{{ detailData.authorUserId || '-' }}）</el-descriptions-item>
      <el-descriptions-item label="资产/项目">{{ detailData.assetId || '-' }} / {{ detailData.projectId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="摘要">{{ detailData.summary || '-' }}</el-descriptions-item>
      <el-descriptions-item label="标签">{{ detailData.tags || '-' }}</el-descriptions-item>
      <el-descriptions-item label="提示词快照">
        <pre class="m-0 whitespace-pre-wrap text-12px">{{ detailData.promptSnapshot || '-' }}</pre>
      </el-descriptions-item>
      <el-descriptions-item label="发布状态">{{ publishStatusLabel(detailData.publishStatus) }}</el-descriptions-item>
      <el-descriptions-item label="审核状态">{{ auditStatusLabel(detailData.auditStatus) }}</el-descriptions-item>
      <el-descriptions-item label="审核人">{{ detailData.auditorUserId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="审核时间">{{ formatNullableDate(detailData.auditTime) }}</el-descriptions-item>
      <el-descriptions-item label="审核原因">{{ detailData.auditReason || '-' }}</el-descriptions-item>
      <el-descriptions-item label="下架原因">{{ detailData.offlineReason || '-' }}</el-descriptions-item>
      <el-descriptions-item label="下架时间">{{ formatNullableDate(detailData.offlineTime) }}</el-descriptions-item>
      <el-descriptions-item label="互动数据">
        浏览 {{ detailData.viewCount || 0 }} / 点赞 {{ detailData.likeCount || 0 }} / 评论 {{ detailData.commentCount || 0 }} / 分享 {{ detailData.shareCount || 0 }}
      </el-descriptions-item>
    </el-descriptions>
  </el-drawer>

  <Dialog v-model="reasonVisible" :title="reasonTitle" width="520px">
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
import { dateFormatter, formatDate } from '@/utils/formatTime'
import {
  AigcCommunityApi,
  type AigcCommunityAuditReqVO,
  type AigcCommunityPostPageReqVO,
  type AigcCommunityPostVO
} from '@/api/aigc/community'

defineOptions({ name: 'AigcCommunityPost' })

const message = useMessage()
const loading = ref(false)
const list = ref<AigcCommunityPostVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive<AigcCommunityPostPageReqVO>({ pageNo: 1, pageSize: 10 })
const detailVisible = ref(false)
const detailData = ref<AigcCommunityPostVO>({ id: 0 })
const reasonVisible = ref(false)
const reasonFormRef = ref()
const reasonAction = ref<'reject' | 'offline'>('reject')
const reasonForm = reactive<AigcCommunityAuditReqVO>({ id: 0, reason: '' })
const reasonRules = reactive({ reason: [{ required: true, message: '请输入处理原因', trigger: 'blur' }] })

const publishStatusOptions = [
  { label: '待发布', value: 'PENDING' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已下架', value: 'OFFLINE' },
  { label: '已驳回', value: 'REJECTED' }
]
const auditStatusOptions = [
  { label: '待审核', value: 'PENDING' },
  { label: '审核通过', value: 'PASS' },
  { label: '审核驳回', value: 'REJECT' },
  { label: '人工复核', value: 'MANUAL_REVIEW' }
]
const reasonTitle = computed(() => reasonAction.value === 'reject' ? '驳回社区作品' : '下架社区作品')

const optionLabel = (options: Array<{ label: string; value: string }>, value?: string) => options.find((item) => item.value === value)?.label || value || '-'
const publishStatusLabel = (value?: string) => optionLabel(publishStatusOptions, value)
const auditStatusLabel = (value?: string) => optionLabel(auditStatusOptions, value)
const publishTagType = (value?: string) => value === 'PUBLISHED' ? 'success' : value === 'OFFLINE' || value === 'REJECTED' ? 'danger' : 'warning'
const auditTagType = (value?: string) => value === 'PASS' ? 'success' : value === 'REJECT' ? 'danger' : 'warning'
const formatNullableDate = (value?: string) => value ? formatDate(value) : '-'

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
  await message.confirm('确认通过该社区作品审核吗？通过后作品将在社区公开展示。')
  await AigcCommunityApi.auditPassPost(id)
  message.success('审核通过')
  await getList()
}
const handleRestore = async (id: number) => {
  await message.confirm('确认恢复该社区作品公开展示吗？')
  await AigcCommunityApi.restorePost(id)
  message.success('恢复成功')
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
  if (reasonAction.value === 'reject') {
    await AigcCommunityApi.auditRejectPost(reasonForm)
    message.success('已驳回')
  } else {
    await AigcCommunityApi.offlinePost(reasonForm)
    message.success('已下架')
  }
  reasonVisible.value = false
  await getList()
}

onMounted(() => getList())
</script>
