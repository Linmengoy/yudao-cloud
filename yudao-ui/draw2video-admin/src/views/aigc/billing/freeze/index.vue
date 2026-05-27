<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :inline="true" :model="queryParams" class="-mb-15px" label-width="80px">
      <el-form-item label="用户编号" prop="userId">
        <el-input v-model="queryParams.userId" class="!w-220px" clearable placeholder="请输入用户编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="任务编号" prop="taskNo">
        <el-input v-model="queryParams.taskNo" class="!w-220px" clearable placeholder="请输入任务编号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="冻结状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-180px" clearable placeholder="请选择冻结状态">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
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
      <el-table-column align="center" label="冻结号" prop="freezeNo" min-width="180" />
      <el-table-column align="center" label="用户编号" prop="userId" width="120" />
      <el-table-column align="center" label="任务编号" prop="taskNo" min-width="160" />
      <el-table-column align="center" label="冻结积分" width="130"><template #default="scope">{{ formatPoints(scope.row.amount) }}</template></el-table-column>
      <el-table-column align="center" label="已扣费" width="130"><template #default="scope">{{ formatPoints(scope.row.confirmedAmount) }}</template></el-table-column>
      <el-table-column align="center" label="已释放" width="130"><template #default="scope">{{ formatPoints(scope.row.releasedAmount) }}</template></el-table-column>
      <el-table-column align="center" label="状态" width="120"><template #default="scope">{{ mapText(freezeStatusMap, scope.row.status) }}</template></el-table-column>
      <el-table-column :formatter="dateFormatter" align="center" label="过期时间" prop="expireTime" width="180" />
      <el-table-column align="center" fixed="right" label="操作" width="180">
        <template #default="scope">
          <el-button v-hasPermi="['aigc:billing:freeze:update']" link type="primary" @click="handleConfirm(scope.row)">确认扣费</el-button>
          <el-button v-hasPermi="['aigc:billing:freeze:update']" link type="danger" @click="handleRelease(scope.row)">释放</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination v-model:limit="queryParams.pageSize" v-model:page="queryParams.pageNo" :total="total" @pagination="getList" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { dateFormatter } from '@/utils/formatTime'
import { AigcBillingFreezeApi, AigcQuotaFreezeVO } from '@/api/aigc/billing/freeze'
import { freezeStatusMap, formatPoints, mapText } from '../utils'

defineOptions({ name: 'AigcBillingFreeze' })

const message = useMessage()
const loading = ref(true)
const list = ref<AigcQuotaFreezeVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive({ pageNo: 1, pageSize: 10, userId: undefined, taskNo: undefined, status: undefined })
const statusOptions = Object.entries(freezeStatusMap).filter(([key]) => Number.isNaN(Number(key))).map(([value, label]) => ({ value, label }))

const getList = async () => {
  loading.value = true
  try {
    const data = await AigcBillingFreezeApi.getFreezePage(queryParams)
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

const handleConfirm = async (row: AigcQuotaFreezeVO) => {
  await message.confirm('确认将该冻结记录扣费吗？')
  await AigcBillingFreezeApi.confirmFreeze({
    freezeId: row.id,
    taskId: row.taskId,
    taskNo: row.taskNo,
    actualAmount: row.amount,
  })
  message.success('确认扣费成功')
  getList()
}

const handleRelease = async (row: AigcQuotaFreezeVO) => {
  await message.confirm('确认释放该冻结记录吗？')
  await AigcBillingFreezeApi.releaseFreeze({
    freezeId: row.id,
    taskId: row.taskId,
    taskNo: row.taskNo,
    reason: '后台人工释放'
  })
  message.success('释放成功')
  getList()
}

onMounted(() => getList())
</script>
