<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" label-width="88px" class="-mb-15px">
      <el-form-item label="User ID" prop="userId">
        <el-input-number v-model="queryParams.userId" :min="1" controls-position="right" class="!w-180px" clearable />
      </el-form-item>
      <el-form-item label="Title" prop="title">
        <el-input v-model="queryParams.title" placeholder="Asset title keyword" clearable class="!w-220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="Type" prop="assetType">
        <el-select v-model="queryParams.assetType" placeholder="All types" clearable class="!w-160px">
          <el-option v-for="item in assetTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="Audit" prop="auditStatus">
        <el-select v-model="queryParams.auditStatus" placeholder="All audit states" clearable class="!w-170px">
          <el-option v-for="item in auditStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />Search</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />Reset</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="Assets" name="assets">
        <el-table v-loading="assetLoading" :data="assetList" :stripe="true" :show-overflow-tooltip="true">
          <el-table-column label="Preview" width="96" fixed="left">
            <template #default="{ row }">
              <el-image
                v-if="isImage(row)"
                :src="previewUrl(row)"
                fit="cover"
                class="h-56px w-72px rounded border border-[var(--el-border-color)]"
                :preview-src-list="[previewUrl(row)]"
                preview-teleported
              />
              <el-tag v-else>{{ row.assetType || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Asset No" prop="assetNo" width="170" />
          <el-table-column label="User ID" prop="userId" width="100" />
          <el-table-column label="Title" prop="title" min-width="180" />
          <el-table-column label="Type" prop="assetType" width="100">
            <template #default="{ row }">{{ optionLabel(assetTypeOptions, row.assetType) }}</template>
          </el-table-column>
          <el-table-column label="Source" prop="sourceType" width="110">
            <template #default="{ row }">{{ optionLabel(sourceTypeOptions, row.sourceType) }}</template>
          </el-table-column>
          <el-table-column label="Task No" prop="taskNo" width="160" />
          <el-table-column label="Audit" prop="auditStatus" width="120">
            <template #default="{ row }">
              <el-tag :type="auditTagType(row.auditStatus)">{{ optionLabel(auditStatusOptions, row.auditStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Visibility" prop="visibility" width="110">
            <template #default="{ row }">{{ optionLabel(visibilityOptions, row.visibility) }}</template>
          </el-table-column>
          <el-table-column label="Downloads" prop="downloadCount" width="100" />
          <el-table-column label="Created At" prop="createTime" :formatter="dateFormatter" width="180" />
          <el-table-column label="Actions" width="220" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openAssetDetail(row)" v-hasPermi="['aigc:asset:query']">Detail</el-button>
              <el-button link type="success" @click="handleAudit(row, 'PASS')" v-hasPermi="['aigc:asset:audit']">Pass</el-button>
              <el-button link type="warning" @click="handleAudit(row, 'REJECT')" v-hasPermi="['aigc:asset:audit']">Reject</el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination :total="assetTotal" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getAssetList" />
      </el-tab-pane>

      <el-tab-pane label="Canvas Projects" name="projects">
        <el-table v-loading="projectLoading" :data="projectList" :stripe="true" :show-overflow-tooltip="true">
          <el-table-column label="Cover" width="96" fixed="left">
            <template #default="{ row }">
              <el-image
                v-if="row.coverUrl"
                :src="row.coverUrl"
                fit="cover"
                class="h-56px w-72px rounded border border-[var(--el-border-color)]"
                :preview-src-list="[row.coverUrl]"
                preview-teleported
              />
              <el-tag v-else>Project</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Project ID" prop="id" width="110" />
          <el-table-column label="User ID" prop="ownerUserId" width="110" />
          <el-table-column label="Name" prop="name" min-width="200" />
          <el-table-column label="Version" prop="currentVersion" width="90" />
          <el-table-column label="Nodes" prop="nodeCount" width="90" />
          <el-table-column label="Assets" prop="assetCount" width="90" />
          <el-table-column label="Status" prop="status" width="110" />
          <el-table-column label="Updated At" prop="updateTime" :formatter="dateFormatter" width="180" />
        </el-table>
        <Pagination :total="projectTotal" v-model:page="projectQuery.pageNo" v-model:limit="projectQuery.pageSize" @pagination="getProjectList" />
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>

  <el-dialog v-model="detailVisible" title="Asset Detail" width="860px">
    <el-descriptions v-if="currentAsset" :column="2" border>
      <el-descriptions-item label="Asset No">{{ currentAsset.assetNo }}</el-descriptions-item>
      <el-descriptions-item label="User ID">{{ currentAsset.userId }}</el-descriptions-item>
      <el-descriptions-item label="Title">{{ currentAsset.title || '-' }}</el-descriptions-item>
      <el-descriptions-item label="Task No">{{ currentAsset.taskNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="Size">{{ assetSize(currentAsset) }}</el-descriptions-item>
      <el-descriptions-item label="File Size">{{ formatFileSize(currentAsset.fileSize) }}</el-descriptions-item>
      <el-descriptions-item label="Tags" :span="2">{{ currentAsset.tags || '-' }}</el-descriptions-item>
      <el-descriptions-item label="Description" :span="2">{{ currentAsset.description || '-' }}</el-descriptions-item>
    </el-descriptions>
    <el-tabs v-if="currentAsset" class="mt-16px">
      <el-tab-pane label="Files">
        <el-table :data="currentAsset.files || []" size="small">
          <el-table-column label="Role" prop="fileRole" width="110" />
          <el-table-column label="File Name" prop="fileName" min-width="180" />
          <el-table-column label="MIME" prop="mimeType" width="150" />
          <el-table-column label="Size" width="110">
            <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
          </el-table-column>
          <el-table-column label="Access URL" prop="accessUrl" min-width="220" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="Prompt Snapshot">
        <pre class="detail-json">{{ formatJson(currentAsset.promptSnapshot) }}</pre>
      </el-tab-pane>
      <el-tab-pane label="Generation Snapshot">
        <pre class="detail-json">{{ formatJson(currentAsset.generateSnapshot) }}</pre>
      </el-tab-pane>
      <el-tab-pane label="Metadata">
        <pre class="detail-json">{{ formatJson(currentAsset.metadata) }}</pre>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { AigcAssetApi, type AigcAssetPageReqVO, type AigcAssetVO } from '@/api/aigc/asset'
import { AigcCanvasApi, type AigcCanvasProjectPageReqVO, type AigcCanvasProjectVO } from '@/api/aigc/canvas'

defineOptions({ name: 'AigcAsset' })

const message = useMessage()
const activeTab = ref('assets')
const queryFormRef = ref()
const queryParams = reactive<AigcAssetPageReqVO>({ pageNo: 1, pageSize: 10, status: 'NORMAL' })
const projectQuery = reactive<AigcCanvasProjectPageReqVO>({ pageNo: 1, pageSize: 10 })
const assetLoading = ref(false)
const projectLoading = ref(false)
const assetList = ref<AigcAssetVO[]>([])
const projectList = ref<AigcCanvasProjectVO[]>([])
const assetTotal = ref(0)
const projectTotal = ref(0)
const detailVisible = ref(false)
const currentAsset = ref<AigcAssetVO>()

const assetTypeOptions = [
  { label: 'Image', value: 'IMAGE' },
  { label: 'Video', value: 'VIDEO' },
  { label: 'Audio', value: 'AUDIO' },
  { label: 'Text', value: 'TEXT' },
  { label: 'Other', value: 'OTHER' }
]
const sourceTypeOptions = [
  { label: 'Generate', value: 'GENERATE' },
  { label: 'Upload', value: 'UPLOAD' },
  { label: 'Import', value: 'IMPORT' }
]
const auditStatusOptions = [
  { label: 'Pending', value: 'PENDING' },
  { label: 'Pass', value: 'PASS' },
  { label: 'Reject', value: 'REJECT' },
  { label: 'Manual Review', value: 'MANUAL_REVIEW' }
]
const visibilityOptions = [
  { label: 'Private', value: 'PRIVATE' },
  { label: 'Public', value: 'PUBLIC' }
]

const getAssetList = async () => {
  assetLoading.value = true
  try {
    const data = await AigcAssetApi.getAssetPage(queryParams)
    assetList.value = data.list || []
    assetTotal.value = data.total || 0
  } finally {
    assetLoading.value = false
  }
}

const getProjectList = async () => {
  projectLoading.value = true
  try {
    projectQuery.ownerUserId = queryParams.userId
    const data = await AigcCanvasApi.getProjectPage(projectQuery)
    projectList.value = data.list || []
    projectTotal.value = data.total || 0
  } finally {
    projectLoading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  projectQuery.pageNo = 1
  activeTab.value === 'assets' ? getAssetList() : getProjectList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

const handleTabChange = () => {
  activeTab.value === 'assets' ? getAssetList() : getProjectList()
}

const openAssetDetail = (asset: AigcAssetVO) => {
  currentAsset.value = asset
  detailVisible.value = true
}

const handleAudit = async (asset: AigcAssetVO, auditStatus: string) => {
  const auditReason = auditStatus === 'REJECT' ? 'Rejected by admin' : undefined
  await AigcAssetApi.updateAuditStatus({ id: asset.id, auditStatus, auditReason })
  message.success('Audit status updated')
  await getAssetList()
}

const optionLabel = (options: Array<{ label: string; value: string }>, value?: string) => {
  return options.find((item) => item.value === value)?.label || value || '-'
}

const auditTagType = (value?: string) => {
  if (value === 'PASS') return 'success'
  if (value === 'REJECT') return 'danger'
  if (value === 'MANUAL_REVIEW') return 'warning'
  return 'info'
}

const previewUrl = (asset: AigcAssetVO) => asset.thumbnailUrl || asset.coverUrl || asset.fileUrl || ''
const isImage = (asset: AigcAssetVO) => asset.assetType === 'IMAGE' && !!previewUrl(asset)
const assetSize = (asset: AigcAssetVO) => (asset.width && asset.height ? `${asset.width} x ${asset.height}` : '-')

const formatFileSize = (size?: number) => {
  if (!size) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

const formatJson = (value?: string) => {
  if (!value) return '-'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

onMounted(() => getAssetList())
</script>

<style scoped>
.detail-json {
  max-height: 360px;
  overflow: auto;
  padding: 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  background: var(--el-fill-color-light);
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>
