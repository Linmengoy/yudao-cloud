<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :inline="true" :model="queryParams" class="-mb-15px" label-width="80px">
      <el-form-item label="充值单号" prop="rechargeNo"><el-input v-model="queryParams.rechargeNo" class="!w-220px" clearable placeholder="请输入充值单号" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="用户编号" prop="userId"><el-input v-model="queryParams.userId" class="!w-220px" clearable placeholder="请输入用户编号" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="Pay单号" prop="payOrderNo"><el-input v-model="queryParams.payOrderNo" class="!w-220px" clearable placeholder="请输入 Pay 订单号" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="支付渠道" prop="payChannelCode"><el-input v-model="queryParams.payChannelCode" class="!w-180px" clearable placeholder="请输入支付渠道" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="订单状态" prop="status"><el-select v-model="queryParams.status" class="!w-180px" clearable placeholder="请选择订单状态"><el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      <el-form-item label="创建时间" prop="createTime"><el-date-picker v-model="queryParams.createTime" :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]" class="!w-240px" end-placeholder="结束日期" start-placeholder="开始日期" type="daterange" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item>
      <el-form-item label="支付时间" prop="payTime"><el-date-picker v-model="queryParams.payTime" :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]" class="!w-240px" end-placeholder="结束日期" start-placeholder="开始日期" type="daterange" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item>
      <el-form-item><el-button @click="handleQuery"><Icon class="mr-5px" icon="ep:search" />搜索</el-button><el-button @click="resetQuery"><Icon class="mr-5px" icon="ep:refresh" />重置</el-button><el-button v-hasPermi="['aigc:billing:recharge:export']" :loading="exportLoading" plain type="success" @click="handleExport"><Icon class="mr-5px" icon="ep:download" />导出</el-button></el-form-item>
    </el-form>
  </ContentWrap>
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" :stripe="true">
      <el-table-column align="center" label="充值单号" prop="rechargeNo" min-width="180" />
      <el-table-column align="center" label="用户编号" prop="userId" width="120" />
      <el-table-column align="center" label="支付金额" width="120"><template #default="scope">{{ formatMoney(scope.row.payAmount) }}</template></el-table-column>
      <el-table-column align="center" label="到账积分" width="130"><template #default="scope">{{ formatPoints(scope.row.pointAmount) }}</template></el-table-column>
      <el-table-column align="center" label="赠送积分" width="130"><template #default="scope">{{ formatPoints(scope.row.giftAmount) }}</template></el-table-column>
      <el-table-column align="center" label="合计积分" width="130"><template #default="scope">{{ formatPoints(scope.row.totalPointAmount) }}</template></el-table-column>
      <el-table-column align="center" label="支付渠道" prop="payChannelCode" width="120" />
      <el-table-column align="center" label="状态" width="120"><template #default="scope">{{ mapText(rechargeStatusMap, scope.row.status) }}</template></el-table-column>
      <el-table-column :formatter="dateFormatter" align="center" label="支付时间" prop="payTime" width="180" />
      <el-table-column align="center" fixed="right" label="操作" width="180"><template #default="scope"><el-button link type="primary" @click="handleDetail(scope.row)">详情</el-button><el-button link type="warning" @click="handleDiagnostic(scope.row.id)">排障</el-button><el-button v-if="String(scope.row.status) === 'WAIT_PAY' || String(scope.row.status) === '1'" v-hasPermi="['aigc:billing:recharge:update']" link type="danger" @click="handleClose(scope.row.id)">关闭</el-button></template></el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <el-dialog v-model="detailVisible" title="充值订单详情" width="760px">
    <el-descriptions v-if="detailData" :column="2" border>
      <el-descriptions-item label="充值单号">{{ detailData.rechargeNo }} <el-button link type="primary" @click="copyText(detailData.rechargeNo)">复制</el-button></el-descriptions-item>
      <el-descriptions-item label="用户编号">{{ detailData.userId }}</el-descriptions-item>
      <el-descriptions-item label="支付金额">{{ formatMoney(detailData.payAmount) }}</el-descriptions-item>
      <el-descriptions-item label="合计积分">{{ formatPoints(detailData.totalPointAmount) }}</el-descriptions-item>
      <el-descriptions-item label="Pay 订单 ID">{{ detailData.payOrderId || '-' }} <el-button v-if="detailData.payOrderId" link type="primary" @click="copyText(String(detailData.payOrderId))">复制</el-button></el-descriptions-item>
      <el-descriptions-item label="Pay 订单号">{{ detailData.payOrderNo || '-' }} <el-button v-if="detailData.payOrderNo" link type="primary" @click="copyText(detailData.payOrderNo)">复制</el-button></el-descriptions-item>
      <el-descriptions-item label="支付渠道">{{ detailData.payChannelCode || '-' }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ mapText(rechargeStatusMap, detailData.status) }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ formatNullableDate(detailData.createTime) }}</el-descriptions-item>
      <el-descriptions-item label="支付时间">{{ formatNullableDate(detailData.payTime) }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>

  <el-dialog v-model="diagnosticVisible" title="充值支付链路排障" width="900px">
    <el-alert v-if="diagnosticData?.diagnosticMessage" :closable="false" :title="diagnosticData.diagnosticMessage" class="mb-16px" type="info" />
    <el-descriptions v-if="diagnosticData" :column="2" border>
      <el-descriptions-item label="Pay 单匹配">{{ diagnosticData.payOrderMatched ? '是' : '否' }}</el-descriptions-item>
      <el-descriptions-item label="金额匹配">{{ diagnosticData.amountMatched ? '是' : '否' }}</el-descriptions-item>
      <el-descriptions-item label="Pay 已成功">{{ diagnosticData.paySuccess ? '是' : '否' }}</el-descriptions-item>
      <el-descriptions-item label="已生成流水">{{ diagnosticData.billingRecordExists ? '是' : '否' }}</el-descriptions-item>
      <el-descriptions-item label="Pay 订单 ID">{{ diagnosticData.payOrder?.id || '-' }}</el-descriptions-item>
      <el-descriptions-item label="Pay 状态">{{ diagnosticData.payOrder?.status ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="商户订单号">{{ diagnosticData.payOrder?.merchantOrderId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="Pay 金额">{{ formatMoney(diagnosticData.payOrder?.price) }}</el-descriptions-item>
      <el-descriptions-item label="通知任务 ID">{{ diagnosticData.payNotify?.task?.id || '-' }}</el-descriptions-item>
      <el-descriptions-item label="通知状态">{{ diagnosticData.payNotify?.task?.status ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="通知次数">{{ diagnosticData.payNotify?.task?.notifyTimes ?? '-' }} / {{ diagnosticData.payNotify?.task?.maxNotifyTimes ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="下次通知时间">{{ formatNullableDate(diagnosticData.payNotify?.task?.nextNotifyTime) }}</el-descriptions-item>
    </el-descriptions>
    <el-table v-if="diagnosticData?.payNotify?.logs?.length" :data="diagnosticData.payNotify.logs" class="mt-16px" max-height="260">
      <el-table-column label="日志 ID" prop="id" width="100" />
      <el-table-column label="通知次数" prop="notifyTimes" width="100" />
      <el-table-column label="状态" prop="status" width="100" />
      <el-table-column label="响应" prop="response" min-width="240" />
      <el-table-column :formatter="dateFormatter" label="创建时间" prop="createTime" width="180" />
    </el-table>
  </el-dialog>
</template>

<script lang="ts" setup>
import { dateFormatter, formatNullableDate } from '@/utils/formatTime'
import download from '@/utils/download'
import { AigcBillingRechargeApi, AigcRechargeOrderDiagnosticVO, AigcRechargeOrderVO } from '@/api/aigc/billing/recharge'
import { formatMoney, formatPoints, mapText, rechargeStatusMap } from '../utils'

defineOptions({ name: 'AigcBillingRecharge' })

const message = useMessage()
const loading = ref(true)
const exportLoading = ref(false)
const list = ref<AigcRechargeOrderVO[]>([])
const total = ref(0)
const detailVisible = ref(false)
const diagnosticVisible = ref(false)
const detailData = ref<AigcRechargeOrderVO>()
const diagnosticData = ref<AigcRechargeOrderDiagnosticVO>()
const queryFormRef = ref()
const queryParams = reactive({ pageNo: 1, pageSize: 10, rechargeNo: undefined, userId: undefined, payOrderNo: undefined, payChannelCode: undefined, status: undefined, createTime: [], payTime: [] })
const statusOptions = Object.entries(rechargeStatusMap).filter(([key]) => Number.isNaN(Number(key))).map(([value, label]) => ({ value, label }))
const getList = async () => {
  loading.value = true
  try { const data = await AigcBillingRechargeApi.getRechargePage(queryParams); list.value = data.list; total.value = data.total } finally { loading.value = false }
}
const handleQuery = () => { queryParams.pageNo = 1; getList() }
const resetQuery = () => { queryFormRef.value.resetFields(); handleQuery() }
const handleDetail = (row: AigcRechargeOrderVO) => { detailData.value = row; detailVisible.value = true }
const handleDiagnostic = async (id: number) => { diagnosticData.value = await AigcBillingRechargeApi.getDiagnostic(id); diagnosticVisible.value = true }
const handleClose = async (id: number) => { await message.confirm('确认关闭该充值订单吗？'); await AigcBillingRechargeApi.closeRecharge(id); message.success('关闭成功'); getList() }
const handleExport = async () => { try { await message.exportConfirm(); exportLoading.value = true; const data = await AigcBillingRechargeApi.exportRecharge(queryParams); download.excel(data, 'AIGC充值订单.xls') } finally { exportLoading.value = false } }
const copyText = async (text?: string) => { if (!text) return; await navigator.clipboard.writeText(text); message.success('复制成功') }
onMounted(() => getList())
</script>
