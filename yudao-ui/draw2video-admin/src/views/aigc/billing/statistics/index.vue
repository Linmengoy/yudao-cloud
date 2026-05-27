<template>
  <ContentWrap>
    <el-form :inline="true" :model="queryParams" class="-mb-15px" label-width="80px">
      <el-form-item label="统计周期"><el-date-picker v-model="queryParams.times" type="daterange" value-format="YYYY-MM-DD HH:mm:ss" start-placeholder="开始时间" end-placeholder="结束时间" class="!w-360px" /></el-form-item>
      <el-form-item><el-button @click="getData"><Icon class="mr-5px" icon="ep:search" />查询</el-button></el-form-item>
    </el-form>
  </ContentWrap>
  <ContentWrap>
    <el-row :gutter="16">
      <el-col :span="6"><el-card shadow="never"><div class="text-gray-500 text-13px">总充值</div><div class="mt-8px text-22px font-bold">{{ formatPoints(overview.totalRecharge) }}</div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="text-gray-500 text-13px">总消费</div><div class="mt-8px text-22px font-bold">{{ formatPoints(overview.totalConsume) }}</div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="text-gray-500 text-13px">总成本</div><div class="mt-8px text-22px font-bold">{{ formatPoints(overview.totalCost) }}</div></el-card></el-col>
      <el-col :span="6"><el-card shadow="never"><div class="text-gray-500 text-13px">总毛利</div><div class="mt-8px text-22px font-bold">{{ formatPoints(overview.totalGrossProfit) }}</div></el-card></el-col>
    </el-row>
  </ContentWrap>
  <ContentWrap>
    <el-tabs model-value="daily">
      <el-tab-pane label="日趋势" name="daily"><el-table v-loading="loading" :data="dailyList" :stripe="true"><el-table-column align="center" label="日期" prop="date" /><el-table-column align="center" label="充值" prop="rechargeAmount" /><el-table-column align="center" label="消费" prop="consumeAmount" /><el-table-column align="center" label="成本" prop="costAmount" /><el-table-column align="center" label="毛利" prop="grossProfit" /></el-table></el-tab-pane>
      <el-tab-pane label="用户排行" name="rank"><el-table v-loading="loading" :data="userRankList" :stripe="true"><el-table-column align="center" label="用户编号" prop="userId" /><el-table-column align="center" label="消费积分" prop="consumeAmount" /><el-table-column align="center" label="任务数" prop="taskCount" /></el-table></el-tab-pane>
    </el-tabs>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { AigcBillingStatisticsApi } from '@/api/aigc/billing/statistics'
import { formatPoints } from '../utils'

defineOptions({ name: 'AigcBillingStatistics' })

const loading = ref(true)
const queryParams = reactive({ times: [] })
const overview = ref({ totalRecharge: 0, totalConsume: 0, totalCost: 0, totalGrossProfit: 0 })
const dailyList = ref<any[]>([])
const userRankList = ref<any[]>([])
const getData = async () => {
  loading.value = true
  try {
    const [overviewData, dailyData, rankData] = await Promise.all([
      AigcBillingStatisticsApi.getOverview(queryParams).catch(() => overview.value),
      AigcBillingStatisticsApi.getDaily(queryParams).catch(() => []),
      AigcBillingStatisticsApi.getUserRank(queryParams).catch(() => [])
    ])
    overview.value = { ...overview.value, ...overviewData }
    dailyList.value = Array.isArray(dailyData) ? dailyData : dailyData.list || []
    userRankList.value = Array.isArray(rankData) ? rankData : rankData.list || []
  } finally { loading.value = false }
}
onMounted(() => getData())
</script>
