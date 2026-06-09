<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="680px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px" v-loading="formLoading">
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item label="租户 ID" prop="tenantId"><el-input-number v-model="formData.tenantId" class="!w-1/1" :min="1" controls-position="right" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="模型" prop="modelId"><el-select v-model="formData.modelId" class="!w-1/1" filterable placeholder="请选择模型"><el-option v-for="item in modelList" :key="item.id" :label="getModelName(item)" :value="getModelOptionValue(item)" /></el-select></el-form-item></el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="8"><el-form-item label="启用" prop="enabled"><el-switch v-model="formData.enabled" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="用户端展示" prop="publicVisible"><el-switch v-model="formData.publicVisible" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="默认模型" prop="defaultModel"><el-switch v-model="formData.defaultModel" /></el-form-item></el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="8"><el-form-item label="排序" prop="sort"><el-input-number v-model="formData.sort" class="!w-1/1" :min="0" controls-position="right" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="最大并发" prop="maxConcurrent"><el-input-number v-model="formData.maxConcurrent" class="!w-1/1" :min="1" controls-position="right" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="日限额" prop="dailyLimit"><el-input-number v-model="formData.dailyLimit" class="!w-1/1" :min="0" controls-position="right" /></el-form-item></el-col>
      </el-row>
      <el-form-item label="备注" prop="remark"><el-input v-model="formData.remark" type="textarea" placeholder="请输入备注" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { AigcModelTenantApi, type AigcModelTenantSaveReqVO } from '@/api/aigc/model/tenant'
import { AigcModelApi } from '@/api/aigc/model/model'
import type { AigcModelRespVO } from '@/api/aigc/model/types'

defineOptions({ name: 'AigcModelTenantForm' })

const { t } = useI18n()
const message = useMessage()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const modelList = ref<AigcModelRespVO[]>([])
const formData = ref<AigcModelTenantSaveReqVO>({ id: undefined, tenantId: undefined, modelId: undefined, enabled: true, publicVisible: true, defaultModel: false, sort: 0, maxConcurrent: 1, dailyLimit: 0, remark: undefined })
const formRules = reactive({ tenantId: [{ required: true, message: '租户 ID 不能为空', trigger: 'blur' }], modelId: [{ required: true, message: '模型不能为空', trigger: 'change' }] })

const open = async (type: string, id?: number, tenantId?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  await loadModelList()
  if (tenantId) formData.value.tenantId = tenantId
  if (id) {
    formLoading.value = true
    try {
      formData.value = await AigcModelTenantApi.getTenantModel(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const loadModelList = async () => {
  const data = await AigcModelApi.getModelPage({ pageNo: 1, pageSize: 100 })
  modelList.value = data.list || []
}

const getModelName = (model: AigcModelRespVO) => {
  return model.name || `模型 ${model.id}`
}

const getModelOptionValue = (model: AigcModelRespVO) => Number(model.id)

const emit = defineEmits(['success'])
const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await AigcModelTenantApi.createTenantModel(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await AigcModelTenantApi.updateTenantModel(formData.value)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = { id: undefined, tenantId: undefined, modelId: undefined, enabled: true, publicVisible: true, defaultModel: false, sort: 0, maxConcurrent: 1, dailyLimit: 0, remark: undefined }
  formRef.value?.resetFields()
}
</script>
