<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :inline="true" :model="queryParams" class="-mb-15px" label-width="80px">
      <el-form-item label="用户编号" prop="userId"><el-input v-model="queryParams.userId" class="!w-220px" clearable placeholder="请输入用户编号" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="订单状态" prop="status"><el-select v-model="queryParams.status" class="!w-180px" clearable placeholder="请选择订单状态"><el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      <el-form-item><el-button @click="handleQuery"><Icon class="mr-5px" icon="ep:search" />搜索</el-button><el-button @click="resetQuery"><Icon class="mr-5px" icon="ep:refresh" />重置</el-button><el-button v-hasPermi="['aigc:billing:recharge:export']" :loading="exportLoading" plain type="success" @click="handleExport"><Icon class="mr-5px" icon="ep:download" />导出</el-button></el-form-item>
    </el-form>
  </ContentWrap>
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" :stripe="true">
      <el-table-column align="center" label="充值单号" prop="rechargeNo" min-width="180" />
      <el-table-column align="center" label="用户编号" prop="userId" width="120" />
      <el-table-column align="center" label="支付金额" prop="payAmount" width="120" />
      <el-table-column align="center" label="到账积分" width="130"><template #default="scope">{{ formatPoints(scope.row.pointAmount) }}</template></el-table-column>
      <el-table-column align="center" label="赠送积分" width="130"><template #default="scope">{{ formatPoints(scope.row.giftAmount) }}</template></el-table-column>
      <el-table-column align="center" label="合计积分" width="130"><template #default="scope">{{ formatPoints(scope.row.totalPointAmount) }}</template></el-table-column>
      <el-table-column align="center" label="支付渠道" prop="payChannelCode" width="120" />
      <el-table-column align="center" label="状态" width="120"><template #default="scope">{{ mapText(rechargeStatusMap, scope.row.status) }}</template></el-table-column>
      <el-table-column :formatter="dateFormatter" align="center" label="支付时间" prop="payTime" width="180" />
      <el-table-column align="center" fixed="right" label="操作" width="100"><template #default="scope"><el-button v-hasPermi="['aigc:billing:recharge:update']" link type="danger" @click="handleClose(scope.row.id)">关闭</el-button></template></el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import { AigcBillingRechargeApi, AigcRechargeOrderVO } from '@/api/aigc/billing/recharge'
import { formatPoints, mapText, rechargeStatusMap } from '../utils'

defineOptions({ name: 'AigcBillingRecharge' })

const message = useMessage()
const loading = ref(true)
const exportLoading = ref(false)
const list = ref<AigcRechargeOrderVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive({ pageNo: 1, pageSize: 10, userId: undefined, status: undefined })
const statusOptions = Object.entries(rechargeStatusMap).filter(([key]) => Number.isNaN(Number(key))).map(([value, label]) => ({ value, label }))
const getList = async () => {
  loading.value = true
  try { const data = await AigcBillingRechargeApi.getRechargePage(queryParams); list.value = data.list; total.value = data.total } finally { loading.value = false }
}
const handleQuery = () => { queryParams.pageNo = 1; getList() }
const resetQuery = () => { queryFormRef.value.resetFields(); handleQuery() }
const handleClose = async (id: number) => { await message.confirm('确认关闭该充值订单吗？'); await AigcBillingRechargeApi.closeRecharge(id); message.success('关闭成功'); getList() }
const handleExport = async () => { try { await message.exportConfirm(); exportLoading.value = true; const data = await AigcBillingRechargeApi.exportRecharge(queryParams); download.excel(data, 'AIGC充值订单.xls') } finally { exportLoading.value = false } }
onMounted(() => getList())
</script>
