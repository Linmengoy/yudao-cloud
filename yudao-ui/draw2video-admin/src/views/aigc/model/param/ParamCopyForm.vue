<template>
  <Dialog v-model="dialogVisible" title="复制参数模板" width="560px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="96px">
      <el-form-item label="源模型" prop="sourceModelId">
        <el-select v-model="formData.sourceModelId" filterable placeholder="请选择源模型">
          <el-option v-for="item in modelList" :key="item.id" :label="getModelName(item)" :value="Number(item.id)" />
        </el-select>
      </el-form-item>
      <el-form-item label="目标模型" prop="targetModelIds">
        <el-select v-model="formData.targetModelIds" multiple filterable collapse-tags collapse-tags-tooltip placeholder="请选择目标模型">
          <el-option
            v-for="item in targetModelList"
            :key="item.id"
            :label="getModelName(item)"
            :value="Number(item.id)"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="能力">
        <el-select v-model="formData.capabilities" multiple clearable collapse-tags collapse-tags-tooltip placeholder="不选则复制全部能力">
          <el-option v-for="item in AIGC_MODEL_CAPABILITIES" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="覆盖已有">
        <el-switch v-model="formData.overwrite" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="formLoading" @click="submitForm">确定</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { AigcModelParamApi, type AigcModelParamTemplateCopyReqVO } from '@/api/aigc/model/param'
import type { AigcModelRespVO } from '@/api/aigc/model/types'
import { AIGC_MODEL_CAPABILITIES } from '../constants'

defineOptions({ name: 'AigcModelParamCopyForm' })

const message = useMessage()
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
  sourceModelId: [{ required: true, message: '源模型不能为空', trigger: 'change' }],
  targetModelIds: [{ required: true, message: '目标模型不能为空', trigger: 'change' }]
})

const targetModelList = computed(() => modelList.value.filter((item) => Number(item.id) !== formData.value.sourceModelId))
const getModelName = (model: AigcModelRespVO) => {
  const code = model.code || model.model
  return code ? `${model.name || `模型 ${model.id}`} / ${code}` : model.name || `模型 ${model.id}`
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
    message.success(`复制完成：新增 ${result.createdCount || 0}，覆盖 ${result.updatedCount || 0}，跳过 ${result.skippedCount || 0}`)
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>
