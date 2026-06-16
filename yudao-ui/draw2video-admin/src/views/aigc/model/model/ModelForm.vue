<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="760px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px" v-loading="formLoading">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item :label="t('aigc.model.fields.modelType')" prop="type">
            <el-select v-model="formData.type" class="!w-1/1" :placeholder="t('aigc.model.placeholders.modelType')">
              <el-option v-for="item in AIGC_MODEL_TYPES" :key="item.value" :label="getOptionLabel([item], item.value, t)" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item :label="t('aigc.model.fields.modelCode')" prop="code">
            <el-input v-model="formData.code" :placeholder="t('aigc.model.placeholders.modelCode')" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('aigc.model.fields.nameEn')" prop="nameEn">
            <el-input v-model="formData.nameEn" :placeholder="t('aigc.model.placeholders.nameEnOptional')" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item :label="t('aigc.model.fields.modelName')" prop="name">
            <el-input v-model="formData.name" :placeholder="t('aigc.model.placeholders.modelName')" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item :label="t('aigc.model.fields.capabilities')" prop="capabilities">
        <el-select v-model="formData.capabilities" class="!w-1/1" multiple filterable :placeholder="t('aigc.model.placeholders.capabilities')">
          <el-option v-for="item in AIGC_MODEL_CAPABILITIES" :key="item.value" :label="getOptionLabel([item], item.value, t)" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item :label="t('aigc.model.fields.sort')" prop="sort">
            <el-input-number v-model="formData.sort" class="!w-1/1" :min="0" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="t('aigc.model.fields.maxConcurrent')" prop="maxConcurrent">
            <el-input-number v-model="formData.maxConcurrent" class="!w-1/1" :min="1" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="t('aigc.model.fields.timeoutSeconds')" prop="timeoutSeconds">
            <el-input-number v-model="formData.timeoutSeconds" class="!w-1/1" :min="1" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item :label="t('aigc.model.fields.publicVisible')" prop="publicVisible">
            <el-switch v-model="formData.publicVisible" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="t('aigc.model.fields.defaultModel')" prop="defaultModel">
            <el-switch v-model="formData.defaultModel" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="t('common.status')" prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item :label="t('aigc.model.fields.remark')" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :placeholder="t('aigc.model.placeholders.remark')" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">{{ t('common.ok') }}</el-button>
      <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { AigcModelApi, AigcModelSaveReqVO } from '@/api/aigc/model/model'
import { AIGC_MODEL_CAPABILITIES, AIGC_MODEL_TYPES, getOptionLabel } from '../constants'

defineOptions({ name: 'AigcModelForm' })

const { t } = useI18n()
const message = useMessage()
const router = useRouter()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const formData = ref<AigcModelSaveReqVO>({
  id: undefined,
  providerId: undefined,
  code: undefined,
  name: undefined,
  nameEn: undefined,
  model: undefined,
  type: undefined,
  capabilities: [],
  publicVisible: true,
  defaultModel: false,
  sort: 0,
  maxConcurrent: 1,
  timeoutSeconds: 300,
  status: CommonStatusEnum.ENABLE,
  remark: undefined
})
const formRules = reactive({
  code: [{ required: true, message: '模型编码不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '模型名称不能为空', trigger: 'blur' }],
  type: [{ required: true, message: '模型类型不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await AigcModelApi.getModel(id)
      formData.value.capabilities = formData.value.capabilities || []
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
      const id = await AigcModelApi.createModel(formData.value)
      message.success(t('common.createSuccess'))
      if (id) {
        dialogVisible.value = false
        emit('success')
        await router.push(`/aigc/model/detail/${id}`)
        return
      }
    } else {
      await AigcModelApi.updateModel(formData.value)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = {
    id: undefined,
    providerId: undefined,
    code: undefined,
    name: undefined,
    nameEn: undefined,
    model: undefined,
    type: undefined,
    capabilities: [],
    publicVisible: true,
    defaultModel: false,
    sort: 0,
    maxConcurrent: 1,
    timeoutSeconds: 300,
    status: CommonStatusEnum.ENABLE,
    remark: undefined
  }
  formRef.value?.resetFields()
}
</script>
