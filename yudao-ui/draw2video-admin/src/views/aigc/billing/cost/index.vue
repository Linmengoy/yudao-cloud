<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :inline="true" :model="queryParams" class="-mb-15px" label-width="80px">
      <el-form-item label="用户编号" prop="userId"><el-input v-model="queryParams.userId" class="!w-220px" clearable placeholder="请输入用户编号" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="模型编号" prop="modelId"><el-input v-model="queryParams.modelId" class="!w-220px" clearable placeholder="请输入模型编号" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="供应商" prop="providerId"><el-input v-model="queryParams.providerId" class="!w-220px" clearable placeholder="请输入供应商编号" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item><el-button @click="handleQuery"><Icon class="mr-5px" icon="ep:search" />搜索</el-button><el-button @click="resetQuery"><Icon class="mr-5px" icon="ep:refresh" />重置</el-button><el-button v-hasPermi="['aigc:billing:cost:export']" :loading="exportLoading" plain type="success" @click="handleExport"><Icon class="mr-5px" icon="ep:download" />导出</el-button></el-form-item>
    </el-form>
  </ContentWrap>
  <ContentWrap>
    <el-row :gutter="16" class="mb-16px">
      <el-col :span="6"><el-card shadow="never"><div class="text-gray-500 text-13px">销售积分</div><div class="mt-8px text-22px font-bold">{{ formatPoints(summary.saleAmount) }}</div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="text-gray-500 text-13px">成本积分</div><div class="mt-8px text-22px font-bold">{{ formatPoints(summary.costAmount) }}</div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="text-gray-500 text-13px">毛利</div><div class="mt-8px text-22px font-bold">{{ formatPoints(summary.grossProfit) }}</div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="text-gray-500 text-13px">毛利率</div><div class="mt-8px text-22px font-bold">{{ formatPercent(summary.grossProfitRate) }}</div></el-card></el-col>
    </el-row>
    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" :stripe="true">
      <el-table-column align="center" label="任务编号" prop="taskNo" min-width="160" />
      <el-table-column align="center" label="用户编号" prop="userId" width="120" />
      <el-table-column align="center" label="模型编号" prop="modelId" width="120" />
      <el-table-column align="center" label="供应商" prop="providerId" width="120" />
      <el-table-column align="center" label="能力" prop="capability" width="120" />
      <el-table-column align="center" label="用量" prop="usageAmount" width="120" />
      <el-table-column align="center" label="销售积分" width="130"><template #default="scope">{{ formatPoints(scope.row.saleAmount) }}</template></el-table-column>
      <el-table-column align="center" label="成本积分" width="130"><template #default="scope">{{ formatPoints(scope.row.costAmount) }}</template></el-table-column>
      <el-table-column align="center" label="毛利" width="130"><template #default="scope">{{ formatPoints(scope.row.grossProfit) }}</template></el-table-column>
      <el-table-column align="center" label="毛利率" width="120"><template #default="scope">{{ formatPercent(scope.row.grossProfitRate) }}</template></el-table-column>
      <el-table-column :formatter="dateFormatter" align="center" label="创建时间" prop="createTime" width="180" />
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import { AigcBillingCostApi, AigcCostRecordVO } from '@/api/aigc/billing/cost'
import { formatPercent, formatPoints } from '../utils'

defineOptions({ name: 'AigcBillingCost' })

const message = useMessage()
const loading = ref(true)
const exportLoading = ref(false)
const list = ref<AigcCostRecordVO[]>([])
const total = ref(0)
const summary = ref({ saleAmount: 0, costAmount: 0, grossProfit: 0, grossProfitRate: 0 })
const queryFormRef = ref()
const queryParams = reactive({ pageNo: 1, pageSize: 10, userId: undefined, modelId: undefined, providerId: undefined })
const getList = async () => {
  loading.value = true
  try {
    const [pageData, summaryData] = await Promise.all([AigcBillingCostApi.getCostPage(queryParams), AigcBillingCostApi.getCostSummary(queryParams).catch(() => summary.value)])
    list.value = pageData.list
    total.value = pageData.total
    summary.value = { ...summary.value, ...summaryData }
  } finally { loading.value = false }
}
const handleQuery = () => { queryParams.pageNo = 1; getList() }
const resetQuery = () => { queryFormRef.value.resetFields(); handleQuery() }
const handleExport = async () => { try { await message.exportConfirm(); exportLoading.value = true; const data = await AigcBillingCostApi.exportCost(queryParams); download.excel(data, 'AIGC成本记录.xls') } finally { exportLoading.value = false } }
onMounted(() => getList())
</script>
