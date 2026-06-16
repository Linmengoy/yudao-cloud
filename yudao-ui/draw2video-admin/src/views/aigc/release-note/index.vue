<template>
  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="-mb-15px" label-width="76px">
      <el-form-item label="版本号" prop="version">
        <el-input v-model="queryParams.version" class="!w-220px" clearable placeholder="请输入版本号" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" class="!w-240px" clearable placeholder="请输入标题" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" class="!w-180px" clearable placeholder="请选择状态">
          <el-option v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="发布日期" prop="releaseDate">
        <el-date-picker
          v-model="queryParams.releaseDate"
          class="!w-260px"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['aigc:release-note:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="版本号" align="center" prop="version" min-width="120" />
      <el-table-column label="发布日期" align="center" prop="releaseDate" min-width="120" />
      <el-table-column label="标题" align="left" prop="title" min-width="180" />
      <el-table-column label="摘要" align="left" prop="summary" min-width="260" />
      <el-table-column label="状态" align="center" prop="status" min-width="90">
        <template #default="scope"><dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" /></template>
      </el-table-column>
      <el-table-column label="发布人" align="center" prop="publisher" min-width="110" />
      <el-table-column label="发布时间" align="center" prop="publishTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="操作" align="center" width="260" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)" v-hasPermi="['aigc:release-note:update']">编辑</el-button>
          <el-button
            link
            :type="scope.row.status === CommonStatusEnum.ENABLE ? 'warning' : 'success'"
            @click="handleStatus(scope.row)"
            v-hasPermi="['aigc:release-note:publish']"
          >
            {{ scope.row.status === CommonStatusEnum.ENABLE ? '下线' : '发布' }}
          </el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)" v-hasPermi="['aigc:release-note:delete']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <ReleaseNoteForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { AigcReleaseNoteApi } from '@/api/aigc/release-note'
import type { AigcReleaseNoteRespVO } from '@/api/aigc/model/types'
import ReleaseNoteForm from './ReleaseNoteForm.vue'

defineOptions({ name: 'AigcReleaseNote' })

const message = useMessage()
const { t } = useI18n()
const loading = ref(true)
const list = ref<AigcReleaseNoteRespVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  version: undefined,
  title: undefined,
  status: undefined,
  releaseDate: undefined
})

const getList = async () => {
  loading.value = true
  try {
    const data = await AigcReleaseNoteApi.getReleaseNotePage(queryParams)
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
const handleStatus = async (row: AigcReleaseNoteRespVO) => {
  const nextStatus = row.status === CommonStatusEnum.ENABLE ? CommonStatusEnum.DISABLE : CommonStatusEnum.ENABLE
  await AigcReleaseNoteApi.updateReleaseNoteStatus(row.id!, nextStatus)
  message.success(nextStatus === CommonStatusEnum.ENABLE ? '发布成功' : '下线成功')
  await getList()
}
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await AigcReleaseNoteApi.deleteReleaseNote(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

onMounted(() => getList())
</script>
