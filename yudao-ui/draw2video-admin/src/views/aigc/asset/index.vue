<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" label-width="88px" class="-mb-15px">
      <el-form-item :label="t('aigc.asset.fields.userId')" prop="userId">
        <el-input-number v-model="queryParams.userId" :min="1" controls-position="right" class="!w-180px" clearable />
      </el-form-item>
      <el-form-item :label="t('aigc.asset.fields.title')" prop="title">
        <el-input v-model="queryParams.title" :placeholder="t('aigc.asset.placeholders.title')" clearable class="!w-220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item :label="t('aigc.asset.fields.type')" prop="assetType">
        <el-select v-model="queryParams.assetType" :placeholder="t('aigc.asset.placeholders.allTypes')" clearable class="!w-160px">
          <el-option v-for="item in assetTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('aigc.asset.fields.audit')" prop="auditStatus">
        <el-select v-model="queryParams.auditStatus" :placeholder="t('aigc.asset.placeholders.allAuditStates')" clearable class="!w-170px">
          <el-option v-for="item in auditStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />{{ t('common.query') }}</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />{{ t('common.reset') }}</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane :label="t('aigc.asset.tabs.assets')" name="assets">
        <el-table v-loading="assetLoading" :data="assetList" :stripe="true" :show-overflow-tooltip="true">
          <el-table-column :label="t('aigc.asset.fields.preview')" width="96" fixed="left">
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
          <el-table-column :label="t('aigc.asset.fields.assetNo')" prop="assetNo" width="170" />
          <el-table-column :label="t('aigc.asset.fields.userId')" prop="userId" width="100" />
          <el-table-column :label="t('aigc.asset.fields.title')" prop="title" min-width="180" />
          <el-table-column :label="t('aigc.asset.fields.type')" prop="assetType" width="100">
            <template #default="{ row }">{{ optionLabel(assetTypeOptions, row.assetType) }}</template>
          </el-table-column>
          <el-table-column :label="t('aigc.asset.fields.source')" prop="sourceType" width="110">
            <template #default="{ row }">{{ optionLabel(sourceTypeOptions, row.sourceType) }}</template>
          </el-table-column>
          <el-table-column :label="t('aigc.asset.fields.taskNo')" prop="taskNo" width="160" />
          <el-table-column :label="t('aigc.asset.fields.audit')" prop="auditStatus" width="120">
            <template #default="{ row }">
              <el-tag :type="auditTagType(row.auditStatus)">{{ optionLabel(auditStatusOptions, row.auditStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('aigc.asset.fields.visibility')" prop="visibility" width="110">
            <template #default="{ row }">{{ optionLabel(visibilityOptions, row.visibility) }}</template>
          </el-table-column>
          <el-table-column :label="t('aigc.asset.fields.downloads')" prop="downloadCount" width="100" />
          <el-table-column :label="t('common.createTime')" prop="createTime" :formatter="dateFormatter" width="180" />
          <el-table-column :label="t('table.action')" width="220" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openAssetDetail(row)" v-hasPermi="['aigc:asset:query']">{{ t('action.detail') }}</el-button>
              <el-button link type="success" @click="handleAudit(row, 'PASS')" v-hasPermi="['aigc:asset:audit']">{{ t('aigc.asset.audit.pass') }}</el-button>
              <el-button link type="warning" @click="handleAudit(row, 'REJECT')" v-hasPermi="['aigc:asset:audit']">{{ t('aigc.asset.audit.reject') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination :total="assetTotal" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getAssetList" />
      </el-tab-pane>

      <el-tab-pane :label="t('aigc.asset.tabs.projects')" name="projects">
        <el-table v-loading="projectLoading" :data="projectList" :stripe="true" :show-overflow-tooltip="true">
          <el-table-column :label="t('aigc.asset.fields.cover')" width="96" fixed="left">
            <template #default="{ row }">
              <el-image
                v-if="row.coverUrl"
                :src="row.coverUrl"
                fit="cover"
                class="h-56px w-72px rounded border border-[var(--el-border-color)]"
                :preview-src-list="[row.coverUrl]"
                preview-teleported
              />
              <el-tag v-else>{{ t('aigc.asset.labels.project') }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('aigc.asset.fields.projectId')" prop="id" width="110" />
          <el-table-column :label="t('aigc.asset.fields.userId')" prop="ownerUserId" width="110" />
          <el-table-column :label="t('aigc.asset.fields.name')" prop="name" min-width="200" />
          <el-table-column :label="t('aigc.asset.fields.version')" prop="currentVersion" width="90" />
          <el-table-column :label="t('aigc.asset.fields.nodes')" prop="nodeCount" width="90" />
          <el-table-column :label="t('aigc.asset.fields.assets')" prop="assetCount" width="90" />
          <el-table-column :label="t('common.status')" prop="status" width="110" />
          <el-table-column :label="t('common.updateTime')" prop="updateTime" :formatter="dateFormatter" width="180" />
        </el-table>
        <Pagination :total="projectTotal" v-model:page="projectQuery.pageNo" v-model:limit="projectQuery.pageSize" @pagination="getProjectList" />
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>

  <el-dialog v-model="detailVisible" :title="t('aigc.asset.detail.title')" width="860px">
    <el-descriptions v-if="currentAsset" :column="2" border>
      <el-descriptions-item :label="t('aigc.asset.fields.assetNo')">{{ currentAsset.assetNo }}</el-descriptions-item>
      <el-descriptions-item :label="t('aigc.asset.fields.userId')">{{ currentAsset.userId }}</el-descriptions-item>
      <el-descriptions-item :label="t('aigc.asset.fields.title')">{{ currentAsset.title || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="t('aigc.asset.fields.taskNo')">{{ currentAsset.taskNo || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="t('aigc.asset.fields.size')">{{ assetSize(currentAsset) }}</el-descriptions-item>
      <el-descriptions-item :label="t('aigc.asset.fields.fileSize')">{{ formatFileSize(currentAsset.fileSize) }}</el-descriptions-item>
      <el-descriptions-item :label="t('aigc.asset.fields.tags')" :span="2">{{ currentAsset.tags || '-' }}</el-descriptions-item>
      <el-descriptions-item :label="t('aigc.asset.fields.description')" :span="2">{{ currentAsset.description || '-' }}</el-descriptions-item>
    </el-descriptions>
    <el-tabs v-if="currentAsset" class="mt-16px">
      <el-tab-pane :label="t('aigc.asset.detail.files')">
        <el-table :data="currentAsset.files || []" size="small">
          <el-table-column :label="t('aigc.asset.fields.role')" prop="fileRole" width="110" />
          <el-table-column :label="t('aigc.asset.fields.fileName')" prop="fileName" min-width="180" />
          <el-table-column :label="t('aigc.asset.fields.mime')" prop="mimeType" width="150" />
          <el-table-column :label="t('aigc.asset.fields.size')" width="110">
            <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
          </el-table-column>
          <el-table-column :label="t('aigc.asset.fields.accessUrl')" prop="accessUrl" min-width="220" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane :label="t('aigc.asset.detail.promptSnapshot')">
        <pre class="detail-json">{{ formatJson(currentAsset.promptSnapshot) }}</pre>
      </el-tab-pane>
      <el-tab-pane :label="t('aigc.asset.detail.generationSnapshot')">
        <pre class="detail-json">{{ formatJson(currentAsset.generateSnapshot) }}</pre>
      </el-tab-pane>
      <el-tab-pane :label="t('aigc.asset.detail.metadata')">
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
const { t } = useI18n()
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

const assetTypeOptions = computed(() => [
  { label: t('aigc.asset.assetTypes.image'), value: 'IMAGE' },
  { label: t('aigc.asset.assetTypes.video'), value: 'VIDEO' },
  { label: t('aigc.asset.assetTypes.audio'), value: 'AUDIO' },
  { label: t('aigc.asset.assetTypes.text'), value: 'TEXT' },
  { label: t('aigc.asset.assetTypes.other'), value: 'OTHER' }
])
const sourceTypeOptions = computed(() => [
  { label: t('aigc.asset.sourceTypes.generate'), value: 'GENERATE' },
  { label: t('aigc.asset.sourceTypes.upload'), value: 'UPLOAD' },
  { label: t('aigc.asset.sourceTypes.import'), value: 'IMPORT' }
])
const auditStatusOptions = computed(() => [
  { label: t('aigc.asset.auditStatus.pending'), value: 'PENDING' },
  { label: t('aigc.asset.auditStatus.pass'), value: 'PASS' },
  { label: t('aigc.asset.auditStatus.reject'), value: 'REJECT' },
  { label: t('aigc.asset.auditStatus.manualReview'), value: 'MANUAL_REVIEW' }
])
const visibilityOptions = computed(() => [
  { label: t('aigc.asset.visibility.private'), value: 'PRIVATE' },
  { label: t('aigc.asset.visibility.public'), value: 'PUBLIC' }
])

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
  const auditReason = auditStatus === 'REJECT' ? t('aigc.asset.audit.rejectedByAdmin') : undefined
  await AigcAssetApi.updateAuditStatus({ id: asset.id, auditStatus, auditReason })
  message.success(t('aigc.asset.messages.auditUpdated'))
  await getAssetList()
}

const optionLabel = (options: Array<{ label: string; value: string }> | { value: Array<{ label: string; value: string }> }, value?: string) => {
  const list = Array.isArray(options) ? options : options.value
  return list.find((item) => item.value === value)?.label || value || '-'
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
