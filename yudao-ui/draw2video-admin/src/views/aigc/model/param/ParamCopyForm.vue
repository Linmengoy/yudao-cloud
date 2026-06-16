<template>
  <Dialog v-model="dialogVisible" :title="t('aigc.model.actions.copyParams')" width="560px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="96px">
      <el-form-item :label="t('aigc.model.fields.sourceModel')" prop="sourceModelId">
        <el-select v-model="formData.sourceModelId" filterable :placeholder="t('aigc.model.placeholders.sourceModel')">
          <el-option v-for="item in modelList" :key="item.id" :label="getModelName(item)" :value="Number(item.id)" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('aigc.model.fields.targetModel')" prop="targetModelIds">
        <el-select v-model="formData.targetModelIds" multiple filterable collapse-tags collapse-tags-tooltip :placeholder="t('aigc.model.placeholders.targetModel')">
          <el-option
            v-for="item in targetModelList"
            :key="item.id"
            :label="getModelName(item)"
            :value="Number(item.id)"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('aigc.model.fields.capability')">
        <el-select v-model="formData.capabilities" multiple clearable collapse-tags collapse-tags-tooltip :placeholder="t('aigc.model.placeholders.copyAllCapabilities')">
          <el-option v-for="item in AIGC_MODEL_CAPABILITIES" :key="item.value" :label="getOptionLabel([item], item.value, t)" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('aigc.model.fields.overwriteExisting')">
        <el-switch v-model="formData.overwrite" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="formLoading" @click="submitForm">{{ t('common.ok') }}</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { AigcModelParamApi, type AigcModelParamTemplateCopyReqVO } from '@/api/aigc/model/param'
import type { AigcModelRespVO } from '@/api/aigc/model/types'
import { AIGC_MODEL_CAPABILITIES, getOptionLabel } from '../constants'

defineOptions({ name: 'AigcModelParamCopyForm' })

const message = useMessage()
const { t } = useI18n()
const dialogVisible = ref(false)
const formLoading = ref(false)
const formRef = ref()
const modelList = ref<AigcModelRespVO[]>([])
const formData = ref<AigcModelParamTemplateCopyReqVO>({
  sourceModelId: undefined,
  targetModelIds: [],
  capabilities: [],
  overwrite: false
})
const formRules = reactive({
  sourceModelId: [{ required: true, message: t('aigc.model.validation.sourceModelRequired'), trigger: 'change' }],
  targetModelIds: [{ required: true, message: t('aigc.model.validation.targetModelRequired'), trigger: 'change' }]
})

const targetModelList = computed(() => modelList.value.filter((item) => Number(item.id) !== formData.value.sourceModelId))
const getModelName = (model: AigcModelRespVO) => {
  const code = model.code || model.model
  const name = model.name || t('aigc.model.fallbacks.model', { id: model.id })
  return code ? `${name} / ${code}` : name
}

const emit = defineEmits(['success'])
const open = async (models: AigcModelRespVO[], sourceModelId?: number, capability?: string) => {
  modelList.value = models
  formData.value = {
    sourceModelId,
    targetModelIds: [],
    capabilities: capability ? [capability] : [],
    overwrite: false
  }
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}
defineExpose({ open })

const submitForm = async () => {
  await formRef.value?.validate()
  formLoading.value = true
  try {
    const result = await AigcModelParamApi.copyParams(formData.value)
    message.success(t('aigc.model.messages.copyParamsDone', {
      created: result.createdCount || 0,
      updated: result.updatedCount || 0,
      skipped: result.skippedCount || 0
    }))
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
