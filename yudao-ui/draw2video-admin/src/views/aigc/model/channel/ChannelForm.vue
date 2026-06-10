<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="760px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px" v-loading="formLoading">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="业务模型" prop="modelId">
            <el-select v-model="formData.modelId" class="!w-1/1" filterable placeholder="请选择业务模型">
              <el-option v-for="item in modelList" :key="item.id" :label="item.name" :value="Number(item.id)" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="渠道商" prop="providerId">
            <el-select v-model="formData.providerId" class="!w-1/1" filterable placeholder="请选择渠道商">
              <el-option v-for="item in providerList" :key="item.id" :label="item.name" :value="Number(item.id)" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item label="实现名称" prop="name"><el-input v-model="formData.name" placeholder="请输入实现名称" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="上游模型" prop="providerModel"><el-input v-model="formData.providerModel" placeholder="请输入上游模型标识" /></el-form-item></el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="8"><el-form-item label="成本价" prop="costPrice"><el-input-number v-model="formData.costPrice" class="!w-1/1" :min="0" :precision="6" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="权重" prop="weight"><el-input-number v-model="formData.weight" class="!w-1/1" :min="0" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="优先级" prop="priority"><el-input-number v-model="formData.priority" class="!w-1/1" :min="0" /></el-form-item></el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="8"><el-form-item label="最大并发" prop="maxConcurrent"><el-input-number v-model="formData.maxConcurrent" class="!w-1/1" :min="1" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="超时时间" prop="timeoutSeconds"><el-input-number v-model="formData.timeoutSeconds" class="!w-1/1" :min="1" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="状态" prop="status"><el-radio-group v-model="formData.status"><el-radio v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio></el-radio-group></el-form-item></el-col>
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
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { AigcModelApi } from '@/api/aigc/model/model'
import { AigcModelChannelApi, type AigcModelChannelSaveReqVO } from '@/api/aigc/model/channel'
import { AigcModelProviderApi } from '@/api/aigc/model/provider'
import type { AigcModelProviderRespVO, AigcModelRespVO } from '@/api/aigc/model/types'

defineOptions({ name: 'AigcModelChannelForm' })

const { t } = useI18n()
const message = useMessage()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const modelList = ref<AigcModelRespVO[]>([])
const providerList = ref<AigcModelProviderRespVO[]>([])
const formData = ref<AigcModelChannelSaveReqVO>({ id: undefined, modelId: undefined, providerId: undefined, providerModel: undefined, name: undefined, costPrice: 0, currencyType: 'POINT', weight: 100, priority: 100, maxConcurrent: undefined, timeoutSeconds: undefined, status: CommonStatusEnum.ENABLE, remark: undefined })
const formRules = reactive({ modelId: [{ required: true, message: '业务模型不能为空', trigger: 'change' }], providerId: [{ required: true, message: '渠道商不能为空', trigger: 'change' }], providerModel: [{ required: true, message: '上游模型不能为空', trigger: 'blur' }], status: [{ required: true, message: '状态不能为空', trigger: 'change' }] })

const open = async (type: string, id?: number, modelId?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  const [modelPage, providerPage] = await Promise.all([
    AigcModelApi.getModelPage({ pageNo: 1, pageSize: 100 }),
    AigcModelProviderApi.getProviderPage({ pageNo: 1, pageSize: 100 })
  ])
  modelList.value = modelPage.list || []
  providerList.value = providerPage.list || []
  if (modelId) formData.value.modelId = modelId
  if (id) {
    formLoading.value = true
    try {
      formData.value = await AigcModelChannelApi.getChannel(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])
const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await AigcModelChannelApi.createChannel(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await AigcModelChannelApi.updateChannel(formData.value)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = { id: undefined, modelId: undefined, providerId: undefined, providerModel: undefined, name: undefined, costPrice: 0, currencyType: 'POINT', weight: 100, priority: 100, maxConcurrent: undefined, timeoutSeconds: undefined, status: CommonStatusEnum.ENABLE, remark: undefined }
  formRef.value?.resetFields()
}
</script>
