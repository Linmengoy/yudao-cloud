<template>
  <ContentWrap>
    <div class="flex items-start justify-between gap-16px">
      <div>
        <div class="text-18px font-600">Text 系统提示词</div>
        <div class="mt-6px text-13px text-gray-500">
          供 Text Composer 优化提示词时使用，所有项目和用户共用这一份配置。
        </div>
      </div>
      <div class="flex gap-8px">
        <el-button :disabled="loading || saving" @click="getPrompt">
          <Icon icon="ep:refresh" class="mr-5px" />刷新
        </el-button>
        <el-button
          v-hasPermi="['aigc:prompt:text-system:update']"
          type="primary"
          :loading="saving"
          @click="submitForm"
        >
          <Icon icon="ep:check" class="mr-5px" />保存
        </el-button>
      </div>
    </div>
  </ContentWrap>

  <ContentWrap>
    <el-form ref="formRef" v-loading="loading" :model="formData" :rules="formRules" label-width="110px">
      <el-form-item label="配置键名">
        <el-input :model-value="CONFIG_KEY" disabled />
      </el-form-item>
      <el-form-item label="参数名称">
        <el-input :model-value="CONFIG_NAME" disabled />
      </el-form-item>
      <el-form-item label="系统提示词" prop="value">
        <el-input
          v-model="formData.value"
          type="textarea"
          :autosize="{ minRows: 24, maxRows: 40 }"
          maxlength="50000"
          show-word-limit
          placeholder="请输入 Text Composer 生成内容时使用的系统提示词"
        />
      </el-form-item>
      <el-form-item>
        <el-button
          v-hasPermi="['aigc:prompt:text-system:update']"
          type="primary"
          :loading="saving"
          @click="submitForm"
        >
          保存
        </el-button>
        <el-button :disabled="loading || saving" @click="resetForm">还原</el-button>
        <span class="ml-12px text-13px text-gray-500">首次保存后生效</span>
      </el-form-item>
    </el-form>
  </ContentWrap>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { AigcTextSystemPromptApi } from '@/api/aigc/prompt/text-system'

defineOptions({ name: 'AigcTextSystemPrompt' })

const CONFIG_KEY = 'aigc.text.system-prompt'
const CONFIG_NAME = 'Text 系统提示词'

const message = useMessage()
const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const savedValue = ref('')
const formData = reactive({
  value: ''
})

const formRules = reactive<FormRules>({
  value: [
    { required: true, message: '请输入系统提示词', trigger: 'blur' },
    { max: 50000, message: '系统提示词不能超过 50000 个字符', trigger: 'blur' }
  ]
})

const getPrompt = async () => {
  loading.value = true
  try {
    const data = await AigcTextSystemPromptApi.get()
    savedValue.value = data.value || ''
    formData.value = savedValue.value
    formRef.value?.clearValidate()
  } finally {
    loading.value = false
  }
}

const submitForm = async () => {
  await formRef.value?.validate()
  saving.value = true
  try {
    await AigcTextSystemPromptApi.save(formData.value)
    message.success('保存成功')
    await getPrompt()
  } finally {
    saving.value = false
  }
}

const resetForm = () => {
  formData.value = savedValue.value
  formRef.value?.clearValidate()
}

onMounted(() => getPrompt())
</script>
