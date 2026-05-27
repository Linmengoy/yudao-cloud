<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="-mb-15px" label-width="80px">
      <el-form-item label="敏感词" prop="word">
        <el-input v-model="queryParams.word" class="!w-220px" clearable placeholder="请输入敏感词" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="审核场景" prop="scene">
        <el-select v-model="queryParams.scene" class="!w-180px" clearable placeholder="请选择审核场景">
          <el-option v-for="item in AIGC_SAFETY_SCENES" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="风险等级" prop="level">
        <el-select v-model="queryParams.level" class="!w-160px" clearable placeholder="请选择风险等级">
          <el-option v-for="item in AIGC_RISK_LEVELS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="匹配方式" prop="matchType">
        <el-select v-model="queryParams.matchType" class="!w-180px" clearable placeholder="请选择匹配方式">
          <el-option v-for="item in AIGC_SENSITIVE_WORD_MATCH_TYPES" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-160px" clearable placeholder="请选择状态">
          <el-option v-for="item in AIGC_SENSITIVE_WORD_STATUSES" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间" prop="createTime">
        <el-date-picker v-model="queryParams.createTime" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss" start-placeholder="开始日期" end-placeholder="结束日期" class="!w-360px" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['aigc:safety-sensitive-word:create']"><Icon icon="ep:plus" class="mr-5px" />新增</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="敏感词" align="center" prop="word" min-width="160" />
      <el-table-column label="审核场景" align="center" prop="scene" width="120">
        <template #default="scope">{{ getOptionLabel(AIGC_SAFETY_SCENES, scope.row.scene) }}</template>
      </el-table-column>
      <el-table-column label="风险等级" align="center" prop="level" width="100">
        <template #default="scope"><el-tag :type="scope.row.level >= 4 ? 'danger' : 'warning'">{{ scope.row.level || '-' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="匹配方式" align="center" prop="matchType" width="120">
        <template #default="scope">{{ getOptionLabel(AIGC_SENSITIVE_WORD_MATCH_TYPES, scope.row.matchType) }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope"><el-tag :type="scope.row.status === 'ENABLE' ? 'success' : 'info'">{{ getOptionLabel(AIGC_SENSITIVE_WORD_STATUSES, scope.row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" min-width="180" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="操作" align="center" width="240" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['aigc:safety-sensitive-word:update']">编辑</el-button>
          <el-button link :type="scope.row.status === 'ENABLE' ? 'warning' : 'success'" @click="handleStatus(scope.row)" v-hasPermi="['aigc:safety-sensitive-word:update']">
            {{ scope.row.status === 'ENABLE' ? '禁用' : '启用' }}
          </el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['aigc:safety-sensitive-word:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <SensitiveWordForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { AigcSensitiveWordApi } from '@/api/aigc/safety/sensitive-word'
import type { AigcSensitiveWordRespVO } from '@/api/aigc/safety/types'
import SensitiveWordForm from './SensitiveWordForm.vue'
import { AIGC_RISK_LEVELS, AIGC_SAFETY_SCENES, AIGC_SENSITIVE_WORD_MATCH_TYPES, AIGC_SENSITIVE_WORD_STATUSES, getOptionLabel } from '../constants'

defineOptions({ name: 'AigcSensitiveWord' })

const message = useMessage()
const { t } = useI18n()
const loading = ref(true)
const list = ref<AigcSensitiveWordRespVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive({ pageNo: 1, pageSize: 10, word: undefined, scene: undefined, level: undefined, matchType: undefined, status: undefined, createTime: undefined })

const getList = async () => {
  loading.value = true
  try {
    const data = await AigcSensitiveWordApi.getSensitiveWordPage(queryParams)
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

const formRef = ref()
const openForm = (type: string, id?: number) => formRef.value.open(type, id)

const handleDelete = async (id: number) => {
  if (!id) return
  try {
    await message.delConfirm()
    await AigcSensitiveWordApi.deleteSensitiveWord(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const handleStatus = async (row: AigcSensitiveWordRespVO) => {
  if (!row.id) return
  const status = row.status === 'ENABLE' ? 'DISABLE' : 'ENABLE'
  await message.confirm(`确认${status === 'ENABLE' ? '启用' : '禁用'}该敏感词吗？`)
  await AigcSensitiveWordApi.updateSensitiveWordStatus({ id: row.id, status })
  message.success('状态更新成功')
  await getList()
}

onMounted(() => getList())
</script>
