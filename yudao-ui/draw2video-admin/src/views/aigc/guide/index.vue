<template>
  <ContentWrap>
    <div class="guide-toolbar">
      <div>
        <div class="guide-title">使用指南</div>
        <div class="guide-subtitle">管理数据库内容源，只有已发布内容会进入用户可见的指南快照。</div>
      </div>
      <div class="guide-actions">
        <el-button @click="openGuide">
          <Icon icon="ep:link" class="mr-5px" />打开站点
        </el-button>
        <el-button v-hasPermi="['aigc:guide:create']" type="primary" @click="openForm('create')">
          <Icon icon="ep:plus" class="mr-5px" />新增内容
        </el-button>
      </div>
    </div>
  </ContentWrap>

  <ContentWrap>
    <el-form ref="queryFormRef" :model="queryParams" :inline="true" class="-mb-15px" label-width="80px">
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" class="!w-220px" clearable placeholder="请输入标题" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="分类" prop="category">
        <el-input v-model="queryParams.category" class="!w-180px" clearable placeholder="请输入分类" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="publishStatus">
        <el-select v-model="queryParams.publishStatus" class="!w-150px" clearable placeholder="请选择">
          <el-option v-for="item in publishStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="标题" min-width="220">
        <template #default="{ row }">
          <div class="font-medium">{{ row.title || '-' }}</div>
          <div class="mt-4px text-12px text-gray-500">{{ row.slug || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="分类" prop="category" width="140" />
      <el-table-column label="摘要" prop="summary" min-width="240" />
      <el-table-column label="排序" prop="sort" width="90" />
      <el-table-column label="状态" prop="publishStatus" width="110">
        <template #default="{ row }">
          <el-tag :type="row.publishStatus === 'PUBLISHED' ? 'success' : 'info'">
            {{ publishStatusLabel(row.publishStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" prop="publishTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="更新时间" prop="updateTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="操作" width="270" fixed="right">
        <template #default="{ row }">
          <el-button v-hasPermi="['aigc:guide:update']" link type="primary" @click="openForm('update', row.id)">编辑</el-button>
          <el-button
            v-if="row.publishStatus !== 'PUBLISHED'"
            v-hasPermi="['aigc:guide:publish']"
            link
            type="success"
            @click="handlePublish(row.id)"
          >
            发布
          </el-button>
          <el-button v-else v-hasPermi="['aigc:guide:publish']" link type="warning" @click="handleUnpublish(row.id)">
            取消发布
          </el-button>
          <el-button link type="primary" @click="openPreview(row.slug)">预览</el-button>
          <el-button v-hasPermi="['aigc:guide:delete']" link type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </ContentWrap>

  <Dialog v-model="formVisible" :title="formTitle" width="880px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="标题" prop="title">
            <el-input v-model="formData.title" maxlength="128" show-word-limit placeholder="请输入标题" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Slug" prop="slug">
            <el-input v-model="formData.slug" maxlength="128" show-word-limit placeholder="quick-start" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="分类" prop="category">
            <el-input v-model="formData.category" maxlength="64" show-word-limit placeholder="入门" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="formData.sort" class="!w-1/1" :min="0" controls-position="right" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="摘要" prop="summary">
        <el-input v-model="formData.summary" maxlength="512" show-word-limit placeholder="请输入摘要" />
      </el-form-item>
      <el-form-item label="正文" prop="content">
        <el-input v-model="formData.content" type="textarea" :rows="16" placeholder="请输入 Markdown 正文" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">保存</el-button>
      <el-button @click="formVisible = false">取消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import {
  AigcGuideContentApi,
  type AigcGuideContentPageReqVO,
  type AigcGuideContentSaveReqVO,
  type AigcGuideContentVO
} from '@/api/aigc/guide'

defineOptions({ name: 'AigcGuide' })

const message = useMessage()
const guideUrl = computed(() => import.meta.env.VITE_APP_GUIDE_URL || '/guide/')
const loading = ref(false)
const list = ref<AigcGuideContentVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive<AigcGuideContentPageReqVO>({ pageNo: 1, pageSize: 10 })
const formVisible = ref(false)
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const formRef = ref()
const formData = reactive<AigcGuideContentSaveReqVO>({
  slug: '',
  title: '',
  category: '',
  summary: '',
  content: '',
  sort: 0
})

const publishStatusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' }
]
const formTitle = computed(() => (formType.value === 'create' ? '新增指南内容' : '编辑指南内容'))
const formRules = reactive({
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  slug: [
    { required: true, message: '请输入 Slug', trigger: 'blur' },
    { pattern: /^[a-z0-9][a-z0-9-_/]*$/i, message: 'Slug 只能包含字母、数字、-、_、/', trigger: 'blur' }
  ],
  content: [{ required: true, message: '请输入正文', trigger: 'blur' }]
})

const publishStatusLabel = (value?: string) => publishStatusOptions.find((item) => item.value === value)?.label || value || '-'

const resetForm = () => {
  Object.assign(formData, {
    id: undefined,
    slug: '',
    title: '',
    category: '',
    summary: '',
    content: '',
    sort: 0
  })
  formRef.value?.clearValidate()
}

const getList = async () => {
  loading.value = true
  try {
    const data = await AigcGuideContentApi.getPage(queryParams)
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
  queryFormRef.value?.resetFields()
  handleQuery()
}

const openForm = async (type: 'create' | 'update', id?: number) => {
  resetForm()
  formType.value = type
  if (type === 'update' && id) {
    const data = await AigcGuideContentApi.get(id)
    Object.assign(formData, data)
  }
  formVisible.value = true
}

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await AigcGuideContentApi.create(formData)
      message.success('创建成功')
    } else {
      await AigcGuideContentApi.update(formData)
      message.success('保存成功')
    }
    formVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

const handlePublish = async (id: number) => {
  await message.confirm('确认发布该指南内容吗？发布后普通用户可见。')
  await AigcGuideContentApi.publish(id)
  message.success('发布成功')
  await getList()
}

const handleUnpublish = async (id: number) => {
  await message.confirm('确认取消发布该指南内容吗？取消后普通用户不可见。')
  await AigcGuideContentApi.unpublish(id)
  message.success('已取消发布')
  await getList()
}

const handleDelete = async (id: number) => {
  await message.delConfirm()
  await AigcGuideContentApi.delete(id)
  message.success('删除成功')
  await getList()
}

const openGuide = () => {
  window.open(guideUrl.value)
}

const openPreview = (slug?: string) => {
  window.open(slug ? `${guideUrl.value.replace(/\/?$/, '/')}${slug}` : guideUrl.value)
}

onMounted(() => getList())
</script>

<style scoped>
.guide-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.guide-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.guide-subtitle {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
}

.guide-actions {
  display: flex;
  gap: 8px;
}

@media (max-width: 768px) {
  .guide-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .guide-actions {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
