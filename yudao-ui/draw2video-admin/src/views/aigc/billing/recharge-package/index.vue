<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :inline="true" :model="queryParams" class="-mb-15px" label-width="80px">
      <el-form-item label="套餐名称" prop="name"><el-input v-model="queryParams.name" class="!w-220px" clearable placeholder="请输入套餐名称" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" class="!w-180px" clearable placeholder="请选择状态"><el-option v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value" :label="dict.label" :value="dict.value" /></el-select></el-form-item>
      <el-form-item><el-button @click="handleQuery"><Icon class="mr-5px" icon="ep:search" />搜索</el-button><el-button @click="resetQuery"><Icon class="mr-5px" icon="ep:refresh" />重置</el-button><el-button v-hasPermi="['aigc:billing:recharge-package:create']" plain type="primary" @click="openForm('create')"><Icon class="mr-5px" icon="ep:plus" />新增</el-button></el-form-item>
    </el-form>
  </ContentWrap>
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" :stripe="true">
      <el-table-column align="center" label="套餐名称" prop="name" min-width="140" />
      <el-table-column align="center" label="支付金额" width="120"><template #default="scope">{{ formatMoney(scope.row.payAmount) }}</template></el-table-column>
      <el-table-column align="center" label="充值积分" width="130"><template #default="scope">{{ formatPoints(scope.row.pointAmount) }}</template></el-table-column>
      <el-table-column align="center" label="赠送积分" width="130"><template #default="scope">{{ formatPoints(scope.row.giftAmount) }}</template></el-table-column>
      <el-table-column align="center" label="合计积分" width="130"><template #default="scope">{{ formatPoints(scope.row.totalPointAmount) }}</template></el-table-column>
      <el-table-column align="center" label="推荐" width="90"><template #default="scope">{{ scope.row.recommendStatus ? '是' : '否' }}</template></el-table-column>
      <el-table-column align="center" label="排序" prop="sort" width="90" />
      <el-table-column align="center" label="状态" prop="status" width="90"><template #default="scope"><dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" /></template></el-table-column>
      <el-table-column :formatter="dateFormatter" align="center" label="创建时间" prop="createTime" width="180" />
      <el-table-column align="center" fixed="right" label="操作" width="150"><template #default="scope"><el-button v-hasPermi="['aigc:billing:recharge-package:update']" link type="primary" @click="openForm('update', scope.row.id)">编辑</el-button><el-button v-hasPermi="['aigc:billing:recharge-package:delete']" link type="danger" @click="handleDelete(scope.row.id)">删除</el-button></template></el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>
  <PackageForm ref="formRef" @success="getList" />
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { AigcRechargePackageApi, type AigcRechargePackageVO } from '@/api/aigc/billing/recharge-package'
import { formatPoints } from '../utils'
import PackageForm from './PackageForm.vue'

defineOptions({ name: 'AigcRechargePackage' })

const message = useMessage()
const { t } = useI18n()
const loading = ref(true)
const list = ref<AigcRechargePackageVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive({ pageNo: 1, pageSize: 10, name: undefined, status: undefined })
const getList = async () => {
  loading.value = true
  try { const data = await AigcRechargePackageApi.getPackagePage(queryParams); list.value = data.list; total.value = data.total } finally { loading.value = false }
}
const formatMoney = (value?: number | null) => `¥${(Number(value || 0) / 100).toFixed(2)}`
const handleQuery = () => { queryParams.pageNo = 1; getList() }
const resetQuery = () => { queryFormRef.value.resetFields(); handleQuery() }
const formRef = ref()
const openForm = (type: string, id?: number) => formRef.value.open(type, id)
const handleDelete = async (id: number) => { await message.delConfirm(); await AigcRechargePackageApi.deletePackage(id); message.success(t('common.delSuccess')); getList() }
onMounted(() => getList())
</script>
