<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="-mb-15px" label-width="90px">
      <el-form-item label="对象类型" prop="objectType">
        <el-select v-model="queryParams.objectType" class="!w-180px" clearable placeholder="请选择对象类型">
          <el-option v-for="item in AIGC_AUDIT_OBJECT_TYPES" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="对象编号" prop="objectId">
        <el-input v-model="queryParams.objectId" class="!w-180px" clearable placeholder="请输入对象编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="审核场景" prop="scene">
        <el-select v-model="queryParams.scene" class="!w-180px" clearable placeholder="请选择审核场景">
          <el-option v-for="item in AIGC_SAFETY_SCENES" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="审核状态" prop="auditStatus">
        <el-select v-model="queryParams.auditStatus" class="!w-180px" clearable placeholder="请选择审核状态">
          <el-option v-for="item in AIGC_AUDIT_STATUSES" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="审核结果" prop="auditResult">
        <el-select v-model="queryParams.auditResult" class="!w-180px" clearable placeholder="请选择审核结果">
          <el-option v-for="item in AIGC_AUDIT_RESULTS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="风险等级" prop="riskLevel">
        <el-select v-model="queryParams.riskLevel" class="!w-160px" clearable placeholder="请选择风险等级">
          <el-option v-for="item in AIGC_RISK_LEVELS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间" prop="createTime">
        <el-date-picker v-model="queryParams.createTime" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss" start-placeholder="开始日期" end-placeholder="结束日期" class="!w-360px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="审核编号" align="center" prop="id" width="110" />
      <el-table-column label="对象类型" align="center" prop="objectType" width="120">
        <template #default="scope">{{ getOptionLabel(AIGC_AUDIT_OBJECT_TYPES, scope.row.objectType) }}</template>
      </el-table-column>
      <el-table-column label="对象编号" align="center" prop="objectId" width="120" />
      <el-table-column label="内容摘要" align="center" prop="contentSummary" min-width="220" />
      <el-table-column label="审核场景" align="center" prop="scene" width="120">
        <template #default="scope">{{ getOptionLabel(AIGC_SAFETY_SCENES, scope.row.scene) }}</template>
      </el-table-column>
      <el-table-column label="审核状态" align="center" prop="auditStatus" width="120">
        <template #default="scope"><el-tag :type="getAuditStatusTag(scope.row.auditStatus)">{{ getOptionLabel(AIGC_AUDIT_STATUSES, scope.row.auditStatus) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="审核结果" align="center" prop="auditResult" width="120">
        <template #default="scope">{{ getOptionLabel(AIGC_AUDIT_RESULTS, scope.row.auditResult) }}</template>
      </el-table-column>
      <el-table-column label="风险等级" align="center" prop="riskLevel" width="100">
        <template #default="scope"><el-tag v-if="scope.row.riskLevel" :type="scope.row.riskLevel >= 4 ? 'danger' : 'warning'">{{ scope.row.riskLevel }}</el-tag><span v-else>-</span></template>
      </el-table-column>
      <el-table-column label="审核时间" align="center" prop="auditTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="操作" align="center" width="220" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row.id)">详情</el-button>
          <el-button v-if="scope.row.auditStatus === 'PENDING'" link type="success" @click="handlePass(scope.row.id)" v-hasPermi="['aigc:safety-audit-record:audit']">通过</el-button>
          <el-button v-if="scope.row.auditStatus === 'PENDING'" link type="danger" @click="openReject(scope.row.id)" v-hasPermi="['aigc:safety-audit-record:audit']">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <el-drawer v-model="detailVisible" title="审核记录详情" size="560px">
    <el-descriptions v-loading="detailLoading" :column="1" border>
      <el-descriptions-item label="审核编号">{{ detailData.id || '-' }}</el-descriptions-item>
      <el-descriptions-item label="对象类型">{{ getOptionLabel(AIGC_AUDIT_OBJECT_TYPES, detailData.objectType) }}</el-descriptions-item>
      <el-descriptions-item label="对象编号">{{ detailData.objectId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="审核场景">{{ getOptionLabel(AIGC_SAFETY_SCENES, detailData.scene) }}</el-descriptions-item>
      <el-descriptions-item label="审核状态">{{ getOptionLabel(AIGC_AUDIT_STATUSES, detailData.auditStatus) }}</el-descriptions-item>
      <el-descriptions-item label="审核结果">{{ getOptionLabel(AIGC_AUDIT_RESULTS, detailData.auditResult) }}</el-descriptions-item>
      <el-descriptions-item label="风险等级">{{ detailData.riskLevel || '-' }}</el-descriptions-item>
      <el-descriptions-item label="命中敏感词">
        <el-tag v-for="word in hitWordList" :key="word" class="mr-5px">{{ word }}</el-tag>
        <span v-if="hitWordList.length === 0">-</span>
      </el-descriptions-item>
      <el-descriptions-item label="内容摘要">{{ detailData.contentSummary || '-' }}</el-descriptions-item>
      <el-descriptions-item label="拒绝原因">{{ detailData.rejectReason || '-' }}</el-descriptions-item>
      <el-descriptions-item label="审核人">{{ detailData.auditorUserId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="审核时间">{{ detailData.auditTime ? formatDate(detailData.auditTime) : '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ detailData.createTime ? formatDate(detailData.createTime) : '-' }}</el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button v-if="detailData.auditStatus === 'PENDING'" type="success" @click="handlePass(detailData.id!)" v-hasPermi="['aigc:safety-audit-record:audit']">通过</el-button>
      <el-button v-if="detailData.auditStatus === 'PENDING'" type="danger" @click="openReject(detailData.id!)" v-hasPermi="['aigc:safety-audit-record:audit']">拒绝</el-button>
      <el-button @click="detailVisible = false">关闭</el-button>
    </template>
  </el-drawer>

  <Dialog title="人工审核拒绝" v-model="rejectVisible" width="520px">
    <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="90px">
      <el-form-item label="拒绝原因" prop="reason">
        <el-input v-model="rejectForm.reason" type="textarea" :rows="4" maxlength="200" show-word-limit placeholder="请输入拒绝原因" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitReject" type="primary">确 定</el-button>
      <el-button @click="rejectVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { dateFormatter, formatDate } from '@/utils/formatTime'
import { AigcAuditRecordApi } from '@/api/aigc/safety/audit-record'
import type { AigcAuditRecordRespVO } from '@/api/aigc/safety/types'
import { AIGC_AUDIT_OBJECT_TYPES, AIGC_AUDIT_RESULTS, AIGC_AUDIT_STATUSES, AIGC_RISK_LEVELS, AIGC_SAFETY_SCENES, getOptionLabel } from '../constants'

defineOptions({ name: 'AigcAuditRecord' })

const message = useMessage()
const loading = ref(true)
const list = ref<AigcAuditRecordRespVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive({ pageNo: 1, pageSize: 10, objectType: undefined, objectId: undefined, scene: undefined, auditStatus: undefined, auditResult: undefined, riskLevel: undefined, createTime: undefined })
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<AigcAuditRecordRespVO>({})
const rejectVisible = ref(false)
const rejectFormRef = ref()
const rejectForm = reactive({ auditId: undefined as number | undefined, reason: undefined as string | undefined })
const rejectRules = reactive({ reason: [{ required: true, message: '拒绝原因不能为空', trigger: 'blur' }] })

const hitWordList = computed(() => {
  if (!detailData.value.hitWords) return []
  try {
    const words = JSON.parse(detailData.value.hitWords)
    return Array.isArray(words) ? words : [detailData.value.hitWords]
  } catch {
    return [detailData.value.hitWords]
  }
})

const getList = async () => {
  loading.value = true
  try {
    const data = await AigcAuditRecordApi.getAuditRecordPage(queryParams)
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

const openDetail = async (id: number) => {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detailData.value = await AigcAuditRecordApi.getAuditRecord(id)
  } finally {
    detailLoading.value = false
  }
}

const handlePass = async (id: number) => {
  if (!id) return
  await message.confirm('确认通过该审核记录吗？')
  await AigcAuditRecordApi.markPass({ auditId: id, remark: '内容符合平台规范' })
  message.success('审核通过成功')
  await getList()
  if (detailVisible.value) await openDetail(id)
}

const openReject = (id: number) => {
  if (!id) return
  rejectForm.auditId = id
  rejectForm.reason = undefined
  rejectVisible.value = true
  rejectFormRef.value?.resetFields()
}

const submitReject = async () => {
  await rejectFormRef.value.validate()
  if (!rejectForm.auditId || !rejectForm.reason) return
  const auditId = rejectForm.auditId
  await AigcAuditRecordApi.markReject({ auditId, reason: rejectForm.reason })
  message.success('审核拒绝成功')
  rejectVisible.value = false
  await getList()
  if (detailVisible.value) await openDetail(auditId)
}

const getAuditStatusTag = (status?: string) => {
  if (status === 'PASS') return 'success'
  if (status === 'REJECT') return 'danger'
  return 'warning'
}

onMounted(() => getList())
</script>
