<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :inline="true" :model="queryParams" class="-mb-15px" label-width="80px">
      <el-form-item label="用户编号" prop="userId">
        <el-input v-model="queryParams.userId" class="!w-220px" clearable placeholder="请输入用户编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="钱包状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-180px" clearable placeholder="请选择钱包状态">
          <el-option label="正常" :value="1" />
          <el-option label="冻结" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon class="mr-5px" icon="ep:search" />搜索</el-button>
        <el-button @click="resetQuery"><Icon class="mr-5px" icon="ep:refresh" />重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" :stripe="true">
      <el-table-column align="center" label="钱包编号" prop="id" width="100" />
      <el-table-column align="center" label="用户编号" prop="userId" width="120" />
      <el-table-column align="center" label="可用积分" min-width="130">
        <template #default="scope">{{ formatPoints(scope.row.balance) }}</template>
      </el-table-column>
      <el-table-column align="center" label="冻结积分" min-width="130">
        <template #default="scope">{{ formatPoints(scope.row.frozenBalance) }}</template>
      </el-table-column>
      <el-table-column align="center" label="累计充值" min-width="130">
        <template #default="scope">{{ formatPoints(scope.row.totalRecharge) }}</template>
      </el-table-column>
      <el-table-column align="center" label="累计赠送" min-width="130">
        <template #default="scope">{{ formatPoints(scope.row.totalGift) }}</template>
      </el-table-column>
      <el-table-column align="center" label="累计消费" min-width="130">
        <template #default="scope">{{ formatPoints(scope.row.totalConsume) }}</template>
      </el-table-column>
      <el-table-column align="center" label="累计退款" min-width="130">
        <template #default="scope">{{ formatPoints(scope.row.totalRefund) }}</template>
      </el-table-column>
      <el-table-column :formatter="dateFormatter" align="center" label="最后交易时间" prop="lastTransTime" width="180" />
      <el-table-column align="center" fixed="right" label="操作" width="180">
        <template #default="scope">
          <el-button v-hasPermi="['aigc:billing:wallet:update']" link type="primary" @click="openAdjustForm(scope.row.userId)">调整</el-button>
          <el-button v-hasPermi="['aigc:billing:wallet:gift']" link type="success" @click="openGiftForm(scope.row.userId)">赠送</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>

  <WalletAdjustForm ref="adjustFormRef" @success="getList" />
  <WalletGiftForm ref="giftFormRef" @success="getList" />
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { AigcBillingWalletApi, AigcWalletVO } from '@/api/aigc/billing/wallet'
import { formatPoints } from '../utils'
import WalletAdjustForm from './WalletAdjustForm.vue'
import WalletGiftForm from './WalletGiftForm.vue'

defineOptions({ name: 'AigcBillingWallet' })

const loading = ref(true)
const list = ref<AigcWalletVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive({ pageNo: 1, pageSize: 10, userId: undefined, status: undefined })

const getList = async () => {
  loading.value = true
  try {
    const data = await AigcBillingWalletApi.getWalletPage(queryParams)
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

const adjustFormRef = ref()
const openAdjustForm = (userId: number) => adjustFormRef.value.open(userId)
const giftFormRef = ref()
const openGiftForm = (userId: number) => giftFormRef.value.open(userId)

onMounted(() => getList())
</script>
